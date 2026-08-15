package com.prontuario.glasses.soap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun fact(text: String, seq: Int = 0, start: Long = 0, end: Long = 1000) =
    Fact(text, FactStatus.STATED, Provenance("e1", seq, start, end))

class HeuristicSoapClassifierTest {

    @Test
    fun `classifica as quatro secoes por marcadores`() {
        val facts = listOf(
            fact("paciente relata dor de cabeça desde ontem"),
            fact("pressão arterial doze por oito"),
            fact("hipótese diagnóstica de enxaqueca tensional"),
            fact("prescrevo dipirona quinhentos miligramas e retorno em sete dias"),
        )
        val note = HeuristicSoapClassifier.classify("e1", facts)
        assertEquals(1, note.subjective.size)
        assertEquals(1, note.objective.size)
        assertEquals(1, note.assessment.size)
        assertEquals(1, note.plan.size)
    }

    @Test
    fun `fato sem marcador vai para S como incerto, nunca inventa secao`() {
        val note = HeuristicSoapClassifier.classify("e1", listOf(fact("tudo tranquilo por aqui")))
        assertEquals(1, note.subjective.size)
        assertEquals(FactStatus.UNCERTAIN, note.subjective[0].status)
        assertTrue(note.assessment.isEmpty())
    }

    @Test
    fun `avaliacao nao e inferida de sintomas`() {
        val note = HeuristicSoapClassifier.classify(
            "e1",
            listOf(fact("paciente refere febre e dor no corpo há três dias")),
        )
        assertTrue("A só entra se explicitamente enunciada", note.assessment.isEmpty())
    }

    @Test
    fun `marcadores funcionam com acentos do ASR normalizados`() {
        val note = HeuristicSoapClassifier.classify(
            "e1",
            listOf(fact("ao exame abdome flácido indolor")),
        )
        assertEquals(1, note.objective.size)
    }
}

class ProvenanceValidatorTest {

    private val transcript = listOf(
        TranscriptSegment(0, 0, 3000, "paciente relata dor de cabeça desde ontem"),
        TranscriptSegment(0, 4000, 8000, "pressão arterial doze por oito"),
        TranscriptSegment(1, 61000, 65000, "prescrevo dipirona e retorno em sete dias"),
    )

    @Test
    fun `rascunho fiel passa com taxa zero`() {
        val note = HeuristicSoapClassifier.classify(
            "e1",
            listOf(
                fact("paciente relata dor de cabeça desde ontem"),
                fact("pressão arterial doze por oito", seq = 0, start = 4000, end = 8000),
                fact("prescrevo dipirona e retorno em sete dias", seq = 1, start = 61000, end = 65000),
            ),
        )
        val report = ProvenanceValidator.validate(note, transcript)
        assertEquals(0.0, report.unsupportedStatementRate, 0.0001)
        assertTrue(report.passes)
    }

    @Test
    fun `fato alucinado e detectado`() {
        val note = SoapNote(
            encounterId = "e1",
            plan = listOf(fact("prescrevo amoxicilina oitocentos miligramas por dez dias")),
        )
        val report = ProvenanceValidator.validate(note, transcript)
        assertEquals(1, report.unsupportedFacts.size)
        assertFalse(report.passes)
        assertTrue(report.unsupportedStatementRate > 0.99)
    }

    @Test
    fun `nao informado nao conta na taxa`() {
        val note = SoapNote(
            encounterId = "e1",
            assessment = listOf(Fact("não informado", FactStatus.NOT_INFORMED, null)),
        )
        val report = ProvenanceValidator.validate(note, transcript)
        assertEquals(0, report.totalFacts)
        assertTrue(report.passes)
    }

    @Test
    fun `paráfrase leve dentro do limiar e aceita`() {
        val note = SoapNote(
            encounterId = "e1",
            subjective = listOf(fact("relata dor de cabeça desde ontem")),
        )
        assertTrue(ProvenanceValidator.validate(note, transcript).passes)
    }
}
