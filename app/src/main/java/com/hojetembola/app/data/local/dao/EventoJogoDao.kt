package com.hojetembola.app.data.local.dao

import androidx.room.*
import com.hojetembola.app.data.local.entity.EventoJogoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventoJogoDao {

    @Query("SELECT * FROM evento_jogo WHERE jogo_id = :jogoId ORDER BY minuto ASC")
    fun getByJogo(jogoId: String): Flow<List<EventoJogoEntity>>

    @Query("SELECT * FROM evento_jogo WHERE jogo_id = :jogoId AND tipo = :tipo ORDER BY minuto ASC")
    fun getByJogoETipo(jogoId: String, tipo: String): Flow<List<EventoJogoEntity>>

    /** Total de golos marcados por um jogador num torneio (RF28) */
    @Query("""
        SELECT COUNT(*) FROM evento_jogo ej
        INNER JOIN jogo j ON j.id = ej.jogo_id
        WHERE ej.jogador_id = :jogadorId AND ej.tipo = 'golo'
        AND j.torneio_id = :torneioId
    """)
    suspend fun countGolosByJogadorTorneio(jogadorId: String, torneioId: String): Int

    /** Amarelos acumulados num torneio para detetar suspensões (RN04) */
    @Query("""
        SELECT COUNT(*) FROM evento_jogo ej
        INNER JOIN jogo j ON j.id = ej.jogo_id
        WHERE ej.jogador_id = :jogadorId AND ej.tipo = 'amarelo'
        AND j.torneio_id = :torneioId
    """)
    suspend fun countAmarelosByJogadorTorneio(jogadorId: String, torneioId: String): Int

    @Query("SELECT * FROM evento_jogo WHERE is_synced = 0")
    suspend fun getPendentesSync(): List<EventoJogoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(evento: EventoJogoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(eventos: List<EventoJogoEntity>)

    @Delete
    suspend fun delete(evento: EventoJogoEntity)

    @Query("UPDATE evento_jogo SET is_synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
