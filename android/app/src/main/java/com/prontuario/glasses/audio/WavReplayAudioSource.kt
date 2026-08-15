package com.prontuario.glasses.audio

import java.io.DataInputStream
import java.io.File

/**
 * HARNESS DE TESTE (debug): substitui o microfone reproduzindo um WAV 16 kHz mono PCM16
 * em ritmo real. Permite validar o pipeline inteiro (ASR → SOAP → cofre) no emulador,
 * onde não há microfone físico. Nunca ativo em release.
 */
class WavReplayAudioSource(
    private val wavFile: File,
    private val onPcm: (ByteArray, Int) -> Unit,
    private val onFinished: () -> Unit,
) {
    private var thread: Thread? = null

    @Volatile
    private var running = false

    fun start() {
        running = true
        thread = Thread({
            try {
                DataInputStream(wavFile.inputStream().buffered()).use { input ->
                    skipToDataChunk(input)
                    val buffer = ByteArray(3200) // 100 ms @ 16 kHz/16-bit mono
                    while (running) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        onPcm(buffer, read)
                        Thread.sleep(100)
                    }
                }
            } finally {
                if (running) onFinished()
            }
        }, "wav-replay").also { it.start() }
    }

    fun stop() {
        running = false
        thread?.join(1_000)
        thread = null
    }

    /** Varre os chunks RIFF até "data" (headers WAV nem sempre têm 44 bytes). */
    private fun skipToDataChunk(input: DataInputStream) {
        input.skipBytes(12) // "RIFF" + size + "WAVE"
        while (true) {
            val id = ByteArray(4).also { input.readFully(it) }.toString(Charsets.US_ASCII)
            val size = Integer.reverseBytes(input.readInt())
            if (id == "data") return
            input.skipBytes(size)
        }
    }
}
