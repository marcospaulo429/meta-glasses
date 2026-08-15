package com.prontuario.glasses.asr

import com.prontuario.glasses.soap.TranscriptSegment
import org.json.JSONObject

/**
 * Transcrição streaming com proveniência: converte o fluxo PCM em segmentos com
 * timestamps (ms desde o início da consulta), base do provenance de cada fato (IA-02).
 * Lógica pura testável na JVM; o Vosk entra pela [SpeechPort].
 */
class StreamingTranscriber(
    private val port: SpeechPort,
    private val onSegment: (TranscriptSegment) -> Unit,
    private val onPartial: (String) -> Unit = {},
    private val onUtteranceEnd: () -> Unit = {},
) : AutoCloseable {

    companion object {
        private const val BYTES_PER_MS = 32L // 16 kHz * 16 bits mono = 32 bytes/ms
        private const val CHUNK_MS = 60_000L
    }

    private var bytesFed = 0L
    private var utteranceStartMs = 0L

    @Synchronized
    fun feed(pcm: ByteArray, length: Int) {
        val isFinal = port.acceptWaveform(pcm, length)
        bytesFed += length
        val nowMs = bytesFed / BYTES_PER_MS
        if (isFinal) {
            emitSegment(port.result(), nowMs)
            utteranceStartMs = nowMs
            onUtteranceEnd()
        } else {
            val partial = JSONObject(port.partial()).optString("partial")
            if (partial.isNotBlank()) onPartial(partial)
        }
    }

    /** Descarrega a última sentença pendente (fim da consulta). */
    @Synchronized
    fun finish() {
        emitSegment(port.finalResult(), bytesFed / BYTES_PER_MS)
        onUtteranceEnd()
    }

    private fun emitSegment(resultJson: String, nowMs: Long) {
        val json = JSONObject(resultJson)
        val text = json.optString("text").trim()
        if (text.isEmpty()) return

        // Timestamps por palavra quando o Vosk fornece (setWords=true); senão, janela da sentença
        var startMs = utteranceStartMs
        var endMs = nowMs
        json.optJSONArray("result")?.let { words ->
            if (words.length() > 0) {
                startMs = (words.getJSONObject(0).optDouble("start", startMs / 1000.0) * 1000).toLong()
                endMs = (words.getJSONObject(words.length() - 1).optDouble("end", endMs / 1000.0) * 1000).toLong()
            }
        }
        onSegment(
            TranscriptSegment(
                audioChunkSeq = (startMs / CHUNK_MS).toInt(),
                startMs = startMs,
                endMs = endMs,
                text = text,
            ),
        )
    }

    override fun close() {
        port.close()
    }
}
