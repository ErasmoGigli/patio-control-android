package com.patiocontrol.ui.avaria

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patiocontrol.core.Texto
import com.patiocontrol.domain.Avaria
import com.patiocontrol.ui.carreta.CarretaViewModel
import com.patiocontrol.ui.componentes.Aviso
import com.patiocontrol.ui.componentes.BarraTopo
import com.patiocontrol.ui.componentes.CampoSelecao
import com.patiocontrol.ui.componentes.CampoTexto
import com.patiocontrol.ui.componentes.Cartao
import com.patiocontrol.ui.componentes.EspacoVazio
import com.patiocontrol.ui.componentes.ItemLinha
import com.patiocontrol.ui.componentes.RotuloBloco
import com.patiocontrol.ui.componentes.Selo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Avarias da carga.
 *
 * A avaria entra aqui porque ela explica tempo: palete tombado dentro da
 * carreta atrasa descarga e carregamento. O painel compara a média das
 * carretas com avaria contra as sem avaria, e essa diferença é o custo.
 */
@Composable
fun AvariaListaScreen(
    vm: CarretaViewModel,
    aoVoltar: () -> Unit,
    aoAbrirItem: (Long) -> Unit
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val carreta = estado.carreta

    if (carreta == null) {
        Aviso("Carreta não encontrada.", erro = true, modifier = Modifier.padding(14.dp))
        return
    }

    Scaffold(
        topBar = { BarraTopo("AVARIAS", carreta.placa, aoVoltar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (carreta.avarias.isEmpty()) {
                EspacoVazio("Nenhuma avaria registrada nesta carreta.")
            } else {
                RotuloBloco("Itens avariados")
                Cartao {
                    carreta.avarias.forEach { avaria ->
                        ItemLinha(
                            titulo = avaria.item.ifBlank { "sem item" },
                            valor = if (avaria.quantidade == null) "não mensurado"
                            else "${Texto.numeroBr(avaria.quantidade)} ${avaria.unidade}".trim(),
                            aoTocar = { aoAbrirItem(avaria.id) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { aoAbrirItem(0L) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("+ ADICIONAR ITEM AVARIADO") }

            Spacer(Modifier.height(16.dp))
            Cartao {
                RotuloBloco("Como isso é usado")
                Text(
                    "O painel compara a descarga e a permanência das carretas com avaria " +
                        "contra as sem avaria. A diferença é o tempo que a avaria custa, " +
                        "por origem e por transportadora.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

/**
 * Item de avaria. A quantidade é opcional de propósito: quando o palete
 * chega tombado dentro da carreta, muitas vezes não dá para contar na hora,
 * e obrigar um número faria o conferente inventar um.
 */
@Composable
fun AvariaItemScreen(
    vm: CarretaViewModel,
    itemId: Long,
    aoVoltar: () -> Unit
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val carreta = estado.carreta
    val config = estado.configuracao
    val existente = carreta?.avarias?.firstOrNull { it.id == itemId }
    val fotos = vm.fotos()

    var item by remember(itemId) { mutableStateOf(existente?.item.orEmpty()) }
    var quantidade by remember(itemId) { mutableStateOf(Texto.numeroBr(existente?.quantidade)) }
    var unidade by remember(itemId) { mutableStateOf(existente?.unidade.orEmpty()) }
    var origem by remember(itemId) { mutableStateOf(existente?.origem.orEmpty()) }
    var observacao by remember(itemId) { mutableStateOf(existente?.observacao.orEmpty()) }
    var foto by remember(itemId) { mutableStateOf(existente?.foto.orEmpty()) }

    // Nome reservado antes de abrir a câmera: o sistema grava direto neste
    // arquivo, então ele precisa existir como destino antes da chamada.
    var fotoPendente by remember(itemId) { mutableStateOf("") }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { sucesso ->
        // Alguns aparelhos devolvem RESULT_CANCELED mesmo tendo gravado.
        // Quem decide é o arquivo em disco, não o código de retorno.
        if (fotoPendente.isNotBlank() && fotos.existe(fotoPendente)) {
            foto = fotoPendente
        } else if (!sucesso) {
            fotos.apagar(fotoPendente)
        }
        fotoPendente = ""
    }

    // Reservar o nome e chamar a câmera do sistema; só depois da permissão.
    fun abrirCamera() {
        val nome = vm.nomeDeFotoNova()
        fotoPendente = nome
        camera.launch(fotos.uriPara(nome))
    }

    // O app passou a declarar CAMERA por causa do leitor de QR, e o Android
    // então exige que ela esteja concedida até para chamar a câmera do
    // sistema — o que antes, sem a permissão declarada, não era preciso.
    var cameraNegada by remember { mutableStateOf(false) }
    val contexto = LocalContext.current
    val pedirCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida ->
        cameraNegada = !concedida
        if (concedida) abrirCamera()
    }

    // Lado da miniatura em pixels reais deste aparelho, para pedir ao
    // decodificador só o que cabe nela.
    val ladoMiniatura = with(LocalDensity.current) { LADO_MINIATURA.roundToPx() }

    val miniatura by produceState(Miniatura(), foto, ladoMiniatura) {
        value = withContext(Dispatchers.IO) {
            if (foto.isBlank() || !fotos.existe(foto)) Miniatura(pronta = true)
            else Miniatura(miniaturaDe(fotos.arquivo(foto), ladoMiniatura), pronta = true)
        }
    }

    Scaffold(
        topBar = {
            BarraTopo(
                if (itemId == 0L) "NOVA AVARIA" else "EDITAR AVARIA",
                carreta?.placa.orEmpty(),
                aoVoltar
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            CampoSelecao("Item avariado *", item, config.itensAvaria, { item = it }, permiteVazio = false)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoTexto(
                    "Quantidade (opcional)",
                    quantidade,
                    { quantidade = it },
                    numerico = true,
                    modifier = Modifier.weight(1f),
                    apoio = "vazio se não dá para medir"
                )
                CampoSelecao("Unidade", unidade, config.unidades, { unidade = it }, modifier = Modifier.weight(1f))
            }
            CampoSelecao("Origem aparente *", origem, config.origensAvaria, { origem = it }, permiteVazio = false)
            CampoTexto("Observação", observacao, { observacao = it }, linhas = 3)

            RotuloBloco("Foto")
            Cartao {
                // Enquanto o disco não responde, a foto conta como anexada:
                // o nome dela já está na avaria. Só quando a leitura termina
                // sem imagem é que a tela volta a oferecer tirar outra.
                if (foto.isNotBlank() && (!miniatura.pronta || miniatura.bitmap != null)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val molde = Modifier
                            .size(LADO_MINIATURA)
                            .clip(RoundedCornerShape(9.dp))
                        val bitmap = miniatura.bitmap
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Foto da avaria",
                                contentScale = ContentScale.Crop,
                                modifier = molde
                            )
                        } else {
                            // A foto já está anexada; só os pixels ainda não
                            // chegaram do disco. Mostrar o espaço dela evita
                            // piscar o botão de tirar foto por um instante.
                            Box(molde.background(MaterialTheme.colorScheme.surfaceVariant))
                        }
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Selo("foto anexada", MaterialTheme.colorScheme.tertiary)
                            Text(
                                foto,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = {
                            fotos.apagar(foto)
                            foto = ""
                        }) { Text("Trocar") }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            val concedida = ContextCompat.checkSelfPermission(
                                contexto, Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (concedida) abrirCamera()
                            else pedirCamera.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("TIRAR FOTO DA AVARIA") }
                }
                if (cameraNegada) {
                    Spacer(Modifier.height(8.dp))
                    Aviso(
                        "Sem a permissão de câmera não dá para fotografar a avaria. " +
                            "O registro pode ser salvo sem foto.",
                        erro = true
                    )
                }
            }
            Text(
                "A foto fica no aparelho e vai junto na exportação, dentro da pasta fotos/. " +
                    "A planilha guarda só o nome do arquivo.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    vm.salvarAvaria(
                        Avaria(
                            id = itemId,
                            carretaId = carreta?.id.orEmpty(),
                            item = item,
                            quantidade = Texto.numeroDe(quantidade),
                            unidade = unidade,
                            origem = origem,
                            observacao = observacao.trim(),
                            foto = foto
                        )
                    )
                    aoVoltar()
                },
                enabled = item.isNotBlank() && origem.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("SALVAR", fontWeight = FontWeight.Bold)
            }

            if (existente != null) {
                OutlinedButton(
                    onClick = { vm.apagarAvaria(existente); aoVoltar() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Apagar este item") }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

/** Lado do quadrado da foto na tela de avaria. */
private val LADO_MINIATURA = 72.dp

/**
 * A miniatura e o aviso de que o disco já respondeu.
 *
 * Sem o [pronta], um bitmap nulo significaria duas coisas diferentes — "a
 * leitura ainda não voltou" e "não há foto" — e a tela trocaria o quadrado
 * pelo botão de tirar foto no meio do caminho.
 */
private data class Miniatura(val bitmap: Bitmap? = null, val pronta: Boolean = false)

/**
 * Lê a foto já reduzida ao tamanho em que ela vai aparecer.
 *
 * A câmera de um celular atual grava 12 megapixels ou mais. Decodificar
 * isso inteiro custa perto de 50 MB de memória para desenhar um quadrado
 * de 72 dp — e antes esse trabalho ainda acontecia durante a composição,
 * na thread que desenha. Aparelho de pátio não tem essa folga.
 *
 * A primeira passada só mede o arquivo, sem alocar pixels. A segunda pede
 * ao decodificador a maior redução por potência de dois que ainda cobre o
 * lado alvo, que é o único fator que o Android aceita.
 */
private fun miniaturaDe(arquivo: File, ladoAlvo: Int): Bitmap? {
    if (ladoAlvo <= 0) return null

    val medida = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    runCatching { BitmapFactory.decodeFile(arquivo.absolutePath, medida) }
    val maior = maxOf(medida.outWidth, medida.outHeight)
    if (maior <= 0) return null

    var amostra = 1
    while (maior / (amostra * 2) >= ladoAlvo) amostra *= 2

    val opcoes = BitmapFactory.Options().apply {
        inSampleSize = amostra
        // Miniatura opaca de 72 dp não precisa de canal alfa; metade da
        // memória por pixel e nenhuma diferença visível nesse tamanho.
        inPreferredConfig = Bitmap.Config.RGB_565
    }
    return runCatching { BitmapFactory.decodeFile(arquivo.absolutePath, opcoes) }.getOrNull()
}
