package com.hojetembola.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Classificação de uma equipa num torneio (RF26, RN05). */
@Entity(tableName = "classificacao")
data class ClassificacaoEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "torneio_id")
    val torneioId: String,

    @ColumnInfo(name = "equipa_id")
    val equipaId: String,

    val jogos: Int = 0,
    val vitorias: Int = 0,
    val empates: Int = 0,
    val derrotas: Int = 0,

    @ColumnInfo(name = "golos_marcados")
    val golosMarcados: Int = 0,

    @ColumnInfo(name = "golos_sofridos")
    val golosSofridos: Int = 0,

    val pontos: Int = 0,
    val posicao: Int = 0
)
