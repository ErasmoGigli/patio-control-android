package com.patiocontrol.core

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Aritmética de horário do pátio.
 *
 * A operação atravessa a meia-noite todo dia: uma carreta que chega 23:40 e
 * entra 00:15 gastou 35 minutos, não menos 1405. Toda diferença aqui é
 * calculada em módulo 24h, que é o mesmo que a planilha faz com MOD().
 */
object Tempo {

    private val FORMATO_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val FORMATO_DATA_BR: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    const val MINUTOS_DO_DIA = 24 * 60

    /** Limite acima do qual a diferença deixa de ser plausível e vira suspeita
     *  de horário digitado fora de sequência. Doze horas é a mesma régua da
     *  planilha. */
    const val LIMITE_SUSPEITO = 12 * 60

    /**
     * Diferença de [inicio] até [fim] em minutos, tratando a virada do dia.
     * Devolve null se algum dos dois não foi marcado.
     */
    fun diferenca(inicio: LocalTime?, fim: LocalTime?): Int? {
        if (inicio == null || fim == null) return null
        val a = inicio.hour * 60 + inicio.minute
        val b = fim.hour * 60 + fim.minute
        val d = b - a
        return if (d < 0) d + MINUTOS_DO_DIA else d
    }

    /** true quando a diferença passa de 12h, o que quase sempre significa
     *  horário lançado fora de ordem em vez de uma permanência real. */
    fun foraDeSequencia(inicio: LocalTime?, fim: LocalTime?): Boolean {
        val d = diferenca(inicio, fim) ?: return false
        return d > LIMITE_SUSPEITO
    }

    /** Minutos em h:mm, o formato que o operador lê no cartão da carreta. */
    fun hm(minutos: Int?): String {
        if (minutos == null) return "--:--"
        val negativo = minutos < 0
        val m = kotlin.math.abs(minutos)
        return buildString {
            if (negativo) append('-')
            append(m / 60)
            append(':')
            append((m % 60).toString().padStart(2, '0'))
        }
    }

    fun hora(t: LocalTime?): String = t?.format(FORMATO_HORA) ?: ""

    fun dataBr(d: LocalDate?): String = d?.format(FORMATO_DATA_BR) ?: ""

    /**
     * Lê uma hora digitada. Aceita "7:30" além de "07:30": exigir o zero à
     * esquerda só serviria para o campo recusar o que a pessoa quis dizer.
     */
    fun horaDe(texto: String?): LocalTime? {
        val limpo = texto?.trim()?.replace('.', ':') ?: return null
        if (limpo.isEmpty()) return null
        val partes = limpo.split(':')
        if (partes.size < 2) return null
        val h = partes[0].toIntOrNull() ?: return null
        val m = partes[1].toIntOrNull() ?: return null
        return runCatching { LocalTime.of(h, m) }.getOrNull()
    }

    fun percentual(fracao: Double?): String =
        if (fracao == null) "--" else "${Math.round(fracao * 100)}%"
}
