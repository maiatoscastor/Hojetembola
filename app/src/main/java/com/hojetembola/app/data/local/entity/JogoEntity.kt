package com.hojetembola.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jogo")
data class JogoEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "torneio_id")
    val torneioId: String,

    @ColumnInfo(name = "jornada_id")
    val jornadaId: String,

    @ColumnInfo(name = "equipa_casa_id")
    val equipaCasaId: String,

    @ColumnInfo(name = "equipa_visitante_id")
    val equipaVisitanteId: String,

    @ColumnInfo(name = "golos_casa")
    val golosCasa: Int? = null,

    @ColumnInfo(name = "golos_visitante")
    val golosVisitante: Int? = null,

    @ColumnInfo(name = "data_hora")
    val dataHora: String,

    val local: String,

    @ColumnInfo(name = "local_link")
    val localLink: String? = null,

    /** "agendado" | "ao_vivo" | "terminado" */
    val estado: String = "agendado",

    @ColumnInfo(name = "minuto_atual")
    val minutoAtual: Int? = null,

    /** Pendente de sincronização com Supabase (modo offline RF25) */
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = true
)
