package com.patiocontrol.domain

import com.patiocontrol.core.Tempo
import java.time.LocalTime

/**
 * Deriva os catorze indicadores a partir dos horários da carreta.
 *
 * É uma função pura de propósito: recebe a carreta e o "agora", devolve os
 * números e não toca em banco, tela nem relógio do sistema. É o que permite
 * testar a virada de meia-noite e a carreta em andamento sem emulador.
 */
object CalculadoraIndicadores {

    fun calcular(carreta: Carreta, agora: LocalTime? = null): Indicadores {
        val h = carreta.horarios

        val descarga = Tempo.diferenca(h[Marco.INICIO_DESCARGA], h[Marco.FIM_DESCARGA])
        val carregamentoAg = Tempo.diferenca(h[Marco.INICIO_AG], h[Marco.FIM_AG])

        val esperaDescarga = Tempo.diferenca(h[Marco.ENTRADA], h[Marco.INICIO_DESCARGA])
        val esperaAg = Tempo.diferenca(h[Marco.FIM_DESCARGA], h[Marco.INICIO_AG])
        val liberacao = Tempo.diferenca(h[Marco.FIM_AG], h[Marco.LIBERACAO])
        val liberacaoSaida = Tempo.diferenca(h[Marco.LIBERACAO], h[Marco.SAIDA])

        // Atraso só faz sentido quando a chegada vem depois do programado.
        // Acima de 12h é chegada adiantada lida ao contrário pelo módulo 24h,
        // e adiantamento não é atraso negativo: é zero.
        val atraso = Tempo.diferenca(h[Marco.PROGRAMADO], h[Marco.CHEGADA])
            ?.let { if (it > Tempo.LIMITE_SUSPEITO) 0 else it }

        // Permanência conta da entrada até a saída; com a carreta ainda no
        // pátio, conta até agora, para o cartão da tela subir sozinho.
        val permanencia = h[Marco.ENTRADA]?.let { entrada ->
            Tempo.diferenca(entrada, h[Marco.SAIDA] ?: agora)
        }

        val operacional = somaPresentes(descarga, carregamentoAg)
        val espera = somaPresentes(esperaDescarga, esperaAg, liberacao, liberacaoSaida)

        val naoProdutivo = permanencia?.let { maxOf(0, it - (operacional ?: 0)) }
        val produtivo = permanencia
            ?.takeIf { it > 0 }
            ?.let { (operacional ?: 0).toDouble() / it.toDouble() }

        return Indicadores(
            atraso = atraso,
            chegadaEntrada = Tempo.diferenca(h[Marco.CHEGADA], h[Marco.ENTRADA]),
            esperaDescarga = esperaDescarga,
            descarga = descarga,
            esperaAg = esperaAg,
            carregamentoAg = carregamentoAg,
            liberacao = liberacao,
            liberacaoSaida = liberacaoSaida,
            permanencia = permanencia,
            ciclo = Tempo.diferenca(h[Marco.CHEGADA], h[Marco.SAIDA]),
            operacional = operacional,
            espera = espera,
            naoProdutivo = naoProdutivo,
            produtivo = produtivo
        )
    }

    /** Soma o que existe; null quando nenhuma parcela foi marcada ainda. */
    private fun somaPresentes(vararg valores: Int?): Int? {
        val presentes = valores.filterNotNull()
        return if (presentes.isEmpty()) null else presentes.sum()
    }
}

/**
 * Classifica a carreta pela marca mais avançada que ela já tem. A ordem dos
 * testes é do fim para o começo, então acrescentar um marco no meio não
 * reescreve a regra inteira.
 */
object ClassificadorSituacao {

    fun classificar(carreta: Carreta): Situacao {
        val h = carreta.horarios
        return when {
            carreta.transferidaEm != null -> Situacao.TRANSFERIDA
            carreta.placa.isBlank() -> Situacao.REGISTRO_INCOMPLETO
            h[Marco.SAIDA] != null && carreta.respostaAg == RespostaAg.NAO -> Situacao.SAIDA_SEM_AG
            h[Marco.SAIDA] != null -> Situacao.FINALIZADA
            h[Marco.LIBERACAO] != null -> Situacao.LIBERADA
            h[Marco.FIM_AG] != null -> Situacao.AGUARDANDO_LIBERACAO
            h[Marco.INICIO_AG] != null -> Situacao.EM_CARREGAMENTO_AG
            h[Marco.FIM_DESCARGA] != null -> Situacao.AGUARDANDO_CARREGAMENTO_AG
            h[Marco.INICIO_DESCARGA] != null -> Situacao.EM_DESCARGA
            h[Marco.ENTRADA] != null -> Situacao.AGUARDANDO_DESCARGA
            h[Marco.CHEGADA] != null -> Situacao.AGUARDANDO_ENTRADA
            h[Marco.PROGRAMADO] != null -> Situacao.PROGRAMADA
            else -> Situacao.REGISTRO_INCOMPLETO
        }
    }
}

/**
 * O que impede o encerramento. A regra existe para que a linha não chegue
 * incompleta na planilha: corrigir no pátio custa segundos, corrigir depois
 * custa uma investigação.
 */
object VerificadorPendencias {

    fun verificar(carreta: Carreta): List<Pendencia> {
        val p = mutableListOf<Pendencia>()

        if (carreta.placa.isBlank()) p += Pendencia("Placa não informada")
        if (carreta.transportadora.isBlank()) p += Pendencia("Transportadora não informada")
        if (carreta.origem.isBlank()) p += Pendencia("Origem não informada")

        when (carreta.respostaAg) {
            RespostaAg.NAO_RESPONDIDO ->
                p += Pendencia("Falta responder se carregou AG")

            RespostaAg.NAO -> {
                if (carreta.justificativaSemAg.isBlank()) {
                    p += Pendencia("Justificativa da saída sem AG é obrigatória")
                }
                if (carreta.ativosGiro.isNotEmpty()) {
                    p += Pendencia("Marcada sem AG, mas há AG lançado")
                }
            }

            RespostaAg.SIM -> {
                if (carreta.ativosGiro.isEmpty()) {
                    p += Pendencia("Marcada com AG, sem nenhum tipo lançado")
                }
            }
        }

        carreta.ativosGiro.forEachIndexed { indice, ag ->
            when {
                ag.tipo.isBlank() -> p += Pendencia("Linha de AG ${indice + 1} sem tipo")
                ag.quantidade == null || ag.quantidade <= 0.0 ->
                    p += Pendencia("${ag.tipo} sem quantidade")
            }
        }

        return p
    }
}

/**
 * Avisos que não travam nada, mas que o conferente precisa ver antes de
 * encerrar — horário fora de sequência é o caso clássico, porque o número
 * fica plausível na tela e absurdo no indicador.
 */
object VerificadorAlertas {

    fun verificar(carreta: Carreta): List<String> {
        val alertas = mutableListOf<String>()
        val h = carreta.horarios

        Marco.SEQUENCIA.windowed(2).forEach { (anterior, seguinte) ->
            if (Tempo.foraDeSequencia(h[anterior], h[seguinte])) {
                alertas += "${anterior.rotulo} e ${seguinte.rotulo} com mais de 12h entre si"
            }
        }

        if (carreta.temAvaria) {
            alertas += "Carga com avaria registrada (${carreta.avarias.size} item(ns))"
        }

        return alertas
    }
}
