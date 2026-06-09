package com.hojetembola.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Convite de capitão para jogadores — válido 48 horas (RN01). */
@Entity(tableName = "convite")
data class ConviteEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "equipa_id")
    val equipaId: String,

    /** UUID do utilizador convidado. */
    @ColumnInfo(name = "convidado_id")
    val convidadoId: String,

    @ColumnInfo(name = "convidado_email")
    val convidadoEmail: String,

    /** Nome da equipa — para mostrar na notificação sem JOIN. */
    @ColumnInfo(name = "nome_equipa")
    val nomeEquipa: String = "",

    /** Token único para aceitar o convite via link. */
    val token: String,

    @ColumnInfo(name = "data_expiracao")
    val dataExpiracao: String,

    /** "pendente" | "aceite" | "recusado" | "expirado" */
    val estado: String = "pendente",

    @ColumnInfo(name = "criado_por_id")
    val criadoPorId: String
)
