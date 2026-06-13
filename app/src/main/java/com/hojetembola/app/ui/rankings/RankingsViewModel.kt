package com.hojetembola.app.ui.rankings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hojetembola.app.data.local.entity.TorneioEntity
import com.hojetembola.app.data.repository.RankingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RankingsUiState {
    object Loading : RankingsUiState()
    data class Content(val data: RankingsContent) : RankingsUiState()
}

@HiltViewModel
class RankingsViewModel @Inject constructor(
    private val repository: RankingsRepository
) : ViewModel() {

    private val _scope             = MutableStateFlow(RankingScope.GERAL)
    private val _metric            = MutableStateFlow(RankingMetric.GOLOS)
    private val _period            = MutableStateFlow(RankingPeriod.EPOCA_TODA)
    private val _selectedTorneioId = MutableStateFlow<String?>(null)

    private val _torneios = MutableStateFlow<List<TorneioEntity>>(emptyList())
    val torneios: StateFlow<List<TorneioEntity>> = _torneios

    val uiState: StateFlow<RankingsUiState> =
        combine(_scope, _metric, _selectedTorneioId) { s, m, tid -> Triple(s, m, tid) }
            .flatMapLatest { (scope, metric, torneioId) ->
                flow {
                    emit(RankingsUiState.Loading)
                    emit(RankingsUiState.Content(repository.getRankings(scope, metric, torneioId)))
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RankingsUiState.Loading)

    init {
        loadTorneios()
    }

    private fun loadTorneios() {
        viewModelScope.launch {
            repository.getMeusTorneios().collect { list ->
                _torneios.value = list
                // auto-select the first one if nothing selected yet
                if (_selectedTorneioId.value == null && list.isNotEmpty()) {
                    _selectedTorneioId.value = list.first().id
                }
            }
        }
    }

    fun setScope(scope: RankingScope) {
        _scope.update { scope }
        _metric.update { RankingMetric.GOLOS }
        _period.update { defaultPeriodFor(scope) }
    }

    fun setMetric(metric: RankingMetric) { _metric.value = metric }
    fun setPeriod(period: RankingPeriod) { _period.value = period }
    fun setTorneio(id: String)           { _selectedTorneioId.value = id }

    fun metricsForScope(scope: RankingScope): List<RankingMetric> = when (scope) {
        RankingScope.GERAL -> listOf(
            RankingMetric.GOLOS, RankingMetric.JOGOS, RankingMetric.ASSISTENCIAS,
            RankingMetric.AMARELOS, RankingMetric.VERMELHOS, RankingMetric.MVPS
        )
        RankingScope.TORNEIO -> listOf(
            RankingMetric.GOLOS, RankingMetric.ASSISTENCIAS, RankingMetric.MVPS,
            RankingMetric.CARTOES, RankingMetric.CLASSIFICACAO
        )
    }

    fun periodsForScope(scope: RankingScope): List<RankingPeriod> = when (scope) {
        RankingScope.GERAL   -> listOf(RankingPeriod.EPOCA_TODA)
        RankingScope.TORNEIO -> listOf(RankingPeriod.TORNEIO_COMPLETO)
    }

    private fun defaultPeriodFor(scope: RankingScope) = when (scope) {
        RankingScope.GERAL   -> RankingPeriod.EPOCA_TODA
        RankingScope.TORNEIO -> RankingPeriod.TORNEIO_COMPLETO
    }
}
