package com.patiocontrol.ui.ajustes

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.patiocontrol.data.repository.PatioRepository
import com.patiocontrol.domain.Configuracao
import com.patiocontrol.domain.Metas
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.patiocontrol.ui.componentes.Aviso
import com.patiocontrol.ui.componentes.BarraTopo
import com.patiocontrol.ui.componentes.CampoSelecao
import com.patiocontrol.ui.componentes.CampoTexto
import com.patiocontrol.ui.componentes.Cartao
import com.patiocontrol.ui.componentes.ItemLinha
import com.patiocontrol.ui.componentes.RotuloBloco
import com.patiocontrol.ui.repositorio

class AjustesViewModel(private val repositorio: PatioRepository) : ViewModel() {

    val configuracao: StateFlow<Configuracao> = repositorio.configuracao
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Configuracao())

    fun salvar(config: Configuracao) = viewModelScope.launch {
        repositorio.salvarConfiguracao(config)
    }
}

/**
 * Cadastros e metas.
 *
 * Tudo que uma unidade tem de diferente da outra mora aqui, e não no
 * código: transportadora nova, origem nova, tipo de ativo novo. O
 * conferente inclui e usa no mesmo turno, sem esperar uma versão do app.
 */
@Composable
fun AjustesScreen(
    aoVoltar: () -> Unit,
    vm: AjustesViewModel = viewModel(factory = viewModelFactory {
        initializer { AjustesViewModel(repositorio) }
    })
) {
    val config by vm.configuracao.collectAsStateWithLifecycle()

    var listaEmEdicao by remember { mutableStateOf<String?>(null) }

    // Campos de texto guardam estado local e salvam por botão. Salvar a cada
    // tecla faria o valor voltar do DataStore no meio da digitação e o cursor
    // pular para o fim da linha.
    var aparelho by remember(config.aparelho) { mutableStateOf(config.aparelho) }
    var unidade by remember(config.unidade) { mutableStateOf(config.unidade) }
    var responsavel by remember(config.responsavel) { mutableStateOf(config.responsavel) }
    var metas by remember(config.metas) { mutableStateOf(config.metas) }

    Scaffold(
        topBar = { BarraTopo("AJUSTES", config.aparelho, aoVoltar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RotuloBloco("Este aparelho")
            CampoTexto("Código do aparelho", aparelho, { aparelho = it.uppercase() }, maiusculas = true)
            CampoTexto("Unidade", unidade, { unidade = it })
            CampoTexto("Conferente responsável", responsavel, { responsavel = it })
            CampoSelecao("Turno atual", config.turno, config.turnos, { vm.salvar(config.copy(turno = it)) }, permiteVazio = false)
            OutlinedButton(
                onClick = {
                    vm.salvar(
                        config.copy(
                            aparelho = aparelho.trim(),
                            unidade = unidade.trim(),
                            responsavel = responsavel.trim()
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Salvar dados do aparelho") }

            Aviso(
                "O código do aparelho é o prefixo do identificador de cada carreta. " +
                    "Dois aparelhos com o mesmo código geram identificadores repetidos na planilha."
            )

            RotuloBloco("Cadastros")
            Cartao {
                ItemLinha("Transportadoras", "${config.transportadoras.size}", { listaEmEdicao = "transportadoras" })
                ItemLinha("Origens", "${config.origens.size}", { listaEmEdicao = "origens" })
                ItemLinha("Destinos", "${config.destinos.size}", { listaEmEdicao = "destinos" })
                ItemLinha("Tipos de veículo", "${config.veiculos.size}", { listaEmEdicao = "veiculos" })
                ItemLinha("Turnos", "${config.turnos.size}", { listaEmEdicao = "turnos" })
                ItemLinha("Produtos", "${config.produtos.size}", { listaEmEdicao = "produtos" })
                ItemLinha("Unidades de medida", "${config.unidades.size}", { listaEmEdicao = "unidades" })
                ItemLinha("Motivos de saída sem AG", "${config.motivosSemAg.size}", { listaEmEdicao = "motivos" })
                ItemLinha("Itens de avaria", "${config.itensAvaria.size}", { listaEmEdicao = "itensAvaria" })
                ItemLinha("Origens de avaria", "${config.origensAvaria.size}", { listaEmEdicao = "origensAvaria" })
            }

            RotuloBloco("Metas de tempo (minutos)")
            Cartao {
                CampoMeta("Espera para descarga", metas.esperaDescarga) { metas = metas.copy(esperaDescarga = it) }
                CampoMeta("Descarga", metas.descarga) { metas = metas.copy(descarga = it) }
                CampoMeta("Espera de AG", metas.esperaAg) { metas = metas.copy(esperaAg = it) }
                CampoMeta("Carregamento de AG", metas.carregamentoAg) { metas = metas.copy(carregamentoAg = it) }
                CampoMeta("Liberação", metas.liberacao) { metas = metas.copy(liberacao = it) }
                CampoMeta("Permanência", metas.permanencia) { metas = metas.copy(permanencia = it) }
                CampoMeta("Ciclo total", metas.ciclo) { metas = metas.copy(ciclo = it) }
                // Este é o único que é piso, e não teto: a fração do tempo
                // de pátio que foi descarga ou carregamento de fato.
                CampoMeta("Tempo produtivo mínimo (%)", metas.produtivo?.let { (it * 100).toInt() }) { pct ->
                    metas = metas.copy(produtivo = pct?.coerceIn(0, 100)?.let { it / 100.0 })
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { vm.salvar(config.copy(metas = metas)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Salvar metas") }
            }
            Text(
                "Meta em branco não gera alerta. Deixe assim até a operação ter uma " +
                    "referência própria — meta chutada só ensina o time a ignorar alerta. " +
                    "Com meta preenchida, a carreta que passar do tempo recebe o aviso na " +
                    "tela dela e o selo na lista do pátio.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(28.dp))
        }
    }

    listaEmEdicao?.let { chave ->
        EditorDeLista(
            titulo = tituloDaLista(chave),
            valores = listaPorChave(config, chave),
            aoFechar = { listaEmEdicao = null },
            aoSalvar = { novos ->
                vm.salvar(configComLista(config, chave, novos))
                listaEmEdicao = null
            }
        )
    }
}

@Composable
private fun CampoMeta(rotulo: String, valor: Int?, aoMudar: (Int?) -> Unit) {
    CampoTexto(
        rotulo,
        valor?.toString().orEmpty(),
        { texto -> aoMudar(texto.filter { c -> c.isDigit() }.toIntOrNull()) },
        numerico = true
    )
}

/** Editor genérico: uma linha por item, que é o formato que o operador entende. */
@Composable
private fun EditorDeLista(
    titulo: String,
    valores: List<String>,
    aoFechar: () -> Unit,
    aoSalvar: (List<String>) -> Unit
) {
    var texto by remember { mutableStateOf(valores.joinToString("\n")) }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(titulo) },
        text = {
            Column {
                Text(
                    "Um item por linha.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                CampoTexto("Itens", texto, { texto = it }, linhas = 8)
            }
        },
        confirmButton = {
            Button(onClick = {
                aoSalvar(texto.split('\n').map { it.trim() }.filter { it.isNotBlank() })
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } }
    )
}

private fun tituloDaLista(chave: String): String = when (chave) {
    "transportadoras" -> "Transportadoras"
    "origens" -> "Origens"
    "destinos" -> "Destinos"
    "veiculos" -> "Tipos de veículo"
    "turnos" -> "Turnos"
    "produtos" -> "Produtos"
    "unidades" -> "Unidades de medida"
    "motivos" -> "Motivos de saída sem AG"
    "itensAvaria" -> "Itens de avaria"
    "origensAvaria" -> "Origens de avaria"
    else -> "Cadastro"
}

private fun listaPorChave(config: Configuracao, chave: String): List<String> = when (chave) {
    "transportadoras" -> config.transportadoras
    "origens" -> config.origens
    "destinos" -> config.destinos
    "veiculos" -> config.veiculos
    "turnos" -> config.turnos
    "produtos" -> config.produtos
    "unidades" -> config.unidades
    "motivos" -> config.motivosSemAg
    "itensAvaria" -> config.itensAvaria
    "origensAvaria" -> config.origensAvaria
    else -> emptyList()
}

private fun configComLista(config: Configuracao, chave: String, valores: List<String>): Configuracao =
    when (chave) {
        "transportadoras" -> config.copy(transportadoras = valores)
        "origens" -> config.copy(origens = valores)
        "destinos" -> config.copy(destinos = valores)
        "veiculos" -> config.copy(veiculos = valores)
        "turnos" -> config.copy(turnos = valores)
        "produtos" -> config.copy(produtos = valores)
        "unidades" -> config.copy(unidades = valores)
        "motivos" -> config.copy(motivosSemAg = valores)
        "itensAvaria" -> config.copy(itensAvaria = valores)
        "origensAvaria" -> config.copy(origensAvaria = valores)
        else -> config
    }
