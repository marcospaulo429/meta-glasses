package com.prontuario.glasses.atestado

import com.prontuario.glasses.soap.Provenance
import org.json.JSONObject

/**
 * Rascunho de atestado (CFM Res. 1.658/2002): vira documento só após confirmação do médico.
 * days=null significa "não informado" — dias NUNCA são inferidos (mesmo princípio do SOAP).
 * CID só entra com consentimento expresso do paciente E enunciado pelo médico.
 */
data class AtestadoDraft(
    val encounterId: String,
    val days: Int?,
    val cid: String?,
    val spokenText: String,
    val provenance: Provenance?,
    val patientName: String?,
    val doctorName: String?,
    val doctorCrm: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val confirmed: Boolean = false,
)

object AtestadoJson {

    fun toJson(draft: AtestadoDraft): JSONObject =
        JSONObject()
            .put("encounterId", draft.encounterId)
            .putOpt("days", draft.days)
            .putOpt("cid", draft.cid)
            .put("spokenText", draft.spokenText)
            .putOpt(
                "provenance",
                draft.provenance?.let {
                    JSONObject()
                        .put("seq", it.audioChunkSeq)
                        .put("startMs", it.startMs)
                        .put("endMs", it.endMs)
                },
            )
            .putOpt("patientName", draft.patientName)
            .putOpt("doctorName", draft.doctorName)
            .putOpt("doctorCrm", draft.doctorCrm)
            .put("createdAt", draft.createdAt)
            .put("confirmed", draft.confirmed)

    fun fromJson(json: JSONObject): AtestadoDraft =
        AtestadoDraft(
            encounterId = json.getString("encounterId"),
            days = if (json.has("days")) json.getInt("days") else null,
            cid = json.optString("cid").ifEmpty { null },
            spokenText = json.getString("spokenText"),
            provenance = json.optJSONObject("provenance")?.let {
                Provenance(
                    encounterId = json.getString("encounterId"),
                    audioChunkSeq = it.getInt("seq"),
                    startMs = it.getLong("startMs"),
                    endMs = it.getLong("endMs"),
                )
            },
            patientName = json.optString("patientName").ifEmpty { null },
            doctorName = json.optString("doctorName").ifEmpty { null },
            doctorCrm = json.optString("doctorCrm").ifEmpty { null },
            createdAt = json.getLong("createdAt"),
            confirmed = json.getBoolean("confirmed"),
        )
}
