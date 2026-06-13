package com.hojetembola.app

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.hojetembola.app.utils.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Classe Application principal anotada com @HiltAndroidApp.
 *
 * Também implementa [Configuration.Provider] para integrar o WorkManager
 * com Hilt (HiltWorkerFactory). Desta forma, o WorkManager utiliza
 * o factory correto para criar workers injetáveis.
 */
@HiltAndroidApp
class HojeTemBolaApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

    companion object {
        /** Referência estática à Application, útil em utilitários sem DI. */
        lateinit var instance: HojeTemBolaApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        SyncWorker.schedule(this)
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                SyncWorker.triggerImmediate(this@HojeTemBolaApp)
            }
        })
    }
}
