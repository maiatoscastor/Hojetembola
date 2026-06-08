package com.hojetembola.app.ui.rankings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hojetembola.app.databinding.ItemRankingRowBinding
import com.hojetembola.app.databinding.ItemRankingRowLandBinding

class RankingListAdapter(
    private val isLandscape: Boolean,
    private val trendFormatter: (RankingEntry) -> String,
    private val trendColor: (RankingEntry) -> Int
) : ListAdapter<RankingEntry, RecyclerView.ViewHolder>(Diff) {

    override fun getItemViewType(position: Int): Int =
        if (isLandscape) VIEW_LAND else VIEW_PORTRAIT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            VIEW_LAND -> LandHolder(
                ItemRankingRowLandBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
            else -> PortraitHolder(
                ItemRankingRowBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is PortraitHolder -> holder.bind(item)
            is LandHolder -> holder.bind(item)
        }
    }

    private inner class PortraitHolder(
        private val binding: ItemRankingRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RankingEntry) {
            binding.tvPosicao.text = item.posicao.toString()
            binding.tvNome.text = item.nome
            binding.tvEquipa.text = item.equipa
            binding.tvTrend.text = trendFormatter(item)
            binding.tvTrend.setTextColor(trendColor(item))
        }
    }

    private inner class LandHolder(
        private val binding: ItemRankingRowLandBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RankingEntry) {
            binding.tvPosicao.text = item.posicao.toString()
            binding.tvNome.text = item.nome
            binding.tvEquipa.text = item.equipa
            binding.tvValor.text = item.valor.toString()
            binding.tvTrend.text = trendFormatter(item)
            binding.tvTrend.setTextColor(trendColor(item))
        }
    }

    private object Diff : DiffUtil.ItemCallback<RankingEntry>() {
        override fun areItemsTheSame(old: RankingEntry, new: RankingEntry) =
            old.posicao == new.posicao && old.nome == new.nome

        override fun areContentsTheSame(old: RankingEntry, new: RankingEntry) = old == new
    }

    companion object {
        private const val VIEW_PORTRAIT = 0
        private const val VIEW_LAND = 1
    }
}
