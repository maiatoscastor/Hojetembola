package com.hojetembola.app.data.local.dao

import androidx.room.*
import com.hojetembola.app.data.local.entity.JogadorInscricaoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JogadorInscricaoDao {

    @Query("SELECT * FROM jogador_inscricao WHERE inscricao_equipa_id = :inscricaoId")
    fun getByInscricao(inscricaoId: String): Flow<List<JogadorInscricaoEntity>>

    @Query("SELECT * FROM jogador_inscricao WHERE utilizador_id = :utilizadorId")
    fun getByUtilizador(utilizadorId: String): Flow<List<JogadorInscricaoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(jogador: JogadorInscricaoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(jogadores: List<JogadorInscricaoEntity>)

    @Delete
    suspend fun delete(jogador: JogadorInscricaoEntity)

    @Query("DELETE FROM jogador_inscricao WHERE inscricao_equipa_id = :inscricaoId")
    suspend fun deleteByInscricao(inscricaoId: String)
}
