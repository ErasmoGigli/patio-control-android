package com.patiocontrol.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CarretaEntity::class, AtivoGiroEntity::class, AvariaEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Conversores::class)
abstract class PatioDatabase : RoomDatabase() {

    abstract fun carretaDao(): CarretaDao
    abstract fun ativoGiroDao(): AtivoGiroDao
    abstract fun avariaDao(): AvariaDao

    companion object {

        private const val NOME = "patio.db"

        @Volatile
        private var instancia: PatioDatabase? = null

        fun obter(contexto: Context): PatioDatabase =
            instancia ?: synchronized(this) {
                instancia ?: construir(contexto).also { instancia = it }
            }

        private fun construir(contexto: Context): PatioDatabase =
            Room.databaseBuilder(contexto.applicationContext, PatioDatabase::class.java, NOME)
                // Sem fallbackToDestructiveMigration de propósito: apagar o
                // banco do conferente numa atualização é perder turno de
                // coleta. Toda mudança de schema entra como migração escrita.
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        db.execSQL("PRAGMA foreign_keys = ON")
                    }
                })
                .build()
    }
}

/**
 * Exemplo de migração, deixado como referência viva para a primeira mudança
 * de schema. O padrão é sempre este: SQL explícito, testado com
 * MigrationTestHelper contra o JSON exportado pelo Room em app/schemas.
 *
 * val MIGRACAO_1_2 = object : Migration(1, 2) {
 *     override fun migrate(db: SupportSQLiteDatabase) {
 *         db.execSQL("ALTER TABLE carreta ADD COLUMN lacre TEXT NOT NULL DEFAULT ''")
 *     }
 * }
 */
