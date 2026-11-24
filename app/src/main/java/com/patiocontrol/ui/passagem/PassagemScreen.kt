package com.patiocontrol.ui.passagem

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.patiocontrol.data.repository.Entrega
import com.patiocontrol.data.repository.PatioRepository
import com.patiocontrol.domain.Passagem
import com.patiocontrol.ui.componentes.Aviso
import com.patiocontrol.ui.componentes.BarraTopo
import com.patiocontrol.ui.componentes.Cartao
import com.patiocontrol.ui.componentes.RotuloBloco
import com.patiocontrol.ui.repositorio
import com.patiocontrol.util.Compartilhar
import com.patiocontrol.util.Gravar
import com.patiocontrol.util.Qr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Passagem de turno entre aparelhos, sem servidor.
 *
 * Dois caminhos para o mesmo conteúdo: QR na tela, lido pela câmera do
 * outro celular, e arquivo, para quando os dois estão longe um do outro.
 * Nenhum deles depende de rede — funciona com os dois em modo avião.
 */
class PassagemViewModel(private val repositorio: PatioRepository) : ViewModel() {

    data class Estado(
        val partes: List<Bitmap> = emptyList(),
        val abertas: Int = 0,
        val gerando: Boolean = false,
        val recebidas: Int? = null,
        val entregues: Int? = null,
        val erro: String? = null
    )

    /** Bilhete montado esperando destino, igual ao pacote da exportação. */
    private var pendente: Entrega? = null

    private val _estado = MutableStateFlow(Estado())
    val estado: StateFlow<Estado> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            repositorio.abertas().collect { lista ->
                _estado.update { it.copy(abertas = lista.size) }
            }
        }
    }

    fun gerarQr() = viewModelScope.launch {
        _estado.update { it.copy(gerando = true, erro = null) }
        val texto = repositorio.prepararPassagem().texto
        val imagens = withContext(Dispatchers.Default) {
            runCatching { Qr.fatiar(texto).map { Qr.bitmap(it) } }.getOrNull()
        }
        _estado.update {
            if (imagens == null) it.copy(gerando = false, erro = "Não foi possível gerar o QR")
            else it.copy(gerando = false, partes = imagens)
        }
    }

    /**
     * Entrega por compartilhamento. As carretas só são marcadas como
     * transferidas depois que a folha abriu: marcar antes e falhar aqui
     * deixaria o registro fora da fila de exportação sem ter saído do
     * aparelho.
     */
    fun entregarCompartilhando(contexto: Context) = viewModelScope.launch {
        val entrega = montar(contexto) ?: return@launch
        val arquivo = entrega.second

        if (!Compartilhar.arquivo(contexto, arquivo, "text/plain", "Passagem de turno")) {
            _estado.update {
                it.copy(
                    erro = "Nenhum app deste aparelho aceitou o arquivo. Use SALVAR NO " +
                        "APARELHO. As carretas continuam aqui."
                )
            }
            return@launch
        }
        concluir(entrega.first)
    }

    /** Entrega gravando num lugar escolhido, sem depender de outro app. */
    fun entregarSalvando(contexto: Context, abrirSeletor: (String) -> Unit) =
        viewModelScope.launch {
            val entrega = montar(contexto) ?: return@launch
            pendente = entrega.first
            abrirSeletor(entrega.second.name)
        }

    /** Resposta do seletor de arquivos; [destino] nulo é cancelamento. */
    fun gravarEm(contexto: Context, destino: Uri?) = viewModelScope.launch {
        val bilhete = pendente
        pendente = null

        if (bilhete == null) {
            _estado.update { it.copy(erro = "O bilhete se perdeu no caminho. Gere de novo.") }
            return@launch
        }
        if (destino == null) {
            _estado.update {
                it.copy(erro = "Gravação cancelada. As carretas continuam neste aparelho.")
            }
            return@launch
        }

        val origem = File(File(contexto.cacheDir, "pacotes"), ARQUIVO)
        val gravou = withContext(Dispatchers.IO) { Gravar.copiarPara(contexto, origem, destino) }
        if (!gravou) {
            _estado.update { it.copy(erro = "Não foi possível gravar na pasta escolhida.") }
            return@launch
        }
        concluir(bilhete)
    }

    private suspend fun concluir(bilhete: Entrega) {
        repositorio.marcarTransferidas(bilhete.ids)
        _estado.update { it.copy(entregues = bilhete.ids.size, erro = null) }
    }

    /** Monta o bilhete e grava o arquivo temporário; nulo se não der. */
    private suspend fun montar(contexto: Context): Pair<Entrega, File>? {
        val bilhete = repositorio.prepararPassagem()
        if (bilhete.ids.isEmpty()) {
            _estado.update { it.copy(erro = "Nenhuma carreta aberta para passar.") }
            return null
        }
        val arquivo = withContext(Dispatchers.IO) {
            runCatching {
                File(contexto.cacheDir, "pacotes").apply { if (!exists()) mkdirs() }
                    .let { File(it, ARQUIVO) }
                    .apply { writeText(bilhete.texto) }
            }.getOrNull()
        }
        if (arquivo == null) {
            _estado.update { it.copy(erro = "Não foi possível gravar o arquivo") }
            return null
        }
        return bilhete to arquivo
    }

    fun receberTexto(texto: String) = viewModelScope.launch {
        val conteudo = Passagem.decodificar(texto)
        if (conteudo == null) {
            _estado.update { it.copy(erro = "Arquivo não é uma passagem de turno válida") }
            return@launch
        }
        val novas = repositorio.receber(conteudo)
        _estado.update { it.copy(recebidas = novas, erro = null) }
    }

    fun receberDeArquivo(contexto: Context, uri: android.net.Uri) = viewModelScope.launch {
        val texto = withContext(Dispatchers.IO) {
            runCatching {
                contexto.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
        }
        if (texto.isNullOrBlank()) {
            _estado.update { it.copy(erro = "Não foi possível ler o arquivo") }
            return@launch
        }
        receberTexto(texto)
    }

    fun limpar() = _estado.update { it.copy(erro = null, recebidas = null, entregues = null) }

    private companion object {
        const val ARQUIVO = "passagem-turno.txt"
    }
}

@Composable
fun PassagemScreen(
    aoVoltar: () -> Unit,
    vm: PassagemViewModel = viewModel(factory = viewModelFactory {
        initializer { PassagemViewModel(repositorio) }
    })
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    var lendoQr by remember { mutableStateOf(false) }

    val escolherArquivo = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { vm.receberDeArquivo(contexto, it) } }

    // Seletor do sistema para gravar o bilhete sem depender de outro app.
    val ondeSalvar = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { destino -> vm.gravarEm(contexto, destino) }

    Scaffold(
        topBar = { BarraTopo("PASSAGEM DE TURNO", "${estado.abertas} carreta(s) aberta(s)", aoVoltar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
                .verticalScroll(rememberScrollState())
        ) {
            RotuloBloco("Entregar o turno")
            Cartao {
                Text(
                    "As carretas ainda abertas passam para o aparelho do próximo turno. " +
                        "As encerradas ficam aqui e saem na exportação normal. " +
                        "Depois de entregues, elas deixam de sair na exportação deste " +
                        "aparelho — quem exporta passa a ser o outro.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { vm.gerarQr() },
                enabled = !estado.gerando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(if (estado.gerando) "GERANDO..." else "MOSTRAR QR CODE", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { vm.entregarCompartilhando(contexto) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Enviar por arquivo") }

            // Segunda saída, pelo mesmo motivo da exportação: compartilhar
            // depende de haver outro app disposto a receber, e no aparelho
            // corporativo isso pode estar restringido.
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { vm.entregarSalvando(contexto) { nome -> ondeSalvar.launch(nome) } },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Salvar no aparelho") }

            if (estado.partes.isNotEmpty()) {
                RotuloBloco(
                    if (estado.partes.size == 1) "Leia este código no outro aparelho"
                    else "São ${estado.partes.size} códigos — leia todos, em qualquer ordem"
                )
                estado.partes.forEachIndexed { indice, bitmap ->
                    Cartao {
                        if (estado.partes.size > 1) {
                            Text(
                                "parte ${indice + 1} de ${estado.partes.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR da passagem de turno",
                            contentScale = ContentScale.Fit,
                            filterQuality = FilterQuality.None,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }

            RotuloBloco("Receber o turno")
            Button(
                onClick = { lendoQr = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) { Text("LER QR CODE", fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { escolherArquivo.launch("*/*") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Receber por arquivo") }

            Spacer(Modifier.height(8.dp))
            Aviso(
                "Os dois caminhos entregam a mesma coisa. O QR serve com os dois aparelhos " +
                    "lado a lado; o arquivo serve quando estão longe — inclusive por " +
                    "Bluetooth, sem rede."
            )

            estado.entregues?.let { n ->
                Spacer(Modifier.height(12.dp))
                Aviso("$n carreta(s) entregue(s). Elas não saem mais na exportação daqui.")
                Spacer(Modifier.height(4.dp))
                Button(onClick = { vm.limpar() }) { Text("ok") }
            }

            estado.recebidas?.let { n ->
                Spacer(Modifier.height(12.dp))
                Aviso(
                    if (n == 0) "Nada novo: essas carretas já estavam neste aparelho."
                    else "$n carreta(s) recebida(s) e já no pátio."
                )
                Spacer(Modifier.height(4.dp))
                Button(onClick = { vm.limpar() }) { Text("ok") }
            }

            estado.erro?.let { erro ->
                Spacer(Modifier.height(12.dp))
                Aviso(erro, erro = true)
                Spacer(Modifier.height(4.dp))
                Button(onClick = { vm.limpar() }) { Text("ok") }
            }

            Spacer(Modifier.height(28.dp))
        }
    }

    if (lendoQr) {
        LeitorQr(
            aoFechar = { lendoQr = false },
            aoCompletar = { texto ->
                lendoQr = false
                vm.receberTexto(texto)
            }
        )
    }
}
