package com.hojetembola.app.data.remote.dto

import com.hojetembola.app.data.local.entity.JogoEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JogoDto(
    val id: Int,
    @SerialName("torneio_id")   val torneioId: Int,
    @SerialName("jornada_id")   val jornadaId: Int,
    @SerialName("equipa_casa_id")  val equipaCasaId: Int,
    @SerialName("equipa_fora_id")  val equipaForaId: Int,
    @SerialName("golos_casa")      val golosCasa: Int?    = null,
    @SerialName("golos_fora")      val golosFora: Int?    = null,
    @SerialName("data_hora")       val dataHora: String?  = null,
    @SerialName("localizacao_nome") val localizacaoNome: String?    = null,
    @SerialName("localizacao_maps_url") val localizacaoMapsUrl: String? = null,
    val estado: String = "Agendado"
) {
    fun toEntity() = JogoEntity(
        id                = id.toString(),
        torneioId         = torneioId.toString(),
        jornadaId         = jornadaId.toString(),
        equipaCasaId      = equipaCasaId.toString(),
        equipaVisitanteId = equipaForaId.toString(),
        golosCasa         = golosCasa,
        golosVisitante    = golosFora,
        dataHora          = dataHora ?: "",
        local             = localizacaoNome ?: "",
        localLink         = localizacaoMapsUrl,
        estado            = when (estado) {
            "Agendado"  -> "agendado"
            "EmCurso"   -> "ao_vivo"
            "Terminado" -> "terminado"
            else        -> estado.lowercase()
        }
    )
}

/** DTO mínimo para criar um jogo — datas e local são definidos depois pelo organizador. */
@Serializable
data class JogoInsertDto(
    @SerialName("torneio_id")      val torneioId: Int,
    @SerialName("jornada_id")      val jornadaId: Int,
    @SerialName("equipa_casa_id")  val equipaCasaId: Int,
    @SerialName("equipa_fora_id")  val equipaForaId: Int,
    val estado: String = "Agendado"
)
