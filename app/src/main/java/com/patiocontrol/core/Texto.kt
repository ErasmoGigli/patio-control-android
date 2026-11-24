package com.patiocontrol.core

/** Formatação de número no padrão que o Excel em português espera. */
object Texto {

    /** 30.0 -> "30", 12.5 -> "12,5", null -> "" */
    fun numeroBr(valor: Double?): String {
        if (valor == null) return ""
        val inteiro = valor % 1.0 == 0.0
        val texto = if (inteiro) valor.toLong().toString() else valor.toString()
        return texto.replace('.', ',')
    }

    /**
     * Aceita o que o conferente digita de verdade: "12,5", "12.5", "1.280",
     * "1.280,50" e "1280". Devolve null para vazio ou lixo.
     *
     * O ponto é ambíguo em português: em "12.5" ele é decimal, em "1.280" é
     * separador de milhar. Trocar ponto por vírgula sem olhar, como se fazia
     * aqui antes, transformava mil duzentos e oitenta caixas em 1,28 — erro
     * de mil vezes, salvo sem reclamar e exportado para a planilha. A regra
     * agora segue o que a pessoa escreveu:
     *
     * - vírgula e ponto juntos: o último é o decimal, o outro é milhar;
     * - só vírgula: ela é o decimal;
     * - só ponto: é milhar quando os grupos têm três dígitos ("1.280",
     *   "12.500.000"); em qualquer outro caso é decimal ("12.5", "0.75").
     *
     * Sobra um caso insolúvel: quem escrever "1.280" querendo um inteiro e
     * vinte e oito centésimos recebe mil duzentos e oitenta. Num pátio que
     * conta palete e caixa, o milhar é o que acontece.
     */
    fun numeroDe(texto: String?): Double? {
        val limpo = texto?.trim()?.replace(" ", "")?.replace(" ", "") ?: return null
        if (limpo.isEmpty()) return null

        val virgula = limpo.lastIndexOf(',')
        val ponto = limpo.lastIndexOf('.')

        val normalizado = when {
            virgula >= 0 && ponto >= 0 ->
                if (virgula > ponto) limpo.replace(".", "").replace(',', '.')
                else limpo.replace(",", "")
            virgula >= 0 -> limpo.replace(',', '.')
            ponto >= 0 && MILHAR.matches(limpo) -> limpo.replace(".", "")
            else -> limpo
        }
        return normalizado.toDoubleOrNull()
    }

    /** "1.280", "12.500.000": grupos de três dígitos depois do primeiro ponto. */
    private val MILHAR = Regex("""-?\d{1,3}(\.\d{3})+""")

    fun maiusculasSemEspaco(texto: String): String = texto.trim().uppercase()
}
