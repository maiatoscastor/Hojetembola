package com.hojetembola.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Voto no MVP — 1 por utilizador por jogo, pode ser alterado (RF30, RF31). */
@Entity(tableName = "voto_mvp")
data class VotoMvpEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "jogo_id")
    val jogoId: String,

    @ColumnInfo(name = "votante_id")
    val votanteId: String,

    @ColumnInfo(name = "votado_id")
    val votadoId: String,

    /** "jogador" | "publico" | "organizador" */
    @ColumnInfo(name = "tipo_votante")
    val tipoVotante: String
)
