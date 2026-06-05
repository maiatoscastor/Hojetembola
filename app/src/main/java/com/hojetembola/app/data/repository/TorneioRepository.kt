package com.hojetembola.app.data.repository

import com.hojetembola.app.data.local.dao.TorneioDao
import com.hojetembola.app.data.local.entity.TorneioEntity
import com.hojetembola.app.data.remote.dto.TorneioDto
import com.hojetembola.app.data.remote.dto.toDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorneioRepository @Inject constructor(
    private val client: SupabaseClient,
    private val torneioDao: TorneioDao
) {

    // ── Leitura local (Room) ──────────────────────────────────────────────────

    fun getMeusTorneios(utilizadorId: String): Flow<List<TorneioEntity>> =
        torneioDao.getMeusTorneios(utilizadorId)

    fun getTorneiosPublicos(): Flow<List<TorneioEntity>> =
        torneioDao.getPublicos()

    // ── Sincronização com Supabase ────────────────────────────────────────────

    /**
     * Faz pull dos torneios do Supabase e actualiza o Room.
     * Busca: (1) organizados pelo utilizador; (2) todos os públicos.
     * Falha silenciosa — a app usa sempre os dados do Room.
     */
    suspend fun syncTorneios(utilizadorId: String) {
        try {
            val organizados = client.from("torneio")
                .select { filter { eq("organizador_id", utilizadorId) } }
                .decodeList<TorneioDto>()

            val publicos = client.from("torneio")
                .select { filter { eq("publico", true) } }
                .decodeList<TorneioDto>()

            val todos = (organizados + publicos)
                .distinctBy { it.id }
                .map { it.toEntity() }

            if (todos.isNotEmpty()) torneioDao.insertAll(todos)
        } catch (_: Exception) {
            // Sem internet ou tabela não existe — usa cache Room
        }
    }

    // ── Criar torneio ─────────────────────────────────────────────────────────

    /**
     * Cria um torneio:
     *   1. Gera UUID local
     *   2. Insere no Room imediatamente (isSynced = false)
     *   3. Tenta sincronizar com Supabase
     *   4. Marca como sincronizado ou deixa para o SyncWorker
     */
    suspend fun criarTorneio(
        nome: String,
        modalidade: String,
        numJogadoresPersonalizado: Int?,
        formato: String,
        maxEquipas: Int,
        maxJogadoresPorEquipa: Int,
        dataInicioInscricoes: String,
        dataFimInscricoes: String,
        dataInicio: String,
        dataFimPrevista: String,
        localizacao: String,
        localizacaoLink: String?,
        criterioDesempate: String,
        amarelasParaSuspensao: Int,
        publico: Boolean,
        permitirEspectadores: Boolean,
        votacaoMvpAtiva: Boolean,
        regulamento: String?,
        organizadorId: String
    ): Result<TorneioEntity> {
        val id = UUID.randomUUID().toString()
        val estado = calcularEstadoInicial(dataInicioInscricoes)

        val entity = TorneioEntity(
            id                        = id,
            nome                      = nome,
            modalidade                = modalidade,
            numJogadoresPersonalizado = numJogadoresPersonalizado,
            formato                   = formato,
            maxEquipas                = maxEquipas,
            maxJogadoresPorEquipa     = maxJogadoresPorEquipa,
            dataInicioInscricoes      = dataInicioInscricoes,
            dataFimInscricoes         = dataFimInscricoes,
            dataInicio                = dataInicio,
            dataFimPrevista           = dataFimPrevista,
            localizacao               = localizacao,
            localizacaoLink           = localizacaoLink?.ifBlank { null },
            estado                    = estado,
            organizadorId             = organizadorId,
            publico                   = publico,
            permitirEspectadores      = permitirEspectadores,
            votacaoMvpAtiva           = votacaoMvpAtiva,
            amarelasParaSuspensao     = amarelasParaSuspensao,
            criterioDesempate         = criterioDesempate,
            regulamento               = regulamento?.ifBlank { null },
            isSynced                  = false
        )

        // Persiste localmente primeiro
        torneioDao.insert(entity)

        // Tenta sincronizar
        return try {
            client.from("torneio").insert(entity.toDto())
            torneioDao.markAsSynced(id)
            Result.success(entity.copy(isSynced = true))
        } catch (_: Exception) {
            // Fica em fila para o SyncWorker
            Result.success(entity)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun calcularEstadoInicial(dataInicioInscricoes: String): String {
        return try {
            val hoje = java.time.LocalDate.now().toString()   // YYYY-MM-DD
            if (dataInicioInscricoes <= hoje) "inscricoes_abertas" else "criado"
        } catch (_: Exception) {
            "criado"
        }
    }
}
