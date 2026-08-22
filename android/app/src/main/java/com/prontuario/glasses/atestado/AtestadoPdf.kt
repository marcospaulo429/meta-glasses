package com.prontuario.glasses.atestado

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PDF do atestado gerado APÓS confirmação do médico. Documento sai do cofre por decisão
 * explícita dele (compartilhar/imprimir); assinatura ICP-Brasil é fase futura — até lá o
 * rodapé exige assinatura física.
 */
object AtestadoPdf {

    fun write(draft: AtestadoDraft, out: File) {
        require(draft.confirmed) { "PDF só após confirmação do médico" }
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create()) // A4 @72dpi
        val canvas = page.canvas

        val title = Paint().apply {
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val body = Paint().apply { textSize = 12f }
        val small = Paint().apply { textSize = 9f }

        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(draft.createdAt))
        var y = 80f
        fun line(text: String, paint: Paint = body, gap: Float = 22f) {
            canvas.drawText(text, 60f, y, paint)
            y += gap
        }

        line("ATESTADO MÉDICO", title, 40f)
        line("Atesto, para os devidos fins, que o(a) paciente")
        line(draft.patientName ?: "________________________________________", body, 30f)
        line(
            "necessita de afastamento de suas atividades por " +
                (draft.days?.let { "$it ${if (it == 1) "dia" else "dias"}" } ?: "____ dias") + ",",
        )
        line("a contar de $dateStr.", body, 30f)
        draft.cid?.let { line("CID: $it (inclusão autorizada expressamente pelo paciente)", body, 30f) }
        y += 40f
        line("________________________________________", body, 18f)
        line(draft.doctorName ?: "Nome do médico", body, 18f)
        line(draft.doctorCrm ?: "CRM", body, 40f)
        line("Documento gerado pela Medware a partir de consulta com", small, 12f)
        line("consentimento registrado; conteúdo revisado e confirmado pelo médico.", small, 12f)
        line("Sem validade até assinatura do médico. Trecho de origem auditável no prontuário.", small, 12f)

        doc.finishPage(page)
        out.outputStream().use { doc.writeTo(it) }
        doc.close()
    }
}
