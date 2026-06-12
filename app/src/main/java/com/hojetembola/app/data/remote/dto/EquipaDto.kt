package com.hojetembola.app.data.remote.dto

import com.hojetembola.app.data.local.entity.EquipaEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** DTO de leitura — espelha a tabela `equipa` do Supabase (id integer auto-increment). */
@Serializable
data class EquipaDto(
    val id: Int,
    val nome: String?               = null,
    val iniciais: String?           = null,
    @SerialName("cor_avatar")      val corAvatar: String?     = null,
    val cidade: String?             = null,
    @SerialName("foto_escudo_uri") val fotoEscudoUri: String? = null,
    @SerialName("data_criacao")    val dataCriacao: String?   = null,
    val ativa: Boolean?             = null,
    @SerialName("capitao_id")      val capitaoId: String?     = null
) {
    fun toEntity() = EquipaEntity(
        id            = id.toString(),
        nome          = nome ?: "",
        iniciais      = iniciais ?: "?",
        corAvatar     = corAvatar ?: "#3D5A80",
        cidade        = cidade,
        fotoEscudoUri = fotoEscudoUri,
        dataCriacao   = dataCriacao ?: "",
        ativa         = ativa ?: true,
        capitaoId     = capitaoId ?: ""
    )
}

/** DTO de inserção — sem `id` (gerado pelo Supabase). */
@Serializable
data class EquipaInsertDto(
    val nome: String,
    val iniciais: String,
    @SerialName("cor_avatar")     val corAvatar: String,
    val cidade: String?           = null,
    @SerialName("foto_escudo_uri") val fotoEscudoUri: String? = null,
    @SerialName("capitao_id")     val capitaoId: String,
    val ativa: Boolean            = true
)
