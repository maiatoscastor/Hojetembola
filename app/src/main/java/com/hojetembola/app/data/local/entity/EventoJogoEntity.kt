package com.hojetembola.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Evento registado durante um jogo (golo, cartão, substituição).
 * Suporta registo offline — [isSynced] = false enquanto sem internet.
 */
@Entity(tableName = "evento_jogo")
data class EventoJogoEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "jogo_id")
    val jogoId: String,

    /** "golo" | "amarelo" | "vermelho" | "substituicao" */
    val tipo: String,

    val minuto: Int,

    @ColumnInfo(name = "jogador_id")
    val jogadorId: String? = null,

    @ColumnInfo(name = "equipa_id")
    val equipaId: String? = null,

    /** ID do jogador que SAI (apenas substituições) */
    @ColumnInfo(name = "jogador_sai_id")
    val jogadorSaiId: String? = null,

    /** ID do jogador que ENTRA (apenas substituições) */
    @ColumnInfo(name = "jogador_entra_id")
    val jogadorEntraId: String? = null,

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = true
)
