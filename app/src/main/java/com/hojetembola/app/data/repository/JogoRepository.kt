package com.hojetembola.app.data.repository

import android.util.Log
import com.hojetembola.app.data.local.dao.ClassificacaoDao
import com.hojetembola.app.data.local.dao.ConvocatoriaJogoDao
import com.hojetembola.app.data.local.dao.EquipaDao
import com.hojetembola.app.data.local.dao.EventoComNome
import com.hojetembola.app.data.local.dao.EventoJogoDao
import com.hojetembola.app.data.local.dao.InscricaoEquipaDao
import com.hojetembola.app.data.local.dao.JogadorInscricaoDao
import com.hojetembola.app.data.local.dao.JogoComEquipas
import com.hojetembola.app.data.local.dao.JogoDao
import com.hojetembola.app.data.local.dao.JornadaDao
import com.hojetembola.app.data.local.dao.SuspensaoDao
import com.hojetembola.app.data.local.dao.TorneioDao
import com.hojetembola.app.data.local.entity.ClassificacaoEntity
import com.hojetembola.app.data.local.entity.ConvocatoriaJogoEntity
import com.hojetembola.app.data.local.entity.EventoJogoEntity
import com.hojetembola.app.data.local.entity.JogoEntity
import com.hojetembola.app.data.local.entity.JornadaEntity
import com.hojetembola.app.data.local.entity.SuspensaoEntity
import com.hojetembola.app.data.remote.dto.ClassificacaoDto
import com.hojetembola.app.data.remote.dto.ClassificacaoUpsertDto
import com.hojetembola.app.data.remote.dto.ConvocatoriaJogoDto
import com.hojetembola.app.data.remote.dto.ConvocatoriaJogoInsertDto
import com.hojetembola.app.data.remote.dto.EquipaDto
import com.hojetembola.app.data.remote.dto.EventoJogoDto
import com.hojetembola.app.data.remote.dto.EventoJogoInsertDto
import com.hojetembola.app.data.remote.dto.JogoDto
import com.hojetembola.app.data.remote.dto.JogoInsertDto
import com.hojetembola.app.data.remote.dto.JornadaDto
import com.hojetembola.app.data.remote.dto.JornadaInsertDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlin.math.sqrt

/** One entry passed to [JogoRepository.definirTitulares] per player. */
data class ConvocatoriaEntrada(
    val utilizadorId: String,
    val equipaId: String,
    val isTitular: Boolean
)

private const val TAG = "HTB-JogoRepo"

@Singleton
class JogoRepository @Inject constructor(
    private val client: SupabaseClient,
    private val jogoDao: JogoDao,
    private val jornadaDao: JornadaDao,
    private val torneioDao: TorneioDao,
    private val inscricaoEquipaDao: InscricaoEquipaDao,
    private val eventoJogoDao: EventoJogoDao,
    private val classificacaoDao: ClassificacaoDao,
    private val equipaDao: EquipaDao,
    private val convocatoriaJogoDao: ConvocatoriaJogoDao,
    private val jogadorInscricaoDao: JogadorInscricaoDao,
    private val suspensaoDao: SuspensaoDao
) {

    // ── Leitura local ─────────────────────────────────────────────────────────

    fun getJogosByTorneio(torneioId: String): Flow<List<JogoEntity>> =
        jogoDao.getByTorneio(torneioId)

    fun getJornadasByTorneio(torneioId: String): Flow<List<JornadaEntity>> =
        jornadaDao.getByTorneio(torneioId)

    // ── Leitura ───────────────────────────────────────────────────────────────

    fun getJogosComEquipas(torneioId: String): Flow<List<JogoComEquipas>> =
        jogoDao.getJogosComEquipas(torneioId)

    fun getJornadasFlow(torneioId: String): Flow<List<JornadaEntity>> =
        jornadaDao.getByTorneio(torneioId)

    fun getEventosComNome(jogoId: String): Flow<List<EventoComNome>> =
        eventoJogoDao.getEventosComNome(jogoId)

    suspend fun getJogoById(jogoId: String): JogoEntity? = jogoDao.getById(jogoId)

    suspend fun getTorneioById(torneioId: String): com.hojetembola.app.data.local.entity.TorneioEntity? =
        torneioDao.getById(torneioId)

    // ── Gestão de jogos (organizador) ─────────────────────────────────────────

    suspend fun iniciarJogo(jogoId: String): Result<Unit> {
        val idInt = jogoId.toIntOrNull() ?: return Result.failure(Exception("ID inválido."))
        return try {
            // Offline-first: atualiza Room primeiro para o UI reagir imediatamente
            jogoDao.updateEstadoMinuto(jogoId, "ao_vivo", 0)
            // Supabase best-effort — falha se o enum ainda não tiver 'EmCurso'
            // (correr no Supabase SQL Editor: ALTER TYPE estado_jogo ADD VALUE IF NOT EXISTS 'EmCurso';)
            try {
                client.from("jogo").update({ set("estado", "EmCurso"); set("minuto_atual", 0) }) {
                    filter { eq("id", idInt) }
                }
            } catch (e: Exception) {
                Log.w(TAG, "iniciarJogo Supabase sync falhou (enum?): ${e.message}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "iniciarJogo falhou: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Erro ao iniciar jogo."))
        }
    }

    suspend fun terminarJogo(jogoId: String, golosCasa: Int, golosVisitante: Int): Result<Unit> {
        val idInt = jogoId.toIntOrNull() ?: return Result.failure(Exception("ID inválido."))
        return try {
            val jogo = jogoDao.getById(jogoId)
            jogoDao.updateEstadoMinuto(jogoId, "terminado", null)
            jogoDao.updateResultado(jogoId, golosCasa, golosVisitante)
            try {
                client.from("jogo").update({
                    set("estado", "Terminado")
                    set("golos_casa", golosCasa)
                    set("golos_fora", golosVisitante)
                    set("minuto_atual", null as Int?)
                }) { filter { eq("id", idInt) } }
            } catch (e: Exception) {
                Log.w(TAG, "terminarJogo Supabase sync falhou: ${e.message}")
            }
            // Mark active suspensions from previous games as cumprida — player sat out this game
            if (jogo != null) {
                marcarSuspensoesCumpridas(jogo.torneioId, jogoId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "terminarJogo falhou: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Erro ao terminar jogo."))
        }
    }

    private suspend fun marcarSuspensoesCumpridas(torneioId: String, jogoAtualId: String) {
        try {
            val ativas = suspensaoDao.getSuspensoesAtivasSuspend(torneioId)
                .filter { it.jogoId != jogoAtualId }
            if (ativas.isEmpty()) return
            suspensaoDao.marcarCumpridasExcetoJogo(torneioId, jogoAtualId)
            Log.d(TAG, "suspensões cumpridas: ${ativas.size} para torneio $torneioId")
            // Sync to Supabase
            val torneioIdInt = torneioId.toIntOrNull() ?: return
            val suspensosUtilIds = ativas.map { it.utilizadorId }
            try {
                client.from("suspensao").update({ set("estado", "Cumprida") }) {
                    filter {
                        eq("torneio_id", torneioIdInt)
                        isIn("utilizador_id", suspensosUtilIds)
                        eq("estado", "Ativa")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "suspensões Supabase cumprida falhou: ${e.message}", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "marcarSuspensoesCumpridas falhou: ${e.message}", e)
        }
    }

    suspend fun getSuspensosIds(torneioId: String): Set<String> =
        try { suspensaoDao.getSuspensosIds(torneioId).toSet() } catch (_: Exception) { emptySet() }

    suspend fun syncSuspensoes(torneioId: String) {
        val tId = torneioId.toIntOrNull() ?: return
        try {
            val rows = client.from("suspensao")
                .select { filter { eq("torneio_id", tId) } }
                .decodeList<com.hojetembola.app.data.remote.dto.SuspensaoReadDto>()
            Log.d(TAG, "syncSuspensoes: ${rows.size} suspensões para torneio $torneioId")
            // Only replace Room when Supabase returns rows — avoids wiping local data
            // during a race condition where the Supabase INSERT hasn't landed yet
            if (rows.isNotEmpty()) {
                suspensaoDao.deleteByTorneio(torneioId)
                suspensaoDao.insertAll(rows.map { it.toEntity() })
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncSuspensoes falhou: ${e.message}", e)
        }
    }

    suspend fun registarEvento(
        jogoId: String,
        tipo: String,
        minuto: Int,
        equipaId: String?,
        jogadorId: String? = null,
        jogadorSaiId: String? = null,
        jogadorEntraId: String? = null,
        assistenciaId: String? = null
    ): Result<Unit> {
        return try {
            val tipoDb = when (tipo) {
                "golo"         -> "Golo"
                "amarelo"      -> "Amarelo"
                "vermelho"     -> "Vermelho"
                "substituicao" -> "Substituicao"
                else           -> tipo.replaceFirstChar { it.uppercase() }
            }
            val dto = EventoJogoInsertDto(
                jogoId = jogoId.toInt(),
                tipo = tipoDb,
                minuto = minuto,
                equipaId = equipaId?.toIntOrNull(),
                jogadorId = jogadorId,
                jogadorSaiId = jogadorSaiId,
                jogadorEntraId = jogadorEntraId
            )
            val created = client.from("evento_jogo")
                .insert(dto) { select() }
                .decodeSingle<EventoJogoDto>()
            // Store in Room — always include assistenciaId even if Supabase column missing
            eventoJogoDao.insert(created.toEntity().copy(assistenciaId = assistenciaId))
            // Best-effort Supabase assist update — requires: ALTER TABLE evento_jogo ADD COLUMN IF NOT EXISTS assistencia_id TEXT;
            if (assistenciaId != null) {
                try {
                    client.from("evento_jogo").update({ set("assistencia_id", assistenciaId) }) {
                        filter { eq("id", created.id) }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "assistencia_id update falhou (correr SQL migration): ${e.message}")
                }
            }

            when (tipo) {
                "golo" -> {
                    val jogo = jogoDao.getById(jogoId)
                    if (jogo != null) {
                        val casaNova = if (equipaId == jogo.equipaCasaId) (jogo.golosCasa ?: 0) + 1 else (jogo.golosCasa ?: 0)
                        val visitNova = if (equipaId == jogo.equipaVisitanteId) (jogo.golosVisitante ?: 0) + 1 else (jogo.golosVisitante ?: 0)
                        jogoDao.updateResultado(jogoId, casaNova, visitNova)
                        jogoId.toIntOrNull()?.let { id ->
                            client.from("jogo").update({
                                set("golos_casa", casaNova)
                                set("golos_fora", visitNova)
                            }) { filter { eq("id", id) } }
                        }
                    }
                }
                "amarelo" -> if (jogadorId != null) {
                    processAmarelo(jogoId, minuto, equipaId, jogadorId)
                }
                "vermelho" -> if (jogadorId != null) {
                    registarSuspensao(jogadorId, jogoId, "vermelho")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "registarEvento falhou: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Erro ao registar evento."))
        }
    }

    private suspend fun processAmarelo(jogoId: String, minuto: Int, equipaId: String?, jogadorId: String) {
        val amarelos = eventoJogoDao.countAmarelosByJogadorJogo(jogadorId, jogoId)
        if (amarelos >= 2) {
            // 2º amarelo → vermelho automático + expulsão
            insertVermelhoAutomatico(jogoId, minuto, equipaId, jogadorId)
            registarSuspensao(jogadorId, jogoId, "acumulacao_amarelos")
            return
        }
        // Verificar acumulação de amarelos no torneio
        val jogo = jogoDao.getById(jogoId) ?: return
        val torneio = torneioDao.getById(jogo.torneioId) ?: return
        val threshold = torneio.amarelasParaSuspensao
        if (threshold > 0) {
            val totalAmarelos = eventoJogoDao.countAmarelosByJogadorTorneio(jogadorId, jogo.torneioId)
            if (totalAmarelos % threshold == 0) {
                registarSuspensao(jogadorId, jogoId, "acumulacao_amarelos")
            }
        }
    }

    private suspend fun insertVermelhoAutomatico(jogoId: String, minuto: Int, equipaId: String?, jogadorId: String) {
        try {
            val vermelhoDto = EventoJogoInsertDto(
                jogoId = jogoId.toInt(), tipo = "Vermelho", minuto = minuto,
                equipaId = equipaId?.toIntOrNull(), jogadorId = jogadorId
            )
            val created = client.from("evento_jogo").insert(vermelhoDto) { select() }.decodeSingle<EventoJogoDto>()
            eventoJogoDao.insert(created.toEntity())
        } catch (e: Exception) {
            Log.e(TAG, "vermelho automático falhou, gravando local: ${e.message}", e)
            eventoJogoDao.insert(EventoJogoEntity(
                id = UUID.randomUUID().toString(), jogoId = jogoId, tipo = "vermelho",
                minuto = minuto, equipaId = equipaId, jogadorId = jogadorId, isSynced = false
            ))
        }
    }

    private suspend fun registarSuspensao(jogadorId: String, jogoId: String, motivo: String) {
        val jogo = jogoDao.getById(jogoId) ?: return
        val entityId = UUID.randomUUID().toString()
        val entity = SuspensaoEntity(
            id = entityId,
            utilizadorId = jogadorId,
            torneioId = jogo.torneioId,
            motivo = motivo,
            jogoId = jogoId
        )
        // Room first
        try {
            suspensaoDao.insert(entity)
            Log.d(TAG, "suspensão criada em Room: jogador=$jogadorId motivo=$motivo")
        } catch (e: Exception) {
            Log.e(TAG, "suspensão Room falhou: ${e.message}", e)
        }
        // Supabase best-effort — uses jogo_suspenso_id (existing schema column name)
        val torneioIdInt = jogo.torneioId.toIntOrNull()
        val jogoIdInt    = jogoId.toIntOrNull()
        if (torneioIdInt != null && jogoIdInt != null) {
            try {
                client.from("suspensao").insert(
                    com.hojetembola.app.data.remote.dto.SuspensaoInsertDto(
                        utilizadorId   = jogadorId,
                        torneioId      = torneioIdInt,
                        jogoSuspensoId = jogoIdInt,
                        motivo         = motivo
                    )
                )
                Log.d(TAG, "suspensão guardada no Supabase: jogador=$jogadorId motivo=$motivo")
            } catch (e: Exception) {
                Log.e(TAG, "suspensão Supabase falhou: ${e.message}", e)
            }
        }
    }

    /** Persiste o minuto atual de um jogo em curso apenas no Room (sem hit ao Supabase). */
    suspend fun atualizarMinuto(jogoId: String, minuto: Int) {
        try { jogoDao.updateEstadoMinuto(jogoId, "ao_vivo", minuto) } catch (_: Exception) {}
    }

    suspend fun syncEventos(jogoId: String) {
        val jogoIdInt = jogoId.toIntOrNull() ?: return
        try {
            val eventos = client.from("evento_jogo")
                .select { filter { eq("jogo_id", jogoIdInt) } }
                .decodeList<EventoJogoDto>()
                .map { it.toEntity() }
            eventoJogoDao.deleteByJogo(jogoId)
            eventoJogoDao.insertAll(eventos)
        } catch (e: Exception) {
            Log.w(TAG, "syncEventos falhou: ${e.message}")
        }
    }

    // ── Convocatórias / Titulares ─────────────────────────────────────────────

    /** Live flow of all convocatoria entries for a game (used by the ViewModel). */
    fun getConvocatoriaFlow(jogoId: String): Flow<List<ConvocatoriaJogoEntity>> =
        convocatoriaJogoDao.getByJogo(jogoId)

    /**
     * Persists the starting lineup for both teams.
     * Offline-first: writes to Room immediately, then syncs to Supabase.
     *
     * Supabase requirement (run once in SQL Editor):
     *   ALTER TABLE convocatoria_jogo
     *       ADD COLUMN IF NOT EXISTS is_titular BOOLEAN NOT NULL DEFAULT false;
     */
    suspend fun definirTitulares(
        jogoId: String,
        entradas: List<ConvocatoriaEntrada>
    ): Result<Unit> {
        return try {
            val jogoIdInt = jogoId.toIntOrNull()
                ?: return Result.failure(Exception("ID inválido."))

            // 1. Clear previous convocatorias for this game in Room
            convocatoriaJogoDao.deleteByJogo(jogoId)

            // 2. Write to Room immediately (offline-first)
            val entities = entradas.map { e ->
                ConvocatoriaJogoEntity(
                    id           = UUID.randomUUID().toString(),
                    jogoId       = jogoId,
                    utilizadorId = e.utilizadorId,
                    equipaId     = e.equipaId,
                    isTitular    = e.isTitular
                )
            }
            convocatoriaJogoDao.insertAll(entities)

            // 3. Sync to Supabase (best-effort)
            try {
                // Delete remote entries
                client.from("convocatoria_jogo")
                    .delete { filter { eq("jogo_id", jogoIdInt) } }

                // Batch insert and get back real IDs
                val insertDtos = entradas.mapNotNull { e ->
                    val equipaIdInt = e.equipaId.toIntOrNull() ?: return@mapNotNull null
                    ConvocatoriaJogoInsertDto(
                        jogoId       = jogoIdInt,
                        utilizadorId = e.utilizadorId,
                        equipaId     = equipaIdInt,
                        isTitular    = e.isTitular
                    )
                }
                if (insertDtos.isNotEmpty()) {
                    val created = client.from("convocatoria_jogo")
                        .insert(insertDtos) { select() }
                        .decodeList<ConvocatoriaJogoDto>()

                    // Replace Room entities with real Supabase IDs
                    convocatoriaJogoDao.deleteByJogo(jogoId)
                    convocatoriaJogoDao.insertAll(created.map { it.toEntity() })
                }
            } catch (e: Exception) {
                Log.e(TAG, "definirTitulares Supabase sync falhou: ${e.message}", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "definirTitulares falhou: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Erro ao definir titulares."))
        }
    }

    /** Fetches convocatoria for a game from Supabase and caches in Room.
     *  Only replaces Room data when Supabase returns results — avoids wiping
     *  the local lineup if the remote row is missing or RLS blocks the read. */
    suspend fun syncConvocatoria(jogoId: String) {
        val jogoIdInt = jogoId.toIntOrNull() ?: return
        try {
            val convocatorias = client.from("convocatoria_jogo")
                .select { filter { eq("jogo_id", jogoIdInt) } }
                .decodeList<ConvocatoriaJogoDto>()
            Log.d(TAG, "syncConvocatoria: ${convocatorias.size} registos para jogo $jogoId")
            if (convocatorias.isNotEmpty()) {
                convocatoriaJogoDao.deleteByJogo(jogoId)
                convocatoriaJogoDao.insertAll(convocatorias.map { it.toEntity() })
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncConvocatoria falhou: ${e.message}", e)
        }
    }

    // ── Classificação ─────────────────────────────────────────────────────────

    fun getClassificacao(torneioId: String): Flow<List<ClassificacaoEntity>> =
        classificacaoDao.getByTorneio(torneioId)

    /** Returns a map of equipaId → equipa nome for all teams inscribed in the torneio. */
    suspend fun getEquipaNomesMap(torneioId: String): Map<String, String> {
        return inscricaoEquipaDao.getByTorneioSuspend(torneioId).mapNotNull { inscricao ->
            equipaDao.getById(inscricao.equipaId)?.let { inscricao.equipaId to it.nome }
        }.toMap()
    }

    /**
     * Recalcula a classificação a partir dos jogos terminados e guarda em Room + Supabase.
     */
    suspend fun recalcularClassificacao(torneioId: String) {
        try {
            val jogos = jogoDao.getByTorneio(torneioId).first().filter { it.estado == "terminado" }
            val inscricoes = inscricaoEquipaDao.getByTorneioSuspend(torneioId)
                .filter { it.estado == "Confirmada" }

            val statsMap = mutableMapOf<String, MutableList<Int>>() // equipaId -> [J,V,E,D,GM,GS]
            inscricoes.forEach { statsMap[it.equipaId] = mutableListOf(0,0,0,0,0,0) }

            jogos.forEach { jogo ->
                val gc = jogo.golosCasa ?: 0
                val gv = jogo.golosVisitante ?: 0
                val casa = statsMap.getOrPut(jogo.equipaCasaId) { mutableListOf(0,0,0,0,0,0) }
                val visit = statsMap.getOrPut(jogo.equipaVisitanteId) { mutableListOf(0,0,0,0,0,0) }
                casa[0]++; visit[0]++ // jogos
                casa[4] += gc; casa[5] += gv
                visit[4] += gv; visit[5] += gc
                when {
                    gc > gv -> { casa[1]++; visit[3]++ }
                    gc < gv -> { visit[1]++; casa[3]++ }
                    else    -> { casa[2]++; visit[2]++ }
                }
            }

            val entidades = statsMap.entries
                .map { (equipaId, s) ->
                    val pts = s[1]*3 + s[2]
                    ClassificacaoEntity(
                        id = "${torneioId}_${equipaId}",
                        torneioId = torneioId,
                        equipaId = equipaId,
                        jogos = s[0], vitorias = s[1], empates = s[2], derrotas = s[3],
                        golosMarcados = s[4], golosSofridos = s[5], pontos = pts,
                        posicao = 0
                    )
                }
                .sortedWith(compareByDescending<ClassificacaoEntity> { it.pontos }
                    .thenByDescending { it.golosMarcados - it.golosSofridos }
                    .thenByDescending { it.golosMarcados })
                .mapIndexed { i, e -> e.copy(posicao = i + 1) }

            classificacaoDao.insertAll(entidades)

            // Upsert to Supabase so all devices see the updated standings
            try {
                val tId = torneioId.toIntOrNull() ?: return
                val upserts = entidades.mapNotNull { e ->
                    val eId = e.equipaId.toIntOrNull() ?: return@mapNotNull null
                    ClassificacaoUpsertDto(
                        torneioId     = tId,
                        equipaId      = eId,
                        jogos         = e.jogos,
                        vitorias      = e.vitorias,
                        empates       = e.empates,
                        derrotas      = e.derrotas,
                        golosMarcados = e.golosMarcados,
                        golosSofridos = e.golosSofridos,
                        pontos        = e.pontos,
                        posicao       = e.posicao
                    )
                }
                if (upserts.isNotEmpty()) {
                    client.from("classificacao").upsert(upserts) {
                        onConflict = "torneio_id,equipa_id"
                    }
                    Log.d(TAG, "recalcularClassificacao: ${upserts.size} linhas guardadas no Supabase")
                }
            } catch (e: Exception) {
                Log.e(TAG, "recalcularClassificacao upsert Supabase falhou: ${e.message}", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "recalcularClassificacao falhou: ${e.message}", e)
        }
    }

    suspend fun syncClassificacao(torneioId: String) {
        val tId = torneioId.toIntOrNull() ?: return
        try {
            val rows = client.from("classificacao")
                .select { filter { eq("torneio_id", tId) } }
                .decodeList<ClassificacaoDto>()
            Log.d(TAG, "syncClassificacao: ${rows.size} linhas para torneio $torneioId")
            if (rows.isNotEmpty()) {
                classificacaoDao.insertAll(rows.map { it.toEntity() })
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncClassificacao falhou: ${e.message}", e)
        }
    }

    // ── Sincronização ─────────────────────────────────────────────────────────

    suspend fun syncJogosETorneio(torneioId: String) {
        val tId = torneioId.toIntOrNull() ?: return
        try {
            val jornadas = client.from("jornada")
                .select { filter { eq("torneio_id", tId) } }
                .decodeList<JornadaDto>()
            // Limpa cache antes de reinserir — garante que dados apagados remotamente
            // não ficam em cache local (ex: calendário regenerado)
            jornadaDao.deleteByTorneio(torneioId)
            jornadaDao.insertAll(jornadas.map { it.toEntity() })

            val jogos = client.from("jogo")
                .select { filter { eq("torneio_id", tId) } }
                .decodeList<JogoDto>()
            jogoDao.deleteByTorneio(torneioId)
            jogoDao.insertAll(jogos.map { it.toEntity() })

            // Sync equipa records needed for the JOIN in getJogosComEquipas.
            // This ensures the calendar always shows real team names even after
            // a local DB wipe (fallbackToDestructiveMigration on version bump).
            val equipaIds = jogos.flatMap { listOf(it.equipaCasaId, it.equipaForaId) }
                .toSet().toList()
            Log.d(TAG, "syncJogosETorneio: a sincronizar ${equipaIds.size} equipas: $equipaIds")
            if (equipaIds.isNotEmpty()) {
                try {
                    val equipas = client.from("equipa")
                        .select { filter { isIn("id", equipaIds) } }
                        .decodeList<EquipaDto>()
                    Log.d(TAG, "syncJogosETorneio: ${equipas.size} equipas recebidas do Supabase")
                    if (equipas.isNotEmpty()) {
                        equipaDao.insertAll(equipas.map { it.toEntity() })
                        Log.d(TAG, "syncJogosETorneio: equipas guardadas no Room")
                    } else {
                        Log.w(TAG, "syncJogosETorneio: Supabase devolveu 0 equipas para IDs $equipaIds — verificar RLS")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "syncEquipas (via jogo) falhou: ${e.message}", e)
                }
            }
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

        // Sincroniza do Supabase antes de verificar — evita falso-positivo quando
        // o calendário foi apagado remotamente mas o Room ainda tem cache antiga
        syncJogosETorneio(torneioId)

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

        val result = try {
            when (torneio.formato) {
                "Liga", "TodosContraTodos" -> {
                    // Número de rondas numa volta (N par → N-1; N ímpar → N)
                    val firstLegRounds =
                        if (equipaIds.size % 2 == 0) equipaIds.size - 1 else equipaIds.size
                    var total = 0
                    // 1ª volta
                    gerarRoundRobin(tId, equipaIds, grupoNome = null)
                        .onSuccess { total += it }
                        .onFailure { throw it }
                    // 2ª volta — mesmos pares, casa/fora invertidos, jornadas continuadas
                    gerarRoundRobin(
                        tId, equipaIds,
                        grupoNome      = null,
                        jornadaOffset  = firstLegRounds,
                        swapHomes      = true
                    ).onSuccess { total += it }
                     .onFailure { throw it }
                    Result.success(total)
                }

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

        // Transição de estado: torneio passa a "ADecorrer" logo que o calendário é gerado
        result.onSuccess {
            try {
                torneioDao.updateEstado(torneioId, "ADecorrer")
                client.from("torneio")
                    .update({ set("estado", "ADecorrer") }) { filter { eq("id", tId) } }
                Log.i(TAG, "Torneio $torneioId → ADecorrer")
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao atualizar estado do torneio: ${e.message}")
            }
        }

        return result
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
        jornadaOffset: Int = 0,
        swapHomes: Boolean = false
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
                val a = combined[i]
                val b = combined[n - 1 - i]
                // Na 2ª volta invertemos quem joga em casa
                val (casa, fora) = if (swapHomes) Pair(b, a) else Pair(a, b)
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
