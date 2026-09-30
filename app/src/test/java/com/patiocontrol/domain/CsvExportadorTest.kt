package com.patiocontrol.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Estes testes são o contrato com a planilha. Se um deles quebrar, a culpa
 * não é do teste: alguém mudou uma coluna e a colagem vai desalinhar.
 *
 * As asserções separam a linha com [separar], e não com um split simples,
 * porque um campo que contém ponto e vírgula sai entre aspas — separar sem
 * respeitar isso partiria o campo ao meio e acusaria uma coluna a mais que
 * não existe. O cenário de teste inclui uma observação com ponto e vírgula
 * exatamente para manter essa regra sob verificação.
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

    /** Separador de CSV que respeita aspas e aspa dobrada, como o Excel. */
    private fun separar(linha: String): List<String> {
        val campos = mutableListOf<String>()
        val atual = StringBuilder()
        var dentroDeAspas = false
        var i = 0
        while (i < linha.length) {
            val c = linha[i]
            when {
                dentroDeAspas && c == '"' && i + 1 < linha.length && linha[i + 1] == '"' -> {
                    atual.append('"')
                    i++
                }

                c == '"' -> dentroDeAspas = !dentroDeAspas
                c == ';' && !dentroDeAspas -> {
                    campos += atual.toString()
                    atual.setLength(0)
                }

                else -> atual.append(c)
            }
            i++
        }
        campos += atual.toString()
        return campos
    }

    private fun registros() = separar(CsvExportador.registros(listOf(carreta)))
    private fun ativos() = separar(CsvExportador.ativos(listOf(carreta)))
    private fun avarias() = separar(CsvExportador.avarias(listOf(carreta)))

    @Test
    fun `registros tem exatamente 29 colunas`() {
        assertEquals(CsvExportador.COLUNAS_REGISTROS, registros().size)
    }

    @Test
    fun `ativos e avarias tem 7 colunas`() {
        assertEquals(CsvExportador.COLUNAS_ATIVOS, ativos().size)
        assertEquals(CsvExportador.COLUNAS_AVARIAS, avarias().size)
    }

    @Test
    fun `data sai em dd-MM-aaaa e hora em HH-mm`() {
        val c = registros()
        assertEquals("01/01/2026", c[1])
        assertEquals("07:05", c[18])
        assertEquals("11:40", c[25])
    }

    @Test
    fun `decimal sai com virgula e inteiro sem casa`() {
        assertEquals("12,5", registros()[14])
        assertEquals("30", ativos()[2])
    }

    @Test
    fun `horario nao marcado sai vazio, nao sai zero`() {
        val c = registros()
        assertEquals("", c[17]) // programado
        assertEquals("", c[19]) // entrada
    }

    @Test
    fun `avaria sem quantidade deixa a coluna vazia`() {
        assertEquals("", avarias()[2])
    }

    @Test
    fun `campo com ponto e virgula sai entre aspas`() {
        assertTrue(
            CsvExportador.avarias(listOf(carreta))
                .contains("\"Palete tombado; sem contagem\"")
        )
    }

    @Test
    fun `campo entre aspas volta inteiro na leitura`() {
        // Prova de que o quoting não perde conteúdo: o ponto e vírgula
        // continua dentro do mesmo campo depois de separar.
        assertEquals("Palete tombado; sem contagem", avarias()[5])
    }

    @Test
    fun `aspa dentro do campo e dobrada`() {
        assertEquals("\"disse \"\"ok\"\"\"", CsvExportador.campo("disse \"ok\""))
    }

    @Test
    fun `a coluna de foto leva o nome do arquivo, nunca a imagem`() {
        assertEquals("AVARIA_YYTN-20260101-001_1_abc.jpg", avarias()[6])
    }

    @Test
    fun `resposta de AG sai como Sim, Nao ou vazio`() {
        fun coluna(r: RespostaAg) =
            separar(CsvExportador.registros(listOf(carreta.copy(respostaAg = r))))[26]

        assertEquals("Sim", coluna(RespostaAg.SIM))
        assertEquals("Não", coluna(RespostaAg.NAO))
        assertEquals("", coluna(RespostaAg.NAO_RESPONDIDO))
    }

    @Test
    fun `varias carretas saem em linhas separadas por CRLF`() {
        val texto = CsvExportador.registros(
            listOf(carreta, carreta.copy(id = "YYTN-20260101-002"))
        )
        assertEquals(2, texto.split(CsvExportador.QUEBRA).size)
    }
}
