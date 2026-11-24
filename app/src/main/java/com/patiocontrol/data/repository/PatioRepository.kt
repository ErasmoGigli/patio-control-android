package com.patiocontrol.data.repository

import com.patiocontrol.data.ConfiguracaoStore
import com.patiocontrol.data.FotoStore
import com.patiocontrol.data.local.AtivoGiroDao
import com.patiocontrol.data.local.AvariaDao
import com.patiocontrol.data.local.CarretaDao
import com.patiocontrol.data.local.paraDominio
import com.patiocontrol.data.local.paraEntidade
import com.patiocontrol.domain.AtivoGiro
import com.patiocontrol.domain.Avaria
import com.patiocontrol.domain.Carreta
import com.patiocontrol.domain.Configuracao
import com.patiocontrol.domain.Marco
import com.patiocontrol.domain.Passagem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Fonte única da verdade da aplicação.
 *
 * Toda leitura da interface passa por aqui e todo dado sai como modelo de
 * domínio. Os ViewModel não conhecem Room, DataStore nem arquivo de foto —
 * conhecem este contrato.
 */
class PatioRepository(
    private val carretaDao: CarretaDao,
    private val ativoGiroDao: AtivoGiroDao,
    private val avariaDao: AvariaDao,
    private val configuracaoStore: ConfiguracaoStore,
    val fotos: FotoStore
) {

    val configuracao: Flow<Configuracao> = configuracaoStore.configuracao

    fun abertas(): Flow<List<Carreta>> =
        carretaDao.observarAbertas().map { lista -> lista.map { it.paraDominio() } }

    fun encerradas(): Flow<List<Carreta>> =
        carretaDao.observarEncerradas().map { lista -> lista.map { it.paraDominio() } }

    fun paraExportar(): Flow<List<Carreta>> =
        carretaDao.observarParaExportar().map { lista -> lista.map { it.paraDominio() } }

    fun observar(id: String): Flow<Carreta?> =
        carretaDao.observar(id).map { it?.paraDominio() }

    suspend fun buscar(id: String): Carreta? = carretaDao.buscar(id)?.paraDominio()

    suspend fun salvarConfiguracao(config: Configuracao) = configuracaoStore.salvar(config)

    suspend fun garantirAparelho(): String = configuracaoStore.garantirAparelho()

    /**
     * Cria a carreta já com a chegada marcada. O identificador junta código
     * do aparelho, data e sequência local: sem servidor, é ele que evita
     * dois aparelhos gerarem o mesmo id no mesmo dia.
     */
    suspend fun abrir(base: Carreta): Carreta {
        val config = configuracao.first()
        val sequencia = configuracaoStore.proximaSequencia()
        val hoje = LocalDate.now()
        val id = "%s-%04d%02d%02d-%03d".format(
            config.aparelho, hoje.year, hoje.monthValue, hoje.dayOfMonth, sequencia
        )
        val carreta = base.copy(id = id).com(Marco.CHEGADA, LocalTime.now().semSegundos())
        carretaDao.salvar(carreta.paraEntidade())
        return carreta
    }

    suspend fun salvar(carreta: Carreta) = carretaDao.salvar(carreta.paraEntidade())

    suspend fun marcar(id: String, marco: Marco, hora: LocalTime = LocalTime.now().semSegundos()) {
        val carreta = buscar(id) ?: return
        carretaDao.salvar(carreta.com(marco, hora).paraEntidade())
    }

    suspend fun desmarcar(id: String, marco: Marco) {
        val carreta = buscar(id) ?: return
        carretaDao.salvar(carreta.com(marco, null).paraEntidade())
    }

    suspend fun encerrar(id: String) {
        val carreta = buscar(id) ?: return
        carretaDao.salvar(carreta.copy(encerradaEm = Instant.now()).paraEntidade())
    }

    /**
     * Devolve a carreta ao fluxo normal deste aparelho.
     *
     * Limpa também a marca de transferida: se a passagem de turno não chegou
     * do outro lado, este é o caminho de volta — sem isso o registro ficaria
     * fora da fila de exportação para sempre.
     */
    suspend fun reabrir(id: String) {
        val carreta = buscar(id) ?: return
        carretaDao.salvar(
            carreta.copy(encerradaEm = null, exportadaEm = null, transferidaEm = null)
                .paraEntidade()
        )
    }

    suspend fun apagar(id: String) {
        buscar(id)?.avarias?.forEach { fotos.apagar(it.foto) }
        carretaDao.apagar(id)
    }

    suspend fun placaRepetidaNoDia(placa: String, data: LocalDate, exceto: String): Boolean =
        placa.isNotBlank() && carretaDao.contarPlacaNoDia(placa, data, exceto) > 0

    // ------------------------------------------------------------ itens

    suspend fun salvarAtivoGiro(carretaId: String, ativo: AtivoGiro) {
        if (ativo.id == 0L) ativoGiroDao.inserir(ativo.paraEntidade(carretaId))
        else ativoGiroDao.atualizar(ativo.paraEntidade(carretaId))
    }

    suspend fun apagarAtivoGiro(carretaId: String, ativo: AtivoGiro) =
        ativoGiroDao.apagar(ativo.paraEntidade(carretaId))

    suspend fun salvarAvaria(carretaId: String, avaria: Avaria) {
        // Troca de foto: o arquivo antigo some junto, senão a pasta cresce
        // com imagens que nenhum registro aponta.
        val anterior = if (avaria.id != 0L) avariaDao.buscar(avaria.id) else null
        if (anterior != null && anterior.foto.isNotBlank() && anterior.foto != avaria.foto) {
            fotos.apagar(anterior.foto)
        }
        if (avaria.id == 0L) avariaDao.inserir(avaria.paraEntidade(carretaId))
        else avariaDao.atualizar(avaria.paraEntidade(carretaId))
    }

    suspend fun apagarAvaria(carretaId: String, avaria: Avaria) {
        fotos.apagar(avaria.foto)
        avariaDao.apagar(avaria.paraEntidade(carretaId))
    }

    // ------------------------------------------------------- exportação

    suspend fun marcarExportadas(ids: List<String>) {
        if (ids.isEmpty()) return
        carretaDao.marcarExportadas(ids, Instant.now())
    }

    // -------------------------------------------------- passagem de turno

    /**
     * Monta o bilhete da passagem de turno e devolve junto os identificadores
     * que ele carrega.
     *
     * Os ids voltam de propósito: quem entrega precisa marcar exatamente
     * essas carretas como transferidas depois que a entrega se confirmar —
     * nem antes, nem um conjunto diferente do que saiu.
     */
    suspend fun prepararPassagem(): Entrega {
        val config = configuracao.first()
        val abertas = abertasAgora()
        return Entrega(
            texto = Passagem.codificar(config.aparelho, config.turno, abertas),
            ids = abertas.map { it.id }
        )
    }

    suspend fun abertasAgora(): List<Carreta> = abertas().first()

    /**
     * Recebe as carretas de outro aparelho. Um id que já existe aqui é
     * ignorado: receber duas vezes o mesmo QR não pode duplicar carreta
     * nem sobrescrever o que este aparelho já marcou depois.
     */
    suspend fun receber(conteudo: Passagem.Conteudo): Int {
        var novas = 0
        conteudo.carretas.forEach { recebida ->
            if (carretaDao.buscar(recebida.id) != null) return@forEach
            carretaDao.salvar(recebida.paraEntidade())
            recebida.ativosGiro.forEach { ativoGiroDao.inserir(it.copy(id = 0).paraEntidade(recebida.id)) }
            // A foto não viaja no QR; o registro chega sem imagem, de propósito.
            recebida.avarias.forEach {
                avariaDao.inserir(it.copy(id = 0, foto = "").paraEntidade(recebida.id))
            }
            novas++
        }
        return novas
    }

    /** Marca como transferidas as carretas que saíram na passagem de turno. */
    suspend fun marcarTransferidas(ids: List<String>) {
        if (ids.isEmpty()) return
        carretaDao.marcarTransferidas(ids, Instant.now())
    }
}

/** O relógio do pátio tem precisão de minuto; segundo só polui o dado. */
fun LocalTime.semSegundos(): LocalTime = withSecond(0).withNano(0)

/** Bilhete de passagem pronto para entregar, com o que ele carrega dentro. */
data class Entrega(val texto: String, val ids: List<String>)
