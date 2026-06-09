package com.hojetembola.app.ui.notificacoes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hojetembola.app.data.local.entity.ConviteEntity
import com.hojetembola.app.data.local.entity.NotificacaoEntity
import com.hojetembola.app.data.repository.ConviteRepository
import com.hojetembola.app.data.repository.NotificacaoRepository
import com.hojetembola.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Item unificado que representa quer uma notificação quer um convite pendente. */
sealed class NotificacaoItem {
    data class Convite(val convite: ConviteEntity) : NotificacaoItem()
    data class Notificacao(val notificacao: NotificacaoEntity) : NotificacaoItem()
}

data class NotificacoesUiState(
    val items: List<NotificacaoItem> = emptyList(),
    val isLoading: Boolean = true,
    val mensagemAcao: String? = null
)

@HiltViewModel
class NotificacoesViewModel @Inject constructor(
    private val notificacaoRepository: NotificacaoRepository,
    private val conviteRepository: ConviteRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificacoesUiState())
    val uiState: StateFlow<NotificacoesUiState> = _uiState.asStateFlow()

    private var utilizadorId: String = ""

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val userResult = userRepository.getCurrentUser()
            val user = userResult.getOrElse {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            utilizadorId = user.id

            // Sincroniza dados remotos
            notificacaoRepository.syncNotificacoes(utilizadorId)
            conviteRepository.syncConvitesRecebidos(utilizadorId)

            // Combina notificações e convites pendentes num Flow único
            combine(
                notificacaoRepository.getNotificacoes(utilizadorId),
                conviteRepository.getConvitesPendentesPorConvidado(utilizadorId)
            ) { notificacoes, convites ->
                val conviteItems = convites.map { NotificacaoItem.Convite(it) }
                val notifItems   = notificacoes.map { NotificacaoItem.Notificacao(it) }
                // Convites pendentes primeiro, depois notificações por data
                conviteItems + notifItems
            }.collect { items ->
                _uiState.update { it.copy(items = items, isLoading = false) }
            }
        }
    }

    fun aceitarConvite(conviteId: String) {
        viewModelScope.launch {
            conviteRepository.aceitarConvite(conviteId)
                .onSuccess {
                    _uiState.update { it.copy(mensagemAcao = "Convite aceite! Já és membro da equipa.") }
                    // Re-sincroniza notificações — o trigger DB criou uma nova para este utilizador
                    if (utilizadorId.isNotEmpty()) notificacaoRepository.syncNotificacoes(utilizadorId)
                }
                .onFailure { e -> _uiState.update { it.copy(mensagemAcao = e.message) } }
        }
    }

    fun recusarConvite(conviteId: String) {
        viewModelScope.launch {
            conviteRepository.recusarConvite(conviteId)
                .onSuccess {
                    _uiState.update { it.copy(mensagemAcao = "Convite recusado.") }
                    // Re-sincroniza notificações — o trigger DB criou uma nova para este utilizador
                    if (utilizadorId.isNotEmpty()) notificacaoRepository.syncNotificacoes(utilizadorId)
                }
                .onFailure { e -> _uiState.update { it.copy(mensagemAcao = e.message) } }
        }
    }

    fun marcarComoLida(notificacaoId: String) {
        viewModelScope.launch {
            notificacaoRepository.marcarComoLida(notificacaoId)
        }
    }

    fun marcarTodasComoLidas() {
        if (utilizadorId.isEmpty()) return
        viewModelScope.launch {
            notificacaoRepository.marcarTodasComoLidas(utilizadorId)
        }
    }

    fun consumirMensagemAcao() {
        _uiState.update { it.copy(mensagemAcao = null) }
    }
}
