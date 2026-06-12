package com.hojetembola.app.data.local.dao

import androidx.room.*
import com.hojetembola.app.data.local.entity.SuspensaoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SuspensaoDao {

    @Query("SELECT * FROM suspensao WHERE utilizador_id = :utilizadorId AND torneio_id = :torneioId AND cumprida = 0")
    fun getSuspensoesAtivas(utilizadorId: String, torneioId: String): Flow<List<SuspensaoEntity>>

    @Query("SELECT * FROM suspensao WHERE torneio_id = :torneioId AND cumprida = 0")
    fun getSuspensoesAtivasTorneio(torneioId: String): Flow<List<SuspensaoEntity>>

    @Query("SELECT * FROM suspensao WHERE torneio_id = :torneioId AND cumprida = 0")
    suspend fun getSuspensoesAtivasSuspend(torneioId: String): List<SuspensaoEntity>

    /** Returns IDs of players currently suspended in this torneio. */
    @Query("SELECT utilizador_id FROM suspensao WHERE torneio_id = :torneioId AND cumprida = 0")
    suspend fun getSuspensosIds(torneioId: String): List<String>

    /** Marks all active suspensions from PREVIOUS games as cumprida (player sat out). */
    @Query("UPDATE suspensao SET cumprida = 1 WHERE torneio_id = :torneioId AND jogo_id != :jogoAtualId AND cumprida = 0")
    suspend fun marcarCumpridasExcetoJogo(torneioId: String, jogoAtualId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(suspensao: SuspensaoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(suspensoes: List<SuspensaoEntity>)

    @Update
    suspend fun update(suspensao: SuspensaoEntity)

    @Query("UPDATE suspensao SET cumprida = 1 WHERE id = :id")
    suspend fun marcarComoCumprida(id: String)

    @Query("DELETE FROM suspensao WHERE torneio_id = :torneioId")
    suspend fun deleteByTorneio(torneioId: String)

    @Delete
    suspend fun delete(suspensao: SuspensaoEntity)
}
