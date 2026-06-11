package com.hojetembola.app.ui.torneios

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hojetembola.app.data.local.dao.EventoComNome
import com.hojetembola.app.data.local.dao.InscricaoEquipaDao
import com.hojetembola.app.data.local.dao.JogadorInscricaoComNome
import com.hojetembola.app.data.local.dao.JogadorInscricaoDao
import com.hojetembola.app.data.local.entity.JogoEntity
import com.hojetembola.app.data.repository.JogoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JogoDetalheUiState(
    val loading: Boolean = true,
    val jogo: JogoEntity? = null,
    val casaNome: String = "",
    val casaIniciais: String = "",
    val casaCor: String = "#3D5A80",
    val visitanteNome: String = "",
    val visitanteIniciais: String = "",
    val visitanteCor: String = "#3D5A80",
    val eventos: List<EventoComNome> = emptyList(),
    val casaJogadores: List<JogadorInscricaoComNome> = emptyList(),
    val visitanteJogadores: List<JogadorInscricaoComNome> = emptyList(),
    val erro: String? = null
)

sealed class JogoDetalheAcao {
    object Idle : JogoDetalheAcao()
    object Loading : JogoDetalheAcao()
    data class Sucesso(val msg: String) : JogoDetalheAcao()
    data class Erro(val msg: String) : JogoDetalheAcao()
}

@HiltViewModel
class JogoDetalheViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val jogoRepository: JogoRepository,
    private val inscricaoEquipaDao: InscricaoEquipaDao,
    private val jogadorInscricaoDao: JogadorInscricaoDao
) : ViewModel() {

    val jogoId: String = checkNotNull(savedStateHandle["jogoId"])
    val torneioId: String = checkNotNull(savedStateHandle["torneioId"])
    val isOrganizador: Boolean = savedStateHandle["isOrganizador"] ?: false

    private val _uiState = MutableStateFlow(JogoDetalheUiState())
    val uiState: StateFlow<JogoDetalheUiState> = _uiState.asStateFlow()

    private val _acao = MutableStateFlow<JogoDetalheAcao>(JogoDetalheAcao.Idle)
    val acao: StateFlow<JogoDetalheAcao> = _acao.asStateFlow()

    /** Minuto ao vivo gerido por timer local no ViewModel. */
    private val _minutoVivo = MutableStateFlow<Int?>(null)
    val minutoVivo: StateFlow<Int?> = _minutoVivo.asStateFlow()

    private var timerJob: Job? = null

    init { load() }

    private fun load() {
        viewModelScope.launch {
            jogoRepository.syncEventos(jogoId)

            // Carrega jogadores das duas equipas (uma só vez — não mudam durante o jogo)
            val jogoInit = jogoRepository.getJogoById(jogoId)
            val casaJogadores    = loadJogadores(jogoInit?.equipaCasaId)
            val visitanteJogadores = loadJogadores(jogoInit?.equipaVisitanteId)

            // Se o jogo já estava ao vivo (restart da app), retoma o timer
            if (jogoInit?.estado == "ao_vivo") startTimer(jogoInit.minutoAtual ?: 0)

            combine(
                jogoRepository.getJogosComEquipas(torneioId),
                jogoRepository.getEventosComNome(jogoId)
            ) { jogos, eventos ->
                val jogoData   = jogos.find { it.jogoId == jogoId }
                val jogoEntity = jogoRepository.getJogoById(jogoId)
                _uiState.value = JogoDetalheUiState(
                    loading            = false,
                    jogo               = jogoEntity,
                    casaNome           = jogoData?.casaNome ?: "",
                    casaIniciais       = jogoData?.casaIniciais ?: "?",
                    casaCor            = jogoData?.casaCor ?: "#3D5A80",
                    visitanteNome      = jogoData?.visitanteNome ?: "",
                    visitanteIniciais  = jogoData?.visitanteIniciais ?: "?",
                    visitanteCor       = jogoData?.visitanteCor ?: "#3D5A80",
                    eventos            = eventos,
                    casaJogadores      = casaJogadores,
                    visitanteJogadores = visitanteJogadores
                )
            }.collect {}
        }
    }

    private suspend fun loadJogadores(equipaId: String?): List<JogadorInscricaoComNome> {
        equipaId ?: return emptyList()
        val inscricao = inscricaoEquipaDao.getByTorneioAndEquipa(torneioId, equipaId) ?: return emptyList()
        return jogadorInscricaoDao.getComNomeSuspend(inscricao.id)
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    private fun startTimer(fromMinuto: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var m = fromMinuto
            _minutoVivo.value = m
            while (true) {
                delay(60_000L)
                m++
                _minutoVivo.value = m
                jogoRepository.atualizarMinuto(jogoId, m)   // persiste em Room para sobreviver a restarts
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _minutoVivo.value = null
    }

    // ── Ações ─────────────────────────────────────────────────────────────────

    fun iniciarJogo() {
        viewModelScope.launch {
            _acao.value = JogoDetalheAcao.Loading
            jogoRepository.iniciarJogo(jogoId)
                .onSuccess {
                    startTimer(0)
                    _acao.value = JogoDetalheAcao.Sucesso("Jogo iniciado!")
                }
                .onFailure { _acao.value = JogoDetalheAcao.Erro(it.message ?: "Erro") }
        }
    }

    fun terminarJogo() {
        viewModelScope.launch {
            _acao.value = JogoDetalheAcao.Loading
            stopTimer()
            val jogo = _uiState.value.jogo
            jogoRepository.terminarJogo(jogoId, jogo?.golosCasa ?: 0, jogo?.golosVisitante ?: 0)
                .onSuccess {
                    jogoRepository.recalcularClassificacao(torneioId)
                    _acao.value = JogoDetalheAcao.Sucesso("Jogo terminado!")
                }
                .onFailure { _acao.value = JogoDetalheAcao.Erro(it.message ?: "Erro") }
        }
    }

    fun registarEvento(
        tipo: String,
        minuto: Int,
        equipaId: String?,
        jogadorId: String? = null,
        jogadorSaiId: String? = null,
        jogadorEntraId: String? = null
    ) {
        viewModelScope.launch {
            _acao.value = JogoDetalheAcao.Loading
            jogoRepository.registarEvento(jogoId, tipo, minuto, equipaId, jogadorId, jogadorSaiId, jogadorEntraId)
                .onSuccess { _acao.value = JogoDetalheAcao.Sucesso("Evento registado!") }
                .onFailure { _acao.value = JogoDetalheAcao.Erro(it.message ?: "Erro") }
        }
    }

    fun resetAcao() { _acao.value = JogoDetalheAcao.Idle }
}
