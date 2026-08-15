package com.prontuario.glasses.vault

import android.content.Context
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * Chave pública do custodiante institucional (modo blindado, docs/LGPD.md §8).
 * A chave PRIVADA nunca fica no aparelho: sai uma única vez para o custodiante,
 * que só a usa mediante ordem judicial. Sem chave pública configurada, vídeo não grava.
 */
object RecoveryKeyStore {

    private const val PREFS = "recovery_keys"
    private const val KEY_PUBLIC = "custodian_public_key"
    const val RSA_BITS = 3072

    fun publicKey(context: Context): PublicKey? {
        val encoded = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PUBLIC, null) ?: return null
        return decodePublicKey(encoded)
    }

    fun importPublicKey(context: Context, base64X509: String) {
        decodePublicKey(base64X509) // valida antes de persistir
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_PUBLIC, base64X509).apply()
    }

    /**
     * SOMENTE DEMO/hackathon: gera o par no aparelho e devolve a privada em PEM para
     * entrega imediata ao custodiante. Em produção o par nasce fora do aparelho (SEC-01).
     */
    fun generateDemoPair(context: Context): String {
        val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(RSA_BITS) }.genKeyPair()
        val publicB64 = Base64.getEncoder().encodeToString(pair.public.encoded)
        importPublicKey(context, publicB64)
        val privateB64 = Base64.getEncoder().encodeToString(pair.private.encoded)
        return buildString {
            appendLine("-----BEGIN PRIVATE KEY-----")
            privateB64.chunked(64).forEach(::appendLine)
            appendLine("-----END PRIVATE KEY-----")
        }
    }

    fun decodePublicKey(base64X509: String): PublicKey =
        KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(base64X509)))
}
