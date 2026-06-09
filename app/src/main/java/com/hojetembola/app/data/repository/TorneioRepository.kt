package com.hojetembola.app.data.repository

import android.util.Log
import com.hojetembola.app.data.local.dao.TorneioDao
import com.hojetembola.app.data.local.entity.TorneioEntity
import com.hojetembola.app.data.remote.dto.TorneioDto
import com.hojetembola.app.data.remote.dto.toInsertDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Usado apenas para verificar existência de código de acesso no Supabase */
@Serializable
private data class CodigoCheck(val id: Int)

private const val TAG = "HTB-TorneioRepo"

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

    fun getTorneioById(id: String): Flow<TorneioEntity?> =
        torneioDao.getByIdFlow(id)

    /** Versão suspend — usada para validação pontual (sem observar mudanças). */
    suspend fun getTorneioByIdSuspend(id: String): TorneioEntity? =
        torneioDao.getById(id)

    /**
     * Tenta localizar um torneio privado pelo código de acesso de 4 dígitos.
     * Verifica primeiro o Room; se não encontrar, tenta o Supabase e insere localmente.
     * Devolve Result.failure se o código não existir ou houver erro de rede.
     */
    suspend fun buscarPorCodigo(codigo: String): Result<TorneioEntity> {
        // 1. Verificação local
        torneioDao.getByCodigoAcesso(codigo)?.let { return Result.success(it) }

        // 2. Verificação remota
        return try {
            val remoto = client.from("torneio")
                .select { filter { eq("codigo_acesso", codigo) } }
                .decodeList<TorneioDto>()
                .firstOrNull()
                ?: return Result.failure(Exception("Código inválido. Verifica se está correto."))

            val entity = remoto.toEntity()
            torneioDao.insert(entity)
            Result.success(entity)
        } catch (_: Exception) {
            Result.failure(Exception("Sem ligação à internet. Verifica a tua rede."))
        }
    }

    // ── Sincronização com Supabase ────────────────────────────────────────────

    /**
     * Faz pull dos torneios do Supabase e actualiza o Room.
     * Busca: (1) organizados pelo utilizador; (2) todos os públicos.
     * Falha silenciosa — a app usa sempre os dados do Room.
     */
    suspend fun syncTorneios(utilizadorId: String) {
        try {
            // 1. Torneios do próprio utilizador
            val organizados = client.from("torneio")
                .select { filter { eq("organizador_id", utilizadorId) } }
                .decodeList<TorneioDto>()
                .map { it.toEntity() }

            if (organizados.isNotEmpty()) {
                // Apaga as entradas já sincronizadas locais (UUID) para evitar
                // duplicados com as entradas vindas do Supabase (ID integer)
                torneioDao.deleteSyncedByOrganizadorId(utilizadorId)
                torneioDao.insertAll(organizados)
            }

            // 2. Torneios públicos de outros organizadores
            val outrosPublicos = client.from("torneio")
                .select { filter { eq("visibilidade", "Publico") } }
                .decodeList<TorneioDto>()
                .filter { it.organizadorId != utilizadorId }
                .map { it.toEntity() }

            if (outrosPublicos.isNotEmpty()) torneioDao.insertAll(outrosPublicos)

        } catch (_: Exception) {
            // Sem internet — usa cache Room
        }
    }

    // ── Código de acesso único ────────────────────────────────────────────────

    /**
     * Gera um código de acesso de 4 dígitos que não existe nem localmente (Room)
     * nem no Supabase. Tenta até 20 vezes; se offline, aceita apenas verificação local.
     */
    suspend fun gerarCodigoUnico(): String {
        repeat(20) {
            val code = String.format("%04d", (0..9999).random())

            // 1. Verificação local (Room)
            if (torneioDao.countByCodigoAcesso(code) > 0) return@repeat

            // 2. Verificação remota (Supabase)
            try {
                val rows = client.from("torneio")
                    .select { filter { eq("codigo_acesso", code) } }
                    .decodeList<CodigoCheck>()
                if (rows.isEmpty()) return code
            } catch (_: Exception) {
                // Offline ou erro de rede — usa só verificação local
                return code
            }
        }
        // Fallback improvável (só com >9000 torneios privados activos)
        return String.format("%04d", (0..9999).random())
    }

    // ── Criar torneio ─────────────────────────────────────────────────────────

    /**
     * Cria um torneio:
     *   1. Gera UUID local
     *   2. Insere no Room imediatamente (isSynced = false)
     *   3. Tenta sincronizar com Supabase usando TorneioInsertDto
     *   4. Marca como sincronizado ou devolve erro ao caller
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
        localizacaoNome: String,
        localizacaoMorada: String?,
        localizacaoMapsUrl: String?,
        criterioDesempate: String,
        tempoExtraMinutos: Int,
        amarelasParaSuspensao: Int,
        visibilidade: String,
        codigoAcesso: String?,
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
            localizacaoNome           = localizacaoNome,
            localizacaoMorada         = localizacaoMorada,
            localizacaoMapsUrl        = localizacaoMapsUrl,
            estado                    = estado,
            organizadorId             = organizadorId,
            visibilidade              = visibilidade,
            permitirEspectadores      = permitirEspectadores,
            votacaoMvpAtiva           = votacaoMvpAtiva,
            amarelasParaSuspensao     = amarelasParaSuspensao,
            criterioDesempate         = criterioDesempate,
            tempoExtraMinutos         = tempoExtraMinutos,
            codigoAcesso              = codigoAcesso?.ifBlank { null },
            regulamento               = regulamento?.ifBlank { null },
            isSynced                  = false
        )

        // Persiste localmente primeiro (sempre — garante funcionamento offline)
        torneioDao.insert(entity)

        // Sincroniza com Supabase — propaga o erro se falhar
        return try {
            client.from("torneio").insert(entity.toInsertDto())
            torneioDao.markAsSynced(id)
            Log.i(TAG, "Torneio ${entity.id} sincronizado com Supabase com sucesso.")
            Result.success(entity.copy(isSynced = true))
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao inserir torneio no Supabase: ${e::class.simpleName} — ${e.message}", e)
            Result.failure(
                Exception(buildSupabaseErrorMessage(e))
            )
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildSupabaseErrorMessage(e: Exception): String {
        val raw = e.message ?: "erro desconhecido"
        return when {
            raw.contains("column", ignoreCase = true) &&
            raw.contains("does not exist", ignoreCase = true) ->
                "Coluna em falta na BD do Supabase.\nDetalhes técnicos (Logcat → HTB-TorneioRepo):\n$raw"

            raw.contains("invalid input value for enum", ignoreCase = true) ->
                "Valor inválido para ENUM do Supabase. Verifica os valores de modalidade/formato/estado.\nDetalhe: $raw"

            raw.contains("invalid input syntax for type integer", ignoreCase = true) ->
                "Tipo errado no campo organizador_id. O Supabase espera um integer mas recebeu um UUID.\n" +
                "Verifica se a coluna organizador_id na tabela torneio é UUID ou integer."

            raw.contains("row-level security", ignoreCase = true) ||
            raw.contains("new row violates", ignoreCase = true) ||
            raw.contains("permission denied", ignoreCase = true) ->
                "Sem permissão para inserir na tabela 'torneio'.\nVerifica as RLS Policies no Supabase."

            raw.contains("foreign key", ignoreCase = true) ||
            raw.contains("violates foreign key", ignoreCase = true) ->
                "O teu utilizador não existe na tabela 'utilizador' do Supabase.\n" +
                "Verifica se o trigger de criação de utilizador está ativo."

            raw.contains("Unable to resolve host", ignoreCase = true) ||
            raw.contains("timeout", ignoreCase = true) ||
            raw.contains("network", ignoreCase = true) ->
                "Sem ligação ao Supabase. Verifica a internet."

            raw.contains("JWT", ignoreCase = true) ||
            raw.contains("token", ignoreCase = true) ->
                "Sessão expirada. Faz logout e login novamente."

            else ->
                "Erro Supabase: $raw"
        }
    }

    private fun calcularEstadoInicial(dataInicioInscricoes: String): String {
        return try {
            val hoje = java.time.LocalDate.now().toString()   // YYYY-MM-DD
            if (dataInicioInscricoes <= hoje) "InscricoesAbertas" else "Criado"
        } catch (_: Exception) {
            "Criado"
        }
    }
}
