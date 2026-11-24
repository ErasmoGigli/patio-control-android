package com.patiocontrol.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * O campo de quantidade é digitado às pressas, no pátio, por gente que
 * escreve milhar com ponto. O que entra aqui vai inteiro para a planilha,
 * então cada forma de escrever precisa cair no número que a pessoa quis.
 */
class TextoTest {

    @Test
    fun `inteiro simples`() {
        assertEquals(1280.0, Texto.numeroDe("1280"))
    }

    @Test
    fun `milhar com ponto nao vira decimal`() {
        assertEquals(1280.0, Texto.numeroDe("1.280"))
        assertEquals(12500000.0, Texto.numeroDe("12.500.000"))
    }

    @Test
    fun `decimal com virgula`() {
        assertEquals(12.5, Texto.numeroDe("12,5"))
        assertEquals(1.28, Texto.numeroDe("1,280"))
    }

    @Test
    fun `decimal com ponto continua valendo`() {
        assertEquals(12.5, Texto.numeroDe("12.5"))
        assertEquals(0.75, Texto.numeroDe("0.75"))
    }

    @Test
    fun `milhar e decimal juntos`() {
        assertEquals(1280.5, Texto.numeroDe("1.280,50"))
        assertEquals(1280.5, Texto.numeroDe("1,280.50"))
    }

    @Test
    fun `espaco e espaco fino nao atrapalham`() {
        assertEquals(1280.0, Texto.numeroDe(" 1 280 "))
        assertEquals(1280.0, Texto.numeroDe("1 280"))
    }

    @Test
    fun `vazio e lixo viram nulo`() {
        assertNull(Texto.numeroDe(null))
        assertNull(Texto.numeroDe(""))
        assertNull(Texto.numeroDe("   "))
        assertNull(Texto.numeroDe("doze"))
        assertNull(Texto.numeroDe("1.2.3,4,5"))
    }

    @Test
    fun `ida e volta pela formatacao do app`() {
        listOf(30.0, 1280.0, 12.5, 0.75).forEach { valor ->
            assertEquals(valor, Texto.numeroDe(Texto.numeroBr(valor)))
        }
    }
}
