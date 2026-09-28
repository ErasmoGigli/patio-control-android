package com.patiocontrol.ui.exportar

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.patiocontrol.data.repository.PatioRepository
import com.patiocontrol.domain.Carreta
import com.patiocontrol.domain.CsvExportador
import com.patiocontrol.domain.LeiaMe
import com.patiocontrol.domain.NomesPacote
import com.patiocontrol.ui.componentes.Aviso
import com.patiocontrol.ui.componentes.BarraTopo
import com.patiocontrol.ui.componentes.Cartao
import com.patiocontrol.ui.componentes.Metrica
import com.patiocontrol.ui.componentes.RotuloBloco
import com.patiocontrol.ui.repositorio
import com.patiocontrol.util.Compartilhar
import com.patiocontrol.util.Pacote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

class ExportarViewModel(private val repositorio: PatioRepository) : ViewModel() {

    data class Estado(
        val carretas: List<Carreta> = emptyList(),
        val abertas: Int = 0,
        val aparelho: String = "",
        val turno: String = ""
    ) {
        val linhasAg: Int get() = carretas.sumOf { it.ativosGiro.size }
        val itensAvaria: Int get() = carretas.sumOf { it.avarias.size }
        val fotos: List<String>
            get() = carretas.flatMap { c -> c.avarias.map { it.foto } }
                .filter { it.isNotBlank() }
                .distinct()
    }

    val estado: StateFlow<Estado> = combine(
        repositorio.paraExportar(),
        repositorio.abertas().map { it.size },
        repositorio.configuracao
    ) { paraExportar, abertas, config ->
        Estado(paraExportar, abertas, config.aparelho, config.turno)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    private val _mensagem = MutableStateFlow<String?>(null)
    val mensagem: StateFlow<String?> = _mensagem.asStateFlow()

    /**
     * Monta o pacote e abre a folha de compartilhamento. As carretas só são
     * marcadas como exportadas depois que o arquivo existe — se der erro no
     * meio, elas continuam na fila em vez de sumirem silenciosamente.
     */
    fun exportar(contexto: Context) = viewModelScope.launch {
        val atual = estado.value
        if (atual.carretas.isEmpty()) {
            _mensagem.value = "Nada para exportar"
            return@launch
        }

        val hoje = LocalDate.now()
        val agora = LocalTime.now()
        val carimbo = NomesPacote.carimbo(hoje, agora)

        val textos = buildMap {
            put(NomesPacote.registros(carimbo), CsvExportador.registros(atual.carretas))
            val ativos = CsvExportador.ativos(atual.carretas)
            if (ativos.isNotBlank()) put(NomesPacote.ativos(carimbo), ativos)
            val avarias = CsvExportador.avarias(atual.carretas)
            if (avarias.isNotBlank()) put(NomesPacote.avarias(carimbo), avarias)
        }

        val binarios: Map<String, File> = atual.fotos
            .filter { repositorio.fotos.existe(it) }
            .associate { NomesPacote.foto(it) to repositorio.fotos.arquivo(it) }

        val comLeiaMe = textos + mapOf(
            NomesPacote.LEIA_ME to LeiaMe.texto(
                carimbo = carimbo,
                aparelho = atual.aparelho,
                turno = atual.turno,
                data = hoje,
                hora = agora,
                carretas = atual.carretas,
                fotos = binarios.size
            )
        )

        val arquivo = withContext(Dispatchers.IO) {
            runCatching {
                Pacote.montar(
                    contexto,
                    NomesPacote.pacote(atual.aparelho.ifBlank { "APP" }, carimbo),
                    comLeiaMe,
                    binarios
                )
            }.getOrNull()
        }

        if (arquivo == null || !arquivo.exists()) {
            _mensagem.value = "Falha ao montar o pacote. Nada foi marcado como exportado."
            return@launch
        }

        repositorio.marcarExportadas(atual.carretas.map { it.id })
        Pacote.limparAntigos(contexto, manter = arquivo.name)
        Compartilhar.arquivo(contexto, arquivo, "application/zip", "Pátio — exportação")
    }

    fun limparMensagem() {
        _mensagem.value = null
    }
}

@Composable
fun ExportarScreen(
    aoVoltar: () -> Unit,
    vm: ExportarViewModel = viewModel(factory = viewModelFactory {
        initializer { ExportarViewModel(repositorio) }
    })
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val mensagem by vm.mensagem.collectAsStateWithLifecycle()
    val contexto = LocalContext.current

    Scaffold(
        topBar = {
            BarraTopo(
                "EXPORTAR",
                listOf(estado.aparelho, estado.turno).filter { it.isNotBlank() }.joinToString(" · "),
                aoVoltar
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
                .verticalScroll(rememberScrollState())
        ) {
            RotuloBloco("Vai para o pacote")
            Cartao {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Metrica(estado.carretas.size.toString(), "carretas encerradas")
                    Metrica(estado.linhasAg.toString(), "linhas de AG")
                    Metrica(estado.itensAvaria.toString(), "itens de avaria")
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    buildString {
                        append("Sai um arquivo .zip com os três CSV")
                        if (estado.fotos.isNotEmpty()) {
                            append(" e a pasta fotos/ com ${estado.fotos.size} foto(s) de avaria")
                        }
                        append(". No PC, extraia e cole: registros em REGISTROS, ")
                        append("ativos em AG_DETALHE e avarias em AVARIAS, sempre na coluna A.")
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (estado.abertas > 0) {
                Spacer(Modifier.height(12.dp))
                Aviso(
                    "${estado.abertas} carreta(s) continuam abertas. Não entram neste pacote — " +
                        "sairiam de novo ao encerrar, criando linha repetida."
                )
            }

            if (mensagem != null) {
                Spacer(Modifier.height(12.dp))
                Aviso(mensagem!!, erro = true)
                Spacer(Modifier.height(4.dp))
                Button(onClick = { vm.limparMensagem() }) { Text("ok") }
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { vm.exportar(contexto) },
                enabled = estado.carretas.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("GERAR E COMPARTILHAR PACOTE", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
