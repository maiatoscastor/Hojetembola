package com.hojetembola.app.utils

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.hojetembola.app.data.local.dao.EventoJogoDao
import com.hojetembola.app.data.local.dao.JogoDao
import com.hojetembola.app.data.local.dao.TorneioDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Worker responsável pela sincronização offline → Supabase.
 *
 * Quando o organizador regista eventos sem internet (RF25), os registos
 * ficam com [isSynced] = false no Room. Este worker deteta-os e envia
 * para o Supabase assim que houver conetividade.
 *
 * Agendado com [Constraints] que exige rede ativa, por isso só corre
 * quando o dispositivo está online.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val torneioDao: TorneioDao,
    private val jogoDao: JogoDao,
    private val eventoJogoDao: EventoJogoDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Sincronização iniciada")

            syncTorneios()
            syncJogos()
            syncEventos()

            Log.d(TAG, "Sincronização concluída com sucesso")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincronização: ${e.message}", e)
            // Retry automático (máx 3 tentativas definidas no agendamento)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    // ── Sync individual ───────────────────────────────────────────────────────

    private suspend fun syncTorneios() {
        val pendentes = torneioDao.getPendentesSync()
        if (pendentes.isEmpty()) return
        Log.d(TAG, "Sync torneios: ${pendentes.size} pendentes")

        // TODO Passo 2: chamar TorneioRepository.upsertRemote(pendentes)
        // Por agora, marcamos como sincronizados (stub)
        pendentes.forEach { torneioDao.markAsSynced(it.id) }
    }

    private suspend fun syncJogos() {
        val pendentes = jogoDao.getPendentesSync()
        if (pendentes.isEmpty()) return
        Log.d(TAG, "Sync jogos: ${pendentes.size} pendentes")

        // TODO Passo 2: chamar JogoRepository.upsertRemote(pendentes)
        pendentes.forEach { jogoDao.markAsSynced(it.id) }
    }

    private suspend fun syncEventos() {
        val pendentes = eventoJogoDao.getPendentesSync()
        if (pendentes.isEmpty()) return
        Log.d(TAG, "Sync eventos: ${pendentes.size} pendentes")

        // TODO Passo 2: chamar EventoJogoRepository.upsertRemote(pendentes)
        pendentes.forEach { eventoJogoDao.markAsSynced(it.id) }
    }

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "SyncWorker"
        private const val MAX_RETRIES = 3
        const val WORK_NAME = "HojeTemBola_Sync"

        /**
         * Agenda o worker para correr periodicamente (15 min mínimo do WorkManager),
         * apenas com rede disponível — chamado em [com.hojetembola.app.HojeTemBolaApp].
         */
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

        /** Força uma sincronização imediata (ex: ao voltar a ter rede). */
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
