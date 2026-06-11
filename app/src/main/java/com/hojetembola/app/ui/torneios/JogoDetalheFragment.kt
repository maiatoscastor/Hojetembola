package com.hojetembola.app.ui.torneios

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.TextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
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
import com.hojetembola.app.data.local.dao.EventoComNome
import com.hojetembola.app.data.local.dao.JogadorInscricaoComNome
import com.hojetembola.app.databinding.FragmentJogoDetalheBinding
import com.hojetembola.app.databinding.ItemEventoJogoBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class JogoDetalheFragment : Fragment() {

    private var _binding: FragmentJogoDetalheBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JogoDetalheViewModel by viewModels()
    private lateinit var eventosAdapter: EventosAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentJogoDetalheBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        eventosAdapter = EventosAdapter()
        binding.rvEventos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEventos.adapter = eventosAdapter

        setupButtonListeners()
        observeState()
        observeTimer()
        observeAcao()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Button listeners ──────────────────────────────────────────────────────

    private fun setupButtonListeners() {
        binding.btnIniciarJogo.setOnClickListener {
            viewModel.iniciarJogo()
        }

        // Golo Casa: pede minuto → pede jogador da equipa de casa
        binding.btnGoloCasa.setOnClickListener {
            val state = viewModel.uiState.value
            showMinutoDialog { minuto ->
                showJogadorDialog(getString(R.string.qual_jogador), state.casaJogadores) { jogadorId ->
                    viewModel.registarEvento("golo", minuto, state.jogo?.equipaCasaId, jogadorId)
                }
            }
        }

        // Golo Visitante: pede minuto → pede jogador da equipa visitante
        binding.btnGoloVisitante.setOnClickListener {
            val state = viewModel.uiState.value
            showMinutoDialog { minuto ->
                showJogadorDialog(getString(R.string.qual_jogador), state.visitanteJogadores) { jogadorId ->
                    viewModel.registarEvento("golo", minuto, state.jogo?.equipaVisitanteId, jogadorId)
                }
            }
        }

        // Cartão Amarelo: pede minuto → pede equipa → pede jogador
        binding.btnCartaoAmarelo.setOnClickListener {
            val state = viewModel.uiState.value
            showMinutoDialog { minuto ->
                showEquipaDialog(state.casaNome, state.visitanteNome) { isCasa ->
                    val (equipaId, jogadores) = if (isCasa)
                        Pair(state.jogo?.equipaCasaId, state.casaJogadores)
                    else
                        Pair(state.jogo?.equipaVisitanteId, state.visitanteJogadores)
                    showJogadorDialog(getString(R.string.qual_jogador), jogadores) { jogadorId ->
                        viewModel.registarEvento("amarelo", minuto, equipaId, jogadorId)
                    }
                }
            }
        }

        // Cartão Vermelho: pede minuto → pede equipa → pede jogador
        binding.btnCartaoVermelho.setOnClickListener {
            val state = viewModel.uiState.value
            showMinutoDialog { minuto ->
                showEquipaDialog(state.casaNome, state.visitanteNome) { isCasa ->
                    val (equipaId, jogadores) = if (isCasa)
                        Pair(state.jogo?.equipaCasaId, state.casaJogadores)
                    else
                        Pair(state.jogo?.equipaVisitanteId, state.visitanteJogadores)
                    showJogadorDialog(getString(R.string.qual_jogador), jogadores) { jogadorId ->
                        viewModel.registarEvento("vermelho", minuto, equipaId, jogadorId)
                    }
                }
            }
        }

        // Substituição: pede minuto → pede equipa → pede quem sai → pede quem entra
        binding.btnSubstituicao.setOnClickListener {
            val state = viewModel.uiState.value
            showMinutoDialog { minuto ->
                showEquipaDialog(state.casaNome, state.visitanteNome) { isCasa ->
                    val (equipaId, jogadores) = if (isCasa)
                        Pair(state.jogo?.equipaCasaId, state.casaJogadores)
                    else
                        Pair(state.jogo?.equipaVisitanteId, state.visitanteJogadores)
                    showJogadorDialog(getString(R.string.quem_sai), jogadores, allowAnon = false) { saiId ->
                        showJogadorDialog(getString(R.string.quem_entra), jogadores, allowAnon = false) { entraId ->
                            viewModel.registarEvento("substituicao", minuto, equipaId,
                                jogadorSaiId = saiId, jogadorEntraId = entraId)
                        }
                    }
                }
            }
        }

        binding.btnTerminarJogo.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.terminar_jogo)
                .setMessage(R.string.confirmar_terminar_jogo)
                .setPositiveButton(R.string.confirmar) { _, _ -> viewModel.terminarJogo() }
                .setNegativeButton(R.string.cancelar, null)
                .show()
        }
    }

    // ── Dialog helpers ────────────────────────────────────────────────────────

    private fun showMinutoDialog(onConfirm: (Int) -> Unit) {
        val et = EditText(requireContext()).apply {
            hint = "0"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            // Pre-fill with the current live minute
            viewModel.minutoVivo.value?.let { setText(it.toString()) }
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.minuto_dialog_title)
            .setView(et)
            .setPositiveButton(R.string.confirmar) { _, _ ->
                onConfirm(et.text.toString().toIntOrNull() ?: 0)
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    /**
     * Mostra uma lista de jogadores. Inclui "Anónimo" no topo quando [allowAnon] = true.
     * [onSelect] recebe o utilizadorId do jogador selecionado, ou null se anónimo.
     */
    private fun showJogadorDialog(
        title: String,
        jogadores: List<JogadorInscricaoComNome>,
        allowAnon: Boolean = true,
        onSelect: (String?) -> Unit
    ) {
        val nomes = buildList {
            if (allowAnon) add(getString(R.string.anonimo))
            addAll(jogadores.map { it.nome })
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setItems(nomes) { _, which ->
                val jogadorId = if (allowAnon) {
                    if (which == 0) null else jogadores[which - 1].utilizadorId
                } else {
                    jogadores.getOrNull(which)?.utilizadorId
                }
                onSelect(jogadorId)
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    /** Mostra diálogo para escolher entre equipa de casa e visitante. */
    private fun showEquipaDialog(
        casaNome: String,
        visitanteNome: String,
        onSelect: (isCasa: Boolean) -> Unit
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.qual_equipa)
            .setItems(arrayOf(casaNome, visitanteNome)) { _, which -> onSelect(which == 0) }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    // ── State observation ─────────────────────────────────────────────────────

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.loading
                    binding.scrollContent.isVisible = !state.loading

                    if (!state.loading) {
                        val jogo = state.jogo

                        // Avatars
                        binding.tvAvatarCasa.text = state.casaIniciais
                        setCircleBackground(binding.tvAvatarCasa, state.casaCor)
                        binding.tvAvatarVisitante.text = state.visitanteIniciais
                        setCircleBackground(binding.tvAvatarVisitante, state.visitanteCor)

                        // Names
                        binding.tvNomeCasa.text = state.casaNome
                        binding.tvNomeVisitante.text = state.visitanteNome

                        // Score
                        binding.tvScore.text = if (jogo != null && jogo.estado != "agendado") {
                            getString(R.string.resultado_jogo, jogo.golosCasa ?: 0, jogo.golosVisitante ?: 0)
                        } else {
                            getString(R.string.vs)
                        }

                        // Estado badge
                        applyEstadoBadge(jogo?.estado ?: "agendado")

                        // Organizer buttons
                        if (viewModel.isOrganizador && jogo != null) {
                            binding.layoutBotoesOrganizador.isVisible = true
                            val aoVivo = jogo.estado == "ao_vivo"
                            binding.btnIniciarJogo.isVisible    = jogo.estado == "agendado"
                            binding.btnGoloCasa.isVisible       = aoVivo
                            binding.btnGoloVisitante.isVisible  = aoVivo
                            binding.btnCartaoAmarelo.isVisible  = aoVivo
                            binding.btnCartaoVermelho.isVisible = aoVivo
                            binding.btnSubstituicao.isVisible   = aoVivo
                            binding.btnTerminarJogo.isVisible   = aoVivo
                            if (jogo.estado == "terminado") binding.layoutBotoesOrganizador.isVisible = false
                        } else {
                            binding.layoutBotoesOrganizador.isVisible = false
                        }

                        // Events
                        binding.tvSectionEventos.isVisible = state.eventos.isNotEmpty()
                        eventosAdapter.submitList(state.eventos)
                    }
                }
            }
        }
    }

    /** Observa o timer ao vivo e atualiza tvMinutoAtual independentemente. */
    private fun observeTimer() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.minutoVivo.collect { minuto ->
                    binding.tvMinutoAtual.isVisible = minuto != null
                    if (minuto != null) {
                        binding.tvMinutoAtual.text = getString(R.string.minuto_jogo, minuto)
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
                        is JogoDetalheAcao.Sucesso -> {
                            Snackbar.make(binding.root, acao.msg, Snackbar.LENGTH_SHORT).show()
                            viewModel.resetAcao()
                        }
                        is JogoDetalheAcao.Erro -> {
                            Snackbar.make(binding.root, acao.msg, Snackbar.LENGTH_LONG).show()
                            viewModel.resetAcao()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun applyEstadoBadge(estado: String) {
        val (label, color) = when (estado) {
            "ao_vivo"   -> Pair(getString(R.string.ao_vivo), "#F57C00")
            "terminado" -> Pair("FIM", "#8A9BB8")
            else        -> Pair(getString(R.string.agendado), "#4CAF50")
        }
        binding.tvEstadoBadge.text = label
        binding.tvEstadoBadge.setTextColor(Color.parseColor(color))
    }

    private fun setCircleBackground(view: TextView, hexColor: String) {
        val color = try { Color.parseColor(hexColor) } catch (_: Exception) { Color.parseColor("#3D5A80") }
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }
}

class EventosAdapter : ListAdapter<EventoComNome, EventosAdapter.Holder>(EventosDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemEventoJogoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    class Holder(private val binding: ItemEventoJogoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(evento: EventoComNome) {
            binding.tvMinuto.text = "${evento.minuto}'"
            binding.tvTipoIcon.text = when (evento.tipo) {
                "golo"         -> "⚽"
                "amarelo"      -> "🟨"
                "vermelho"     -> "🟥"
                "substituicao" -> "🔄"
                else           -> "•"
            }
            binding.tvDescricao.text = when (evento.tipo) {
                "golo"    -> evento.jogadorNome ?: getString(R.string.golo_anonimo)
                "amarelo" -> evento.jogadorNome ?: getString(R.string.cartao_anonimo)
                "vermelho"-> evento.jogadorNome ?: getString(R.string.cartao_anonimo)
                "substituicao" -> {
                    val sai   = evento.jogadorSaiNome ?: ""
                    val entra = evento.jogadorEntraNome ?: ""
                    if (sai.isNotBlank() && entra.isNotBlank()) "$sai → $entra" else "Substituição"
                }
                else -> evento.tipo
            }
        }

        private fun getString(resId: Int) = binding.root.context.getString(resId)
    }

    private object EventosDiff : DiffUtil.ItemCallback<EventoComNome>() {
        override fun areItemsTheSame(old: EventoComNome, new: EventoComNome) = old.eventoId == new.eventoId
        override fun areContentsTheSame(old: EventoComNome, new: EventoComNome) = old == new
    }
}
