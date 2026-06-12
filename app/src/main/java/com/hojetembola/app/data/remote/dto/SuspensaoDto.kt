package com.hojetembola.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * The suspensao table already exists in Supabase. Run these fixes once:
 *
 *   -- utilizador_id is int4 but user IDs are UUIDs (text)
 *   ALTER TABLE suspensao ALTER COLUMN utilizador_id TYPE text USING utilizador_id::text;
 *
 *   -- RLS policies (if not already added)
 *   ALTER TABLE suspensao ENABLE ROW LEVEL SECURITY;
 *   CREATE POLICY "suspensao_select" ON suspensao FOR SELECT USING (true);
 *   CREATE POLICY "suspensao_insert" ON suspensao FOR INSERT TO authenticated WITH CHECK (true);
 *   CREATE POLICY "suspensao_update" ON suspensao FOR UPDATE TO authenticated USING (true);
 */

@Serializable
data class SuspensaoReadDto(
    val id: Int,
    @SerialName("utilizador_id")    val utilizadorId: String,
    @SerialName("torneio_id")       val torneioId: Int,
    @SerialName("jogo_suspenso_id") val jogoSuspensoId: Int,
    val motivo: String,
    val estado: String = "Ativa",
    val sincronizado: Boolean? = null,
    @SerialName("data_criacao")     val dataCriacao: String? = null
) {
    fun toEntity() = com.hojetembola.app.data.local.entity.SuspensaoEntity(
        id           = id.toString(),
        utilizadorId = utilizadorId,
        torneioId    = torneioId.toString(),
        motivo       = motivo,
        jogoId       = jogoSuspensoId.toString(),
        cumprida     = estado != "Ativa"
    )
}

@Serializable
data class SuspensaoInsertDto(
    @SerialName("utilizador_id")    val utilizadorId: String,
    @SerialName("torneio_id")       val torneioId: Int,
    @SerialName("jogo_suspenso_id") val jogoSuspensoId: Int,
    val motivo: String,
    val estado: String = "Ativa",
    val sincronizado: Boolean = true
)
