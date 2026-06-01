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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(suspensao: SuspensaoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(suspensoes: List<SuspensaoEntity>)

    @Update
    suspend fun update(suspensao: SuspensaoEntity)

    @Query("UPDATE suspensao SET cumprida = 1 WHERE id = :id")
    suspend fun marcarComoCumprida(id: String)

    @Delete
    suspend fun delete(suspensao: SuspensaoEntity)
}
