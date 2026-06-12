package com.hojetembola.app.data.local.dao

import androidx.room.*
import com.hojetembola.app.data.local.entity.EventoJogoEntity
import kotlinx.coroutines.flow.Flow

data class EventoComNome(
    @ColumnInfo(name = "evento_id") val eventoId: String,
    @ColumnInfo(name = "jogo_id") val jogoId: String,
    val tipo: String,
    val minuto: Int,
    @ColumnInfo(name = "equipa_id") val equipaId: String?,
    @ColumnInfo(name = "jogador_nome") val jogadorNome: String?,
    @ColumnInfo(name = "jogador_sai_nome") val jogadorSaiNome: String?,
    @ColumnInfo(name = "jogador_entra_nome") val jogadorEntraNome: String?,
    /** IDs needed to track who is on the field after substitutions. */
    @ColumnInfo(name = "jogador_sai_id") val jogadorSaiId: String?,
    @ColumnInfo(name = "jogador_entra_id") val jogadorEntraId: String?
)

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

    /** Total de golos marcados por um jogador em todos os torneios (RF28) */
    @Query("SELECT COUNT(*) FROM evento_jogo WHERE jogador_id = :jogadorId AND tipo = 'golo'")
    suspend fun countTotalGolosByJogador(jogadorId: String): Int

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

    @Query("""
        SELECT e.id AS evento_id, e.jogo_id, e.tipo, e.minuto, e.equipa_id,
               u1.nome AS jogador_nome,
               u2.nome AS jogador_sai_nome,
               u3.nome AS jogador_entra_nome,
               e.jogador_sai_id,
               e.jogador_entra_id
        FROM evento_jogo e
        LEFT JOIN utilizador u1 ON u1.id = e.jogador_id
        LEFT JOIN utilizador u2 ON u2.id = e.jogador_sai_id
        LEFT JOIN utilizador u3 ON u3.id = e.jogador_entra_id
        WHERE e.jogo_id = :jogoId
        ORDER BY e.minuto ASC
    """)
    fun getEventosComNome(jogoId: String): Flow<List<EventoComNome>>

    @Query("DELETE FROM evento_jogo WHERE jogo_id = :jogoId")
    suspend fun deleteByJogo(jogoId: String)
}
