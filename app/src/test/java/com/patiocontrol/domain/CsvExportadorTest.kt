package com.patiocontrol.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Estes testes são o contrato com a planilha. Se um deles quebrar, a culpa
 * não é do teste: alguém mudou uma coluna e a colagem vai desalinhar.
 */
class CsvExportadorTest {

    private val carreta = Carreta(
        id = "YYTN-20260101-001",
        data = LocalDate.of(2026, 1, 1),
        turno = "1º TURNO",
        transportadora = "FROTA PRÓPRIA",
        placa = "ABC1D23",
        origem = "NÃO INFORMADO",
        quantidade = 12.5,
        unidade = "PALETE",
        horarios = mapOf(
            Marco.CHEGADA to LocalTime.of(7, 5),
            Marco.SAIDA to LocalTime.of(11, 40)
        ),
        respostaAg = RespostaAg.SIM,
        ativosGiro = listOf(
            AtivoGiro(tipo = "LITRINHO (300 ML)", quantidade = 30.0, unidade = "PALETE")
        ),
        avarias = listOf(
            Avaria(
                item = "PALETE PBR1",
                quantidade = null,
                origem = "TRANSPORTE",
                observacao = "Palete tombado; sem contagem",
                foto = "AVARIA_YYTN-20260101-001_1_abc.jpg"
            )
        )
    )

    @Test
    fun `registros tem exatamente 29 colunas`() {
        val colunas = CsvExportador.registros(listOf(carreta)).split(';')
        assertEquals(CsvExportador.COLUNAS_REGISTROS, colunas.size)
    }

    @Test
    fun `ativos e avarias tem 7 colunas`() {
        assertEquals(CsvExportador.COLUNAS_ATIVOS, CsvExportador.ativos(listOf(carreta)).split(';').size)
        assertEquals(CsvExportador.COLUNAS_AVARIAS, CsvExportador.avarias(listOf(carreta)).split(';').size)
    }

    @Test
    fun `data sai em dd-MM-aaaa e hora em HH-mm`() {
        val colunas = CsvExportador.registros(listOf(carreta)).split(';')
        assertEquals("01/01/2026", colunas[1])
        assertEquals("07:05", colunas[18])
        assertEquals("11:40", colunas[25])
    }

    @Test
    fun `decimal sai com virgula e inteiro sem casa`() {
        val colunas = CsvExportador.registros(listOf(carreta)).split(';')
        assertEquals("12,5", colunas[14])
        assertEquals("30", CsvExportador.ativos(listOf(carreta)).split(';')[2])
    }

    @Test
    fun `horario nao marcado sai vazio, nao sai zero`() {
        val colunas = CsvExportador.registros(listOf(carreta)).split(';')
        assertEquals("", colunas[17]) // programado
        assertEquals("", colunas[19]) // entrada
    }

    @Test
    fun `avaria sem quantidade deixa a coluna vazia`() {
        assertEquals("", CsvExportador.avarias(listOf(carreta)).split(';')[2])
    }

    @Test
    fun `campo com ponto e virgula sai entre aspas`() {
        val linha = CsvExportador.avarias(listOf(carreta))
        assertTrue(linha.contains("\"Palete tombado; sem contagem\""))
    }

    @Test
    fun `aspa dentro do campo e dobrada`() {
        assertEquals("\"disse \"\"ok\"\"\"", CsvExportador.campo("disse \"ok\""))
    }

    @Test
    fun `a coluna de foto leva o nome do arquivo, nunca a imagem`() {
        val foto = CsvExportador.avarias(listOf(carreta)).split(';')[6]
        assertEquals("AVARIA_YYTN-20260101-001_1_abc.jpg", foto)
    }

    @Test
    fun `resposta de AG sai como Sim, Nao ou vazio`() {
        fun coluna(r: RespostaAg) =
            CsvExportador.registros(listOf(carreta.copy(respostaAg = r))).split(';')[26]

        assertEquals("Sim", coluna(RespostaAg.SIM))
        assertEquals("Não", coluna(RespostaAg.NAO))
        assertEquals("", coluna(RespostaAg.NAO_RESPONDIDO))
    }

    @Test
    fun `varias carretas saem em linhas separadas por CRLF`() {
        val texto = CsvExportador.registros(listOf(carreta, carreta.copy(id = "YYTN-20260101-002")))
        assertEquals(2, texto.split(CsvExportador.QUEBRA).size)
    }
}
