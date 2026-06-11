package com.hojetembola.app.data.repository

import android.util.Log
import com.hojetembola.app.data.local.dao.EquipaDao
import com.hojetembola.app.data.local.dao.InscricaoEquipaDao
import com.hojetembola.app.data.local.dao.MembroEquipaDao
import com.hojetembola.app.data.local.dao.TorneioDao
import com.hojetembola.app.data.local.dao.UtilizadorDao
import com.hojetembola.app.data.local.entity.EquipaEntity
import com.hojetembola.app.data.local.entity.InscricaoComEquipa
import com.hojetembola.app.data.local.entity.InscricaoEquipaEntity
import com.hojetembola.app.data.local.entity.MembroComNome
import com.hojetembola.app.data.local.entity.UtilizadorEntity
import com.hojetembola.app.data.remote.dto.EquipaDto
import com.hojetembola.app.data.remote.dto.EquipaInsertDto
import com.hojetembola.app.data.remote.dto.InscricaoEquipaDto
import com.hojetembola.app.data.remote.dto.InscricaoEquipaInsertDto
import com.hojetembola.app.data.remote.dto.MembroEquipaDto
import com.hojetembola.app.data.remote.dto.MembroEquipaInsertDto
import com.hojetembola.app.data.remote.dto.UtilizadorDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "HTB-EquipaRepo"

@Singleton
class EquipaRepository @Inject constructor(
    private val client: SupabaseClient,
    private val equipaDao: EquipaDao,
    private val inscricaoEquipaDao: InscricaoEquipaDao,
    private val membroEquipaDao: MembroEquipaDao,
    private val utilizadorDao: UtilizadorDao,
    private val torneioDao: TorneioDao
) {

    // ── Leitura local — equipas ───────────────────────────────────────────────

    /** Equipas onde o utilizador é capitão. */
    fun getMinhasEquipas(capitaoId: String): Flow<List<EquipaEntity>> =
        equipaDao.getByCapitao(capitaoId)

    /**
     * Todas as equipas do utilizador: como capitão E como membro.
     * Combina dois flows e remove duplicados.
     */
    fun getTodasMinhasEquipas(userId: String): Flow<List<EquipaEntity>> =
        combine(
            equipaDao.getByCapitao(userId),
            equipaDao.getEquipasByMembro(userId)
        ) { comoCapitao, comoMembro ->
            (comoCapitao + comoMembro).distinctBy { it.id }
        }

    /** Inscrições de um torneio específico. */
    fun getInscricoesDoTorneio(torneioId: String): Flow<List<InscricaoEquipaEntity>> =
        inscricaoEquipaDao.getByTorneio(torneioId)

    /** Inscrições com dados da equipa — para o ecrã de Gerir Torneio. */
    fun getInscricoesComEquipa(torneioId: String): Flow<List<InscricaoComEquipa>> =
        inscricaoEquipaDao.getByTorneioComEquipa(torneioId)

    /** Verifica localmente se esta equipa já está inscrita neste torneio (estado activo). */
    suspend fun jaInscrita(torneioId: String, equipaId: String): Boolean =
        inscricaoEquipaDao.countInscricaoAtiva(torneioId, equipaId) > 0

    /** Devolve o estado actual da inscrição, ou null se não existe nenhuma. */
    suspend fun getEstadoInscricao(torneioId: String, equipaId: String): String? =
        inscricaoEquipaDao.getByTorneioAndEquipa(torneioId, equipaId)?.estado

    // ── Leitura local — membros ───────────────────────────────────────────────

    /** Flow de membros com nomes (requer JOIN com utilizador em cache). */
    fun getMembrosComNome(equipaId: String): Flow<List<MembroComNome>> =
        membroEquipaDao.getMembrosComNome(equipaId)

    /** Contagem de membros activos de uma equipa. */
    suspend fun contarMembros(equipaId: String): Int =
        membroEquipaDao.countMembros(equipaId)

    // ── Sincronização ─────────────────────────────────────────────────────────

    /**
     * Sincroniza as equipas onde o utilizador é membro (não apenas capitão).
     * Necessário para o perfil mostrar as equipas aceites via convite.
     */
    suspend fun syncMinhasEquipasComoMembro(userId: String) {
        try {
            // 1. Pull de todas as filiações deste utilizador
            val memberships = client.from("membro_equipa")
                .select { filter { eq("utilizador_id", userId) } }
                .decodeList<MembroEquipaDto>()
                .map { it.toEntity() }

            if (memberships.isEmpty()) return
            membroEquipaDao.insertAll(memberships)

            // 2. Pull das equipas correspondentes (para o JOIN local funcionar)
            val equipaIds = memberships.mapNotNull { it.equipaId.toIntOrNull() }
            if (equipaIds.isEmpty()) return

            val equipas = client.from("equipa")
                .select { filter { isIn("id", equipaIds) } }
                .decodeList<EquipaDto>()
                .map { it.toEntity() }
            if (equipas.isNotEmpty()) equipaDao.insertAll(equipas)
        } catch (e: Exception) {
            Log.w(TAG, "syncMinhasEquipasComoMembro falhou: ${e.message}")
        }
    }

    /** Faz pull das equipas onde o utilizador é capitão e actualiza o Room. */
    suspend fun syncEquipas(capitaoId: String) {
        try {
            val equipas = client.from("equipa")
                .select { filter { eq("capitao_id", capitaoId) } }
                .decodeList<EquipaDto>()
                .map { it.toEntity() }
            if (equipas.isNotEmpty()) equipaDao.insertAll(equipas)
        } catch (e: Exception) {
            Log.w(TAG, "syncEquipas falhou: ${e.message}")
        }
    }

    /** Faz pull das inscrições de um torneio e actualiza o Room. */
    suspend fun syncInscricoes(torneioId: String) {
        try {
            val inscricoes = client.from("inscricao_equipa")
                .select { filter { eq("torneio_id", torneioId.toInt()) } }
                .decodeList<InscricaoEquipaDto>()
                .map { it.toEntity() }
            if (inscricoes.isNotEmpty()) inscricaoEquipaDao.insertAll(inscricoes)
        } catch (e: Exception) {
            Log.w(TAG, "syncInscricoes falhou: ${e.message}")
        }
    }

    /**
     * Faz pull dos membros de uma equipa e dos seus perfis de utilizador.
     * Guarda ambos em Room para que o JOIN funcione offline.
     */
    suspend fun syncMembros(equipaId: String) {
        val equipaIdInt = equipaId.toIntOrNull() ?: return
        try {
            val membros = client.from("membro_equipa")
                .select { filter { eq("equipa_id", equipaIdInt) } }
                .decodeList<MembroEquipaDto>()
                .map { it.toEntity() }

            if (membros.isNotEmpty()) {
                membroEquipaDao.insertAll(membros)

                // Sincroniza os utilizadores para que o JOIN funcione
                val utilizadorIds = membros.map { it.utilizadorId }
                val utilizadores = client.from("utilizador")
                    .select { filter { isIn("id", utilizadorIds) } }
                    .decodeList<UtilizadorDto>()
                    .map { it.toEntity() }
                utilizadorDao.insertAll(utilizadores)
            }
        } catch (e: Exception) {
            Log.w(TAG, "syncMembros falhou: ${e.message}")
        }
    }

    // ── Criar equipa ──────────────────────────────────────────────────────────

    /**
     * Cria uma nova equipa no Supabase, guarda-a localmente e adiciona o
     * capitão como primeiro membro da equipa.
     */
    suspend fun criarEquipa(
        nome: String,
        iniciais: String,
        corAvatar: String,
        cidade: String?,
        capitaoId: String
    ): Result<EquipaEntity> {
        return try {
            val dto = EquipaInsertDto(
                nome      = nome.trim(),
                iniciais  = iniciais.trim().uppercase(),
                corAvatar = corAvatar,
                cidade    = cidade?.ifBlank { null },
                capitaoId = capitaoId
            )
            val created = client.from("equipa")
                .insert(dto) { select() }
                .decodeSingle<EquipaDto>()

            val entity = created.toEntity()
            equipaDao.insert(entity)
            Log.i(TAG, "Equipa ${entity.id} criada com sucesso.")

            // Adiciona o capitão como membro automaticamente
            adicionarMembroInterno(entity.id, capitaoId)

            Result.success(entity)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao criar equipa: ${e.message}", e)
            Result.failure(Exception(buildErrorMessage(e)))
        }
    }

    // ── Editar equipa ─────────────────────────────────────────────────────────

    suspend fun getEquipaById(equipaId: String): EquipaEntity? = equipaDao.getById(equipaId)

    suspend fun updateEquipa(
        equipaId: String,
        nome: String,
        iniciais: String,
        corAvatar: String,
        cidade: String?
    ): Result<Unit> {
        return try {
            client.from("equipa")
                .update({
                    set("nome", nome.trim())
                    set("iniciais", iniciais.trim().uppercase())
                    set("cor_avatar", corAvatar)
                    set("cidade", cidade?.ifBlank { null })
                }) { filter { eq("id", equipaId.toInt()) } }
            val cached = equipaDao.getById(equipaId)
            if (cached != null) {
                equipaDao.insert(cached.copy(
                    nome = nome.trim(),
                    iniciais = iniciais.trim().uppercase(),
                    corAvatar = corAvatar,
                    cidade = cidade?.ifBlank { null }
                ))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "updateEquipa falhou: ${e.message}")
            Result.failure(Exception("Não foi possível atualizar a equipa."))
        }
    }

    // ── Inscrever equipa ──────────────────────────────────────────────────────

    /**
     * Inscreve uma equipa num torneio.
     * Valida primeiro:
     *   1. Se a equipa já está inscrita.
     *   2. Se algum membro já está inscrito neste torneio com outra equipa.
     * [torneioId] e [equipaId] devem ser IDs inteiros em formato String.
     */
    suspend fun inscreverEquipa(
        torneioId: String,
        equipaId: String,
        capitaoId: String
    ): Result<InscricaoEquipaEntity> {
        if (jaInscrita(torneioId, equipaId)) {
            return Result.failure(Exception("Esta equipa já está inscrita neste torneio."))
        }

        val torneioIdInt = torneioId.toIntOrNull()
            ?: return Result.failure(Exception("ID do torneio inválido."))
        val equipaIdInt = equipaId.toIntOrNull()
            ?: return Result.failure(Exception("ID da equipa inválido."))

        // Verifica conflito de jogadores antes de tentar a inscrição
        val conflito = verificarConflitoJogadores(torneioId, equipaId)
        if (conflito != null) {
            return Result.failure(Exception(conflito))
        }

        return try {
            // Re-inscrição após rejeição: apagar a linha rejeitada e inserir de novo.
            // Usar DELETE+INSERT (em vez de UPDATE) para:
            //   1. respeitar o RLS (capitão pode INSERT e DELETE das suas próprias inscrições)
            //   2. disparar o trigger de notificação ao organizador (só activa em INSERT)
            val existente = inscricaoEquipaDao.getByTorneioAndEquipa(torneioId, equipaId)
            if (existente != null && existente.estado == "Rejeitada") {
                existente.id.toIntOrNull()?.let { existenteIdInt ->
                    client.from("inscricao_equipa")
                        .delete { filter { eq("id", existenteIdInt) } }
                    inscricaoEquipaDao.delete(existente)
                    Log.i(TAG, "Inscrição rejeitada ${existente.id} removida para re-inscrição.")
                }
            }

            val dto = InscricaoEquipaInsertDto(
                torneioId     = torneioIdInt,
                equipaId      = equipaIdInt,
                inscritaPorId = capitaoId,
                estado        = "Pendente"
            )
            val created = client.from("inscricao_equipa")
                .insert(dto) { select() }
                .decodeSingle<InscricaoEquipaDto>()

            val entity = created.toEntity()
            inscricaoEquipaDao.insert(entity)
            Log.i(TAG, "Inscrição ${entity.id} criada com sucesso.")
            Result.success(entity)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao inscrever equipa: ${e.message}", e)
            Result.failure(Exception(buildErrorMessage(e)))
        }
    }

    /**
     * Verifica se algum membro da equipa já está inscrito neste torneio com outra equipa.
     * Sincroniza os dados antes de verificar para garantir frescura.
     * Devolve null se não há conflito, ou a mensagem de erro se há.
     */
    private suspend fun verificarConflitoJogadores(
        torneioId: String,
        equipaId: String
    ): String? {
        return try {
            // Garante que temos as inscrições e membros actualizados
            syncInscricoes(torneioId)
            syncMembros(equipaId)

            // Obter membros activos desta equipa (em cache após sync)
            val membros = membroEquipaDao.getMembrosAtivos(equipaId)
            if (membros.isEmpty()) return null

            // Verifica se o organizador do torneio faz parte da equipa
            val organizadorId = torneioDao.getById(torneioId)?.organizadorId
            if (organizadorId != null && membros.any { it.utilizadorId == organizadorId }) {
                return "O organizador não pode inscrever uma equipa em que participa no seu próprio torneio."
            }

            // Obter todas as outras inscrições activas neste torneio
            val outrasInscricoes = inscricaoEquipaDao.getByTorneioSuspend(torneioId)
                .filter { it.equipaId != equipaId && it.estado !in listOf("Rejeitada", "Desistente") }
            if (outrasInscricoes.isEmpty()) return null

            // Sincroniza membros das outras equipas para ter dados locais
            outrasInscricoes.forEach { syncMembros(it.equipaId) }

            // Verifica sobreposição
            for (membro in membros) {
                for (outraInscricao in outrasInscricoes) {
                    if (membroEquipaDao.countMembroAtivo(outraInscricao.equipaId, membro.utilizadorId) > 0) {
                        val nome = utilizadorDao.getById(membro.utilizadorId)?.nome
                            ?: "Um jogador"
                        return "\"$nome\" já está inscrito neste torneio com outra equipa."
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "verificarConflitoJogadores falhou (será validado pelo servidor): ${e.message}")
            null // Se falhar, o trigger DB vai barrar no servidor
        }
    }

    /**
     * Devolve os IDs dos utilizadores já inscritos neste torneio por outra equipa.
     * Usado para assinalar conflitos no ecrã de seleção de jogadores.
     */
    suspend fun getUtilizadoresNoTorneio(torneioId: String, equipaId: String): List<String> {
        return try {
            syncInscricoes(torneioId)
            val outrasInscricoes = inscricaoEquipaDao.getByTorneioSuspend(torneioId)
                .filter { it.equipaId != equipaId && it.estado !in listOf("Rejeitada", "Desistente") }
            outrasInscricoes.forEach { syncMembros(it.equipaId) }
            membroEquipaDao.getUtilizadoresNoTorneio(torneioId, equipaId)
        } catch (e: Exception) {
            Log.w(TAG, "getUtilizadoresNoTorneio falhou: ${e.message}")
            emptyList()
        }
    }

    // ── Gestão de inscrições (organizador) ────────────────────────────────────

    /** Sincroniza inscrições e equipas de um torneio (para o Gerir Torneio). */
    suspend fun syncInscricoesComEquipas(torneioId: String) {
        try {
            syncInscricoes(torneioId)
            val inscricoes = inscricaoEquipaDao.getByTorneioSuspend(torneioId)
            inscricoes.forEach { syncEquipaById(it.equipaId) }
        } catch (e: Exception) {
            Log.w(TAG, "syncInscricoesComEquipas falhou: ${e.message}")
        }
    }

    private suspend fun syncEquipaById(equipaId: String) {
        val equipaIdInt = equipaId.toIntOrNull() ?: return
        try {
            val dto = client.from("equipa")
                .select { filter { eq("id", equipaIdInt) } }
                .decodeList<EquipaDto>()
                .firstOrNull() ?: return
            equipaDao.insert(dto.toEntity())
        } catch (_: Exception) {}
    }

    /** Aceita uma inscrição (Pendente → Confirmada). */
    suspend fun aceitarInscricao(inscricaoId: String): Result<Unit> {
        val id = inscricaoId.toIntOrNull() ?: return Result.failure(Exception("ID inválido."))
        return try {
            client.from("inscricao_equipa")
                .update({ set("estado", "Confirmada") }) {
                    filter { eq("id", id) }
                }
            inscricaoEquipaDao.updateEstadoById(inscricaoId, "Confirmada")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "aceitarInscricao falhou: ${e.message}", e)
            Result.failure(Exception(buildErrorMessage(e)))
        }
    }

    /** Rejeita uma inscrição (qualquer estado → Rejeitada). */
    suspend fun rejeitarInscricao(inscricaoId: String): Result<Unit> {
        val id = inscricaoId.toIntOrNull() ?: return Result.failure(Exception("ID inválido."))
        return try {
            client.from("inscricao_equipa")
                .update({ set("estado", "Rejeitada") }) {
                    filter { eq("id", id) }
                }
            inscricaoEquipaDao.updateEstadoById(inscricaoId, "Rejeitada")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "rejeitarInscricao falhou: ${e.message}", e)
            Result.failure(Exception(buildErrorMessage(e)))
        }
    }

    /** Confirma todas as inscrições Pendentes de um torneio de uma vez. */
    suspend fun confirmarTodasPendentes(torneioId: String): Result<Int> {
        val tId = torneioId.toIntOrNull() ?: return Result.failure(Exception("ID inválido."))
        return try {
            val pendentes = inscricaoEquipaDao.getByTorneioSuspend(torneioId)
                .filter { it.estado == "Pendente" }
            if (pendentes.isEmpty()) return Result.success(0)

            client.from("inscricao_equipa")
                .update({ set("estado", "Confirmada") }) {
                    filter { eq("torneio_id", tId); eq("estado", "Pendente") }
                }
            pendentes.forEach { inscricaoEquipaDao.updateEstado(torneioId, it.equipaId, "Confirmada") }
            Result.success(pendentes.size)
        } catch (e: Exception) {
            Log.e(TAG, "confirmarTodasPendentes falhou: ${e.message}", e)
            Result.failure(Exception(buildErrorMessage(e)))
        }
    }

    // ── Gestão de membros ─────────────────────────────────────────────────────

    /** Adiciona um utilizador à equipa (Supabase + Room). */
    suspend fun adicionarMembro(equipaId: String, utilizadorId: String): Result<Unit> {
        val equipaIdInt = equipaId.toIntOrNull()
            ?: return Result.failure(Exception("ID da equipa inválido."))
        return adicionarMembroInterno(equipaId, utilizadorId, equipaIdInt)
    }

    private suspend fun adicionarMembroInterno(
        equipaId: String,
        utilizadorId: String,
        equipaIdInt: Int = equipaId.toInt()
    ): Result<Unit> {
        return try {
            val dto = MembroEquipaInsertDto(equipaId = equipaIdInt, utilizadorId = utilizadorId)
            val created = client.from("membro_equipa")
                .insert(dto) { select() }
                .decodeSingle<MembroEquipaDto>()
            membroEquipaDao.insert(created.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "adicionarMembro falhou: ${e.message}")
            Result.failure(Exception(buildErrorMessage(e)))
        }
    }

    /** Remove um utilizador da equipa (Supabase + Room). */
    suspend fun removerMembro(equipaId: String, utilizadorId: String): Result<Unit> {
        val equipaIdInt = equipaId.toIntOrNull()
            ?: return Result.failure(Exception("ID da equipa inválido."))
        return try {
            client.from("membro_equipa")
                .delete { filter {
                    eq("equipa_id", equipaIdInt)
                    eq("utilizador_id", utilizadorId)
                } }
            membroEquipaDao.deleteByEquipaAndUtilizador(equipaId, utilizadorId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "removerMembro falhou: ${e.message}")
            Result.failure(Exception(buildErrorMessage(e)))
        }
    }

    // ── Pesquisa de utilizadores ──────────────────────────────────────────────

    /**
     * Pesquisa utilizadores pelo nome no Supabase (case-insensitive).
     * Guarda os resultados em Room para uso no JOIN de membros.
     */
    suspend fun pesquisarUtilizadores(query: String): Result<List<UtilizadorEntity>> {
        if (query.length < 2) return Result.success(emptyList())
        return try {
            val dtos = client.from("utilizador")
                .select { filter { ilike("nome", "%$query%") } }
                .decodeList<UtilizadorDto>()
                .map { it.toEntity() }
            utilizadorDao.insertAll(dtos)
            Result.success(dtos)
        } catch (e: Exception) {
            Log.w(TAG, "pesquisarUtilizadores falhou: ${e.message}")
            Result.failure(Exception("Não foi possível pesquisar utilizadores."))
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun buildErrorMessage(e: Exception): String {
        val raw = e.message ?: "erro desconhecido"
        return when {
            // Mensagens do trigger DB (passam directamente para a UI)
            raw.contains("já está inscrito neste torneio", ignoreCase = true) ||
            raw.contains("já participa neste torneio",     ignoreCase = true) -> raw
            // Erros de permissão
            raw.contains("row-level security", ignoreCase = true) ||
            raw.contains("permission denied",  ignoreCase = true) ->
                "Sem permissão para esta operação."
            raw.contains("foreign key", ignoreCase = true) ->
                "Referência inválida (torneio ou equipa não existe)."
            raw.contains("Unable to resolve host", ignoreCase = true) ||
            raw.contains("timeout",                ignoreCase = true) ->
                "Sem ligação à internet."
            raw.contains("duplicate", ignoreCase = true) ||
            raw.contains("unique",    ignoreCase = true) ->
                "Este utilizador já é membro desta equipa."
            else -> raw  // Devolve a mensagem original sem "Erro: " prefix desnecessário
        }
    }
}
