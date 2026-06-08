package com.hojetembola.app.ui.rankings

import androidx.lifecycle.ViewModel
import com.hojetembola.app.data.repository.RankingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

sealed class RankingsUiState {
    object Loading : RankingsUiState()
    data class Content(val data: RankingsContent) : RankingsUiState()
}

@HiltViewModel
class RankingsViewModel @Inject constructor(
    private val repository: RankingsRepository
) : ViewModel() {

    private val _scope = MutableStateFlow(RankingScope.GERAL)
    private val _metric = MutableStateFlow(RankingMetric.GOLOS)
    private val _period = MutableStateFlow(RankingPeriod.ESTA_JORNADA)
    private val _ready = MutableStateFlow(false)

    val uiState: StateFlow<RankingsUiState> = combine(
        _scope, _metric, _period, _ready
    ) { scope, metric, period, ready ->
        if (!ready) {
            RankingsUiState.Loading
        } else {
            RankingsUiState.Content(repository.getRankings(scope, metric, period))
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, RankingsUiState.Loading)

    init {
        _ready.value = true
    }

    fun setScope(scope: RankingScope) {
        _scope.update { scope }
        _metric.update { defaultMetricFor(scope) }
        _period.update { defaultPeriodFor(scope) }
    }

    fun setMetric(metric: RankingMetric) {
        _metric.value = metric
    }

    fun setPeriod(period: RankingPeriod) {
        _period.value = period
    }

    fun metricsForScope(scope: RankingScope): List<RankingMetric> = when (scope) {
        RankingScope.GERAL -> listOf(
            RankingMetric.GOLOS,
            RankingMetric.JOGOS,
            RankingMetric.ASSISTENCIAS,
            RankingMetric.MINUTOS,
            RankingMetric.AMARELOS,
            RankingMetric.VERMELHOS,
            RankingMetric.MVPS
        )
        RankingScope.TORNEIO -> listOf(
            RankingMetric.GOLOS,
            RankingMetric.ASSISTENCIAS,
            RankingMetric.MVPS,
            RankingMetric.CARTOES,
            RankingMetric.CLASSIFICACAO
        )
    }

    fun periodsForScope(scope: RankingScope): List<RankingPeriod> = when (scope) {
        RankingScope.GERAL -> listOf(
            RankingPeriod.ESTA_JORNADA,
            RankingPeriod.ESTE_MES,
            RankingPeriod.EPOCA_TODA
        )
        RankingScope.TORNEIO -> listOf(
            RankingPeriod.JORNADA_ATUAL,
            RankingPeriod.ESTE_MES,
            RankingPeriod.TORNEIO_COMPLETO
        )
    }

    private fun defaultMetricFor(scope: RankingScope): RankingMetric =
        RankingMetric.GOLOS

    private fun defaultPeriodFor(scope: RankingScope): RankingPeriod = when (scope) {
        RankingScope.GERAL -> RankingPeriod.ESTA_JORNADA
        RankingScope.TORNEIO -> RankingPeriod.JORNADA_ATUAL
    }
}
