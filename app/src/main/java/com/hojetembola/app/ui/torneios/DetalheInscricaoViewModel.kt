package com.hojetembola.app.ui.torneios

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hojetembola.app.data.local.dao.JogadorInscricaoComNome
import com.hojetembola.app.data.repository.EquipaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetalheInscricaoUiState(
    val equipaNome: String = "",
    val cidade: String? = null,
    val jogadores: List<JogadorInscricaoComNome> = emptyList(),
    val loading: Boolean = true
)

@HiltViewModel
class DetalheInscricaoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val equipaRepository: EquipaRepository
) : ViewModel() {

    val inscricaoId: String = checkNotNull(savedStateHandle["inscricaoId"])
    val equipaId: String    = checkNotNull(savedStateHandle["equipaId"])
    val equipaNome: String  = savedStateHandle["equipaNome"] ?: ""

    private val _uiState = MutableStateFlow(DetalheInscricaoUiState(equipaNome = equipaNome))
    val uiState: StateFlow<DetalheInscricaoUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            // Sync players from Supabase
            equipaRepository.syncJogadoresInscricao(inscricaoId)

            val cidade = equipaRepository.getEquipaById(equipaId)?.cidade

            equipaRepository.getJogadoresInscricao(inscricaoId).collect { jogadores ->
                _uiState.value = DetalheInscricaoUiState(
                    equipaNome = equipaNome,
                    cidade     = cidade,
                    jogadores  = jogadores,
                    loading    = false
                )
            }
        }
    }
}
