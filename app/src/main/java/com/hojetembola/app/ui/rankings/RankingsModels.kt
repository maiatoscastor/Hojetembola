package com.hojetembola.app.ui.rankings

enum class RankingScope { GERAL, TORNEIO }

enum class RankingMetric {
    GOLOS,
    JOGOS,
    ASSISTENCIAS,
    MINUTOS,
    AMARELOS,
    VERMELHOS,
    MVPS,
    CARTOES,
    CLASSIFICACAO
}

enum class RankingPeriod {
    ESTA_JORNADA,
    ESTE_MES,
    EPOCA_TODA,
    JORNADA_ATUAL,
    TORNEIO_COMPLETO
}

enum class RankingTrend { UP, DOWN, FLAT }

data class RankingEntry(
    val posicao: Int,
    val nome: String,
    val equipa: String,
    val iniciais: String,
    val avatarColor: Int,
    val valor: Int,
    val variacao: Int? = null,
    val amarelos: Int? = null,
    val vermelhos: Int? = null
) {
    val trend: RankingTrend = when {
        variacao == null || variacao == 0 -> RankingTrend.FLAT
        variacao > 0 -> RankingTrend.UP
        else -> RankingTrend.DOWN
    }
}

data class TorneioRankingInfo(
    val nome: String,
    val estadoLabel: String,
    val isLive: Boolean,
    val descricao: String
)

data class ClassificacaoRow(
    val posicao: Int,
    val equipa: String,
    val jogos: Int,
    val vitorias: Int,
    val empates: Int,
    val derrotas: Int,
    val golosMarcados: Int,
    val golosSofridos: Int,
    val pontos: Int
)

data class RankingsContent(
    val scope: RankingScope,
    val metric: RankingMetric,
    val period: RankingPeriod,
    val torneioInfo: TorneioRankingInfo?,
    val podium: List<RankingEntry>,
    val entries: List<RankingEntry>,
    val classificacao: List<ClassificacaoRow>,
    val showPodium: Boolean,
    val showClassificacao: Boolean
)
