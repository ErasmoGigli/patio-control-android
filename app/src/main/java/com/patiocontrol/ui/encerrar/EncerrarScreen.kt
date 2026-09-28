package com.patiocontrol.ui.encerrar

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patiocontrol.core.Tempo
import com.patiocontrol.domain.Marco
import com.patiocontrol.ui.componentes.Aviso
import com.patiocontrol.ui.componentes.BarraTopo
import com.patiocontrol.ui.componentes.Cartao
import com.patiocontrol.ui.componentes.ItemLinha
import com.patiocontrol.ui.componentes.RotuloBloco

/**
 * Última conferência antes de a carreta sair do pátio.
 *
 * Pendência trava, alerta só avisa. A diferença importa: travar o que é
 * apenas incomum faria o conferente procurar como burlar.
 */
@Composable
fun EncerrarScreen(
    vm: com.patiocontrol.ui.carreta.CarretaViewModel,
    aoVoltar: () -> Unit,
    aoEncerrado: () -> Unit
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val carreta = estado.carreta

    if (carreta == null) {
        Aviso("Carreta não encontrada.", erro = true, modifier = Modifier.padding(14.dp))
        return
    }

    Scaffold(
        topBar = { BarraTopo("ENCERRAR", carreta.placa, aoVoltar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
                .verticalScroll(rememberScrollState())
        ) {
            RotuloBloco("Resumo")
            Cartao {
                ItemLinha("Permanência", Tempo.hm(estado.indicadores.permanencia))
                ItemLinha("Descarga", Tempo.hm(estado.indicadores.descarga))
                ItemLinha("Carregamento de AG", Tempo.hm(estado.indicadores.carregamentoAg))
                ItemLinha("Ciclo total", Tempo.hm(estado.indicadores.ciclo))
                ItemLinha("Tempo produtivo", Tempo.percentual(estado.indicadores.produtivo))
                ItemLinha("Situação", estado.situacao.rotulo)
            }

            if (estado.pendencias.isNotEmpty()) {
                RotuloBloco("Pendências que impedem o encerramento")
                estado.pendencias.forEach { pendencia ->
                    Aviso(pendencia.mensagem, erro = true)
                    Spacer(Modifier.height(6.dp))
                }
            }

            if (estado.alertas.isNotEmpty()) {
                RotuloBloco("Avisos")
                estado.alertas.forEach { alerta ->
                    Aviso(alerta)
                    Spacer(Modifier.height(6.dp))
                }
            }

            val faltaSaida = carreta.horario(Marco.SAIDA) == null
            if (faltaSaida) {
                Spacer(Modifier.height(10.dp))
                Aviso("A saída ainda não foi marcada. A carreta pode ser encerrada assim mesmo, mas o ciclo total ficará em branco na planilha.")
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { vm.encerrar(aoEncerrado) },
                enabled = estado.podeEncerrar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    if (estado.podeEncerrar) "ENCERRAR E TIRAR DO PÁTIO" else "RESOLVA AS PENDÊNCIAS",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "Depois de encerrada, a carreta entra na fila de exportação e sai no próximo pacote.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}
