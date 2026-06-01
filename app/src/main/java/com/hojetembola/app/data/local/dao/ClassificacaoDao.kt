package com.hojetembola.app.data.local.dao

import androidx.room.*
import com.hojetembola.app.data.local.entity.ClassificacaoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassificacaoDao {

    @Query("SELECT * FROM classificacao WHERE torneio_id = :torneioId ORDER BY posicao ASC")
    fun getByTorneio(torneioId: String): Flow<List<ClassificacaoEntity>>

    @Query("SELECT * FROM classificacao WHERE torneio_id = :torneioId AND equipa_id = :equipaId LIMIT 1")
    suspend fun getByTorneioEquipa(torneioId: String, equipaId: String): ClassificacaoEntity?

    @Query("""
        SELECT SUM(golos_marcados) FROM classificacao
        WHERE equipa_id = :equipaId
    """)
    suspend fun totalGolsMarcados(equipaId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(classificacao: ClassificacaoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(classificacoes: List<ClassificacaoEntity>)

    @Update
    suspend fun update(classificacao: ClassificacaoEntity)

    @Delete
    suspend fun delete(classificacao: ClassificacaoEntity)
}
