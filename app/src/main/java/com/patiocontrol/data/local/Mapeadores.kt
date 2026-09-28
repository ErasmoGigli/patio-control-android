package com.patiocontrol.data.local

import com.patiocontrol.domain.AtivoGiro
import com.patiocontrol.domain.Avaria
import com.patiocontrol.domain.Carreta
import com.patiocontrol.domain.Marco
import com.patiocontrol.domain.RespostaAg
import java.time.LocalTime

/**
 * A fronteira entre banco e domínio.
 *
 * O domínio nunca vê CarretaEntity, e o Room nunca vê Marco. Isso é o que
 * permite mudar uma coluna sem mexer na regra de negócio, e testar a regra
 * de negócio sem subir banco.
 */

fun CarretaComItens.paraDominio(): Carreta {
    val horarios = buildMap<Marco, LocalTime> {
        carreta.hProgramado?.let { put(Marco.PROGRAMADO, it) }
        carreta.hChegada?.let { put(Marco.CHEGADA, it) }
        carreta.hEntrada?.let { put(Marco.ENTRADA, it) }
        carreta.hInicioDescarga?.let { put(Marco.INICIO_DESCARGA, it) }
        carreta.hFimDescarga?.let { put(Marco.FIM_DESCARGA, it) }
        carreta.hInicioAg?.let { put(Marco.INICIO_AG, it) }
        carreta.hFimAg?.let { put(Marco.FIM_AG, it) }
        carreta.hLiberacao?.let { put(Marco.LIBERACAO, it) }
        carreta.hSaida?.let { put(Marco.SAIDA, it) }
    }

    return Carreta(
        id = carreta.id,
        data = carreta.data,
        turno = carreta.turno,
        transportadora = carreta.transportadora,
        placa = carreta.placa,
        placaCavalo = carreta.placaCavalo,
        motorista = carreta.motorista,
        veiculo = carreta.veiculo,
        origem = carreta.origem,
        destino = carreta.destino,
        documento = carreta.documento,
        responsavel = carreta.responsavel,
        produto = carreta.produto,
        notaFiscal = carreta.notaFiscal,
        quantidade = carreta.quantidade,
        unidade = carreta.unidade,
        observacaoCarga = carreta.observacaoCarga,
        horarios = horarios,
        respostaAg = runCatching { RespostaAg.valueOf(carreta.respostaAg) }
            .getOrDefault(RespostaAg.NAO_RESPONDIDO),
        justificativaSemAg = carreta.justificativaSemAg,
        observacaoCarregamento = carreta.observacaoCarregamento,
        encerradaEm = carreta.encerradaEm,
        exportadaEm = carreta.exportadaEm,
        transferidaEm = carreta.transferidaEm,
        ativosGiro = ativosGiro.map { it.paraDominio() },
        avarias = avarias.map { it.paraDominio() }
    )
}

fun Carreta.paraEntidade(): CarretaEntity = CarretaEntity(
    id = id,
    data = data,
    turno = turno,
    transportadora = transportadora,
    placa = placa,
    placaCavalo = placaCavalo,
    motorista = motorista,
    veiculo = veiculo,
    origem = origem,
    destino = destino,
    documento = documento,
    responsavel = responsavel,
    produto = produto,
    notaFiscal = notaFiscal,
    quantidade = quantidade,
    unidade = unidade,
    observacaoCarga = observacaoCarga,
    hProgramado = horario(Marco.PROGRAMADO),
    hChegada = horario(Marco.CHEGADA),
    hEntrada = horario(Marco.ENTRADA),
    hInicioDescarga = horario(Marco.INICIO_DESCARGA),
    hFimDescarga = horario(Marco.FIM_DESCARGA),
    hInicioAg = horario(Marco.INICIO_AG),
    hFimAg = horario(Marco.FIM_AG),
    hLiberacao = horario(Marco.LIBERACAO),
    hSaida = horario(Marco.SAIDA),
    respostaAg = respostaAg.name,
    justificativaSemAg = justificativaSemAg,
    observacaoCarregamento = observacaoCarregamento,
    encerradaEm = encerradaEm,
    exportadaEm = exportadaEm,
    transferidaEm = transferidaEm
)

fun AtivoGiroEntity.paraDominio() = AtivoGiro(
    id = id,
    carretaId = carretaId,
    tipo = tipo,
    quantidade = quantidade,
    unidade = unidade,
    documento = documento,
    destino = destino,
    observacao = observacao
)

fun AtivoGiro.paraEntidade(carreta: String = carretaId) = AtivoGiroEntity(
    id = id,
    carretaId = carreta,
    tipo = tipo,
    quantidade = quantidade,
    unidade = unidade,
    documento = documento,
    destino = destino,
    observacao = observacao
)

fun AvariaEntity.paraDominio() = Avaria(
    id = id,
    carretaId = carretaId,
    item = item,
    quantidade = quantidade,
    unidade = unidade,
    origem = origem,
    observacao = observacao,
    foto = foto
)

fun Avaria.paraEntidade(carreta: String = carretaId) = AvariaEntity(
    id = id,
    carretaId = carreta,
    item = item,
    quantidade = quantidade,
    unidade = unidade,
    origem = origem,
    observacao = observacao,
    foto = foto
)
