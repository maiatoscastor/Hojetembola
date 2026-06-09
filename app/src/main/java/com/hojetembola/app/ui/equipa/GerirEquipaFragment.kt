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
import com.hojetembola.app.data.local.entity.ConviteEntity
import com.hojetembola.app.data.local.entity.MembroComNome
import com.hojetembola.app.databinding.FragmentGerirEquipaBinding
import com.hojetembola.app.databinding.ItemConvitePendenteBinding
import com.hojetembola.app.databinding.ItemMembroEquipaBinding
import com.hojetembola.app.databinding.ItemUtilizadorResultadoBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GerirEquipaFragment : Fragment() {

    private var _binding: FragmentGerirEquipaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GerirEquipaViewModel by viewModels()

    /** Estado atual — usado no click do botão inscrever para dar feedback. */
    private var estadoAtual: GerirEquipaUiState.Content? = null

    private val membrosAdapter = MembroEquipaAdapter { utilizadorId ->
        viewModel.removerJogador(utilizadorId)
    }
    private val convitesAdapter = ConvitePendenteAdapter { conviteId ->
        viewModel.revogarConvite(conviteId)
    }
    private val resultadosAdapter = UtilizadorResultadoAdapter(
        onConvidar = { utilizadorId -> viewModel.convidarJogador(utilizadorId) }
    )

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

        binding.rvConvitesPendentes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvConvitesPendentes.adapter = convitesAdapter

        binding.rvResultadosPesquisa.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResultadosPesquisa.adapter = resultadosAdapter

        binding.btnInscrever.isVisible = viewModel.torneioId != null
        binding.btnInscrever.setOnClickListener { tentarInscrever() }

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
                                estadoAtual = state
                                binding.progressBar.isVisible = false
                                binding.rvMembros.isVisible   = true

                                membrosAdapter.submitList(state.membros)

                                // Secção de convites pendentes
                                val temConvites = state.convitesPendentes.isNotEmpty()
                                binding.tvLabelConvites.isVisible     = temConvites
                                binding.rvConvitesPendentes.isVisible = temConvites
                                binding.dividerConvites.isVisible     = temConvites
                                if (temConvites) convitesAdapter.submitList(state.convitesPendentes)

                                // Contador: membros confirmados + pendentes
                                val totalEfetivo = state.membros.size + state.convitesPendentes.size
                                binding.tvContador.text = getString(
                                    R.string.jogadores_count_formato, totalEfetivo
                                )

                                if (viewModel.torneioId != null) {
                                    // Botão sempre clicável — feedback dado em tentarInscrever()
                                    binding.btnInscrever.isEnabled = true
                                    binding.btnInscrever.alpha =
                                        if (state.podeContinuar) 1f else 0.5f
                                    binding.tvContador.setTextColor(
                                        if (state.podeContinuar)
                                            resources.getColor(R.color.green, null)
                                        else
                                            resources.getColor(R.color.color_live, null)
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

    // ── Lógica de inscrição ───────────────────────────────────────────────────

    private fun tentarInscrever() {
        val state = estadoAtual ?: return
        if (!state.podeContinuar) {
            val total = state.membros.size + state.convitesPendentes.size
            val msg = when {
                total < viewModel.minJogadores ->
                    getString(
                        R.string.erro_poucos_jogadores,
                        viewModel.minJogadores,
                        total,
                        viewModel.minJogadores - total
                    )
                total > viewModel.maxJogadores ->
                    getString(
                        R.string.erro_muitos_jogadores,
                        viewModel.maxJogadores,
                        total
                    )
                else -> getString(R.string.erro_jogadores_insuficientes_generico)
            }
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
            return
        }
        viewModel.inscreverEquipa()
    }

    // ── Adapter: convites pendentes ───────────────────────────────────────────

    inner class ConvitePendenteAdapter(
        private val onRevogar: (String) -> Unit
    ) : ListAdapter<ConviteEntity, ConvitePendenteAdapter.VH>(
        object : DiffUtil.ItemCallback<ConviteEntity>() {
            override fun areItemsTheSame(old: ConviteEntity, new: ConviteEntity) = old.id == new.id
            override fun areContentsTheSame(old: ConviteEntity, new: ConviteEntity) = old == new
        }
    ) {
        inner class VH(val b: ItemConvitePendenteBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemConvitePendenteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val convite = getItem(position)
            // Usa email como fallback se não temos o nome em cache
            val nomeOuEmail = convite.convidadoEmail
            holder.b.tvNome.text  = nomeOuEmail
            holder.b.tvEmail.text = convite.convidadoEmail
            holder.b.tvAvatar.text = nomeOuEmail.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            holder.b.tvAvatar.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#F57C00"))
                alpha = 160
            }
            holder.b.btnRevogar.setOnClickListener { onRevogar(convite.id) }
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
        private val onConvidar: (String) -> Unit
    ) : ListAdapter<UtilizadorComEstado, UtilizadorResultadoAdapter.VH>(
        object : DiffUtil.ItemCallback<UtilizadorComEstado>() {
            override fun areItemsTheSame(old: UtilizadorComEstado, new: UtilizadorComEstado) =
                old.utilizador.id == new.utilizador.id
            override fun areContentsTheSame(old: UtilizadorComEstado, new: UtilizadorComEstado) =
                old == new
        }
    ) {
        inner class VH(val b: ItemUtilizadorResultadoBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemUtilizadorResultadoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = getItem(position)
            val u    = item.utilizador

            holder.b.tvNome.text  = u.nome
            holder.b.tvEmail.text = u.email
            holder.b.tvAvatar.text = u.nome.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

            when (item.estado) {
                EstadoNaEquipa.DISPONIVEL -> {
                    holder.b.tvAvatar.background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor("#F57C00"))
                    }
                    holder.b.tvEstado.visibility = View.GONE
                    holder.b.btnAdicionar.text    = getString(R.string.convidar)
                    holder.b.btnAdicionar.isEnabled = true
                    holder.b.btnAdicionar.alpha   = 1f
                    holder.b.btnAdicionar.setOnClickListener { onConvidar(u.id) }
                }
                EstadoNaEquipa.JA_CONVIDADO -> {
                    holder.b.tvAvatar.background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor("#F57C00"))
                        alpha = 100
                    }
                    holder.b.tvEstado.visibility  = View.VISIBLE
                    holder.b.tvEstado.text        = getString(R.string.ja_convidado)
                    holder.b.tvEstado.setTextColor(Color.parseColor("#F57C00"))
                    holder.b.btnAdicionar.text    = getString(R.string.convidar)
                    holder.b.btnAdicionar.isEnabled = false
                    holder.b.btnAdicionar.alpha   = 0.4f
                    holder.b.btnAdicionar.setOnClickListener(null)
                }
                EstadoNaEquipa.JA_MEMBRO -> {
                    holder.b.tvAvatar.background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor("#1E3260"))
                        alpha = 100
                    }
                    holder.b.tvEstado.visibility  = View.VISIBLE
                    holder.b.tvEstado.text        = getString(R.string.ja_membro)
                    holder.b.tvEstado.setTextColor(Color.parseColor("#4CAF50"))
                    holder.b.btnAdicionar.text    = getString(R.string.convidar)
                    holder.b.btnAdicionar.isEnabled = false
                    holder.b.btnAdicionar.alpha   = 0.4f
                    holder.b.btnAdicionar.setOnClickListener(null)
                }
            }
        }
    }
}
