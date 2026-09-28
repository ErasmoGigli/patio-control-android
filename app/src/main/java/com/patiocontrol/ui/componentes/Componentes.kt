package com.patiocontrol.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.patiocontrol.core.Tempo

/** Barra de topo padrão das telas internas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraTopo(
    titulo: String,
    subtitulo: String? = null,
    aoVoltar: (() -> Unit)? = null,
    acoes: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(titulo, style = MaterialTheme.typography.titleLarge)
                if (!subtitulo.isNullOrBlank()) {
                    Text(
                        subtitulo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                    )
                }
            }
        },
        navigationIcon = {
            if (aoVoltar != null) {
                IconButton(onClick = aoVoltar) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                }
            }
        },
        actions = { acoes() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

/** Rótulo de seção, em versalete. */
@Composable
fun RotuloBloco(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 16.dp, bottom = 6.dp)
    )
}

@Composable
fun Cartao(
    modifier: Modifier = Modifier,
    conteudo: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp), content = conteudo)
    }
}

/** Número grande com legenda, o bloco que o conferente lê de relance. */
@Composable
fun Metrica(
    valor: String,
    legenda: String,
    cor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(
            valor,
            style = MaterialTheme.typography.headlineMedium,
            color = cor
        )
        Text(
            legenda,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MetricaTempo(minutos: Int?, legenda: String, modifier: Modifier = Modifier) =
    Metrica(Tempo.hm(minutos), legenda, modifier = modifier)

/** Selo colorido de situação. */
@Composable
fun Selo(texto: String, cor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(cor.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.labelSmall,
            color = cor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CampoTexto(
    rotulo: String,
    valor: String,
    aoMudar: (String) -> Unit,
    modifier: Modifier = Modifier,
    maiusculas: Boolean = false,
    numerico: Boolean = false,
    linhas: Int = 1,
    apoio: String? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { novo -> aoMudar(if (maiusculas) novo.uppercase() else novo) },
        label = { Text(rotulo) },
        supportingText = if (apoio == null) null else ({ Text(apoio) }),
        singleLine = linhas == 1,
        minLines = linhas,
        keyboardOptions = KeyboardOptions(
            capitalization = if (maiusculas) KeyboardCapitalization.Characters else KeyboardCapitalization.Sentences,
            keyboardType = if (numerico) KeyboardType.Decimal else KeyboardType.Text
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Seleção de um cadastro. Lista vazia continua exibindo o campo, para que o
 * conferente veja que falta cadastrar em vez de encontrar um campo sumido.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoSelecao(
    rotulo: String,
    valor: String,
    opcoes: List<String>,
    aoSelecionar: (String) -> Unit,
    modifier: Modifier = Modifier,
    permiteVazio: Boolean = true
) {
    var aberto by remember { mutableStateOf(false) }

    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            label = { Text(rotulo) },
            trailingIcon = {
                Icon(Icons.Filled.ExpandMore, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth()
        )
        // Camada transparente por cima: o campo é somente leitura, então o
        // toque precisa ser capturado aqui para abrir a lista.
        Box(
            Modifier
                .matchParentSize()
                .clickable { aberto = true }
        )
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            if (permiteVazio) {
                DropdownMenuItem(
                    text = { Text("— não informado —") },
                    onClick = { aoSelecionar(""); aberto = false }
                )
            }
            opcoes.forEach { opcao ->
                DropdownMenuItem(
                    text = { Text(opcao) },
                    onClick = { aoSelecionar(opcao); aberto = false }
                )
            }
        }
    }
}

/** Linha de lista com título à esquerda e valor à direita. */
@Composable
fun ItemLinha(
    titulo: String,
    valor: String = "",
    aoTocar: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (aoTocar != null) Modifier.clickable { aoTocar() } else Modifier)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(titulo, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (valor.isNotBlank()) {
            Text(
                valor,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Faixa de aviso; [erro] troca o tom para o de impedimento. */
@Composable
fun Aviso(texto: String, erro: Boolean = false, modifier: Modifier = Modifier) {
    val cor = if (erro) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
    Box(
        modifier
            .fillMaxWidth()
            .background(cor.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(texto, style = MaterialTheme.typography.bodyLarge, color = cor)
    }
}

@Composable
fun EspacoVazio(mensagem: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            mensagem,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PontoDeCor(cor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(10.dp)
            .background(cor, RoundedCornerShape(5.dp))
    )
}
