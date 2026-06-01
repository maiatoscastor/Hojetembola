package com.hojetembola.app.data.local.dao

import androidx.room.*
import com.hojetembola.app.data.local.entity.EquipaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipaDao {

    @Query("SELECT * FROM equipa WHERE id = :id")
    suspend fun getById(id: String): EquipaEntity?

    @Query("SELECT * FROM equipa WHERE capitao_id = :capitaoId")
    fun getByCapitao(capitaoId: String): Flow<List<EquipaEntity>>

    @Query("""
        SELECT e.* FROM equipa e
        INNER JOIN membro_equipa m ON m.equipa_id = e.id
        WHERE m.utilizador_id = :utilizadorId
    """)
    fun getEquipasByMembro(utilizadorId: String): Flow<List<EquipaEntity>>

    @Query("SELECT * FROM equipa")
    fun getAll(): Flow<List<EquipaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(equipa: EquipaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(equipas: List<EquipaEntity>)

    @Update
    suspend fun update(equipa: EquipaEntity)

    @Delete
    suspend fun delete(equipa: EquipaEntity)
}
