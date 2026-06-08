package com.hojetembola.app.data.remote.dto

import com.hojetembola.app.data.local.entity.TorneioEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO de leitura — espelha as colunas reais da tabela `torneio` do Supabase.
 *
 * Usado em [com.hojetembola.app.data.repository.TorneioRepository.syncTorneios]
 * para descodificar registos do Supabase e guardar em Room.
 *
 * Nota: o `id` do Supabase é integer auto-increment; é convertido para String
 * quando guardado no Room (que usa String UUID como PK).
 */
@Serializable
data class TorneioDto(
    val id: Int,
    val nome: String,

    @SerialName("organizador_id")
    val organizadorId: String,

    val modalidade: String,

    @SerialName("num_jogadores_personalizado")
    val numJogadoresPersonalizado: Int? = null,

    val formato: String,

    @SerialName("max_equipas")
    val maxEquipas: Int,

    @SerialName("max_jogadores_equipa")
    val maxJogadoresEquipa: Int,

    @SerialName("data_inicio_inscricoes")
    val dataInicioInscricoes: String,

    @SerialName("data_fim_inscricoes")
    val dataFimInscricoes: String,

    @SerialName("data_inicio")
    val dataInicio: String,

    @SerialName("data_fim_prevista")
    val dataFimPrevista: String? = null,

    @SerialName("localizacao_nome")
    val localizacaoNome: String,

    @SerialName("localizacao_morada")
    val localizacaoMorada: String? = null,

    @SerialName("localizacao_maps_url")
    val localizacaoMapsUrl: String? = null,

    @SerialName("criterio_desempate")
    val criterioDesempate: String = "Penalidades",

    @SerialName("amarelos_para_suspensao")
    val amarelos: Int = 3,

    /** ENUM Supabase visibilidade_tipo: "Publico" | "Privado" */
    val visibilidade: String = "Publico",

    @SerialName("codigo_acesso")
    val codigoAcesso: String? = null,

    @SerialName("permitir_espectadores")
    val permitirEspectadores: Boolean = true,

    @SerialName("votacao_mvp_ativa")
    val votacaoMvpAtiva: Boolean = true,

    val regulamento: String? = null,

    val estado: String = "Criado",

    @SerialName("tempo_extra_minutos")
    val tempoExtraMinutos: Int = 10,

    val sincronizado: Boolean = true
) {
    fun toEntity() = TorneioEntity(
        id                        = id.toString(),
        nome                      = nome,
        modalidade                = modalidade,
        numJogadoresPersonalizado = numJogadoresPersonalizado,
        formato                   = formato,
        maxEquipas                = maxEquipas,
        maxJogadoresPorEquipa     = maxJogadoresEquipa,
        dataInicioInscricoes      = dataInicioInscricoes,
        dataFimInscricoes         = dataFimInscricoes,
        dataInicio                = dataInicio,
        dataFimPrevista           = dataFimPrevista ?: "",
        localizacaoNome           = localizacaoNome,
        localizacaoMorada         = localizacaoMorada,
        localizacaoMapsUrl        = localizacaoMapsUrl,
        estado                    = estado,
        organizadorId             = organizadorId,
        visibilidade              = visibilidade,
        permitirEspectadores      = permitirEspectadores,
        votacaoMvpAtiva           = votacaoMvpAtiva,
        amarelasParaSuspensao     = amarelos,
        criterioDesempate         = criterioDesempate,
        tempoExtraMinutos         = tempoExtraMinutos,
        codigoAcesso              = codigoAcesso,
        regulamento               = regulamento,
        isSynced                  = sincronizado
    )
}
