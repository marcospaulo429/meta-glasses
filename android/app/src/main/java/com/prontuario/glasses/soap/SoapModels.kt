package com.prontuario.glasses.soap

/**
 * Modelo SOAP com proveniência obrigatória (MEMORY.md §4: extração factual antes de
 * estruturação; LLM não completa lacunas; "não informado"/"incerto" são estados válidos).
 */
data class Provenance(
    val encounterId: String,
    val audioChunkSeq: Int,
    val startMs: Long,
    val endMs: Long,
)

enum class FactStatus { STATED, UNCERTAIN, NOT_INFORMED }

data class Fact(
    val text: String,
    val status: FactStatus,
    val provenance: Provenance?,
) {
    init {
        // Invariante anti-alucinação: fato afirmado sem proveniência é proibido (IA-02).
        require(status == FactStatus.NOT_INFORMED || provenance != null) {
            "Fato '$text' sem proveniência"
        }
    }
}

data class SoapNote(
    val encounterId: String,
    val subjective: List<Fact> = emptyList(),
    val objective: List<Fact> = emptyList(),
    val assessment: List<Fact> = emptyList(),
    val plan: List<Fact> = emptyList(),
    val reviewedByPhysician: Boolean = false,
)

data class TranscriptSegment(
    val audioChunkSeq: Int,
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

/** Estágio 1 do pipeline (fatos), separado do estágio 2 (classificação SOAP). */
interface FactExtractor {
    suspend fun extract(encounterId: String, transcript: List<TranscriptSegment>): List<Fact>
}

/** Placeholder até o benchmark de LLM local (IA-04): não inventa nada, só ecoa segmentos. */
class PassthroughFactExtractor : FactExtractor {
    override suspend fun extract(
        encounterId: String,
        transcript: List<TranscriptSegment>,
    ): List<Fact> = transcript.map { segment ->
        Fact(
            text = segment.text,
            status = FactStatus.UNCERTAIN,
            provenance = Provenance(encounterId, segment.audioChunkSeq, segment.startMs, segment.endMs),
        )
    }
}
