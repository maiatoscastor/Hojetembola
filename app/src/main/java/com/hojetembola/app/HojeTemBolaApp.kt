package com.hojetembola.app

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
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
    }
}
