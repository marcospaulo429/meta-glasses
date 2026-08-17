package com.prontuario.glasses.ui

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.prontuario.glasses.BuildConfig
import com.prontuario.glasses.atestado.AtestadoJson
import com.prontuario.glasses.atestado.AtestadoPdf
import com.prontuario.glasses.encounter.ChunkKind
import com.prontuario.glasses.encounter.Encounter
import com.prontuario.glasses.encounter.EncounterRepository
import com.prontuario.glasses.soap.FactStatus
import com.prontuario.glasses.soap.SoapJson
import com.prontuario.glasses.soap.SoapNote
import com.prontuario.glasses.vault.SecurityVault
import java.io.File
import org.json.JSONObject

/**
 * Revisão humana obrigatória (MEMORY.md §4): o rascunho só vira base do registro oficial
 * após confirmação do médico. Edição campo a campo entra na Fase B.
 */
class ReviewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ENCOUNTER_ID = "encounter_id"
    }

    private lateinit var repository: EncounterRepository
    private var encounter: Encounter? = null
    private lateinit var content: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = EncounterRepository(this)

        val encounterId = intent.getStringExtra(EXTRA_ENCOUNTER_ID)
            ?: repository.listEncounterIds().lastOrNull()
        encounter = encounterId?.let(repository::load)

        content = TextView(this).apply { textSize = 14f }
        val confirmButton = Button(this).apply {
            text = "Confirmar rascunho"
            setOnClickListener {
                encounter?.let {
                    repository.markReviewed(it)
                    content.append("\n✅ Rascunho confirmado pelo médico (auditado).")
                    isEnabled = false
                }
            }
        }
        val discardButton = Button(this).apply {
            text = "Descartar consulta (apagar tudo)"
            setOnClickListener {
                encounter?.let {
                    repository.discard(it)
                    content.text = "Consulta descartada — todos os dados apagados (LGPD art. 18)."
                    encounter = null
                }
            }
        }
        val eraseVideoButton = Button(this).apply {
            text = "Eliminar vídeo (crypto-erasure)"
            setOnClickListener {
                encounter?.let {
                    repository.eliminateChunks(it, ChunkKind.VIDEO)
                    content.append("\n🔒 Chaves do vídeo destruídas — conteúdo irrecuperável.")
                }
            }
        }
        val auditButton = Button(this).apply {
            text = "Verificar cadeia de auditoria"
            setOnClickListener {
                val ok = repository.auditLog.verifyChain()
                content.append(if (ok) "\n✅ Cadeia de auditoria íntegra." else "\n⛔ CADEIA VIOLADA!")
            }
        }
        val atestadoButton = Button(this).apply {
            text = "Confirmar atestado e gerar PDF"
            setOnClickListener { confirmAtestado() }
        }

        val density = resources.displayMetrics.density
        val pad = (16 * density).toInt()
        setContentView(
            ScrollView(this).apply {
                addView(
                    LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(pad, pad, pad, pad)
                        addView(TextView(context).apply { text = "Revisão do rascunho"; textSize = 20f })
                        addView(confirmButton)
                        addView(atestadoButton)
                        addView(eraseVideoButton)
                        addView(discardButton)
                        addView(auditButton)
                        addView(content)
                    },
                )
            },
        )
        render()

        // HARNESS (debug): adb shell am start ... --ez auto_confirm_atestado true
        if (BuildConfig.DEBUG && intent.getBooleanExtra("auto_confirm_atestado", false)) {
            confirmAtestado()
        }
    }

    /** Ato do médico: confirma o rascunho do atestado e materializa o PDF (auditado). */
    private fun confirmAtestado() {
        val enc = encounter ?: return
        val bytes = repository.readDocument(enc, "atestado_draft")
        if (bytes == null) {
            content.append("\n(sem rascunho de atestado nesta consulta)")
            return
        }
        val draft = AtestadoJson.fromJson(JSONObject(String(bytes))).copy(confirmed = true)
        repository.saveDocument(enc, "atestado_draft", AtestadoJson.toJson(draft).toString(2).toByteArray())
        val pdf = File(enc.dir, "atestado.pdf")
        AtestadoPdf.write(draft, pdf)
        repository.auditLog.append(
            "atestado_emitido",
            JSONObject()
                .put("encounterId", enc.id)
                .putOpt("days", draft.days)
                .put("cidIncluido", draft.cid != null)
                .put("pdfSha256", SecurityVault.sha256(pdf)),
        )
        content.append("\n📄 Atestado confirmado — PDF em ${pdf.absolutePath}")
    }

    private fun render() {
        val enc = encounter ?: run {
            content.text = "Nenhuma consulta encontrada."
            return
        }
        val draftBytes = repository.readDocument(enc, "soap_draft")
        if (draftBytes == null) {
            content.text = "Consulta ${enc.id.take(8)}: sem rascunho (ASR indisponível ou consulta vazia)."
            return
        }
        val draftJson = JSONObject(String(draftBytes))
        val note = SoapJson.noteFromJson(draftJson)
        val gaps = ChunkKind.entries.associateWith { repository.detectGaps(enc, it) }

        content.text = buildString {
            appendLine("Consulta: ${enc.id.take(8)}…")
            draftJson.optJSONObject("validation")?.let {
                appendLine(
                    "Validação de proveniência: " +
                        if (it.getBoolean("passes")) "✅ todos os fatos suportados"
                        else "⚠️ taxa não-suportada: ${"%.1f".format(it.getDouble("unsupportedStatementRate") * 100)}%",
                )
            }
            gaps.filterValues { it.isNotEmpty() }.forEach { (kind, missing) ->
                appendLine("⚠️ Gaps de ${kind.name}: sequências $missing (documentado no manifesto)")
            }
            repository.readDocument(enc, "atestado_draft")?.let { bytes ->
                val atestado = AtestadoJson.fromJson(JSONObject(String(bytes)))
                appendLine()
                appendLine("📋 ATESTADO (rascunho — pendente de confirmação)")
                appendLine("  Paciente: ${atestado.patientName ?: "não informado"}")
                appendLine("  Dias: ${atestado.days?.toString() ?: "❗ não informado — preencher"}")
                appendLine("  CID: ${atestado.cid ?: "não incluso"}")
                atestado.provenance?.let { appendLine("  Origem: [${it.startMs / 1000}s–${it.endMs / 1000}s] “${atestado.spokenText.take(60)}…”") }
            }
            appendLine()
            appendSection("S — Subjetivo", note, SoapNote::subjective)
            appendSection("O — Objetivo", note, SoapNote::objective)
            appendSection("A — Avaliação", note, SoapNote::assessment)
            appendSection("P — Plano", note, SoapNote::plan)
        }
    }

    private fun StringBuilder.appendSection(
        title: String,
        note: SoapNote,
        selector: (SoapNote) -> List<com.prontuario.glasses.soap.Fact>,
    ) {
        appendLine(title)
        val facts = selector(note)
        if (facts.isEmpty()) {
            appendLine("  (não informado)")
        } else {
            facts.forEach { fact ->
                val marker = if (fact.status == FactStatus.UNCERTAIN) "❓" else "•"
                val prov = fact.provenance?.let { " [${it.startMs / 1000}s–${it.endMs / 1000}s]" } ?: ""
                appendLine("  $marker ${fact.text}$prov")
            }
        }
        appendLine()
    }
}
