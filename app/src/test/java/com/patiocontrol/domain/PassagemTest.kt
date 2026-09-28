package com.patiocontrol.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class PassagemTest {

    private val carreta = Carreta(
        id = "YYTN-20260101-001",
        data = LocalDate.of(2026, 1, 1),
        turno = "1º TURNO",
        transportadora = "FROTA PRÓPRIA",
        placa = "ABC1D23",
        origem = "NÃO INFORMADO",
        quantidade = 12.5,
        unidade = "PALETE",
        observacaoCarga = "carga com barra | e aspas \" dentro",
        horarios = mapOf(
            Marco.CHEGADA to LocalTime.of(7, 5),
            Marco.ENTRADA to LocalTime.of(7, 40)
        ),
        respostaAg = RespostaAg.SIM,
        ativosGiro = listOf(
            AtivoGiro(tipo = "LITRINHO (300 ML)", quantidade = 30.0, unidade = "PALETE")
        ),
        avarias = listOf(
            Avaria(item = "PALETE PBR1", quantidade = null, origem = "TRANSPORTE")
        )
    )

    @Test
    fun `ida e volta preserva a carreta inteira`() {
        val texto = Passagem.codificar("YYTN", "1º TURNO", listOf(carreta))
        val lido = Passagem.decodificar(texto)

        assertNotNull(lido)
        assertEquals("YYTN", lido!!.aparelho)
        assertEquals(1, lido.carretas.size)

        val c = lido.carretas.first()
        assertEquals(carreta.id, c.id)
        assertEquals(carreta.data, c.data)
        assertEquals(carreta.placa, c.placa)
        assertEquals(carreta.quantidade, c.quantidade)
        assertEquals(carreta.respostaAg, c.respostaAg)
        assertEquals(LocalTime.of(7, 5), c.horario(Marco.CHEGADA))
        assertEquals(LocalTime.of(7, 40), c.horario(Marco.ENTRADA))
        assertNull(c.horario(Marco.SAIDA))
    }

    @Test
    fun `barra vertical na observacao nao quebra a leitura`() {
        val texto = Passagem.codificar("YYTN", "1º TURNO", listOf(carreta))
        val c = Passagem.decodificar(texto)!!.carretas.first()
        assertEquals("carga com barra | e aspas \" dentro", c.observacaoCarga)
    }

    @Test
    fun `itens filhos voltam ligados a carreta certa`() {
        val outra = carreta.copy(id = "YYTN-20260101-002", ativosGiro = emptyList(), avarias = emptyList())
        val texto = Passagem.codificar("YYTN", "1º TURNO", listOf(carreta, outra))
        val lido = Passagem.decodificar(texto)!!

        assertEquals(1, lido.carretas.first { it.id == carreta.id }.ativosGiro.size)
        assertEquals(1, lido.carretas.first { it.id == carreta.id }.avarias.size)
        assertEquals(0, lido.carretas.first { it.id == outra.id }.ativosGiro.size)
    }

    @Test
    fun `texto que nao e pacote de passagem devolve null`() {
        assertNull(Passagem.decodificar("https://exemplo.com"))
        assertNull(Passagem.decodificar(""))
    }

    @Test
    fun `pacote vazio e valido e nao traz carreta`() {
        val lido = Passagem.decodificar(Passagem.codificar("YYTN", "2º TURNO", emptyList()))
        assertNotNull(lido)
        assertEquals("2º TURNO", lido!!.turno)
        assertEquals(0, lido.carretas.size)
    }
}
