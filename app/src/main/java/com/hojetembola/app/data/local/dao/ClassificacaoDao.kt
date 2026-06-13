package com.hojetembola.app.data.local.dao

import androidx.room.*
import com.hojetembola.app.data.local.entity.ClassificacaoEntity
import kotlinx.coroutines.flow.Flow
import com.hojetembola.app.data.local.dao.ClassificacaoNomeRow

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

    @Query("""
        SELECT c.posicao, COALESCE(e.nome,'?') AS equipa_nome,
               c.jogos, c.vitorias, c.empates, c.derrotas,
               c.golos_marcados, c.golos_sofridos, c.pontos
        FROM classificacao c
        LEFT JOIN equipa e ON e.id = c.equipa_id
        WHERE c.torneio_id = :torneioId
        ORDER BY c.posicao ASC
    """)
    suspend fun getClassificacaoComNome(torneioId: String): List<ClassificacaoNomeRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(classificacao: ClassificacaoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(classificacoes: List<ClassificacaoEntity>)

    @Update
    suspend fun update(classificacao: ClassificacaoEntity)

    @Delete
    suspend fun delete(classificacao: ClassificacaoEntity)
}
