package com.hojetembola.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "utilizador")
data class UtilizadorEntity(
    @PrimaryKey
    val id: String,

    val nome: String,

    val email: String,

    /** "administrador" | "organizador" | "capitao" | "jogador" | "espectador" */
    val perfil: String,

    @ColumnInfo(name = "foto_url")
    val fotoUrl: String? = null,

    @ColumnInfo(name = "data_registo")
    val dataRegisto: String = ""
)
