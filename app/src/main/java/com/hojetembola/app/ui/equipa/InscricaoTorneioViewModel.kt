package com.hojetembola.app.ui.equipa

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hojetembola.app.data.local.entity.EquipaEntity
import com.hojetembola.app.data.local.entity.minJogadoresPorEquipa
import com.hojetembola.app.data.repository.EquipaRepository
import com.hojetembola.app.data.repository.TorneioRepository
import com.hojetembola.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EquipaComEstado(
    val equipa: EquipaEntity,
    val jaInscrita: Boolean,
    val numMembros: Int = 0
)

sealed class InscricaoUiState {
    object Loading : InscricaoUiState()
    data class Content(val equipas: List<EquipaComEstado>) : InscricaoUiState()
    data class Error(val message: String) : InscricaoUiState()
}

sealed class InscricaoAcao {
    object Idle : InscricaoAcao()
    object Loading : InscricaoAcao()
    data class Sucesso(val equipaNome: String) : InscricaoAcao()
    data class Erro(val message: String) : InscricaoAcao()
    /** Equipa com menos jogadores do que o mínimo exigido. */
    data class PoucosMembros(
        val equipaId: String, val equipaNome: String,
        val count: Int, val min: Int, val max: Int
    ) : InscricaoAcao()
    /** Equipa com mais jogadores do que o máximo — tem de selecionar quais vão. */
    data class MuitosMembros(
        val equipaId: String, val equipaNome: String,
        val min: Int, val max: Int
    ) : InscricaoAcao()
}

@HiltViewModel
class InscricaoTorneioViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val equipaRepository: EquipaRepository,
    private val torneioRepository: TorneioRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val torneioId: String = checkNotNull(savedStateHandle["torneioId"])

    private val _uiState = MutableStateFlow<InscricaoUiState>(InscricaoUiState.Loading)
    val uiState: StateFlow<InscricaoUiState> = _uiState.asStateFlow()

    private val _acao = MutableStateFlow<InscricaoAcao>(InscricaoAcao.Idle)
    val acao: StateFlow<InscricaoAcao> = _acao.asStateFlow()

    private var capitaoId: String = ""

    init { loadEquipas() }

    fun loadEquipas() {
        viewModelScope.launch {
            _uiState.value = InscricaoUiState.Loading

            val utilizador = userRepository.getCurrentUser().getOrElse {
                _uiState.value = InscricaoUiState.Error("Sessão expirada.")
                return@launch
            }
            capitaoId = utilizador.id

            equipaRepository.syncEquipas(capitaoId)

            equipaRepository.getMinhasEquipas(capitaoId).collect { equipas ->
                // Sincroniza membros de cada equipa para ter contagem correcta
                equipas.forEach { e -> equipaRepository.syncMembros(e.id) }

                val comEstado = equipas.map { e ->
                    EquipaComEstado(
                        equipa     = e,
                        jaInscrita = equipaRepository.jaInscrita(torneioId, e.id),
                        numMembros = equipaRepository.contarMembros(e.id)
                    )
                }
                _uiState.value = InscricaoUiState.Content(comEstado)
            }
        }
    }

    /**
     * Valida antes de abrir a seleção de jogadores:
     * - Utilizador já tem outra equipa inscrita → Erro
     * - count < min → PoucosMembros (deve ir gerir a equipa)
     * - count >= min → MuitosMembros (abre seleção de jogadores sempre)
     */
    fun inscrever(equipaId: String, equipaNome: String) {
        viewModelScope.launch {
            _acao.value = InscricaoAcao.Loading

            // Verifica se o utilizador já tem outra equipa inscrita neste torneio
            val state = _uiState.value as? InscricaoUiState.Content
            if (state != null && state.equipas.any { it.jaInscrita && it.equipa.id != equipaId }) {
                _acao.value = InscricaoAcao.Erro("Já tens uma equipa inscrita neste torneio.")
                return@launch
            }

            val torneio = torneioRepository.getTorneioByIdSuspend(torneioId)
            if (torneio == null) {
                _acao.value = InscricaoAcao.Erro("Torneio não encontrado.")
                return@launch
            }

            val count = equipaRepository.contarMembros(equipaId)
            val min   = torneio.minJogadoresPorEquipa()
            val max   = torneio.maxJogadoresPorEquipa

            when {
                count < min -> _acao.value = InscricaoAcao.PoucosMembros(equipaId, equipaNome, count, min, max)
                else        -> _acao.value = InscricaoAcao.MuitosMembros(equipaId, equipaNome, min, max)
            }
        }
    }

    /** Chamado após o utilizador selecionar jogadores no SelecionarJogadoresBottomSheet. */
    fun inscreverComJogadores(equipaId: String, equipaNome: String) {
        viewModelScope.launch {
            _acao.value = InscricaoAcao.Loading
            executarInscricao(equipaId, equipaNome)
        }
    }

    private suspend fun executarInscricao(equipaId: String, equipaNome: String) {
        equipaRepository.inscreverEquipa(torneioId, equipaId, capitaoId)
            .onSuccess { _acao.value = InscricaoAcao.Sucesso(equipaNome) }
            .onFailure { e -> _acao.value = InscricaoAcao.Erro(e.message ?: "Erro ao inscrever.") }
    }

    fun resetAcao() { _acao.value = InscricaoAcao.Idle }
}
