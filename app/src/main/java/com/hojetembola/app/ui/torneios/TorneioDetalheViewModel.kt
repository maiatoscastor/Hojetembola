package com.hojetembola.app.ui.torneios

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hojetembola.app.data.local.entity.TorneioEntity
import com.hojetembola.app.data.repository.TorneioRepository
import com.hojetembola.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TorneioDetalheUiState {
    object Loading : TorneioDetalheUiState()
    data class Error(val message: String) : TorneioDetalheUiState()
    data class Content(
        val torneio: TorneioEntity,
        /** true quando o utilizador autenticado é o organizador deste torneio */
        val isOrganizador: Boolean
    ) : TorneioDetalheUiState()
}

@HiltViewModel
class TorneioDetalheViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val torneioRepository: TorneioRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val torneioId: String = checkNotNull(savedStateHandle["torneioId"])

    private val _uiState = MutableStateFlow<TorneioDetalheUiState>(TorneioDetalheUiState.Loading)
    val uiState: StateFlow<TorneioDetalheUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val utilizadorResult = userRepository.getCurrentUser()
            val utilizadorId = utilizadorResult.getOrNull()?.id

            // Se o ID for UUID (torneio criado antes do fix), resolve para o ID inteiro real
            val effectiveId = if (torneioId.toIntOrNull() == null && utilizadorId != null) {
                torneioRepository.resolverTorneioId(torneioId, utilizadorId)
            } else {
                torneioId
            }

            torneioRepository.getTorneioById(effectiveId)
                .collect { torneio ->
                    _uiState.value = if (torneio == null) {
                        TorneioDetalheUiState.Error("Torneio não encontrado.")
                    } else {
                        TorneioDetalheUiState.Content(
                            torneio       = torneio,
                            isOrganizador = torneio.organizadorId == utilizadorId
                        )
                    }
                }
        }
    }
}
