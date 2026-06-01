package com.hojetembola.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "membro_equipa")
data class MembroEquipaEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "equipa_id")
    val equipaId: String,

    @ColumnInfo(name = "utilizador_id")
    val utilizadorId: String,

    @ColumnInfo(name = "data_entrada")
    val dataEntrada: String = ""
)
