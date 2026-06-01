package com.hojetembola.app.data.local.dao

import androidx.room.*
import com.hojetembola.app.data.local.entity.InscricaoEquipaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InscricaoEquipaDao {

    @Query("SELECT * FROM inscricao_equipa WHERE torneio_id = :torneioId")
    fun getByTorneio(torneioId: String): Flow<List<InscricaoEquipaEntity>>

    @Query("SELECT * FROM inscricao_equipa WHERE equipa_id = :equipaId")
    fun getByEquipa(equipaId: String): Flow<List<InscricaoEquipaEntity>>

    @Query("SELECT COUNT(*) FROM inscricao_equipa WHERE torneio_id = :torneioId AND estado = 'inscrita'")
    suspend fun countInscritasAtivas(torneioId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(inscricao: InscricaoEquipaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(inscricoes: List<InscricaoEquipaEntity>)

    @Update
    suspend fun update(inscricao: InscricaoEquipaEntity)

    @Query("UPDATE inscricao_equipa SET estado = :estado WHERE torneio_id = :torneioId AND equipa_id = :equipaId")
    suspend fun updateEstado(torneioId: String, equipaId: String, estado: String)

    @Delete
    suspend fun delete(inscricao: InscricaoEquipaEntity)
}
