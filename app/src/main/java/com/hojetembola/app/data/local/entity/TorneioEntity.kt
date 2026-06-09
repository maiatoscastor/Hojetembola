package com.hojetembola.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "torneio")
data class TorneioEntity(
    @PrimaryKey
    val id: String,

    val nome: String,

    /** "fut5" | "fut7" | "fut11" | "personalizado" */
    val modalidade: String,

    /** Número de jogadores quando modalidade = "personalizado" */
    @ColumnInfo(name = "num_jogadores_personalizado")
    val numJogadoresPersonalizado: Int? = null,

    /** "liga" | "eliminatorias" | "grupos_eliminatorias" | "todos_vs_todos" */
    val formato: String,

    @ColumnInfo(name = "max_equipas")
    val maxEquipas: Int,

    /** Coluna Supabase: max_jogadores_equipa */
    @ColumnInfo(name = "max_jogadores_equipa")
    val maxJogadoresPorEquipa: Int,

    @ColumnInfo(name = "data_inicio_inscricoes")
    val dataInicioInscricoes: String,

    @ColumnInfo(name = "data_fim_inscricoes")
    val dataFimInscricoes: String,

    @ColumnInfo(name = "data_inicio")
    val dataInicio: String,

    @ColumnInfo(name = "data_fim_prevista")
    val dataFimPrevista: String,

    /** Nome do campo / instalação desportiva */
    @ColumnInfo(name = "localizacao_nome")
    val localizacaoNome: String,

    /** Morada opcional */
    @ColumnInfo(name = "localizacao_morada")
    val localizacaoMorada: String? = null,

    /** Link Google Maps (opcional) */
    @ColumnInfo(name = "localizacao_maps_url")
    val localizacaoMapsUrl: String? = null,

    /**
     * "criado" | "inscricoes_abertas" | "inscricoes_fechadas"
     * | "a_decorrer" | "terminado"
     */
    val estado: String = "Criado",

    @ColumnInfo(name = "organizador_id")
    val organizadorId: String,

    /** ENUM Supabase visibilidade_tipo: "Publico" | "Privado" */
    val visibilidade: String = "Publico",

    @ColumnInfo(name = "permitir_espectadores")
    val permitirEspectadores: Boolean = true,

    @ColumnInfo(name = "votacao_mvp_ativa")
    val votacaoMvpAtiva: Boolean = true,

    /** Nº de amarelos para suspensão automática — coluna Supabase: amarelos_para_suspensao */
    @ColumnInfo(name = "amarelos_para_suspensao")
    val amarelasParaSuspensao: Int = 3,

    /** ENUM Supabase criterio_desempate: "Prolongamento" | "Penalidades" | "GoloDeOuro" */
    @ColumnInfo(name = "criterio_desempate")
    val criterioDesempate: String = "Penalidades",

    /** Duração de cada parte do prolongamento em minutos */
    @ColumnInfo(name = "tempo_extra_minutos")
    val tempoExtraMinutos: Int = 10,

    /** Código de acesso para torneios privados (null quando visibilidade = "Publico") */
    @ColumnInfo(name = "codigo_acesso")
    val codigoAcesso: String? = null,

    val regulamento: String? = null,

    /** Indica se este registo já foi sincronizado com o Supabase */
    @ColumnInfo(name = "sincronizado")
    val isSynced: Boolean = true
)

/** Número mínimo de jogadores por equipa consoante a modalidade. */
fun TorneioEntity.minJogadoresPorEquipa(): Int = when (modalidade.lowercase()) {
    "fut5"          -> 5
    "fut7"          -> 7
    "fut11"         -> 11
    "personalizado" -> numJogadoresPersonalizado ?: 1
    else            -> 1
}
