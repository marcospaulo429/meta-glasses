package com.prontuario.glasses.vault

import java.io.File
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONObject

/**
 * Log de auditoria append-only hash-encadeado (spec §5 e docs/LGPD.md §2 "Responsabilização").
 * Cada entrada inclui o hash da anterior; adulteração quebra a cadeia de forma detectável.
 */
class AuditLog(private val file: File) {

    @Synchronized
    fun append(type: String, payload: JSONObject): String {
        val id = UUID.randomUUID().toString()
        val entry = JSONObject()
            .put("id", id)
            .put("ts", System.currentTimeMillis())
            .put("type", type)
            .put("payload", payload)
            .put("prevHash", lastHash())
        entry.put("hash", hashOf(entry))
        file.parentFile?.mkdirs()
        file.appendText(entry.toString() + "\n")
        return id
    }

    @Synchronized
    fun verifyChain(): Boolean {
        if (!file.exists()) return true
        var prev = ""
        file.useLines { lines ->
            for (line in lines) {
                if (line.isBlank()) continue
                val entry = JSONObject(line)
                val expected = entry.getString("hash")
                val clone = JSONObject(line).also { it.remove("hash") }
                if (hashOf(clone) != expected || entry.getString("prevHash") != prev) return false
                prev = expected
            }
        }
        return true
    }

    private fun lastHash(): String {
        if (!file.exists()) return ""
        var last = ""
        file.forEachLine { line -> if (line.isNotBlank()) last = JSONObject(line).getString("hash") }
        return last
    }

    private fun hashOf(entry: JSONObject): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(entry.toString().toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
