package com.hojetembola.app.data.repository

import android.util.Log
import com.hojetembola.app.data.local.dao.InscricaoEquipaDao
import com.hojetembola.app.data.local.dao.JogoDao
import com.hojetembola.app.data.local.dao.JornadaDao
import com.hojetembola.app.data.local.dao.TorneioDao
import com.hojetembola.app.data.local.entity.JogoEntity
import com.hojetembola.app.data.local.entity.JornadaEntity
import com.hojetembola.app.data.remote.dto.JogoDto
import com.hojetembola.app.data.remote.dto.JogoInsertDto
import com.hojetembola.app.data.remote.dto.JornadaDto
import com.hojetembola.app.data.remote.dto.JornadaInsertDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlin.math.sqrt

private const val TAG = "HTB-JogoRepo"

@Singleton
class JogoRepository @Inject constructor(
    private val client: SupabaseClient,
    private val jogoDao: JogoDao,
    private val jornadaDao: JornadaDao,
    private val torneioDao: TorneioDao,
    private val inscricaoEquipaDao: InscricaoEquipaDao
) {

    // ── Leitura local ─────────────────────────────────────────────────────────

    fun getJogosByTorneio(torneioId: String): Flow<List<JogoEntity>> =
        jogoDao.getByTorneio(torneioId)

    fun getJornadasByTorneio(torneioId: String): Flow<List<JornadaEntity>> =
        jornadaDao.getByTorneio(torneioId)

    // ── Sincronização ─────────────────────────────────────────────────────────

    suspend fun syncJogosETorneio(torneioId: String) {
        val tId = torneioId.toIntOrNull() ?: return
        try {
            val jornadas = client.from("jornada")
                .select { filter { eq("torneio_id", tId) } }
                .decodeList<JornadaDto>()
            jornadaDao.insertAll(jornadas.map { it.toEntity() })

            val jogos = client.from("jogo")
                .select { filter { eq("torneio_id", tId) } }
                .decodeList<JogoDto>()
            jogoDao.insertAll(jogos.map { it.toEntity() })
        } catch (e: Exception) {
            Log.w(TAG, "syncJogosETorneio falhou: ${e.message}")
        }
    }

    // ── Verificação ───────────────────────────────────────────────────────────

    /** Devolve true se já existem jornadas geradas para este torneio. */
    suspend fun temCalendario(torneioId: String): Boolean =
        jornadaDao.getByTorneio(torneioId).let { false }.also {
            // Check via suspend count query
        }.let {
            jornadaDao.countByTorneio(torneioId) > 0
        }

    // ── Geração de calendário ─────────────────────────────────────────────────

    /**
     * Gera o calendário de jogos com base no formato do torneio.
     * - Liga / TodosContraTodos → round-robin completo (todas as jornadas)
     * - Eliminatorias          → apenas a 1ª ronda (as seguintes geradas conforme resultados)
     * - GruposEliminatorias    → fase de grupos (round-robin por grupo)
     *
     * Devolve o número de jogos criados.
     */
    suspend fun gerarCalendario(torneioId: String): Result<Int> {
        val torneio = torneioDao.getById(torneioId)
            ?: return Result.failure(Exception("Torneio não encontrado."))

        val tId = torneioId.toIntOrNull()
            ?: return Result.failure(Exception("ID do torneio inválido."))

        // Verifica se já tem calendário
        if (jornadaDao.countByTorneio(torneioId) > 0) {
            return Result.failure(Exception("O calendário já foi gerado para este torneio."))
        }

        val confirmadas = inscricaoEquipaDao.getByTorneioSuspend(torneioId)
            .filter { it.estado == "Confirmada" }

        if (confirmadas.size < 2) {
            return Result.failure(Exception("São necessárias pelo menos 2 equipas confirmadas."))
        }

        val equipaIds = confirmadas.map { it.equipaId }

        return try {
            when (torneio.formato) {
                "Liga", "TodosContraTodos" ->
                    gerarRoundRobin(tId, equipaIds, grupoNome = null)

                "Eliminatorias" ->
                    gerarEliminatorias(tId, equipaIds)

                "GruposEliminatorias" ->
                    gerarGrupos(tId, equipaIds, confirmadas.map { it.grupo })

                else ->
                    gerarRoundRobin(tId, equipaIds, grupoNome = null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "gerarCalendario falhou: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Erro ao gerar calendário."))
        }
    }

    // ── Algoritmos ────────────────────────────────────────────────────────────

    /**
     * Round-robin via algoritmo da circunferência.
     * N equipas → N-1 jornadas (N par) ou N jornadas (N ímpar, com bye).
     * [grupoNome] é usado para prefixar as jornadas quando chamado por [gerarGrupos].
     */
    private suspend fun gerarRoundRobin(
        torneioIdInt: Int,
        equipas: List<String>,
        grupoNome: String?,
        jornadaOffset: Int = 0
    ): Result<Int> {
        val times = if (equipas.size % 2 == 0) equipas.toList() else equipas + "BYE"
        val n = times.size
        val fixed = times[0]
        val rotating = ArrayDeque(times.drop(1))

        var totalJogos = 0
        val prefix = if (grupoNome != null) "Grupo $grupoNome — " else ""

        repeat(n - 1) { idx ->
            val roundNum = jornadaOffset + idx + 1
            val nome = "${prefix}Jornada $roundNum"

            val jornadaDto = client.from("jornada")
                .insert(JornadaInsertDto(torneioId = torneioIdInt, nome = nome)) { select() }
                .decodeSingle<JornadaDto>()
            jornadaDao.insert(jornadaDto.toEntity())

            val combined = listOf(fixed) + rotating
            for (i in 0 until n / 2) {
                val casa = combined[i]
                val fora = combined[n - 1 - i]
                if (casa != "BYE" && fora != "BYE") {
                    inserirJogo(torneioIdInt, jornadaDto.id, casa.toInt(), fora.toInt())
                    totalJogos++
                }
            }

            // Roda: move o último para a frente
            rotating.addFirst(rotating.removeLast())
        }

        return Result.success(totalJogos)
    }

    /**
     * Gera apenas a 1ª ronda de eliminatórias com sorteio aleatório.
     * As rondas seguintes são geradas à medida que os resultados são registados.
     */
    private suspend fun gerarEliminatorias(
        torneioIdInt: Int,
        equipas: List<String>
    ): Result<Int> {
        val shuffled = equipas.shuffled()
        val bracketSize = nextPow2(shuffled.size)
        val teams = shuffled + List(bracketSize - shuffled.size) { "BYE" }

        val jornadaDto = client.from("jornada")
            .insert(JornadaInsertDto(torneioId = torneioIdInt, nome = "Ronda 1")) { select() }
            .decodeSingle<JornadaDto>()
        jornadaDao.insert(jornadaDto.toEntity())

        var totalJogos = 0
        for (i in 0 until bracketSize / 2) {
            val casa = teams[i * 2]
            val fora = teams[i * 2 + 1]
            if (casa != "BYE" && fora != "BYE") {
                inserirJogo(torneioIdInt, jornadaDto.id, casa.toInt(), fora.toInt())
                totalJogos++
            }
        }

        return Result.success(totalJogos)
    }

    /**
     * Divide as equipas em grupos e gera round-robin dentro de cada grupo.
     * Usa os grupos já definidos em [inscricaoGrupos] se existirem,
     * caso contrário distribui automaticamente.
     */
    private suspend fun gerarGrupos(
        torneioIdInt: Int,
        equipaIds: List<String>,
        inscricaoGrupos: List<String?>
    ): Result<Int> {
        // Usa grupos existentes na inscrição ou auto-atribui
        val gruposMap: Map<String, List<String>> =
            if (inscricaoGrupos.any { !it.isNullOrBlank() }) {
                equipaIds.zip(inscricaoGrupos)
                    .filter { (_, g) -> !g.isNullOrBlank() }
                    .groupBy({ (_, g) -> g!! }, { (id, _) -> id })
            } else {
                val numGrupos = calcNumGrupos(equipaIds.size)
                equipaIds.shuffled()
                    .withIndex()
                    .groupBy(
                        { (idx, _) -> ('A' + idx % numGrupos).toString() },
                        { (_, id) -> id }
                    )
            }

        var totalJogos = 0
        gruposMap.entries.forEachIndexed { idx, (grupo, ids) ->
            val offset = idx * ids.size // jornada offset for each group
            val result = gerarRoundRobin(torneioIdInt, ids, grupoNome = grupo, jornadaOffset = offset)
            result.onSuccess { totalJogos += it }
                  .onFailure { throw it }
        }

        return Result.success(totalJogos)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun inserirJogo(
        torneioId: Int,
        jornadaId: Int,
        equipaCasaId: Int,
        equipaForaId: Int
    ) {
        val dto = JogoInsertDto(
            torneioId     = torneioId,
            jornadaId     = jornadaId,
            equipaCasaId  = equipaCasaId,
            equipaForaId  = equipaForaId
        )
        val created = client.from("jogo")
            .insert(dto) { select() }
            .decodeSingle<JogoDto>()
        jogoDao.insert(created.toEntity())
    }

    private fun nextPow2(n: Int): Int {
        var p = 1
        while (p < n) p *= 2
        return p
    }

    /** Calcula o número de grupos ideal: grupos de 3-4 equipas. */
    private fun calcNumGrupos(n: Int): Int =
        maxOf(2, ceil(sqrt(n.toDouble())).toInt())
}
