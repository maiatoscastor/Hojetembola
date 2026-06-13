package com.hojetembola.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jornada")
data class JornadaEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "torneio_id")
    val torneioId: String,

    val nome: String = "",

    /** Número sequencial da jornada (1, 2, 3…) */
    val numero: Int,

    @ColumnInfo(name = "data_inicio")
    val dataInicio: String? = null,

    @ColumnInfo(name = "data_fim")
    val dataFim: String? = null
)
