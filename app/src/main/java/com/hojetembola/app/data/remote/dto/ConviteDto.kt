package com.hojetembola.app.data.remote.dto

import com.hojetembola.app.data.local.entity.ConviteEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO de rede — espelha a tabela `convite` do Supabase.
 * Usado para enviar e receber convites de equipas.
 */
@Serializable
data class ConviteDto(
    val id: String,
    @SerialName("equipa_id")       val equipaId: Int,
    @SerialName("convidado_id")    val convidadoId: String,
    @SerialName("convidado_email") val convidadoEmail: String,
    @SerialName("nome_equipa")     val nomeEquipa: String = "",
    val token: String,
    @SerialName("data_expiracao")  val dataExpiracao: String,
    val estado: String = "pendente",
    @SerialName("criado_por_id")   val criadoPorId: String
) {
    fun toEntity() = ConviteEntity(
        id             = id,
        equipaId       = equipaId.toString(),
        convidadoId    = convidadoId,
        convidadoEmail = convidadoEmail,
        nomeEquipa     = nomeEquipa,
        token          = token,
        dataExpiracao  = dataExpiracao,
        estado         = estado,
        criadoPorId    = criadoPorId
    )
}

/**
 * DTO para INSERT de um novo convite.
 * O `id`, `token` e `data_expiracao` são gerados no servidor.
 */
@Serializable
data class ConviteInsertDto(
    @SerialName("equipa_id")       val equipaId: Int,
    @SerialName("convidado_id")    val convidadoId: String,
    @SerialName("convidado_email") val convidadoEmail: String,
    @SerialName("nome_equipa")     val nomeEquipa: String,
    @SerialName("criado_por_id")   val criadoPorId: String
)
