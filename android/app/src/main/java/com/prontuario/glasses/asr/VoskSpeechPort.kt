package com.prontuario.glasses.asr

import android.util.Log
import com.prontuario.glasses.audio.ConsultationAudioRecorder
import java.io.File
import org.vosk.Model
import org.vosk.Recognizer

/** Implementação Vosk (on-device, PT-BR). Modelo instalado via scripts/install-vosk-model.sh. */
class VoskSpeechPort private constructor(
    private val model: Model,
    private val recognizer: Recognizer,
) : SpeechPort {

    companion object {
        private const val TAG = "VoskSpeechPort"

        /** @return null se o modelo não está instalado — o app segue sem ASR (IA-03). */
        fun tryCreate(modelDir: File): VoskSpeechPort? {
            if (!modelDir.isDirectory || modelDir.listFiles().isNullOrEmpty()) {
                Log.w(TAG, "Modelo Vosk ausente em ${modelDir.absolutePath}")
                return null
            }
            return try {
                val model = Model(modelDir.absolutePath)
                val recognizer = Recognizer(
                    model,
                    ConsultationAudioRecorder.SAMPLE_RATE.toFloat(),
                ).apply { setWords(true) }
                VoskSpeechPort(model, recognizer)
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao carregar modelo Vosk", e)
                null
            }
        }
    }

    override fun acceptWaveform(pcm: ByteArray, length: Int): Boolean =
        recognizer.acceptWaveForm(pcm, length)

    override fun result(): String = recognizer.result

    override fun partial(): String = recognizer.partialResult

    override fun finalResult(): String = recognizer.finalResult

    override fun close() {
        runCatching { recognizer.close() }
        runCatching { model.close() }
    }
}
