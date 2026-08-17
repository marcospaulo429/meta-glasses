package com.prontuario.glasses.atestado

import com.prontuario.glasses.soap.Provenance
import com.prontuario.glasses.soap.TranscriptSegment

/**
 * Extrai o pedido de atestado da transcrição. Regras anti-alucinação:
 * dias só se ditos ("atestado de três dias"); CID só se enunciado ("cid j zero seis"/"cid j06");
 * nada encontrado → null (sem atestado).
 */
object AtestadoExtractor {

    private val DAYS_AFTER_ATESTADO = Regex("""atestado\s+de\s+(\S+)\s+dias?""")
    private val DAYS_ANYWHERE = Regex("""(\S+)\s+dias?\b""")
    private val CID_PATTERN = Regex("""\bcid\s+([a-z])\s*(\d{2})""")

    fun extract(encounterId: String, segments: List<TranscriptSegment>): AtestadoDraft? {
        val segment = segments.firstOrNull {
            PtBrNumbers.normalize(it.text).contains("atestado")
        } ?: return null

        val norm = PtBrNumbers.normalize(segment.text)
        val afterKeyword = norm.substringAfter("atestado")

        val days = DAYS_AFTER_ATESTADO.find(norm)?.let { PtBrNumbers.parse(it.groupValues[1]) }
            ?: DAYS_ANYWHERE.find(afterKeyword)?.let { PtBrNumbers.parse(it.groupValues[1]) }

        val cid = CID_PATTERN.find(norm)?.let {
            "${it.groupValues[1].uppercase()}${it.groupValues[2]}"
        }

        return AtestadoDraft(
            encounterId = encounterId,
            days = days,
            cid = cid,
            spokenText = segment.text,
            provenance = Provenance(encounterId, segment.audioChunkSeq, segment.startMs, segment.endMs),
            patientName = null,
            doctorName = null,
            doctorCrm = null,
        )
    }
}
