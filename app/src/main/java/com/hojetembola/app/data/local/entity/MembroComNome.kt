package com.hojetembola.app.data.local.entity

import androidx.room.ColumnInfo

/**
 * Resultado da query JOIN entre membro_equipa e utilizador.
 * Não é uma @Entity — é usado apenas como resultado de @Query no Room.
 */
data class MembroComNome(
    val id: String,
    @ColumnInfo(name = "equipa_id")     val equipaId: String,
    @ColumnInfo(name = "utilizador_id") val utilizadorId: String,
    val nome: String,
    val email: String,
    val ativo: Boolean
)
