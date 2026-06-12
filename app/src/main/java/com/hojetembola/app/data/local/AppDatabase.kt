package com.hojetembola.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 10,
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

        /**
         * v8 → v9: adiciona is_titular à convocatoria_jogo (primeira tentativa com
         * fallbackToDestructiveMigration, DB pode já ter a coluna ou não).
         * Esta migration é no-op — a coluna já existe se a app v9 foi instalada,
         * ou não existe se ainda estava na v8.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // A coluna pode já existir se o fallback anterior já a criou via schema recreate.
                // Usamos uma abordagem segura: tenta adicionar, ignora erro de coluna duplicada.
                try {
                    db.execSQL(
                        "ALTER TABLE convocatoria_jogo ADD COLUMN is_titular INTEGER NOT NULL DEFAULT 0"
                    )
                } catch (_: Exception) { /* coluna já existe */ }
            }
        }

        /**
         * v9 → v10: migration de recuperação para utilizadores que ficaram com a
         * DB na v9 (coluna já existe, esta migration é no-op mas garante que o
         * Room não force um fallback destrutivo).
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No-op — esquema já está correcto desde a v9.
            }
        }
    }
}
