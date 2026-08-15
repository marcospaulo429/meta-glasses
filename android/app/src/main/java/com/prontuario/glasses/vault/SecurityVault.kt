package com.prontuario.glasses.vault

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Metadados de cifra por chunk; persistidos no manifesto do encontro. */
class ChunkCrypto(
    val wrapMode: String,
    val wrappedDek: String?,
    val wrapIv: String?,
    val wrappedDekRecovery: String?,
    val chunkIv: String,
    val sha256Plain: String,
) {
    companion object {
        const val MODE_LOCAL = "LOCAL"
        const val MODE_RECOVERY_ONLY = "RECOVERY_ONLY"
    }
}

/** Política de embrulho da DEK (docs/LGPD.md §8). */
sealed class WrapPolicy {
    /** Decifrável no aparelho (KEK no Keystore) — áudio clínico, que o pipeline consome. */
    data object Local : WrapPolicy()

    /**
     * Modo blindado: DEK embrulhada SOMENTE com a chave pública do custodiante.
     * O aparelho é tecnicamente incapaz de decifrar — abertura exige a chave privada
     * (custódia institucional, gatilho por ordem judicial).
     */
    data class RecoveryOnly(val custodianPublicKey: PublicKey) : WrapPolicy()
}

/**
 * Cofre por envelope (spec arquiteto-android §5): DEK AES-256 por chunk (AES-GCM),
 * AAD = encounterId+seq impede transplante de chunks entre sessões (SEC-01, POL-04).
 */
object SecurityVault {

    private const val KEK_ALIAS = "prontuario_kek_v1"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val GCM_TAG_BITS = 128
    private const val RSA_OAEP = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
    private val random = SecureRandom()

    private fun kek(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (ks.getKey(KEK_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEK_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun encryptFile(
        plain: File,
        encrypted: File,
        aad: ByteArray,
        policy: WrapPolicy = WrapPolicy.Local,
    ): ChunkCrypto {
        val hash = sha256(plain)

        val dekBytes = ByteArray(32).also(random::nextBytes)
        val dek = SecretKeySpec(dekBytes, "AES")
        val chunkIv = ByteArray(12).also(random::nextBytes)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, dek, GCMParameterSpec(GCM_TAG_BITS, chunkIv))
        cipher.updateAAD(aad)
        plain.inputStream().use { input ->
            CipherOutputStream(encrypted.outputStream(), cipher).use { output ->
                input.copyTo(output, 64 * 1024)
            }
        }

        val crypto = when (policy) {
            is WrapPolicy.Local -> {
                val wrapCipher = Cipher.getInstance("AES/GCM/NoPadding")
                wrapCipher.init(Cipher.ENCRYPT_MODE, kek())
                val wrapped = wrapCipher.doFinal(dekBytes)
                ChunkCrypto(
                    wrapMode = ChunkCrypto.MODE_LOCAL,
                    wrappedDek = Base64.getEncoder().encodeToString(wrapped),
                    wrapIv = Base64.getEncoder().encodeToString(wrapCipher.iv),
                    wrappedDekRecovery = null,
                    chunkIv = Base64.getEncoder().encodeToString(chunkIv),
                    sha256Plain = hash,
                )
            }
            is WrapPolicy.RecoveryOnly -> {
                val rsa = Cipher.getInstance(RSA_OAEP)
                rsa.init(Cipher.ENCRYPT_MODE, policy.custodianPublicKey)
                val wrapped = rsa.doFinal(dekBytes)
                ChunkCrypto(
                    wrapMode = ChunkCrypto.MODE_RECOVERY_ONLY,
                    wrappedDek = null,
                    wrapIv = null,
                    wrappedDekRecovery = Base64.getEncoder().encodeToString(wrapped),
                    chunkIv = Base64.getEncoder().encodeToString(chunkIv),
                    sha256Plain = hash,
                )
            }
        }
        dekBytes.fill(0)
        return crypto
    }

    /**
     * Decifra local (áudio). Chunks RECOVERY_ONLY são irrecuperáveis por este caminho —
     * por design. Exige justificativa registrada ANTES no log de auditoria.
     */
    fun decryptFile(
        crypto: ChunkCrypto,
        encrypted: File,
        out: File,
        aad: ByteArray,
        auditJustificationId: String,
    ) {
        require(auditJustificationId.isNotBlank()) { "Acesso sem justificativa registrada" }
        require(crypto.wrapMode == ChunkCrypto.MODE_LOCAL && crypto.wrappedDek != null && crypto.wrapIv != null) {
            "Chunk em modo blindado: só a chave privada do custodiante decifra (ordem judicial)"
        }

        val unwrapCipher = Cipher.getInstance("AES/GCM/NoPadding")
        unwrapCipher.init(
            Cipher.DECRYPT_MODE,
            kek(),
            GCMParameterSpec(GCM_TAG_BITS, Base64.getDecoder().decode(crypto.wrapIv)),
        )
        val dekBytes = unwrapCipher.doFinal(Base64.getDecoder().decode(crypto.wrappedDek))
        decryptWithDek(dekBytes, crypto, encrypted, out, aad)
    }

    /**
     * Caminho break-glass: usado pelo custodiante (fora do fluxo normal do app) com a
     * chave privada apresentada sob ordem judicial. Justificativa de auditoria obrigatória.
     */
    fun decryptWithRecoveryKey(
        crypto: ChunkCrypto,
        encrypted: File,
        out: File,
        aad: ByteArray,
        custodianPrivateKey: PrivateKey,
        auditJustificationId: String,
    ) {
        require(auditJustificationId.isNotBlank()) { "Acesso sem justificativa registrada" }
        val wrapped = requireNotNull(crypto.wrappedDekRecovery) { "Chunk sem envelope de recuperação" }
        val rsa = Cipher.getInstance(RSA_OAEP)
        rsa.init(Cipher.DECRYPT_MODE, custodianPrivateKey)
        val dekBytes = rsa.doFinal(Base64.getDecoder().decode(wrapped))
        decryptWithDek(dekBytes, crypto, encrypted, out, aad)
    }

    private fun decryptWithDek(
        dekBytes: ByteArray,
        crypto: ChunkCrypto,
        encrypted: File,
        out: File,
        aad: ByteArray,
    ) {
        val dek = SecretKeySpec(dekBytes, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            dek,
            GCMParameterSpec(GCM_TAG_BITS, Base64.getDecoder().decode(crypto.chunkIv)),
        )
        cipher.updateAAD(aad)
        CipherInputStream(encrypted.inputStream(), cipher).use { input ->
            out.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
        }
        dekBytes.fill(0)
    }
}
