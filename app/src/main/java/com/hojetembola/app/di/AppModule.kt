package com.hojetembola.app.di

import android.content.Context
import androidx.room.Room
import com.hojetembola.app.data.local.AppDatabase
import com.hojetembola.app.data.local.dao.*
import com.hojetembola.app.data.remote.SupabaseClient
import com.hojetembola.app.utils.NetworkUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient as SupabaseClientType
import javax.inject.Singleton

/**
 * Módulo Hilt principal — [SingletonComponent] garante instâncias únicas
 * em toda a vida da aplicação.
 *
 * Passo 2 irá acrescentar os Repositories e UseCases.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ── Supabase ──────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClientType =
        SupabaseClient.client

    // ── Room ──────────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10)
            .fallbackToDestructiveMigration()
            .build()

    // ── DAOs ──────────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideUtilizadorDao(db: AppDatabase): UtilizadorDao = db.utilizadorDao()

    @Provides
    @Singleton
    fun provideEquipaDao(db: AppDatabase): EquipaDao = db.equipaDao()

    @Provides
    @Singleton
    fun provideMembroEquipaDao(db: AppDatabase): MembroEquipaDao = db.membroEquipaDao()

    @Provides
    @Singleton
    fun provideConviteDao(db: AppDatabase): ConviteDao = db.conviteDao()

    @Provides
    @Singleton
    fun provideTorneioDao(db: AppDatabase): TorneioDao = db.torneioDao()

    @Provides
    @Singleton
    fun provideInscricaoEquipaDao(db: AppDatabase): InscricaoEquipaDao = db.inscricaoEquipaDao()

    @Provides
    @Singleton
    fun provideJogadorInscricaoDao(db: AppDatabase): JogadorInscricaoDao = db.jogadorInscricaoDao()

    @Provides
    @Singleton
    fun provideJornadaDao(db: AppDatabase): JornadaDao = db.jornadaDao()

    @Provides
    @Singleton
    fun provideJogoDao(db: AppDatabase): JogoDao = db.jogoDao()

    @Provides
    @Singleton
    fun provideConvocatoriaJogoDao(db: AppDatabase): ConvocatoriaJogoDao = db.convocatoriaJogoDao()

    @Provides
    @Singleton
    fun provideEventoJogoDao(db: AppDatabase): EventoJogoDao = db.eventoJogoDao()

    @Provides
    @Singleton
    fun provideSuspensaoDao(db: AppDatabase): SuspensaoDao = db.suspensaoDao()

    @Provides
    @Singleton
    fun provideClassificacaoDao(db: AppDatabase): ClassificacaoDao = db.classificacaoDao()

    @Provides
    @Singleton
    fun provideVotoMvpDao(db: AppDatabase): VotoMvpDao = db.votoMvpDao()

    @Provides
    @Singleton
    fun provideNotificacaoDao(db: AppDatabase): NotificacaoDao = db.notificacaoDao()

    // ── Repositories ─────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideEquipaRepository(
        client: io.github.jan.supabase.SupabaseClient,
        equipaDao: EquipaDao,
        conviteDao: ConviteDao,
        inscricaoEquipaDao: InscricaoEquipaDao,
        membroEquipaDao: MembroEquipaDao,
        utilizadorDao: UtilizadorDao,
        torneioDao: TorneioDao,
        jogadorInscricaoDao: JogadorInscricaoDao
    ): com.hojetembola.app.data.repository.EquipaRepository =
        com.hojetembola.app.data.repository.EquipaRepository(
            client, equipaDao, conviteDao, inscricaoEquipaDao, membroEquipaDao, utilizadorDao, torneioDao, jogadorInscricaoDao
        )

    @Provides
    @Singleton
    fun provideConviteRepository(
        client: io.github.jan.supabase.SupabaseClient,
        conviteDao: ConviteDao,
        membroEquipaDao: MembroEquipaDao,
        utilizadorDao: UtilizadorDao
    ): com.hojetembola.app.data.repository.ConviteRepository =
        com.hojetembola.app.data.repository.ConviteRepository(
            client, conviteDao, membroEquipaDao, utilizadorDao
        )

    @Provides
    @Singleton
    fun provideNotificacaoRepository(
        client: io.github.jan.supabase.SupabaseClient,
        notificacaoDao: NotificacaoDao
    ): com.hojetembola.app.data.repository.NotificacaoRepository =
        com.hojetembola.app.data.repository.NotificacaoRepository(
            client, notificacaoDao
        )

    @Provides
    @Singleton
    fun provideJogoRepository(
        client: io.github.jan.supabase.SupabaseClient,
        jogoDao: JogoDao,
        jornadaDao: JornadaDao,
        torneioDao: TorneioDao,
        inscricaoEquipaDao: InscricaoEquipaDao,
        eventoJogoDao: EventoJogoDao,
        classificacaoDao: ClassificacaoDao,
        equipaDao: EquipaDao,
        convocatoriaJogoDao: ConvocatoriaJogoDao,
        jogadorInscricaoDao: JogadorInscricaoDao
    ): com.hojetembola.app.data.repository.JogoRepository =
        com.hojetembola.app.data.repository.JogoRepository(
            client, jogoDao, jornadaDao, torneioDao, inscricaoEquipaDao,
            eventoJogoDao, classificacaoDao, equipaDao,
            convocatoriaJogoDao, jogadorInscricaoDao
        )

    // ── Utils ─────────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideNetworkUtils(@ApplicationContext context: Context): NetworkUtils =
        NetworkUtils(context)
}
