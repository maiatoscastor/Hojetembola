package com.hojetembola.app.data.local.dao

import androidx.room.*
import com.hojetembola.app.data.local.entity.VotoMvpEntity
import kotlinx.coroutines.flow.Flow

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
