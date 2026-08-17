package com.prontuario.glasses.atestado

import java.text.Normalizer

/** Números por extenso PT-BR como saem do ASR ("três dias", "quinze dias"). */
object PtBrNumbers {

    private val WORDS = mapOf(
        "um" to 1, "uma" to 1, "dois" to 2, "duas" to 2, "tres" to 3, "quatro" to 4,
        "cinco" to 5, "seis" to 6, "sete" to 7, "oito" to 8, "nove" to 9, "dez" to 10,
        "onze" to 11, "doze" to 12, "treze" to 13, "quatorze" to 14, "catorze" to 14,
        "quinze" to 15, "vinte" to 20, "trinta" to 30,
    )

    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")

    /** @return null se o token não é um número reconhecível — nunca chutar. */
    fun parse(token: String): Int? {
        val norm = normalize(token.trim())
        return norm.toIntOrNull() ?: WORDS[norm]
    }
}
