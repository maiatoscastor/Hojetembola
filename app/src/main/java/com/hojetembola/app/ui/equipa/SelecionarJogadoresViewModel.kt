package com.hojetembola.app.ui.equipa

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hojetembola.app.data.local.entity.MembroComNome
import com.hojetembola.app.data.repository.EquipaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MembroSelecao(
    val membro: MembroComNome,
    val selecionado: Boolean = true
)

sealed class SelecionarJogadoresUiState {
    object Loading : SelecionarJogadoresUiState()
    data class Content(
        val membros: List<MembroSelecao>,
        val podeContinuar: Boolean
    ) : SelecionarJogadoresUiState()
    data class Error(val message: String) : SelecionarJogadoresUiState()
}

@HiltViewModel
class SelecionarJogadoresViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val equipaRepository: EquipaRepository
) : ViewModel() {

    val equipaId: String   = checkNotNull(savedStateHandle["equipaId"])
    val equipaNome: String = savedStateHandle["equipaNome"] ?: ""
    val min: Int           = savedStateHandle["min"] ?: 0
    val max: Int           = savedStateHandle["max"] ?: 99

    private val _uiState = MutableStateFlow<SelecionarJogadoresUiState>(SelecionarJogadoresUiState.Loading)
    val uiState: StateFlow<SelecionarJogadoresUiState> = _uiState.asStateFlow()

    /** Mantém o estado de seleção: utilizadorId → selecionado */
    private val selecao = mutableMapOf<String, Boolean>()

    init { loadMembros() }

    private fun loadMembros() {
        viewModelScope.launch {
            equipaRepository.getMembrosComNome(equipaId).collect { membros ->
                // Inicializar todos como selecionados se ainda não temos estado
                membros.forEach { m ->
                    if (!selecao.containsKey(m.utilizadorId)) {
                        selecao[m.utilizadorId] = true
                    }
                }
                emitContent(membros)
            }
        }
    }

    fun toggleSelecao(utilizadorId: String) {
        selecao[utilizadorId] = !(selecao[utilizadorId] ?: true)
        // Rebuild content with current member list
        viewModelScope.launch {
            val membros = equipaRepository.getMembrosComNome(equipaId).first()
            emitContent(membros)
        }
    }

    private fun emitContent(membros: List<MembroComNome>) {
        val lista = membros.map { m ->
            MembroSelecao(membro = m, selecionado = selecao[m.utilizadorId] ?: true)
        }
        val count = lista.count { it.selecionado }
        _uiState.value = SelecionarJogadoresUiState.Content(
            membros       = lista,
            podeContinuar = count in min..max
        )
    }
}
