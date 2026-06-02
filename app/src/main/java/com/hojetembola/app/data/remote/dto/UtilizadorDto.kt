package com.hojetembola.app.data.remote.dto

import com.hojetembola.app.data.local.entity.UtilizadorEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO de rede — espelha a tabela `utilizador` do Supabase.
 *
 * Os nomes dos campos seguem o snake_case da base de dados;
 * [toEntity] converte para o camelCase do Room.
 */
@Serializable
data class UtilizadorDto(
    val id: String,
    val nome: String,
    val email: String,
    /** "utilizador" | "organizador" */
    val perfil: String,
    @SerialName("foto_url")    val fotoUrl: String? = null,
    @SerialName("data_registo") val dataRegisto: String = "",
    val ativo: Boolean = true
) {
    fun toEntity() = UtilizadorEntity(
        id          = id,
        nome        = nome,
        email       = email,
        perfil      = perfil,
        fotoUrl     = fotoUrl,
        dataRegisto = dataRegisto,
        ativo       = ativo
    )
}
