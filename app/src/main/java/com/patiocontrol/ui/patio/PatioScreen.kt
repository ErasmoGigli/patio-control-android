package com.patiocontrol.ui.patio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.patiocontrol.core.Tempo
import com.patiocontrol.domain.Carreta
import com.patiocontrol.domain.Indicadores
import com.patiocontrol.domain.Situacao
import com.patiocontrol.ui.Rotas
import com.patiocontrol.ui.componentes.Aviso
import com.patiocontrol.ui.componentes.BarraTopo
import com.patiocontrol.ui.componentes.Cartao
import com.patiocontrol.ui.componentes.EspacoVazio
import com.patiocontrol.ui.componentes.Metrica
import com.patiocontrol.ui.componentes.Selo
import com.patiocontrol.ui.corDaSituacao
import com.patiocontrol.ui.repositorio

@Composable
fun PatioScreen(
    aoAbrirNova: () -> Unit,
    aoAbrirCarreta: (String) -> Unit,
    aoIr: (String) -> Unit,
    vm: PatioViewModel = viewModel(factory = viewModelFactory {
        initializer { PatioViewModel(repositorio) }
    })
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            BarraTopo(
                titulo = "PÁTIO",
                subtitulo = listOf(estado.configuracao.aparelho, estado.configuracao.turno)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Filled.Inventory, contentDescription = null) },
                    label = { Text("Pátio") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { aoIr(Rotas.ENCERRADAS) },
                    icon = { Icon(Icons.Filled.Inventory, contentDescription = null) },
                    label = { Text("Encerradas") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { aoIr(Rotas.EXPORTAR) },
                    icon = { Icon(Icons.Filled.Upload, contentDescription = null) },
                    label = { Text("Exportar") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { aoIr(Rotas.PASSAGEM) },
                    icon = { Icon(Icons.Filled.SwapHoriz, contentDescription = null) },
                    label = { Text("Turno") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { aoIr(Rotas.AJUSTES) },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Ajustes") }
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = aoAbrirNova,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("NOVA CARRETA") }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
        ) {
            Cartao(Modifier.padding(top = 12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Metrica(estado.carretas.size.toString(), "no pátio")
                    Metrica(estado.emOperacao.toString(), "em operação")
                    Metrica(estado.aExportar.toString(), "a exportar")
                }
            }

            if (estado.aparelhoSemCodigo) {
                Spacer(Modifier.height(10.dp))
                Aviso("Aparelho ainda sem código. Abra Ajustes e confirme antes do primeiro registro.")
            }

            if (estado.carretas.isEmpty() && !estado.carregando) {
                EspacoVazio("Nenhuma carreta no pátio.\nToque em NOVA CARRETA quando a primeira chegar.")
            } else {
                LazyColumn(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
                ) {
                    items(estado.carretas, key = { it.carreta.id }) { linha ->
                        CartaoCarreta(linha) { aoAbrirCarreta(linha.carreta.id) }
                    }
                }
            }
        }
    }
}

@Composable
fun EncerradasScreen(
    aoVoltar: () -> Unit,
    aoAbrirCarreta: (String) -> Unit,
    vm: PatioViewModel = viewModel(factory = viewModelFactory {
        initializer { PatioViewModel(repositorio) }
    })
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            BarraTopo(
                titulo = "ENCERRADAS",
                subtitulo = "${estado.encerradas.size} carretas",
                aoVoltar = aoVoltar
            )
        }
    ) { padding ->
        if (estado.encerradas.isEmpty()) {
            EspacoVazio("Nenhuma carreta encerrada ainda.")
            return@Scaffold
        }
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
        ) {
            items(estado.encerradas, key = { it.carreta.id }) { linha ->
                CartaoCarreta(linha) { aoAbrirCarreta(linha.carreta.id) }
            }
        }
    }
}

@Composable
private fun CartaoCarreta(linha: LinhaCarreta, aoTocar: () -> Unit) {
    val c = linha.carreta
    Cartao(Modifier.clickable { aoTocar() }) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    c.placa.ifBlank { "SEM PLACA" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    listOf(c.transportadora, c.origem).filter { it.isNotBlank() }
                        .joinToString(" · ").ifBlank { "—" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                Tempo.hm(linha.indicadores.permanencia),
                style = MaterialTheme.typography.titleLarge
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Selo(linha.situacao.rotulo, corDaSituacao(linha.situacao))
            if (c.temAvaria) {
                Selo("avaria", MaterialTheme.colorScheme.error)
            }
            if (c.encerrada && c.exportadaEm == null) {
                Selo("a exportar", MaterialTheme.colorScheme.secondary)
            }
        }
        // Em linha própria: com a situação longa, mais um selo na linha de
        // cima sairia da tela num aparelho estreito.
        if (linha.estourosDeMeta > 0) {
            Spacer(Modifier.height(6.dp))
            Selo(
                if (linha.estourosDeMeta == 1) "1 meta estourada"
                else "${linha.estourosDeMeta} metas estouradas",
                MaterialTheme.colorScheme.error
            )
        }
    }
}

/** Carreta já acompanhada dos números que a lista mostra. */
data class LinhaCarreta(
    val carreta: Carreta,
    val indicadores: Indicadores,
    val situacao: Situacao,
    /** Quantas metas de tempo esta carreta já passou; 0 quando não há meta. */
    val estourosDeMeta: Int = 0
)
