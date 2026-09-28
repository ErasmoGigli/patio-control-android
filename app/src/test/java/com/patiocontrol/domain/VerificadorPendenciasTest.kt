package com.patiocontrol.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class VerificadorPendenciasTest {

    private val completa = Carreta(
        id = "TST-20260101-001",
        data = LocalDate.of(2026, 1, 1),
        placa = "ABC1D23",
        transportadora = "FROTA PRÓPRIA",
        origem = "NÃO INFORMADO",
        respostaAg = RespostaAg.SIM,
        ativosGiro = listOf(
            AtivoGiro(tipo = "LITRINHO (300 ML)", quantidade = 30.0, unidade = "PALETE")
        )
    )

    private fun mensagens(c: Carreta) = VerificadorPendencias.verificar(c).map { it.mensagem }

    @Test
    fun `carreta completa nao tem pendencia`() {
        assertTrue(mensagens(completa).isEmpty())
    }

    @Test
    fun `campos obrigatorios de identificacao`() {
        val m = mensagens(completa.copy(placa = "", transportadora = "", origem = ""))
        assertTrue(m.contains("Placa não informada"))
        assertTrue(m.contains("Transportadora não informada"))
        assertTrue(m.contains("Origem não informada"))
    }

    @Test
    fun `a pergunta de AG precisa ser respondida`() {
        val m = mensagens(completa.copy(respostaAg = RespostaAg.NAO_RESPONDIDO))
        assertTrue(m.contains("Falta responder se carregou AG"))
    }

    @Test
    fun `saida sem AG exige justificativa`() {
        val m = mensagens(completa.copy(respostaAg = RespostaAg.NAO, ativosGiro = emptyList()))
        assertEquals(listOf("Justificativa da saída sem AG é obrigatória"), m)
    }

    @Test
    fun `resposta e lancamento nao podem se contradizer`() {
        val semLancamento = mensagens(completa.copy(ativosGiro = emptyList()))
        assertTrue(semLancamento.contains("Marcada com AG, sem nenhum tipo lançado"))

        val comLancamento = mensagens(
            completa.copy(respostaAg = RespostaAg.NAO, justificativaSemAg = "Falta de AG no pátio")
        )
        assertTrue(comLancamento.contains("Marcada sem AG, mas há AG lançado"))
    }

    @Test
    fun `linha de AG sem quantidade e pendencia`() {
        val m = mensagens(
            completa.copy(
                ativosGiro = listOf(AtivoGiro(tipo = "PALETE PBR1", quantidade = null, unidade = "UNIDADE"))
            )
        )
        assertEquals(listOf("PALETE PBR1 sem quantidade"), m)
    }

    @Test
    fun `avaria sem quantidade nao trava o encerramento`() {
        // Nem sempre dá para medir a avaria na hora; travar aqui faria o
        // conferente inventar um número, que é pior que o campo vazio.
        val m = mensagens(
            completa.copy(
                avarias = listOf(Avaria(item = "PALETE PBR1", quantidade = null, origem = "TRANSPORTE"))
            )
        )
        assertTrue(m.isEmpty())
    }
}

class VerificadorAlertasTest {

    @Test
    fun `horario fora de sequencia vira alerta, nao pendencia`() {
        val c = Carreta(
            id = "TST-20260101-001",
            data = LocalDate.of(2026, 1, 1),
            placa = "ABC1D23",
            horarios = mapOf(
                Marco.CHEGADA to LocalTime.of(8, 0),
                Marco.ENTRADA to LocalTime.of(21, 0)
            )
        )
        assertTrue(VerificadorAlertas.verificar(c).any { it.contains("mais de 12h") })
    }
}
