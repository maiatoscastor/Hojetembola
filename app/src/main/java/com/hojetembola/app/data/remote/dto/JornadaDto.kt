package com.hojetembola.app.data.remote.dto

import com.hojetembola.app.data.local.entity.JornadaEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JornadaDto(
    val id: Int,
    @SerialName("torneio_id") val torneioId: Int,
    val nome: String = "",
    @SerialName("data_inicio") val dataInicio: String? = null,
    @SerialName("data_fim")    val dataFim: String? = null
) {
    fun toEntity() = JornadaEntity(
        id        = id.toString(),
        torneioId = torneioId.toString(),
        numero    = nome.filter { it.isDigit() }.toIntOrNull() ?: 0,
        dataInicio = dataInicio,
        dataFim    = dataFim
    )
}

@Serializable
data class JornadaInsertDto(
    @SerialName("torneio_id") val torneioId: Int,
    val nome: String
)
