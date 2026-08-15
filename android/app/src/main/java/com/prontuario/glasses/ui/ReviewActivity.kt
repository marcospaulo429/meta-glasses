package com.prontuario.glasses.ui

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.prontuario.glasses.encounter.ChunkKind
import com.prontuario.glasses.encounter.Encounter
import com.prontuario.glasses.encounter.EncounterRepository
import com.prontuario.glasses.soap.FactStatus
import com.prontuario.glasses.soap.SoapJson
import com.prontuario.glasses.soap.SoapNote
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
                        addView(eraseVideoButton)
                        addView(discardButton)
                        addView(auditButton)
                        addView(content)
                    },
                )
            },
        )
        render()
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
