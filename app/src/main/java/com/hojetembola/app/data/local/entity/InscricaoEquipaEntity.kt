package com.hojetembola.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Espelha a tabela `inscricao_equipa` do Supabase.
 * Estado ENUM do Supabase: Pendente | Aceite | Recusada | Eliminada
 */
@Entity(tableName = "inscricao_equipa")
data class InscricaoEquipaEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "torneio_id")
    val torneioId: String,

    @ColumnInfo(name = "equipa_id")
    val equipaId: String,

    /** UUID do utilizador que inscreveu (capitão) */
    @ColumnInfo(name = "inscrita_por_id")
    val inscritaPorId: String? = null,

    @ColumnInfo(name = "data_inscricao")
    val dataInscricao: String = "",

    /** ENUM Supabase estado_inscricao: Pendente | Aceite | Recusada | Eliminada */
    val estado: String = "Pendente",

    val grupo: String? = null
)
