package com.hojetembola.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "equipa")
data class EquipaEntity(
    @PrimaryKey
    val id: String,

    val nome: String,

    /** Cor hex da equipa (ex: "#E84C3D") */
    val cor: String,

    @ColumnInfo(name = "escudo_url")
    val escudoUrl: String? = null,

    @ColumnInfo(name = "capitao_id")
    val capitaoId: String,

    @ColumnInfo(name = "data_criacao")
    val dataCriacao: String = ""
)
