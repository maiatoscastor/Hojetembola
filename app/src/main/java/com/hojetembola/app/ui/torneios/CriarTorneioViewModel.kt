package com.hojetembola.app.ui.torneios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hojetembola.app.data.local.entity.TorneioEntity
import com.hojetembola.app.data.repository.TorneioRepository
import com.hojetembola.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// ── Estado do formulário ──────────────────────────────────────────────────────

data class CriarTorneioForm(
    val nome: String                     = "",
    val modalidade: String               = "Fut5",
    val numJogadoresPersonalizado: Int?  = null,
    /** Mínimo calculado automaticamente com base na modalidade */
    val minJogadoresPorEquipa: Int       = 5,
    val formato: String                  = "Liga",
    val maxEquipas: Int                  = 8,
    val maxJogadoresPorEquipa: Int       = 12,
    val dataInicioInscricoes: String     = "",
    val dataFimInscricoes: String        = "",
    val dataInicio: String               = "",
    val dataFimPrevista: String          = "",
    /** Nome do campo / instalação (campo obrigatório) */
    val localizacaoNome: String          = "",
    /** Morada opcional */
    val localizacaoMorada: String        = "",
    /** Link Google Maps (opcional) */
    val localizacaoMapsUrl: String       = "",
    val criterioDesempate: String        = "Penalidades",
    /** Duração de cada parte do prolongamento em minutos */
    val tempoExtraMinutos: Int           = 10,
    val amarelasParaSuspensao: Int       = 3,
    /** true → visibilidade = "Publico"; false → "Privado" */
    val publico: Boolean                 = true,
    /** Código gerado automaticamente para torneios privados (4 dígitos) */
    val codigoAcesso: String             = "",
    /** true enquanto o código único está a ser gerado via Supabase */
    val codigoAcessoLoading: Boolean     = false,
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

    fun setNome(v: String) { _form.update { it.copy(nome = v) } }

    /** Ao mudar modalidade: recalcula o mínimo e auto-ajusta o max se necessário */
    fun setModalidade(v: String) {
        val min = minJogadoresPorModalidade(v, _form.value.numJogadoresPersonalizado)
        _form.update { f ->
            f.copy(
                modalidade                = v,
                numJogadoresPersonalizado = if (v != "Personalizado") null else f.numJogadoresPersonalizado,
                minJogadoresPorEquipa     = min,
                maxJogadoresPorEquipa     = maxOf(f.maxJogadoresPorEquipa, min)
            )
        }
    }

    /** Ao definir o nº personalizado: atualiza o mínimo e auto-ajusta o max */
    fun setNumPersonalizado(v: Int?) {
        val min = v?.takeIf { it > 0 } ?: 5
        _form.update { f ->
            f.copy(
                numJogadoresPersonalizado = v,
                minJogadoresPorEquipa     = min,
                maxJogadoresPorEquipa     = maxOf(f.maxJogadoresPorEquipa, min)
            )
        }
    }

    fun setFormato(v: String)             { _form.update { it.copy(formato = v) } }
    fun setMaxEquipas(v: Int)             { _form.update { it.copy(maxEquipas = v.coerceIn(2, 32)) } }

    /** Decrement/increment respeitam o mínimo dinâmico da modalidade */
    fun setMaxJogadores(v: Int) {
        _form.update { f -> f.copy(maxJogadoresPorEquipa = v.coerceIn(f.minJogadoresPorEquipa, 25)) }
    }

    fun setDataInicioInscricoes(v: String) { _form.update { it.copy(dataInicioInscricoes = v) } }
    fun setDataFimInscricoes(v: String)    { _form.update { it.copy(dataFimInscricoes = v) } }
    fun setDataInicio(v: String)           { _form.update { it.copy(dataInicio = v) } }
    fun setDataFimPrevista(v: String)      { _form.update { it.copy(dataFimPrevista = v) } }
    fun setLocalizacao(v: String)          { _form.update { it.copy(localizacaoNome = v) } }
    fun setLocalizacaoLink(v: String)      { _form.update { it.copy(localizacaoMapsUrl = v) } }
    fun setCriterioDesempate(v: String)    { _form.update { it.copy(criterioDesempate = v) } }
    fun setTempoExtraMinutos(v: Int)       { _form.update { it.copy(tempoExtraMinutos = v.coerceIn(5, 30)) } }
    fun setAmarelasParaSuspensao(v: Int)   { _form.update { it.copy(amarelasParaSuspensao = v.coerceIn(1, 5)) } }

    /** Ao tornar privado: inicia geração automática de código único */
    fun setPublico(v: Boolean) {
        if (v) {
            // Público — limpa o código
            _form.update { it.copy(publico = true, codigoAcesso = "", codigoAcessoLoading = false) }
        } else {
            // Privado — gera código único via Supabase
            _form.update { it.copy(publico = false, codigoAcesso = "", codigoAcessoLoading = true) }
            viewModelScope.launch {
                val code = torneioRepository.gerarCodigoUnico()
                _form.update { it.copy(codigoAcesso = code, codigoAcessoLoading = false) }
            }
        }
    }
    fun setPermitirEspectadores(v: Boolean) { _form.update { it.copy(permitirEspectadores = v) } }
    fun setVotacaoMvpAtiva(v: Boolean)      { _form.update { it.copy(votacaoMvpAtiva = v) } }
    fun setRegulamento(v: String)           { _form.update { it.copy(regulamento = v) } }

    fun resetState() { _uiState.value = CriarTorneioUiState.Idle }

    // ── Validação ─────────────────────────────────────────────────────────────

    fun validate(): String? {
        val f = _form.value

        if (f.nome.isBlank())            return "O nome do torneio é obrigatório."
        if (f.localizacaoNome.isBlank()) return "A localização é obrigatória."

        if (f.modalidade == "Personalizado" && (f.numJogadoresPersonalizado ?: 0) < 1)
            return "Indica o número de jogadores para a modalidade personalizada."

        if (f.maxJogadoresPorEquipa < f.minJogadoresPorEquipa)
            return "O plantel deve ter pelo menos ${f.minJogadoresPorEquipa} jogadores para ${f.modalidade.uppercase()}."

        if (f.dataInicioInscricoes.isBlank()) return "Define a data de início das inscrições."
        if (f.dataFimInscricoes.isBlank())    return "Define a data de fim das inscrições."
        if (f.dataInicio.isBlank())           return "Define a data de início do torneio."

        return try {
            val hoje             = LocalDate.now()
            val inicioInscricoes = LocalDate.parse(f.dataInicioInscricoes)
            val fimInscricoes    = LocalDate.parse(f.dataFimInscricoes)
            val inicioTorneio    = LocalDate.parse(f.dataInicio)

            if (inicioInscricoes < hoje)
                return "A data de início das inscrições não pode ser no passado."
            if (!fimInscricoes.isAfter(inicioInscricoes))
                return "O fim das inscrições deve ser posterior ao início."
            if (!inicioTorneio.isAfter(fimInscricoes))
                return "O torneio deve começar depois de terminadas as inscrições."
            if (f.dataFimPrevista.isNotBlank()) {
                val fimPrevista = LocalDate.parse(f.dataFimPrevista)
                if (!fimPrevista.isAfter(inicioTorneio))
                    return "A data de fim prevista deve ser posterior ao início do torneio."
            }

            if (!f.publico && f.codigoAcessoLoading)
                return "Aguarda a geração do código de acesso."
            if (!f.publico && f.codigoAcesso.isBlank())
                return "Erro ao gerar código de acesso. Tenta mudar para Público e voltar a Privado."

            null
        } catch (_: Exception) {
            null // Parsing error — não deve acontecer pois as datas vêm do DatePicker
        }
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

            // MELHORIA-01: Apenas organizadores podem criar torneios
            if (!utilizador.perfil.equals("Organizador", ignoreCase = true)) {
                _uiState.value = CriarTorneioUiState.Error("Apenas organizadores podem criar torneios.")
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
                localizacaoNome           = f.localizacaoNome.trim(),
                localizacaoMorada         = f.localizacaoMorada.ifBlank { null },
                localizacaoMapsUrl        = f.localizacaoMapsUrl.ifBlank { null },
                criterioDesempate         = f.criterioDesempate,
                tempoExtraMinutos         = f.tempoExtraMinutos,
                amarelasParaSuspensao     = f.amarelasParaSuspensao,
                visibilidade              = if (f.publico) "Publico" else "Privado",
                codigoAcesso              = f.codigoAcesso.ifBlank { null },
                permitirEspectadores      = f.permitirEspectadores,
                votacaoMvpAtiva           = f.votacaoMvpAtiva,
                regulamento               = f.regulamento.ifBlank { null },
                organizadorId             = utilizador.id
            )
            .onSuccess { t -> _uiState.value = CriarTorneioUiState.Success(t) }
            .onFailure { e -> _uiState.value = CriarTorneioUiState.Error(e.message ?: "Erro ao criar torneio.") }
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun minJogadoresPorModalidade(modalidade: String, numPersonalizado: Int?): Int =
        when (modalidade) {
            "Fut5"         -> 5
            "Fut7"         -> 7
            "Fut11"        -> 11
            "Personalizado"-> numPersonalizado?.takeIf { it > 0 } ?: 5
            else           -> 5
        }
}
