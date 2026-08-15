package com.prontuario.glasses.vault

import java.io.File
import java.security.KeyPairGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Valida o modo blindado (RECOVERY_ONLY) na JVM — caminho sem Keystore.
 * O caminho LOCAL (KEK) depende do AndroidKeyStore e é coberto em instrumented test (Fase B).
 */
class SecurityVaultRecoveryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val keyPair = KeyPairGenerator.getInstance("RSA")
        .apply { initialize(RecoveryKeyStore.RSA_BITS) }
        .genKeyPair()

    private fun encryptRecoveryOnly(content: ByteArray, aad: ByteArray): Triple<ChunkCrypto, File, File> {
        val plain = tmp.newFile("chunk.tmp").apply { writeBytes(content) }
        val encrypted = tmp.newFile("chunk.enc")
        val crypto = SecurityVault.encryptFile(plain, encrypted, aad, WrapPolicy.RecoveryOnly(keyPair.public))
        return Triple(crypto, encrypted, plain)
    }

    @Test
    fun `chunk blindado nao guarda envelope local`() {
        val (crypto, _, _) = encryptRecoveryOnly("video".toByteArray(), "e1:0".toByteArray())
        assertEquals(ChunkCrypto.MODE_RECOVERY_ONLY, crypto.wrapMode)
        assertNull("aparelho não pode ter como decifrar", crypto.wrappedDek)
        assertNull(crypto.wrapIv)
    }

    @Test
    fun `decifra local e recusada para chunk blindado`() {
        val (crypto, encrypted, _) = encryptRecoveryOnly("video".toByteArray(), "e1:0".toByteArray())
        val out = tmp.newFile("out.bin")
        assertThrows(IllegalArgumentException::class.java) {
            SecurityVault.decryptFile(crypto, encrypted, out, "e1:0".toByteArray(), "just-1")
        }
    }

    @Test
    fun `custodiante decifra com a chave privada e justificativa`() {
        val content = "frames de video da consulta".toByteArray()
        val aad = "e1:7".toByteArray()
        val (crypto, encrypted, _) = encryptRecoveryOnly(content, aad)
        val out = tmp.newFile("out.bin")
        SecurityVault.decryptWithRecoveryKey(crypto, encrypted, out, aad, keyPair.private, "audit-42")
        assertEquals(String(content), out.readText())
    }

    @Test
    fun `decifra sem justificativa e recusada`() {
        val (crypto, encrypted, _) = encryptRecoveryOnly("x".toByteArray(), "e1:0".toByteArray())
        val out = tmp.newFile("out.bin")
        assertThrows(IllegalArgumentException::class.java) {
            SecurityVault.decryptWithRecoveryKey(crypto, encrypted, out, "e1:0".toByteArray(), keyPair.private, "")
        }
    }

    @Test
    fun `aad errada impede transplante de chunk entre consultas`() {
        val (crypto, encrypted, _) = encryptRecoveryOnly("x".toByteArray(), "e1:0".toByteArray())
        val out = tmp.newFile("out.bin")
        assertThrows(Exception::class.java) {
            SecurityVault.decryptWithRecoveryKey(
                crypto, encrypted, out, "OUTRA-consulta:0".toByteArray(), keyPair.private, "audit-1",
            )
        }
    }

    @Test
    fun `hash de integridade bate com o conteudo original`() {
        val plain = tmp.newFile("a.bin").apply { writeBytes("prova de integridade".toByteArray()) }
        val expected = SecurityVault.sha256(plain)
        val encrypted = tmp.newFile("a.enc")
        val crypto = SecurityVault.encryptFile(plain, encrypted, "e:0".toByteArray(), WrapPolicy.RecoveryOnly(keyPair.public))
        assertEquals(expected, crypto.sha256Plain)
    }
}
