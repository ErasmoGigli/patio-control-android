package com.patiocontrol.domain

import com.patiocontrol.core.Texto
import java.time.LocalDate
import java.time.LocalTime

/**
 * Formato de passagem de turno entre dois aparelhos.
 *
 * Sem servidor, a transferência acontece por QR code na tela de um celular
 * lido pela câmera do outro, ou por arquivo. Os dois caminhos carregam este
 * mesmo texto, e a capacidade do QR é o que manda: por isso é um formato de
 * linhas com separador de um caractere, e não JSON.
 *
 *   PATIO/1
 *   A|aparelho|turno
 *   C|<campos da carreta>
 *   G|<campos do ativo de giro>
 *   V|<campos da avaria>
 *
 * O separador dentro do campo é escapado: "\|" para a barra, "\\" para a
 * contrabarra e "\n" para quebra de linha. Sem isso, uma observação com
 * barra vertical quebraria a leitura no outro aparelho.
 */
object Passagem {

    const val CABECALHO = "PATIO/1"
    private const val SEP = '|'

    fun codificar(aparelho: String, turno: String, carretas: List<Carreta>): String {
        val linhas = mutableListOf(CABECALHO, junta("A", aparelho, turno))

        carretas.forEach { c ->
            linhas += junta(
                "C",
                c.id,
                c.data.toString(),
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
                c.unidade,
                c.observacaoCarga,
                *Marco.entries.map { hora(c.horario(it)) }.toTypedArray(),
                c.respostaAg.name,
                c.justificativaSemAg,
                c.observacaoCarregamento
            )
            c.ativosGiro.forEach { ag ->
                linhas += junta(
                    "G", c.id, ag.tipo, Texto.numeroBr(ag.quantidade),
                    ag.unidade, ag.documento, ag.destino, ag.observacao
                )
            }
            c.avarias.forEach { av ->
                linhas += junta(
                    "V", c.id, av.item, Texto.numeroBr(av.quantidade),
                    av.unidade, av.origem, av.observacao, av.foto
                )
            }
        }

        return linhas.joinToString("\n")
    }

    data class Conteudo(
        val aparelho: String = "",
        val turno: String = "",
        val carretas: List<Carreta> = emptyList()
    )

    /** Devolve null quando o texto não é um pacote de passagem válido. */
    fun decodificar(texto: String): Conteudo? {
        val linhas = texto.trim().lines()
        if (linhas.firstOrNull()?.trim() != CABECALHO) return null

        var aparelho = ""
        var turno = ""
        val carretas = linkedMapOf<String, Carreta>()

        linhas.drop(1).forEach { bruta ->
            val campos = separa(bruta)
            when (campos.firstOrNull()) {
                "A" -> {
                    aparelho = campos.getOrElse(1) { "" }
                    turno = campos.getOrElse(2) { "" }
                }

                "C" -> leCarreta(campos)?.let { carretas[it.id] = it }

                "G" -> {
                    val id = campos.getOrElse(1) { "" }
                    carretas[id]?.let { c ->
                        carretas[id] = c.copy(
                            ativosGiro = c.ativosGiro + AtivoGiro(
                                carretaId = id,
                                tipo = campos.getOrElse(2) { "" },
                                quantidade = Texto.numeroDe(campos.getOrElse(3) { "" }),
                                unidade = campos.getOrElse(4) { "" },
                                documento = campos.getOrElse(5) { "" },
                                destino = campos.getOrElse(6) { "" },
                                observacao = campos.getOrElse(7) { "" }
                            )
                        )
                    }
                }

                "V" -> {
                    val id = campos.getOrElse(1) { "" }
                    carretas[id]?.let { c ->
                        carretas[id] = c.copy(
                            avarias = c.avarias + Avaria(
                                carretaId = id,
                                item = campos.getOrElse(2) { "" },
                                quantidade = Texto.numeroDe(campos.getOrElse(3) { "" }),
                                unidade = campos.getOrElse(4) { "" },
                                origem = campos.getOrElse(5) { "" },
                                observacao = campos.getOrElse(6) { "" },
                                foto = campos.getOrElse(7) { "" }
                            )
                        )
                    }
                }
            }
        }

        return Conteudo(aparelho, turno, carretas.values.toList())
    }

    private fun leCarreta(campos: List<String>): Carreta? {
        val id = campos.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
        val data = runCatching { LocalDate.parse(campos.getOrElse(2) { "" }) }.getOrNull() ?: return null

        val horarios = mutableMapOf<Marco, LocalTime>()
        Marco.entries.forEachIndexed { indice, marco ->
            // Os nove horários começam no campo 18: 0 é o tipo da linha e
            // 1..17 são os campos de identificação e carga.
            horaDe(campos.getOrElse(18 + indice) { "" })?.let { horarios[marco] = it }
        }
        val base = 18 + Marco.entries.size

        return Carreta(
            id = id,
            data = data,
            turno = campos.getOrElse(3) { "" },
            transportadora = campos.getOrElse(4) { "" },
            placa = campos.getOrElse(5) { "" },
            placaCavalo = campos.getOrElse(6) { "" },
            motorista = campos.getOrElse(7) { "" },
            veiculo = campos.getOrElse(8) { "" },
            origem = campos.getOrElse(9) { "" },
            destino = campos.getOrElse(10) { "" },
            documento = campos.getOrElse(11) { "" },
            responsavel = campos.getOrElse(12) { "" },
            produto = campos.getOrElse(13) { "" },
            notaFiscal = campos.getOrElse(14) { "" },
            quantidade = Texto.numeroDe(campos.getOrElse(15) { "" }),
            unidade = campos.getOrElse(16) { "" },
            observacaoCarga = campos.getOrElse(17) { "" },
            horarios = horarios,
            respostaAg = runCatching {
                RespostaAg.valueOf(campos.getOrElse(base) { "" })
            }.getOrDefault(RespostaAg.NAO_RESPONDIDO),
            justificativaSemAg = campos.getOrElse(base + 1) { "" },
            observacaoCarregamento = campos.getOrElse(base + 2) { "" }
        )
    }

    private fun hora(t: LocalTime?): String =
        t?.let { "%02d:%02d".format(it.hour, it.minute) } ?: ""

    private fun horaDe(texto: String): LocalTime? {
        if (texto.isBlank()) return null
        val partes = texto.split(':')
        if (partes.size < 2) return null
        val h = partes[0].toIntOrNull() ?: return null
        val m = partes[1].toIntOrNull() ?: return null
        return runCatching { LocalTime.of(h, m) }.getOrNull()
    }

    private fun junta(vararg campos: String): String =
        campos.joinToString(SEP.toString()) { escapa(it) }

    private fun escapa(valor: String): String = buildString {
        valor.forEach { c ->
            when (c) {
                '\\' -> append("\\\\")
                SEP -> append("\\|")
                '\n' -> append("\\n")
                '\r' -> Unit
                else -> append(c)
            }
        }
    }

    private fun separa(linha: String): List<String> {
        val campos = mutableListOf<String>()
        val atual = StringBuilder()
        var escapando = false
        linha.forEach { c ->
            when {
                escapando -> {
                    when (c) {
                        'n' -> atual.append('\n')
                        '\\' -> atual.append('\\')
                        '|' -> atual.append('|')
                        else -> atual.append(c)
                    }
                    escapando = false
                }

                c == '\\' -> escapando = true
                c == SEP -> {
                    campos += atual.toString()
                    atual.setLength(0)
                }

                else -> atual.append(c)
            }
        }
        campos += atual.toString()
        return campos
    }
}
