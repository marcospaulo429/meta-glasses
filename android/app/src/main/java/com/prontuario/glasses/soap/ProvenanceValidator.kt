package com.prontuario.glasses.soap

import java.text.Normalizer

/**
 * Validador anti-alucinação (IA-02): mede o unsupported-statement-rate — fração de fatos
 * do rascunho cujo texto NÃO encontra suporte na transcrição. Meta do projeto: 0% no
 * classificador heurístico; vira métrica de aceitação quando o LLM entrar (docs/PLANO.md §6).
 */
object ProvenanceValidator {

    data class Report(
        val totalFacts: Int,
        val unsupportedFacts: List<Fact>,
        val factsWithoutProvenance: List<Fact>,
    ) {
        val unsupportedStatementRate: Double
            get() = if (totalFacts == 0) 0.0 else unsupportedFacts.size.toDouble() / totalFacts
        val passes: Boolean
            get() = unsupportedFacts.isEmpty() && factsWithoutProvenance.isEmpty()
    }

    private const val MIN_TOKEN_OVERLAP = 0.6

    private fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")

    private fun tokens(text: String): Set<String> =
        normalize(text).split(Regex("\\W+")).filter { it.length > 2 }.toSet()

    fun validate(note: SoapNote, transcript: List<TranscriptSegment>): Report {
        val allFacts = note.subjective + note.objective + note.assessment + note.plan
        val stated = allFacts.filter { it.status != FactStatus.NOT_INFORMED }

        val transcriptTokens = tokens(transcript.joinToString(" ") { it.text })
        val unsupported = stated.filter { fact ->
            val factTokens = tokens(fact.text)
            if (factTokens.isEmpty()) return@filter false
            val overlap = factTokens.count { it in transcriptTokens }.toDouble() / factTokens.size
            overlap < MIN_TOKEN_OVERLAP
        }
        val withoutProvenance = stated.filter { it.provenance == null }
        return Report(
            totalFacts = stated.size,
            unsupportedFacts = unsupported,
            factsWithoutProvenance = withoutProvenance,
        )
    }
}
