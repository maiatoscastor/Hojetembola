package com.hojetembola.app.ui.torneios

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hojetembola.app.data.local.dao.EquipaDao
import com.hojetembola.app.data.local.entity.ClassificacaoEntity
import com.hojetembola.app.data.repository.JogoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClassificacaoTorneioRow(
    val posicao: Int,
    val equipaNome: String,
    val equipaIniciais: String,
    val equipaCor: String,
    val jogos: Int,
    val vitorias: Int,
    val empates: Int,
    val derrotas: Int,
    val golosMarcados: Int,
    val golosSofridos: Int,
    val pontos: Int
)

@HiltViewModel
class ClassificacaoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val jogoRepository: JogoRepository,
    private val equipaDao: EquipaDao
) : ViewModel() {

    val torneioId: String = checkNotNull(savedStateHandle["torneioId"])
    val torneioNome: String = savedStateHandle["torneioNome"] ?: ""

    private val _rows = MutableStateFlow<List<ClassificacaoTorneioRow>>(emptyList())
    val rows: StateFlow<List<ClassificacaoTorneioRow>> = _rows.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            jogoRepository.syncJogosETorneio(torneioId)
            jogoRepository.recalcularClassificacao(torneioId)
            jogoRepository.getClassificacao(torneioId).collect { list ->
                _loading.value = false
                _rows.value = list.mapNotNull { entry ->
                    val equipa = equipaDao.getById(entry.equipaId) ?: return@mapNotNull null
                    ClassificacaoTorneioRow(
                        posicao = entry.posicao,
                        equipaNome = equipa.nome,
                        equipaIniciais = equipa.iniciais,
                        equipaCor = equipa.corAvatar,
                        jogos = entry.jogos,
                        vitorias = entry.vitorias,
                        empates = entry.empates,
                        derrotas = entry.derrotas,
                        golosMarcados = entry.golosMarcados,
                        golosSofridos = entry.golosSofridos,
                        pontos = entry.pontos
                    )
                }
            }
        }
    }
}
