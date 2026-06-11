package com.hojetembola.app.data.remote.dto

import com.hojetembola.app.data.local.entity.JogadorInscricaoEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JogadorInscricaoDto(
    val id: Int,
    @SerialName("inscricao_id") val inscricaoId: Int,
    @SerialName("utilizador_id") val utilizadorId: String
) {
    fun toEntity() = JogadorInscricaoEntity(
        id = id.toString(),
        inscricaoEquipaId = inscricaoId.toString(),
        utilizadorId = utilizadorId
    )
}

@Serializable
data class JogadorInscricaoInsertDto(
    @SerialName("inscricao_id") val inscricaoId: Int,
    @SerialName("utilizador_id") val utilizadorId: String
)
