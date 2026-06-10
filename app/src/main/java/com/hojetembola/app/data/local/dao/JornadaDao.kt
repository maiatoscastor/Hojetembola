package com.hojetembola.app.data.local.dao

import androidx.room.*
import com.hojetembola.app.data.local.entity.JornadaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JornadaDao {

    @Query("SELECT * FROM jornada WHERE id = :id")
    suspend fun getById(id: String): JornadaEntity?

    @Query("SELECT * FROM jornada WHERE torneio_id = :torneioId ORDER BY numero ASC")
    fun getByTorneio(torneioId: String): Flow<List<JornadaEntity>>

    @Query("SELECT COUNT(*) FROM jornada WHERE torneio_id = :torneioId")
    suspend fun countByTorneio(torneioId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(jornada: JornadaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(jornadas: List<JornadaEntity>)

    @Update
    suspend fun update(jornada: JornadaEntity)

    @Delete
    suspend fun delete(jornada: JornadaEntity)
}
