package com.hojetembola.app.data.remote.dto

import com.hojetembola.app.data.local.entity.EventoJogoEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventoJogoDto(
    val id: Int,
    @SerialName("jogo_id") val jogoId: Int,
    val tipo: String,
    val minuto: Int,
    @SerialName("equipa_id") val equipaId: Int? = null,
    @SerialName("jogador_id") val jogadorId: String? = null,
    @SerialName("jogador_sai_id") val jogadorSaiId: String? = null,
    @SerialName("jogador_entra_id") val jogadorEntraId: String? = null
) {
    fun toEntity() = EventoJogoEntity(
        id = id.toString(),
        jogoId = jogoId.toString(),
        tipo = tipo,
        minuto = minuto,
        equipaId = equipaId?.toString(),
        jogadorId = jogadorId,
        jogadorSaiId = jogadorSaiId,
        jogadorEntraId = jogadorEntraId,
        isSynced = true
    )
}

@Serializable
data class EventoJogoInsertDto(
    @SerialName("jogo_id") val jogoId: Int,
    val tipo: String,
    val minuto: Int,
    @SerialName("equipa_id") val equipaId: Int? = null,
    @SerialName("jogador_id") val jogadorId: String? = null,
    @SerialName("jogador_sai_id") val jogadorSaiId: String? = null,
    @SerialName("jogador_entra_id") val jogadorEntraId: String? = null
)
