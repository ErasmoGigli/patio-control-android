package com.patiocontrol.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface CarretaDao {

    /** No pátio: tudo que ainda não foi encerrado, mais recente primeiro. */
    @Transaction
    @Query("SELECT * FROM carreta WHERE encerradaEm IS NULL ORDER BY data DESC, id DESC")
    fun observarAbertas(): Flow<List<CarretaComItens>>

    @Transaction
    @Query("SELECT * FROM carreta WHERE encerradaEm IS NOT NULL ORDER BY encerradaEm DESC LIMIT :limite")
    fun observarEncerradas(limite: Int = 200): Flow<List<CarretaComItens>>

    /**
     * Fila de exportação: encerradas que ainda não saíram num pacote.
     *
     * Carreta transferida fica de fora. Ela passou a ser responsabilidade do
     * outro aparelho, e se saísse daqui também a planilha receberia a mesma
     * linha duas vezes, com o mesmo identificador.
     */
    @Transaction
    @Query(
        "SELECT * FROM carreta WHERE encerradaEm IS NOT NULL AND exportadaEm IS NULL " +
            "AND transferidaEm IS NULL ORDER BY encerradaEm"
    )
    fun observarParaExportar(): Flow<List<CarretaComItens>>

    @Transaction
    @Query(
        "SELECT * FROM carreta WHERE encerradaEm IS NOT NULL AND exportadaEm IS NULL " +
            "AND transferidaEm IS NULL ORDER BY encerradaEm"
    )
    suspend fun paraExportar(): List<CarretaComItens>

    @Transaction
    @Query("SELECT * FROM carreta WHERE id = :id")
    fun observar(id: String): Flow<CarretaComItens?>

    @Transaction
    @Query("SELECT * FROM carreta WHERE id = :id")
    suspend fun buscar(id: String): CarretaComItens?

    /** Alerta de placa repetida no mesmo dia — duas linhas para a mesma viagem. */
    @Query("SELECT COUNT(*) FROM carreta WHERE placa = :placa AND data = :data AND id <> :exceto")
    suspend fun contarPlacaNoDia(placa: String, data: java.time.LocalDate, exceto: String): Int

    @Upsert
    suspend fun salvar(carreta: CarretaEntity)

    @Update
    suspend fun atualizar(carreta: CarretaEntity)

    @Query("UPDATE carreta SET exportadaEm = :quando WHERE id IN (:ids)")
    suspend fun marcarExportadas(ids: List<String>, quando: Instant)

    @Query("UPDATE carreta SET transferidaEm = :quando WHERE id IN (:ids)")
    suspend fun marcarTransferidas(ids: List<String>, quando: Instant)

    @Query("DELETE FROM carreta WHERE id = :id")
    suspend fun apagar(id: String)

    @Query("SELECT COUNT(*) FROM carreta")
    suspend fun total(): Int
}

@Dao
interface AtivoGiroDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(ativo: AtivoGiroEntity): Long

    @Update
    suspend fun atualizar(ativo: AtivoGiroEntity)

    @Delete
    suspend fun apagar(ativo: AtivoGiroEntity)

    @Query("DELETE FROM ativo_giro WHERE carretaId = :carretaId")
    suspend fun apagarDaCarreta(carretaId: String)

    @Query("SELECT * FROM ativo_giro WHERE id = :id")
    suspend fun buscar(id: Long): AtivoGiroEntity?
}

@Dao
interface AvariaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(avaria: AvariaEntity): Long

    @Update
    suspend fun atualizar(avaria: AvariaEntity)

    @Delete
    suspend fun apagar(avaria: AvariaEntity)

    @Query("DELETE FROM avaria WHERE carretaId = :carretaId")
    suspend fun apagarDaCarreta(carretaId: String)

    @Query("SELECT * FROM avaria WHERE id = :id")
    suspend fun buscar(id: Long): AvariaEntity?
}
