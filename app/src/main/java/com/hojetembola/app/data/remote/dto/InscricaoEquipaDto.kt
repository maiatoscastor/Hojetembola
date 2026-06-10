package com.hojetembola.app.data.remote.dto

import com.hojetembola.app.data.local.entity.InscricaoEquipaEntity
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** DTO de leitura — espelha `inscricao_equipa` do Supabase. */
@Serializable
data class InscricaoEquipaDto(
    val id: Int,
    @SerialName("torneio_id")     val torneioId: Int,
    @SerialName("equipa_id")      val equipaId: Int,
    @SerialName("inscrita_por_id") val inscritaPorId: String? = null,
    @SerialName("data_inscricao") val dataInscricao: String  = "",
    val estado: String            = "Pendente",
    val grupo: String?            = null
) {
    fun toEntity() = InscricaoEquipaEntity(
        id            = id.toString(),
        torneioId     = torneioId.toString(),
        equipaId      = equipaId.toString(),
        inscritaPorId = inscritaPorId,
        dataInscricao = dataInscricao,
        estado        = estado,
        grupo         = grupo
    )
}

/** DTO de inserção — sem `id` (gerado pelo Supabase). */
@Serializable
data class InscricaoEquipaInsertDto(
    @SerialName("torneio_id")     val torneioId: Int,
    @SerialName("equipa_id")      val equipaId: Int,
    @SerialName("inscrita_por_id") val inscritaPorId: String? = null,
    @EncodeDefault val estado: String = "Pendente"
)
