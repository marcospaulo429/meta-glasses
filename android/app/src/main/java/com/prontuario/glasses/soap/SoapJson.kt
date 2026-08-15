package com.prontuario.glasses.soap

import org.json.JSONArray
import org.json.JSONObject

/** Serialização dos artefatos clínicos (documentos cifrados do encontro). */
object SoapJson {

    fun transcriptToJson(segments: List<TranscriptSegment>): JSONObject =
        JSONObject().put(
            "segments",
            JSONArray().apply {
                segments.forEach {
                    put(
                        JSONObject()
                            .put("seq", it.audioChunkSeq)
                            .put("startMs", it.startMs)
                            .put("endMs", it.endMs)
                            .put("text", it.text),
                    )
                }
            },
        )

    fun transcriptFromJson(json: JSONObject): List<TranscriptSegment> {
        val array = json.getJSONArray("segments")
        return (0 until array.length()).map { i ->
            val seg = array.getJSONObject(i)
            TranscriptSegment(
                audioChunkSeq = seg.getInt("seq"),
                startMs = seg.getLong("startMs"),
                endMs = seg.getLong("endMs"),
                text = seg.getString("text"),
            )
        }
    }

    fun noteToJson(note: SoapNote, validation: ProvenanceValidator.Report? = null): JSONObject =
        JSONObject()
            .put("encounterId", note.encounterId)
            .put("reviewed", note.reviewedByPhysician)
            .put("S", factsToJson(note.subjective))
            .put("O", factsToJson(note.objective))
            .put("A", factsToJson(note.assessment))
            .put("P", factsToJson(note.plan))
            .putOpt(
                "validation",
                validation?.let {
                    JSONObject()
                        .put("totalFacts", it.totalFacts)
                        .put("unsupportedStatementRate", it.unsupportedStatementRate)
                        .put("passes", it.passes)
                },
            )

    fun noteFromJson(json: JSONObject): SoapNote =
        SoapNote(
            encounterId = json.getString("encounterId"),
            subjective = factsFromJson(json.getJSONArray("S")),
            objective = factsFromJson(json.getJSONArray("O")),
            assessment = factsFromJson(json.getJSONArray("A")),
            plan = factsFromJson(json.getJSONArray("P")),
            reviewedByPhysician = json.optBoolean("reviewed"),
        )

    private fun factsToJson(facts: List<Fact>): JSONArray =
        JSONArray().apply {
            facts.forEach { fact ->
                put(
                    JSONObject()
                        .put("text", fact.text)
                        .put("status", fact.status.name)
                        .putOpt(
                            "provenance",
                            fact.provenance?.let {
                                JSONObject()
                                    .put("encounterId", it.encounterId)
                                    .put("seq", it.audioChunkSeq)
                                    .put("startMs", it.startMs)
                                    .put("endMs", it.endMs)
                            },
                        ),
                )
            }
        }

    private fun factsFromJson(array: JSONArray): List<Fact> =
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            Fact(
                text = obj.getString("text"),
                status = FactStatus.valueOf(obj.getString("status")),
                provenance = obj.optJSONObject("provenance")?.let {
                    Provenance(
                        encounterId = it.getString("encounterId"),
                        audioChunkSeq = it.getInt("seq"),
                        startMs = it.getLong("startMs"),
                        endMs = it.getLong("endMs"),
                    )
                },
            )
        }
}
