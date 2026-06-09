package com.hojetembola.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hojetembola.app.data.local.dao.*
import com.hojetembola.app.data.local.entity.*

/**
 * Base de dados Room local — cache offline de todas as entidades.
 *
 * Versão 1 — sem exportSchema para projecto académico.
 * Em produção, exportSchema = true + migration strategy.
 *
 * Instanciada como singleton via Hilt em [com.hojetembola.app.di.AppModule].
 */
@Database(
    entities = [
        UtilizadorEntity::class,
        EquipaEntity::class,
        MembroEquipaEntity::class,
        ConviteEntity::class,
        TorneioEntity::class,
        InscricaoEquipaEntity::class,
        JogadorInscricaoEntity::class,
        JornadaEntity::class,
        JogoEntity::class,
        ConvocatoriaJogoEntity::class,
        EventoJogoEntity::class,
        SuspensaoEntity::class,
        ClassificacaoEntity::class,
        VotoMvpEntity::class,
        NotificacaoEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // ── DAOs ──────────────────────────────────────────────────────────────────
    abstract fun utilizadorDao(): UtilizadorDao
    abstract fun equipaDao(): EquipaDao
    abstract fun membroEquipaDao(): MembroEquipaDao
    abstract fun conviteDao(): ConviteDao
    abstract fun torneioDao(): TorneioDao
    abstract fun inscricaoEquipaDao(): InscricaoEquipaDao
    abstract fun jogadorInscricaoDao(): JogadorInscricaoDao
    abstract fun jornadaDao(): JornadaDao
    abstract fun jogoDao(): JogoDao
    abstract fun convocatoriaJogoDao(): ConvocatoriaJogoDao
    abstract fun eventoJogoDao(): EventoJogoDao
    abstract fun suspensaoDao(): SuspensaoDao
    abstract fun classificacaoDao(): ClassificacaoDao
    abstract fun votoMvpDao(): VotoMvpDao
    abstract fun notificacaoDao(): NotificacaoDao

    companion object {
        const val DATABASE_NAME = "hojetembola.db"
    }
}
