package com.hojetembola.app.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hojetembola.app.data.local.entity.EquipaEntity
import com.hojetembola.app.data.local.entity.UtilizadorEntity
import com.hojetembola.app.data.repository.EquipaRepository
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

// ── Estados ───────────────────────────────────────────────────────────────────

sealed class PerfilSaveState {
    object Idle : PerfilSaveState()
    object Loading : PerfilSaveState()
    object Success : PerfilSaveState()
    data class Error(val message: String) : PerfilSaveState()
}

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
    private val userRepository: UserRepository,
    private val equipaRepository: EquipaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PerfilUiState>(PerfilUiState.Loading)
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

    private val _equipas = MutableStateFlow<List<EquipaEntity>>(emptyList())
    val equipas: StateFlow<List<EquipaEntity>> = _equipas.asStateFlow()

    private val _saveState = MutableStateFlow<PerfilSaveState>(PerfilSaveState.Idle)
    val saveState: StateFlow<PerfilSaveState> = _saveState.asStateFlow()

    private val _passState = MutableStateFlow<PerfilSaveState>(PerfilSaveState.Idle)
    val passState: StateFlow<PerfilSaveState> = _passState.asStateFlow()

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
                .onSuccess { user ->
                    _uiState.value = PerfilUiState.Success(user)
                    // Sincroniza equipas como capitão e como membro (convites aceites)
                    equipaRepository.syncEquipas(user.id)
                    equipaRepository.syncMinhasEquipasComoMembro(user.id)
                    equipaRepository.getTodasMinhasEquipas(user.id).collect { equipas ->
                        _equipas.value = equipas
                    }
                }
                .onFailure { e -> _uiState.value = PerfilUiState.Error(e.message ?: "Erro desconhecido") }
        }
    }

    fun updateProfile(nome: String, perfil: String) {
        viewModelScope.launch {
            _saveState.value = PerfilSaveState.Loading
            userRepository.updateProfile(nome, perfil)
                .onSuccess {
                    _saveState.value = PerfilSaveState.Success
                    loadProfile()
                }
                .onFailure { e ->
                    _saveState.value = PerfilSaveState.Error(e.message ?: "Erro ao atualizar perfil.")
                }
        }
    }

    fun resetSaveState() { _saveState.value = PerfilSaveState.Idle }

    fun changePassword(newPassword: String) {
        viewModelScope.launch {
            _passState.value = PerfilSaveState.Loading
            userRepository.changePassword(newPassword)
                .onSuccess { _passState.value = PerfilSaveState.Success }
                .onFailure { e -> _passState.value = PerfilSaveState.Error(e.message ?: "Erro ao alterar password.") }
        }
    }

    fun resetPassState() { _passState.value = PerfilSaveState.Idle }

    fun signOut() {
        viewModelScope.launch {
            userRepository.signOut()
            _signOutEvent.emit(Unit)
        }
    }
}
