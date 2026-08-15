package com.prontuario.glasses.capture

import com.prontuario.glasses.audio.ConsultationAudioRecorder
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File

/** Monta um WAV PCM16 mono a partir do buffer acumulado do chunk. */
object WavWriter {

    fun write(pcm: ByteArray, out: File, sampleRate: Int = ConsultationAudioRecorder.SAMPLE_RATE) {
        DataOutputStream(out.outputStream().buffered()).use { stream ->
            val byteRate = sampleRate * 2
            stream.writeBytes("RIFF")
            stream.writeIntLe(36 + pcm.size)
            stream.writeBytes("WAVE")
            stream.writeBytes("fmt ")
            stream.writeIntLe(16)
            stream.writeShortLe(1)
            stream.writeShortLe(1)
            stream.writeIntLe(sampleRate)
            stream.writeIntLe(byteRate)
            stream.writeShortLe(2)
            stream.writeShortLe(16)
            stream.writeBytes("data")
            stream.writeIntLe(pcm.size)
            stream.write(pcm)
        }
    }

    private fun DataOutputStream.writeIntLe(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
        write((value shr 16) and 0xFF)
        write((value shr 24) and 0xFF)
    }

    private fun DataOutputStream.writeShortLe(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
    }
}

/** Acumulador de PCM com rotação por duração (chunks de 60 s — spec §4). */
class PcmChunkBuffer {
    private val buffer = ByteArrayOutputStream()

    @Synchronized
    fun append(data: ByteArray, length: Int) = buffer.write(data, 0, length)

    @Synchronized
    fun drain(): ByteArray {
        val bytes = buffer.toByteArray()
        buffer.reset()
        return bytes
    }

    @Synchronized
    fun isEmpty(): Boolean = buffer.size() == 0
}
