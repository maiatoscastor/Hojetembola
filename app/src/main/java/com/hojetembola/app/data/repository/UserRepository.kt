package com.hojetembola.app.data.repository

import com.hojetembola.app.data.local.dao.UtilizadorDao
import com.hojetembola.app.data.local.entity.UtilizadorEntity
import com.hojetembola.app.data.remote.dto.UtilizadorDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório do utilizador autenticado.
 *
 * Estratégia cache-first com fallback à rede:
 *   1. Tenta buscar do Room (cache offline)
 *   2. Refaz sempre o fetch do Supabase para manter dados frescos
 *   3. Guarda em Room para disponibilidade offline
 *
 * Injetado via Hilt em [com.hojetembola.app.ui.perfil.PerfilViewModel].
 */
@Singleton
class UserRepository @Inject constructor(
    private val client: SupabaseClient,
    private val utilizadorDao: UtilizadorDao
) {

    /**
     * Devolve o perfil do utilizador autenticado.
     *
     * @return [Result.success] com [UtilizadorEntity] ou [Result.failure] com mensagem PT.
     */
    suspend fun getCurrentUser(): Result<UtilizadorEntity> {
        val uid = client.auth.currentSessionOrNull()?.user?.id
            ?: return Result.failure(Exception("Sessão expirada. Faz login novamente."))

        return try {
            // Fetch remoto → actualiza cache
            val dto = client.from("utilizador")
                .select { filter { eq("id", uid) } }
                .decodeSingle<UtilizadorDto>()

            val entity = dto.toEntity()
            utilizadorDao.insert(entity)
            Result.success(entity)
        } catch (e: Exception) {
            // Sem rede → usa cache
            val cached = utilizadorDao.getById(uid)
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(Exception("Não foi possível carregar o perfil. Verifica a ligação."))
            }
        }
    }

    /**
     * Termina a sessão no Supabase.
     * O Room é preservado para não perder dados offline.
     */
    suspend fun signOut() {
        try {
            client.auth.signOut()
        } catch (_: Exception) {
            // Ignora erros de rede ao fazer sign out
        }
    }
}
