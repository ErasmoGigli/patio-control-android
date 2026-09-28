package com.patiocontrol.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Os nove horários de uma carreta. PROGRAMADO é digitado na abertura; os
 * outros oito são marcados com um toque, sempre na ordem desta enum — é ela
 * que define qual é o próximo botão na tela da carreta.
 */
enum class Marco(val rotulo: String, val botao: String, val marcadoComToque: Boolean) {
    PROGRAMADO("Horário programado", "", false),
    CHEGADA("Chegada", "MARCAR CHEGADA", true),
    ENTRADA("Entrada na unidade", "MARCAR ENTRADA", true),
    INICIO_DESCARGA("Início da descarga", "INICIAR DESCARGA", true),
    FIM_DESCARGA("Fim da descarga", "MARCAR FIM DA DESCARGA", true),
    INICIO_AG("Início do carregamento de AG", "INICIAR CARREGAMENTO DE AG", true),
    FIM_AG("Fim do carregamento de AG", "MARCAR FIM DO CARREGAMENTO", true),
    LIBERACAO("Liberação", "MARCAR LIBERAÇÃO", true),
    SAIDA("Saída da unidade", "MARCAR SAÍDA", true);

    companion object {
        /** Os oito marcos de toque, na ordem em que acontecem no pátio. */
        val SEQUENCIA: List<Marco> = entries.filter { it.marcadoComToque }
    }
}

/** Resposta à pergunta "a carreta saiu carregada com ativo de giro?". */
enum class RespostaAg { NAO_RESPONDIDO, SIM, NAO }

data class AtivoGiro(
    val id: Long = 0,
    val carretaId: String = "",
    val tipo: String = "",
    val quantidade: Double? = null,
    val unidade: String = "",
    val documento: String = "",
    val destino: String = "",
    val observacao: String = ""
)

data class Avaria(
    val id: Long = 0,
    val carretaId: String = "",
    val item: String = "",
    val quantidade: Double? = null,
    val unidade: String = "",
    val origem: String = "",
    val observacao: String = "",
    /** Nome do arquivo em disco; vazio quando não há foto. A quantidade é
     *  opcional de propósito: nem sempre dá para medir a avaria na hora. */
    val foto: String = ""
)

data class Carreta(
    val id: String,
    val data: LocalDate,
    val turno: String = "",
    val transportadora: String = "",
    val placa: String = "",
    val placaCavalo: String = "",
    val motorista: String = "",
    val veiculo: String = "",
    val origem: String = "",
    val destino: String = "",
    val documento: String = "",
    val responsavel: String = "",

    val produto: String = "",
    val notaFiscal: String = "",
    val quantidade: Double? = null,
    val unidade: String = "",
    val observacaoCarga: String = "",

    val horarios: Map<Marco, LocalTime> = emptyMap(),

    val respostaAg: RespostaAg = RespostaAg.NAO_RESPONDIDO,
    val justificativaSemAg: String = "",
    val observacaoCarregamento: String = "",

    val encerradaEm: Instant? = null,
    val exportadaEm: Instant? = null,
    val transferidaEm: Instant? = null,

    val ativosGiro: List<AtivoGiro> = emptyList(),
    val avarias: List<Avaria> = emptyList()
) {
    fun horario(marco: Marco): LocalTime? = horarios[marco]

    fun com(marco: Marco, hora: LocalTime?): Carreta =
        copy(horarios = if (hora == null) horarios - marco else horarios + (marco to hora))

    val encerrada: Boolean get() = encerradaEm != null

    /** O próximo marco a marcar, ou null quando a sequência acabou. */
    val proximoMarco: Marco?
        get() = Marco.SEQUENCIA.firstOrNull { horarios[it] == null }

    val temAvaria: Boolean get() = avarias.isNotEmpty()
}

/** As doze situações possíveis, derivadas dos horários — nunca digitadas. */
enum class Situacao(val rotulo: String) {
    REGISTRO_INCOMPLETO("Registro incompleto"),
    PROGRAMADA("Programada"),
    AGUARDANDO_ENTRADA("Aguardando entrada"),
    AGUARDANDO_DESCARGA("Aguardando descarga"),
    EM_DESCARGA("Em descarga"),
    AGUARDANDO_CARREGAMENTO_AG("Aguardando carregamento de AG"),
    EM_CARREGAMENTO_AG("Em carregamento de AG"),
    AGUARDANDO_LIBERACAO("Aguardando liberação"),
    LIBERADA("Liberada"),
    FINALIZADA("Finalizada"),
    SAIDA_SEM_AG("Saída sem AG"),
    TRANSFERIDA("Transferida")
}

/**
 * Os catorze indicadores de tempo, todos em minutos exceto [produtivo],
 * que é uma fração de 0 a 1. null significa "ainda não dá para calcular",
 * e é diferente de zero.
 */
data class Indicadores(
    val atraso: Int? = null,
    val chegadaEntrada: Int? = null,
    val esperaDescarga: Int? = null,
    val descarga: Int? = null,
    val esperaAg: Int? = null,
    val carregamentoAg: Int? = null,
    val liberacao: Int? = null,
    val liberacaoSaida: Int? = null,
    val permanencia: Int? = null,
    val ciclo: Int? = null,
    val operacional: Int? = null,
    val espera: Int? = null,
    val naoProdutivo: Int? = null,
    val produtivo: Double? = null
)

/** Um impedimento que trava o encerramento da carreta. */
data class Pendencia(val mensagem: String)

data class TipoAtivoGiro(val tipo: String, val unidadePadrao: String)

/** Metas opcionais, em minutos; vazias até a operação definir as suas. */
data class Metas(
    val esperaDescarga: Int? = null,
    val descarga: Int? = null,
    val esperaAg: Int? = null,
    val carregamentoAg: Int? = null,
    val liberacao: Int? = null,
    val permanencia: Int? = null,
    val ciclo: Int? = null,
    val produtivo: Double? = null
)

/**
 * Tudo que é cadastro editável pelo conferente. Nada disso é constante no
 * código: transportadora, origem e destino mudam por unidade, e quem opera
 * precisa conseguir incluir uma nova sem esperar uma versão do app.
 */
data class Configuracao(
    val aparelho: String = "",
    val unidade: String = "",
    val responsavel: String = "",
    val turno: String = "",
    val sequencia: Int = 0,
    val metas: Metas = Metas(),
    val transportadoras: List<String> = emptyList(),
    val origens: List<String> = emptyList(),
    val destinos: List<String> = emptyList(),
    val veiculos: List<String> = emptyList(),
    val turnos: List<String> = emptyList(),
    val produtos: List<String> = emptyList(),
    val unidades: List<String> = emptyList(),
    val tiposAg: List<TipoAtivoGiro> = emptyList(),
    val motivosSemAg: List<String> = emptyList(),
    val itensAvaria: List<String> = emptyList(),
    val origensAvaria: List<String> = emptyList()
) {
    companion object {
        /**
         * Catálogos de partida. São valores do ramo de bebidas, não de uma
         * empresa: a lista de transportadoras nasce vazia justamente porque
         * é o que mais varia de unidade para unidade.
         */
        fun padrao(aparelho: String): Configuracao = Configuracao(
            aparelho = aparelho,
            unidade = "",
            turno = "1º TURNO",
            transportadoras = listOf("FROTA PRÓPRIA", "NÃO INFORMADO"),
            origens = listOf("NÃO INFORMADO"),
            destinos = listOf("NÃO INFORMADO"),
            veiculos = listOf(
                "CARRETA", "CARRETA SIDER", "CARRETA BAÚ", "CARRETA LS",
                "BITREM", "RODOTREM", "VANDERLEIA", "TRUCK", "TOCO"
            ),
            turnos = listOf("1º TURNO", "2º TURNO", "3º TURNO", "ADMINISTRATIVO"),
            produtos = listOf(
                "DESCARTÁVEL", "DESCARTÁVEL + CHOPP", "CARGA MISTA",
                "RETORNÁVEL 300 ML", "RETORNÁVEL 600 ML", "RETORNÁVEL 1 L",
                "RETORNÁVEL MISTO", "CHOPP", "ÁGUA", "REFRIGERANTE",
                "ENERGÉTICO", "LATAS / LONG NECK"
            ),
            unidades = listOf(
                "PALETE", "UNIDADE", "BARRIL", "CAIXA", "FARDO",
                "GARRAFA", "HL", "TONELADA"
            ),
            tiposAg = listOf(
                TipoAtivoGiro("LITRINHO (300 ML)", "PALETE"),
                TipoAtivoGiro("GARRAFA 600 ML", "PALETE"),
                TipoAtivoGiro("LITRÃO (1 L)", "PALETE"),
                TipoAtivoGiro("BARRIL DE CHOPP 50 L", "BARRIL"),
                TipoAtivoGiro("PALETE PBR1", "UNIDADE"),
                TipoAtivoGiro("PALETE PBR2", "UNIDADE"),
                TipoAtivoGiro("CHAPATEX", "UNIDADE"),
                TipoAtivoGiro("OUTROS AG", "UNIDADE")
            ),
            motivosSemAg = listOf(
                "Falta de AG disponível no pátio",
                "AG sem documento fiscal liberado",
                "Determinação da fábrica / origem",
                "Urgência no retorno da carreta",
                "Veículo não compatível com o AG",
                "Falta de equipe ou equipamento para carregar",
                "Carreta com avaria ou restrição",
                "Carreta veio apenas para carregar",
                "Outros (detalhar em observações)"
            ),
            itensAvaria = listOf(
                "PRODUTO DESCARTÁVEL", "PRODUTO RETORNÁVEL 300 ML",
                "PRODUTO RETORNÁVEL 600 ML", "PRODUTO RETORNÁVEL 1 L",
                "CHOPP (BARRIL CHEIO)", "ÁGUA", "REFRIGERANTE", "ENERGÉTICO",
                "LATAS / LONG NECK", "CARGA MISTA", "PALETE PBR1", "PALETE PBR2",
                "CHAPATEX", "GARRAFEIRA LITRINHO (300 ML)", "GARRAFEIRA 600 ML",
                "GARRAFEIRA LITRÃO (1 L)", "BARRIL DE CHOPP VAZIO", "OUTROS"
            ),
            origensAvaria = listOf(
                "TRANSPORTE", "FÁBRICA / ORIGEM", "MANUSEIO NA UNIDADE", "INDEFINIDA"
            )
        )
    }
}
