package com.hojetembola.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Suspensão disciplinar automática (RN04). */
@Entity(tableName = "suspensao")
data class SuspensaoEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "utilizador_id")
    val utilizadorId: String,

    @ColumnInfo(name = "torneio_id")
    val torneioId: String,

    /** "acumulacao_amarelos" | "vermelho" */
    val motivo: String,

    /** Jogo onde a suspensão foi acionada */
    @ColumnInfo(name = "jogo_id")
    val jogoId: String,

    /** True quando o jogador já cumpriu a suspensão */
    val cumprida: Boolean = false
)
