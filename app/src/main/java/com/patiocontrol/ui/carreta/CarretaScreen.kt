package com.patiocontrol.ui.carreta

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patiocontrol.core.Tempo
import com.patiocontrol.domain.Marco
import com.patiocontrol.domain.RespostaAg
import com.patiocontrol.ui.componentes.Aviso
import com.patiocontrol.ui.componentes.BarraTopo
import com.patiocontrol.ui.componentes.Cartao
import com.patiocontrol.ui.componentes.ItemLinha
import com.patiocontrol.ui.componentes.Metrica
import com.patiocontrol.ui.componentes.RotuloBloco
import com.patiocontrol.ui.componentes.Selo
import com.patiocontrol.ui.corDaSituacao

/**
 * Tela central da operação: um botão grande para marcar o próximo horário,
 * os tempos já apurados e os atalhos para carga, AG e avarias.
 *
 * O botão é único de propósito. Uma lista de nove horários todos clicáveis
 * convida ao toque errado; um botão por vez torna a sequência óbvia e o
 * registro consistente.
 */
@Composable
fun CarretaScreen(
    vm: CarretaViewModel,
    aoVoltar: () -> Unit,
    aoIdentificacao: () -> Unit,
    aoCarga: () -> Unit,
    aoAg: () -> Unit,
    aoAvarias: () -> Unit,
    aoEncerrar: () -> Unit
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val mensagem by vm.mensagem.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmandoReabertura by remember { mutableStateOf(false) }

    LaunchedEffect(mensagem) {
        mensagem?.let {
            snackbar.showSnackbar(it)
            vm.limparMensagem()
        }
    }

    val carreta = estado.carreta

    Scaffold(
        topBar = {
            BarraTopo(
                titulo = carreta?.placa?.ifBlank { "SEM PLACA" } ?: "CARRETA",
                subtitulo = listOfNotNull(carreta?.transportadora, carreta?.origem)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                aoVoltar = aoVoltar
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->

        if (carreta == null) {
            Aviso("Carreta não encontrada.", erro = true, modifier = Modifier.padding(padding).padding(14.dp))
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))
            Selo(estado.situacao.rotulo, corDaSituacao(estado.situacao))

            Spacer(Modifier.height(12.dp))
            val proximo = carreta.proximoMarco
            if (proximo != null) {
                Button(
                    onClick = { vm.marcar(proximo) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(proximo.botao, fontWeight = FontWeight.Bold)
                        Text(
                            "agora",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            } else {
                Aviso("Todos os horários foram marcados. Falta encerrar a carreta.")
            }

            if (Marco.SEQUENCIA.any { carreta.horario(it) != null }) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { vm.desfazerUltimo() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Desfazer último horário")
                }
            }

            RotuloBloco("Tempos")
            Cartao {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Metrica(Tempo.hm(estado.indicadores.permanencia), "permanência")
                    Metrica(Tempo.hm(estado.indicadores.descarga), "descarga")
                    Metrica(Tempo.hm(estado.indicadores.carregamentoAg), "carga de AG")
                }
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Metrica(Tempo.hm(estado.indicadores.espera), "espera")
                    Metrica(Tempo.hm(estado.indicadores.operacional), "operacional")
                    Metrica(Tempo.percentual(estado.indicadores.produtivo), "produtivo")
                }
            }

            RotuloBloco("Horários")
            Cartao {
                Marco.entries.forEach { marco ->
                    ItemLinha(
                        titulo = marco.rotulo,
                        valor = Tempo.hora(carreta.horario(marco)).ifBlank { "—" }
                    )
                }
            }

            RotuloBloco("Registro")
            Cartao {
                ItemLinha("Identificação e programação", "editar", aoIdentificacao)
                ItemLinha(
                    "Carga recebida",
                    carreta.produto.ifBlank { "informar" },
                    aoCarga
                )
                ItemLinha(
                    "Ativos de giro",
                    when (carreta.respostaAg) {
                        RespostaAg.SIM -> "${carreta.ativosGiro.size} linha(s)"
                        RespostaAg.NAO -> "sem AG"
                        RespostaAg.NAO_RESPONDIDO -> "responder"
                    },
                    aoAg
                )
                ItemLinha(
                    "Avarias",
                    if (carreta.temAvaria) "${carreta.avarias.size} item(ns)" else "nenhuma",
                    aoAvarias
                )
            }

            if (estado.alertas.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                estado.alertas.forEach { alerta ->
                    Aviso(alerta)
                    Spacer(Modifier.height(6.dp))
                }
            }

            Spacer(Modifier.height(18.dp))

            // Carreta encerrada nao tem o que encerrar de novo. O que ela
            // precisa e de um caminho de volta: sem isso, um erro de
            // digitacao percebido depois do encerramento nao tem conserto
            // dentro do app.
            if (carreta?.encerrada == true) {
                OutlinedButton(
                    onClick = { confirmandoReabertura = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("REABRIR CARRETA")
                }
            } else {
                Button(
                    onClick = aoEncerrar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("ENCERRAR CARRETA", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    if (confirmandoReabertura) {
        AlertDialog(
            onDismissRequest = { confirmandoReabertura = false },
            title = { Text("Reabrir esta carreta?") },
            text = {
                Text(
                    "Ela volta para o pátio e entra de novo na fila de exportação. " +
                        "Se este registro já foi colado na planilha, colar o próximo " +
                        "pacote vai criar uma linha repetida — apague a linha antiga lá.\n\n" +
                        "Se esta carreta tinha sido entregue na passagem de turno, " +
                        "reabrir também desfaz a entrega e ela volta a ser deste aparelho."
                )
            },
            confirmButton = {
                Button(onClick = {
                    vm.reabrir()
                    confirmandoReabertura = false
                }) { Text("Reabrir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoReabertura = false }) { Text("Cancelar") }
            }
        )
    }
}
