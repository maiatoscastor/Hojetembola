package com.hojetembola.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Notificação recebida pelo utilizador (RN08). */
@Entity(tableName = "notificacao")
data class NotificacaoEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "utilizador_id")
    val utilizadorId: String,

    /**
     * "jogo_marcado" | "resultado" | "convite" | "suspensao"
     * | "convite_expirado" | "alteracao_calendario"
     */
    val tipo: String,

    val titulo: String,

    val mensagem: String,

    @ColumnInfo(name = "data_hora")
    val dataHora: String,

    val lida: Boolean = false,

    /** JSON com dados extra (ex: id do jogo, id do torneio) */
    @ColumnInfo(name = "dados_extra")
    val dadosExtra: String? = null
)
