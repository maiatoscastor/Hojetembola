package com.hojetembola.app.data.local.dao

import androidx.room.*
import com.hojetembola.app.data.local.entity.ConvocatoriaJogoEntity
import kotlinx.coroutines.flow.Flow
import com.hojetembola.app.data.local.dao.RankingRow

@Dao
interface ConvocatoriaJogoDao {

    @Query("SELECT * FROM convocatoria_jogo WHERE jogo_id = :jogoId")
    fun getByJogo(jogoId: String): Flow<List<ConvocatoriaJogoEntity>>

    @Query("SELECT * FROM convocatoria_jogo WHERE jogo_id = :jogoId")
    suspend fun getByJogoSuspend(jogoId: String): List<ConvocatoriaJogoEntity>

    @Query("SELECT * FROM convocatoria_jogo WHERE jogo_id = :jogoId AND equipa_id = :equipaId")
    fun getByJogoEquipa(jogoId: String, equipaId: String): Flow<List<ConvocatoriaJogoEntity>>

    @Query("""
        SELECT * FROM convocatoria_jogo
        WHERE jogo_id = :jogoId AND equipa_id = :equipaId AND is_titular = 1
    """)
    suspend fun getTitularesSuspend(jogoId: String, equipaId: String): List<ConvocatoriaJogoEntity>

    @Query("SELECT COUNT(*) FROM convocatoria_jogo WHERE jogo_id = :jogoId")
    suspend fun countByJogo(jogoId: String): Int

    @Query("""
        SELECT COUNT(*) FROM convocatoria_jogo cj
        INNER JOIN jogo j ON j.id = cj.jogo_id
        WHERE cj.utilizador_id = :utilizadorId AND j.estado = 'terminado'
    """)
    suspend fun countJogosDisputadosByJogador(utilizadorId: String): Int

    @Query("""
        SELECT cj.utilizador_id AS jogador_id, COALESCE(u.nome,'?') AS nome,
               COALESCE(e.nome,'—') AS equipa_nome, COALESCE(e.iniciais,'?') AS equipa_iniciais,
               COALESCE(e.cor_avatar,'#3D5A80') AS equipa_cor,
               COUNT(DISTINCT cj.jogo_id) AS total
        FROM convocatoria_jogo cj
        INNER JOIN jogo j ON j.id = cj.jogo_id AND j.estado = 'terminado'
        LEFT JOIN utilizador u ON u.id = cj.utilizador_id
        LEFT JOIN equipa e ON e.id = cj.equipa_id
        GROUP BY cj.utilizador_id ORDER BY total DESC
    """)
    suspend fun getRankingJogos(): List<RankingRow>

    @Query("""
        SELECT cj.utilizador_id AS jogador_id, COALESCE(u.nome,'?') AS nome,
               COALESCE(e.nome,'—') AS equipa_nome, COALESCE(e.iniciais,'?') AS equipa_iniciais,
               COALESCE(e.cor_avatar,'#3D5A80') AS equipa_cor,
               COUNT(DISTINCT cj.jogo_id) AS total
        FROM convocatoria_jogo cj
        INNER JOIN jogo j ON j.id = cj.jogo_id AND j.torneio_id = :torneioId AND j.estado = 'terminado'
        LEFT JOIN utilizador u ON u.id = cj.utilizador_id
        LEFT JOIN equipa e ON e.id = cj.equipa_id
        GROUP BY cj.utilizador_id ORDER BY total DESC
    """)
    suspend fun getRankingJogosByTorneio(torneioId: String): List<RankingRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(convocatoria: ConvocatoriaJogoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(convocatorias: List<ConvocatoriaJogoEntity>)

    @Delete
    suspend fun delete(convocatoria: ConvocatoriaJogoEntity)

    @Query("DELETE FROM convocatoria_jogo WHERE jogo_id = :jogoId")
    suspend fun deleteByJogo(jogoId: String)
}
