package com.hojetembola.app.ui.equipa

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hojetembola.app.data.local.entity.ConviteEntity
import com.hojetembola.app.data.local.entity.EquipaEntity
import com.hojetembola.app.data.local.entity.MembroComNome
import com.hojetembola.app.data.local.entity.UtilizadorEntity
import com.hojetembola.app.data.repository.ConviteRepository
import com.hojetembola.app.data.repository.EquipaRepository
import com.hojetembola.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class EstadoNaEquipa { DISPONIVEL, JA_CONVIDADO, JA_MEMBRO, NO_TORNEIO }

data class UtilizadorComEstado(
    val utilizador: UtilizadorEntity,
    val estado: EstadoNaEquipa
)

sealed class GerirEquipaUiState {
    object Loading : GerirEquipaUiState()
    data class Content(
        val membros: List<MembroComNome>,
        val convitesPendentes: List<ConviteEntity>,
        val podeContinuar: Boolean   // membros confirmados dentro de [min, max]
    ) : GerirEquipaUiState()
    data class Error(val message: String) : GerirEquipaUiState()
}

sealed class EditarEquipaState {
    object Idle : EditarEquipaState()
    object Loading : EditarEquipaState()
    object Success : EditarEquipaState()
    data class Error(val message: String) : EditarEquipaState()
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
    private val conviteRepository: ConviteRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val equipaId: String   = checkNotNull(savedStateHandle["equipaId"])
    val equipaNome: String = savedStateHandle["equipaNome"] ?: ""

    /** Presente quando este ecrã deve inscrever a equipa após gerir jogadores. */
    val torneioId: String? = savedStateHandle["torneioId"]
    val minJogadores: Int  = savedStateHandle["minJogadores"] ?: 0
    val maxJogadores: Int  = savedStateHandle["maxJogadores"] ?: 99

    private val _uiState = MutableStateFlow<GerirEquipaUiState>(GerirEquipaUiState.Loading)
    val uiState: StateFlow<GerirEquipaUiState> = _uiState.asStateFlow()

    private val _acao = MutableStateFlow<GerirEquipaAcao>(GerirEquipaAcao.Idle)
    val acao: StateFlow<GerirEquipaAcao> = _acao.asStateFlow()

    private val _resultadosPesquisa = MutableStateFlow<List<UtilizadorComEstado>>(emptyList())
    val resultadosPesquisa: StateFlow<List<UtilizadorComEstado>> = _resultadosPesquisa.asStateFlow()

    /** IDs de utilizadores já inscritos no torneio por outras equipas (só carregado quando torneioId != null). */
    private val _emConflito = MutableStateFlow<Set<String>>(emptySet())

    private val _isPesquisando = MutableStateFlow(false)
    val isPesquisando: StateFlow<Boolean> = _isPesquisando.asStateFlow()

    private val _equipaInfo = MutableStateFlow<EquipaEntity?>(null)
    val equipaInfo: StateFlow<EquipaEntity?> = _equipaInfo.asStateFlow()

    private val _isCapitao = MutableStateFlow(false)
    val isCapitao: StateFlow<Boolean> = _isCapitao.asStateFlow()

    private val _editarState = MutableStateFlow<EditarEquipaState>(EditarEquipaState.Idle)
    val editarState: StateFlow<EditarEquipaState> = _editarState.asStateFlow()

    private var membrosJob: Job? = null
    private var capitaoId: String = ""

    init { loadMembros() }

    fun loadMembros() {
        membrosJob?.cancel()
        membrosJob = viewModelScope.launch {
            // Busca o id do utilizador actual e info da equipa
            userRepository.getCurrentUser().onSuccess { capitaoId = it.id }
            val entity = equipaRepository.getEquipaById(equipaId)
            _equipaInfo.value = entity
            _isCapitao.value = entity?.capitaoId == capitaoId

            // Sincroniza membros e convites do servidor
            equipaRepository.syncMembros(equipaId)
            conviteRepository.syncConvitesEnviados(equipaId)

            // Carrega IDs de jogadores já inscritos no torneio por outras equipas
            if (!torneioId.isNullOrEmpty()) {
                _emConflito.value =
                    equipaRepository.getUtilizadoresNoTorneio(torneioId, equipaId).toSet()
            }

            // Combina membros confirmados + convites pendentes num único estado
            combine(
                equipaRepository.getMembrosComNome(equipaId),
                conviteRepository.getConvitesPendentes(equipaId)
            ) { membros, convites ->
                GerirEquipaUiState.Content(
                    membros           = membros,
                    convitesPendentes = convites,
                    // Só membros confirmados contam para o mínimo (convites pendentes não garantem presença)
                    podeContinuar     = membros.size in minJogadores..maxJogadores
                )
            }.collect { _uiState.value = it }
        }
    }

    fun pesquisar(query: String) {
        if (query.length < 2) { _resultadosPesquisa.value = emptyList(); return }
        viewModelScope.launch {
            _isPesquisando.value = true
            equipaRepository.pesquisarUtilizadores(query)
                .onSuccess { todos ->
                    val state        = _uiState.value as? GerirEquipaUiState.Content
                    val membrosIds   = state?.membros?.map { it.utilizadorId }?.toSet() ?: emptySet()
                    val convitesIds  = state?.convitesPendentes?.map { it.convidadoId }?.toSet() ?: emptySet()
                    val conflitosIds = _emConflito.value

                    _resultadosPesquisa.value = todos
                        .filter { it.id != capitaoId }   // só esconde o próprio capitão
                        .map { u ->
                            val estado = when (u.id) {
                                in conflitosIds -> EstadoNaEquipa.NO_TORNEIO   // já noutras equipas deste torneio
                                in membrosIds   -> EstadoNaEquipa.JA_MEMBRO
                                in convitesIds  -> EstadoNaEquipa.JA_CONVIDADO
                                else            -> EstadoNaEquipa.DISPONIVEL
                            }
                            UtilizadorComEstado(u, estado)
                        }
                }
                .onFailure { _resultadosPesquisa.value = emptyList() }
            _isPesquisando.value = false
        }
    }

    fun limparPesquisa() { _resultadosPesquisa.value = emptyList<UtilizadorComEstado>() }

    /**
     * Envia um convite ao utilizador em vez de o adicionar directamente.
     * O jogador só passa a membro após aceitar.
     */
    fun convidarJogador(utilizadorId: String) {
        if (!_isCapitao.value) {
            _acao.value = GerirEquipaAcao.Erro("Só o capitão pode convidar jogadores.")
            return
        }
        viewModelScope.launch {
            // Bloquear convite se o jogador já está noutras equipa confirmada/pendente no torneio
            if (utilizadorId in _emConflito.value) {
                _acao.value = GerirEquipaAcao.Erro("Este jogador já participa neste torneio com outra equipa.")
                return@launch
            }
            val capitaoResult = userRepository.getCurrentUser()
            val capitao = capitaoResult.getOrElse {
                _acao.value = GerirEquipaAcao.Erro("Sessão expirada.")
                return@launch
            }
            conviteRepository.enviarConvite(
                equipaId    = equipaId,
                equipaNome  = equipaNome,
                convidadoId = utilizadorId,
                criadoPorId = capitao.id
            )
                .onSuccess {
                    limparPesquisa()
                    _acao.value = GerirEquipaAcao.Sucesso("Convite enviado!")
                }
                .onFailure { e -> _acao.value = GerirEquipaAcao.Erro(e.message ?: "Erro ao convidar.") }
        }
    }

    fun removerJogador(utilizadorId: String) {
        if (!_isCapitao.value) {
            _acao.value = GerirEquipaAcao.Erro("Só o capitão pode remover jogadores.")
            return
        }
        if (utilizadorId == capitaoId) {
            _acao.value = GerirEquipaAcao.Erro("Não podes remover o capitão da equipa.")
            return
        }
        viewModelScope.launch {
            equipaRepository.removerMembro(equipaId, utilizadorId)
                .onFailure { e -> _acao.value = GerirEquipaAcao.Erro(e.message ?: "Erro ao remover.") }
        }
    }

    fun revogarConvite(conviteId: String) {
        if (!_isCapitao.value) {
            _acao.value = GerirEquipaAcao.Erro("Só o capitão pode revogar convites.")
            return
        }
        viewModelScope.launch {
            conviteRepository.revogarConvite(conviteId)
                .onFailure { e -> _acao.value = GerirEquipaAcao.Erro(e.message ?: "Erro ao revogar convite.") }
        }
    }

    /** Inscreve a equipa no torneio (só disponível quando torneioId != null e count está ok). */
    fun inscreverEquipa() {
        val tid = torneioId ?: return
        viewModelScope.launch {
            _acao.value = GerirEquipaAcao.Loading

            // Validação: apenas membros confirmados contam; convites pendentes não garantem presença
            val state = _uiState.value as? GerirEquipaUiState.Content
            if (state != null && state.membros.size < minJogadores) {
                _acao.value = GerirEquipaAcao.Erro(
                    "São necessários pelo menos $minJogadores jogadores confirmados. " +
                    "Tens apenas ${state.membros.size}. Convites pendentes não contam."
                )
                return@launch
            }

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

    fun updateEquipa(nome: String, iniciais: String, corAvatar: String, cidade: String?) {
        viewModelScope.launch {
            _editarState.value = EditarEquipaState.Loading
            equipaRepository.updateEquipa(equipaId, nome, iniciais, corAvatar, cidade)
                .onSuccess {
                    _equipaInfo.value = equipaRepository.getEquipaById(equipaId)
                    _editarState.value = EditarEquipaState.Success
                }
                .onFailure { e -> _editarState.value = EditarEquipaState.Error(e.message ?: "Erro ao atualizar.") }
        }
    }

    fun resetEditarState() { _editarState.value = EditarEquipaState.Idle }
}
