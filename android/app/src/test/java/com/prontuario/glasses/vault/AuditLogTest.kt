package com.prontuario.glasses.vault

import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AuditLogTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun newLog(): Pair<AuditLog, File> {
        val file = File(tmp.root, "audit.jsonl")
        return AuditLog(file) to file
    }

    @Test
    fun `cadeia vazia e valida`() {
        val (log, _) = newLog()
        assertTrue(log.verifyChain())
    }

    @Test
    fun `entradas encadeadas verificam`() {
        val (log, _) = newLog()
        log.append("encounter_created", JSONObject().put("encounterId", "e1"))
        log.append("chunk_closed", JSONObject().put("seq", 0))
        log.append("capture_stopped", JSONObject().put("encounterId", "e1"))
        assertTrue(log.verifyChain())
    }

    @Test
    fun `adulteracao quebra a cadeia de forma detectavel`() {
        val (log, file) = newLog()
        log.append("encounter_created", JSONObject().put("encounterId", "e1"))
        log.append("chunk_closed", JSONObject().put("seq", 0))

        val tampered = file.readText().replace("\"seq\":0", "\"seq\":99")
        file.writeText(tampered)

        assertFalse(log.verifyChain())
    }

    @Test
    fun `append retorna id unico para justificativa break-glass`() {
        val (log, _) = newLog()
        val id1 = log.append("break_glass_request", JSONObject().put("motivo", "processo"))
        val id2 = log.append("break_glass_request", JSONObject().put("motivo", "processo"))
        assertTrue(id1.isNotBlank())
        assertTrue(id1 != id2)
    }

    @Test
    fun `remocao de linha intermediaria e detectada`() {
        val (log, file) = newLog()
        log.append("a", JSONObject())
        log.append("b", JSONObject())
        log.append("c", JSONObject())
        val lines = file.readLines().toMutableList()
        lines.removeAt(1)
        file.writeText(lines.joinToString("\n") + "\n")
        assertFalse(log.verifyChain())
        assertEquals(2, file.readLines().count { it.isNotBlank() })
    }
}
