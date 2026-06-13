package com.hojetembola.app.data.repository

import android.graphics.Color
import android.util.Log
import com.hojetembola.app.data.local.dao.CartaoRow
import com.hojetembola.app.data.local.dao.ClassificacaoDao
import com.hojetembola.app.data.local.dao.ConvocatoriaJogoDao
import com.hojetembola.app.data.local.dao.EquipaDao
import com.hojetembola.app.data.local.dao.EventoJogoDao
import com.hojetembola.app.data.local.dao.RankingRow
import com.hojetembola.app.data.local.dao.TorneioDao
import com.hojetembola.app.data.local.dao.UtilizadorDao
import com.hojetembola.app.data.local.dao.VotoMvpDao
import com.hojetembola.app.data.local.entity.TorneioEntity
import com.hojetembola.app.data.local.entity.VotoMvpEntity
import com.hojetembola.app.data.remote.dto.ClassificacaoDto
import com.hojetembola.app.data.remote.dto.ConvocatoriaJogoDto
import com.hojetembola.app.data.remote.dto.EquipaDto
import com.hojetembola.app.data.remote.dto.EventoJogoDto
import com.hojetembola.app.data.remote.dto.UtilizadorDto
import com.hojetembola.app.ui.rankings.ClassificacaoRow
import com.hojetembola.app.ui.rankings.RankingEntry
import com.hojetembola.app.ui.rankings.RankingMetric
import com.hojetembola.app.ui.rankings.RankingPeriod
import com.hojetembola.app.ui.rankings.RankingScope
import com.hojetembola.app.ui.rankings.RankingsContent
import com.hojetembola.app.ui.rankings.TorneioRankingInfo
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class VotoMvpSyncDto(
    val id: String,
    @SerialName("jogo_id")      val jogoId: String,
    @SerialName("votante_id")   val votanteId: String,
    @SerialName("votado_id")    val votadoId: String,
    @SerialName("tipo_votante") val tipoVotante: String
)

@Singleton
class RankingsRepository @Inject constructor(
    private val eventoJogoDao: EventoJogoDao,
    private val votoMvpDao: VotoMvpDao,
    private val convocatoriaJogoDao: ConvocatoriaJogoDao,
    private val classificacaoDao: ClassificacaoDao,
    private val torneioDao: TorneioDao,
    private val utilizadorDao: UtilizadorDao,
    private val equipaDao: EquipaDao,
    private val client: SupabaseClient,
    private val userRepository: UserRepository
) {

    fun getMeusTorneios(): Flow<List<TorneioEntity>> = flow {
        val userId = userRepository.getCurrentUser().getOrNull()?.id
        if (userId == null) { emit(emptyList()); return@flow }
        emitAll(torneioDao.getMeusTorneios(userId))
    }

    suspend fun getRankings(
        scope: RankingScope,
        metric: RankingMetric,
        torneioId: String? = null
    ): RankingsContent = try {
        when (scope) {
            RankingScope.GERAL   -> buildGeral(metric)
            RankingScope.TORNEIO -> buildTorneio(metric, torneioId)
        }
    } catch (_: Exception) {
        emptyContent(scope, metric, RankingPeriod.EPOCA_TODA)
    }

    // ── Geral ─────────────────────────────────────────────────────────────────

    private suspend fun buildGeral(metric: RankingMetric): RankingsContent {
        val showPodium = metric !in setOf(RankingMetric.CARTOES, RankingMetric.CLASSIFICACAO, RankingMetric.AMARELOS, RankingMetric.VERMELHOS)

        val entries = when (metric) {
            RankingMetric.GOLOS        -> eventoJogoDao.getRankingGolos().toEntries()
            RankingMetric.ASSISTENCIAS -> eventoJogoDao.getRankingAssistencias().toEntries()
            RankingMetric.MVPS         -> votoMvpDao.getRankingMvps().toEntries()
            RankingMetric.JOGOS        -> convocatoriaJogoDao.getRankingJogos().toEntries()
            RankingMetric.AMARELOS     -> eventoJogoDao.getRankingAmarelos().toEntries()
            RankingMetric.VERMELHOS    -> eventoJogoDao.getRankingVermelhos().toEntries()
            else                       -> emptyList()
        }

        return RankingsContent(
            scope = RankingScope.GERAL,
            metric = metric,
            period = RankingPeriod.EPOCA_TODA,
            torneioInfo = null,
            podium = if (showPodium) entries.take(3) else emptyList(),
            entries = if (showPodium) entries.drop(3) else entries,
            classificacao = emptyList(),
            showPodium = showPodium && entries.isNotEmpty(),
            showClassificacao = false
        )
    }

    // ── Torneio ───────────────────────────────────────────────────────────────

    private suspend fun buildTorneio(metric: RankingMetric, torneioId: String?): RankingsContent {
        val torneio = torneioId?.let { torneioDao.getById(it) }

        val showClassificacao = metric == RankingMetric.CLASSIFICACAO
        val showPodium = !showClassificacao && metric != RankingMetric.CARTOES

        val entries: List<RankingEntry>
        val classificacao: List<ClassificacaoRow>

        if (torneio == null) {
            entries = emptyList()
            classificacao = emptyList()
        } else {
            val tid = torneio.id
            entries = when (metric) {
                RankingMetric.GOLOS        -> eventoJogoDao.getRankingGolosByTorneio(tid).toEntries()
                RankingMetric.ASSISTENCIAS -> eventoJogoDao.getRankingAssistenciasByTorneio(tid).toEntries()
                RankingMetric.MVPS         -> votoMvpDao.getRankingMvpsByTorneio(tid).toEntries()
                RankingMetric.JOGOS        -> convocatoriaJogoDao.getRankingJogosByTorneio(tid).toEntries()
                RankingMetric.CARTOES      -> eventoJogoDao.getRankingCartoesByTorneio(tid).toCartaoEntries()
                else                       -> emptyList()
            }
            classificacao = if (showClassificacao) {
                classificacaoDao.getClassificacaoComNome(tid).map { row ->
                    ClassificacaoRow(
                        posicao       = row.posicao,
                        equipa        = row.equipaNome,
                        jogos         = row.jogos,
                        vitorias      = row.vitorias,
                        empates       = row.empates,
                        derrotas      = row.derrotas,
                        golosMarcados = row.golosMarcados,
                        golosSofridos = row.golosSofridos,
                        pontos        = row.pontos
                    )
                }
            } else emptyList()
        }

        val torneioInfo = torneio?.let {
            TorneioRankingInfo(
                nome       = it.nome,
                estadoLabel = it.estado,
                isLive     = it.estado.lowercase().replace("_", "") == "adecorrer",
                descricao  = "${it.formato} · ${it.modalidade}"
            )
        }

        return RankingsContent(
            scope = RankingScope.TORNEIO,
            metric = metric,
            period = RankingPeriod.TORNEIO_COMPLETO,
            torneioInfo = torneioInfo,
            podium = if (showPodium) entries.take(3) else emptyList(),
            entries = if (showPodium) entries.drop(3) else entries,
            classificacao = classificacao,
            showPodium = showPodium && entries.isNotEmpty(),
            showClassificacao = showClassificacao
        )
    }

    // ── Sync do Supabase → Room ───────────────────────────────────────────────

    suspend fun syncDadosRanking() {
        try {
            val utilizadores = client.from("utilizador").select().decodeList<UtilizadorDto>()
            utilizadorDao.insertAll(utilizadores.map { it.toEntity() })
        } catch (e: Exception) { Log.w(TAG, "sync utilizador: ${e.message}") }

        try {
            val equipas = client.from("equipa").select().decodeList<EquipaDto>()
            equipaDao.insertAll(equipas.map { it.toEntity() })
        } catch (e: Exception) { Log.w(TAG, "sync equipa: ${e.message}") }

        try {
            val eventos = client.from("evento_jogo").select().decodeList<EventoJogoDto>()
            eventoJogoDao.insertAll(eventos.map { it.toEntity() })
        } catch (e: Exception) { Log.w(TAG, "sync evento_jogo: ${e.message}") }

        try {
            val convocatorias = client.from("convocatoria_jogo").select().decodeList<ConvocatoriaJogoDto>()
            convocatoriaJogoDao.insertAll(convocatorias.map { it.toEntity() })
        } catch (e: Exception) { Log.w(TAG, "sync convocatoria_jogo: ${e.message}") }

        try {
            val classificacoes = client.from("classificacao").select().decodeList<ClassificacaoDto>()
            classificacaoDao.insertAll(classificacoes.map { it.toEntity() })
        } catch (e: Exception) { Log.w(TAG, "sync classificacao: ${e.message}") }

        try {
            val votos = client.from("voto_mvp").select().decodeList<VotoMvpSyncDto>()
            votoMvpDao.insertAll(votos.map {
                VotoMvpEntity(
                    id = it.id, jogoId = it.jogoId,
                    votanteId = it.votanteId, votadoId = it.votadoId,
                    tipoVotante = it.tipoVotante
                )
            })
        } catch (e: Exception) { Log.w(TAG, "sync voto_mvp: ${e.message}") }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────


    private fun List<RankingRow>.toEntries(): List<RankingEntry> =
        mapIndexed { i, row ->
            RankingEntry(
                posicao     = i + 1,
                nome        = row.nome,
                equipa      = row.equipaNome,
                iniciais    = initials(row.nome),
                avatarColor = parseColor(row.equipaCor),
                valor       = row.total
            )
        }

    private fun List<CartaoRow>.toCartaoEntries(): List<RankingEntry> =
        mapIndexed { i, row ->
            RankingEntry(
                posicao     = i + 1,
                nome        = row.nome,
                equipa      = row.equipaNome,
                iniciais    = initials(row.nome),
                avatarColor = parseColor(row.equipaCor),
                valor       = row.amarelos + row.vermelhos,
                amarelos    = row.amarelos,
                vermelhos   = row.vermelhos
            )
        }

    private fun initials(nome: String) =
        nome.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")

    private fun parseColor(hex: String): Int =
        try { Color.parseColor(hex) } catch (_: Exception) { 0xFF3D5A80.toInt() }

    private fun emptyContent(scope: RankingScope, metric: RankingMetric, period: RankingPeriod) =
        RankingsContent(
            scope = scope, metric = metric, period = period,
            torneioInfo = null, podium = emptyList(), entries = emptyList(),
            classificacao = emptyList(), showPodium = false, showClassificacao = false
        )

    companion object { private const val TAG = "RankingsRepository" }
}
