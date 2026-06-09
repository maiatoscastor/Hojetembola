package com.hojetembola.app.data.remote.dto

import com.hojetembola.app.data.local.entity.NotificacaoEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO de rede — espelha a tabela `notificacao` do Supabase.
 */
@Serializable
data class NotificacaoDto(
    val id: String,
    @SerialName("utilizador_id") val utilizadorId: String,
    val tipo: String,
    val titulo: String,
    val mensagem: String,
    @SerialName("data_hora")   val dataHora: String,
    val lida: Boolean = false,
    @SerialName("dados_extra") val dadosExtra: String? = null
) {
    fun toEntity() = NotificacaoEntity(
        id           = id,
        utilizadorId = utilizadorId,
        tipo         = tipo,
        titulo       = titulo,
        mensagem     = mensagem,
        dataHora     = dataHora,
        lida         = lida,
        dadosExtra   = dadosExtra
    )
}
