package com.hojetembola.app.ui.equipa

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.hojetembola.app.R
import com.hojetembola.app.databinding.FragmentInscricaoTorneioBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InscricaoTorneioBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentInscricaoTorneioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InscricaoTorneioViewModel by viewModels()

    /**
     * Set this before calling show() to handle navigation to CriarEquipaFragment.
     * The bottom sheet will dismiss itself before invoking the callback.
     */
    var onNavegarParaCriarEquipa: (() -> Unit)? = null

    /**
     * Set this before calling show() to handle navigation to GerirEquipaFragment
     * when a team has fewer members than the tournament minimum.
     */
    var onNavegarParaGerirEquipa: ((equipaId: String, equipaNome: String) -> Unit)? = null

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInscricaoTorneioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = EquipaInscricaoAdapter { equipaComEstado ->
            viewModel.inscrever(equipaComEstado.equipa.id, equipaComEstado.equipa.nome)
        }

        binding.rvEquipas.apply {
            this.adapter     = adapter
            layoutManager    = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
            overScrollMode   = View.OVER_SCROLL_NEVER
        }

        binding.btnFechar.setOnClickListener { dismiss() }

        binding.btnCriarEquipa.setOnClickListener {
            dismiss()
            onNavegarParaCriarEquipa?.invoke()
        }

        observeUiState(adapter)
        observeAcao()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Observation ───────────────────────────────────────────────────────────

    private fun observeUiState(adapter: EquipaInscricaoAdapter) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is InscricaoUiState.Loading -> {
                            binding.progressBar.isVisible  = true
                            binding.rvEquipas.isVisible    = false
                            binding.tvSemEquipas.isVisible = false
                        }
                        is InscricaoUiState.Content -> {
                            binding.progressBar.isVisible = false
                            val equipas = state.equipas
                            if (equipas.isEmpty()) {
                                binding.rvEquipas.isVisible    = false
                                binding.tvSemEquipas.isVisible = true
                                binding.tvSemEquipas.text      = getString(R.string.sem_equipas_para_inscrever)
                            } else {
                                binding.rvEquipas.isVisible    = true
                                binding.tvSemEquipas.isVisible = false
                                adapter.submitList(equipas)
                            }
                        }
                        is InscricaoUiState.Error -> {
                            binding.progressBar.isVisible  = false
                            binding.rvEquipas.isVisible    = false
                            binding.tvSemEquipas.isVisible = true
                            binding.tvSemEquipas.text      = state.message
                        }
                    }
                }
            }
        }
    }

    private fun observeAcao() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.acao.collect { acao ->
                    when (acao) {
                        is InscricaoAcao.Sucesso -> {
                            val msg = getString(R.string.equipa_inscrita, acao.equipaNome)
                            val anchor = _binding?.root ?: requireActivity().window.decorView
                            Snackbar.make(anchor, msg, Snackbar.LENGTH_SHORT).show()
                            viewModel.resetAcao()
                            viewModel.loadEquipas()
                        }
                        is InscricaoAcao.Erro -> {
                            val anchor = _binding?.root ?: requireActivity().window.decorView
                            Snackbar.make(anchor, acao.message, Snackbar.LENGTH_LONG).show()
                            viewModel.resetAcao()
                        }
                        is InscricaoAcao.PoucosMembros -> {
                            viewModel.resetAcao()
                            val msg = getString(R.string.poucos_membros_aviso, acao.min, acao.count)
                            val anchor = _binding?.root ?: requireActivity().window.decorView
                            Snackbar.make(anchor, msg, Snackbar.LENGTH_LONG).show()
                            dismiss()
                            onNavegarParaGerirEquipa?.invoke(acao.equipaId, acao.equipaNome)
                        }
                        is InscricaoAcao.MuitosMembros -> {
                            viewModel.resetAcao()
                            // Abrir bottom sheet de seleção de jogadores
                            if (parentFragmentManager.findFragmentByTag(SelecionarJogadoresBottomSheet.TAG) == null) {
                                val sheet = SelecionarJogadoresBottomSheet.newInstance(
                                    equipaId   = acao.equipaId,
                                    equipaNome = acao.equipaNome,
                                    torneioId  = viewModel.torneioId,
                                    min        = acao.min,
                                    max        = acao.max
                                )
                                sheet.onConfirmar = { equipaId, equipaNome ->
                                    viewModel.inscreverComJogadores(equipaId, equipaNome)
                                }
                                sheet.show(parentFragmentManager, SelecionarJogadoresBottomSheet.TAG)
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    companion object {
        const val TAG = "InscricaoTorneioBottomSheet"

        fun newInstance(torneioId: String): InscricaoTorneioBottomSheet =
            InscricaoTorneioBottomSheet().apply {
                arguments = bundleOf("torneioId" to torneioId)
            }
    }
}
