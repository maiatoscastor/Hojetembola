package com.hojetembola.app.data.repository

import com.hojetembola.app.data.local.dao.EquipaDao
import com.hojetembola.app.data.local.dao.EventoJogoDao
import com.hojetembola.app.data.local.dao.JogoDao
import com.hojetembola.app.data.local.dao.TorneioDao
import com.hojetembola.app.data.local.entity.JogoEntity
import com.hojetembola.app.data.local.entity.TorneioEntity
import com.hojetembola.app.data.local.entity.UtilizadorEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepository @Inject constructor(
    private val userRepository: UserRepository,
    private val torneioDao: TorneioDao,
    private val jogoDao: JogoDao,
    private val eventoJogoDao: EventoJogoDao,
    private val equipaDao: EquipaDao
) {
    suspend fun getCurrentUser(): Result<UtilizadorEntity> =
        userRepository.getCurrentUser()

    fun getMeusTorneios(userId: String): Flow<List<TorneioEntity>> =
        torneioDao.getMeusTorneios(userId)

    fun getTorneiosByOrganizador(userId: String): Flow<List<TorneioEntity>> =
        torneioDao.getByOrganizador(userId)

    fun getJogosAoVivo(): Flow<List<JogoEntity>> =
        jogoDao.getAoVivo()

    fun getProximosJogos(): Flow<List<JogoEntity>> =
        jogoDao.getProximosAgendados()

    suspend fun getTotalGolos(userId: String): Int = try {
        eventoJogoDao.countTotalGolosByJogador(userId)
    } catch (_: Exception) { 0 }

    suspend fun getEquipaNome(id: String): String =
        equipaDao.getById(id)?.nome ?: "—"
}
