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
import androidx.compose.material3.Button
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.patiocontrol.core.Tempo
import com.patiocontrol.core.Texto
import com.patiocontrol.domain.Carreta
import com.patiocontrol.domain.Marco
import com.patiocontrol.ui.componentes.Aviso
import com.patiocontrol.ui.componentes.BarraTopo
import com.patiocontrol.ui.componentes.CampoSelecao
import com.patiocontrol.ui.componentes.CampoTexto
import com.patiocontrol.ui.componentes.RotuloBloco
import com.patiocontrol.ui.patio.PatioViewModel
import com.patiocontrol.ui.repositorio
import java.time.LocalDate
import java.time.LocalTime

/**
 * Abertura da carreta. Só placa, transportadora e origem são obrigatórias:
 * na portaria não dá para parar o caminhão para preencher quinze campos, e
 * o resto pode ser completado enquanto ele descarrega.
 */
@Composable
fun NovaCarretaScreen(
    aoVoltar: () -> Unit,
    aoAbrir: (String) -> Unit,
    vm: PatioViewModel = viewModel(factory = viewModelFactory {
        initializer { PatioViewModel(repositorio) }
    })
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val config = estado.configuracao

    var placa by remember { mutableStateOf("") }
    var placaCavalo by remember { mutableStateOf("") }
    var transportadora by remember { mutableStateOf("") }
    var origem by remember { mutableStateOf("") }
    var destino by remember { mutableStateOf("") }
    var veiculo by remember { mutableStateOf("") }
    var motorista by remember { mutableStateOf("") }
    var documento by remember { mutableStateOf("") }
    var turno by remember { mutableStateOf("") }
    var programado by remember { mutableStateOf("") }

    LaunchedEffect(config.turno) {
        if (turno.isBlank()) turno = config.turno
    }

    val valido = placa.isNotBlank() && transportadora.isNotBlank() && origem.isNotBlank()

    Scaffold(
        topBar = { BarraTopo("ABRIR CARRETA", "a chegada é marcada ao salvar", aoVoltar) }
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
            CampoTexto("Placa da carreta *", placa, { placa = it }, maiusculas = true)
            CampoSelecao("Transportadora *", transportadora, config.transportadoras, { transportadora = it }, permiteVazio = false)
            CampoSelecao("Origem *", origem, config.origens, { origem = it }, permiteVazio = false)

            RotuloBloco("Complementos")
            CampoTexto("Placa do cavalo", placaCavalo, { placaCavalo = it }, maiusculas = true)
            CampoSelecao("Tipo de veículo", veiculo, config.veiculos, { veiculo = it })
            CampoSelecao("Destino", destino, config.destinos, { destino = it })
            CampoTexto("Motorista", motorista, { motorista = it })
            CampoTexto("Carga / mapa / documento", documento, { documento = it })
            CampoSelecao("Turno", turno, config.turnos, { turno = it }, permiteVazio = false)
            CampoTexto(
                "Horário programado",
                programado,
                { programado = it },
                apoio = "formato 24h, exemplo 07:30"
            )

            Spacer(Modifier.height(8.dp))
            Aviso("A chegada é registrada com a hora do aparelho no momento em que você salvar.")

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val base = Carreta(
                        id = "",
                        data = LocalDate.now(),
                        turno = turno,
                        transportadora = transportadora,
                        placa = placa.trim(),
                        placaCavalo = placaCavalo.trim(),
                        motorista = motorista.trim(),
                        veiculo = veiculo,
                        origem = origem,
                        destino = destino,
                        documento = documento.trim(),
                        responsavel = config.responsavel,
                        horarios = Tempo.horaDe(programado)
                            ?.let { mapOf(Marco.PROGRAMADO to it) }
                            ?: emptyMap()
                    )
                    vm.abrir(base, aoAbrir)
                },
                enabled = valido,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("ABRIR E MARCAR CHEGADA", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

/** Edição da identificação de uma carreta já aberta. */
@Composable
fun IdentificacaoScreen(vm: CarretaViewModel, aoVoltar: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val carreta = estado.carreta
    val config = estado.configuracao

    if (carreta == null) {
        Aviso("Carreta não encontrada.", erro = true, modifier = Modifier.padding(14.dp))
        return
    }

    var edicao by remember(carreta.id) { mutableStateOf(carreta) }
    var programado by remember(carreta.id) {
        mutableStateOf(Tempo.hora(carreta.horario(Marco.PROGRAMADO)))
    }

    Scaffold(
        topBar = { BarraTopo("IDENTIFICAÇÃO", carreta.id, aoVoltar) }
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
            CampoTexto("Placa da carreta *", edicao.placa, { edicao = edicao.copy(placa = it) }, maiusculas = true)
            CampoTexto("Placa do cavalo", edicao.placaCavalo, { edicao = edicao.copy(placaCavalo = it) }, maiusculas = true)
            CampoSelecao("Transportadora *", edicao.transportadora, config.transportadoras, { edicao = edicao.copy(transportadora = it) })
            CampoSelecao("Tipo de veículo", edicao.veiculo, config.veiculos, { edicao = edicao.copy(veiculo = it) })
            CampoSelecao("Origem *", edicao.origem, config.origens, { edicao = edicao.copy(origem = it) })
            CampoSelecao("Destino", edicao.destino, config.destinos, { edicao = edicao.copy(destino = it) })
            CampoTexto("Motorista", edicao.motorista, { edicao = edicao.copy(motorista = it) })
            CampoTexto("Carga / mapa / documento", edicao.documento, { edicao = edicao.copy(documento = it) })
            CampoSelecao("Turno", edicao.turno, config.turnos, { edicao = edicao.copy(turno = it) })
            CampoTexto("Conferente responsável", edicao.responsavel, { edicao = edicao.copy(responsavel = it) })
            CampoTexto("Horário programado", programado, { programado = it }, apoio = "formato 24h, exemplo 07:30")

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val hora: LocalTime? = Tempo.horaDe(programado)
                    vm.salvar(edicao.com(Marco.PROGRAMADO, hora))
                    aoVoltar()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("SALVAR", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

/** Carga recebida: o que o caminhão trouxe. */
@Composable
fun CargaScreen(vm: CarretaViewModel, aoVoltar: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val carreta = estado.carreta
    val config = estado.configuracao

    if (carreta == null) {
        Aviso("Carreta não encontrada.", erro = true, modifier = Modifier.padding(14.dp))
        return
    }

    var produto by remember(carreta.id) { mutableStateOf(carreta.produto) }
    var nota by remember(carreta.id) { mutableStateOf(carreta.notaFiscal) }
    var quantidade by remember(carreta.id) { mutableStateOf(Texto.numeroBr(carreta.quantidade)) }
    var unidade by remember(carreta.id) { mutableStateOf(carreta.unidade) }
    var observacao by remember(carreta.id) { mutableStateOf(carreta.observacaoCarga) }

    Scaffold(
        topBar = { BarraTopo("CARGA RECEBIDA", carreta.placa, aoVoltar) }
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
            CampoSelecao("Produto recebido", produto, config.produtos, { produto = it })
            CampoTexto("Nota fiscal", nota, { nota = it })
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoTexto("Quantidade", quantidade, { quantidade = it }, numerico = true, modifier = Modifier.weight(1f))
                CampoSelecao("Unidade", unidade, config.unidades, { unidade = it }, modifier = Modifier.weight(1f))
            }
            CampoTexto("Observação da carga", observacao, { observacao = it }, linhas = 3)

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    vm.salvar(
                        carreta.copy(
                            produto = produto,
                            notaFiscal = nota.trim(),
                            quantidade = Texto.numeroDe(quantidade),
                            unidade = unidade,
                            observacaoCarga = observacao.trim()
                        )
                    )
                    aoVoltar()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("SALVAR", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
