package com.hojetembola.app.ui.torneios

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.hojetembola.app.databinding.FragmentJogoDetalheBinding
import com.hojetembola.app.databinding.ItemEventoTimelineBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class JogoDetalheFragment : Fragment() {

    private var _binding: FragmentJogoDetalheBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JogoDetalheViewModel by viewModels()
    private val eventosAdapter = EventoTimelineAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJogoDetalheBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

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
        binding.btnDefinirTitulares.setOnClickListener {
            TitularesBottomSheet.newInstance()
                .show(childFragmentManager, TitularesBottomSheet.TAG)
        }

        binding.btnIniciarJogo.setOnClickListener {
            viewModel.iniciarJogo()
        }

        binding.btnRegistarEvento.setOnClickListener {
            RegistarEventoBottomSheet.newInstance()
                .show(childFragmentManager, RegistarEventoBottomSheet.TAG)
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

    // ── State observation ─────────────────────────────────────────────────────

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.loading
                    binding.scrollContent.isVisible = !state.loading

                    if (!state.loading) {
                        val jogo = state.jogo

                        // Score card — names and score
                        binding.tvNomeCasa.text = state.casaNome
                        binding.tvNomeVisitante.text = state.visitanteNome
                        binding.tvScore.text = if (jogo != null && jogo.estado != "agendado") {
                            getString(R.string.resultado_jogo, jogo.golosCasa ?: 0, jogo.golosVisitante ?: 0)
                        } else {
                            getString(R.string.vs)
                        }

                        // Estado badge
                        applyEstadoBadge(jogo?.estado ?: "agendado")

                        // Organizer action buttons
                        if (viewModel.isOrganizador && jogo != null) {
                            binding.layoutBotoesOrganizador.isVisible = jogo.estado != "terminado"
                            val agendado = jogo.estado == "agendado"
                            val aoVivo   = jogo.estado == "ao_vivo"
                            binding.btnDefinirTitulares.isVisible = agendado
                            binding.btnIniciarJogo.isVisible      = agendado
                            binding.btnRegistarEvento.isVisible   = aoVivo
                            binding.btnTerminarJogo.isVisible     = aoVivo
                        } else {
                            binding.layoutBotoesOrganizador.isVisible = false
                        }

                        // Timeline card — always visible once loaded
                        binding.tvFluxoLabel.isVisible = true
                        binding.cardTimeline.isVisible = true
                        binding.tvCasaHeader.text = state.casaNome
                        binding.tvVisitanteHeader.text = state.visitanteNome

                        val hasEventos = state.eventos.isNotEmpty()
                        binding.tvSemEventos.isVisible = !hasEventos
                        binding.rvEventos.isVisible    = hasEventos

                        eventosAdapter.casaEquipaId = jogo?.equipaCasaId ?: ""
                        eventosAdapter.submitList(state.eventos)
                    }
                }
            }
        }
    }

    /** Observes the live match timer and updates the pill independently. */
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
}

// ── Timeline adapter ─────────────────────────────────────────────────────────

class EventoTimelineAdapter : ListAdapter<EventoComNome, EventoTimelineAdapter.Holder>(EventosDiff) {

    /** Set to equipaCasaId so each event is routed to the correct column. */
    var casaEquipaId: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemEventoTimelineBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position), casaEquipaId)
    }

    class Holder(private val binding: ItemEventoTimelineBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(evento: EventoComNome, casaEquipaId: String) {
            val isCasa = evento.equipaId == casaEquipaId
            binding.layoutEventoCasa.isVisible = isCasa
            binding.layoutEventoVisitante.isVisible = !isCasa

            val minutoText = "${evento.minuto}'"
            val icon = when (evento.tipo) {
                "golo"         -> "⚽"
                "amarelo"      -> "🟨"
                "vermelho"     -> "🟥"
                "substituicao" -> "🔄"
                else           -> "•"
            }
            val descricao = buildDescricao(evento)

            if (isCasa) {
                binding.tvMinutoCasa.text    = minutoText
                binding.tvIconCasa.text      = icon
                binding.tvDescricaoCasa.text = descricao
            } else {
                binding.tvMinutoVisitante.text    = minutoText
                binding.tvIconVisitante.text      = icon
                binding.tvDescricaoVisitante.text = descricao
            }
        }

        private fun buildDescricao(evento: EventoComNome): String = when (evento.tipo) {
            "golo"    -> evento.jogadorNome
                ?: binding.root.context.getString(R.string.golo_anonimo)
            "amarelo", "vermelho" -> evento.jogadorNome
                ?: binding.root.context.getString(R.string.cartao_anonimo)
            "substituicao" -> {
                val sai   = evento.jogadorSaiNome   ?: ""
                val entra = evento.jogadorEntraNome ?: ""
                if (sai.isNotBlank() && entra.isNotBlank()) "$sai → $entra"
                else binding.root.context.getString(R.string.substituicao)
            }
            else -> evento.tipo
        }
    }

    private object EventosDiff : DiffUtil.ItemCallback<EventoComNome>() {
        override fun areItemsTheSame(old: EventoComNome, new: EventoComNome) =
            old.eventoId == new.eventoId
        override fun areContentsTheSame(old: EventoComNome, new: EventoComNome) = old == new
    }
}
