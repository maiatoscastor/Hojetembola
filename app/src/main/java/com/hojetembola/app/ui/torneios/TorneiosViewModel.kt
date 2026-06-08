package com.hojetembola.app.ui.torneios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hojetembola.app.data.local.entity.TorneioEntity
import com.hojetembola.app.data.local.entity.UtilizadorEntity
import com.hojetembola.app.data.repository.TorneioRepository
import com.hojetembola.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Estado UI ─────────────────────────────────────────────────────────────────

sealed class TorneiosUiState {
    object Loading : TorneiosUiState()
    data class Content(
        val utilizador: UtilizadorEntity,
        val meusTorneios: List<TorneioEntity>,
        val torneiosPublicos: List<TorneioEntity>,
        /** "todos" = tab Torneios, "meus" = tab Os meus */
        val tabAtiva: String = "todos"
    ) : TorneiosUiState()
    data class Error(val message: String) : TorneiosUiState()
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class TorneiosViewModel @Inject constructor(
    private val torneioRepository: TorneioRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _filtro  = MutableStateFlow("todos")
    private val _pesquisa = MutableStateFlow("")
    private val _zona    = MutableStateFlow("")
    private val _tabAtiva = MutableStateFlow("todos")   // "todos" | "meus"

    val filtro:   StateFlow<String> = _filtro.asStateFlow()
    val pesquisa: StateFlow<String> = _pesquisa.asStateFlow()
    val tabAtiva: StateFlow<String> = _tabAtiva.asStateFlow()

    // Estado base (antes dos filtros)
    private val _baseState = MutableStateFlow<TorneiosUiState>(TorneiosUiState.Loading)

    // Estado final exposto ao Fragment (aplica filtros + tab)
    val uiState: StateFlow<TorneiosUiState> = combine(
        _baseState, _filtro, _pesquisa, _zona, _tabAtiva
    ) { base, filtro, pesquisa, zona, tab ->
        when (base) {
            is TorneiosUiState.Content -> base.filtered(filtro, pesquisa, zona).copy(tabAtiva = tab)
            else -> base
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, TorneiosUiState.Loading)

    init { loadTorneios() }

    // ── Carregamento ──────────────────────────────────────────────────────────

    private fun loadTorneios() {
        viewModelScope.launch {
            val userResult = userRepository.getCurrentUser()
            val utilizador = userResult.getOrElse {
                _baseState.value = TorneiosUiState.Error(it.message ?: "Sessão expirada")
                return@launch
            }

            launch { torneioRepository.syncTorneios(utilizador.id) }

            combine(
                torneioRepository.getMeusTorneios(utilizador.id),
                torneioRepository.getTorneiosPublicos()
            ) { meus, publicos ->
                val meusIds = meus.map { it.id }.toSet()
                TorneiosUiState.Content(
                    utilizador       = utilizador,
                    meusTorneios     = meus,
                    torneiosPublicos = publicos.filter { it.id !in meusIds }
                )
            }.catch { e ->
                _baseState.value = TorneiosUiState.Error(e.message ?: "Erro ao carregar")
            }.collect { _baseState.value = it }
        }
    }

    fun refresh() {
        _baseState.value = TorneiosUiState.Loading
        loadTorneios()
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    fun setFiltro(valor: String)    { _filtro.value = valor }
    fun setPesquisa(valor: String)  { _pesquisa.value = valor }
    fun setZona(valor: String)      { _zona.value = valor }
    fun setTabAtiva(valor: String)  { _tabAtiva.value = valor }

    // ── Filtragem ─────────────────────────────────────────────────────────────

    private fun TorneiosUiState.Content.filtered(
        filtro: String,
        pesquisa: String,
        zona: String
    ): TorneiosUiState.Content {
        val p = pesquisa.trim().lowercase()
        val z = zona.trim().lowercase()

        fun List<TorneioEntity>.applyFilters() = filter { t ->
            (p.isEmpty() || t.nome.lowercase().contains(p)) &&
            (z.isEmpty() || t.localizacaoNome.lowercase().contains(z)) &&
            when (filtro) {
                "a_decorrer" -> t.estado == "ADecorrer"         || t.estado == "a_decorrer"
                "inscricoes" -> t.estado == "InscricoesAbertas" || t.estado == "inscricoes_abertas"
                "publicos"   -> t.visibilidade == "Publico"     || t.visibilidade == "publico"
                "terminados" -> t.estado == "Terminado"         || t.estado == "terminado"
                else         -> true
            }
        }

        return copy(
            meusTorneios     = meusTorneios.applyFilters(),
            torneiosPublicos = torneiosPublicos.applyFilters()
        )
    }
}
