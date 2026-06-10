package com.hojetembola.app.data.local.dao

import androidx.room.*
import com.hojetembola.app.data.local.entity.MembroComNome
import com.hojetembola.app.data.local.entity.MembroEquipaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MembroEquipaDao {

    @Query("SELECT * FROM membro_equipa WHERE equipa_id = :equipaId")
    fun getMembros(equipaId: String): Flow<List<MembroEquipaEntity>>

    @Query("SELECT * FROM membro_equipa WHERE utilizador_id = :utilizadorId")
    fun getEquipasByUtilizador(utilizadorId: String): Flow<List<MembroEquipaEntity>>

    @Query("SELECT COUNT(*) FROM membro_equipa WHERE equipa_id = :equipaId AND ativo = 1")
    suspend fun countMembros(equipaId: String): Int

    /** Verifica se um utilizador específico é membro activo de uma equipa. */
    @Query("SELECT COUNT(*) FROM membro_equipa WHERE equipa_id = :equipaId AND utilizador_id = :utilizadorId AND ativo = 1")
    suspend fun countMembroAtivo(equipaId: String, utilizadorId: String): Int

    /** Lista plana de membros activos — sem JOIN, para validações rápidas. */
    @Query("SELECT * FROM membro_equipa WHERE equipa_id = :equipaId AND ativo = 1")
    suspend fun getMembrosAtivos(equipaId: String): List<MembroEquipaEntity>

    /**
     * JOIN com utilizador para obter nomes e emails dos membros.
     * Requer que os registos de utilizador estejam em cache no Room.
     */
    @Query("""
        SELECT m.id, m.equipa_id, m.utilizador_id, u.nome, u.email, m.ativo
        FROM membro_equipa m
        INNER JOIN utilizador u ON u.id = m.utilizador_id
        WHERE m.equipa_id = :equipaId AND m.ativo = 1
        ORDER BY u.nome
    """)
    fun getMembrosComNome(equipaId: String): Flow<List<MembroComNome>>

    @Query("""
        SELECT m.id, m.equipa_id, m.utilizador_id, u.nome, u.email, m.ativo
        FROM membro_equipa m
        INNER JOIN utilizador u ON u.id = m.utilizador_id
        WHERE m.equipa_id = :equipaId AND m.ativo = 1
        ORDER BY u.nome
    """)
    suspend fun getMembrosComNomeSuspend(equipaId: String): List<MembroComNome>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(membro: MembroEquipaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(membros: List<MembroEquipaEntity>)

    @Delete
    suspend fun delete(membro: MembroEquipaEntity)

    @Query("DELETE FROM membro_equipa WHERE equipa_id = :equipaId AND utilizador_id = :utilizadorId")
    suspend fun deleteByEquipaAndUtilizador(equipaId: String, utilizadorId: String)

    /** IDs de utilizadores já inscritos neste torneio por outra equipa (para detectar conflitos). */
    @Query("""
        SELECT DISTINCT m.utilizador_id
        FROM membro_equipa m
        INNER JOIN inscricao_equipa ie ON ie.equipa_id = m.equipa_id
        WHERE ie.torneio_id = :torneioId
          AND m.equipa_id != :equipaId
          AND ie.estado NOT IN ('Rejeitada', 'Desistente')
    """)
    suspend fun getUtilizadoresNoTorneio(torneioId: String, equipaId: String): List<String>
}
