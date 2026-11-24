package com.patiocontrol.ui.exportar

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.OutlinedButton
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
import com.patiocontrol.util.Gravar
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

    /** Recado curto para o conferente; [erro] só muda a cor da faixa. */
    data class Recado(val texto: String, val erro: Boolean = true)

    private val _recado = MutableStateFlow<Recado?>(null)
    val recado: StateFlow<Recado?> = _recado.asStateFlow()

    /** Pacote montado esperando destino, com as carretas que ele carrega. */
    private data class Pendente(val arquivo: File, val ids: List<String>)

    private var pendente: Pendente? = null

    /**
     * Monta o pacote e abre a folha de compartilhamento.
     *
     * A ordem importa: as carretas só saem da fila depois que o destino
     * confirmou. Marcar antes e falhar no caminho é como um turno de coleta
     * desaparece sem ter sido entregue.
     */
    fun compartilhar(contexto: Context) = viewModelScope.launch {
        val p = montar(contexto) ?: return@launch

        val abriu = Compartilhar.arquivo(
            contexto, p.arquivo, "application/zip", "Pátio — exportação"
        )

        if (!abriu) {
            _recado.value = Recado(
                "Nenhum app deste aparelho aceitou receber o arquivo. O pacote está " +
                    "pronto — use SALVAR NO APARELHO. As carretas continuam na fila."
            )
            return@launch
        }

        concluir(contexto, p)
    }

    /**
     * Monta o pacote e pede ao [abrirSeletor] que mostre o seletor de
     * arquivos do sistema com [File.name] como nome sugerido.
     */
    fun salvarNoAparelho(contexto: Context, abrirSeletor: (String) -> Unit) =
        viewModelScope.launch {
            val p = montar(contexto) ?: return@launch
            pendente = p
            abrirSeletor(p.arquivo.name)
        }

    /** Resposta do seletor de arquivos; [destino] nulo é cancelamento. */
    fun gravarEm(contexto: Context, destino: Uri?) = viewModelScope.launch {
        val p = pendente
        pendente = null

        if (p == null) {
            _recado.value = Recado("O pacote se perdeu no caminho. Gere de novo.")
            return@launch
        }
        if (destino == null) {
            _recado.value = Recado("Gravação cancelada. As carretas continuam na fila.")
            return@launch
        }

        val gravou = withContext(Dispatchers.IO) { Gravar.copiarPara(contexto, p.arquivo, destino) }

        if (!gravou) {
            _recado.value = Recado("Não foi possível gravar na pasta escolhida. Tente outra.")
            return@launch
        }

        concluir(contexto, p)
        _recado.value = Recado("Pacote gravado: ${p.arquivo.name}", erro = false)
    }

    /** Tira da fila e limpa o cache. Só depois de o destino ter confirmado. */
    private suspend fun concluir(contexto: Context, p: Pendente) {
        repositorio.marcarExportadas(p.ids)
        // Listar a pasta e apagar arquivo é disco; não pode ser na thread
        // que desenha, ainda que sejam poucos pacotes.
        withContext(Dispatchers.IO) { Pacote.limparAntigos(contexto, manter = p.arquivo.name) }
    }

    private suspend fun montar(contexto: Context): Pendente? {
        val atual = estado.value
        if (atual.carretas.isEmpty()) {
            _recado.value = Recado("Nada para exportar")
            return null
        }

        val hoje = LocalDate.now()
        val agora = LocalTime.now()
        val carimbo = NomesPacote.carimbo(hoje, agora)

        // Tudo o que vem a seguir sai da thread principal: além de escrever o
        // zip, montar os CSV percorre todas as carretas concatenando texto e
        // cada foto do pacote custa um acesso a disco para conferir se ainda
        // existe. Num turno cheio, com foto de avaria, isso é tempo de sobra
        // para o Android acusar que o app travou.
        val arquivo = withContext(Dispatchers.IO) {
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
                    fotos = binarios.size,
                    arquivos = textos.keys.toList()
                )
            )

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
            _recado.value = Recado("Falha ao montar o pacote. Nada foi marcado como exportado.")
            return null
        }

        return Pendente(arquivo, atual.carretas.map { it.id })
    }

    fun limparRecado() {
        _recado.value = null
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
    val recado by vm.recado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current

    // Seletor de arquivos do sistema. Devolve null quando o conferente
    // desiste, e o ViewModel trata isso como fila intacta.
    val seletor = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { destino -> vm.gravarEm(contexto, destino) }

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
                // O pacote só leva o CSV que tem conteúdo. Prometer três e
                // entregar um faz quem abre no PC procurar arquivo que nunca
                // existiu e achar que se perdeu no caminho — por isso a frase
                // é montada a partir do que vai de fato.
                Text(
                    buildString {
                        val destinos = buildList {
                            add("registros em REGISTROS")
                            if (estado.linhasAg > 0) add("ativos em AG_DETALHE")
                            if (estado.itensAvaria > 0) add("avarias em AVARIAS")
                        }
                        append(
                            when (destinos.size) {
                                1 -> "Sai um arquivo .zip com o CSV de registros"
                                2 -> "Sai um arquivo .zip com os dois CSV"
                                else -> "Sai um arquivo .zip com os três CSV"
                            }
                        )
                        if (estado.fotos.isNotEmpty()) {
                            append(" e a pasta fotos/ com ${estado.fotos.size} foto(s) de avaria")
                        }
                        append(". No PC, extraia e cole: ")
                        append(destinos.joinToString(", "))
                        append(", sempre na coluna A.")
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

            recado?.let { r ->
                Spacer(Modifier.height(12.dp))
                Aviso(r.texto, erro = r.erro)
                Spacer(Modifier.height(4.dp))
                Button(onClick = { vm.limparRecado() }) { Text("ok") }
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { vm.compartilhar(contexto) },
                enabled = estado.carretas.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("GERAR E COMPARTILHAR PACOTE", fontWeight = FontWeight.Bold)
            }

            // Segunda saída, não enfeite: compartilhar depende de haver
            // outro app disposto a receber, e no aparelho corporativo isso
            // pode estar restringido pela política de dispositivos. O
            // seletor de arquivos é do sistema e nunca falta.
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { vm.salvarNoAparelho(contexto) { nome -> seletor.launch(nome) } },
                enabled = estado.carretas.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("SALVAR NO APARELHO")
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Salvar grava o .zip na pasta que você escolher — Downloads, cartão de " +
                    "memória ou a pasta do Drive. Serve quando o compartilhamento não " +
                    "estiver disponível.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}
