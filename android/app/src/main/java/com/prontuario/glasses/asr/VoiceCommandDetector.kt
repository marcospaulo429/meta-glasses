package com.prontuario.glasses.asr

import java.text.Normalizer

/**
 * Detecção de comando de voz no texto parcial do ASR ("registrar imagem" → capturePhoto,
 * HW-01: interação sem display). Dispara no máximo uma vez por sentença.
 */
class VoiceCommandDetector(
    private val onPhotoCommand: () -> Unit,
    private val onStopCommand: () -> Unit = {},
) {
    companion object {
        // Prefixos tolerantes a erros do ASR (teste 15/08: "imagem" transcrito como "image")
        private val PHOTO_KEYWORDS = listOf("registrar ima", "tirar foto", "capturar ima")
        private val STOP_KEYWORDS = listOf("encerrar consulta", "finalizar consulta")

        fun normalize(text: String): String =
            Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
                .replace(Regex("\\p{M}"), "")
    }

    private var photoFired = false
    private var stopFired = false

    fun onPartial(text: String) {
        val normalized = normalize(text)
        if (!photoFired && PHOTO_KEYWORDS.any { normalized.contains(it) }) {
            photoFired = true
            onPhotoCommand()
        }
        if (!stopFired && STOP_KEYWORDS.any { normalized.contains(it) }) {
            stopFired = true
            onStopCommand()
        }
    }

    fun onUtteranceEnd() {
        photoFired = false
        stopFired = false
    }
}
