package com.hojetembola.app.data.local.entity

import androidx.room.ColumnInfo

/** Projeção de Room: inscrição + dados da equipa (via JOIN). */
data class InscricaoComEquipa(
    val id: String,
    @ColumnInfo(name = "torneio_id") val torneioId: String,
    @ColumnInfo(name = "equipa_id")  val equipaId: String,
    @ColumnInfo(name = "inscrita_por_id") val inscritaPorId: String?,
    @ColumnInfo(name = "data_inscricao")  val dataInscricao: String,
    val estado: String,
    val grupo: String?,
    val nome: String,
    val iniciais: String,
    @ColumnInfo(name = "cor_avatar") val corAvatar: String,
    @ColumnInfo(name = "num_membros") val numMembros: Int = 0,
    @ColumnInfo(name = "cidade") val cidade: String? = null
)
