package com.hojetembola.app.data.remote.dto

import com.hojetembola.app.data.local.entity.MembroEquipaEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** DTO de leitura — espelha `membro_equipa` do Supabase. */
@Serializable
data class MembroEquipaDto(
    val id: Int,
    @SerialName("equipa_id") val equipaId: Int,
    @SerialName("utilizador_id") val utilizadorId: String,
    val data: String = "",
    val ativo: Boolean = true
) {
    fun toEntity() = MembroEquipaEntity(
        id           = id.toString(),
        equipaId     = equipaId.toString(),
        utilizadorId = utilizadorId,
        data         = data,
        ativo        = ativo
    )
}

/** DTO de inserção — sem `id` (gerado pelo Supabase). */
@Serializable
data class MembroEquipaInsertDto(
    @SerialName("equipa_id")     val equipaId: Int,
    @SerialName("utilizador_id") val utilizadorId: String
)
