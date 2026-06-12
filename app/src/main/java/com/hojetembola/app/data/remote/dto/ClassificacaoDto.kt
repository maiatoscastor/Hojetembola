package com.hojetembola.app.data.remote.dto

import com.hojetembola.app.data.local.entity.ClassificacaoEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Supabase setup — run once in the SQL Editor:
 *
 *   CREATE TABLE IF NOT EXISTS classificacao (
 *       id              SERIAL PRIMARY KEY,
 *       torneio_id      INTEGER NOT NULL,
 *       equipa_id       INTEGER NOT NULL,
 *       jogos           INTEGER NOT NULL DEFAULT 0,
 *       vitorias        INTEGER NOT NULL DEFAULT 0,
 *       empates         INTEGER NOT NULL DEFAULT 0,
 *       derrotas        INTEGER NOT NULL DEFAULT 0,
 *       golos_marcados  INTEGER NOT NULL DEFAULT 0,
 *       golos_sofridos  INTEGER NOT NULL DEFAULT 0,
 *       pontos          INTEGER NOT NULL DEFAULT 0,
 *       posicao         INTEGER NOT NULL DEFAULT 0,
 *       UNIQUE (torneio_id, equipa_id)
 *   );
 *
 *   -- RLS
 *   ALTER TABLE classificacao ENABLE ROW LEVEL SECURITY;
 *   CREATE POLICY "classificacao_leitura" ON classificacao FOR SELECT USING (true);
 *   CREATE POLICY "classificacao_escrita"  ON classificacao FOR INSERT TO authenticated WITH CHECK (true);
 *   CREATE POLICY "classificacao_update"   ON classificacao FOR UPDATE TO authenticated USING (true);
 */

@Serializable
data class ClassificacaoDto(
    val id: Int,
    @SerialName("torneio_id")         val torneioId: Int,
    @SerialName("equipa_id")          val equipaId: Int,
    val grupo: String? = null,
    val jogos: Int = 0,
    val vitorias: Int = 0,
    val empates: Int = 0,
    val derrotas: Int = 0,
    @SerialName("golos_marcados")     val golosMarcados: Int = 0,
    @SerialName("golos_sofridos")     val golosSofridos: Int = 0,
    val pontos: Int = 0,
    val posicao: Int = 0,
    @SerialName("posicao_anterior")   val posicaoAnterior: Int? = null,
    @SerialName("data_atualizacao")   val dataAtualizacao: String? = null
) {
    fun toEntity() = ClassificacaoEntity(
        id            = "${torneioId}_${equipaId}",
        torneioId     = torneioId.toString(),
        equipaId      = equipaId.toString(),
        jogos         = jogos,
        vitorias      = vitorias,
        empates       = empates,
        derrotas      = derrotas,
        golosMarcados = golosMarcados,
        golosSofridos = golosSofridos,
        pontos        = pontos,
        posicao       = posicao
    )
}

@Serializable
data class ClassificacaoUpsertDto(
    @SerialName("torneio_id")      val torneioId: Int,
    @SerialName("equipa_id")       val equipaId: Int,
    val jogos: Int,
    val vitorias: Int,
    val empates: Int,
    val derrotas: Int,
    @SerialName("golos_marcados")  val golosMarcados: Int,
    @SerialName("golos_sofridos")  val golosSofridos: Int,
    val pontos: Int,
    val posicao: Int
)
