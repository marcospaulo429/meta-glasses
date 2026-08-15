package com.prontuario.glasses.asr

import com.prontuario.glasses.soap.TranscriptSegment
import java.io.File

/**
 * Interface do ASR PT-BR on-device. Candidatos a benchmark (docs/PLANO.md Fase B):
 * whisper.cpp small/medium, Vosk PT-BR, SpeechRecognizer offline. Meta de WER pendente (IA-03).
 */
interface AsrEngine {
    suspend fun transcribe(wavFile: File, audioChunkSeq: Int): List<TranscriptSegment>
}

/** Stub até a integração do motor real; nunca inventa texto. */
class StubAsrEngine : AsrEngine {
    override suspend fun transcribe(wavFile: File, audioChunkSeq: Int): List<TranscriptSegment> =
        emptyList()
}
