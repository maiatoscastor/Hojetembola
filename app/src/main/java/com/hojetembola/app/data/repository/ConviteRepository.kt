package com.hojetembola.app.data.repository

import android.util.Log
import com.hojetembola.app.data.local.dao.ConviteDao
import com.hojetembola.app.data.local.dao.MembroEquipaDao
import com.hojetembola.app.data.local.dao.UtilizadorDao
import com.hojetembola.app.data.local.entity.ConviteEntity
import com.hojetembola.app.data.remote.dto.ConviteDto
import com.hojetembola.app.data.remote.dto.ConviteInsertDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "HTB-ConviteRepo"

@Singleton
class ConviteRepository @Inject constructor(
    private val client: SupabaseClient,
    private val conviteDao: ConviteDao,
    private val membroEquipaDao: MembroEquipaDao,
    private val utilizadorDao: UtilizadorDao
) {

    // ── Leitura local ─────────────────────────────────────────────────────────

    /** Convites pendentes enviados pela equipa do capitão. */
    fun getConvitesPendentes(equipaId: String): Flow<List<ConviteEntity>> =
        conviteDao.getConvitesPendentes(equipaId)

    /** Convites pendentes recebidos por um utilizador. */
    fun getConvitesPendentesPorConvidado(convidadoId: String): Flow<List<ConviteEntity>> =
        conviteDao.getConvitesPendentesPorConvidado(convidadoId)

    // ── Sincronização ─────────────────────────────────────────────────────────

    /** Pull dos convites enviados por uma equipa do Supabase. */
    suspend fun syncConvitesEnviados(equipaId: String) {
        val equipaIdInt = equipaId.toIntOrNull() ?: return
        try {
            val convites = client.from("convite")
                .select { filter { eq("equipa_id", equipaIdInt) } }
                .decodeList<ConviteDto>()
                .map { it.toEntity() }
            if (convites.isNotEmpty()) conviteDao.insertAll(convites)
        } catch (e: Exception) {
            Log.w(TAG, "syncConvitesEnviados falhou: ${e.message}")
        }
    }

    /** Pull dos convites recebidos por um utilizador do Supabase. */
    suspend fun syncConvitesRecebidos(convidadoId: String) {
        try {
            val convites = client.from("convite")
                .select { filter { eq("convidado_id", convidadoId) } }
                .decodeList<ConviteDto>()
                .map { it.toEntity() }
            if (convites.isNotEmpty()) conviteDao.insertAll(convites)
        } catch (e: Exception) {
            Log.w(TAG, "syncConvitesRecebidos falhou: ${e.message}")
        }
    }

    // ── Enviar convite ────────────────────────────────────────────────────────

    /**
     * Envia um convite ao utilizador para integrar a equipa.
     * Verifica se o jogador já é membro antes de enviar.
     */
    suspend fun enviarConvite(
        equipaId: String,
        equipaNome: String,
        convidadoId: String,
        criadoPorId: String
    ): Result<ConviteEntity> {
        val equipaIdInt = equipaId.toIntOrNull()
            ?: return Result.failure(Exception("ID da equipa inválido."))

        // Verificar se já é membro
        if (membroEquipaDao.countMembroAtivo(equipaId, convidadoId) > 0) {
            return Result.failure(Exception("Este jogador já faz parte da equipa."))
        }

        // Sincroniza antes de verificar — garante que temos o estado real do Supabase
        syncConvitesEnviados(equipaId)

        // Verificar se já tem convite pendente (dados frescos após sync)
        val pendentes = conviteDao.getConvitesPendentesSuspend(equipaId)
        if (pendentes.any { it.convidadoId == convidadoId }) {
            return Result.failure(Exception("Já existe um convite pendente para este jogador."))
        }

        // Se existir convite anterior recusado/expirado, apaga-o para libertar a constraint única
        val conviteAnterior = conviteDao.getConviteAnteriorNaoPendente(equipaId, convidadoId)
        if (conviteAnterior != null) {
            try {
                client.from("convite").delete { filter { eq("id", conviteAnterior.id) } }
                conviteDao.deleteById(conviteAnterior.id)
                Log.i(TAG, "Convite anterior (${conviteAnterior.estado}) removido para re-convite.")
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao remover convite anterior: ${e.message}")
                // Continua mesmo assim — o Supabase pode ter uma policy que permite o insert
            }
        }

        return try {
            val utilizador = utilizadorDao.getById(convidadoId)
                ?: return Result.failure(Exception("Utilizador não encontrado."))

            val dto = ConviteInsertDto(
                equipaId       = equipaIdInt,
                convidadoId    = convidadoId,
                convidadoEmail = utilizador.email,
                nomeEquipa     = equipaNome,
                criadoPorId    = criadoPorId
            )
            val created = client.from("convite")
                .insert(dto) { select() }
                .decodeSingle<ConviteDto>()

            val entity = created.toEntity()
            conviteDao.insert(entity)
            Log.i(TAG, "Convite ${entity.id} enviado para ${utilizador.nome}.")
            Result.success(entity)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao enviar convite: ${e.message}", e)
            Result.failure(Exception(buildErrorMessage(e)))
        }
    }

    // ── Aceitar / Recusar convite ─────────────────────────────────────────────

    /**
     * Aceita um convite: actualiza o estado no Supabase e insere o utilizador
     * como membro da equipa. O servidor deverá ter um trigger que faz o mesmo.
     */
    suspend fun aceitarConvite(conviteId: String): Result<Unit> {
        Log.d(TAG, "aceitarConvite: a tentar aceitar conviteId=$conviteId")
        return try {
            // 1. Log do convite local antes de tentar
            val conviteLocal = conviteDao.getById(conviteId)
            Log.d(TAG, "aceitarConvite: convite local=$conviteLocal")

            // 2. Chamada ao Supabase
            Log.d(TAG, "aceitarConvite: a chamar Supabase UPDATE...")
            client.from("convite")
                .update({ set("estado", "aceite") }) {
                    filter { eq("id", conviteId) }
                }
            Log.d(TAG, "aceitarConvite: UPDATE concluído com sucesso")

            // 3. Actualiza Room
            val convite = conviteLocal ?: conviteDao.getById(conviteId)
            if (convite != null) {
                conviteDao.update(convite.copy(estado = "aceite"))
                Log.d(TAG, "aceitarConvite: Room actualizado")
            } else {
                Log.w(TAG, "aceitarConvite: convite não encontrado no Room após update")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "aceitarConvite FALHOU — tipo: ${e::class.simpleName}", e)
            Log.e(TAG, "aceitarConvite FALHOU — message: ${e.message}")
            Log.e(TAG, "aceitarConvite FALHOU — cause: ${e.cause}")
            val msg = when {
                e.message?.contains("já participa", ignoreCase = true) == true -> e.message!!
                e.message?.contains("row-level security", ignoreCase = true) == true ->
                    "Sem permissão para aceitar este convite."
                else -> "Erro: ${e.message ?: "desconhecido"}"
            }
            Result.failure(Exception(msg))
        }
    }

    /**
     * Recusa um convite: actualiza o estado no Supabase.
     */
    suspend fun recusarConvite(conviteId: String): Result<Unit> {
        return try {
            client.from("convite")
                .update({ set("estado", "recusado") }) {
                    filter { eq("id", conviteId) }
                }
            val convite = conviteDao.getById(conviteId) ?: return Result.success(Unit)
            conviteDao.update(convite.copy(estado = "recusado"))
            Log.i(TAG, "Convite $conviteId recusado.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao recusar convite: ${e.message}", e)
            Result.failure(Exception("Não foi possível recusar o convite."))
        }
    }

    /**
     * Revogar um convite pendente (apenas pelo capitão antes do prazo).
     */
    suspend fun revogarConvite(conviteId: String): Result<Unit> {
        return try {
            client.from("convite")
                .delete { filter { eq("id", conviteId) } }
            conviteDao.deleteById(conviteId)
            Log.i(TAG, "Convite $conviteId revogado.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao revogar convite: ${e.message}", e)
            Result.failure(Exception("Não foi possível revogar o convite."))
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun buildErrorMessage(e: Exception): String {
        val raw = e.message ?: "erro desconhecido"
        return when {
            raw.contains("row-level security", ignoreCase = true) ||
            raw.contains("permission denied",  ignoreCase = true) ->
                "Sem permissão para convidar jogadores."
            raw.contains("Unable to resolve host", ignoreCase = true) ||
            raw.contains("timeout",                ignoreCase = true) ->
                "Sem ligação à internet."
            raw.contains("duplicate", ignoreCase = true) ||
            raw.contains("unique",    ignoreCase = true) ->
                "Já existe um convite pendente para este jogador."
            else -> raw
        }
    }
}
