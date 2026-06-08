package com.hojetembola.app.data.remote.dto

import com.hojetembola.app.data.local.entity.TorneioEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO usado exclusivamente para INSERT no Supabase.
 *
 * Não contém o campo `id` — a base de dados usa um integer auto-increment
 * e gera o PK automaticamente.
 *
 * Nomes dos campos estão alinhados com as colunas reais da tabela `torneio`
 * no Supabase (snake_case, sem discrepâncias).
 */
@Serializable
data class TorneioInsertDto(
    val nome: String,

    @SerialName("organizador_id")
    val organizadorId: String,

    val modalidade: String,

    @SerialName("num_jogadores_personalizado")
    val numJogadoresPersonalizado: Int? = null,

    val formato: String,

    @SerialName("max_equipas")
    val maxEquipas: Int,

    /** Coluna real: max_jogadores_equipa */
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

    /** Nome do campo / instalação */
    @SerialName("localizacao_nome")
    val localizacaoNome: String,

    @SerialName("localizacao_morada")
    val localizacaoMorada: String? = null,

    @SerialName("localizacao_maps_url")
    val localizacaoMapsUrl: String? = null,

    @SerialName("criterio_desempate")
    val criterioDesempate: String,

    /** Coluna real: amarelos_para_suspensao */
    @SerialName("amarelos_para_suspensao")
    val amarelos: Int,

    /** ENUM Supabase: "Publico" | "Privado" */
    val visibilidade: String,

    @SerialName("codigo_acesso")
    val codigoAcesso: String? = null,

    @SerialName("permitir_espectadores")
    val permitirEspectadores: Boolean,

    @SerialName("votacao_mvp_ativa")
    val votacaoMvpAtiva: Boolean,

    val regulamento: String? = null,

    val estado: String,

    @SerialName("tempo_extra_minutos")
    val tempoExtraMinutos: Int = 10,

    /** Sempre true no insert — estamos a inserir directamente no Supabase */
    val sincronizado: Boolean = true
)

/** Converte um TorneioEntity local para o DTO de inserção no Supabase. */
fun TorneioEntity.toInsertDto() = TorneioInsertDto(
    nome                      = nome,
    organizadorId             = organizadorId,
    modalidade                = modalidade,
    numJogadoresPersonalizado = numJogadoresPersonalizado,
    formato                   = formato,
    maxEquipas                = maxEquipas,
    maxJogadoresEquipa        = maxJogadoresPorEquipa,
    dataInicioInscricoes      = dataInicioInscricoes,
    dataFimInscricoes         = dataFimInscricoes,
    dataInicio                = dataInicio,
    dataFimPrevista           = dataFimPrevista.ifEmpty { null },
    localizacaoNome           = localizacaoNome,
    localizacaoMorada         = localizacaoMorada,
    localizacaoMapsUrl        = localizacaoMapsUrl,
    criterioDesempate         = criterioDesempate,
    amarelos                  = amarelasParaSuspensao,
    visibilidade              = visibilidade,
    codigoAcesso              = codigoAcesso,
    permitirEspectadores      = permitirEspectadores,
    votacaoMvpAtiva           = votacaoMvpAtiva,
    regulamento               = regulamento,
    estado                    = estado,
    tempoExtraMinutos         = tempoExtraMinutos,
    sincronizado              = true
)
