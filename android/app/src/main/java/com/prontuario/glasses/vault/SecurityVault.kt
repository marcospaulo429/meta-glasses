package com.prontuario.glasses.vault

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

/** Metadados de cifra por chunk; persistidos no manifesto do encontro. */
class ChunkCrypto(
    val wrappedDek: String,
    val wrapIv: String,
    val chunkIv: String,
    val sha256Plain: String,
)

/**
 * Cofre por envelope (spec arquiteto-android §5): DEK AES-256 por chunk (AES-GCM),
 * KEK não-exportável no Android Keystore. AAD = encounterId+seq impede transplante de chunks.
 * Break-glass com chave de recuperação institucional: SEC-01 (fase 2 — requer custodiante).
 */
object SecurityVault {

    private const val KEK_ALIAS = "prontuario_kek_v1"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val GCM_TAG_BITS = 128
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

    fun encryptFile(plain: File, encrypted: File, aad: ByteArray): ChunkCrypto {
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

        val wrapCipher = Cipher.getInstance("AES/GCM/NoPadding")
        wrapCipher.init(Cipher.ENCRYPT_MODE, kek())
        val wrapped = wrapCipher.doFinal(dekBytes)
        dekBytes.fill(0)

        return ChunkCrypto(
            wrappedDek = Base64.encodeToString(wrapped, Base64.NO_WRAP),
            wrapIv = Base64.encodeToString(wrapCipher.iv, Base64.NO_WRAP),
            chunkIv = Base64.encodeToString(chunkIv, Base64.NO_WRAP),
            sha256Plain = hash,
        )
    }

    /**
     * Decifra exige justificativa registrada ANTES no log de auditoria (docs/LGPD.md §8):
     * o chamador deve passar o id da entrada de auditoria da justificativa.
     */
    fun decryptFile(
        crypto: ChunkCrypto,
        encrypted: File,
        out: File,
        aad: ByteArray,
        auditJustificationId: String,
    ) {
        require(auditJustificationId.isNotBlank()) { "Acesso sem justificativa registrada" }

        val unwrapCipher = Cipher.getInstance("AES/GCM/NoPadding")
        unwrapCipher.init(
            Cipher.DECRYPT_MODE,
            kek(),
            GCMParameterSpec(GCM_TAG_BITS, Base64.decode(crypto.wrapIv, Base64.NO_WRAP)),
        )
        val dekBytes = unwrapCipher.doFinal(Base64.decode(crypto.wrappedDek, Base64.NO_WRAP))
        val dek = SecretKeySpec(dekBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            dek,
            GCMParameterSpec(GCM_TAG_BITS, Base64.decode(crypto.chunkIv, Base64.NO_WRAP)),
        )
        cipher.updateAAD(aad)
        CipherInputStream(encrypted.inputStream(), cipher).use { input ->
            out.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
        }
        dekBytes.fill(0)
    }
}
