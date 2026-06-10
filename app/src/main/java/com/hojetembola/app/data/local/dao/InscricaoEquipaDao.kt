package com.hojetembola.app.data.local.dao

import androidx.room.*
import com.hojetembola.app.data.local.entity.InscricaoComEquipa
import com.hojetembola.app.data.local.entity.InscricaoEquipaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InscricaoEquipaDao {

    @Query("SELECT * FROM inscricao_equipa WHERE torneio_id = :torneioId")
    fun getByTorneio(torneioId: String): Flow<List<InscricaoEquipaEntity>>

    @Query("SELECT * FROM inscricao_equipa WHERE equipa_id = :equipaId")
    fun getByEquipa(equipaId: String): Flow<List<InscricaoEquipaEntity>>

    /** Devolve a inscrição desta equipa neste torneio, ou null se não existe. */
    @Query("SELECT * FROM inscricao_equipa WHERE torneio_id = :torneioId AND equipa_id = :equipaId LIMIT 1")
    suspend fun getByTorneioAndEquipa(torneioId: String, equipaId: String): InscricaoEquipaEntity?

    /** Versão suspend — necessária para validações pontuais sem observar mudanças. */
    @Query("SELECT * FROM inscricao_equipa WHERE torneio_id = :torneioId")
    suspend fun getByTorneioSuspend(torneioId: String): List<InscricaoEquipaEntity>

    /** JOIN com equipa — usado no ecrã de Gerir Torneio. */
    @Query("""
        SELECT ie.id, ie.torneio_id, ie.equipa_id, ie.inscrita_por_id, ie.data_inscricao,
               ie.estado, ie.grupo,
               COALESCE(e.nome, '?') AS nome,
               COALESCE(e.iniciais, '?') AS iniciais,
               COALESCE(e.cor_avatar, '#888888') AS cor_avatar,
               (SELECT COUNT(*) FROM membro_equipa m WHERE m.equipa_id = ie.equipa_id AND m.ativo = 1) AS num_membros
        FROM inscricao_equipa ie
        LEFT JOIN equipa e ON e.id = ie.equipa_id
        WHERE ie.torneio_id = :torneioId
        ORDER BY
            CASE ie.estado
                WHEN 'Pendente'   THEN 0
                WHEN 'Confirmada' THEN 1
                WHEN 'Rejeitada'  THEN 2
                WHEN 'Desistente' THEN 3
                ELSE 4 END,
            ie.data_inscricao ASC
    """)
    fun getByTorneioComEquipa(torneioId: String): Flow<List<InscricaoComEquipa>>

    /** true se já existe uma inscrição activa (Pendente ou Confirmada) para esta equipa neste torneio. */
    @Query("""
        SELECT COUNT(*) FROM inscricao_equipa
        WHERE torneio_id = :torneioId AND equipa_id = :equipaId
          AND estado NOT IN ('Rejeitada', 'Desistente')
    """)
    suspend fun countInscricaoAtiva(torneioId: String, equipaId: String): Int

    /** Nº total de equipas aceites num torneio. */
    @Query("SELECT COUNT(*) FROM inscricao_equipa WHERE torneio_id = :torneioId AND estado = 'Aceite'")
    suspend fun countAceites(torneioId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(inscricao: InscricaoEquipaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(inscricoes: List<InscricaoEquipaEntity>)

    @Update
    suspend fun update(inscricao: InscricaoEquipaEntity)

    @Query("UPDATE inscricao_equipa SET estado = :estado WHERE torneio_id = :torneioId AND equipa_id = :equipaId")
    suspend fun updateEstado(torneioId: String, equipaId: String, estado: String)

    @Query("UPDATE inscricao_equipa SET estado = :estado WHERE id = :id")
    suspend fun updateEstadoById(id: String, estado: String)

    @Delete
    suspend fun delete(inscricao: InscricaoEquipaEntity)
}
