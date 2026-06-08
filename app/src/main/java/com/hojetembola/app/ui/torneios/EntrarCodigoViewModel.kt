package com.hojetembola.app.ui.torneios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hojetembola.app.data.local.entity.TorneioEntity
import com.hojetembola.app.data.repository.TorneioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EntrarCodigoState {
    object Idle    : EntrarCodigoState()
    object Loading : EntrarCodigoState()
    data class Success(val torneio: TorneioEntity) : EntrarCodigoState()
    data class Error(val message: String)          : EntrarCodigoState()
}

@HiltViewModel
class EntrarCodigoViewModel @Inject constructor(
    private val torneioRepository: TorneioRepository
) : ViewModel() {

    private val _state = MutableStateFlow<EntrarCodigoState>(EntrarCodigoState.Idle)
    val state: StateFlow<EntrarCodigoState> = _state.asStateFlow()

    fun entrarComCodigo(codigo: String) {
        val trimmed = codigo.trim()
        if (trimmed.length != 4 || !trimmed.all { it.isDigit() }) {
            _state.value = EntrarCodigoState.Error("O código deve ter exatamente 4 dígitos.")
            return
        }
        viewModelScope.launch {
            _state.value = EntrarCodigoState.Loading
            torneioRepository.buscarPorCodigo(trimmed)
                .onSuccess { t -> _state.value = EntrarCodigoState.Success(t) }
                .onFailure { e -> _state.value = EntrarCodigoState.Error(e.message ?: "Erro ao procurar torneio.") }
        }
    }

    fun resetState() { _state.value = EntrarCodigoState.Idle }
}
