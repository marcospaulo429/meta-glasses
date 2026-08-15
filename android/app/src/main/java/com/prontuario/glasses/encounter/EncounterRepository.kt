package com.prontuario.glasses.encounter

import android.content.Context
import com.prontuario.glasses.vault.AuditLog
import com.prontuario.glasses.vault.ChunkCrypto
import java.io.File
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

class ConsentRecord(
    val patientConsented: Boolean,
    val companionPresent: Boolean,
    val companionConsented: Boolean,
    val securityVideoConsented: Boolean,
)

class Encounter(val id: String, val dir: File)

enum class ChunkKind { AUDIO, VIDEO, PHOTO }

/**
 * Armazenamento local por consulta: manifesto JSON com cadeia de chunks (nº de sequência,
 * hash, hash do anterior — cadeia de custódia, spec §4) + log de auditoria.
 */
class EncounterRepository(context: Context) {

    private val root = File(context.filesDir, "encounters")
    val auditLog = AuditLog(File(context.filesDir, "audit/audit.jsonl"))

    /** Recusa impede início (docs/LGPD.md §3): lança se paciente não consentiu. */
    fun create(consent: ConsentRecord): Encounter {
        require(consent.patientConsented) { "Sem consentimento do paciente não há captura" }
        require(!consent.companionPresent || consent.companionConsented) {
            "Acompanhante presente exige consentimento do acompanhante"
        }
        val id = UUID.randomUUID().toString()
        val dir = File(root, id).apply { mkdirs() }
        val manifest = JSONObject()
            .put("encounterId", id)
            .put("createdAt", System.currentTimeMillis())
            .put(
                "consent",
                JSONObject()
                    .put("patient", consent.patientConsented)
                    .put("companionPresent", consent.companionPresent)
                    .put("companion", consent.companionConsented)
                    .put("securityVideo", consent.securityVideoConsented),
            )
            .put("chunks", JSONArray())
        manifestFile(dir).writeText(manifest.toString(2))
        auditLog.append(
            "encounter_created",
            JSONObject().put("encounterId", id).put("securityVideo", consent.securityVideoConsented),
        )
        return Encounter(id, dir)
    }

    @Synchronized
    fun addChunk(
        encounter: Encounter,
        kind: ChunkKind,
        seq: Int,
        encryptedFile: File,
        crypto: ChunkCrypto,
    ) {
        val manifest = JSONObject(manifestFile(encounter.dir).readText())
        val chunks = manifest.getJSONArray("chunks")
        val prevHash = if (chunks.length() > 0) {
            chunks.getJSONObject(chunks.length() - 1).getString("sha256")
        } else ""
        chunks.put(
            JSONObject()
                .put("kind", kind.name)
                .put("seq", seq)
                .put("file", encryptedFile.name)
                .put("sha256", crypto.sha256Plain)
                .put("prevSha256", prevHash)
                .put("wrapMode", crypto.wrapMode)
                .putOpt("wrappedDek", crypto.wrappedDek)
                .putOpt("wrapIv", crypto.wrapIv)
                .putOpt("wrappedDekRecovery", crypto.wrappedDekRecovery)
                .put("chunkIv", crypto.chunkIv)
                .put("closedAt", System.currentTimeMillis()),
        )
        manifestFile(encounter.dir).writeText(manifest.toString(2))
        auditLog.append(
            "chunk_closed",
            JSONObject().put("encounterId", encounter.id).put("kind", kind.name).put("seq", seq),
        )
    }

    /** Direito de eliminação (docs/LGPD.md §3 "descartar consulta"). */
    fun discard(encounter: Encounter) {
        encounter.dir.deleteRecursively()
        auditLog.append("encounter_discarded", JSONObject().put("encounterId", encounter.id))
    }

    fun aadFor(encounterId: String, seq: Int): ByteArray = "$encounterId:$seq".toByteArray()

    private fun manifestFile(dir: File) = File(dir, "manifest.json")
}
