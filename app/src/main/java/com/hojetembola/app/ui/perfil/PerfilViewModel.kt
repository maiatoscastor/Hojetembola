package com.hojetembola.app.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hojetembola.app.data.local.entity.UtilizadorEntity
import com.hojetembola.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Estado da UI ──────────────────────────────────────────────────────────────

sealed class PerfilUiState {
    /** A carregar dados do perfil. */
    object Loading : PerfilUiState()
    /** Dados carregados com sucesso. */
    data class Success(val utilizador: UtilizadorEntity) : PerfilUiState()
    /** Erro ao carregar — [message] para mostrar ao utilizador. */
    data class Error(val message: String) : PerfilUiState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * ViewModel do ecrã de Perfil.
 *
 * Expõe:
 *   - [uiState]      — estado da UI (Loading / Success / Error)
 *   - [signOutEvent] — evento único que dispara a navegação para o ecrã de auth
 */
@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PerfilUiState>(PerfilUiState.Loading)
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

    /** Evento de navegação one-shot — emitido uma única vez ao terminar sessão. */
    private val _signOutEvent = MutableSharedFlow<Unit>()
    val signOutEvent: SharedFlow<Unit> = _signOutEvent.asSharedFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = PerfilUiState.Loading
            userRepository.getCurrentUser()
                .onSuccess { user -> _uiState.value = PerfilUiState.Success(user) }
                .onFailure { e   -> _uiState.value = PerfilUiState.Error(e.message ?: "Erro desconhecido") }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            userRepository.signOut()
            _signOutEvent.emit(Unit)
        }
    }
}
