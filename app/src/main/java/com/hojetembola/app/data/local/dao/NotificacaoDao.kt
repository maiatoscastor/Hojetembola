package com.hojetembola.app.data.local.dao

import androidx.room.*
import com.hojetembola.app.data.local.entity.NotificacaoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificacaoDao {

    @Query("SELECT * FROM notificacao WHERE utilizador_id = :utilizadorId ORDER BY data_hora DESC")
    fun getAll(utilizadorId: String): Flow<List<NotificacaoEntity>>

    @Query("SELECT * FROM notificacao WHERE utilizador_id = :utilizadorId AND lida = 0 ORDER BY data_hora DESC")
    fun getNaoLidas(utilizadorId: String): Flow<List<NotificacaoEntity>>

    @Query("SELECT COUNT(*) FROM notificacao WHERE utilizador_id = :utilizadorId AND lida = 0")
    fun countNaoLidas(utilizadorId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notificacao: NotificacaoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notificacoes: List<NotificacaoEntity>)

    @Query("UPDATE notificacao SET lida = 1 WHERE id = :id")
    suspend fun marcarComoLida(id: String)

    @Query("UPDATE notificacao SET lida = 1 WHERE utilizador_id = :utilizadorId")
    suspend fun marcarTodasComoLidas(utilizadorId: String)

    @Delete
    suspend fun delete(notificacao: NotificacaoEntity)

    @Query("DELETE FROM notificacao WHERE utilizador_id = :utilizadorId")
    suspend fun deleteAll(utilizadorId: String)
}
