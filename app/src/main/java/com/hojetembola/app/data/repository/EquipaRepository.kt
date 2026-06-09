package com.hojetembola.app.data.repository

import android.util.Log
import com.hojetembola.app.data.local.dao.EquipaDao
import com.hojetembola.app.data.local.dao.InscricaoEquipaDao
import com.hojetembola.app.data.local.dao.MembroEquipaDao
import com.hojetembola.app.data.local.dao.UtilizadorDao
import com.hojetembola.app.data.local.entity.EquipaEntity
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
    private val utilizadorDao: UtilizadorDao
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

    /** Verifica localmente se esta equipa já está inscrita neste torneio. */
    suspend fun jaInscrita(torneioId: String, equipaId: String): Boolean =
        inscricaoEquipaDao.countInscricaoAtiva(torneioId, equipaId) > 0

    // ── Leitura local — membros ───────────────────────────────────────────────

    /** Flow de membros com nomes (requer JOIN com utilizador em cache). */
    fun getMembrosComNome(equipaId: String): Flow<List<MembroComNome>> =
        membroEquipaDao.getMembrosComNome(equipaId)

    /** Contagem de membros activos de uma equipa. */
    suspend fun contarMembros(equipaId: String): Int =
        membroEquipaDao.countMembros(equipaId)

    // ── Sincronização ─────────────────────────────────────────────────────────

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

    // ── Inscrever equipa ──────────────────────────────────────────────────────

    /**
     * Inscreve uma equipa num torneio.
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

        return try {
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
            raw.contains("row-level security", ignoreCase = true) ||
            raw.contains("permission denied", ignoreCase = true) ->
                "Sem permissão. Verifica as políticas RLS no Supabase."
            raw.contains("foreign key", ignoreCase = true) ->
                "Referência inválida (torneio ou equipa não existe)."
            raw.contains("Unable to resolve host", ignoreCase = true) ||
            raw.contains("timeout", ignoreCase = true) ->
                "Sem ligação à internet."
            raw.contains("duplicate", ignoreCase = true) ||
            raw.contains("unique", ignoreCase = true) ->
                "Este utilizador já é membro desta equipa."
            else -> "Erro: $raw"
        }
    }
}
