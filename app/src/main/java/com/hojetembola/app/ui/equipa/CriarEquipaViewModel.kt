package com.hojetembola.app.ui.equipa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hojetembola.app.data.local.entity.EquipaEntity
import com.hojetembola.app.data.repository.EquipaRepository
import com.hojetembola.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CriarEquipaForm(
    val nome: String       = "",
    val iniciais: String   = "",
    val corAvatar: String  = "#E84C3D",   // cor padrão (vermelho)
    val cidade: String     = ""
)

sealed class CriarEquipaUiState {
    object Idle    : CriarEquipaUiState()
    object Loading : CriarEquipaUiState()
    data class Success(val equipa: EquipaEntity) : CriarEquipaUiState()
    data class Error(val message: String)        : CriarEquipaUiState()
}

@HiltViewModel
class CriarEquipaViewModel @Inject constructor(
    private val equipaRepository: EquipaRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _form = MutableStateFlow(CriarEquipaForm())
    val form: StateFlow<CriarEquipaForm> = _form.asStateFlow()

    private val _uiState = MutableStateFlow<CriarEquipaUiState>(CriarEquipaUiState.Idle)
    val uiState: StateFlow<CriarEquipaUiState> = _uiState.asStateFlow()

    // ── Setters ───────────────────────────────────────────────────────────────

    fun setNome(v: String) {
        val iniciais = if (_form.value.iniciais.isEmpty() || _form.value.iniciais == _form.value.nome.toIniciais()) {
            v.toIniciais()   // auto-actualiza as iniciais enquanto o utilizador escreve o nome
        } else {
            _form.value.iniciais   // utilizador já editou manualmente → não sobrescrever
        }
        _form.update { it.copy(nome = v, iniciais = iniciais) }
    }

    fun setIniciais(v: String) {
        _form.update { it.copy(iniciais = v.uppercase().take(3)) }
    }

    fun setCorAvatar(v: String) { _form.update { it.copy(corAvatar = v) } }
    fun setCidade(v: String)    { _form.update { it.copy(cidade = v) } }
    fun resetState()            { _uiState.value = CriarEquipaUiState.Idle }

    // ── Validação ─────────────────────────────────────────────────────────────

    fun validate(): String? {
        val f = _form.value
        if (f.nome.isBlank())              return "O nome da equipa é obrigatório."
        if (f.iniciais.isBlank())          return "As iniciais são obrigatórias."
        if (f.iniciais.length > 3)         return "As iniciais têm no máximo 3 letras."
        return null
    }

    // ── Criar ─────────────────────────────────────────────────────────────────

    fun criarEquipa() {
        val erro = validate()
        if (erro != null) { _uiState.value = CriarEquipaUiState.Error(erro); return }

        viewModelScope.launch {
            _uiState.value = CriarEquipaUiState.Loading

            val utilizador = userRepository.getCurrentUser().getOrElse {
                _uiState.value = CriarEquipaUiState.Error("Sessão expirada. Faz login novamente.")
                return@launch
            }

            val f = _form.value
            equipaRepository.criarEquipa(
                nome      = f.nome,
                iniciais  = f.iniciais,
                corAvatar = f.corAvatar,
                cidade    = f.cidade.ifBlank { null },
                capitaoId = utilizador.id
            )
            .onSuccess { e -> _uiState.value = CriarEquipaUiState.Success(e) }
            .onFailure { e -> _uiState.value = CriarEquipaUiState.Error(e.message ?: "Erro ao criar equipa.") }
        }
    }
}

// ── Helper ────────────────────────────────────────────────────────────────────

private fun String.toIniciais(): String =
    split(" ")
        .filter { it.isNotBlank() && it.first().isLetter() }
        .take(3)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { take(3).uppercase() }
