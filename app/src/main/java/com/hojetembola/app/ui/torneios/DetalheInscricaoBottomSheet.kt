package com.hojetembola.app.ui.torneios

import android.app.Dialog
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
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hojetembola.app.R
import com.hojetembola.app.data.local.dao.JogadorInscricaoComNome
import com.hojetembola.app.databinding.FragmentDetalheInscricaoBinding
import com.hojetembola.app.databinding.ItemJogadorInscricaoBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DetalheInscricaoBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentDetalheInscricaoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetalheInscricaoViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.setBackgroundResource(R.color.bg_primary)
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalheInscricaoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = JogadorAdapter()
        binding.rvJogadores.apply {
            this.adapter = adapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.loading

                    binding.tvEquipaNome.text = state.equipaNome

                    val cidade = state.cidade
                    if (!cidade.isNullOrBlank()) {
                        binding.layoutCidade.isVisible = true
                        binding.tvCidade.text = cidade
                    } else {
                        binding.layoutCidade.isVisible = false
                    }

                    if (!state.loading) {
                        if (state.jogadores.isEmpty()) {
                            binding.tvSemJogadores.isVisible   = true
                            binding.tvLabelJogadores.isVisible = false
                            binding.rvJogadores.isVisible      = false
                        } else {
                            binding.tvSemJogadores.isVisible   = false
                            binding.tvLabelJogadores.isVisible = true
                            binding.rvJogadores.isVisible      = true
                            adapter.submitList(state.jogadores)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    inner class JogadorAdapter : ListAdapter<JogadorInscricaoComNome, JogadorAdapter.VH>(
        object : DiffUtil.ItemCallback<JogadorInscricaoComNome>() {
            override fun areItemsTheSame(old: JogadorInscricaoComNome, new: JogadorInscricaoComNome) =
                old.utilizadorId == new.utilizadorId
            override fun areContentsTheSame(old: JogadorInscricaoComNome, new: JogadorInscricaoComNome) =
                old == new
        }
    ) {
        inner class VH(val b: ItemJogadorInscricaoBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemJogadorInscricaoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = getItem(position)
            holder.b.tvNome.text  = item.nome
            holder.b.tvEmail.text = item.email
            holder.b.tvAvatar.text = item.nome.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            holder.b.tvAvatar.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1E3260"))
            }
        }
    }

    companion object {
        const val TAG = "DetalheInscricaoBottomSheet"

        fun newInstance(
            inscricaoId: String,
            equipaId: String,
            equipaNome: String
        ): DetalheInscricaoBottomSheet =
            DetalheInscricaoBottomSheet().apply {
                arguments = bundleOf(
                    "inscricaoId" to inscricaoId,
                    "equipaId"   to equipaId,
                    "equipaNome" to equipaNome
                )
            }
    }
}
