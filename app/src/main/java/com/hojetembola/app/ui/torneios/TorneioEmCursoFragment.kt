package com.hojetembola.app.ui.torneios

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.TextUtils
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
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
import com.hojetembola.app.data.local.dao.JogoComEquipas
import com.hojetembola.app.data.local.entity.InscricaoComEquipa
import com.hojetembola.app.databinding.FragmentTorneioEmCursoBinding
import com.hojetembola.app.databinding.ItemBracketMatchBinding
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
    private var isEliminationFormat: Boolean = false
    private var isGruposEliminatorias: Boolean = false

    private val tabListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) { currentTab = tab.position; showTab(currentTab) }
        override fun onTabUnselected(tab: TabLayout.Tab) = Unit
        override fun onTabReselected(tab: TabLayout.Tab) = Unit
    }

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
        isEliminationFormat = false
        isGruposEliminatorias = false
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

        binding.tabLayout.addOnTabSelectedListener(tabListener)

        showTab(currentTab)
    }

    private fun showTab(index: Int) {
        if (isEliminationFormat) {
            // Elimination layout: tab0=Bracket, tab1=Teams, tab2=Info (no Standings)
            binding.rvClassificacao.isVisible = false
            binding.scrollGrupos.isVisible    = false
            binding.scrollBracket.isVisible   = index == 0
            binding.rvJornadas.isVisible      = false
            binding.rvEquipas.isVisible       = index == 1
            binding.scrollInfo.isVisible      = index == 2
        } else if (isGruposEliminatorias) {
            // Groups+Knockout: tab0=Standings, tab1=Rounds, tab2=Bracket, tab3=Teams, tab4=Info
            binding.rvClassificacao.isVisible = false
            binding.scrollGrupos.isVisible    = index == 0
            binding.rvJornadas.isVisible      = index == 1
            binding.scrollBracket.isVisible   = index == 2
            binding.rvEquipas.isVisible       = index == 3
            binding.scrollInfo.isVisible      = index == 4
        } else {
            binding.rvClassificacao.isVisible = index == 0
            binding.scrollGrupos.isVisible    = false
            binding.rvJornadas.isVisible      = index == 1
            binding.scrollBracket.isVisible   = false
            binding.rvEquipas.isVisible       = index == 2
            binding.scrollInfo.isVisible      = index == 3
        }
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

                            val fmt = torneio.formato.trim().lowercase()
                            val isElim = fmt == "eliminatorias"
                            val isGrupos = fmt.contains("grupos") && fmt.contains("eliminatori")
                            android.util.Log.d("TorneioFmt", "formato='${torneio.formato}' isElim=$isElim isGrupos=$isGrupos")
                            if (isElim != isEliminationFormat || isGrupos != isGruposEliminatorias) {
                                isEliminationFormat = isElim
                                isGruposEliminatorias = isGrupos
                                rebuildTabs(isElim)
                            }
                        }

                        classificacaoAdapter.submitList(state.classificacao)

                        if (isGruposEliminatorias) {
                            renderGroupStandings(state.classificacao, state.equipas, state.jornadas)
                        }

                        val items = mutableListOf<CalendarioItem>()
                        state.jornadas.forEach { jc ->
                            items.add(CalendarioItem.Header(jc.jornada))
                            jc.jogos.forEach { items.add(CalendarioItem.Match(it)) }
                        }
                        jornadasAdapter.submitList(items)

                        when {
                            isEliminationFormat -> {
                                renderBracket(
                                    state.jornadas,
                                    viewModel.torneioId,
                                    state.isOrganizador,
                                    state.torneio?.estado ?: ""
                                )
                            }
                            isGruposEliminatorias -> {
                                // Bracket shows only knockout rounds
                                val knockoutJornadas = state.jornadas
                                    .filter { !it.jornada.nome.startsWith("Grupo") }
                                renderBracket(
                                    knockoutJornadas,
                                    viewModel.torneioId,
                                    state.isOrganizador,
                                    state.torneio?.estado ?: ""
                                )
                            }
                            else -> {
                                // Liga / TodosContraTodos: champion banner from standings
                                val torneioEstado = state.torneio?.estado ?: ""
                                if (torneioEstado.lowercase() in listOf("terminado", "terminated")) {
                                    val vencedor = state.classificacao.firstOrNull()
                                    if (vencedor != null) {
                                        binding.layoutCampeao.isVisible = true
                                        binding.tvCampeaoNome.text = vencedor.equipaNome
                                        val cor = try { Color.parseColor(vencedor.equipaCor) } catch (_: Exception) { Color.parseColor("#3D5A80") }
                                        binding.vCorCampeao.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(cor) }
                                    } else {
                                        binding.layoutCampeao.isVisible = false
                                    }
                                } else {
                                    binding.layoutCampeao.isVisible = false
                                }
                            }
                        }

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

    // ── Tab rebuild ───────────────────────────────────────────────────────────

    private fun rebuildTabs(isElim: Boolean) {
        binding.tabLayout.removeOnTabSelectedListener(tabListener)
        binding.tabLayout.removeAllTabs()

        val labels = when {
            isElim -> listOf(
                getString(R.string.tab_bracket),
                getString(R.string.tab_equipas),
                getString(R.string.tab_info)
            )
            isGruposEliminatorias -> listOf(
                getString(R.string.tab_classificacao),
                getString(R.string.tab_jornadas),
                getString(R.string.tab_bracket),
                getString(R.string.tab_equipas),
                getString(R.string.tab_info)
            )
            else -> listOf(
                getString(R.string.tab_classificacao),
                getString(R.string.tab_jornadas),
                getString(R.string.tab_equipas),
                getString(R.string.tab_info)
            )
        }
        labels.forEach { binding.tabLayout.addTab(binding.tabLayout.newTab().setText(it)) }
        binding.tabLayout.addOnTabSelectedListener(tabListener)

        // Select first tab (Bracket for elim, Standings for others)
        currentTab = 0
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))
        showTab(0)
    }

    // ── Bracket ───────────────────────────────────────────────────────────────

    private fun renderBracket(
        jornadas: List<JornadaComJogos>,
        torneioId: String,
        isOrganizador: Boolean,
        torneioEstado: String = ""
    ) {
        val container = binding.linearBracket
        container.removeAllViews()

        val rounds = jornadas.filter { it.jogos.isNotEmpty() }.sortedBy { it.jornada.numero }
        if (rounds.isEmpty()) return
        val firstRoundCount = rounds.first().jogos.size

        val totalRounds = rounds.size
        val columnWidthPx = dpToPx(172)
        val gapPx = dpToPx(10)

        // Show champion banner if tournament is finished
        val isTorneioTerminado = torneioEstado.lowercase() in listOf("terminado", "terminated")
        val finalJogo = rounds.lastOrNull()?.jogos?.firstOrNull()
        if (isTorneioTerminado && finalJogo != null && finalJogo.estado == "terminado") {
            val casaGanhou = (finalJogo.golosCasa ?: 0) >= (finalJogo.golosVisitante ?: 0)
            val campeaoNome = if (casaGanhou) finalJogo.casaNome else finalJogo.visitanteNome
            val campeaoCor  = if (casaGanhou) finalJogo.casaCor  else finalJogo.visitanteCor
            binding.layoutCampeao.isVisible = true
            binding.tvCampeaoNome.text = campeaoNome
            val cor = try { Color.parseColor(campeaoCor) } catch (_: Exception) { Color.parseColor("#3D5A80") }
            binding.vCorCampeao.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(cor) }
        } else {
            binding.layoutCampeao.isVisible = false
        }

        rounds.forEachIndexed { ri, jc ->
            val slotsPerMatch = 1.shl(ri) // 2^ri

            val colLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(columnWidthPx, LinearLayout.LayoutParams.MATCH_PARENT)
            }

            // Round name header
            val tvHeader = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = getRoundName(jc.jogos.size)
                textSize = 11f
                setTextColor(Color.parseColor("#8A9BB8"))
                typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_semi_bold)
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(0, 0, 0, dpToPx(6))
            }
            colLayout.addView(tvHeader)

            // Match slots (weight-based vertical distribution)
            val matchesLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                weightSum = firstRoundCount.toFloat()
            }

            jc.jogos.forEach { jogo ->
                val slot = FrameLayout(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        slotsPerMatch.toFloat()
                    )
                }
                val cardBinding = ItemBracketMatchBinding.inflate(layoutInflater, slot, false)
                bindBracketCard(cardBinding, jogo, torneioId, isOrganizador)
                cardBinding.root.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL
                ).apply { setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4)) }
                slot.addView(cardBinding.root)
                matchesLayout.addView(slot)
            }

            colLayout.addView(matchesLayout)
            container.addView(colLayout)

            // Gap between columns
            if (ri < totalRounds - 1) {
                container.addView(View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(gapPx, LinearLayout.LayoutParams.MATCH_PARENT)
                })
            }
        }
    }

    private fun bindBracketCard(
        b: ItemBracketMatchBinding,
        jogo: JogoComEquipas,
        torneioId: String,
        isOrganizador: Boolean
    ) {
        val casaNome = jogo.casaNome.ifBlank { getString(R.string.a_definir) }
        val visitanteNome = jogo.visitanteNome.ifBlank { getString(R.string.a_definir) }
        b.tvNomeCasa.text = casaNome
        b.tvNomeVisitante.text = visitanteNome

        val casaColor = try { Color.parseColor(jogo.casaCor) } catch (_: Exception) { Color.parseColor("#3D5A80") }
        val visitanteColor = try { Color.parseColor(jogo.visitanteCor) } catch (_: Exception) { Color.parseColor("#3D5A80") }
        b.vCorCasa.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(casaColor) }
        b.vCorVisitante.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(visitanteColor) }

        val hasScore = jogo.estado in listOf("ao_vivo", "terminado")
        b.tvGoloCasa.text = if (hasScore) (jogo.golosCasa ?: 0).toString() else "-"
        b.tvGoloVisitante.text = if (hasScore) (jogo.golosVisitante ?: 0).toString() else "-"

        if (jogo.estado == "terminado" && jogo.golosCasa != null && jogo.golosVisitante != null) {
            val casaWon = jogo.golosCasa > jogo.golosVisitante
            b.tvNomeCasa.alpha = if (casaWon) 1f else 0.45f
            b.tvGoloCasa.alpha = if (casaWon) 1f else 0.45f
            b.vCorCasa.alpha = if (casaWon) 1f else 0.45f
            b.tvNomeVisitante.alpha = if (!casaWon) 1f else 0.45f
            b.tvGoloVisitante.alpha = if (!casaWon) 1f else 0.45f
            b.vCorVisitante.alpha = if (!casaWon) 1f else 0.45f
            val winnerScoreColor = Color.parseColor("#F57C00")
            if (casaWon) b.tvGoloCasa.setTextColor(winnerScoreColor)
            else b.tvGoloVisitante.setTextColor(winnerScoreColor)
        }

        b.root.setOnClickListener {
            findNavController().navigate(
                R.id.action_torneioEmCursoFragment_to_jogoDetalheFragment,
                bundleOf(
                    "jogoId"        to jogo.jogoId,
                    "torneioId"     to torneioId,
                    "isOrganizador" to isOrganizador
                )
            )
        }
    }

    private fun getRoundName(matchCount: Int): String {
        return when (matchCount) {
            1    -> getString(R.string.round_final)
            2    -> getString(R.string.round_meia_final)
            4    -> getString(R.string.round_quartos)
            8    -> getString(R.string.round_oitavos)
            else -> getString(R.string.round_numero, matchCount)
        }
    }

    // ── Group standings ───────────────────────────────────────────────────────

    private fun renderGroupStandings(
        classificacao: List<ClassificacaoTorneioRow>,
        equipas: List<InscricaoComEquipa>,
        jornadas: List<JornadaComJogos>
    ) {
        val container = binding.linearGrupos
        container.removeAllViews()
        val ctx = requireContext()

        // Primary: use grupo field from inscricao
        var groupedEquipas: Map<String, List<InscricaoComEquipa>> = equipas
            .filter { !it.grupo.isNullOrBlank() }
            .groupBy { it.grupo!! }
            .toSortedMap()

        // Fallback: derive groups from jornada names ("Grupo A — Jornada 1")
        if (groupedEquipas.isEmpty()) {
            val grupoToEquipaIds = mutableMapOf<String, MutableSet<String>>()
            for (jc in jornadas) {
                val nome = jc.jornada.nome
                if (!nome.startsWith("Grupo ")) continue
                val grupoChar = nome.removePrefix("Grupo ").substringBefore(" ").trim()
                if (grupoChar.isBlank()) continue
                val ids = jc.jogos.flatMap { listOf(it.equipaCasaId, it.equipaVisitanteId) }
                grupoToEquipaIds.getOrPut(grupoChar) { mutableSetOf() }.addAll(ids)
            }
            if (grupoToEquipaIds.isNotEmpty()) {
                val equipaById = equipas.associateBy { it.equipaId }
                groupedEquipas = grupoToEquipaIds
                    .mapValues { (_, ids) -> ids.mapNotNull { equipaById[it] } }
                    .toSortedMap()
            }
        }

        if (groupedEquipas.isEmpty()) return

        val classificacaoById = classificacao.associateBy { it.equipaId }

        groupedEquipas.forEach { (grupo, membros) ->
            // Group title
            val tvGrupo = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(6)) }
                text = "Grupo $grupo"
                textSize = 13f
                setTextColor(Color.parseColor("#FFFFFF"))
                typeface = ResourcesCompat.getFont(ctx, R.font.poppins_bold)
            }
            container.addView(tvGrupo)

            // Header row
            container.addView(makeStandingsRow(
                ctx,
                pos = "Pos", equipa = "Equipa", j = "J", v = "V",
                e = "E", d = "D", gm = "GM", gs = "GS", pts = "Pts",
                isHeader = true
            ))

            // Team rows
            val membrosOrdenados = membros.sortedBy {
                classificacaoById[it.equipaId]?.posicao ?: Int.MAX_VALUE
            }
            membrosOrdenados.forEachIndexed { idx, inscricao ->
                val row = classificacaoById[inscricao.equipaId]
                container.addView(makeStandingsRow(
                    ctx,
                    pos = (idx + 1).toString(),
                    equipa = inscricao.nome,
                    j = row?.jogos?.toString() ?: "0",
                    v = row?.vitorias?.toString() ?: "0",
                    e = row?.empates?.toString() ?: "0",
                    d = row?.derrotas?.toString() ?: "0",
                    gm = row?.golosMarcados?.toString() ?: "0",
                    gs = row?.golosSofridos?.toString() ?: "0",
                    pts = row?.pontos?.toString() ?: "0",
                    isHeader = false
                ))
            }

            // Divider between groups
            container.addView(View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)
                ).also { it.setMargins(dpToPx(16), dpToPx(12), dpToPx(16), 0) }
                setBackgroundColor(Color.parseColor("#1E2D45"))
            })
        }
    }

    private fun makeStandingsRow(
        ctx: Context,
        pos: String, equipa: String, j: String, v: String, e: String,
        d: String, gm: String, gs: String, pts: String,
        isHeader: Boolean
    ): LinearLayout {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(dpToPx(6), 0, dpToPx(6), 0) }
            setPadding(0, dpToPx(6), 0, dpToPx(6))
            gravity = Gravity.CENTER_VERTICAL
        }

        val headerColor = Color.parseColor("#8A9BB8")
        val textColor   = if (isHeader) headerColor else Color.parseColor("#FFFFFF")
        val ptsColor    = if (isHeader) headerColor else Color.parseColor("#FFFFFF")
        val font        = if (isHeader) R.font.poppins_regular else R.font.poppins_regular
        val ptsFont     = if (isHeader) R.font.poppins_regular else R.font.poppins_bold

        fun cell(text: String, weight: Float, textCol: Int = textColor, fontRes: Int = font): TextView =
            TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
                this.text = text
                textSize = 11f
                setTextColor(textCol)
                gravity = Gravity.CENTER
                typeface = ResourcesCompat.getFont(ctx, fontRes)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }

        val tvEquipa = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            text = equipa
            textSize = 11f
            setTextColor(if (isHeader) headerColor else Color.parseColor("#E0E8F4"))
            typeface = ResourcesCompat.getFont(ctx, font)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        row.addView(cell(pos, 0.6f))
        row.addView(tvEquipa)
        row.addView(cell(j, 1f))
        row.addView(cell(v, 1f))
        row.addView(cell(e, 1f))
        row.addView(cell(d, 1f))
        row.addView(cell(gm, 1f))
        row.addView(cell(gs, 1f))
        row.addView(cell(pts, 1f, ptsColor, ptsFont))

        return row
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

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
                            gravity = Gravity.CENTER_VERTICAL
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
