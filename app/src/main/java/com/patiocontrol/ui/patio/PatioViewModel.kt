package com.patiocontrol.ui.patio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patiocontrol.data.repository.PatioRepository
import com.patiocontrol.domain.CalculadoraIndicadores
import com.patiocontrol.domain.Carreta
import com.patiocontrol.domain.ClassificadorSituacao
import com.patiocontrol.domain.ComparadorMetas
import com.patiocontrol.domain.Configuracao
import com.patiocontrol.domain.Metas
import com.patiocontrol.domain.Situacao
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

class PatioViewModel(private val repositorio: PatioRepository) : ViewModel() {

    data class Estado(
        val carretas: List<LinhaCarreta> = emptyList(),
        val encerradas: List<LinhaCarreta> = emptyList(),
        val configuracao: Configuracao = Configuracao(),
        val aExportar: Int = 0,
        val carregando: Boolean = true
    ) {
        val emOperacao: Int
            get() = carretas.count {
                it.situacao == Situacao.EM_DESCARGA || it.situacao == Situacao.EM_CARREGAMENTO_AG
            }

        val aparelhoSemCodigo: Boolean get() = !carregando && configuracao.aparelho.isBlank()
    }

    private val relogio: Flow<LocalTime> = flow {
        while (true) {
            emit(LocalTime.now())
            delay(30_000)
        }
    }

    val estado: StateFlow<Estado> = combine(
        repositorio.abertas(),
        repositorio.encerradas(),
        repositorio.paraExportar(),
        repositorio.configuracao,
        relogio
    ) { abertas, encerradas, aExportar, config, agora ->
        Estado(
            carretas = abertas.map { linha(it, agora, config.metas) },
            encerradas = encerradas.map { linha(it, agora, config.metas) },
            configuracao = config,
            aExportar = aExportar.size,
            carregando = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    private fun linha(carreta: Carreta, agora: LocalTime, metas: Metas): LinhaCarreta {
        val indicadores = CalculadoraIndicadores.calcular(carreta, agora)
        return LinhaCarreta(
            carreta = carreta,
            indicadores = indicadores,
            situacao = ClassificadorSituacao.classificar(carreta),
            estourosDeMeta = ComparadorMetas.estouros(indicadores, metas).size
        )
    }

    /** Abre a carreta já com a chegada marcada e devolve o id gerado. */
    fun abrir(base: Carreta, aoAbrir: (String) -> Unit) = viewModelScope.launch {
        val criada = repositorio.abrir(base)
        aoAbrir(criada.id)
    }
}
