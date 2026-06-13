package com.hojetembola.app.ui.torneios

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hojetembola.app.data.local.dao.EventoJogoDao
import com.hojetembola.app.data.local.dao.JogadorInscricaoComNome
import com.hojetembola.app.data.local.dao.VotoMvpDao
import com.hojetembola.app.data.local.entity.VotoMvpEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

data class MvpCandidato(
    val utilizadorId: String,
    val nome: String,
    val iniciais: String,
    val avatarColor: String,
    val golosNoJogo: Int,
    val assistenciasNoJogo: Int
)

data class VotarMvpUiState(
    val loading: Boolean = true,
    val casaNome: String = "",
    val visitanteNome: String = "",
    val casaJogadores: List<MvpCandidato> = emptyList(),
    val visitanteJogadores: List<MvpCandidato> = emptyList(),
    val votosJogadores: Map<String, Int> = emptyMap(),
    val votosPublico: Map<String, Int> = emptyMap(),
    val votosOrganizador: Map<String, Int> = emptyMap(),
    val meuVotoJogadorId: String? = null,
    val meuVotoPublicoId: String? = null,
    val meuVotoOrgId: String? = null,
    val tipoVotante: String = "publico",
    val isOrganizador: Boolean = false,
    val votacaoAtiva: Boolean = false
)

@HiltViewModel
class VotarMvpViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val votoMvpDao: VotoMvpDao,
    private val eventoJogoDao: EventoJogoDao,
    private val client: SupabaseClient
) : ViewModel() {

    val jogoId: String = checkNotNull(savedStateHandle["jogoId"])

    private val _uiState = MutableStateFlow(VotarMvpUiState())
    val uiState: StateFlow<VotarMvpUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    fun init(
        casaNome: String,
        visitanteNome: String,
        isOrganizador: Boolean,
        votacaoAtiva: Boolean,
        casaJogadores: List<JogadorInscricaoComNome>,
        visitanteJogadores: List<JogadorInscricaoComNome>
    ) {
        viewModelScope.launch {
            currentUserId = client.auth.currentSessionOrNull()?.user?.id

            val casaBase = casaJogadores.map { j ->
                MvpCandidato(
                    utilizadorId      = j.utilizadorId,
                    nome              = j.nome,
                    iniciais          = j.nome.toMvpInitials(),
                    avatarColor       = avatarColorForId(j.utilizadorId),
                    golosNoJogo       = eventoJogoDao.countGolosNoJogo(j.utilizadorId, jogoId),
                    assistenciasNoJogo = eventoJogoDao.countAssistenciasNoJogo(j.utilizadorId, jogoId)
                )
            }
            val visitanteBase = visitanteJogadores.map { j ->
                MvpCandidato(
                    utilizadorId      = j.utilizadorId,
                    nome              = j.nome,
                    iniciais          = j.nome.toMvpInitials(),
                    avatarColor       = avatarColorForId(j.utilizadorId),
                    golosNoJogo       = eventoJogoDao.countGolosNoJogo(j.utilizadorId, jogoId),
                    assistenciasNoJogo = eventoJogoDao.countAssistenciasNoJogo(j.utilizadorId, jogoId)
                )
            }

            val allIds = (casaBase + visitanteBase).map { it.utilizadorId }
            val tipoVotante = when {
                isOrganizador                     -> "organizador"
                allIds.contains(currentUserId)    -> "jogador"
                else                              -> "publico"
            }

            // Sync votos do Supabase → Room para que todos os utilizadores vejam os votos uns dos outros
            val jogoIdInt = jogoId.toIntOrNull()
            launch {
                try {
                    val remotos = if (jogoIdInt != null) {
                        client.from("voto_mvp")
                            .select { filter { eq("jogo_id", jogoIdInt) } }
                            .decodeList<VotoMvpReadDto>()
                    } else emptyList()

                    remotos.forEach { dto ->
                        val localId = "${dto.jogoId}_${dto.votanteId}_${dto.tipoVotante}"
                        votoMvpDao.insert(
                            VotoMvpEntity(localId, dto.jogoId.toString(), dto.votanteId, dto.votadoId, dto.tipoVotante)
                        )
                    }
                } catch (e: Exception) {
                    Log.w("VotarMvpViewModel", "sync fetch failed: ${e.message}")
                }
            }

            combine(
                votoMvpDao.getByJogoETipo(jogoId, "jogador"),
                votoMvpDao.getByJogoETipo(jogoId, "publico"),
                votoMvpDao.getByJogoETipo(jogoId, "organizador")
            ) { votosJog, votosPub, votosOrg ->
                val uid = currentUserId
                VotarMvpUiState(
                    loading            = false,
                    casaNome           = casaNome,
                    visitanteNome      = visitanteNome,
                    casaJogadores      = casaBase,
                    visitanteJogadores = visitanteBase,
                    votosJogadores     = votosJog.groupBy { it.votadoId }.mapValues { it.value.size },
                    votosPublico       = votosPub.groupBy { it.votadoId }.mapValues { it.value.size },
                    votosOrganizador   = votosOrg.groupBy { it.votadoId }.mapValues { it.value.size },
                    meuVotoJogadorId   = votosJog.find { it.votanteId == uid }?.votadoId,
                    meuVotoPublicoId   = votosPub.find { it.votanteId == uid }?.votadoId,
                    meuVotoOrgId       = votosOrg.find { it.votanteId == uid }?.votadoId,
                    tipoVotante        = tipoVotante,
                    isOrganizador      = isOrganizador,
                    votacaoAtiva       = votacaoAtiva
                )
            }.collect { _uiState.value = it }
        }
    }

    fun votar(votadoId: String, tipoVotante: String) {
        val userId = currentUserId ?: return
        // id local determinístico: mesmo voto de mesmo utilizador+tipo sempre tem o mesmo id em Room
        val localId = "${jogoId}_${userId}_${tipoVotante}"
        viewModelScope.launch {
            val existing = votoMvpDao.getVotoDoUtilizadorETipo(jogoId, userId, tipoVotante)
            when {
                existing != null && existing.votadoId == votadoId -> {
                    // Mesmo jogador → desfazer voto
                    votoMvpDao.delete(existing)
                    syncDeleteRemoto(userId, tipoVotante)
                }
                else -> {
                    // Novo voto ou mudança de jogador
                    val voto = VotoMvpEntity(localId, jogoId, userId, votadoId, tipoVotante)
                    votoMvpDao.insert(voto)
                    syncEscreverRemoto(userId, votadoId, tipoVotante)
                }
            }
        }
    }

    private fun syncEscreverRemoto(votanteId: String, votadoId: String, tipoVotante: String) {
        val jogoIdInt = jogoId.toIntOrNull() ?: return
        viewModelScope.launch {
            try {
                // Delete primeiro (pode não existir, é ignorado), depois insert fresco
                client.from("voto_mvp").delete {
                    filter {
                        eq("jogo_id", jogoIdInt)
                        eq("votante_id", votanteId)
                        eq("tipo_votante", tipoVotante)
                    }
                }
                client.from("voto_mvp").insert(
                    VotoMvpWriteDto(jogoIdInt, votanteId, votadoId, tipoVotante)
                )
                Log.d("VotarMvpViewModel", "sync write OK: jogo=$jogoIdInt tipo=$tipoVotante")
            } catch (e: Exception) {
                Log.w("VotarMvpViewModel", "sync write failed: ${e.message}")
            }
        }
    }

    private fun syncDeleteRemoto(votanteId: String, tipoVotante: String) {
        val jogoIdInt = jogoId.toIntOrNull() ?: return
        viewModelScope.launch {
            try {
                client.from("voto_mvp").delete {
                    filter {
                        eq("jogo_id", jogoIdInt)
                        eq("votante_id", votanteId)
                        eq("tipo_votante", tipoVotante)
                    }
                }
                Log.d("VotarMvpViewModel", "sync delete OK: jogo=$jogoIdInt tipo=$tipoVotante")
            } catch (e: Exception) {
                Log.w("VotarMvpViewModel", "sync delete failed: ${e.message}")
            }
        }
    }
}

private val AVATAR_COLORS = listOf(
    "#3498DB", "#E84C3D", "#2ECC71", "#E67E22", "#1ABC9C",
    "#D35400", "#8E44AD", "#16A085", "#F39C12", "#C0392B"
)

fun avatarColorForId(id: String): String =
    AVATAR_COLORS[Math.abs(id.hashCode()) % AVATAR_COLORS.size]

fun String.toMvpInitials(): String =
    split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")

/** DTO de leitura — o id é int4 auto-gerado pelo Supabase; jogo_id é int4 FK */
@Serializable
private data class VotoMvpReadDto(
    @SerialName("id")           val id: Int,
    @SerialName("jogo_id")      val jogoId: Int,
    @SerialName("votante_id")   val votanteId: String,
    @SerialName("votado_id")    val votadoId: String,
    @SerialName("tipo_votante") val tipoVotante: String
)

/** DTO de escrita — sem id (Supabase gera o int4 automaticamente); jogo_id é int4 */
@Serializable
private data class VotoMvpWriteDto(
    @SerialName("jogo_id")      val jogoId: Int,
    @SerialName("votante_id")   val votanteId: String,
    @SerialName("votado_id")    val votadoId: String,
    @SerialName("tipo_votante") val tipoVotante: String
)
