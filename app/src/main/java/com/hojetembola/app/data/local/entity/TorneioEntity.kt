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

    @ColumnInfo(name = "max_jogadores_por_equipa")
    val maxJogadoresPorEquipa: Int,

    @ColumnInfo(name = "data_inicio_inscricoes")
    val dataInicioInscricoes: String,

    @ColumnInfo(name = "data_fim_inscricoes")
    val dataFimInscricoes: String,

    @ColumnInfo(name = "data_inicio")
    val dataInicio: String,

    @ColumnInfo(name = "data_fim_prevista")
    val dataFimPrevista: String,

    val localizacao: String,

    @ColumnInfo(name = "localizacao_link")
    val localizacaoLink: String? = null,

    /**
     * "criado" | "inscricoes_abertas" | "inscricoes_fechadas"
     * | "a_decorrer" | "terminado"
     */
    val estado: String = "criado",

    @ColumnInfo(name = "organizador_id")
    val organizadorId: String,

    val publico: Boolean = true,

    @ColumnInfo(name = "permitir_espectadores")
    val permitirEspectadores: Boolean = true,

    @ColumnInfo(name = "votacao_mvp_ativa")
    val votacaoMvpAtiva: Boolean = true,

    /** Nº de amarelos para suspensão automática (RN04) */
    @ColumnInfo(name = "amarelas_para_suspensao")
    val amarelasParaSuspensao: Int = 3,

    /** "prolongamento" | "penalidades" | "golo_de_ouro" */
    @ColumnInfo(name = "criterio_desempate")
    val criterioDesempate: String = "penalidades",

    val regulamento: String? = null,

    /** Indica se este registo já foi sincronizado com o Supabase */
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = true
)
