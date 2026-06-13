package com.hojetembola.app.data.local.dao

import androidx.room.*
import com.hojetembola.app.data.local.entity.VotoMvpEntity
import kotlinx.coroutines.flow.Flow
import com.hojetembola.app.data.local.dao.RankingRow

@Dao
interface VotoMvpDao {

    @Query("SELECT * FROM voto_mvp WHERE jogo_id = :jogoId")
    fun getByJogo(jogoId: String): Flow<List<VotoMvpEntity>>

    @Query("SELECT * FROM voto_mvp WHERE jogo_id = :jogoId AND tipo_votante = :tipo")
    fun getByJogoETipo(jogoId: String, tipo: String): Flow<List<VotoMvpEntity>>

    @Query("SELECT * FROM voto_mvp WHERE jogo_id = :jogoId AND votante_id = :votanteId LIMIT 1")
    suspend fun getVotoDoUtilizador(jogoId: String, votanteId: String): VotoMvpEntity?

    @Query("SELECT * FROM voto_mvp WHERE jogo_id = :jogoId AND votante_id = :votanteId AND tipo_votante = :tipo LIMIT 1")
    suspend fun getVotoDoUtilizadorETipo(jogoId: String, votanteId: String, tipo: String): VotoMvpEntity?

    @Query("DELETE FROM voto_mvp WHERE jogo_id = :jogoId AND votante_id = :votanteId AND tipo_votante = :tipo")
    suspend fun deleteVotoDoUtilizadorETipo(jogoId: String, votanteId: String, tipo: String)

    /** Contagem de votos por candidato num jogo (para calcular percentagens) */
    @Query("""
        SELECT votado_id, COUNT(*) as total FROM voto_mvp
        WHERE jogo_id = :jogoId
        GROUP BY votado_id
        ORDER BY total DESC
    """)
    fun getContagemVotos(jogoId: String): Flow<List<VotoContagem>>

    @Query("SELECT COUNT(DISTINCT jogo_id) FROM voto_mvp WHERE votado_id = :jogadorId")
    suspend fun countMvpsRecebidosByJogador(jogadorId: String): Int

    @Query("""
        SELECT vm.votado_id AS jogador_id, COALESCE(u.nome,'?') AS nome,
               COALESCE(e.nome,'—') AS equipa_nome, COALESCE(e.iniciais,'?') AS equipa_iniciais,
               COALESCE(e.cor_avatar,'#3D5A80') AS equipa_cor,
               COUNT(DISTINCT vm.jogo_id) AS total
        FROM voto_mvp vm
        LEFT JOIN utilizador u ON u.id = vm.votado_id
        LEFT JOIN convocatoria_jogo cj ON cj.utilizador_id = vm.votado_id AND cj.jogo_id = vm.jogo_id
        LEFT JOIN equipa e ON e.id = cj.equipa_id
        GROUP BY vm.votado_id ORDER BY total DESC
    """)
    suspend fun getRankingMvps(): List<RankingRow>

    @Query("""
        SELECT vm.votado_id AS jogador_id, COALESCE(u.nome,'?') AS nome,
               COALESCE(e.nome,'—') AS equipa_nome, COALESCE(e.iniciais,'?') AS equipa_iniciais,
               COALESCE(e.cor_avatar,'#3D5A80') AS equipa_cor,
               COUNT(DISTINCT vm.jogo_id) AS total
        FROM voto_mvp vm
        INNER JOIN jogo j ON j.id = vm.jogo_id AND j.torneio_id = :torneioId
        LEFT JOIN utilizador u ON u.id = vm.votado_id
        LEFT JOIN convocatoria_jogo cj ON cj.utilizador_id = vm.votado_id AND cj.jogo_id = vm.jogo_id
        LEFT JOIN equipa e ON e.id = cj.equipa_id
        GROUP BY vm.votado_id ORDER BY total DESC
    """)
    suspend fun getRankingMvpsByTorneio(torneioId: String): List<RankingRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(voto: VotoMvpEntity)

    @Update
    suspend fun update(voto: VotoMvpEntity)

    @Delete
    suspend fun delete(voto: VotoMvpEntity)

    @Query("DELETE FROM voto_mvp WHERE jogo_id = :jogoId AND votante_id = :votanteId")
    suspend fun deleteVotoDoUtilizador(jogoId: String, votanteId: String)
}

/** Projeção auxiliar para contagem de votos por candidato. */
data class VotoContagem(
    @ColumnInfo(name = "votado_id") val votadoId: String,
    val total: Int
)
