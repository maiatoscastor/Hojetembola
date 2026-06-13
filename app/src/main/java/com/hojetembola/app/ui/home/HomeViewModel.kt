package com.hojetembola.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hojetembola.app.data.local.entity.TorneioEntity
import com.hojetembola.app.data.local.entity.UtilizadorEntity
import com.hojetembola.app.data.repository.ConviteRepository
import com.hojetembola.app.data.repository.HomeRepository
import com.hojetembola.app.data.repository.NotificacaoRepository
import com.hojetembola.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Modelo auxiliar ───────────────────────────────────────────────────────────

data class JogoInfo(
    val jogoId: String,
    val torneioId: String,
    val equipaCasaNome: String,
    val equipaVisitanteNome: String,
    val golosCasa: Int,
    val golosVisitante: Int,
    val minutoAtual: Int?,
    val local: String,
    val estado: String,
    val dataHora: String
)

// ── Estados da UI ─────────────────────────────────────────────────────────────

sealed class HomeUiState {
    object Loading : HomeUiState()

    data class JogadorState(
        val utilizador: UtilizadorEntity,
        val totalTorneios: Int,
        val torneiosAtivos: Int,
        val totalGolos: Int,
        val jogoAoVivo: JogoInfo?,
        val proximoJogo: JogoInfo?,
        val meusTorneios: List<TorneioEntity>
    ) : HomeUiState()

    data class OrganizadorState(
        val utilizador: UtilizadorEntity,
        val torneiosAtivos: Int,
        val torneiosComInscricoes: Int,
        val torneiosGeridos: List<TorneioEntity>,
        val jogoAoVivo: JogoInfo?
    ) : HomeUiState()

    data class Error(val message: String) : HomeUiState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val notificacaoRepository: NotificacaoRepository,
    private val conviteRepository: ConviteRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Número de notificações não lidas + convites pendentes — alimenta o badge do sino. */
    private val _countNaoLidas = MutableStateFlow(0)
    val countNaoLidas: StateFlow<Int> = _countNaoLidas.asStateFlow()

    init {
        load()
        observeBadge()
    }

    private fun observeBadge() {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser().getOrNull() ?: return@launch
            combine(
                notificacaoRepository.countNaoLidas(user.id),
                conviteRepository.getConvitesPendentesPorConvidado(user.id)
            ) { naoLidas, convites ->
                naoLidas + convites.size
            }.collect { _countNaoLidas.value = it }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            val userResult = homeRepository.getCurrentUser()
            userResult.onFailure {
                _uiState.value = HomeUiState.Error(it.message ?: "Erro desconhecido")
                return@launch
            }
            val user = userResult.getOrThrow()

            if (user.perfil == "organizador") {
                combine(
                    homeRepository.getTorneiosByOrganizador(user.id),
                    homeRepository.getJogosAoVivo()
                ) { torneios, jogosVivo ->
                    val jogoVivo = jogosVivo.firstOrNull()?.let { j ->
                        JogoInfo(
                            jogoId = j.id,
                            torneioId = j.torneioId,
                            equipaCasaNome = homeRepository.getEquipaNome(j.equipaCasaId),
                            equipaVisitanteNome = homeRepository.getEquipaNome(j.equipaVisitanteId),
                            golosCasa = j.golosCasa ?: 0,
                            golosVisitante = j.golosVisitante ?: 0,
                            minutoAtual = j.minutoAtual,
                            local = j.local,
                            estado = j.estado,
                            dataHora = j.dataHora
                        )
                    }
                    HomeUiState.OrganizadorState(
                        utilizador = user,
                        torneiosAtivos = torneios.count { it.estado.lowercase().replace("_","") in setOf("adecorrer", "inscricoesabertas") },
                        torneiosComInscricoes = torneios.count { it.estado.lowercase().replace("_","") == "inscricoesabertas" },
                        torneiosGeridos = torneios,
                        jogoAoVivo = jogoVivo
                    )
                }.collect { _uiState.value = it }
            } else {
                val totalGolos = homeRepository.getTotalGolos(user.id)
                combine(
                    homeRepository.getTorneiosComoJogador(user.id),
                    homeRepository.getJogosAoVivo(),
                    homeRepository.getProximosJogosByJogador(user.id)
                ) { torneios, jogosVivo, proximosJogos ->
                    val jogoVivo = jogosVivo.firstOrNull()?.let { j ->
                        JogoInfo(
                            jogoId = j.id,
                            torneioId = j.torneioId,
                            equipaCasaNome = homeRepository.getEquipaNome(j.equipaCasaId),
                            equipaVisitanteNome = homeRepository.getEquipaNome(j.equipaVisitanteId),
                            golosCasa = j.golosCasa ?: 0,
                            golosVisitante = j.golosVisitante ?: 0,
                            minutoAtual = j.minutoAtual,
                            local = j.local,
                            estado = j.estado,
                            dataHora = j.dataHora
                        )
                    }
                    val proximoJogo = proximosJogos.firstOrNull()?.let { j ->
                        JogoInfo(
                            jogoId = j.id,
                            torneioId = j.torneioId,
                            equipaCasaNome = homeRepository.getEquipaNome(j.equipaCasaId),
                            equipaVisitanteNome = homeRepository.getEquipaNome(j.equipaVisitanteId),
                            golosCasa = j.golosCasa ?: 0,
                            golosVisitante = j.golosVisitante ?: 0,
                            minutoAtual = j.minutoAtual,
                            local = j.local,
                            estado = j.estado,
                            dataHora = j.dataHora
                        )
                    }
                    HomeUiState.JogadorState(
                        utilizador = user,
                        totalTorneios = torneios.size,
                        torneiosAtivos = torneios.count { it.estado.lowercase().replace("_","") == "adecorrer" },
                        totalGolos = totalGolos,
                        jogoAoVivo = jogoVivo,
                        proximoJogo = proximoJogo,
                        meusTorneios = torneios
                    )
                }.collect { _uiState.value = it }
            }
        }
    }
}
