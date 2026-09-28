package com.patiocontrol.ui.ag

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patiocontrol.core.Texto
import com.patiocontrol.domain.AtivoGiro
import com.patiocontrol.domain.RespostaAg
import com.patiocontrol.ui.carreta.CarretaViewModel
import com.patiocontrol.ui.componentes.Aviso
import com.patiocontrol.ui.componentes.BarraTopo
import com.patiocontrol.ui.componentes.CampoSelecao
import com.patiocontrol.ui.componentes.CampoTexto
import com.patiocontrol.ui.componentes.Cartao
import com.patiocontrol.ui.componentes.EspacoVazio
import com.patiocontrol.ui.componentes.ItemLinha
import com.patiocontrol.ui.componentes.Metrica
import com.patiocontrol.ui.componentes.RotuloBloco

/**
 * Ativos de giro da carreta.
 *
 * Cada tipo ocupa a sua própria linha e nunca se mistura com outro: trinta
 * paletes de litrinho e dez barris não somam quarenta de coisa nenhuma. É
 * a mesma regra da aba AG_DETALHE da planilha.
 */
@Composable
fun AgListaScreen(
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

    var justificativa by remember(carreta.id) { mutableStateOf(carreta.justificativaSemAg) }
    var observacao by remember(carreta.id) { mutableStateOf(carreta.observacaoCarregamento) }

    Scaffold(
        topBar = { BarraTopo("ATIVOS DE GIRO", carreta.placa, aoVoltar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
                .verticalScroll(rememberScrollState())
        ) {
            RotuloBloco("A carreta saiu carregada com AG?")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { vm.responderAg(RespostaAg.SIM) },
                    modifier = Modifier.weight(1f),
                    colors = if (carreta.respostaAg == RespostaAg.SIM) ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors()
                ) { Text("SIM") }
                Button(
                    onClick = { vm.responderAg(RespostaAg.NAO) },
                    modifier = Modifier.weight(1f),
                    colors = if (carreta.respostaAg == RespostaAg.NAO) ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors()
                ) { Text("NÃO") }
            }

            when (carreta.respostaAg) {
                RespostaAg.NAO -> {
                    RotuloBloco("Justificativa obrigatória")
                    CampoSelecao(
                        "Motivo da saída sem AG",
                        justificativa,
                        estado.configuracao.motivosSemAg,
                        {
                            justificativa = it
                            vm.salvar(carreta.copy(justificativaSemAg = it))
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    Aviso("Sem AG é informação de gestão, não é erro. O que não pode é sair sem explicação — é isso que o painel cruza por transportadora e por origem.")
                }

                RespostaAg.SIM -> {
                    RotuloBloco("Tipos carregados")
                    if (carreta.ativosGiro.isEmpty()) {
                        EspacoVazio("Nenhum tipo lançado ainda.")
                    } else {
                        Cartao {
                            carreta.ativosGiro.forEach { ag ->
                                ItemLinha(
                                    titulo = ag.tipo.ifBlank { "sem tipo" },
                                    valor = "${Texto.numeroBr(ag.quantidade)} ${ag.unidade}".trim(),
                                    aoTocar = { aoAbrirItem(ag.id) }
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Cartao {
                            RotuloBloco("Totais por unidade")
                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                carreta.ativosGiro
                                    .filter { it.quantidade != null }
                                    .groupBy { it.unidade }
                                    .forEach { (unidade, linhas) ->
                                        Metrica(
                                            Texto.numeroBr(linhas.sumOf { it.quantidade ?: 0.0 }),
                                            unidade.lowercase().ifBlank { "sem unidade" }
                                        )
                                    }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { aoAbrirItem(0L) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("+ ADICIONAR TIPO DE AG") }
                }

                RespostaAg.NAO_RESPONDIDO -> {
                    Spacer(Modifier.height(14.dp))
                    Aviso("Responda acima para liberar o lançamento.")
                }
            }

            RotuloBloco("Observação do carregamento")
            CampoTexto("Observação", observacao, { observacao = it }, linhas = 3)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { vm.salvar(carreta.copy(observacaoCarregamento = observacao.trim())) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Salvar observação") }

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
fun AgItemScreen(
    vm: CarretaViewModel,
    itemId: Long,
    aoVoltar: () -> Unit
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val carreta = estado.carreta
    val config = estado.configuracao
    val existente = carreta?.ativosGiro?.firstOrNull { it.id == itemId }

    var tipo by remember(itemId) { mutableStateOf(existente?.tipo.orEmpty()) }
    var quantidade by remember(itemId) { mutableStateOf(Texto.numeroBr(existente?.quantidade)) }
    var unidade by remember(itemId) { mutableStateOf(existente?.unidade.orEmpty()) }
    var documento by remember(itemId) { mutableStateOf(existente?.documento.orEmpty()) }
    var destino by remember(itemId) { mutableStateOf(existente?.destino.orEmpty()) }
    var observacao by remember(itemId) { mutableStateOf(existente?.observacao.orEmpty()) }

    // Escolher o tipo já traz a unidade que ele costuma usar; o conferente
    // só mexe quando é exceção.
    LaunchedEffect(tipo) {
        val padrao = config.tiposAg.firstOrNull { it.tipo == tipo }?.unidadePadrao
        if (padrao != null && unidade.isBlank()) unidade = padrao
    }

    Scaffold(
        topBar = {
            BarraTopo(
                if (itemId == 0L) "NOVO TIPO DE AG" else "EDITAR AG",
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
            CampoSelecao("Tipo de ativo *", tipo, config.tiposAg.map { it.tipo }, { tipo = it }, permiteVazio = false)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoTexto("Quantidade *", quantidade, { quantidade = it }, numerico = true, modifier = Modifier.weight(1f))
                CampoSelecao("Unidade", unidade, config.unidades, { unidade = it }, modifier = Modifier.weight(1f))
            }
            CampoTexto("Documento do AG", documento, { documento = it })
            CampoSelecao("Destino do AG", destino, config.destinos, { destino = it })
            CampoTexto("Observação", observacao, { observacao = it }, linhas = 2)

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    vm.salvarAtivoGiro(
                        AtivoGiro(
                            id = itemId,
                            carretaId = carreta?.id.orEmpty(),
                            tipo = tipo,
                            quantidade = Texto.numeroDe(quantidade),
                            unidade = unidade,
                            documento = documento.trim(),
                            destino = destino,
                            observacao = observacao.trim()
                        )
                    )
                    aoVoltar()
                },
                enabled = tipo.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("SALVAR", fontWeight = FontWeight.Bold)
            }

            if (existente != null) {
                OutlinedButton(
                    onClick = { vm.apagarAtivoGiro(existente); aoVoltar() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Apagar este tipo") }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
