package com.hojetembola.app.data.local.dao

import androidx.room.*
import com.hojetembola.app.data.local.entity.MembroEquipaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MembroEquipaDao {

    @Query("SELECT * FROM membro_equipa WHERE equipa_id = :equipaId")
    fun getMembros(equipaId: String): Flow<List<MembroEquipaEntity>>

    @Query("SELECT * FROM membro_equipa WHERE utilizador_id = :utilizadorId")
    fun getEquipasByUtilizador(utilizadorId: String): Flow<List<MembroEquipaEntity>>

    @Query("SELECT COUNT(*) FROM membro_equipa WHERE equipa_id = :equipaId")
    suspend fun countMembros(equipaId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(membro: MembroEquipaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(membros: List<MembroEquipaEntity>)

    @Delete
    suspend fun delete(membro: MembroEquipaEntity)

    @Query("DELETE FROM membro_equipa WHERE equipa_id = :equipaId AND utilizador_id = :utilizadorId")
    suspend fun deleteByEquipaAndUtilizador(equipaId: String, utilizadorId: String)
}
