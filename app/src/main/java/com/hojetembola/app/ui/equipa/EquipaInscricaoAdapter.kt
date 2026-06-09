package com.hojetembola.app.ui.equipa

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hojetembola.app.databinding.ItemEquipaInscricaoBinding

class EquipaInscricaoAdapter(
    private val onInscrever: (EquipaComEstado) -> Unit
) : ListAdapter<EquipaComEstado, EquipaInscricaoAdapter.ViewHolder>(DIFF_CALLBACK) {

    class ViewHolder(val binding: ItemEquipaInscricaoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEquipaInscricaoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val b    = holder.binding

        // ── Avatar ──────────────────────────────────────────────────────────
        val color = try { Color.parseColor(item.equipa.corAvatar) }
                    catch (_: Exception) { Color.parseColor("#E84C3D") }
        b.vAvatar.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL; setColor(color)
        }
        b.tvIniciais.text = item.equipa.iniciais

        // ── Name + city + member count ───────────────────────────────────────
        b.tvNome.text = item.equipa.nome
        val cidade = item.equipa.cidade
        if (!cidade.isNullOrBlank()) {
            b.tvCidade.isVisible = true
            b.tvCidade.text      = "$cidade · "
        } else {
            b.tvCidade.isVisible = false
        }
        b.tvNumMembros.text = "👥 ${item.numMembros} jogador${if (item.numMembros != 1) "es" else ""}"

        // ── Inscription state ────────────────────────────────────────────────
        if (item.jaInscrita) {
            b.btnInscrever.isVisible = false
            b.tvJaInscrita.isVisible = true
        } else {
            b.btnInscrever.isVisible = true
            b.tvJaInscrita.isVisible = false
            b.btnInscrever.setOnClickListener { onInscrever(item) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<EquipaComEstado>() {
            override fun areItemsTheSame(old: EquipaComEstado, new: EquipaComEstado) =
                old.equipa.id == new.equipa.id
            override fun areContentsTheSame(old: EquipaComEstado, new: EquipaComEstado) =
                old == new
        }
    }
}
