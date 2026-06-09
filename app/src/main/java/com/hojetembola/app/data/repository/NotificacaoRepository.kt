package com.hojetembola.app.data.repository

import android.util.Log
import com.hojetembola.app.data.local.dao.NotificacaoDao
import com.hojetembola.app.data.local.entity.NotificacaoEntity
import com.hojetembola.app.data.remote.dto.NotificacaoDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "HTB-NotificacaoRepo"

@Singleton
class NotificacaoRepository @Inject constructor(
    private val client: SupabaseClient,
    private val notificacaoDao: NotificacaoDao
) {

    // ── Leitura local ─────────────────────────────────────────────────────────

    /** Todas as notificações do utilizador (mais recentes primeiro). */
    fun getNotificacoes(utilizadorId: String): Flow<List<NotificacaoEntity>> =
        notificacaoDao.getAll(utilizadorId)

    /** Só as notificações não lidas. */
    fun getNaoLidas(utilizadorId: String): Flow<List<NotificacaoEntity>> =
        notificacaoDao.getNaoLidas(utilizadorId)

    /** Contagem de notificações não lidas — para badge no sino. */
    fun countNaoLidas(utilizadorId: String): Flow<Int> =
        notificacaoDao.countNaoLidas(utilizadorId)

    // ── Sincronização ─────────────────────────────────────────────────────────

    /** Pull das notificações do utilizador do Supabase. */
    suspend fun syncNotificacoes(utilizadorId: String) {
        try {
            val notificacoes = client.from("notificacao")
                .select { filter { eq("utilizador_id", utilizadorId) } }
                .decodeList<NotificacaoDto>()
                .map { it.toEntity() }
            if (notificacoes.isNotEmpty()) notificacaoDao.insertAll(notificacoes)
        } catch (e: Exception) {
            Log.w(TAG, "syncNotificacoes falhou: ${e.message}")
        }
    }

    // ── Marcação como lida ────────────────────────────────────────────────────

    suspend fun marcarComoLida(notificacaoId: String) {
        try {
            client.from("notificacao")
                .update({ set("lida", true) }) {
                    filter { eq("id", notificacaoId) }
                }
            notificacaoDao.marcarComoLida(notificacaoId)
        } catch (e: Exception) {
            // Falha silenciosa — marca localmente mesmo sem rede
            notificacaoDao.marcarComoLida(notificacaoId)
            Log.w(TAG, "marcarComoLida falhou remotamente: ${e.message}")
        }
    }

    suspend fun marcarTodasComoLidas(utilizadorId: String) {
        try {
            client.from("notificacao")
                .update({ set("lida", true) }) {
                    filter {
                        eq("utilizador_id", utilizadorId)
                        eq("lida", false)
                    }
                }
            notificacaoDao.marcarTodasComoLidas(utilizadorId)
        } catch (e: Exception) {
            notificacaoDao.marcarTodasComoLidas(utilizadorId)
            Log.w(TAG, "marcarTodasComoLidas falhou remotamente: ${e.message}")
        }
    }
}
