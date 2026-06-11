package com.hojetembola.app.ui.torneios

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
import com.google.android.material.snackbar.Snackbar
import com.hojetembola.app.R
import com.hojetembola.app.data.local.entity.InscricaoComEquipa
import com.hojetembola.app.databinding.FragmentGerirTorneioBinding
import com.hojetembola.app.databinding.ItemInscricaoTorneioBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GerirTorneioFragment : Fragment() {

    private var _binding: FragmentGerirTorneioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GerirTorneioViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGerirTorneioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        val adapter = InscricaoAdapter(
            onAceitar  = { inscricaoId -> viewModel.aceitar(inscricaoId) },
            onRejeitar = { inscricaoId -> viewModel.rejeitar(inscricaoId) }
        )

        binding.rvInscricoes.apply {
            this.adapter  = adapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }

        binding.btnConfirmarTodas.setOnClickListener { viewModel.confirmarTodas() }
        binding.btnGerarCalendario.setOnClickListener { viewModel.gerarCalendario() }

        observeState(adapter)
        observeAcao()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Observation ───────────────────────────────────────────────────────────

    private fun observeState(adapter: InscricaoAdapter) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state is GerirTorneioUiState.Loading
                    binding.tvEmpty.isVisible     = false
                    binding.rvInscricoes.isVisible = false

                    when (state) {
                        is GerirTorneioUiState.Loading -> Unit

                        is GerirTorneioUiState.Error -> {
                            binding.tvEmpty.isVisible = true
                            binding.tvEmpty.text = state.message
                        }

                        is GerirTorneioUiState.Content -> {
                            binding.tvTorneioNome.text = state.torneioNome

                            val total = state.inscricoes.size
                            val pendentes = state.numPendentes
                            binding.tvContador.text = getString(
                                R.string.inscricoes_contador, total, pendentes
                            )
                            binding.btnConfirmarTodas.isVisible = pendentes > 0
                            // Mostrar "Gerar Calendário" só se há ≥2 confirmadas e ainda sem calendário
                            binding.btnGerarCalendario.isVisible =
                                state.numConfirmadas >= 2 && !state.temCalendario

                            if (state.inscricoes.isEmpty()) {
                                binding.tvEmpty.isVisible = true
                                binding.tvEmpty.text = getString(R.string.sem_inscricoes)
                            } else {
                                binding.rvInscricoes.isVisible = true
                                adapter.submitList(state.inscricoes)
                            }
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
                        is GerirTorneioAcao.Sucesso -> {
                            Snackbar.make(binding.root, acao.message, Snackbar.LENGTH_SHORT).show()
                            viewModel.resetAcao()
                        }
                        is GerirTorneioAcao.Erro -> {
                            Snackbar.make(binding.root, acao.message, Snackbar.LENGTH_LONG).show()
                            viewModel.resetAcao()
                        }
                        is GerirTorneioAcao.CalendarioGerado -> {
                            // Vai direto para o ecrã do torneio em curso, limpando a backstack
                            findNavController().navigate(
                                R.id.action_gerirTorneioFragment_to_torneioEmCursoFragment,
                                androidx.core.os.bundleOf("torneioId" to acao.torneioId)
                            )
                            viewModel.resetAcao()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    inner class InscricaoAdapter(
        private val onAceitar:  (inscricaoId: String) -> Unit,
        private val onRejeitar: (inscricaoId: String) -> Unit
    ) : ListAdapter<InscricaoComEquipa, InscricaoAdapter.VH>(
        object : DiffUtil.ItemCallback<InscricaoComEquipa>() {
            override fun areItemsTheSame(old: InscricaoComEquipa, new: InscricaoComEquipa) = old.id == new.id
            override fun areContentsTheSame(old: InscricaoComEquipa, new: InscricaoComEquipa) = old == new
        }
    ) {

        inner class VH(val b: ItemInscricaoTorneioBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemInscricaoTorneioBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = getItem(position)
            val b = holder.b

            // Avatar
            val color = try { Color.parseColor(item.corAvatar) }
                        catch (_: Exception) { Color.parseColor("#1E3260") }
            b.tvIniciais.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL; setColor(color)
            }
            b.tvIniciais.text = item.iniciais

            // Info
            b.tvNome.text = item.nome
            b.tvMembros.text = getString(R.string.jogadores_count_formato, item.numMembros)

            // Estado badge
            when (item.estado) {
                "Pendente" -> {
                    b.tvEstado.text = getString(R.string.estado_pendente)
                    b.tvEstado.setTextColor(Color.parseColor("#F57C00"))
                    b.layoutBotoes.isVisible = true
                    b.btnAceitar.isVisible   = true
                    b.btnAceitar.setOnClickListener { onAceitar(item.id) }
                    b.btnRejeitar.setOnClickListener { onRejeitar(item.id) }
                }
                "Confirmada" -> {
                    b.tvEstado.text = getString(R.string.estado_confirmada)
                    b.tvEstado.setTextColor(Color.parseColor("#4CAF50"))
                    b.layoutBotoes.isVisible = true
                    b.btnAceitar.isVisible   = false
                    b.btnRejeitar.setOnClickListener { onRejeitar(item.id) }
                }
                "Rejeitada" -> {
                    b.tvEstado.text = getString(R.string.estado_rejeitada)
                    b.tvEstado.setTextColor(Color.parseColor("#8A9BB8"))
                    b.layoutBotoes.isVisible = false
                }
                else -> {
                    b.tvEstado.text = item.estado
                    b.tvEstado.setTextColor(Color.parseColor("#8A9BB8"))
                    b.layoutBotoes.isVisible = false
                }
            }

            // Tap on the card opens the detail bottom sheet
            b.root.setOnClickListener {
                if (parentFragmentManager.findFragmentByTag(DetalheInscricaoBottomSheet.TAG) == null) {
                    DetalheInscricaoBottomSheet.newInstance(
                        inscricaoId = item.id,
                        equipaId    = item.equipaId,
                        equipaNome  = item.nome
                    ).show(parentFragmentManager, DetalheInscricaoBottomSheet.TAG)
                }
            }
        }

    }
}
