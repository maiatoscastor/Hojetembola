package com.hojetembola.app.data.local.dao

import androidx.room.*
import com.hojetembola.app.data.local.entity.JogoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JogoDao {

    @Query("SELECT * FROM jogo WHERE id = :id")
    suspend fun getById(id: String): JogoEntity?

    @Query("SELECT * FROM jogo WHERE torneio_id = :torneioId ORDER BY data_hora ASC")
    fun getByTorneio(torneioId: String): Flow<List<JogoEntity>>

    @Query("SELECT * FROM jogo WHERE jornada_id = :jornadaId ORDER BY data_hora ASC")
    fun getByJornada(jornadaId: String): Flow<List<JogoEntity>>

    @Query("SELECT * FROM jogo WHERE estado = 'ao_vivo'")
    fun getAoVivo(): Flow<List<JogoEntity>>

    @Query("SELECT * FROM jogo WHERE estado = 'agendado' ORDER BY data_hora ASC LIMIT 10")
    fun getProximosAgendados(): Flow<List<JogoEntity>>

    @Query("""
        SELECT * FROM jogo
        WHERE (equipa_casa_id = :equipaId OR equipa_visitante_id = :equipaId)
        ORDER BY data_hora DESC
    """)
    fun getByEquipa(equipaId: String): Flow<List<JogoEntity>>

    @Query("SELECT * FROM jogo WHERE is_synced = 0")
    suspend fun getPendentesSync(): List<JogoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(jogo: JogoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(jogos: List<JogoEntity>)

    @Update
    suspend fun update(jogo: JogoEntity)

    @Delete
    suspend fun delete(jogo: JogoEntity)

    @Query("UPDATE jogo SET estado = :estado, minuto_atual = :minuto WHERE id = :id")
    suspend fun updateEstadoMinuto(id: String, estado: String, minuto: Int?)

    @Query("UPDATE jogo SET golos_casa = :casa, golos_visitante = :visitante WHERE id = :id")
    suspend fun updateResultado(id: String, casa: Int, visitante: Int)

    @Query("UPDATE jogo SET is_synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
