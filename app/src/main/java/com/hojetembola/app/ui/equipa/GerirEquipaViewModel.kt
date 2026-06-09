package com.hojetembola.app.ui.equipa

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hojetembola.app.data.local.entity.MembroComNome
import com.hojetembola.app.data.local.entity.UtilizadorEntity
import com.hojetembola.app.data.repository.EquipaRepository
import com.hojetembola.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class GerirEquipaUiState {
    object Loading : GerirEquipaUiState()
    data class Content(
        val membros: List<MembroComNome>,
        val podeContinuar: Boolean   // count dentro de [min, max]
    ) : GerirEquipaUiState()
    data class Error(val message: String) : GerirEquipaUiState()
}

sealed class GerirEquipaAcao {
    object Idle : GerirEquipaAcao()
    object Loading : GerirEquipaAcao()
    data class Sucesso(val mensagem: String) : GerirEquipaAcao()
    data class Erro(val mensagem: String) : GerirEquipaAcao()
    /** Inscrição concluída com sucesso — fechar todas as telas e mostrar confirmação. */
    data class InscricaoConcluida(val equipaNome: String) : GerirEquipaAcao()
}

@HiltViewModel
class GerirEquipaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val equipaRepository: EquipaRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val equipaId: String  = checkNotNull(savedStateHandle["equipaId"])
    val equipaNome: String = savedStateHandle["equipaNome"] ?: ""

    /** Presente quando este ecrã deve inscrever a equipa após adicionar jogadores. */
    val torneioId: String? = savedStateHandle["torneioId"]
    val minJogadores: Int  = savedStateHandle["minJogadores"] ?: 0
    val maxJogadores: Int  = savedStateHandle["maxJogadores"] ?: 99

    private val _uiState = MutableStateFlow<GerirEquipaUiState>(GerirEquipaUiState.Loading)
    val uiState: StateFlow<GerirEquipaUiState> = _uiState.asStateFlow()

    private val _acao = MutableStateFlow<GerirEquipaAcao>(GerirEquipaAcao.Idle)
    val acao: StateFlow<GerirEquipaAcao> = _acao.asStateFlow()

    private val _resultadosPesquisa = MutableStateFlow<List<UtilizadorEntity>>(emptyList())
    val resultadosPesquisa: StateFlow<List<UtilizadorEntity>> = _resultadosPesquisa.asStateFlow()

    private val _isPesquisando = MutableStateFlow(false)
    val isPesquisando: StateFlow<Boolean> = _isPesquisando.asStateFlow()

    private var membrosJob: Job? = null
    private var capitaoId: String = ""

    init { loadMembros() }

    fun loadMembros() {
        membrosJob?.cancel()
        membrosJob = viewModelScope.launch {
            // Busca o id do utilizador actual (para não se poder remover a si próprio da lista)
            userRepository.getCurrentUser().onSuccess { capitaoId = it.id }

            equipaRepository.syncMembros(equipaId)

            equipaRepository.getMembrosComNome(equipaId).collect { membros ->
                val count = membros.size
                _uiState.value = GerirEquipaUiState.Content(
                    membros      = membros,
                    podeContinuar = count in minJogadores..maxJogadores
                )
            }
        }
    }

    fun pesquisar(query: String) {
        if (query.length < 2) { _resultadosPesquisa.value = emptyList(); return }
        viewModelScope.launch {
            _isPesquisando.value = true
            equipaRepository.pesquisarUtilizadores(query)
                .onSuccess { _resultadosPesquisa.value = it }
                .onFailure { _resultadosPesquisa.value = emptyList() }
            _isPesquisando.value = false
        }
    }

    fun limparPesquisa() { _resultadosPesquisa.value = emptyList() }

    fun adicionarJogador(utilizadorId: String) {
        viewModelScope.launch {
            equipaRepository.adicionarMembro(equipaId, utilizadorId)
                .onSuccess  { limparPesquisa() }
                .onFailure  { e -> _acao.value = GerirEquipaAcao.Erro(e.message ?: "Erro ao adicionar.") }
        }
    }

    fun removerJogador(utilizadorId: String) {
        if (utilizadorId == capitaoId) {
            _acao.value = GerirEquipaAcao.Erro("Não podes remover o capitão da equipa.")
            return
        }
        viewModelScope.launch {
            equipaRepository.removerMembro(equipaId, utilizadorId)
                .onFailure { e -> _acao.value = GerirEquipaAcao.Erro(e.message ?: "Erro ao remover.") }
        }
    }

    /** Inscreve a equipa no torneio (só disponível quando torneioId != null e count está ok). */
    fun inscreverEquipa() {
        val tid = torneioId ?: return
        viewModelScope.launch {
            _acao.value = GerirEquipaAcao.Loading
            val utilizador = userRepository.getCurrentUser().getOrElse {
                _acao.value = GerirEquipaAcao.Erro("Sessão expirada.")
                return@launch
            }
            equipaRepository.inscreverEquipa(tid, equipaId, utilizador.id)
                .onSuccess { _acao.value = GerirEquipaAcao.InscricaoConcluida(equipaNome) }
                .onFailure { e -> _acao.value = GerirEquipaAcao.Erro(e.message ?: "Erro ao inscrever.") }
        }
    }

    fun resetAcao() { _acao.value = GerirEquipaAcao.Idle }
}
