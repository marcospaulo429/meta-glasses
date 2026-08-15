package com.prontuario.glasses.encounter

import android.content.Context
import com.prontuario.glasses.vault.AuditLog
import com.prontuario.glasses.vault.ChunkCrypto
import com.prontuario.glasses.vault.SecurityVault
import com.prontuario.glasses.vault.WrapPolicy
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
 * hash, hash do anterior — cadeia de custódia, spec §4), documentos cifrados
 * (transcrição/rascunho) e log de auditoria.
 */
class EncounterRepository(
    private val root: File,
    val auditLog: AuditLog,
) {

    constructor(context: Context) : this(
        File(context.filesDir, "encounters"),
        AuditLog(File(context.filesDir, "audit/audit.jsonl")),
    )

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
            .put("documents", JSONArray())
            .put("reviewed", false)
        manifestFile(dir).writeText(manifest.toString(2))
        auditLog.append(
            "encounter_created",
            JSONObject().put("encounterId", id).put("securityVideo", consent.securityVideoConsented),
        )
        return Encounter(id, dir)
    }

    fun listEncounterIds(): List<String> =
        root.listFiles()?.filter { File(it, "manifest.json").exists() }
            ?.sortedBy { it.lastModified() }?.map { it.name } ?: emptyList()

    fun load(encounterId: String): Encounter? {
        val dir = File(root, encounterId)
        return if (manifestFile(dir).exists()) Encounter(encounterId, dir) else null
    }

    fun manifest(encounter: Encounter): JSONObject =
        JSONObject(manifestFile(encounter.dir).readText())

    @Synchronized
    fun addChunk(
        encounter: Encounter,
        kind: ChunkKind,
        seq: Int,
        encryptedFile: File,
        crypto: ChunkCrypto,
    ) {
        val manifest = manifest(encounter)
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

    /** Documento cifrado (transcrição, rascunho SOAP). AAD própria impede troca entre consultas. */
    @Synchronized
    fun saveDocument(
        encounter: Encounter,
        name: String,
        content: ByteArray,
        policy: WrapPolicy = WrapPolicy.Local,
    ) {
        val plain = File(encounter.dir, "$name.tmp").apply { writeBytes(content) }
        val encrypted = File(encounter.dir, "$name.enc")
        val crypto = SecurityVault.encryptFile(plain, encrypted, docAad(encounter.id, name), policy)
        plain.delete()

        val manifest = manifest(encounter)
        val documents = manifest.getJSONArray("documents")
        // Substitui versão anterior do mesmo documento, se existir
        val kept = JSONArray()
        for (i in 0 until documents.length()) {
            if (documents.getJSONObject(i).getString("name") != name) kept.put(documents.getJSONObject(i))
        }
        kept.put(
            JSONObject()
                .put("name", name)
                .put("file", encrypted.name)
                .put("sha256", crypto.sha256Plain)
                .put("wrapMode", crypto.wrapMode)
                .putOpt("wrappedDek", crypto.wrappedDek)
                .putOpt("wrapIv", crypto.wrapIv)
                .putOpt("wrappedDekRecovery", crypto.wrappedDekRecovery)
                .put("chunkIv", crypto.chunkIv)
                .put("savedAt", System.currentTimeMillis()),
        )
        manifest.put("documents", kept)
        manifestFile(encounter.dir).writeText(manifest.toString(2))
        auditLog.append(
            "document_saved",
            JSONObject().put("encounterId", encounter.id).put("name", name),
        )
    }

    /** Leitura para revisão do médico (só documentos LOCAL; registra o acesso). */
    fun readDocument(encounter: Encounter, name: String): ByteArray? {
        val manifest = manifest(encounter)
        val documents = manifest.getJSONArray("documents")
        val entry = (0 until documents.length())
            .map(documents::getJSONObject)
            .firstOrNull { it.getString("name") == name } ?: return null
        val crypto = entry.toChunkCrypto()
        val encrypted = File(encounter.dir, entry.getString("file"))
        if (!encrypted.exists()) return null
        val justification = auditLog.append(
            "document_read",
            JSONObject().put("encounterId", encounter.id).put("name", name).put("reason", "physician_review"),
        )
        val out = File.createTempFile("doc", ".bin", encounter.dir)
        return try {
            SecurityVault.decryptFile(crypto, encrypted, out, docAad(encounter.id, name), justification)
            out.readBytes()
        } finally {
            out.delete()
        }
    }

    /**
     * Crypto-erasure (LGPD art. 18 / docs/LGPD.md §8): remove todas as DEKs embrulhadas
     * dos chunks do tipo dado. O conteúdo cifrado vira lixo irrecuperável sem tocar nele.
     */
    @Synchronized
    fun eliminateChunks(encounter: Encounter, kind: ChunkKind) {
        val manifest = manifest(encounter)
        val chunks = manifest.getJSONArray("chunks")
        var erased = 0
        for (i in 0 until chunks.length()) {
            val chunk = chunks.getJSONObject(i)
            if (chunk.getString("kind") == kind.name && !chunk.optBoolean("erased")) {
                chunk.remove("wrappedDek")
                chunk.remove("wrapIv")
                chunk.remove("wrappedDekRecovery")
                chunk.put("erased", true)
                erased++
            }
        }
        manifestFile(encounter.dir).writeText(manifest.toString(2))
        auditLog.append(
            "chunks_crypto_erased",
            JSONObject().put("encounterId", encounter.id).put("kind", kind.name).put("count", erased),
        )
    }

    /**
     * Gaps de sequência por tipo (spec §4): um buraco documentado vale mais juridicamente
     * do que um vídeo "editado".
     */
    fun detectGaps(encounter: Encounter, kind: ChunkKind): List<Int> {
        val manifest = manifest(encounter)
        val chunks = manifest.getJSONArray("chunks")
        val seqs = (0 until chunks.length())
            .map(chunks::getJSONObject)
            .filter { it.getString("kind") == kind.name }
            .map { it.getInt("seq") }
            .sorted()
        if (seqs.isEmpty()) return emptyList()
        return ((seqs.first()..seqs.last()).toSet() - seqs.toSet()).sorted()
    }

    /** Confirmação do médico: o rascunho vira base do registro oficial (fora do app). */
    fun markReviewed(encounter: Encounter) {
        val manifest = manifest(encounter).put("reviewed", true)
        manifestFile(encounter.dir).writeText(manifest.toString(2))
        auditLog.append("draft_confirmed", JSONObject().put("encounterId", encounter.id))
    }

    /** Direito de eliminação total (docs/LGPD.md §3 "descartar consulta"). */
    fun discard(encounter: Encounter) {
        encounter.dir.deleteRecursively()
        auditLog.append("encounter_discarded", JSONObject().put("encounterId", encounter.id))
    }

    /** Temp órfão de crash nunca é aproveitado (spec §4): apagar no próximo boot do serviço. */
    fun cleanupOrphans(): Int {
        var cleaned = 0
        root.listFiles()?.forEach { dir ->
            dir.listFiles { f -> f.name.endsWith(".tmp") }?.forEach { orphan ->
                if (orphan.delete()) cleaned++
            }
        }
        if (cleaned > 0) auditLog.append("orphans_cleaned", JSONObject().put("count", cleaned))
        return cleaned
    }

    fun aadFor(encounterId: String, seq: Int): ByteArray = "$encounterId:$seq".toByteArray()

    private fun docAad(encounterId: String, name: String): ByteArray =
        "$encounterId:doc:$name".toByteArray()

    private fun manifestFile(dir: File) = File(dir, "manifest.json")

    private fun JSONObject.toChunkCrypto() = ChunkCrypto(
        wrapMode = getString("wrapMode"),
        wrappedDek = optString("wrappedDek").ifEmpty { null },
        wrapIv = optString("wrapIv").ifEmpty { null },
        wrappedDekRecovery = optString("wrappedDekRecovery").ifEmpty { null },
        chunkIv = getString("chunkIv"),
        sha256Plain = getString("sha256"),
    )
}
