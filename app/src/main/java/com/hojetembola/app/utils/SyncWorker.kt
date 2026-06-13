package com.hojetembola.app.utils

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.hojetembola.app.data.local.dao.EventoJogoDao
import com.hojetembola.app.data.local.dao.JogoDao
import com.hojetembola.app.data.local.dao.TorneioDao
import com.hojetembola.app.data.remote.dto.EventoJogoDto
import com.hojetembola.app.data.remote.dto.EventoJogoInsertDto
import com.hojetembola.app.data.remote.dto.TorneioDto
import com.hojetembola.app.data.remote.dto.toInsertDto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val torneioDao: TorneioDao,
    private val jogoDao: JogoDao,
    private val eventoJogoDao: EventoJogoDao,
    private val client: SupabaseClient
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Sincronização iniciada")
            syncEventos()
            syncJogos()
            syncTorneios()
            Log.d(TAG, "Sincronização concluída com sucesso")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincronização: ${e.message}", e)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    // ── Eventos ───────────────────────────────────────────────────────────────

    private suspend fun syncEventos() {
        val pendentes = eventoJogoDao.getPendentesSync()
        if (pendentes.isEmpty()) return
        Log.d(TAG, "Sync eventos: ${pendentes.size} pendentes")

        for (evento in pendentes) {
            // Só conseguimos sincronizar se o jogoId já é um inteiro do Supabase
            val jogoIdInt = evento.jogoId.toIntOrNull() ?: continue

            try {
                val tipoDb = evento.tipo.replaceFirstChar { it.uppercase() }
                val dto = EventoJogoInsertDto(
                    jogoId         = jogoIdInt,
                    tipo           = tipoDb,
                    minuto         = evento.minuto,
                    equipaId       = evento.equipaId?.toIntOrNull(),
                    jogadorId      = evento.jogadorId,
                    jogadorSaiId   = evento.jogadorSaiId,
                    jogadorEntraId = evento.jogadorEntraId
                )
                val created = client.from("evento_jogo")
                    .insert(dto) { select() }
                    .decodeSingle<EventoJogoDto>()

                // Substituir UUID local pelo ID real do Supabase
                eventoJogoDao.deleteById(evento.id)
                eventoJogoDao.insert(created.toEntity().copy(assistenciaId = evento.assistenciaId))

                // Assistência (best-effort)
                if (evento.assistenciaId != null) {
                    try {
                        client.from("evento_jogo")
                            .update({ set("assistencia_id", evento.assistenciaId) }) {
                                filter { eq("id", created.id) }
                            }
                    } catch (e: Exception) {
                        Log.w(TAG, "assistencia_id sync falhou: ${e.message}")
                    }
                }

                Log.d(TAG, "Evento ${evento.id} → ${created.id} sincronizado")
            } catch (e: Exception) {
                Log.w(TAG, "Evento ${evento.id} falhou, tentará depois: ${e.message}")
            }
        }
    }

    // ── Jogos ─────────────────────────────────────────────────────────────────

    private suspend fun syncJogos() {
        val pendentes = jogoDao.getPendentesSync()
        if (pendentes.isEmpty()) return
        Log.d(TAG, "Sync jogos: ${pendentes.size} pendentes")

        for (jogo in pendentes) {
            val jogoIdInt = jogo.id.toIntOrNull() ?: continue

            try {
                // Empurrar o estado e resultado atuais para o Supabase
                val estadoDb = when (jogo.estado) {
                    "ao_vivo"  -> "EmCurso"
                    "terminado"-> "Terminado"
                    else       -> jogo.estado.replaceFirstChar { it.uppercase() }
                }
                client.from("jogo").update({
                    set("estado", estadoDb)
                    if (jogo.golosCasa != null)      set("golos_casa", jogo.golosCasa)
                    if (jogo.golosVisitante != null)  set("golos_fora", jogo.golosVisitante)
                    if (jogo.minutoAtual != null)     set("minuto_atual", jogo.minutoAtual)
                }) { filter { eq("id", jogoIdInt) } }

                jogoDao.markAsSynced(jogo.id)
                Log.d(TAG, "Jogo ${jogo.id} sincronizado (estado=${estadoDb})")
            } catch (e: Exception) {
                Log.w(TAG, "Jogo ${jogo.id} falhou, tentará depois: ${e.message}")
            }
        }
    }

    // ── Torneios ──────────────────────────────────────────────────────────────

    private suspend fun syncTorneios() {
        val pendentes = torneioDao.getPendentesSync()
        if (pendentes.isEmpty()) return
        Log.d(TAG, "Sync torneios: ${pendentes.size} pendentes")

        for (torneio in pendentes) {
            // Torneios com ID inteiro já foram criados no Supabase; apenas marcamos como synced
            if (torneio.id.toIntOrNull() != null) {
                torneioDao.markAsSynced(torneio.id)
                continue
            }

            // Torneio com UUID — foi criado offline, tentar inserir agora
            try {
                val created = client.from("torneio")
                    .insert(torneio.toInsertDto()) { select() }
                    .decodeSingle<TorneioDto>()

                val realEntity = created.toEntity()
                torneioDao.deleteById(torneio.id)
                torneioDao.insert(realEntity)
                Log.d(TAG, "Torneio ${torneio.id} → ${realEntity.id} sincronizado")
            } catch (e: Exception) {
                Log.w(TAG, "Torneio ${torneio.id} falhou, tentará depois: ${e.message}")
            }
        }
    }

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "HTB-SyncWorker"
        private const val MAX_RETRIES = 3
        const val WORK_NAME = "HojeTemBola_Sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun triggerImmediate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
