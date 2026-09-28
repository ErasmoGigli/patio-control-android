package com.patiocontrol.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class ClassificadorSituacaoTest {

    private fun carreta(
        placa: String = "ABC1D23",
        resposta: RespostaAg = RespostaAg.NAO_RESPONDIDO,
        transferida: Instant? = null,
        vararg marcos: Pair<Marco, String>
    ) = Carreta(
        id = "TST-20260101-001",
        data = LocalDate.of(2026, 1, 1),
        placa = placa,
        respostaAg = resposta,
        transferidaEm = transferida,
        horarios = marcos.associate { (m, h) -> m to LocalTime.parse(h) }
    )

    @Test
    fun `sem placa e registro incompleto`() {
        assertEquals(Situacao.REGISTRO_INCOMPLETO, ClassificadorSituacao.classificar(carreta(placa = "")))
    }

    @Test
    fun `so com programado e programada`() {
        assertEquals(
            Situacao.PROGRAMADA,
            ClassificadorSituacao.classificar(carreta(marcos = arrayOf(Marco.PROGRAMADO to "07:00")))
        )
    }

    @Test
    fun `avanca com cada marco ate finalizada`() {
        val esperado = listOf(
            Marco.CHEGADA to Situacao.AGUARDANDO_ENTRADA,
            Marco.ENTRADA to Situacao.AGUARDANDO_DESCARGA,
            Marco.INICIO_DESCARGA to Situacao.EM_DESCARGA,
            Marco.FIM_DESCARGA to Situacao.AGUARDANDO_CARREGAMENTO_AG,
            Marco.INICIO_AG to Situacao.EM_CARREGAMENTO_AG,
            Marco.FIM_AG to Situacao.AGUARDANDO_LIBERACAO,
            Marco.LIBERACAO to Situacao.LIBERADA,
            Marco.SAIDA to Situacao.FINALIZADA
        )

        val acumulados = mutableListOf<Pair<Marco, String>>()
        esperado.forEach { (marco, situacao) ->
            acumulados += marco to "08:00"
            assertEquals(
                "após marcar ${marco.rotulo}",
                situacao,
                ClassificadorSituacao.classificar(carreta(marcos = acumulados.toTypedArray()))
            )
        }
    }

    @Test
    fun `saida com resposta nao vira saida sem AG`() {
        assertEquals(
            Situacao.SAIDA_SEM_AG,
            ClassificadorSituacao.classificar(
                carreta(resposta = RespostaAg.NAO, marcos = arrayOf(Marco.SAIDA to "10:00"))
            )
        )
    }

    @Test
    fun `transferida vence qualquer outro estado`() {
        assertEquals(
            Situacao.TRANSFERIDA,
            ClassificadorSituacao.classificar(
                carreta(transferida = Instant.EPOCH, marcos = arrayOf(Marco.SAIDA to "10:00"))
            )
        )
    }
}
