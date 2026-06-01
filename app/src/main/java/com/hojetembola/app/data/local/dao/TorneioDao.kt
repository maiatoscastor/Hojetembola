package com.hojetembola.app.data.local.dao

import androidx.room.*
import com.hojetembola.app.data.local.entity.TorneioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TorneioDao {

    @Query("SELECT * FROM torneio WHERE id = :id")
    suspend fun getById(id: String): TorneioEntity?

    @Query("SELECT * FROM torneio ORDER BY data_inicio DESC")
    fun getAll(): Flow<List<TorneioEntity>>

    @Query("SELECT * FROM torneio WHERE organizador_id = :organizadorId ORDER BY data_inicio DESC")
    fun getByOrganizador(organizadorId: String): Flow<List<TorneioEntity>>

    @Query("SELECT * FROM torneio WHERE estado = :estado")
    fun getByEstado(estado: String): Flow<List<TorneioEntity>>

    @Query("SELECT * FROM torneio WHERE publico = 1 ORDER BY data_inicio DESC")
    fun getPublicos(): Flow<List<TorneioEntity>>

    @Query("""
        SELECT t.* FROM torneio t
        INNER JOIN inscricao_equipa ie ON ie.torneio_id = t.id
        INNER JOIN equipa e ON e.id = ie.equipa_id
        WHERE e.capitao_id = :utilizadorId OR EXISTS (
            SELECT 1 FROM membro_equipa m
            WHERE m.equipa_id = e.id AND m.utilizador_id = :utilizadorId
        )
        GROUP BY t.id
        ORDER BY t.data_inicio DESC
    """)
    fun getMeusTorneios(utilizadorId: String): Flow<List<TorneioEntity>>

    /** Torneios pendentes de sincronização (modo offline) */
    @Query("SELECT * FROM torneio WHERE is_synced = 0")
    suspend fun getPendentesSync(): List<TorneioEntity>

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

    @Query("UPDATE torneio SET is_synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
