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

    @Query("SELECT * FROM convite WHERE equipa_id = :equipaId AND estado = 'pendente'")
    fun getConvitesPendentes(equipaId: String): Flow<List<ConviteEntity>>

    @Query("SELECT * FROM convite WHERE convidado_email = :email")
    fun getConvitesByEmail(email: String): Flow<List<ConviteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(convite: ConviteEntity)

    @Update
    suspend fun update(convite: ConviteEntity)

    @Delete
    suspend fun delete(convite: ConviteEntity)

    @Query("DELETE FROM convite WHERE equipa_id = :equipaId")
    suspend fun deleteByEquipa(equipaId: String)
}
