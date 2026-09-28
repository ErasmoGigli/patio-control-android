package com.patiocontrol.data.local

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Datas e horas são guardadas como texto ISO e o instante como epoch em
 * milissegundos. Texto ordena igual à ordem cronológica em SQLite, então
 * `ORDER BY data` continua fazendo o que parece que faz.
 */
class Conversores {

    @TypeConverter
    fun paraData(valor: String?): LocalDate? = valor?.let { LocalDate.parse(it) }

    @TypeConverter
    fun deData(valor: LocalDate?): String? = valor?.toString()

    @TypeConverter
    fun paraHora(valor: String?): LocalTime? = valor?.let { LocalTime.parse(it) }

    @TypeConverter
    fun deHora(valor: LocalTime?): String? =
        valor?.let { "%02d:%02d".format(it.hour, it.minute) }

    @TypeConverter
    fun paraInstante(valor: Long?): Instant? = valor?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun deInstante(valor: Instant?): Long? = valor?.toEpochMilli()
}
