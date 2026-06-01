package com.hojetembola.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Inscrição de uma equipa num torneio (RF15). */
@Entity(tableName = "inscricao_equipa")
data class InscricaoEquipaEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "torneio_id")
    val torneioId: String,

    @ColumnInfo(name = "equipa_id")
    val equipaId: String,

    /** "inscrita" | "desistente" */
    val estado: String = "inscrita",

    @ColumnInfo(name = "data_inscricao")
    val dataInscricao: String = ""
)
