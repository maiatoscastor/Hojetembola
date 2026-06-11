package com.hojetembola.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Jogadores selecionados pelo capitão para um torneio específico (RF16). */
@Entity(tableName = "jogador_inscricao")
data class JogadorInscricaoEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "inscricao_id")
    val inscricaoEquipaId: String,

    @ColumnInfo(name = "utilizador_id")
    val utilizadorId: String
)
