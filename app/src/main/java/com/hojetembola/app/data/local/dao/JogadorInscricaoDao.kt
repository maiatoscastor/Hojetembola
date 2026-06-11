package com.hojetembola.app.data.local.dao

import androidx.room.*
import com.hojetembola.app.data.local.entity.JogadorInscricaoEntity
import kotlinx.coroutines.flow.Flow

data class JogadorInscricaoComNome(
    @ColumnInfo(name = "utilizador_id") val utilizadorId: String,
    val nome: String,
    val email: String
)

@Dao
interface JogadorInscricaoDao {

    @Query("SELECT * FROM jogador_inscricao WHERE inscricao_id = :inscricaoId")
    fun getByInscricao(inscricaoId: String): Flow<List<JogadorInscricaoEntity>>

    @Query("SELECT * FROM jogador_inscricao WHERE utilizador_id = :utilizadorId")
    fun getByUtilizador(utilizadorId: String): Flow<List<JogadorInscricaoEntity>>

    @Query("""
        SELECT ji.utilizador_id, u.nome, u.email
        FROM jogador_inscricao ji
        INNER JOIN utilizador u ON u.id = ji.utilizador_id
        WHERE ji.inscricao_id = :inscricaoId
        ORDER BY u.nome
    """)
    fun getComNome(inscricaoId: String): Flow<List<JogadorInscricaoComNome>>

    @Query("""
        SELECT ji.utilizador_id, u.nome, u.email
        FROM jogador_inscricao ji
        INNER JOIN utilizador u ON u.id = ji.utilizador_id
        WHERE ji.inscricao_id = :inscricaoId
        ORDER BY u.nome
    """)
    suspend fun getComNomeSuspend(inscricaoId: String): List<JogadorInscricaoComNome>

    @Query("SELECT * FROM jogador_inscricao WHERE inscricao_id = :inscricaoId")
    suspend fun getByInscricaoSuspend(inscricaoId: String): List<JogadorInscricaoEntity>

    @Query("SELECT COUNT(*) FROM jogador_inscricao WHERE inscricao_id = :inscricaoId")
    suspend fun countByInscricao(inscricaoId: String): Int

    @Query("SELECT COUNT(*) FROM jogador_inscricao WHERE inscricao_id = :inscricaoId AND utilizador_id = :utilizadorId")
    suspend fun countByInscricaoAndUtilizador(inscricaoId: String, utilizadorId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(jogador: JogadorInscricaoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(jogadores: List<JogadorInscricaoEntity>)

    @Delete
    suspend fun delete(jogador: JogadorInscricaoEntity)

    @Query("DELETE FROM jogador_inscricao WHERE inscricao_id = :inscricaoId")
    suspend fun deleteByInscricao(inscricaoId: String)
}
