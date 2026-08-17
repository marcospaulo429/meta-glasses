package com.prontuario.glasses.atestado

import com.prontuario.glasses.soap.TranscriptSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PtBrNumbersTest {

    @Test
    fun `numeros por extenso e digitos`() {
        assertEquals(3, PtBrNumbers.parse("três"))
        assertEquals(3, PtBrNumbers.parse("tres"))
        assertEquals(15, PtBrNumbers.parse("quinze"))
        assertEquals(7, PtBrNumbers.parse("7"))
        assertEquals(2, PtBrNumbers.parse("Duas"))
    }

    @Test
    fun `token desconhecido retorna null, nunca chuta`() {
        assertNull(PtBrNumbers.parse("alguns"))
        assertNull(PtBrNumbers.parse("de"))
    }
}

class AtestadoExtractorTest {

    private fun seg(text: String, start: Long = 30_000, end: Long = 35_000) =
        TranscriptSegment(audioChunkSeq = 0, startMs = start, endMs = end, text = text)

    @Test
    fun `extrai dias do padrao atestado de N dias`() {
        val draft = AtestadoExtractor.extract(
            "e1",
            listOf(seg("prescrevo dipirona emitir atestado de três dias por motivo de doença")),
        )
        assertNotNull(draft)
        assertEquals(3, draft!!.days)
        assertEquals(30_000, draft.provenance!!.startMs)
    }

    @Test
    fun `dias ditos depois da palavra atestado tambem contam`() {
        val draft = AtestadoExtractor.extract("e1", listOf(seg("vou emitir um atestado para você de quinze dias")))
        assertEquals(15, draft!!.days)
    }

    @Test
    fun `sem numero dito, days fica null - nao informado`() {
        val draft = AtestadoExtractor.extract("e1", listOf(seg("vou emitir o atestado depois")))
        assertNotNull(draft)
        assertNull("nunca inventar dias", draft!!.days)
    }

    @Test
    fun `sem mencao a atestado nao gera rascunho`() {
        assertNull(AtestadoExtractor.extract("e1", listOf(seg("prescrevo repouso por três dias"))))
    }

    @Test
    fun `cid so quando enunciado`() {
        val comCid = AtestadoExtractor.extract("e1", listOf(seg("atestado de dois dias cid j 06")))
        assertEquals("J06", comCid!!.cid)
        val semCid = AtestadoExtractor.extract("e1", listOf(seg("atestado de dois dias")))
        assertNull(semCid!!.cid)
    }

    @Test
    fun `roundtrip json preserva campos incluindo nulls`() {
        val original = AtestadoExtractor.extract("e1", listOf(seg("atestado de cinco dias")))!!
            .copy(patientName = "Paciente Demo", doctorName = "Dra. Ana", doctorCrm = "CRM-GO 123")
        val restored = AtestadoJson.fromJson(AtestadoJson.toJson(original))
        assertEquals(original, restored)

        val semDias = AtestadoExtractor.extract("e1", listOf(seg("emitir atestado hoje")))!!
        assertNull(AtestadoJson.fromJson(AtestadoJson.toJson(semDias)).days)
    }
}
