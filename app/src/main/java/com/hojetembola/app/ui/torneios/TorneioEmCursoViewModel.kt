package com.hojetembola.app.ui.torneios

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hojetembola.app.data.local.dao.EquipaDao
import com.hojetembola.app.data.local.dao.EventoJogoDao
import com.hojetembola.app.data.local.dao.JogadorInscricaoDao
import com.hojetembola.app.data.local.dao.UtilizadorDao
import com.hojetembola.app.data.local.entity.InscricaoComEquipa
import com.hojetembola.app.data.local.entity.TorneioEntity
import com.hojetembola.app.data.repository.EquipaRepository
import com.hojetembola.app.data.repository.JogoRepository
import com.hojetembola.app.data.repository.TorneioRepository
import com.hojetembola.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JogadorComGolos(
    val nome: String,
    val golosTorneio: Int,
    val assistenciasTorneio: Int = 0
)

data class TorneioEmCursoUiState(
    val loading: Boolean = true,
    val torneio: TorneioEntity? = null,
    val isOrganizador: Boolean = false,
    val jornadas: List<JornadaComJogos> = emptyList(),
    val classificacao: List<ClassificacaoTorneioRow> = emptyList(),
    val equipas: List<InscricaoComEquipa> = emptyList(),
    /** Mapa equipaId → lista de jogadores inscritos com golos no torneio */
    val jogadoresPorEquipa: Map<String, List<JogadorComGolos>> = emptyMap()
)

@HiltViewModel
class TorneioEmCursoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val torneioRepository: TorneioRepository,
    private val jogoRepository: JogoRepository,
    private val equipaRepository: EquipaRepository,
    private val userRepository: UserRepository,
    private val equipaDao: EquipaDao,
    private val jogadorInscricaoDao: JogadorInscricaoDao,
    private val utilizadorDao: UtilizadorDao,
    private val eventoJogoDao: EventoJogoDao
) : ViewModel() {

    val torneioId: String = checkNotNull(savedStateHandle["torneioId"])

    private val _uiState = MutableStateFlow(TorneioEmCursoUiState())
    val uiState: StateFlow<TorneioEmCursoUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            jogoRepository.syncJogosETorneio(torneioId)
            equipaRepository.syncInscricoesComEquipas(torneioId)
            jogoRepository.syncClassificacao(torneioId)
            jogoRepository.recalcularClassificacao(torneioId)

            val currentUserId = userRepository.getCurrentUser().getOrNull()?.id

            combine(
                torneioRepository.getTorneioById(torneioId),
                jogoRepository.getJornadasFlow(torneioId),
                jogoRepository.getJogosComEquipas(torneioId),
                jogoRepository.getClassificacao(torneioId),
                equipaRepository.getInscricoesComEquipa(torneioId)
            ) { torneio, jornadas, jogos, classificacao, inscricoes ->
                val confirmedInscricoes = inscricoes.filter { it.estado == "Confirmada" }

                val classificacaoRows = classificacao.mapNotNull { entry ->
                    val equipa = equipaDao.getById(entry.equipaId) ?: return@mapNotNull null
                    ClassificacaoTorneioRow(
                        posicao = entry.posicao,
                        equipaNome = equipa.nome,
                        equipaIniciais = equipa.iniciais,
                        equipaCor = equipa.corAvatar,
                        jogos = entry.jogos,
                        vitorias = entry.vitorias,
                        empates = entry.empates,
                        derrotas = entry.derrotas,
                        golosMarcados = entry.golosMarcados,
                        golosSofridos = entry.golosSofridos,
                        pontos = entry.pontos
                    )
                }

                val jornadasComJogos = jornadas.map { jornada ->
                    JornadaComJogos(jornada, jogos.filter { it.jornadaId == jornada.id })
                }

                // Carrega jogadores + golos para cada equipa confirmada
                val jogadoresPorEquipa = mutableMapOf<String, List<JogadorComGolos>>()
                for (insc in confirmedInscricoes) {
                    val jogadoresInscricao = jogadorInscricaoDao.getByInscricaoSuspend(insc.id)
                    val jogadoresComGolos = jogadoresInscricao.mapNotNull { ji ->
                        val utilizador = utilizadorDao.getById(ji.utilizadorId) ?: return@mapNotNull null
                        val golos = eventoJogoDao.countGolosByJogadorTorneio(ji.utilizadorId, torneioId)
                        val assists = eventoJogoDao.countAssistenciasByJogadorTorneio(ji.utilizadorId, torneioId)
                        JogadorComGolos(nome = utilizador.nome, golosTorneio = golos, assistenciasTorneio = assists)
                    }.sortedByDescending { it.golosTorneio }
                    jogadoresPorEquipa[insc.equipaId] = jogadoresComGolos
                }

                TorneioEmCursoUiState(
                    loading = false,
                    torneio = torneio,
                    isOrganizador = torneio?.organizadorId == currentUserId,
                    jornadas = jornadasComJogos,
                    classificacao = classificacaoRows,
                    equipas = confirmedInscricoes,
                    jogadoresPorEquipa = jogadoresPorEquipa
                )
            }.catch {
                _uiState.value = TorneioEmCursoUiState(loading = false)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
