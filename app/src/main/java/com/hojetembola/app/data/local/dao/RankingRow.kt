package com.hojetembola.app.data.local.dao

import androidx.room.ColumnInfo

/** Room projection used by all ranking queries. */
data class RankingRow(
    @ColumnInfo(name = "jogador_id") val jogadorId: String,
    val nome: String,
    @ColumnInfo(name = "equipa_nome") val equipaNome: String,
    @ColumnInfo(name = "equipa_iniciais") val equipaIniciais: String,
    @ColumnInfo(name = "equipa_cor") val equipaCor: String,
    val total: Int
)

/** Room projection for cartões (amarelos + vermelhos separate). */
data class CartaoRow(
    @ColumnInfo(name = "jogador_id") val jogadorId: String,
    val nome: String,
    @ColumnInfo(name = "equipa_nome") val equipaNome: String,
    @ColumnInfo(name = "equipa_iniciais") val equipaIniciais: String,
    @ColumnInfo(name = "equipa_cor") val equipaCor: String,
    val amarelos: Int,
    val vermelhos: Int
)

/** Room projection for classificação with equipa name. */
data class ClassificacaoNomeRow(
    val posicao: Int,
    @ColumnInfo(name = "equipa_nome") val equipaNome: String,
    val jogos: Int,
    val vitorias: Int,
    val empates: Int,
    val derrotas: Int,
    @ColumnInfo(name = "golos_marcados") val golosMarcados: Int,
    @ColumnInfo(name = "golos_sofridos") val golosSofridos: Int,
    val pontos: Int
)
