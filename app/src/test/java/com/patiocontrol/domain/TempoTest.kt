package com.patiocontrol.domain

import com.patiocontrol.core.Tempo
import com.patiocontrol.core.Texto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class TempoTest {

    @Test
    fun `diferenca comum`() {
        assertEquals(90, Tempo.diferenca(LocalTime.of(8, 0), LocalTime.of(9, 30)))
    }

    @Test
    fun `diferenca atravessando a meia-noite`() {
        assertEquals(35, Tempo.diferenca(LocalTime.of(23, 40), LocalTime.of(0, 15)))
    }

    @Test
    fun `sem um dos horarios nao ha diferenca`() {
        assertNull(Tempo.diferenca(null, LocalTime.of(9, 0)))
        assertNull(Tempo.diferenca(LocalTime.of(9, 0), null))
    }

    @Test
    fun `fora de sequencia so acima de doze horas`() {
        assertFalse(Tempo.foraDeSequencia(LocalTime.of(8, 0), LocalTime.of(19, 0)))
        assertTrue(Tempo.foraDeSequencia(LocalTime.of(8, 0), LocalTime.of(21, 0)))
    }

    @Test
    fun `formato h mm`() {
        assertEquals("1:30", Tempo.hm(90))
        assertEquals("0:05", Tempo.hm(5))
        assertEquals("25:00", Tempo.hm(1500))
        assertEquals("--:--", Tempo.hm(null))
    }

    @Test
    fun `numero no padrao brasileiro`() {
        assertEquals("30", Texto.numeroBr(30.0))
        assertEquals("12,5", Texto.numeroBr(12.5))
        assertEquals("", Texto.numeroBr(null))
    }

    @Test
    fun `le numero com virgula ou ponto`() {
        assertEquals(12.5, Texto.numeroDe("12,5")!!, 0.0001)
        assertEquals(12.5, Texto.numeroDe("12.5")!!, 0.0001)
        assertNull(Texto.numeroDe(""))
        assertNull(Texto.numeroDe("abc"))
    }
}
