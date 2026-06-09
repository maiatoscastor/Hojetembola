package com.hojetembola.app.ui.equipa

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.hojetembola.app.data.local.entity.MembroComNome
import com.hojetembola.app.data.local.entity.UtilizadorEntity
import com.hojetembola.app.databinding.FragmentGerirEquipaBinding
import com.hojetembola.app.databinding.ItemMembroEquipaBinding
import com.hojetembola.app.databinding.ItemUtilizadorResultadoBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GerirEquipaFragment : Fragment() {

    private var _binding: FragmentGerirEquipaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GerirEquipaViewModel by viewModels()

    private val membrosAdapter = MembroEquipaAdapter { utilizadorId ->
        viewModel.removerJogador(utilizadorId)
    }
    private val resultadosAdapter = UtilizadorResultadoAdapter { utilizadorId ->
        viewModel.adicionarJogador(utilizadorId)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGerirEquipaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.tvEquipaNome.text = viewModel.equipaNome

        // Mostrar limites se há torneio associado
        if (viewModel.torneioId != null) {
            binding.tvLimites.text = getString(
                R.string.limites_jogadores_formato,
                viewModel.minJogadores,
                viewModel.maxJogadores
            )
        } else {
            binding.tvLimites.isVisible = false
        }

        binding.rvMembros.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMembros.adapter = membrosAdapter

        binding.rvResultadosPesquisa.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResultadosPesquisa.adapter = resultadosAdapter

        binding.btnInscrever.isVisible = viewModel.torneioId != null
        binding.btnInscrever.setOnClickListener { viewModel.inscreverEquipa() }

        setupPesquisa()
        observeState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupPesquisa() {
        binding.etPesquisa.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val q = s.toString().trim()
                viewModel.pesquisar(q)
                if (q.isEmpty()) viewModel.limparPesquisa()
            }
        })
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is GerirEquipaUiState.Loading -> {
                                binding.progressBar.isVisible = true
                                binding.rvMembros.isVisible   = false
                            }
                            is GerirEquipaUiState.Content -> {
                                binding.progressBar.isVisible = false
                                binding.rvMembros.isVisible   = true
                                membrosAdapter.submitList(state.membros)

                                val count = state.membros.size
                                binding.tvContador.text = getString(
                                    R.string.jogadores_count_formato, count
                                )

                                if (viewModel.torneioId != null) {
                                    binding.btnInscrever.isEnabled = state.podeContinuar
                                    val cor = if (state.podeContinuar) R.color.orange else R.color.text_muted
                                    binding.tvContador.setTextColor(
                                        if (state.podeContinuar) resources.getColor(R.color.green, null)
                                        else resources.getColor(R.color.color_live, null)
                                    )
                                }
                            }
                            is GerirEquipaUiState.Error -> {
                                binding.progressBar.isVisible = false
                                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                launch {
                    viewModel.resultadosPesquisa.collect { resultados ->
                        resultadosAdapter.submitList(resultados)
                        binding.rvResultadosPesquisa.isVisible = resultados.isNotEmpty()
                    }
                }

                launch {
                    viewModel.isPesquisando.collect { loading ->
                        binding.progressPesquisa.isVisible = loading
                    }
                }

                launch {
                    viewModel.acao.collect { acao ->
                        when (acao) {
                            is GerirEquipaAcao.Sucesso -> {
                                Snackbar.make(binding.root, acao.mensagem, Snackbar.LENGTH_SHORT).show()
                                viewModel.resetAcao()
                            }
                            is GerirEquipaAcao.Erro -> {
                                Snackbar.make(binding.root, acao.mensagem, Snackbar.LENGTH_LONG).show()
                                viewModel.resetAcao()
                            }
                            is GerirEquipaAcao.InscricaoConcluida -> {
                                Snackbar.make(
                                    binding.root,
                                    getString(R.string.equipa_inscrita, acao.equipaNome),
                                    Snackbar.LENGTH_SHORT
                                ).show()
                                viewModel.resetAcao()
                                // Volta ao TorneioDetalhe (pop 2 fragmentos: GerirEquipa + CriarEquipa se existir)
                                findNavController().popBackStack(R.id.torneioDetalheFragment, false)
                            }
                            is GerirEquipaAcao.Loading -> Unit
                            is GerirEquipaAcao.Idle    -> Unit
                        }
                    }
                }
            }
        }
    }

    // ── Adapter: membros actuais ──────────────────────────────────────────────

    inner class MembroEquipaAdapter(
        private val onRemover: (String) -> Unit
    ) : ListAdapter<MembroComNome, MembroEquipaAdapter.VH>(
        object : DiffUtil.ItemCallback<MembroComNome>() {
            override fun areItemsTheSame(old: MembroComNome, new: MembroComNome) = old.id == new.id
            override fun areContentsTheSame(old: MembroComNome, new: MembroComNome) = old == new
        }
    ) {
        inner class VH(val b: ItemMembroEquipaBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemMembroEquipaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val membro = getItem(position)
            holder.b.tvNome.text  = membro.nome
            holder.b.tvEmail.text = membro.email
            holder.b.tvAvatar.text = membro.nome.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            holder.b.tvAvatar.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1E3260"))
            }
            holder.b.btnRemover.setOnClickListener { onRemover(membro.utilizadorId) }
        }
    }

    // ── Adapter: resultados de pesquisa ───────────────────────────────────────

    inner class UtilizadorResultadoAdapter(
        private val onAdicionar: (String) -> Unit
    ) : ListAdapter<UtilizadorEntity, UtilizadorResultadoAdapter.VH>(
        object : DiffUtil.ItemCallback<UtilizadorEntity>() {
            override fun areItemsTheSame(old: UtilizadorEntity, new: UtilizadorEntity) = old.id == new.id
            override fun areContentsTheSame(old: UtilizadorEntity, new: UtilizadorEntity) = old == new
        }
    ) {
        inner class VH(val b: ItemUtilizadorResultadoBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemUtilizadorResultadoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val u = getItem(position)
            holder.b.tvNome.text  = u.nome
            holder.b.tvEmail.text = u.email
            holder.b.tvAvatar.text = u.nome.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            holder.b.tvAvatar.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#F57C00"))
            }
            holder.b.btnAdicionar.setOnClickListener { onAdicionar(u.id) }
        }
    }
}
