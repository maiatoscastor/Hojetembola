package com.hojetembola.app.data.local.dao

import androidx.room.*
import com.hojetembola.app.data.local.entity.UtilizadorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UtilizadorDao {

    @Query("SELECT * FROM utilizador WHERE id = :id")
    suspend fun getById(id: String): UtilizadorEntity?

    @Query("SELECT * FROM utilizador WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): UtilizadorEntity?

    @Query("SELECT * FROM utilizador")
    fun getAll(): Flow<List<UtilizadorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(utilizador: UtilizadorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(utilizadores: List<UtilizadorEntity>)

    @Update
    suspend fun update(utilizador: UtilizadorEntity)

    @Delete
    suspend fun delete(utilizador: UtilizadorEntity)

    @Query("DELETE FROM utilizador WHERE id = :id")
    suspend fun deleteById(id: String)
}
