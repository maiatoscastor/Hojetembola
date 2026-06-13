package com.hojetembola.app.ui.torneios

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
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
import com.hojetembola.app.data.local.entity.ClassificacaoEntity
import com.hojetembola.app.databinding.FragmentJogoDetalheBinding
import com.hojetembola.app.databinding.ItemClassificacaoJogoBinding
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

        // Default: Fluxo tab selected — check() fires BEFORE listener is added,
        // so set visibility explicitly here instead of relying on the listener.
        binding.tabGroupSecoes.check(R.id.btnTabFluxo)
        updateTabColors(R.id.btnTabFluxo)
        binding.sectionFormacao.isVisible = false
        binding.sectionRanking.isVisible  = false
        binding.sectionFluxo.isVisible    = true
        binding.tabGroupSecoes.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            binding.sectionFormacao.isVisible = checkedId == R.id.btnTabFormacao
            binding.sectionRanking.isVisible  = checkedId == R.id.btnTabRanking
            binding.sectionFluxo.isVisible    = checkedId == R.id.btnTabFluxo
            updateTabColors(checkedId)
        }

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
            if (!viewModel.uiState.value.titularesDefinidos) {
                // Force lineup selection before starting — opens the sheet in "start after save" mode
                TitularesBottomSheet.newInstance(startAfterSave = true)
                    .show(childFragmentManager, TitularesBottomSheet.TAG)
            } else {
                viewModel.iniciarJogo()
            }
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

        binding.btnVotarMvp.setOnClickListener {
            if (childFragmentManager.findFragmentByTag(VotarMvpBottomSheet.TAG) == null) {
                VotarMvpBottomSheet.newInstance(viewModel.jogoId)
                    .show(childFragmentManager, VotarMvpBottomSheet.TAG)
            }
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

                        // MVP vote button — visible when voting is active and game has started
                        val jogoEmCursoOuTerminado = jogo?.estado in listOf("ao_vivo", "terminado")
                        binding.btnVotarMvp.isVisible = state.votacaoMvpAtiva && jogoEmCursoOuTerminado

                        // Formation section — update pitch + empty state (visibility controlled by tabs)
                        if (state.titularesDefinidos) {
                            binding.pitchView.setJogadores(
                                casa = state.casaJogadoresEmCampo.map { it.nome },
                                visitante = state.visitanteJogadoresEmCampo.map { it.nome }
                            )
                            binding.pitchView.isVisible = true
                            binding.tvFormacaoVazia.isVisible = false
                        } else {
                            binding.pitchView.isVisible = false
                            binding.tvFormacaoVazia.isVisible = true
                        }

                        // Ranking section — always update rows
                        updateRankingRows(state.classificacao, state.equipaNomesMap)

                        // Timeline section
                        binding.tvCasaHeader.text = state.casaNome
                        binding.tvVisitanteHeader.text = state.visitanteNome

                        val hasEventos = state.eventos.isNotEmpty()
                        binding.tvSemEventos.isVisible = !hasEventos
                        binding.rvEventos.isVisible    = hasEventos

                        eventosAdapter.casaEquipaId     = jogo?.equipaCasaId ?: ""
                        eventosAdapter.casaBancoIds     = state.casaJogadoresNoBanco.map { it.utilizadorId }.toSet()
                        eventosAdapter.visitanteBancoIds = state.visitanteJogadoresNoBanco.map { it.utilizadorId }.toSet()
                        eventosAdapter.casaExpulsoIds   = state.casaExpulsoIds
                        eventosAdapter.visitanteExpulsoIds = state.visitanteExpulsoIds
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

    private fun updateTabColors(checkedId: Int) {
        val orange = requireContext().getColor(R.color.orange)
        val transparent = Color.TRANSPARENT
        val white = Color.WHITE
        val unselectedText = Color.parseColor("#8A9BB8")
        val borderColor = Color.parseColor("#1E3260")
        listOf(R.id.btnTabFluxo, R.id.btnTabFormacao, R.id.btnTabRanking).forEach { id ->
            val btn = binding.tabGroupSecoes.findViewById<com.google.android.material.button.MaterialButton>(id)
            val isSelected = id == checkedId
            val bgColor = if (isSelected) orange else transparent
            val strokeCol = if (isSelected) orange else borderColor
            btn.backgroundTintList = ColorStateList(arrayOf(intArrayOf()), intArrayOf(bgColor))
            btn.strokeColor = ColorStateList(arrayOf(intArrayOf()), intArrayOf(strokeCol))
            btn.setTextColor(if (isSelected) white else unselectedText)
        }
    }

    private fun updateRankingRows(
        classificacao: List<ClassificacaoEntity>,
        equipaNomesMap: Map<String, String>
    ) {
        val container = binding.layoutRankingRows
        container.removeAllViews()
        classificacao.forEach { c ->
            val rowBinding = ItemClassificacaoJogoBinding.inflate(
                layoutInflater, container, false
            )
            rowBinding.tvPosicao.text = c.posicao.toString()
            rowBinding.tvEquipaNome.text = equipaNomesMap[c.equipaId] ?: c.equipaId
            rowBinding.tvPJ.text = c.jogos.toString()
            val dg = c.golosMarcados - c.golosSofridos
            rowBinding.tvDG.text = if (dg > 0) "+$dg" else dg.toString()
            rowBinding.tvPontos.text = c.pontos.toString()
            container.addView(rowBinding.root)
        }
    }

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

    var casaEquipaId: String = ""
    var casaBancoIds: Set<String> = emptySet()
    var visitanteBancoIds: Set<String> = emptySet()
    var casaExpulsoIds: Set<String> = emptySet()
    var visitanteExpulsoIds: Set<String> = emptySet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemEventoTimelineBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val isCasaEvent = getItem(position).equipaId == casaEquipaId
        val bancoIds    = if (isCasaEvent) casaBancoIds else visitanteBancoIds
        val expulsoIds  = if (isCasaEvent) casaExpulsoIds else visitanteExpulsoIds
        holder.bind(getItem(position), casaEquipaId, bancoIds, expulsoIds)
    }

    class Holder(private val binding: ItemEventoTimelineBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            evento: EventoComNome,
            casaEquipaId: String,
            bancoIds: Set<String>,
            expulsoIds: Set<String>
        ) {
            val isCasa = evento.equipaId == casaEquipaId
            binding.layoutEventoCasa.isVisible      = isCasa
            binding.layoutEventoVisitante.isVisible = !isCasa

            val minutoText = "${evento.minuto}'"
            val icon = when (evento.tipo) {
                "golo"         -> "⚽"
                "amarelo"      -> "🟨"
                "vermelho"     -> "🟥"
                "substituicao" -> "🔄"
                else           -> "•"
            }
            val accentColor = when (evento.tipo) {
                "golo"         -> Color.parseColor("#4CAF50")
                "amarelo"      -> Color.parseColor("#FFC107")
                "vermelho"     -> Color.parseColor("#F44336")
                "substituicao" -> Color.parseColor("#42A5F5")
                else           -> Color.parseColor("#F07B3F")
            }
            val descricao = buildDescricao(evento, bancoIds, expulsoIds)

            val assistNome = if (evento.tipo == "golo") evento.assistenciaNome else null
            val assistSpan = assistNome?.let { buildAssistSpan("(A $it)") }

            if (isCasa) {
                binding.tvMinutoCasa.text    = minutoText
                binding.tvIconCasa.text      = icon
                binding.tvDescricaoCasa.text = descricao
                binding.viewAccentCasa.setBackgroundColor(accentColor)
                binding.tvAssistenciaCasa.isVisible = assistSpan != null
                if (assistSpan != null) binding.tvAssistenciaCasa.text = assistSpan
            } else {
                binding.tvMinutoVisitante.text    = minutoText
                binding.tvIconVisitante.text      = icon
                binding.tvDescricaoVisitante.text = descricao
                binding.viewAccentVisitante.setBackgroundColor(accentColor)
                binding.tvAssistenciaVisitante.isVisible = assistSpan != null
                if (assistSpan != null) binding.tvAssistenciaVisitante.text = assistSpan
            }
        }

        private fun buildDescricao(
            evento: EventoComNome,
            bancoIds: Set<String>,
            expulsoIds: Set<String>
        ): String {
            val jogadorId = evento.jogadorId
            val emBanco   = jogadorId != null && jogadorId in bancoIds
            val expulso   = jogadorId != null && jogadorId in expulsoIds
            return when (evento.tipo) {
                "golo" -> evento.jogadorNome ?: binding.root.context.getString(R.string.golo_anonimo)
                "amarelo" -> {
                    val nome = evento.jogadorNome ?: binding.root.context.getString(R.string.cartao_anonimo)
                    if (emBanco) "$nome (fora do campo)" else nome
                }
                "vermelho" -> {
                    val nome = evento.jogadorNome ?: binding.root.context.getString(R.string.cartao_anonimo)
                    if (expulso || !emBanco) "$nome (expulso)" else "$nome (fora do campo — expulso)"
                }
                "substituicao" -> {
                    val sai   = evento.jogadorSaiNome   ?: ""
                    val entra = evento.jogadorEntraNome ?: ""
                    if (sai.isNotBlank() && entra.isNotBlank()) "$sai → $entra"
                    else binding.root.context.getString(R.string.substituicao)
                }
                else -> evento.tipo
            }
        }

        // text is expected to be "(A name)" — makes the A at index 1 bold red
        private fun buildAssistSpan(text: String): SpannableString {
            val span = SpannableString(text)
            span.setSpan(ForegroundColorSpan(Color.parseColor("#F44336")), 1, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            span.setSpan(StyleSpan(Typeface.BOLD), 1, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return span
        }
    }

    private object EventosDiff : DiffUtil.ItemCallback<EventoComNome>() {
        override fun areItemsTheSame(old: EventoComNome, new: EventoComNome) =
            old.eventoId == new.eventoId
        override fun areContentsTheSame(old: EventoComNome, new: EventoComNome) = old == new
    }
}
