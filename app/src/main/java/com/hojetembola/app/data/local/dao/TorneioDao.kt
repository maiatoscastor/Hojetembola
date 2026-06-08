package com.hojetembola.app.data.local.dao

import androidx.room.*
import com.hojetembola.app.data.local.entity.TorneioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TorneioDao {

    @Query("SELECT * FROM torneio WHERE id = :id")
    suspend fun getById(id: String): TorneioEntity?

    @Query("SELECT * FROM torneio WHERE id = :id")
    fun getByIdFlow(id: String): Flow<TorneioEntity?>

    /** Procura um torneio local pelo código de acesso (4 dígitos) */
    @Query("SELECT * FROM torneio WHERE codigo_acesso = :codigo LIMIT 1")
    suspend fun getByCodigoAcesso(codigo: String): TorneioEntity?

    @Query("SELECT * FROM torneio ORDER BY data_inicio DESC")
    fun getAll(): Flow<List<TorneioEntity>>

    @Query("SELECT * FROM torneio WHERE organizador_id = :organizadorId ORDER BY data_inicio DESC")
    fun getByOrganizador(organizadorId: String): Flow<List<TorneioEntity>>

    @Query("SELECT * FROM torneio WHERE estado = :estado")
    fun getByEstado(estado: String): Flow<List<TorneioEntity>>

    @Query("SELECT * FROM torneio WHERE visibilidade = 'Publico' ORDER BY data_inicio DESC")
    fun getPublicos(): Flow<List<TorneioEntity>>

    /**
     * Torneios em que o utilizador está envolvido:
     *   1. É o organizador
     *   2. É capitão de uma equipa inscrita
     *   3. É membro de uma equipa inscrita
     */
    @Query("""
        SELECT DISTINCT t.* FROM torneio t
        WHERE t.organizador_id = :utilizadorId
        UNION
        SELECT DISTINCT t.* FROM torneio t
        INNER JOIN inscricao_equipa ie ON ie.torneio_id = t.id
        INNER JOIN equipa e ON e.id = ie.equipa_id
        WHERE e.capitao_id = :utilizadorId
           OR EXISTS (
               SELECT 1 FROM membro_equipa m
               WHERE m.equipa_id = e.id AND m.utilizador_id = :utilizadorId
           )
        ORDER BY data_inicio DESC
    """)
    fun getMeusTorneios(utilizadorId: String): Flow<List<TorneioEntity>>

    /** Torneios pendentes de sincronização (modo offline) */
    @Query("SELECT * FROM torneio WHERE sincronizado = 0")
    suspend fun getPendentesSync(): List<TorneioEntity>

    /** Verifica se já existe um torneio local com este código de acesso */
    @Query("SELECT COUNT(*) FROM torneio WHERE codigo_acesso = :codigo")
    suspend fun countByCodigoAcesso(codigo: String): Int

    /**
     * Remove os torneios já sincronizados de um organizador.
     * Usado em syncTorneios para evitar duplicados UUID/integer-id após re-sync.
     */
    @Query("DELETE FROM torneio WHERE organizador_id = :organizadorId AND sincronizado = 1")
    suspend fun deleteSyncedByOrganizadorId(organizadorId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(torneio: TorneioEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(torneios: List<TorneioEntity>)

    @Update
    suspend fun update(torneio: TorneioEntity)

    @Delete
    suspend fun delete(torneio: TorneioEntity)

    @Query("UPDATE torneio SET estado = :estado WHERE id = :id")
    suspend fun updateEstado(id: String, estado: String)

    @Query("UPDATE torneio SET sincronizado = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
