package com.hojetembola.app.data.local.dao

import androidx.room.*
import com.hojetembola.app.data.local.entity.ConvocatoriaJogoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConvocatoriaJogoDao {

    @Query("SELECT * FROM convocatoria_jogo WHERE jogo_id = :jogoId")
    fun getByJogo(jogoId: String): Flow<List<ConvocatoriaJogoEntity>>

    @Query("SELECT * FROM convocatoria_jogo WHERE jogo_id = :jogoId AND equipa_id = :equipaId")
    fun getByJogoEquipa(jogoId: String, equipaId: String): Flow<List<ConvocatoriaJogoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(convocatoria: ConvocatoriaJogoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(convocatorias: List<ConvocatoriaJogoEntity>)

    @Delete
    suspend fun delete(convocatoria: ConvocatoriaJogoEntity)

    @Query("DELETE FROM convocatoria_jogo WHERE jogo_id = :jogoId")
    suspend fun deleteByJogo(jogoId: String)
}
