package com.hojetembola.app.data.local.dao

import androidx.room.*
import com.hojetembola.app.data.local.entity.JogoEntity
import kotlinx.coroutines.flow.Flow

data class JogoComEquipas(
    @ColumnInfo(name = "jogo_id") val jogoId: String,
    @ColumnInfo(name = "jornada_id") val jornadaId: String,
    @ColumnInfo(name = "torneio_id") val torneioId: String,
    @ColumnInfo(name = "casa_nome") val casaNome: String,
    @ColumnInfo(name = "casa_iniciais") val casaIniciais: String,
    @ColumnInfo(name = "casa_cor") val casaCor: String,
    @ColumnInfo(name = "visitante_nome") val visitanteNome: String,
    @ColumnInfo(name = "visitante_iniciais") val visitanteIniciais: String,
    @ColumnInfo(name = "visitante_cor") val visitanteCor: String,
    @ColumnInfo(name = "golos_casa") val golosCasa: Int?,
    @ColumnInfo(name = "golos_visitante") val golosVisitante: Int?,
    val estado: String,
    @ColumnInfo(name = "data_hora") val dataHora: String,
    val local: String,
    @ColumnInfo(name = "minuto_atual") val minutoAtual: Int?,
    @ColumnInfo(name = "equipa_casa_id") val equipaCasaId: String,
    @ColumnInfo(name = "equipa_visitante_id") val equipaVisitanteId: String
)

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

    @Query("DELETE FROM jogo WHERE torneio_id = :torneioId")
    suspend fun deleteByTorneio(torneioId: String)

    @Query("""
        SELECT j.id AS jogo_id, j.jornada_id, j.torneio_id,
               j.equipa_casa_id, j.equipa_visitante_id,
               COALESCE(ec.nome, '?') AS casa_nome,
               COALESCE(ec.iniciais, '??') AS casa_iniciais,
               COALESCE(ec.cor_avatar, '#3D5A80') AS casa_cor,
               COALESCE(ev.nome, '?') AS visitante_nome,
               COALESCE(ev.iniciais, '??') AS visitante_iniciais,
               COALESCE(ev.cor_avatar, '#3D5A80') AS visitante_cor,
               j.golos_casa, j.golos_visitante, j.estado,
               j.data_hora, j.local, j.minuto_atual
        FROM jogo j
        LEFT JOIN equipa ec ON ec.id = j.equipa_casa_id
        LEFT JOIN equipa ev ON ev.id = j.equipa_visitante_id
        WHERE j.torneio_id = :torneioId
        ORDER BY j.jornada_id ASC, j.id ASC
    """)
    fun getJogosComEquipas(torneioId: String): Flow<List<JogoComEquipas>>
}
