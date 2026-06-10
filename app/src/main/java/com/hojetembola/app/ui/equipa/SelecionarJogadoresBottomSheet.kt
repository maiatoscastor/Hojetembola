package com.hojetembola.app.ui.equipa

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hojetembola.app.R
import com.hojetembola.app.databinding.FragmentSelecionarJogadoresBinding
import com.hojetembola.app.databinding.ItemMembroSelecaoBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SelecionarJogadoresBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentSelecionarJogadoresBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SelecionarJogadoresViewModel by viewModels()

    /** Callback disparado quando o utilizador confirma a seleção. */
    var onConfirmar: ((equipaId: String, equipaNome: String) -> Unit)? = null

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelecionarJogadoresBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvDescricao.text = getString(
            R.string.selecionar_jogadores_desc,
            viewModel.min,
            viewModel.max
        )

        val adapter = MembroSelecaoAdapter { utilizadorId ->
            viewModel.toggleSelecao(utilizadorId)
        }

        binding.rvMembros.apply {
            this.adapter     = adapter
            layoutManager    = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
            overScrollMode   = View.OVER_SCROLL_NEVER
        }

        binding.btnFechar.setOnClickListener { dismiss() }
        binding.btnConfirmar.setOnClickListener {
            onConfirmar?.invoke(viewModel.equipaId, viewModel.equipaNome)
            dismiss()
        }

        observeUiState(adapter)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Observation ───────────────────────────────────────────────────────────

    private fun observeUiState(adapter: MembroSelecaoAdapter) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is SelecionarJogadoresUiState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.rvMembros.isVisible   = false
                        }
                        is SelecionarJogadoresUiState.Content -> {
                            binding.progressBar.isVisible = false
                            binding.rvMembros.isVisible   = true
                            adapter.submitList(state.membros)

                            val count = state.membros.count { it.selecionado }
                            binding.tvContador.text = getString(
                                R.string.jogadores_selecionados_formato,
                                count,
                                viewModel.max
                            )
                            binding.tvContador.setTextColor(
                                if (state.podeContinuar) resources.getColor(R.color.green, null)
                                else resources.getColor(R.color.color_live, null)
                            )
                            binding.btnConfirmar.isEnabled = state.podeContinuar
                        }
                        is SelecionarJogadoresUiState.Error -> {
                            binding.progressBar.isVisible = false
                        }
                    }
                }
            }
        }
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    inner class MembroSelecaoAdapter(
        private val onToggle: (String) -> Unit
    ) : ListAdapter<MembroSelecao, MembroSelecaoAdapter.VH>(
        object : DiffUtil.ItemCallback<MembroSelecao>() {
            override fun areItemsTheSame(old: MembroSelecao, new: MembroSelecao) =
                old.membro.utilizadorId == new.membro.utilizadorId
            override fun areContentsTheSame(old: MembroSelecao, new: MembroSelecao) = old == new
        }
    ) {
        inner class VH(val b: ItemMembroSelecaoBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemMembroSelecaoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = getItem(position)
            val m    = item.membro

            holder.b.tvNome.text  = m.nome
            holder.b.tvEmail.text = m.email
            holder.b.tvAvatar.text = m.nome.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            holder.b.tvAvatar.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1E3260"))
            }

            if (item.emConflito) {
                holder.b.checkBox.setOnCheckedChangeListener(null)
                holder.b.checkBox.isChecked = false
                holder.b.checkBox.isEnabled = false
                holder.b.tvConflito.visibility = android.view.View.VISIBLE
                holder.b.tvConflito.text = holder.b.root.context.getString(
                    R.string.jogador_ja_no_torneio
                )
                holder.b.root.setOnClickListener(null)
            } else {
                holder.b.checkBox.isEnabled = true
                holder.b.tvConflito.visibility = android.view.View.GONE
                holder.b.checkBox.setOnCheckedChangeListener(null)
                holder.b.checkBox.isChecked = item.selecionado
                holder.b.checkBox.setOnCheckedChangeListener { _, _ ->
                    onToggle(m.utilizadorId)
                }
                holder.b.root.setOnClickListener { onToggle(m.utilizadorId) }
            }
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    companion object {
        const val TAG = "SelecionarJogadoresBottomSheet"

        fun newInstance(
            equipaId: String,
            equipaNome: String,
            torneioId: String,
            min: Int,
            max: Int
        ): SelecionarJogadoresBottomSheet =
            SelecionarJogadoresBottomSheet().apply {
                arguments = bundleOf(
                    "equipaId"   to equipaId,
                    "equipaNome" to equipaNome,
                    "torneioId"  to torneioId,
                    "min"        to min,
                    "max"        to max
                )
            }
    }
}
