package com.patiocontrol.domain

import com.patiocontrol.core.Tempo
import com.patiocontrol.core.Texto
import java.time.LocalDate
import java.time.LocalTime

/**
 * Gera os três CSV que alimentam a planilha de permanência.
 *
 * O formato é ditado pelo Excel em português, não por gosto: separador ponto
 * e vírgula, decimal com vírgula, data dd/MM/aaaa e hora HH:mm. O BOM de
 * UTF-8 é adicionado na hora de gravar o arquivo, não aqui.
 *
 * A ordem das colunas é contrato com a planilha. Mudou aqui, muda lá — é
 * por isso que existe o teste que trava a contagem de colunas.
 */
object CsvExportador {

    const val SEPARADOR = ';'
    const val QUEBRA = "\r\n"

    /** REGISTROS: colunas A..AC da planilha, exatamente nesta ordem. */
    const val COLUNAS_REGISTROS = 29

    /** AG_DETALHE e AVARIAS: colunas A..G. */
    const val COLUNAS_ATIVOS = 7
    const val COLUNAS_AVARIAS = 7

    fun registros(carretas: List<Carreta>): String =
        carretas.joinToString(QUEBRA) { c ->
            linha(
                c.id,
                Tempo.dataBr(c.data),
                c.turno,
                c.transportadora,
                c.placa,
                c.placaCavalo,
                c.motorista,
                c.veiculo,
                c.origem,
                c.destino,
                c.documento,
                c.responsavel,
                c.produto,
                c.notaFiscal,
                Texto.numeroBr(c.quantidade),
                if (c.quantidade != null) c.unidade else "",
                c.observacaoCarga,
                Tempo.hora(c.horario(Marco.PROGRAMADO)),
                Tempo.hora(c.horario(Marco.CHEGADA)),
                Tempo.hora(c.horario(Marco.ENTRADA)),
                Tempo.hora(c.horario(Marco.INICIO_DESCARGA)),
                Tempo.hora(c.horario(Marco.FIM_DESCARGA)),
                Tempo.hora(c.horario(Marco.INICIO_AG)),
                Tempo.hora(c.horario(Marco.FIM_AG)),
                Tempo.hora(c.horario(Marco.LIBERACAO)),
                Tempo.hora(c.horario(Marco.SAIDA)),
                when (c.respostaAg) {
                    RespostaAg.SIM -> "Sim"
                    RespostaAg.NAO -> "Não"
                    RespostaAg.NAO_RESPONDIDO -> ""
                },
                c.justificativaSemAg,
                c.observacaoCarregamento
            )
        }

    /** AG_DETALHE: uma linha por tipo de ativo, nunca dois tipos na mesma. */
    fun ativos(carretas: List<Carreta>): String =
        carretas.flatMap { c ->
            c.ativosGiro.map { ag ->
                linha(
                    c.id,
                    ag.tipo,
                    Texto.numeroBr(ag.quantidade),
                    ag.unidade,
                    ag.documento,
                    ag.destino,
                    ag.observacao
                )
            }
        }.joinToString(QUEBRA)

    /** AVARIAS: a coluna de foto leva o nome do arquivo, não a imagem. */
    fun avarias(carretas: List<Carreta>): String =
        carretas.flatMap { c ->
            c.avarias.map { a ->
                linha(
                    c.id,
                    a.item,
                    Texto.numeroBr(a.quantidade),
                    a.unidade,
                    a.origem,
                    a.observacao,
                    a.foto
                )
            }
        }.joinToString(QUEBRA)

    fun linha(vararg campos: String): String = campos.joinToString(SEPARADOR.toString()) { campo(it) }

    /** Aspas só quando o conteúdo exige; aspa interna dobra, como manda o RFC. */
    fun campo(valor: String): String {
        val precisaAspas = valor.any { it == SEPARADOR || it == '"' || it == '\n' || it == '\r' }
        return if (precisaAspas) "\"" + valor.replace("\"", "\"\"") + "\"" else valor
    }
}

/** Nomes dos arquivos dentro do pacote e o carimbo que os identifica. */
object NomesPacote {

    fun carimbo(data: LocalDate, hora: LocalTime): String =
        "%04d%02d%02d-%02d%02d".format(
            data.year, data.monthValue, data.dayOfMonth, hora.hour, hora.minute
        )

    fun pacote(aparelho: String, carimbo: String) = "patio-$aparelho-$carimbo.zip"
    fun registros(carimbo: String) = "registros-$carimbo.csv"
    fun ativos(carimbo: String) = "ativos-$carimbo.csv"
    fun avarias(carimbo: String) = "avarias-$carimbo.csv"
    fun foto(nome: String) = "fotos/$nome"
    const val LEIA_ME = "leia-me.txt"

    fun nomeDeFoto(carretaId: String, indice: Int, agora: Long): String =
        "AVARIA_${carretaId}_${indice}_${agora.toString(36)}.jpg"
}

/** O bilhete que viaja dentro do pacote, para quem abrir sem contexto. */
object LeiaMe {

    /**
     * @param arquivos os CSV que realmente entraram no pacote, na ordem em
     *   que devem ser lancados. Listar um arquivo que nao esta ali faz quem
     *   abrir procurar o que nao existe e achar que se perdeu no caminho —
     *   por isso a lista vem de fora, e nao de nomes fixos.
     */
    fun texto(
        carimbo: String,
        aparelho: String,
        turno: String,
        data: LocalDate,
        hora: LocalTime,
        carretas: List<Carreta>,
        fotos: Int,
        arquivos: List<String>
    ): String {
        val linhasAg = carretas.sumOf { it.ativosGiro.size }
        val itensAvaria = carretas.sumOf { it.avarias.size }

        val comoLancar = arquivos.mapIndexed { indice, nome ->
            "${indice + 1}. ${nome.padEnd(32)}->  aba ${abaDe(nome, carimbo)}, " +
                "primeira linha vazia da coluna A"
        }

        val sobreFoto =
            if (fotos > 0) listOf(
                "",
                "A coluna Foto do arquivo de avarias traz o nome do arquivo que esta",
                "na pasta fotos/ deste pacote. Guarde o pacote inteiro para manter o vinculo."
            ) else emptyList()

        return (
            listOf(
                "CONTROLE DE PATIO - exportacao ${Tempo.dataBr(data)} ${Tempo.hora(hora)}",
                "Aparelho: $aparelho   Turno: $turno",
                "",
                "Carretas: ${carretas.size}   Linhas de ativo de giro: $linhasAg   " +
                    "Itens de avaria: $itensAvaria   Fotos: $fotos",
                "",
                "COMO LANCAR NA PLANILHA"
            ) + comoLancar + listOf(
                "",
                "Os arquivos usam ponto e virgula como separador e UTF-8 com BOM.",
                "Ao abrir no Excel, use Dados > De Texto/CSV se as colunas nao separarem sozinhas."
            ) + sobreFoto + listOf("")
            ).joinToString("\r\n")
    }

    private fun abaDe(nome: String, carimbo: String): String = when (nome) {
        NomesPacote.registros(carimbo) -> "REGISTROS"
        NomesPacote.ativos(carimbo) -> "AG_DETALHE"
        NomesPacote.avarias(carimbo) -> "AVARIAS"
        else -> "?"
    }
}
