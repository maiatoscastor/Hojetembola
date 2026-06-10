package com.hojetembola.app.ui.torneios

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hojetembola.app.data.local.entity.InscricaoComEquipa
import com.hojetembola.app.data.repository.EquipaRepository
import com.hojetembola.app.data.repository.JogoRepository
import com.hojetembola.app.data.repository.TorneioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class GerirTorneioUiState {
    object Loading : GerirTorneioUiState()
    data class Error(val message: String) : GerirTorneioUiState()
    data class Content(
        val torneioNome: String,
        val inscricoes: List<InscricaoComEquipa>,
        val numPendentes: Int,
        val numConfirmadas: Int,
        val temCalendario: Boolean
    ) : GerirTorneioUiState()
}

sealed class GerirTorneioAcao {
    object Idle : GerirTorneioAcao()
    object Loading : GerirTorneioAcao()
    data class Sucesso(val message: String) : GerirTorneioAcao()
    data class Erro(val message: String) : GerirTorneioAcao()
}

@HiltViewModel
class GerirTorneioViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val equipaRepository: EquipaRepository,
    private val torneioRepository: TorneioRepository,
    private val jogoRepository: JogoRepository
) : ViewModel() {

    val torneioId: String = checkNotNull(savedStateHandle["torneioId"])

    private val _uiState = MutableStateFlow<GerirTorneioUiState>(GerirTorneioUiState.Loading)
    val uiState: StateFlow<GerirTorneioUiState> = _uiState.asStateFlow()

    private val _acao = MutableStateFlow<GerirTorneioAcao>(GerirTorneioAcao.Idle)
    val acao: StateFlow<GerirTorneioAcao> = _acao.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val torneio = torneioRepository.getTorneioByIdSuspend(torneioId)
            if (torneio == null) {
                _uiState.value = GerirTorneioUiState.Error("Torneio não encontrado.")
                return@launch
            }

            equipaRepository.syncInscricoesComEquipas(torneioId)

            equipaRepository.getInscricoesComEquipa(torneioId)
                .catch { e -> _uiState.value = GerirTorneioUiState.Error(e.message ?: "Erro") }
                .collect { inscricoes ->
                    _uiState.value = GerirTorneioUiState.Content(
                        torneioNome    = torneio.nome,
                        inscricoes     = inscricoes,
                        numPendentes   = inscricoes.count { it.estado == "Pendente" },
                        numConfirmadas = inscricoes.count { it.estado == "Confirmada" },
                        temCalendario  = jogoRepository.temCalendario(torneioId)
                    )
                }
        }
    }

    fun aceitar(inscricaoId: String) {
        viewModelScope.launch {
            _acao.value = GerirTorneioAcao.Loading
            equipaRepository.aceitarInscricao(inscricaoId)
                .onSuccess { _acao.value = GerirTorneioAcao.Sucesso("Equipa confirmada.") }
                .onFailure { e -> _acao.value = GerirTorneioAcao.Erro(e.message ?: "Erro") }
        }
    }

    fun rejeitar(inscricaoId: String) {
        viewModelScope.launch {
            _acao.value = GerirTorneioAcao.Loading
            equipaRepository.rejeitarInscricao(inscricaoId)
                .onSuccess { _acao.value = GerirTorneioAcao.Sucesso("Equipa rejeitada.") }
                .onFailure { e -> _acao.value = GerirTorneioAcao.Erro(e.message ?: "Erro") }
        }
    }

    fun confirmarTodas() {
        viewModelScope.launch {
            _acao.value = GerirTorneioAcao.Loading
            equipaRepository.confirmarTodasPendentes(torneioId)
                .onSuccess { count ->
                    _acao.value = GerirTorneioAcao.Sucesso("$count equipa(s) confirmada(s).")
                }
                .onFailure { e -> _acao.value = GerirTorneioAcao.Erro(e.message ?: "Erro") }
        }
    }

    fun gerarCalendario() {
        viewModelScope.launch {
            _acao.value = GerirTorneioAcao.Loading
            jogoRepository.gerarCalendario(torneioId)
                .onSuccess { count ->
                    _acao.value = GerirTorneioAcao.Sucesso("Calendário gerado: $count jogos criados.")
                }
                .onFailure { e ->
                    _acao.value = GerirTorneioAcao.Erro(e.message ?: "Erro ao gerar calendário.")
                }
        }
    }

    fun resetAcao() { _acao.value = GerirTorneioAcao.Idle }
}
