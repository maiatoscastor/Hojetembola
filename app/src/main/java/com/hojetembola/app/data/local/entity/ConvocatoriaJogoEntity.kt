package com.hojetembola.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Jogadores convocados para um jogo específico. */
@Entity(tableName = "convocatoria_jogo")
data class ConvocatoriaJogoEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "jogo_id")
    val jogoId: String,

    @ColumnInfo(name = "utilizador_id")
    val utilizadorId: String,

    @ColumnInfo(name = "equipa_id")
    val equipaId: String
)
