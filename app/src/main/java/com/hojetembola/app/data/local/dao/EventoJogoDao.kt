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
    @ColumnInfo(name = "jogador_id") val jogadorId: String?,
    @ColumnInfo(name = "jogador_nome") val jogadorNome: String?,
    @ColumnInfo(name = "jogador_sai_nome") val jogadorSaiNome: String?,
    @ColumnInfo(name = "jogador_entra_nome") val jogadorEntraNome: String?,
    /** IDs needed to track who is on the field after substitutions. */
    @ColumnInfo(name = "jogador_sai_id") val jogadorSaiId: String?,
    @ColumnInfo(name = "jogador_entra_id") val jogadorEntraId: String?,
    /** Assist player name — only filled for golos with an assist registered. */
    @ColumnInfo(name = "assistencia_nome") val assistenciaNome: String?
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

    /** Total de assistências de um jogador num torneio */
    @Query("""
        SELECT COUNT(*) FROM evento_jogo ej
        INNER JOIN jogo j ON j.id = ej.jogo_id
        WHERE ej.assistencia_id = :jogadorId AND ej.tipo = 'golo'
        AND j.torneio_id = :torneioId
    """)
    suspend fun countAssistenciasByJogadorTorneio(jogadorId: String, torneioId: String): Int

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

    @Query("SELECT COUNT(*) FROM evento_jogo WHERE assistencia_id = :jogadorId AND tipo = 'golo'")
    suspend fun countTotalAssistenciasByJogador(jogadorId: String): Int

    @Query("SELECT COUNT(*) FROM evento_jogo WHERE jogador_id = :jogadorId AND tipo = 'amarelo'")
    suspend fun countTotalAmarelosByJogador(jogadorId: String): Int

    @Query("SELECT * FROM evento_jogo WHERE is_synced = 0")
    suspend fun getPendentesSync(): List<EventoJogoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(evento: EventoJogoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(eventos: List<EventoJogoEntity>)

    @Delete
    suspend fun delete(evento: EventoJogoEntity)

    @Query("DELETE FROM evento_jogo WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE evento_jogo SET is_synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    /** Amarelos de um jogador num jogo específico (para detetar 2º amarelo = vermelho). */
    @Query("SELECT COUNT(*) FROM evento_jogo WHERE jogador_id = :jogadorId AND jogo_id = :jogoId AND tipo = 'amarelo'")
    suspend fun countAmarelosByJogadorJogo(jogadorId: String, jogoId: String): Int

    /** Golos de um jogador num jogo específico (para meta do MVP). */
    @Query("SELECT COUNT(*) FROM evento_jogo WHERE jogador_id = :jogadorId AND jogo_id = :jogoId AND tipo = 'golo'")
    suspend fun countGolosNoJogo(jogadorId: String, jogoId: String): Int

    /** Assistências de um jogador num jogo específico (para meta do MVP). */
    @Query("SELECT COUNT(*) FROM evento_jogo WHERE assistencia_id = :jogadorId AND jogo_id = :jogoId AND tipo = 'golo'")
    suspend fun countAssistenciasNoJogo(jogadorId: String, jogoId: String): Int

    @Query("""
        SELECT e.id AS evento_id, e.jogo_id, e.tipo, e.minuto, e.equipa_id,
               e.jogador_id,
               u1.nome AS jogador_nome,
               u2.nome AS jogador_sai_nome,
               u3.nome AS jogador_entra_nome,
               e.jogador_sai_id,
               e.jogador_entra_id,
               u4.nome AS assistencia_nome
        FROM evento_jogo e
        LEFT JOIN utilizador u1 ON u1.id = e.jogador_id
        LEFT JOIN utilizador u2 ON u2.id = e.jogador_sai_id
        LEFT JOIN utilizador u3 ON u3.id = e.jogador_entra_id
        LEFT JOIN utilizador u4 ON u4.id = e.assistencia_id
        WHERE e.jogo_id = :jogoId
        ORDER BY e.minuto ASC
    """)
    fun getEventosComNome(jogoId: String): Flow<List<EventoComNome>>

    @Query("DELETE FROM evento_jogo WHERE jogo_id = :jogoId")
    suspend fun deleteByJogo(jogoId: String)

    // ── Ranking queries ───────────────────────────────────────────────────────

    @Query("""
        SELECT ej.jogador_id, COALESCE(u.nome, '?') AS nome,
               COALESCE(e.nome,'—') AS equipa_nome, COALESCE(e.iniciais,'?') AS equipa_iniciais,
               COALESCE(e.cor_avatar,'#3D5A80') AS equipa_cor, COUNT(*) AS total
        FROM evento_jogo ej
        LEFT JOIN utilizador u ON u.id = ej.jogador_id
        LEFT JOIN equipa e ON e.id = ej.equipa_id
        WHERE ej.tipo = 'golo' AND ej.jogador_id IS NOT NULL
        GROUP BY ej.jogador_id ORDER BY total DESC
    """)
    suspend fun getRankingGolos(): List<RankingRow>

    @Query("""
        SELECT ej.jogador_id, COALESCE(u.nome, '?') AS nome,
               COALESCE(e.nome,'—') AS equipa_nome, COALESCE(e.iniciais,'?') AS equipa_iniciais,
               COALESCE(e.cor_avatar,'#3D5A80') AS equipa_cor, COUNT(*) AS total
        FROM evento_jogo ej
        INNER JOIN jogo j ON j.id = ej.jogo_id AND j.torneio_id = :torneioId
        LEFT JOIN utilizador u ON u.id = ej.jogador_id
        LEFT JOIN equipa e ON e.id = ej.equipa_id
        WHERE ej.tipo = 'golo' AND ej.jogador_id IS NOT NULL
        GROUP BY ej.jogador_id ORDER BY total DESC
    """)
    suspend fun getRankingGolosByTorneio(torneioId: String): List<RankingRow>

    @Query("""
        SELECT ej.assistencia_id AS jogador_id, COALESCE(u.nome, '?') AS nome,
               COALESCE(e.nome,'—') AS equipa_nome, COALESCE(e.iniciais,'?') AS equipa_iniciais,
               COALESCE(e.cor_avatar,'#3D5A80') AS equipa_cor, COUNT(*) AS total
        FROM evento_jogo ej
        LEFT JOIN utilizador u ON u.id = ej.assistencia_id
        LEFT JOIN equipa e ON e.id = ej.equipa_id
        WHERE ej.tipo = 'golo' AND ej.assistencia_id IS NOT NULL
        GROUP BY ej.assistencia_id ORDER BY total DESC
    """)
    suspend fun getRankingAssistencias(): List<RankingRow>

    @Query("""
        SELECT ej.assistencia_id AS jogador_id, COALESCE(u.nome, '?') AS nome,
               COALESCE(e.nome,'—') AS equipa_nome, COALESCE(e.iniciais,'?') AS equipa_iniciais,
               COALESCE(e.cor_avatar,'#3D5A80') AS equipa_cor, COUNT(*) AS total
        FROM evento_jogo ej
        INNER JOIN jogo j ON j.id = ej.jogo_id AND j.torneio_id = :torneioId
        LEFT JOIN utilizador u ON u.id = ej.assistencia_id
        LEFT JOIN equipa e ON e.id = ej.equipa_id
        WHERE ej.tipo = 'golo' AND ej.assistencia_id IS NOT NULL
        GROUP BY ej.assistencia_id ORDER BY total DESC
    """)
    suspend fun getRankingAssistenciasByTorneio(torneioId: String): List<RankingRow>

    @Query("""
        SELECT ej.jogador_id, COALESCE(u.nome, '?') AS nome,
               COALESCE(e.nome,'—') AS equipa_nome, COALESCE(e.iniciais,'?') AS equipa_iniciais,
               COALESCE(e.cor_avatar,'#3D5A80') AS equipa_cor, COUNT(*) AS total
        FROM evento_jogo ej
        LEFT JOIN utilizador u ON u.id = ej.jogador_id
        LEFT JOIN equipa e ON e.id = ej.equipa_id
        WHERE ej.tipo = 'amarelo' AND ej.jogador_id IS NOT NULL
        GROUP BY ej.jogador_id ORDER BY total DESC
    """)
    suspend fun getRankingAmarelos(): List<RankingRow>

    @Query("""
        SELECT ej.jogador_id, COALESCE(u.nome, '?') AS nome,
               COALESCE(e.nome,'—') AS equipa_nome, COALESCE(e.iniciais,'?') AS equipa_iniciais,
               COALESCE(e.cor_avatar,'#3D5A80') AS equipa_cor, COUNT(*) AS total
        FROM evento_jogo ej
        LEFT JOIN utilizador u ON u.id = ej.jogador_id
        LEFT JOIN equipa e ON e.id = ej.equipa_id
        WHERE ej.tipo = 'vermelho' AND ej.jogador_id IS NOT NULL
        GROUP BY ej.jogador_id ORDER BY total DESC
    """)
    suspend fun getRankingVermelhos(): List<RankingRow>

    @Query("""
        SELECT ej.jogador_id, COALESCE(u.nome, '?') AS nome,
               COALESCE(e.nome,'—') AS equipa_nome, COALESCE(e.iniciais,'?') AS equipa_iniciais,
               COALESCE(e.cor_avatar,'#3D5A80') AS equipa_cor,
               SUM(CASE WHEN ej.tipo='amarelo' THEN 1 ELSE 0 END) AS amarelos,
               SUM(CASE WHEN ej.tipo='vermelho' THEN 1 ELSE 0 END) AS vermelhos
        FROM evento_jogo ej
        INNER JOIN jogo j ON j.id = ej.jogo_id AND j.torneio_id = :torneioId
        LEFT JOIN utilizador u ON u.id = ej.jogador_id
        LEFT JOIN equipa e ON e.id = ej.equipa_id
        WHERE ej.tipo IN ('amarelo','vermelho') AND ej.jogador_id IS NOT NULL
        GROUP BY ej.jogador_id ORDER BY (amarelos + vermelhos) DESC
    """)
    suspend fun getRankingCartoesByTorneio(torneioId: String): List<CartaoRow>
}
