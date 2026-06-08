package com.hojetembola.app.data.repository

import com.hojetembola.app.ui.rankings.ClassificacaoRow
import com.hojetembola.app.ui.rankings.RankingEntry
import com.hojetembola.app.ui.rankings.RankingMetric
import com.hojetembola.app.ui.rankings.RankingPeriod
import com.hojetembola.app.ui.rankings.RankingScope
import com.hojetembola.app.ui.rankings.RankingsContent
import com.hojetembola.app.ui.rankings.TorneioRankingInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fornece dados de rankings.
 * Dados de exemplo consistentes com o design system — substituir por queries Room/Supabase.
 */
@Singleton
class RankingsRepository @Inject constructor() {

    private val torneioAtivo = TorneioRankingInfo(
        nome = "Torneio Verão 2025",
        estadoLabel = "a_decorrer",
        isLive = true,
        descricao = "Liga · Fut7 · 8 equipas"
    )

    fun getRankings(
        scope: RankingScope,
        metric: RankingMetric,
        period: RankingPeriod
    ): RankingsContent {
        val showClassificacao = metric == RankingMetric.CLASSIFICACAO
        val entries = when {
            showClassificacao -> emptyList()
            scope == RankingScope.GERAL -> geralEntries(metric)
            else -> torneioEntries(metric)
        }

        val showPodium = metric !in setOf(RankingMetric.CARTOES, RankingMetric.CLASSIFICACAO)

        return RankingsContent(
            scope = scope,
            metric = metric,
            period = period,
            torneioInfo = if (scope == RankingScope.TORNEIO) torneioAtivo else null,
            podium = if (showPodium) entries.take(3) else emptyList(),
            entries = if (showPodium) entries.drop(3) else entries,
            classificacao = if (showClassificacao) classificacaoEquipas() else emptyList(),
            showPodium = showPodium,
            showClassificacao = showClassificacao
        )
    }

    private fun geralEntries(metric: RankingMetric): List<RankingEntry> = when (metric) {
        RankingMetric.GOLOS -> listOf(
            entry(1, "Gonçalo", "FC Malcata", "GT", 0xFFEF9F27.toInt(), 12),
            entry(2, "João", "Turbo FC", "JM", 0xFF378ADD.toInt(), 9),
            entry(3, "Rui", "Os Putos", "RF", 0xFFD85A30.toInt(), 7),
            entry(4, "Pedro S.", "Os Putos", "PS", 0xFF2ECC71.toInt(), 6, 1),
            entry(5, "Nuno T.", "Turbo FC", "NT", 0xFF3498DB.toInt(), 5, -2),
            entry(6, "Miguel L.", "FC Malcata", "ML", 0xFFE84C3D.toInt(), 4, 0)
        )
        RankingMetric.JOGOS -> listOf(
            entry(1, "Gonçalo", "FC Malcata", "GT", 0xFFEF9F27.toInt(), 14),
            entry(2, "João", "Turbo FC", "JM", 0xFF378ADD.toInt(), 14),
            entry(3, "Rui", "Os Putos", "RF", 0xFFD85A30.toInt(), 13),
            entry(4, "Pedro S.", "Os Putos", "PS", 0xFF2ECC71.toInt(), 12, 1),
            entry(5, "Nuno T.", "Turbo FC", "NT", 0xFF3498DB.toInt(), 11, 0)
        )
        RankingMetric.ASSISTENCIAS -> listOf(
            entry(1, "Gonçalo", "FC Malcata", "GT", 0xFFEF9F27.toInt(), 8),
            entry(2, "Pedro", "Os Putos", "PS", 0xFF378ADD.toInt(), 6),
            entry(3, "João", "Turbo FC", "JM", 0xFFD85A30.toInt(), 5),
            entry(4, "Rui F.", "FC Malcata", "RF", 0xFFE84C3D.toInt(), 4, 0)
        )
        RankingMetric.MINUTOS -> listOf(
            entry(1, "Gonçalo", "FC Malcata", "GT", 0xFFEF9F27.toInt(), 860),
            entry(2, "João", "Turbo FC", "JM", 0xFF378ADD.toInt(), 820),
            entry(3, "Rui", "Os Putos", "RF", 0xFFD85A30.toInt(), 790),
            entry(4, "Nuno T.", "Turbo FC", "NT", 0xFF3498DB.toInt(), 760, 0)
        )
        RankingMetric.AMARELOS -> listOf(
            entry(1, "Pedro", "Os Putos", "PS", 0xFFEF9F27.toInt(), 4),
            entry(2, "Miguel", "FC Malcata", "ML", 0xFF378ADD.toInt(), 3),
            entry(3, "Rui", "Turbo FC", "RF", 0xFFD85A30.toInt(), 2),
            entry(4, "Nuno T.", "Turbo FC", "NT", 0xFF3498DB.toInt(), 2, 0)
        )
        RankingMetric.VERMELHOS -> listOf(
            entry(1, "Pedro", "Os Putos", "PS", 0xFFEF9F27.toInt(), 1),
            entry(2, "Miguel", "FC Malcata", "ML", 0xFF378ADD.toInt(), 1),
            entry(3, "Gonçalo", "FC Malcata", "GT", 0xFFD85A30.toInt(), 0),
            entry(4, "Rui F.", "Turbo FC", "RF", 0xFF3498DB.toInt(), 0, 0)
        )
        RankingMetric.MVPS -> listOf(
            entry(1, "Gonçalo", "FC Malcata", "GT", 0xFFEF9F27.toInt(), 5),
            entry(2, "João", "Turbo FC", "JM", 0xFF378ADD.toInt(), 3),
            entry(3, "Rui", "Os Putos", "RF", 0xFFD85A30.toInt(), 2),
            entry(4, "Pedro S.", "Os Putos", "PS", 0xFF2ECC71.toInt(), 2, 0)
        )
        else -> geralEntries(RankingMetric.GOLOS)
    }

    private fun torneioEntries(metric: RankingMetric): List<RankingEntry> = when (metric) {
        RankingMetric.GOLOS -> listOf(
            entry(1, "Gonçalo", "FC Malcata", "GT", 0xFFEF9F27.toInt(), 4),
            entry(2, "João M.", "Turbo FC", "JM", 0xFF378ADD.toInt(), 3),
            entry(3, "Pedro S.", "Os Putos", "PS", 0xFFD85A30.toInt(), 2),
            entry(4, "Rui F.", "FC Malcata", "RF", 0xFFE84C3D.toInt(), 2, 0)
        )
        RankingMetric.ASSISTENCIAS -> listOf(
            entry(1, "Gonçalo", "FC Malcata", "GT", 0xFFEF9F27.toInt(), 3),
            entry(2, "Pedro S.", "Os Putos", "PS", 0xFF378ADD.toInt(), 2),
            entry(3, "João M.", "Turbo FC", "JM", 0xFFD85A30.toInt(), 1)
        )
        RankingMetric.MVPS -> listOf(
            entry(1, "Gonçalo", "FC Malcata", "GT", 0xFFEF9F27.toInt(), 2),
            entry(2, "João M.", "Turbo FC", "JM", 0xFF378ADD.toInt(), 1),
            entry(3, "Pedro S.", "Os Putos", "PS", 0xFFD85A30.toInt(), 1)
        )
        RankingMetric.CARTOES -> listOf(
            cartoesEntry(1, "Pedro S.", "Os Putos", 4, 1),
            cartoesEntry(2, "Miguel L.", "FC Malcata", 3, 0),
            cartoesEntry(3, "Rui F.", "Turbo FC", 2, 0)
        )
        else -> torneioEntries(RankingMetric.GOLOS)
    }

    private fun classificacaoEquipas(): List<ClassificacaoRow> = listOf(
        ClassificacaoRow(1, "FC Malcata", 4, 3, 1, 0, 10, 4, 10),
        ClassificacaoRow(2, "Os Putos", 4, 2, 2, 0, 8, 5, 8),
        ClassificacaoRow(3, "Turbo FC", 4, 2, 1, 1, 7, 6, 7),
        ClassificacaoRow(4, "Super Raia", 4, 1, 1, 2, 6, 8, 4)
    )

    private fun entry(
        posicao: Int,
        nome: String,
        equipa: String,
        iniciais: String,
        avatarColor: Int,
        valor: Int,
        variacao: Int? = null
    ) = RankingEntry(posicao, nome, equipa, iniciais, avatarColor, valor, variacao)

    private fun cartoesEntry(
        posicao: Int,
        nome: String,
        equipa: String,
        amarelos: Int,
        vermelhos: Int
    ) = RankingEntry(
        posicao = posicao,
        nome = nome,
        equipa = equipa,
        iniciais = nome.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString(""),
        avatarColor = 0xFF8A9BB8.toInt(),
        valor = amarelos + vermelhos,
        amarelos = amarelos,
        vermelhos = vermelhos
    )
}
