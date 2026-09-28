package com.patiocontrol.ui.avaria

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    val miniatura = remember(foto) {
        if (foto.isNotBlank() && fotos.existe(foto)) {
            runCatching { BitmapFactory.decodeFile(fotos.arquivo(foto).absolutePath) }.getOrNull()
        } else null
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
                if (miniatura != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            bitmap = miniatura.asImageBitmap(),
                            contentDescription = "Foto da avaria",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(9.dp))
                        )
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
                            val nome = vm.nomeDeFotoNova()
                            fotoPendente = nome
                            camera.launch(fotos.uriPara(nome))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("TIRAR FOTO DA AVARIA") }
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
