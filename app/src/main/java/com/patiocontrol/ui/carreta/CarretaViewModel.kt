package com.patiocontrol.ui.carreta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patiocontrol.data.repository.PatioRepository
import com.patiocontrol.domain.AtivoGiro
import com.patiocontrol.domain.Avaria
import com.patiocontrol.domain.CalculadoraIndicadores
import com.patiocontrol.domain.Carreta
import com.patiocontrol.domain.ClassificadorSituacao
import com.patiocontrol.domain.Configuracao
import com.patiocontrol.domain.Indicadores
import com.patiocontrol.domain.Marco
import com.patiocontrol.domain.Pendencia
import com.patiocontrol.domain.RespostaAg
import com.patiocontrol.domain.Situacao
import com.patiocontrol.domain.VerificadorAlertas
import com.patiocontrol.domain.VerificadorPendencias
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * Estado da tela de uma carreta e das suas filhas.
 *
 * Fluxo unidirecional: o banco é a fonte, o estado desce para a tela e a
 * tela só manda eventos para cá. Nenhuma tela guarda cópia do dado — é o
 * que impede o rótulo velho ao lado do valor novo.
 */
class CarretaViewModel(
    private val repositorio: PatioRepository,
    private val carretaId: String
) : ViewModel() {

    data class Estado(
        val carreta: Carreta? = null,
        val configuracao: Configuracao = Configuracao(),
        val indicadores: Indicadores = Indicadores(),
        val situacao: Situacao = Situacao.REGISTRO_INCOMPLETO,
        val pendencias: List<Pendencia> = emptyList(),
        val alertas: List<String> = emptyList(),
        val carregando: Boolean = true
    ) {
        val podeEncerrar: Boolean get() = pendencias.isEmpty() && carreta != null
    }

    /** Relógio de parede: faz a permanência da carreta em andamento subir
     *  sozinha na tela, sem o Composable ler LocalTime.now() por conta. */
    private val relogio: Flow<LocalTime> = flow {
        while (true) {
            emit(LocalTime.now())
            delay(30_000)
        }
    }

    val estado: StateFlow<Estado> = combine(
        repositorio.observar(carretaId),
        repositorio.configuracao,
        relogio
    ) { carreta, config, agora ->
        if (carreta == null) {
            Estado(configuracao = config, carregando = false)
        } else {
            Estado(
                carreta = carreta,
                configuracao = config,
                indicadores = CalculadoraIndicadores.calcular(carreta, agora),
                situacao = ClassificadorSituacao.classificar(carreta),
                pendencias = VerificadorPendencias.verificar(carreta),
                alertas = VerificadorAlertas.verificar(carreta),
                carregando = false
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    private val _mensagem = MutableStateFlow<String?>(null)
    val mensagem: StateFlow<String?> = _mensagem.asStateFlow()

    fun limparMensagem() {
        _mensagem.value = null
    }

    // ------------------------------------------------------------ marcos

    fun marcar(marco: Marco) = viewModelScope.launch {
        repositorio.marcar(carretaId, marco)
    }

    fun marcarComHora(marco: Marco, hora: LocalTime?) = viewModelScope.launch {
        if (hora == null) repositorio.desmarcar(carretaId, marco)
        else repositorio.marcar(carretaId, marco, hora)
    }

    /** Desfaz o último marco — o toque errado é comum com luva e sol na tela. */
    fun desfazerUltimo() = viewModelScope.launch {
        val carreta = estado.value.carreta ?: return@launch
        val ultimo = Marco.SEQUENCIA.lastOrNull { carreta.horario(it) != null } ?: return@launch
        repositorio.desmarcar(carretaId, ultimo)
        _mensagem.value = "${ultimo.rotulo} desfeito"
    }

    // ------------------------------------------------------------ edição

    fun salvar(carreta: Carreta) = viewModelScope.launch {
        repositorio.salvar(carreta)
        if (carreta.placa.isNotBlank() &&
            repositorio.placaRepetidaNoDia(carreta.placa, carreta.data, carreta.id)
        ) {
            _mensagem.value = "Atenção: já existe outra carreta com esta placa nesta data"
        }
    }

    fun responderAg(resposta: RespostaAg) = viewModelScope.launch {
        val carreta = estado.value.carreta ?: return@launch
        repositorio.salvar(carreta.copy(respostaAg = resposta))
    }

    fun salvarAtivoGiro(ativo: AtivoGiro) = viewModelScope.launch {
        repositorio.salvarAtivoGiro(carretaId, ativo)
    }

    fun apagarAtivoGiro(ativo: AtivoGiro) = viewModelScope.launch {
        repositorio.apagarAtivoGiro(carretaId, ativo)
    }

    fun salvarAvaria(avaria: Avaria) = viewModelScope.launch {
        repositorio.salvarAvaria(carretaId, avaria)
    }

    fun apagarAvaria(avaria: Avaria) = viewModelScope.launch {
        repositorio.apagarAvaria(carretaId, avaria)
    }

    fun nomeDeFotoNova(): String =
        com.patiocontrol.domain.NomesPacote.nomeDeFoto(
            carretaId,
            (estado.value.carreta?.avarias?.size ?: 0) + 1,
            System.currentTimeMillis()
        )

    fun fotos() = repositorio.fotos

    // ------------------------------------------------------- encerramento

    fun encerrar(aoConcluir: () -> Unit) = viewModelScope.launch {
        if (!estado.value.podeEncerrar) {
            _mensagem.value = "Resolva as pendências antes de encerrar"
            return@launch
        }
        repositorio.encerrar(carretaId)
        aoConcluir()
    }

    fun reabrir() = viewModelScope.launch { repositorio.reabrir(carretaId) }

    fun apagar(aoConcluir: () -> Unit) = viewModelScope.launch {
        repositorio.apagar(carretaId)
        aoConcluir()
    }
}
