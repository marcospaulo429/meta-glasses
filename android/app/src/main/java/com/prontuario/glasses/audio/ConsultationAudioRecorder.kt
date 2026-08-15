package com.prontuario.glasses.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AudioRecord persistente durante toda a consulta (MEMORY.md §4: não abrir/fechar HFP por comando).
 * 16 kHz mono PCM16: teto prático do HFP (AND-03) e entrada padrão de ASR (Whisper/Vosk).
 */
class ConsultationAudioRecorder(
    private val onPcm: (buffer: ByteArray, bytesRead: Int) -> Unit,
) {
    companion object {
        private const val TAG = "ConsultationAudioRec"
        const val SAMPLE_RATE = 16_000
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }

    private val _wasInterrupted = MutableStateFlow(false)
    val wasInterrupted: StateFlow<Boolean> = _wasInterrupted.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var thread: Thread? = null
    private val running = AtomicBoolean(false)

    /** Pré-requisito: RECORD_AUDIO concedida e FGS microphone já em foreground (AND-01). */
    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (running.get()) return true
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        if (minBuffer <= 0) {
            Log.e(TAG, "Buffer inválido: $minBuffer")
            return false
        }
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SAMPLE_RATE,
            CHANNEL,
            ENCODING,
            minBuffer * 2,
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord não inicializou")
            record.release()
            return false
        }
        audioRecord = record
        _wasInterrupted.value = false
        running.set(true)
        record.startRecording()
        thread = Thread({
            val buffer = ByteArray(minBuffer)
            while (running.get()) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    onPcm(buffer, read)
                } else if (read < 0) {
                    Log.w(TAG, "Leitura interrompida: $read")
                    _wasInterrupted.value = true
                    running.set(false)
                }
            }
        }, "consultation-audio").also { it.start() }
        return true
    }

    fun stop() {
        running.set(false)
        thread?.join(1_000)
        thread = null
        audioRecord?.let {
            runCatching { it.stop() }
            it.release()
        }
        audioRecord = null
    }
}
