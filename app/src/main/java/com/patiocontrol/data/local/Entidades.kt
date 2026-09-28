package com.patiocontrol.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Os nove horários viram nove colunas em vez de um mapa serializado.
 * Custa mais linhas aqui e paga em todo o resto: dá para consultar por
 * horário, indexar, e a coluna mapeia uma para uma com a planilha.
 */
@Entity(
    tableName = "carreta",
    indices = [
        Index("data"),
        Index("placa"),
        Index("encerradaEm"),
        Index("exportadaEm")
    ]
)
data class CarretaEntity(
    @PrimaryKey val id: String,
    val data: LocalDate,
    val turno: String,
    val transportadora: String,
    val placa: String,
    val placaCavalo: String,
    val motorista: String,
    val veiculo: String,
    val origem: String,
    val destino: String,
    val documento: String,
    val responsavel: String,

    val produto: String,
    val notaFiscal: String,
    val quantidade: Double?,
    val unidade: String,
    val observacaoCarga: String,

    @ColumnInfo(name = "h_programado") val hProgramado: LocalTime?,
    @ColumnInfo(name = "h_chegada") val hChegada: LocalTime?,
    @ColumnInfo(name = "h_entrada") val hEntrada: LocalTime?,
    @ColumnInfo(name = "h_inicio_descarga") val hInicioDescarga: LocalTime?,
    @ColumnInfo(name = "h_fim_descarga") val hFimDescarga: LocalTime?,
    @ColumnInfo(name = "h_inicio_ag") val hInicioAg: LocalTime?,
    @ColumnInfo(name = "h_fim_ag") val hFimAg: LocalTime?,
    @ColumnInfo(name = "h_liberacao") val hLiberacao: LocalTime?,
    @ColumnInfo(name = "h_saida") val hSaida: LocalTime?,

    val respostaAg: String,
    val justificativaSemAg: String,
    val observacaoCarregamento: String,

    val encerradaEm: Instant?,
    val exportadaEm: Instant?,
    val transferidaEm: Instant?
)

@Entity(
    tableName = "ativo_giro",
    foreignKeys = [
        ForeignKey(
            entity = CarretaEntity::class,
            parentColumns = ["id"],
            childColumns = ["carretaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carretaId")]
)
data class AtivoGiroEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carretaId: String,
    val tipo: String,
    val quantidade: Double?,
    val unidade: String,
    val documento: String,
    val destino: String,
    val observacao: String
)

@Entity(
    tableName = "avaria",
    foreignKeys = [
        ForeignKey(
            entity = CarretaEntity::class,
            parentColumns = ["id"],
            childColumns = ["carretaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carretaId")]
)
data class AvariaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carretaId: String,
    val item: String,
    val quantidade: Double?,
    val unidade: String,
    val origem: String,
    val observacao: String,
    val foto: String
)

/** A carreta com seus filhos, montada pelo Room numa consulta só. */
data class CarretaComItens(
    @Embedded val carreta: CarretaEntity,
    @Relation(parentColumn = "id", entityColumn = "carretaId")
    val ativosGiro: List<AtivoGiroEntity>,
    @Relation(parentColumn = "id", entityColumn = "carretaId")
    val avarias: List<AvariaEntity>
)
