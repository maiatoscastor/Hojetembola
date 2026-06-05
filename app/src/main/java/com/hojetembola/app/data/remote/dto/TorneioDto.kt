package com.hojetembola.app.data.remote.dto

import com.hojetembola.app.data.local.entity.TorneioEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TorneioDto(
    val id: String,
    val nome: String,
    val modalidade: String,
    @SerialName("num_jogadores_personalizado") val numJogadoresPersonalizado: Int? = null,
    val formato: String,
    @SerialName("max_equipas") val maxEquipas: Int,
    @SerialName("max_jogadores_por_equipa") val maxJogadoresPorEquipa: Int,
    @SerialName("data_inicio_inscricoes") val dataInicioInscricoes: String,
    @SerialName("data_fim_inscricoes") val dataFimInscricoes: String,
    @SerialName("data_inicio") val dataInicio: String,
    @SerialName("data_fim_prevista") val dataFimPrevista: String? = null,
    val localizacao: String,
    @SerialName("localizacao_link") val localizacaoLink: String? = null,
    val estado: String = "criado",
    @SerialName("organizador_id") val organizadorId: String,
    val publico: Boolean = true,
    @SerialName("permitir_espectadores") val permitirEspectadores: Boolean = true,
    @SerialName("votacao_mvp_ativa") val votacaoMvpAtiva: Boolean = true,
    @SerialName("amarelas_para_suspensao") val amarelasParaSuspensao: Int = 3,
    @SerialName("criterio_desempate") val criterioDesempate: String = "penalidades",
    val regulamento: String? = null
) {
    fun toEntity() = TorneioEntity(
        id                      = id,
        nome                    = nome,
        modalidade              = modalidade,
        numJogadoresPersonalizado = numJogadoresPersonalizado,
        formato                 = formato,
        maxEquipas              = maxEquipas,
        maxJogadoresPorEquipa   = maxJogadoresPorEquipa,
        dataInicioInscricoes    = dataInicioInscricoes,
        dataFimInscricoes       = dataFimInscricoes,
        dataInicio              = dataInicio,
        dataFimPrevista         = dataFimPrevista ?: "",
        localizacao             = localizacao,
        localizacaoLink         = localizacaoLink,
        estado                  = estado,
        organizadorId           = organizadorId,
        publico                 = publico,
        permitirEspectadores    = permitirEspectadores,
        votacaoMvpAtiva         = votacaoMvpAtiva,
        amarelasParaSuspensao   = amarelasParaSuspensao,
        criterioDesempate       = criterioDesempate,
        regulamento             = regulamento,
        isSynced                = true
    )
}

fun TorneioEntity.toDto() = TorneioDto(
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
    dataFimPrevista           = dataFimPrevista.ifEmpty { null },
    localizacao               = localizacao,
    localizacaoLink           = localizacaoLink,
    estado                    = estado,
    organizadorId             = organizadorId,
    publico                   = publico,
    permitirEspectadores      = permitirEspectadores,
    votacaoMvpAtiva           = votacaoMvpAtiva,
    amarelasParaSuspensao     = amarelasParaSuspensao,
    criterioDesempate         = criterioDesempate,
    regulamento               = regulamento
)
