package com.hojetembola.app.ui.torneios

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hojetembola.app.R
import com.hojetembola.app.data.local.entity.TorneioEntity
import com.hojetembola.app.databinding.ItemListaHeaderBinding
import com.hojetembola.app.databinding.ItemTorneioMeuBinding
import com.hojetembola.app.databinding.ItemTorneioPublicoBinding

sealed class TorneioListItem {
    data class Header(val titulo: String) : TorneioListItem()
    data class Meu(val torneio: TorneioEntity) : TorneioListItem()
    data class Publico(val torneio: TorneioEntity) : TorneioListItem()
}

class TorneioAdapter(
    private val onMeuClick: (TorneioEntity) -> Unit,
    private val onPublicoClick: (TorneioEntity) -> Unit,
    private val onInscrever: (TorneioEntity) -> Unit
) : ListAdapter<TorneioListItem, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val TYPE_HEADER  = 0
        private const val TYPE_MEU     = 1
        private const val TYPE_PUBLICO = 2

        private val DIFF = object : DiffUtil.ItemCallback<TorneioListItem>() {
            override fun areItemsTheSame(a: TorneioListItem, b: TorneioListItem) = when {
                a is TorneioListItem.Header  && b is TorneioListItem.Header  -> a.titulo == b.titulo
                a is TorneioListItem.Meu     && b is TorneioListItem.Meu     -> a.torneio.id == b.torneio.id
                a is TorneioListItem.Publico && b is TorneioListItem.Publico -> a.torneio.id == b.torneio.id
                else -> false
            }
            override fun areContentsTheSame(a: TorneioListItem, b: TorneioListItem) = a == b
        }
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is TorneioListItem.Header  -> TYPE_HEADER
        is TorneioListItem.Meu     -> TYPE_MEU
        is TorneioListItem.Publico -> TYPE_PUBLICO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER  -> HeaderVH(ItemListaHeaderBinding.inflate(inf, parent, false))
            TYPE_MEU     -> MeuVH(ItemTorneioMeuBinding.inflate(inf, parent, false))
            TYPE_PUBLICO -> PublicoVH(ItemTorneioPublicoBinding.inflate(inf, parent, false))
            else -> throw IllegalStateException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TorneioListItem.Header  -> (holder as HeaderVH).bind(item)
            is TorneioListItem.Meu     -> (holder as MeuVH).bind(item)
            is TorneioListItem.Publico -> (holder as PublicoVH).bind(item)
        }
    }

    // ViewHolders

    inner class HeaderVH(private val b: ItemListaHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: TorneioListItem.Header) { b.root.text = item.titulo }
    }

    inner class MeuVH(private val b: ItemTorneioMeuBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: TorneioListItem.Meu) {
            val t = item.torneio
            val ctx = b.root.context
            b.tvNomeTorneio.text = t.nome
            b.tvSubtitulo.text = "${t.formato.toFormatoLabel(ctx)} · ${t.modalidade.toModalidadeLabel()}"
            b.tvLocalizacao.text = t.localizacaoNome
            applyBadge(b.tvEstado, t.estado, ctx)
            b.root.setOnClickListener { onMeuClick(t) }
        }
    }

    inner class PublicoVH(private val b: ItemTorneioPublicoBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: TorneioListItem.Publico) {
            val t = item.torneio
            val ctx = b.root.context
            b.tvNomeTorneio.text = t.nome
            b.tvSubtitulo.text = "${t.formato.toFormatoLabel(ctx)} · ${t.modalidade.toModalidadeLabel()}"
            b.tvLocalizacao.text = t.localizacaoNome
            applyBadge(b.tvEstado, t.estado, ctx)
            b.btnInscrever.isEnabled = t.estado == "InscricoesAbertas" || t.estado == "inscricoes_abertas"
            b.root.setOnClickListener { onPublicoClick(t) }
            b.btnInscrever.setOnClickListener { onInscrever(t) }
        }
    }
}

// Helper: apply badge visual to a TextView
private fun applyBadge(tv: android.widget.TextView, estado: String, ctx: Context) {
    val (label, bgRes, hexColor) = when (estado) {
        "ADecorrer",         "a_decorrer"        -> Triple(ctx.getString(R.string.estado_a_decorrer),         R.drawable.bg_badge_live, "#FFFFFF")
        "InscricoesAbertas", "inscricoes_abertas" -> Triple(ctx.getString(R.string.estado_inscricoes_abertas), R.drawable.bg_badge_open, "#4CAF50")
        "Terminado",         "terminado"          -> Triple(ctx.getString(R.string.estado_terminado),          R.drawable.bg_badge_done, "#8A9BB8")
        else                                      -> Triple(ctx.getString(R.string.estado_criado),             R.drawable.bg_badge_done, "#8A9BB8")
    }
    tv.text = label
    tv.setBackgroundResource(bgRes)
    tv.setTextColor(Color.parseColor(hexColor))
}

private fun String.toFormatoLabel(ctx: Context) = when (this) {
    "Liga",  "liga"                               -> ctx.getString(R.string.liga)
    "Eliminatorias", "eliminatorias"              -> ctx.getString(R.string.eliminatorias)
    "GruposEliminatorias", "grupos_eliminatorias" -> ctx.getString(R.string.grupos_eliminatorias)
    "TodosContraTodos", "todos_vs_todos"          -> ctx.getString(R.string.todos_vs_todos)
    else                                          -> this
}

private fun String.toModalidadeLabel() = when (lowercase()) {
    "fut5"          -> "Fut5"
    "fut7"          -> "Fut7"
    "fut11"         -> "Fut11"
    "personalizado" -> "Custom"
    else            -> this
}
