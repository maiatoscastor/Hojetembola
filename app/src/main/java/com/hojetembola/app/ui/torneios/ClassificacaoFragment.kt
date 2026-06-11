package com.hojetembola.app.ui.torneios

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hojetembola.app.databinding.FragmentClassificacaoBinding
import com.hojetembola.app.databinding.ItemClassificacaoRowBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ClassificacaoFragment : Fragment() {

    private var _binding: FragmentClassificacaoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ClassificacaoViewModel by viewModels()
    private lateinit var adapter: ClassificacaoTorneioAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentClassificacaoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        if (viewModel.torneioNome.isNotBlank()) {
            binding.tvTitulo.text = viewModel.torneioNome
        }

        adapter = ClassificacaoTorneioAdapter()
        binding.rvClassificacao.layoutManager = LinearLayoutManager(requireContext())
        binding.rvClassificacao.adapter = adapter

        observeState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loading.collect { loading ->
                    binding.progressBar.isVisible = loading
                    binding.rvClassificacao.isVisible = !loading
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rows.collect { rows ->
                    adapter.submitList(rows)
                }
            }
        }
    }
}

class ClassificacaoTorneioAdapter :
    ListAdapter<ClassificacaoTorneioRow, ClassificacaoTorneioAdapter.Holder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemClassificacaoRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    class Holder(private val binding: ItemClassificacaoRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: ClassificacaoTorneioRow) {
            binding.tvPos.text = row.posicao.toString()
            binding.tvEquipa.text = row.equipaNome
            binding.tvJ.text = row.jogos.toString()
            binding.tvV.text = row.vitorias.toString()
            binding.tvE.text = row.empates.toString()
            binding.tvD.text = row.derrotas.toString()
            binding.tvGm.text = row.golosMarcados.toString()
            binding.tvGs.text = row.golosSofridos.toString()
            binding.tvPts.text = row.pontos.toString()
        }
    }

    private object Diff : DiffUtil.ItemCallback<ClassificacaoTorneioRow>() {
        override fun areItemsTheSame(old: ClassificacaoTorneioRow, new: ClassificacaoTorneioRow) =
            old.posicao == new.posicao && old.equipaNome == new.equipaNome
        override fun areContentsTheSame(old: ClassificacaoTorneioRow, new: ClassificacaoTorneioRow) = old == new
    }
}
