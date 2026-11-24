package com.patiocontrol.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A meta só vale quando a operação definiu a dela. O risco aqui não é errar
 * a conta — é avisar sem motivo: alerta que aparece sozinho ensina o time a
 * fechar alerta sem ler.
 */
class MetasTest {

    @Test
    fun `sem meta definida nao ha alerta`() {
        val indicadores = Indicadores(descarga = 240, permanencia = 600)
        assertTrue(ComparadorMetas.estouros(indicadores, Metas()).isEmpty())
        assertTrue(ComparadorMetas.alertas(indicadores, Metas()).isEmpty())
    }

    @Test
    fun `meta zerada conta como meta em branco`() {
        val indicadores = Indicadores(descarga = 90)
        assertTrue(ComparadorMetas.estouros(indicadores, Metas(descarga = 0)).isEmpty())
    }

    @Test
    fun `tempo ainda nao medido nao estoura`() {
        val indicadores = Indicadores(descarga = null)
        assertTrue(ComparadorMetas.estouros(indicadores, Metas(descarga = 60)).isEmpty())
    }

    @Test
    fun `em cima da meta nao estoura`() {
        val indicadores = Indicadores(descarga = 60)
        assertTrue(ComparadorMetas.estouros(indicadores, Metas(descarga = 60)).isEmpty())
    }

    @Test
    fun `acima da meta estoura uma vez por indicador`() {
        val indicadores = Indicadores(descarga = 95, permanencia = 400)
        val estouros = ComparadorMetas.estouros(
            indicadores,
            Metas(descarga = 60, permanencia = 300, esperaAg = 30)
        )
        assertEquals(2, estouros.size)
        assertEquals("Descarga", estouros.first().rotulo)
        assertEquals(95, estouros.first().minutos)
        assertEquals(60, estouros.first().meta)
    }

    @Test
    fun `a frase traz o tempo e a meta em h mm`() {
        val alertas = ComparadorMetas.alertas(
            Indicadores(descarga = 95),
            Metas(descarga = 60)
        )
        assertEquals(listOf("Descarga: 1:35, acima da meta de 1:00"), alertas)
    }

    @Test
    fun `produtivo e piso e nao teto`() {
        val metas = Metas(produtivo = 0.60)

        val abaixo = ComparadorMetas.alertas(Indicadores(produtivo = 0.42), metas)
        assertEquals(listOf("Tempo produtivo: 42%, abaixo da meta de 60%"), abaixo)

        assertTrue(ComparadorMetas.alertas(Indicadores(produtivo = 0.75), metas).isEmpty())
        assertTrue(ComparadorMetas.alertas(Indicadores(produtivo = 0.60), metas).isEmpty())
    }
}
