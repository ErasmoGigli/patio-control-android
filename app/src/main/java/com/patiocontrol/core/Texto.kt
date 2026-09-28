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

    /** Aceita "12,5" e "12.5"; devolve null para vazio ou lixo. */
    fun numeroDe(texto: String?): Double? {
        val limpo = texto?.trim()?.replace(',', '.') ?: return null
        if (limpo.isEmpty()) return null
        return limpo.toDoubleOrNull()
    }

    fun maiusculasSemEspaco(texto: String): String = texto.trim().uppercase()
}
