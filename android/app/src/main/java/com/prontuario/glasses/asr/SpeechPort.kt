package com.prontuario.glasses.asr

/**
 * Porta para o reconhecedor de fala. Isola o Vosk (lib nativa) da lógica de
 * transcrição para que o pipeline seja testável na JVM com um fake.
 */
interface SpeechPort : AutoCloseable {
    /** @return true quando uma sentença foi finalizada (resultado em [result]). */
    fun acceptWaveform(pcm: ByteArray, length: Int): Boolean

    /** JSON Vosk: {"text": "...", "result":[{word,start,end,conf}...]} */
    fun result(): String

    /** JSON Vosk: {"partial": "..."} */
    fun partial(): String

    /** JSON final ao encerrar o stream. */
    fun finalResult(): String
}
