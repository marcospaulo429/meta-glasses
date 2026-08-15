package com.prontuario.glasses.encounter

import com.prontuario.glasses.vault.AuditLog
import com.prontuario.glasses.vault.ChunkCrypto
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EncounterRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun repo(): EncounterRepository =
        EncounterRepository(File(tmp.root, "encounters"), AuditLog(File(tmp.root, "audit.jsonl")))

    private fun consent(video: Boolean = false) =
        ConsentRecord(
            patientConsented = true,
            companionPresent = false,
            companionConsented = false,
            securityVideoConsented = video,
        )

    private fun fakeCrypto(mode: String = ChunkCrypto.MODE_RECOVERY_ONLY) = ChunkCrypto(
        wrapMode = mode,
        wrappedDek = if (mode == ChunkCrypto.MODE_LOCAL) "ZGVr" else null,
        wrapIv = if (mode == ChunkCrypto.MODE_LOCAL) "aXY=" else null,
        wrappedDekRecovery = if (mode == ChunkCrypto.MODE_RECOVERY_ONLY) "cmVj" else null,
        chunkIv = "Y2l2",
        sha256Plain = "abc123",
    )

    private fun addChunk(repo: EncounterRepository, enc: Encounter, seq: Int, kind: ChunkKind = ChunkKind.VIDEO) {
        val file = File(enc.dir, "${kind.name.lowercase()}_$seq.enc").apply { writeText("cifrado") }
        repo.addChunk(enc, kind, seq, file, fakeCrypto())
    }

    @Test
    fun `sem consentimento do paciente nao cria consulta`() {
        assertThrows(IllegalArgumentException::class.java) {
            repo().create(consent().let { ConsentRecord(false, false, false, false) })
        }
    }

    @Test
    fun `acompanhante presente sem consentimento bloqueia`() {
        assertThrows(IllegalArgumentException::class.java) {
            repo().create(ConsentRecord(true, true, false, false))
        }
    }

    @Test
    fun `crypto-erasure remove todas as chaves e marca erased`() {
        val repo = repo()
        val enc = repo.create(consent(video = true))
        addChunk(repo, enc, 0)
        addChunk(repo, enc, 1)

        repo.eliminateChunks(enc, ChunkKind.VIDEO)

        val chunks = repo.manifest(enc).getJSONArray("chunks")
        for (i in 0 until chunks.length()) {
            val chunk = chunks.getJSONObject(i)
            assertTrue(chunk.getBoolean("erased"))
            assertFalse(chunk.has("wrappedDek"))
            assertFalse(chunk.has("wrappedDekRecovery"))
        }
        assertTrue(repo.auditLog.verifyChain())
    }

    @Test
    fun `crypto-erasure de um tipo nao afeta o outro`() {
        val repo = repo()
        val enc = repo.create(consent(video = true))
        addChunk(repo, enc, 0, ChunkKind.AUDIO)
        addChunk(repo, enc, 0, ChunkKind.VIDEO)

        repo.eliminateChunks(enc, ChunkKind.VIDEO)

        val chunks = repo.manifest(enc).getJSONArray("chunks")
        val audio = (0 until chunks.length()).map(chunks::getJSONObject).first { it.getString("kind") == "AUDIO" }
        assertFalse(audio.optBoolean("erased"))
        assertTrue(audio.has("wrappedDekRecovery"))
    }

    @Test
    fun `gaps de sequencia sao detectados e documentados`() {
        val repo = repo()
        val enc = repo.create(consent(video = true))
        addChunk(repo, enc, 0)
        addChunk(repo, enc, 1)
        addChunk(repo, enc, 4)

        assertEquals(listOf(2, 3), repo.detectGaps(enc, ChunkKind.VIDEO))
        assertTrue(repo.detectGaps(enc, ChunkKind.AUDIO).isEmpty())
    }

    @Test
    fun `cadeia de custodia liga sha256 do chunk anterior`() {
        val repo = repo()
        val enc = repo.create(consent())
        addChunk(repo, enc, 0, ChunkKind.AUDIO)
        addChunk(repo, enc, 1, ChunkKind.AUDIO)

        val chunks = repo.manifest(enc).getJSONArray("chunks")
        assertEquals("", chunks.getJSONObject(0).getString("prevSha256"))
        assertEquals(
            chunks.getJSONObject(0).getString("sha256"),
            chunks.getJSONObject(1).getString("prevSha256"),
        )
    }

    @Test
    fun `descartar consulta apaga tudo e registra auditoria`() {
        val repo = repo()
        val enc = repo.create(consent())
        addChunk(repo, enc, 0, ChunkKind.AUDIO)
        repo.discard(enc)

        assertFalse(enc.dir.exists())
        assertNull(repo.load(enc.id))
        assertTrue(repo.auditLog.verifyChain())
    }

    @Test
    fun `markReviewed persiste confirmacao do medico`() {
        val repo = repo()
        val enc = repo.create(consent())
        assertFalse(repo.manifest(enc).getBoolean("reviewed"))
        repo.markReviewed(enc)
        assertTrue(repo.manifest(enc).getBoolean("reviewed"))
    }

    @Test
    fun `listEncounterIds retorna consultas persistidas`() {
        val repo = repo()
        val e1 = repo.create(consent())
        val e2 = repo.create(consent())
        assertEquals(setOf(e1.id, e2.id), repo.listEncounterIds().toSet())
    }
}
