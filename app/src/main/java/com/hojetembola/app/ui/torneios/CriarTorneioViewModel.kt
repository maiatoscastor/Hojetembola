package com.hojetembola.app.ui.torneios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hojetembola.app.data.local.entity.TorneioEntity
import com.hojetembola.app.data.repository.TorneioRepository
import com.hojetembola.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Estado do formulário ──────────────────────────────────────────────────────

data class CriarTorneioForm(
    val nome: String                     = "",
    val modalidade: String               = "fut7",
    val numJogadoresPersonalizado: Int?  = null,
    val formato: String                  = "liga",
    val maxEquipas: Int                  = 8,
    val maxJogadoresPorEquipa: Int       = 12,
    val dataInicioInscricoes: String     = "",
    val dataFimInscricoes: String        = "",
    val dataInicio: String               = "",
    val dataFimPrevista: String          = "",
    val localizacao: String              = "",
    val localizacaoLink: String          = "",
    val criterioDesempate: String        = "penalidades",
    val amarelasParaSuspensao: Int       = 3,
    val publico: Boolean                 = true,
    val permitirEspectadores: Boolean    = true,
    val votacaoMvpAtiva: Boolean         = true,
    val regulamento: String              = ""
)

sealed class CriarTorneioUiState {
    object Idle    : CriarTorneioUiState()
    object Loading : CriarTorneioUiState()
    data class Success(val torneio: TorneioEntity) : CriarTorneioUiState()
    data class Error(val message: String)          : CriarTorneioUiState()
}

@HiltViewModel
class CriarTorneioViewModel @Inject constructor(
    private val torneioRepository: TorneioRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _form = MutableStateFlow(CriarTorneioForm())
    val form: StateFlow<CriarTorneioForm> = _form.asStateFlow()

    private val _uiState = MutableStateFlow<CriarTorneioUiState>(CriarTorneioUiState.Idle)
    val uiState: StateFlow<CriarTorneioUiState> = _uiState.asStateFlow()

    // ── Setters do formulário ─────────────────────────────────────────────────

    fun setNome(v: String)                  { _form.update { it.copy(nome = v) } }
    fun setModalidade(v: String)            { _form.update { it.copy(modalidade = v, numJogadoresPersonalizado = null) } }
    fun setNumPersonalizado(v: Int?)        { _form.update { it.copy(numJogadoresPersonalizado = v) } }
    fun setFormato(v: String)               { _form.update { it.copy(formato = v) } }
    fun setMaxEquipas(v: Int)               { _form.update { it.copy(maxEquipas = v.coerceIn(2, 32)) } }
    fun setMaxJogadores(v: Int)             { _form.update { it.copy(maxJogadoresPorEquipa = v.coerceIn(5, 25)) } }
    fun setDataInicioInscricoes(v: String)  { _form.update { it.copy(dataInicioInscricoes = v) } }
    fun setDataFimInscricoes(v: String)     { _form.update { it.copy(dataFimInscricoes = v) } }
    fun setDataInicio(v: String)            { _form.update { it.copy(dataInicio = v) } }
    fun setDataFimPrevista(v: String)       { _form.update { it.copy(dataFimPrevista = v) } }
    fun setLocalizacao(v: String)           { _form.update { it.copy(localizacao = v) } }
    fun setLocalizacaoLink(v: String)       { _form.update { it.copy(localizacaoLink = v) } }
    fun setCriterioDesempate(v: String)     { _form.update { it.copy(criterioDesempate = v) } }
    fun setAmarelasParaSuspensao(v: Int)    { _form.update { it.copy(amarelasParaSuspensao = v.coerceIn(1, 5)) } }
    fun setPublico(v: Boolean)              { _form.update { it.copy(publico = v) } }
    fun setPermitirEspectadores(v: Boolean) { _form.update { it.copy(permitirEspectadores = v) } }
    fun setVotacaoMvpAtiva(v: Boolean)      { _form.update { it.copy(votacaoMvpAtiva = v) } }
    fun setRegulamento(v: String)           { _form.update { it.copy(regulamento = v) } }

    fun resetState() { _uiState.value = CriarTorneioUiState.Idle }

    // ── Validação ─────────────────────────────────────────────────────────────

    fun validate(): String? {
        val f = _form.value
        if (f.nome.isBlank())                     return "O nome do torneio é obrigatório."
        if (f.localizacao.isBlank())              return "A localização é obrigatória."
        if (f.dataInicioInscricoes.isBlank())     return "Define a data de início das inscrições."
        if (f.dataFimInscricoes.isBlank())        return "Define a data de fim das inscrições."
        if (f.dataInicio.isBlank())               return "Define a data de início do torneio."
        if (f.dataFimInscricoes < f.dataInicioInscricoes)
            return "O fim das inscrições não pode ser antes do início."
        if (f.dataInicio < f.dataFimInscricoes)
            return "O torneio não pode começar antes de terminarem as inscrições."
        if (f.dataFimPrevista.isNotBlank() && f.dataFimPrevista < f.dataInicio)
            return "A data de fim não pode ser antes do início do torneio."
        if (f.modalidade == "personalizado" && (f.numJogadoresPersonalizado ?: 0) < 1)
            return "Indica o número de jogadores para a modalidade personalizada."
        return null
    }

    // ── Criar torneio ─────────────────────────────────────────────────────────

    fun criarTorneio() {
        val erroValidacao = validate()
        if (erroValidacao != null) {
            _uiState.value = CriarTorneioUiState.Error(erroValidacao)
            return
        }

        viewModelScope.launch {
            _uiState.value = CriarTorneioUiState.Loading

            val userResult = userRepository.getCurrentUser()
            val utilizador = userResult.getOrElse {
                _uiState.value = CriarTorneioUiState.Error("Sessão expirada. Faz login novamente.")
                return@launch
            }

            val f = _form.value
            torneioRepository.criarTorneio(
                nome                      = f.nome.trim(),
                modalidade                = f.modalidade,
                numJogadoresPersonalizado = f.numJogadoresPersonalizado,
                formato                   = f.formato,
                maxEquipas                = f.maxEquipas,
                maxJogadoresPorEquipa     = f.maxJogadoresPorEquipa,
                dataInicioInscricoes      = f.dataInicioInscricoes,
                dataFimInscricoes         = f.dataFimInscricoes,
                dataInicio                = f.dataInicio,
                dataFimPrevista           = f.dataFimPrevista,
                localizacao               = f.localizacao.trim(),
                localizacaoLink           = f.localizacaoLink.ifBlank { null },
                criterioDesempate         = f.criterioDesempate,
                amarelasParaSuspensao     = f.amarelasParaSuspensao,
                publico                   = f.publico,
                permitirEspectadores      = f.permitirEspectadores,
                votacaoMvpAtiva           = f.votacaoMvpAtiva,
                regulamento               = f.regulamento.ifBlank { null },
                organizadorId             = utilizador.id
            ).onSuccess { t -> _uiState.value = CriarTorneioUiState.Success(t) }
             .onFailure { e -> _uiState.value = CriarTorneioUiState.Error(e.message ?: "Erro ao criar torneio.") }
        }
    }
}
