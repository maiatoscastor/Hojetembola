package com.hojetembola.app.ui.rankings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hojetembola.app.databinding.ItemClassificacaoRowBinding

class ClassificacaoAdapter :
    ListAdapter<ClassificacaoRow, ClassificacaoAdapter.Holder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): Holder {
        val binding = ItemClassificacaoRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    class Holder(
        private val binding: ItemClassificacaoRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: ClassificacaoRow) {
            binding.tvPos.text = row.posicao.toString()
            binding.tvEquipa.text = row.equipa
            binding.tvJ.text = row.jogos.toString()
            binding.tvV.text = row.vitorias.toString()
            binding.tvE.text = row.empates.toString()
            binding.tvD.text = row.derrotas.toString()
            binding.tvGm.text = row.golosMarcados.toString()
            binding.tvGs.text = row.golosSofridos.toString()
            binding.tvPts.text = row.pontos.toString()
        }
    }

    private object Diff : DiffUtil.ItemCallback<ClassificacaoRow>() {
        override fun areItemsTheSame(old: ClassificacaoRow, new: ClassificacaoRow) =
            old.posicao == new.posicao

        override fun areContentsTheSame(old: ClassificacaoRow, new: ClassificacaoRow) = old == new
    }
}
