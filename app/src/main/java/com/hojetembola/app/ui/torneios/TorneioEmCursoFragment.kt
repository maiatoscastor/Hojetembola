package com.hojetembola.app.ui.torneios

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
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
import com.google.android.material.tabs.TabLayout
import com.hojetembola.app.R
import com.hojetembola.app.data.local.entity.InscricaoComEquipa
import com.hojetembola.app.databinding.FragmentTorneioEmCursoBinding
import com.hojetembola.app.databinding.ItemEquipaTorneioBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class TorneioEmCursoFragment : Fragment() {

    private var _binding: FragmentTorneioEmCursoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TorneioEmCursoViewModel by viewModels()

    private lateinit var classificacaoAdapter: ClassificacaoTorneioAdapter
    private lateinit var jornadasAdapter: CalendarioAdapter
    private lateinit var equipasAdapter: EquipasInscritasAdapter

    private var currentTab: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTorneioEmCursoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        setupTabs()
        setupRecyclerViews()
        observeState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Tabs ──────────────────────────────────────────────────────────────────

    private fun setupTabs() {
        listOf(
            getString(R.string.tab_classificacao),
            getString(R.string.tab_jornadas),
            getString(R.string.tab_equipas),
            getString(R.string.tab_info)
        ).forEach { label ->
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(label))
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) { currentTab = tab.position; showTab(currentTab) }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })

        showTab(currentTab)
    }

    private fun showTab(index: Int) {
        binding.rvClassificacao.isVisible = index == 0
        binding.rvJornadas.isVisible      = index == 1
        binding.rvEquipas.isVisible       = index == 2
        binding.scrollInfo.isVisible      = index == 3
    }

    // ── RecyclerViews ─────────────────────────────────────────────────────────

    private fun setupRecyclerViews() {
        classificacaoAdapter = ClassificacaoTorneioAdapter()
        binding.rvClassificacao.layoutManager = LinearLayoutManager(requireContext())
        binding.rvClassificacao.adapter = classificacaoAdapter

        jornadasAdapter = CalendarioAdapter { jogo ->
            findNavController().navigate(
                R.id.action_torneioEmCursoFragment_to_jogoDetalheFragment,
                bundleOf(
                    "jogoId"        to jogo.jogoId,
                    "torneioId"     to viewModel.torneioId,
                    "isOrganizador" to viewModel.uiState.value.isOrganizador
                )
            )
        }
        binding.rvJornadas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvJornadas.adapter = jornadasAdapter

        equipasAdapter = EquipasInscritasAdapter()
        binding.rvEquipas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEquipas.adapter = equipasAdapter
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.loading

                    if (!state.loading) {
                        state.torneio?.let { torneio ->
                            binding.tvTorneioNome.text = torneio.nome
                            applyEstadoBadge(torneio.estado)
                            fillInfoPanel(torneio)
                        }

                        classificacaoAdapter.submitList(state.classificacao)

                        val items = mutableListOf<CalendarioItem>()
                        state.jornadas.forEach { jc ->
                            items.add(CalendarioItem.Header(jc.jornada))
                            jc.jogos.forEach { items.add(CalendarioItem.Match(it)) }
                        }
                        jornadasAdapter.submitList(items)

                        equipasAdapter.updateData(state.equipas, state.jogadoresPorEquipa)
                    }
                }
            }
        }
    }

    // ── Info panel ────────────────────────────────────────────────────────────

    private fun fillInfoPanel(torneio: com.hojetembola.app.data.local.entity.TorneioEntity) {
        binding.tvModalidade.text           = torneio.modalidade.toModalidadeLabel()
        binding.tvFormato.text              = torneio.formato.toFormatoLabel()
        binding.tvMaxEquipas.text           = torneio.maxEquipas.toString()
        binding.tvLocalizacao.text          = torneio.localizacaoNome
        binding.tvDataInicioInscricoes.text = torneio.dataInicioInscricoes.formatDate()
        binding.tvDataFimInscricoes.text    = torneio.dataFimInscricoes.formatDate()
        binding.tvDataInicio.text           = torneio.dataInicio.formatDate()
        // Rules
        binding.tvCriterio.text    = torneio.criterioDesempate.toCriterioLabel()
        binding.tvAmarelas.text    = torneio.amarelasParaSuspensao.toString()
        val extras = buildList {
            if (torneio.permitirEspectadores) add(getString(R.string.permitir_espectadores))
            if (torneio.votacaoMvpAtiva)      add(getString(R.string.votacao_mvp_ativa))
        }
        binding.tvExtras.text = if (extras.isEmpty()) getString(R.string.nenhum) else extras.joinToString("\n")
    }

    // ── Estado badge ──────────────────────────────────────────────────────────

    private fun applyEstadoBadge(estado: String) {
        val (label, bgRes, hexColor) = when (estado) {
            "ADecorrer", "a_decorrer" -> Triple(getString(R.string.estado_a_decorrer),  R.drawable.bg_badge_live, "#FFFFFF")
            "Terminado",  "terminado" -> Triple(getString(R.string.estado_terminado),   R.drawable.bg_badge_done, "#8A9BB8")
            else                      -> Triple(getString(R.string.estado_criado),      R.drawable.bg_badge_done, "#8A9BB8")
        }
        binding.tvEstadoBadge.text = label
        binding.tvEstadoBadge.setBackgroundResource(bgRes)
        binding.tvEstadoBadge.setTextColor(Color.parseColor(hexColor))
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun String.formatDate(): String = try {
        val parsed = LocalDate.parse(this)
        val fmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("pt", "PT"))
        parsed.format(fmt)
    } catch (_: Exception) { this }

    private fun String.toModalidadeLabel() = when (lowercase()) {
        "fut5" -> "Fut5"; "fut7" -> "Fut7"; "fut11" -> "Fut11"
        "personalizado" -> getString(R.string.personalizado)
        else -> this
    }

    private fun String.toFormatoLabel() = when (this) {
        "Liga", "liga"                               -> getString(R.string.liga)
        "Eliminatorias", "eliminatorias"             -> getString(R.string.eliminatorias)
        "GruposEliminatorias", "grupos_eliminatorias"-> getString(R.string.grupos_eliminatorias)
        "TodosContraTodos", "todos_vs_todos"         -> getString(R.string.todos_vs_todos)
        else -> this
    }

    private fun String.toCriterioLabel() = when (this) {
        "Penalidades",   "penalidades"   -> getString(R.string.penalidades)
        "Prolongamento", "prolongamento" -> getString(R.string.prolongamento)
        "GoloDeOuro",    "golo_de_ouro"  -> getString(R.string.golo_de_ouro)
        else -> this
    }

    // ── EquipasInscritasAdapter ───────────────────────────────────────────────

    class EquipasInscritasAdapter : RecyclerView.Adapter<EquipasInscritasAdapter.Holder>() {

        private var equipas: List<InscricaoComEquipa> = emptyList()
        private var jogadoresPorEquipa: Map<String, List<JogadorComGolos>> = emptyMap()
        private val expandedIds = mutableSetOf<String>()

        fun updateData(
            newEquipas: List<InscricaoComEquipa>,
            newJogadores: Map<String, List<JogadorComGolos>>
        ) {
            equipas = newEquipas
            jogadoresPorEquipa = newJogadores
            notifyDataSetChanged()
        }

        override fun getItemCount() = equipas.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val binding = ItemEquipaTorneioBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return Holder(binding)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = equipas[position]
            val jogadores = jogadoresPorEquipa[item.equipaId] ?: emptyList()
            val isExpanded = item.equipaId in expandedIds
            holder.bind(item, jogadores, isExpanded) {
                if (isExpanded) expandedIds.remove(item.equipaId)
                else expandedIds.add(item.equipaId)
                notifyItemChanged(position)
            }
        }

        class Holder(private val b: ItemEquipaTorneioBinding) : RecyclerView.ViewHolder(b.root) {

            fun bind(
                item: InscricaoComEquipa,
                jogadores: List<JogadorComGolos>,
                isExpanded: Boolean,
                onToggle: () -> Unit
            ) {
                b.tvIniciais.text    = item.iniciais
                b.tvNome.text        = item.nome
                b.tvNumMembros.text  = b.root.context.getString(
                    R.string.jogadores_count_formato, item.numMembros
                )

                // Avatar circle
                val color = try { Color.parseColor(item.corAvatar) }
                            catch (_: Exception) { Color.parseColor("#3D5A80") }
                b.vAvatar.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL; setColor(color)
                }

                // Expand / collapse
                b.tvChevron.text = if (isExpanded) "˅" else "›"
                b.layoutJogadores.isVisible = isExpanded

                if (isExpanded) {
                    // Remove old player rows (keep the divider at index 0)
                    while (b.layoutJogadores.childCount > 1) {
                        b.layoutJogadores.removeViewAt(1)
                    }
                    // Add player rows
                    jogadores.forEach { j ->
                        val ctx = b.root.context
                        val row = LinearLayout(ctx).apply {
                            orientation = LinearLayout.HORIZONTAL
                            setPadding(0, 4, 0, 4)
                            gravity = android.view.Gravity.CENTER_VERTICAL
                        }
                        val tvNome = TextView(ctx).apply {
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                            text = j.nome
                            setTextColor(Color.parseColor("#8A9BB8"))
                            textSize = 12f
                        }
                        val tvGolos = TextView(ctx).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            text = if (j.golosTorneio > 0) "⚽ ${j.golosTorneio}" else ""
                            setTextColor(Color.parseColor("#F57C00"))
                            textSize = 12f
                        }
                        row.addView(tvNome)
                        row.addView(tvGolos)
                        if (j.assistenciasTorneio > 0) {
                            val tvAssist = TextView(ctx).apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                ).also { it.marginStart = 8 }
                                val raw = "(A ${j.assistenciasTorneio})"
                                val span = SpannableString(raw)
                                span.setSpan(ForegroundColorSpan(Color.parseColor("#F44336")), 1, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                span.setSpan(StyleSpan(Typeface.BOLD), 1, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                text = span
                                setTextColor(Color.parseColor("#8A9BB8"))
                                textSize = 12f
                            }
                            row.addView(tvAssist)
                        }
                        b.layoutJogadores.addView(row)
                    }

                    if (jogadores.isEmpty()) {
                        val ctx = b.root.context
                        val tvEmpty = TextView(ctx).apply {
                            text = ctx.getString(R.string.sem_jogadores_inscritos)
                            setTextColor(Color.parseColor("#4A5C7A"))
                            textSize = 11f
                        }
                        b.layoutJogadores.addView(tvEmpty)
                    }
                }

                b.layoutHeader.setOnClickListener { onToggle() }
            }
        }
    }
}
