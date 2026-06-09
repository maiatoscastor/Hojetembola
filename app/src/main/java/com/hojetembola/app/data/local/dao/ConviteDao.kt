package com.hojetembola.app.data.local.dao

import androidx.room.*
import com.hojetembola.app.data.local.entity.ConviteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConviteDao {

    @Query("SELECT * FROM convite WHERE id = :id")
    suspend fun getById(id: String): ConviteEntity?

    @Query("SELECT * FROM convite WHERE token = :token LIMIT 1")
    suspend fun getByToken(token: String): ConviteEntity?

    /** Convites pendentes enviados pelo capitão para uma equipa. */
    @Query("SELECT * FROM convite WHERE equipa_id = :equipaId AND estado = 'pendente' ORDER BY data_expiracao ASC")
    fun getConvitesPendentes(equipaId: String): Flow<List<ConviteEntity>>

    /** Versão suspend — para contagens e validações pontuais. */
    @Query("SELECT * FROM convite WHERE equipa_id = :equipaId AND estado = 'pendente'")
    suspend fun getConvitesPendentesSuspend(equipaId: String): List<ConviteEntity>

    /**
     * Devolve um convite não-pendente (recusado/expirado) para um par concreto
     * equipa+jogador — usado para limpar antes de re-convidar.
     */
    @Query("""
        SELECT * FROM convite
        WHERE equipa_id = :equipaId AND convidado_id = :convidadoId AND estado != 'pendente'
        LIMIT 1
    """)
    suspend fun getConviteAnteriorNaoPendente(equipaId: String, convidadoId: String): ConviteEntity?

    /** Conta os convites pendentes de uma equipa — útil para o limiar de jogadores. */
    @Query("SELECT COUNT(*) FROM convite WHERE equipa_id = :equipaId AND estado = 'pendente'")
    fun countConvitesPendentes(equipaId: String): Flow<Int>

    /** Convites recebidos por um utilizador (para o ecrã de Notificações). */
    @Query("SELECT * FROM convite WHERE convidado_id = :convidadoId ORDER BY data_expiracao DESC")
    fun getConvitesPorConvidado(convidadoId: String): Flow<List<ConviteEntity>>

    /** Convites pendentes recebidos por um utilizador — para aceitar/recusar. */
    @Query("SELECT * FROM convite WHERE convidado_id = :convidadoId AND estado = 'pendente' ORDER BY data_expiracao ASC")
    fun getConvitesPendentesPorConvidado(convidadoId: String): Flow<List<ConviteEntity>>

    @Query("SELECT * FROM convite WHERE convidado_email = :email")
    fun getConvitesByEmail(email: String): Flow<List<ConviteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(convite: ConviteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(convites: List<ConviteEntity>)

    @Update
    suspend fun update(convite: ConviteEntity)

    @Delete
    suspend fun delete(convite: ConviteEntity)

    @Query("DELETE FROM convite WHERE equipa_id = :equipaId")
    suspend fun deleteByEquipa(equipaId: String)

    @Query("DELETE FROM convite WHERE id = :id")
    suspend fun deleteById(id: String)
}
