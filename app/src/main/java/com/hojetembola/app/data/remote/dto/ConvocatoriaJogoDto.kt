package com.hojetembola.app.data.remote.dto

import com.hojetembola.app.data.local.entity.ConvocatoriaJogoEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Supabase setup — run once in the SQL Editor:
 *
 *   ALTER TABLE convocatoria_jogo
 *       ADD COLUMN IF NOT EXISTS is_titular BOOLEAN NOT NULL DEFAULT false;
 */

@Serializable
data class ConvocatoriaJogoDto(
    val id: Int,
    @SerialName("jogo_id")       val jogoId: Int,
    @SerialName("utilizador_id") val utilizadorId: String,
    @SerialName("equipa_id")     val equipaId: Int,
    @SerialName("is_titular")    val isTitular: Boolean = false
) {
    fun toEntity() = ConvocatoriaJogoEntity(
        id           = id.toString(),
        jogoId       = jogoId.toString(),
        utilizadorId = utilizadorId,
        equipaId     = equipaId.toString(),
        isTitular    = isTitular
    )
}

@Serializable
data class ConvocatoriaJogoInsertDto(
    @SerialName("jogo_id")       val jogoId: Int,
    @SerialName("utilizador_id") val utilizadorId: String,
    @SerialName("equipa_id")     val equipaId: Int,
    @SerialName("is_titular")    val isTitular: Boolean
)
