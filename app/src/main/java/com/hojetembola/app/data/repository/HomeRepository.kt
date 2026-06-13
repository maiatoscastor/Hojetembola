package com.hojetembola.app.data.repository

import com.hojetembola.app.data.local.dao.ConvocatoriaJogoDao
import com.hojetembola.app.data.local.dao.EquipaDao
import com.hojetembola.app.data.local.dao.EventoJogoDao
import com.hojetembola.app.data.local.dao.JogoDao
import com.hojetembola.app.data.local.dao.TorneioDao
import com.hojetembola.app.data.local.dao.VotoMvpDao
import com.hojetembola.app.data.local.entity.JogoEntity
import com.hojetembola.app.data.local.entity.TorneioEntity
import com.hojetembola.app.data.local.entity.UtilizadorEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

data class PerfilStats(
    val golos: Int = 0,
    val jogos: Int = 0,
    val mvps: Int = 0,
    val assistencias: Int = 0,
    val amarelos: Int = 0
)

@Singleton
class HomeRepository @Inject constructor(
    private val userRepository: UserRepository,
    private val torneioDao: TorneioDao,
    private val jogoDao: JogoDao,
    private val eventoJogoDao: EventoJogoDao,
    private val equipaDao: EquipaDao,
    private val votoMvpDao: VotoMvpDao,
    private val convocatoriaJogoDao: ConvocatoriaJogoDao
) {
    suspend fun getCurrentUser(): Result<UtilizadorEntity> =
        userRepository.getCurrentUser()

    fun getMeusTorneios(userId: String): Flow<List<TorneioEntity>> =
        torneioDao.getMeusTorneios(userId)

    fun getTorneiosComoJogador(userId: String): Flow<List<TorneioEntity>> =
        torneioDao.getTorneiosComoJogador(userId)

    fun getTorneiosVencidos(userId: String): Flow<List<TorneioEntity>> =
        torneioDao.getTorneiosVencidosByJogador(userId)

    fun getTorneiosByOrganizador(userId: String): Flow<List<TorneioEntity>> =
        torneioDao.getByOrganizador(userId)

    fun getJogosAoVivo(): Flow<List<JogoEntity>> =
        jogoDao.getAoVivo()

    fun getProximosJogos(): Flow<List<JogoEntity>> =
        jogoDao.getProximosAgendados()

    fun getProximosJogosByJogador(userId: String): Flow<List<JogoEntity>> =
        jogoDao.getProximosAgendadosByJogador(userId)

    suspend fun getTotalGolos(userId: String): Int = try {
        eventoJogoDao.countTotalGolosByJogador(userId)
    } catch (_: Exception) { 0 }

    suspend fun getEquipaNome(id: String): String =
        equipaDao.getById(id)?.nome ?: "—"

    suspend fun getPerfilStats(userId: String): PerfilStats = try {
        PerfilStats(
            golos       = eventoJogoDao.countTotalGolosByJogador(userId),
            jogos       = convocatoriaJogoDao.countJogosDisputadosByJogador(userId),
            mvps        = votoMvpDao.countMvpsRecebidosByJogador(userId),
            assistencias = eventoJogoDao.countTotalAssistenciasByJogador(userId),
            amarelos    = eventoJogoDao.countTotalAmarelosByJogador(userId)
        )
    } catch (_: Exception) { PerfilStats() }
}
