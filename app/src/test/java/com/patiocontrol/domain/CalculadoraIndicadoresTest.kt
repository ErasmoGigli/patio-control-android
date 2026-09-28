package com.patiocontrol.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class CalculadoraIndicadoresTest {

    private fun carreta(vararg marcos: Pair<Marco, String>) = Carreta(
        id = "TST-20260101-001",
        data = LocalDate.of(2026, 1, 1),
        placa = "ABC1D23",
        horarios = marcos.associate { (m, h) -> m to LocalTime.parse(h) }
    )

    @Test
    fun `sem horarios nao inventa numero`() {
        val i = CalculadoraIndicadores.calcular(carreta())
        assertNull(i.descarga)
        assertNull(i.permanencia)
        assertNull(i.operacional)
        assertNull(i.produtivo)
    }

    @Test
    fun `descarga simples em minutos`() {
        val i = CalculadoraIndicadores.calcular(
            carreta(
                Marco.INICIO_DESCARGA to "08:00",
                Marco.FIM_DESCARGA to "09:30"
            )
        )
        assertEquals(90, i.descarga)
    }

    @Test
    fun `atravessa a meia-noite sem virar numero negativo`() {
        val i = CalculadoraIndicadores.calcular(
            carreta(
                Marco.ENTRADA to "23:40",
                Marco.SAIDA to "00:15"
            )
        )
        assertEquals(35, i.permanencia)
    }

    @Test
    fun `atraso e a diferenca entre programado e chegada`() {
        val i = CalculadoraIndicadores.calcular(
            carreta(
                Marco.PROGRAMADO to "07:00",
                Marco.CHEGADA to "07:45"
            )
        )
        assertEquals(45, i.atraso)
    }

    @Test
    fun `chegada adiantada conta como zero de atraso, nunca como 23 horas`() {
        val i = CalculadoraIndicadores.calcular(
            carreta(
                Marco.PROGRAMADO to "08:00",
                Marco.CHEGADA to "07:30"
            )
        )
        assertEquals(0, i.atraso)
    }

    @Test
    fun `operacional soma descarga e carregamento, espera soma as filas`() {
        val i = CalculadoraIndicadores.calcular(
            carreta(
                Marco.ENTRADA to "08:00",
                Marco.INICIO_DESCARGA to "08:30",   // 30 de espera
                Marco.FIM_DESCARGA to "09:30",      // 60 de descarga
                Marco.INICIO_AG to "10:00",         // 30 de espera de AG
                Marco.FIM_AG to "10:40",            // 40 de carregamento
                Marco.LIBERACAO to "11:00",         // 20 de liberação
                Marco.SAIDA to "11:10"              // 10 até a saída
            )
        )
        assertEquals(100, i.operacional)
        assertEquals(90, i.espera)
        assertEquals(190, i.permanencia)
        assertEquals(90, i.naoProdutivo)
    }

    @Test
    fun `produtivo e a fracao do tempo em operacao`() {
        val i = CalculadoraIndicadores.calcular(
            carreta(
                Marco.ENTRADA to "08:00",
                Marco.INICIO_DESCARGA to "08:00",
                Marco.FIM_DESCARGA to "09:00",
                Marco.SAIDA to "10:00"
            )
        )
        assertEquals(0.5, i.produtivo!!, 0.0001)
    }

    @Test
    fun `carreta ainda no patio conta permanencia ate agora`() {
        val i = CalculadoraIndicadores.calcular(
            carreta(Marco.ENTRADA to "08:00"),
            agora = LocalTime.of(9, 20)
        )
        assertEquals(80, i.permanencia)
        assertNull(i.ciclo)
    }

    @Test
    fun `ciclo vai da chegada a saida`() {
        val i = CalculadoraIndicadores.calcular(
            carreta(
                Marco.CHEGADA to "06:00",
                Marco.SAIDA to "10:30"
            )
        )
        assertEquals(270, i.ciclo)
    }
}
