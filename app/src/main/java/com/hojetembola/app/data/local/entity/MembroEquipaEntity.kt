package com.hojetembola.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Espelha a tabela `membro_equipa` do Supabase. */
@Entity(tableName = "membro_equipa")
data class MembroEquipaEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "equipa_id")
    val equipaId: String,

    /** UUID do utilizador */
    @ColumnInfo(name = "utilizador_id")
    val utilizadorId: String,

    /** Timestamp de entrada — coluna Supabase: data */
    val data: String = "",

    val ativo: Boolean = true
)
