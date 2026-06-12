package com.hojetembola.app.ui.torneios

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hojetembola.app.data.local.dao.EventoComNome
import com.hojetembola.app.data.local.dao.InscricaoEquipaDao
import com.hojetembola.app.data.local.dao.JogadorInscricaoComNome
import com.hojetembola.app.data.local.dao.JogadorInscricaoDao
import com.hojetembola.app.data.local.entity.ClassificacaoEntity
import com.hojetembola.app.data.local.entity.ConvocatoriaJogoEntity
import com.hojetembola.app.data.local.entity.JogoEntity
import com.hojetembola.app.data.local.entity.minJogadoresPorEquipa
import com.hojetembola.app.data.repository.ConvocatoriaEntrada
import com.hojetembola.app.data.repository.JogoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JogoDetalheUiState(
    val loading: Boolean = true,
    val jogo: JogoEntity? = null,
    val casaNome: String = "",
    val casaIniciais: String = "",
    val casaCor: String = "#3D5A80",
    val visitanteNome: String = "",
    val visitanteIniciais: String = "",
    val visitanteCor: String = "#3D5A80",
    val eventos: List<EventoComNome> = emptyList(),
    /** Full inscribed roster (never changes during a match). */
    val casaJogadores: List<JogadorInscricaoComNome> = emptyList(),
    val visitanteJogadores: List<JogadorInscricaoComNome> = emptyList(),
    /** Derived from starting lineup + substitution events — who is currently on the field. */
    val casaJogadoresEmCampo: List<JogadorInscricaoComNome> = emptyList(),
    val visitanteJogadoresEmCampo: List<JogadorInscricaoComNome> = emptyList(),
    /** Players on the bench (non-titular + substituted out). */
    val casaJogadoresNoBanco: List<JogadorInscricaoComNome> = emptyList(),
    val visitanteJogadoresNoBanco: List<JogadorInscricaoComNome> = emptyList(),
    /** IDs of players in the starting XI — empty if titulares not yet defined. */
    val casaTitularIds: Set<String> = emptySet(),
    val visitanteTitularIds: Set<String> = emptySet(),
    /** True once the organizer has saved a starting lineup. */
    val titularesDefinidos: Boolean = false,
    /** Exact number of starters required per team based on tournament modality. */
    val numJogadoresPorEquipa: Int = 0,
    /** Players expelled (red card) — removed from both campo and banco. */
    val casaExpulsoIds: Set<String> = emptySet(),
    val visitanteExpulsoIds: Set<String> = emptySet(),
    /** Player IDs with an active suspension — cannot be selected as starters. */
    val suspensosIds: Set<String> = emptySet(),
    /** Tournament standings — shown in the ranking section on the game detail screen. */
    val classificacao: List<ClassificacaoEntity> = emptyList(),
    /** Map of equipaId → equipa nome for the ranking table. */
    val equipaNomesMap: Map<String, String> = emptyMap(),
    val erro: String? = null
)

sealed class JogoDetalheAcao {
    object Idle : JogoDetalheAcao()
    object Loading : JogoDetalheAcao()
    data class Sucesso(val msg: String) : JogoDetalheAcao()
    data class Erro(val msg: String) : JogoDetalheAcao()
}

@HiltViewModel
class JogoDetalheViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val jogoRepository: JogoRepository,
    private val inscricaoEquipaDao: InscricaoEquipaDao,
    private val jogadorInscricaoDao: JogadorInscricaoDao
) : ViewModel() {

    val jogoId: String = checkNotNull(savedStateHandle["jogoId"])
    val torneioId: String = checkNotNull(savedStateHandle["torneioId"])
    val isOrganizador: Boolean = savedStateHandle["isOrganizador"] ?: false

    private val _uiState = MutableStateFlow(JogoDetalheUiState())
    val uiState: StateFlow<JogoDetalheUiState> = _uiState.asStateFlow()

    private val _acao = MutableStateFlow<JogoDetalheAcao>(JogoDetalheAcao.Idle)
    val acao: StateFlow<JogoDetalheAcao> = _acao.asStateFlow()

    /** Minuto ao vivo gerido por timer local no ViewModel. */
    private val _minutoVivo = MutableStateFlow<Int?>(null)
    val minutoVivo: StateFlow<Int?> = _minutoVivo.asStateFlow()

    private var timerJob: Job? = null

    init { load() }

    private fun load() {
        viewModelScope.launch {
            // Sync remote data before streaming local cache
            jogoRepository.syncEventos(jogoId)
            jogoRepository.syncConvocatoria(jogoId)

            // Load full roster once — doesn't change during a match
            val jogoInit = jogoRepository.getJogoById(jogoId)
            val casaJogadores      = loadJogadores(jogoInit?.equipaCasaId)
            val visitanteJogadores = loadJogadores(jogoInit?.equipaVisitanteId)
            val numJogadoresPorEquipa = jogoRepository.getTorneioById(torneioId)?.minJogadoresPorEquipa() ?: 0
            jogoRepository.syncSuspensoes(torneioId)
            val suspensosIds = jogoRepository.getSuspensosIds(torneioId)
            val equipaNomesMap = jogoRepository.getEquipaNomesMap(torneioId)

            // Resume live timer if the app was restarted mid-game
            if (jogoInit?.estado == "ao_vivo") startTimer(jogoInit.minutoAtual ?: 0)

            combine(
                jogoRepository.getJogosComEquipas(torneioId),
                jogoRepository.getEventosComNome(jogoId),
                jogoRepository.getConvocatoriaFlow(jogoId),
                jogoRepository.getClassificacao(torneioId)
            ) { jogos, eventos, convocatorias, classificacao ->
                val jogoData   = jogos.find { it.jogoId == jogoId }
                val jogoEntity = jogoRepository.getJogoById(jogoId)

                val casaTitularIds = convocatorias
                    .filter { it.equipaId == jogoEntity?.equipaCasaId && it.isTitular }
                    .map { it.utilizadorId }.toSet()
                val visitanteTitularIds = convocatorias
                    .filter { it.equipaId == jogoEntity?.equipaVisitanteId && it.isTitular }
                    .map { it.utilizadorId }.toSet()
                val titularesDefinidos = convocatorias.isNotEmpty()

                // Derive current on-field players from titulares + substitutions + expulsions
                val casaExpulsoIds = eventos
                    .filter { it.tipo == "vermelho" && it.equipaId == jogoEntity?.equipaCasaId }
                    .mapNotNull { it.jogadorId }.toSet()
                val visitExpulsoIds = eventos
                    .filter { it.tipo == "vermelho" && it.equipaId == jogoEntity?.equipaVisitanteId }
                    .mapNotNull { it.jogadorId }.toSet()

                val casaCampo  = computeEmCampo(casaJogadores,     casaTitularIds,     eventos, jogoEntity?.equipaCasaId)
                val visitCampo = computeEmCampo(visitanteJogadores, visitanteTitularIds, eventos, jogoEntity?.equipaVisitanteId)
                // Expelled players are out of the game — not in campo (already handled) and not in banco
                val casaBanco  = casaJogadores.filter  { j ->
                    casaCampo.none  { it.utilizadorId == j.utilizadorId } && j.utilizadorId !in casaExpulsoIds
                }
                val visitBanco = visitanteJogadores.filter { j ->
                    visitCampo.none { it.utilizadorId == j.utilizadorId } && j.utilizadorId !in visitExpulsoIds
                }

                _uiState.value = JogoDetalheUiState(
                    loading                   = false,
                    jogo                      = jogoEntity,
                    casaNome                  = jogoData?.casaNome ?: "",
                    casaIniciais              = jogoData?.casaIniciais ?: "?",
                    casaCor                   = jogoData?.casaCor ?: "#3D5A80",
                    visitanteNome             = jogoData?.visitanteNome ?: "",
                    visitanteIniciais         = jogoData?.visitanteIniciais ?: "?",
                    visitanteCor              = jogoData?.visitanteCor ?: "#3D5A80",
                    eventos                   = eventos,
                    casaJogadores             = casaJogadores,
                    visitanteJogadores        = visitanteJogadores,
                    casaJogadoresEmCampo      = casaCampo,
                    visitanteJogadoresEmCampo = visitCampo,
                    casaJogadoresNoBanco      = casaBanco,
                    visitanteJogadoresNoBanco = visitBanco,
                    casaTitularIds            = casaTitularIds,
                    visitanteTitularIds       = visitanteTitularIds,
                    titularesDefinidos        = titularesDefinidos,
                    numJogadoresPorEquipa     = numJogadoresPorEquipa,
                    casaExpulsoIds            = casaExpulsoIds,
                    visitanteExpulsoIds       = visitExpulsoIds,
                    suspensosIds              = suspensosIds,
                    classificacao             = classificacao,
                    equipaNomesMap            = equipaNomesMap
                )
            }.collect {}
        }
    }

    private suspend fun loadJogadores(equipaId: String?): List<JogadorInscricaoComNome> {
        equipaId ?: return emptyList()
        val inscricao = inscricaoEquipaDao.getByTorneioAndEquipa(torneioId, equipaId) ?: return emptyList()
        return jogadorInscricaoDao.getComNomeSuspend(inscricao.id)
    }

    /**
     * Returns the players currently on the field.
     *
     * - If the starting lineup (titulares) has been defined, it is used as the
     *   base set.  Substitution events then move players in/out.
     * - If no lineup has been saved yet (pre-game), the full inscribed roster is
     *   used as a fallback so the rest of the UI keeps working.
     */
    private fun computeEmCampo(
        todos: List<JogadorInscricaoComNome>,
        titularIds: Set<String>,
        eventos: List<EventoComNome>,
        equipaId: String?
    ): List<JogadorInscricaoComNome> {
        if (equipaId == null) return todos
        // Base: titulares if defined, otherwise the full squad
        val campoIds = (if (titularIds.isNotEmpty()) titularIds else todos.map { it.utilizadorId }.toSet())
            .toMutableSet()
        eventos
            .filter { it.tipo == "substituicao" && it.equipaId == equipaId }
            .forEach { sub ->
                sub.jogadorSaiId?.let   { campoIds.remove(it) }
                sub.jogadorEntraId?.let { campoIds.add(it) }
            }
        // Remove expelled players (red card — direct or auto from double yellow)
        eventos
            .filter { it.tipo == "vermelho" && it.equipaId == equipaId }
            .forEach { red -> red.jogadorId?.let { campoIds.remove(it) } }
        return todos.filter { it.utilizadorId in campoIds }
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    private fun startTimer(fromMinuto: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var m = fromMinuto
            _minutoVivo.value = m
            while (true) {
                delay(60_000L)
                m++
                _minutoVivo.value = m
                jogoRepository.atualizarMinuto(jogoId, m)   // persiste em Room para sobreviver a restarts
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _minutoVivo.value = null
    }

    // ── Ações ─────────────────────────────────────────────────────────────────

    fun iniciarJogo() {
        viewModelScope.launch {
            _acao.value = JogoDetalheAcao.Loading
            jogoRepository.iniciarJogo(jogoId)
                .onSuccess {
                    startTimer(0)
                    _acao.value = JogoDetalheAcao.Sucesso("Jogo iniciado!")
                }
                .onFailure { _acao.value = JogoDetalheAcao.Erro(it.message ?: "Erro") }
        }
    }

    fun terminarJogo() {
        viewModelScope.launch {
            _acao.value = JogoDetalheAcao.Loading
            stopTimer()
            val jogo = _uiState.value.jogo
            jogoRepository.terminarJogo(jogoId, jogo?.golosCasa ?: 0, jogo?.golosVisitante ?: 0)
                .onSuccess {
                    jogoRepository.recalcularClassificacao(torneioId)
                    _acao.value = JogoDetalheAcao.Sucesso("Jogo terminado!")
                }
                .onFailure { _acao.value = JogoDetalheAcao.Erro(it.message ?: "Erro") }
        }
    }

    fun definirTitulares(
        casaEquipaId: String,
        casaTitularIds: List<String>,
        visitanteEquipaId: String,
        visitanteTitularIds: List<String>
    ) {
        viewModelScope.launch {
            _acao.value = JogoDetalheAcao.Loading
            jogoRepository.definirTitulares(jogoId, buildEntradas(casaEquipaId, casaTitularIds, visitanteEquipaId, visitanteTitularIds))
                .onSuccess { _acao.value = JogoDetalheAcao.Sucesso("Titulares definidos!") }
                .onFailure { _acao.value = JogoDetalheAcao.Erro(it.message ?: "Erro") }
        }
    }

    fun definirTitularesEIniciar(
        casaEquipaId: String,
        casaTitularIds: List<String>,
        visitanteEquipaId: String,
        visitanteTitularIds: List<String>
    ) {
        viewModelScope.launch {
            _acao.value = JogoDetalheAcao.Loading
            jogoRepository.definirTitulares(jogoId, buildEntradas(casaEquipaId, casaTitularIds, visitanteEquipaId, visitanteTitularIds))
                .onSuccess {
                    jogoRepository.iniciarJogo(jogoId)
                        .onSuccess {
                            startTimer(0)
                            _acao.value = JogoDetalheAcao.Sucesso("Jogo iniciado!")
                        }
                        .onFailure { _acao.value = JogoDetalheAcao.Erro(it.message ?: "Erro ao iniciar jogo") }
                }
                .onFailure { _acao.value = JogoDetalheAcao.Erro(it.message ?: "Erro ao definir titulares") }
        }
    }

    private fun buildEntradas(
        casaEquipaId: String,
        casaTitularIds: List<String>,
        visitanteEquipaId: String,
        visitanteTitularIds: List<String>
    ): List<ConvocatoriaEntrada> {
        val state = _uiState.value
        return buildList {
            state.casaJogadores.forEach { j ->
                add(ConvocatoriaEntrada(j.utilizadorId, casaEquipaId, j.utilizadorId in casaTitularIds))
            }
            state.visitanteJogadores.forEach { j ->
                add(ConvocatoriaEntrada(j.utilizadorId, visitanteEquipaId, j.utilizadorId in visitanteTitularIds))
            }
        }
    }

    fun registarEvento(
        tipo: String,
        minuto: Int,
        equipaId: String?,
        jogadorId: String? = null,
        jogadorSaiId: String? = null,
        jogadorEntraId: String? = null,
        assistenciaId: String? = null
    ) {
        viewModelScope.launch {
            _acao.value = JogoDetalheAcao.Loading
            jogoRepository.registarEvento(jogoId, tipo, minuto, equipaId, jogadorId, jogadorSaiId, jogadorEntraId, assistenciaId)
                .onSuccess { _acao.value = JogoDetalheAcao.Sucesso("Evento registado!") }
                .onFailure { _acao.value = JogoDetalheAcao.Erro(it.message ?: "Erro") }
        }
    }

    fun resetAcao() { _acao.value = JogoDetalheAcao.Idle }
}
