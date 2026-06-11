package com.hojetembola.app.ui.torneios

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hojetembola.app.data.local.dao.JogoComEquipas
import com.hojetembola.app.data.local.entity.JornadaEntity
import com.hojetembola.app.data.repository.JogoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JornadaComJogos(
    val jornada: JornadaEntity,
    val jogos: List<JogoComEquipas>
)

@HiltViewModel
class CalendarioViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val jogoRepository: JogoRepository
) : ViewModel() {

    val torneioId: String = checkNotNull(savedStateHandle["torneioId"])
    val torneioNome: String = savedStateHandle["torneioNome"] ?: ""

    private val _uiState = MutableStateFlow<CalendarioUiState>(CalendarioUiState.Loading)
    val uiState: StateFlow<CalendarioUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            jogoRepository.syncJogosETorneio(torneioId)

            combine(
                jogoRepository.getJornadasFlow(torneioId),
                jogoRepository.getJogosComEquipas(torneioId)
            ) { jornadas, jogos ->
                if (jornadas.isEmpty()) {
                    CalendarioUiState.Empty
                } else {
                    val grouped = jornadas.map { jornada ->
                        JornadaComJogos(
                            jornada = jornada,
                            jogos = jogos.filter { it.jornadaId == jornada.id }
                        )
                    }
                    CalendarioUiState.Content(grouped)
                }
            }.catch { e ->
                _uiState.value = CalendarioUiState.Error(e.message ?: "Erro")
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}

sealed class CalendarioUiState {
    object Loading : CalendarioUiState()
    object Empty : CalendarioUiState()
    data class Error(val message: String) : CalendarioUiState()
    data class Content(val jornadas: List<JornadaComJogos>) : CalendarioUiState()
}
