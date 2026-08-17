package com.prontuario.glasses.asr

import java.text.Normalizer

/**
 * Detecção de comando de voz no texto parcial do ASR ("registrar imagem" → capturePhoto,
 * HW-01: interação sem display). Dispara no máximo uma vez por sentença.
 */
class VoiceCommandDetector(
    private val onPhotoCommand: () -> Unit,
    private val onStopCommand: () -> Unit = {},
    private val onAtestadoCommand: () -> Unit = {},
) {
    companion object {
        // Prefixos tolerantes a erros do ASR (teste 15/08: "imagem" transcrito como "image")
        private val PHOTO_KEYWORDS = listOf("registrar ima", "tirar foto", "capturar ima")
        private val STOP_KEYWORDS = listOf("encerrar consulta", "finalizar consulta")
        private val ATESTADO_KEYWORDS = listOf("emitir atestado", "atestado de")

        fun normalize(text: String): String =
            Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
                .replace(Regex("\\p{M}"), "")
    }

    private var photoFired = false
    private var stopFired = false
    private var atestadoFired = false

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
        if (!atestadoFired && ATESTADO_KEYWORDS.any { normalized.contains(it) }) {
            atestadoFired = true
            onAtestadoCommand()
        }
    }

    fun onUtteranceEnd() {
        photoFired = false
        stopFired = false
        atestadoFired = false
    }
}
