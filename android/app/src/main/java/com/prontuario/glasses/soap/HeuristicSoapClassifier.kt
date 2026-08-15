package com.prontuario.glasses.soap

import java.text.Normalizer

/**
 * Classificador SOAP por regras (placeholder do LLM até o benchmark IA-04).
 * Vantagem sobre LLM no MVP: zero alucinação por construção — só reorganiza fatos.
 * A avaliação (A) NUNCA é inferida: só entra se explicitamente enunciada pelo médico.
 */
object HeuristicSoapClassifier {

    private fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")

    // Ordem de teste: P → A → O → S (as marcas de plano/avaliação são mais específicas)
    private val PLAN = listOf(
        "prescrevo", "receito", "vou prescrever", "solicito", "vou pedir", "encaminho",
        "retorno em", "retornar em", "orientacao", "oriento", "manter o uso", "suspender",
        "iniciar", "agendar", "repouso",
    )
    private val ASSESSMENT = listOf(
        "diagnostico", "hipotese diagnostica", "quadro compativel", "avaliacao",
        "impressao clinica", "suspeita de", "compativel com", "concluo",
    )
    private val OBJECTIVE = listOf(
        "ao exame", "exame fisico", "pressao arterial", "pressao de", "frequencia cardiaca",
        "temperatura", "ausculta", "palpacao", "saturacao", "batimentos", "abdome",
        "imagem registrada", "lesao apresenta", "edema", "mucosas",
    )
    private val SUBJECTIVE = listOf(
        "relata", "refere", "queixa", "sente", "sinto", "dor", "desde", "ha cerca de",
        "historico", "antecedente", "alergia", "nega", "informa", "comecou",
    )

    fun classify(encounterId: String, facts: List<Fact>): SoapNote {
        val s = mutableListOf<Fact>()
        val o = mutableListOf<Fact>()
        val a = mutableListOf<Fact>()
        val p = mutableListOf<Fact>()
        for (fact in facts) {
            val text = normalize(fact.text)
            when {
                PLAN.any(text::contains) -> p.add(fact)
                ASSESSMENT.any(text::contains) -> a.add(fact)
                OBJECTIVE.any(text::contains) -> o.add(fact)
                SUBJECTIVE.any(text::contains) -> s.add(fact)
                // Sem marcador claro → S com status UNCERTAIN (nunca inventar seção)
                else -> s.add(fact.copy(status = FactStatus.UNCERTAIN))
            }
        }
        return SoapNote(
            encounterId = encounterId,
            subjective = s,
            objective = o,
            assessment = a,
            plan = p,
        )
    }
}
