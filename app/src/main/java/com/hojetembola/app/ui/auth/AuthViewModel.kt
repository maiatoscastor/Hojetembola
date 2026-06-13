package com.hojetembola.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hojetembola.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Estado de autenticação ────────────────────────────────────────────────────

/**
 * Estados possíveis do fluxo de autenticação.
 *
 * Partilhado por [LoginFragment] e [RegistoFragment] através de [AuthViewModel.authState].
 */
sealed class AuthState {

    /** Estado inicial — sem operação em curso. */
    object Idle : AuthState()

    /** Operação em curso — mostrar indicador de carregamento na UI. */
    object Loading : AuthState()

    /**
     * Login ou registo bem-sucedido.
     * @param uid  UID do utilizador autenticado no Supabase.
     */
    data class Success(val uid: String) : AuthState()

    object EmailSent : AuthState()

    /**
     * Ocorreu um erro.
     * @param message  Mensagem localizável para mostrar ao utilizador via Snackbar.
     */
    data class Error(val message: String) : AuthState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

/** ViewModel de autenticação partilhado por [LoginFragment] e [RegistoFragment]. */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // ── Login com email / password ────────────────────────────────────────────

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.login(email, password)
                .onSuccess { uid -> _authState.value = AuthState.Success(uid) }
                .onFailure { e   -> _authState.value = AuthState.Error(e.localizedMessage ?: "Erro desconhecido") }
        }
    }

    // ── Registo com email / password ──────────────────────────────────────────

    fun register(email: String, password: String, nome: String, perfil: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.register(email, password, nome, perfil)
                .onSuccess { uid -> _authState.value = AuthState.Success(uid) }
                .onFailure { e   -> _authState.value = AuthState.Error(e.localizedMessage ?: "Erro desconhecido") }
        }
    }

    // ── Login com Google OAuth ────────────────────────────────────────────────

    /**
     * Inicia o fluxo OAuth com Google via Supabase.
     *
     * Requer configuração prévia em:
     *   1. Google Cloud Console → OAuth 2.0 credentials
     *   2. Supabase Dashboard → Authentication → Providers → Google
     *
     * Para MVP académico, [AuthRepository.loginWithGoogle] retorna erro informativo
     * até estar configurado.
     */
    fun loginWithGoogle() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.loginWithGoogle()
                .onSuccess { uid -> _authState.value = AuthState.Success(uid) }
                .onFailure { e   -> _authState.value = AuthState.Error(e.localizedMessage ?: "Erro desconhecido") }
        }
    }

    // ── Recuperação de password ───────────────────────────────────────────────

    /**
     * Envia email de recuperação de password.
     *
     * Em caso de sucesso emite [AuthState.EmailSent] (não [AuthState.Success])
     * para que [LoginFragment] mostre uma mensagem sem navegar para MainActivity.
     */
    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.forgotPassword(email)
                .onSuccess { _authState.value = AuthState.EmailSent }
                .onFailure { e -> _authState.value = AuthState.Error(e.localizedMessage ?: "Erro desconhecido") }
        }
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    /**
     * Repõe o estado para [AuthState.Idle].
     *
     * Deve ser chamado pela UI ANTES de navegar para outra Activity/Fragment,
     * de modo a evitar que o observer re-dispare a navegação quando o fragmento
     * é retomado (ex: ao fazer back de MainActivity → SplashActivity).
     */
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
