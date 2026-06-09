package com.hojetembola.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Espelha a tabela `equipa` do Supabase.
 * id é int4 no Supabase (auto-increment), guardado como String para consistência com as
 * outras entidades offline-first.
 */
@Entity(tableName = "equipa")
data class EquipaEntity(
    @PrimaryKey
    val id: String,

    val nome: String,

    /** Abreviatura — ex: "FCB", "SLB" (máx. 3 chars) */
    val iniciais: String,

    /** Cor hex do avatar — ex: "#E84C3D" */
    @ColumnInfo(name = "cor_avatar")
    val corAvatar: String,

    val cidade: String? = null,

    @ColumnInfo(name = "foto_escudo_uri")
    val fotoEscudoUri: String? = null,

    @ColumnInfo(name = "data_criacao")
    val dataCriacao: String = "",

    val ativa: Boolean = true,

    @ColumnInfo(name = "capitao_id")
    val capitaoId: String
)
