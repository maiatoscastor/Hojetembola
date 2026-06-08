package com.hojetembola.app.ui.rankings

import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.hojetembola.app.R
import com.hojetembola.app.databinding.FragmentRankingsBinding
import com.hojetembola.app.databinding.ItemRankingPodiumBinding
import com.hojetembola.app.databinding.LayoutRankingPodiumSectionBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.abs

@AndroidEntryPoint
class RankingsFragment : Fragment() {

    private var _binding: FragmentRankingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RankingsViewModel by viewModels()

    private lateinit var geralAdapter: RankingListAdapter
    private lateinit var torneioAdapter: RankingListAdapter
    private lateinit var classificacaoAdapter: ClassificacaoAdapter
    private lateinit var classificacaoGeralAdapter: ClassificacaoAdapter

    private var isLandscape = false
    private var suppressDropdown = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRankingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        setupAdapters()
        setupTabs()
        setupDropdowns()
        observeState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupAdapters() {
        geralAdapter = createRankingAdapter(isLandscape)
        torneioAdapter = createRankingAdapter(isLandscape = false)

        classificacaoAdapter = ClassificacaoAdapter()
        classificacaoGeralAdapter = ClassificacaoAdapter()

        binding.rvGeral.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = geralAdapter
            isNestedScrollingEnabled = false
        }
        binding.rvTorneio.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = torneioAdapter
            isNestedScrollingEnabled = false
        }
        binding.rvClassificacao.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = classificacaoAdapter
            isNestedScrollingEnabled = false
        }
        findOptionalRecyclerView(R.id.rvClassificacaoGeral)?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = classificacaoGeralAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun createRankingAdapter(isLandscape: Boolean): RankingListAdapter =
        RankingListAdapter(
            isLandscape = isLandscape,
            trendFormatter = { entry -> formatTrend(entry, includeValue = !isLandscape) },
            trendColor = ::trendColor
        )

    private fun setupTabs() {
        binding.tabScope.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val scope = if (tab.position == 0) RankingScope.GERAL else RankingScope.TORNEIO
                viewModel.setScope(scope)
                updateScopePanels(scope)
                refreshDropdowns(scope)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }

    private fun setupDropdowns() {
        binding.actvMetricGeral.setOnItemClickListener { _, _, position, _ ->
            if (suppressDropdown) return@setOnItemClickListener
            metricsForScope(RankingScope.GERAL).getOrNull(position)?.let(viewModel::setMetric)
        }
        binding.actvPeriodGeral.setOnItemClickListener { _, _, position, _ ->
            if (suppressDropdown) return@setOnItemClickListener
            periodsForScope(RankingScope.GERAL).getOrNull(position)?.let(viewModel::setPeriod)
        }
        binding.actvMetricTorneio.setOnItemClickListener { _, _, position, _ ->
            if (suppressDropdown) return@setOnItemClickListener
            metricsForScope(RankingScope.TORNEIO).getOrNull(position)?.let(viewModel::setMetric)
        }
        binding.actvPeriodTorneio.setOnItemClickListener { _, _, position, _ ->
            if (suppressDropdown) return@setOnItemClickListener
            periodsForScope(RankingScope.TORNEIO).getOrNull(position)?.let(viewModel::setPeriod)
        }

        refreshDropdowns(RankingScope.GERAL)
        refreshDropdowns(RankingScope.TORNEIO)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is RankingsUiState.Loading -> showLoading()
                        is RankingsUiState.Content -> showContent(state.data)
                    }
                }
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.isVisible = true
        binding.scrollContent.isVisible = false
    }

    private fun showContent(data: RankingsContent) {
        binding.progressBar.isVisible = false
        binding.scrollContent.isVisible = true

        updateScopePanels(data.scope)
        syncDropdownSelection(data)

        val podiumSection = if (data.scope == RankingScope.GERAL) {
            binding.podiumGeral
        } else {
            binding.podiumTorneio
        }

        binding.podiumGeral.root.isVisible = data.scope == RankingScope.GERAL && data.showPodium
        binding.podiumTorneio.root.isVisible = data.scope == RankingScope.TORNEIO && data.showPodium

        if (data.showPodium) {
            bindPodium(podiumSection, data.podium)
        }

        data.torneioInfo?.let { info ->
            binding.tvTorneioNome.text = info.nome
            binding.tvTorneioMeta.text = info.descricao
            binding.tvTorneioEstado.text = getString(R.string.estado_a_decorrer)
            binding.tvTorneioEstado.isVisible = info.isLive
        }

        when (data.scope) {
            RankingScope.GERAL -> bindGeralContent(data)
            RankingScope.TORNEIO -> bindTorneioContent(data)
        }
    }

    private fun bindGeralContent(data: RankingsContent) {
        binding.layoutClassificacaoHeader.isVisible = false
        binding.rvTorneio.isVisible = false

        if (isLandscape && data.showClassificacao) {
            binding.podiumGeral.root.isVisible = false
            findOptionalView(R.id.layoutGeralContent)?.isVisible = false
            findOptionalView(R.id.layoutClassificacaoGeral)?.isVisible = true
            classificacaoGeralAdapter.submitList(data.classificacao)
            return
        }

        findOptionalView(R.id.layoutGeralContent)?.isVisible = true
        findOptionalView(R.id.layoutClassificacaoGeral)?.isVisible = false
        binding.podiumGeral.root.isVisible = data.showPodium
        binding.rvGeral.isVisible = true
        geralAdapter.submitList(data.entries)
    }

    private fun bindTorneioContent(data: RankingsContent) {
        findOptionalView(R.id.layoutGeralContent)?.isVisible = false
        findOptionalView(R.id.layoutClassificacaoGeral)?.isVisible = false

        binding.podiumTorneio.root.isVisible = data.showPodium
        binding.rvTorneio.isVisible = !data.showClassificacao
        binding.layoutClassificacaoHeader.isVisible = data.showClassificacao

        if (data.showClassificacao) {
            classificacaoAdapter.submitList(data.classificacao)
        } else {
            torneioAdapter.submitList(data.entries)
        }
    }

    private fun bindPodium(section: LayoutRankingPodiumSectionBinding, podium: List<RankingEntry>) {
        val pods = listOf(
            Triple(section.podFirst, 1, R.drawable.bg_podium_gold),
            Triple(section.podSecond, 2, R.drawable.bg_podium_silver),
            Triple(section.podThird, 3, R.drawable.bg_podium_bronze)
        )
        pods.forEachIndexed { index, (view, rank, bg) ->
            val entry = podium.getOrNull(index)
            val podBinding = ItemRankingPodiumBinding.bind(view.root)
            if (entry == null) {
                view.root.isVisible = false
                return@forEachIndexed
            }
            view.root.isVisible = true
            view.root.setBackgroundResource(bg)
            podBinding.tvRank.text = getString(R.string.rankings_pos_rank, rank)
            podBinding.tvValor.text = entry.valor.toString()
            podBinding.tvNome.text = entry.nome
            podBinding.tvEquipa.text = entry.equipa
            podBinding.tvAvatar.text = entry.iniciais
            val avatarBg = podBinding.tvAvatar.background.mutate() as GradientDrawable
            avatarBg.setColor(entry.avatarColor)
        }
    }

    private fun updateScopePanels(scope: RankingScope) {
        binding.panelGeral.isVisible = scope == RankingScope.GERAL
        binding.panelTorneio.isVisible = scope == RankingScope.TORNEIO
        binding.tabScope.getTabAt(if (scope == RankingScope.GERAL) 0 else 1)?.select()
    }

    private fun refreshDropdowns(scope: RankingScope) {
        val metrics = metricsForScope(scope).map { metricLabel(it) }
        val periods = periodsForScope(scope).map { periodLabel(it) }

        when (scope) {
            RankingScope.GERAL -> {
                setupDropdown(binding.actvMetricGeral, metrics)
                setupDropdown(binding.actvPeriodGeral, periods)
            }
            RankingScope.TORNEIO -> {
                setupDropdown(binding.actvMetricTorneio, metrics)
                setupDropdown(binding.actvPeriodTorneio, periods)
            }
        }
    }

    private fun syncDropdownSelection(data: RankingsContent) {
        suppressDropdown = true
        val metric = metricLabel(data.metric)
        val period = periodLabel(data.period)
        when (data.scope) {
            RankingScope.GERAL -> {
                binding.actvMetricGeral.setText(metric, false)
                binding.actvPeriodGeral.setText(period, false)
            }
            RankingScope.TORNEIO -> {
                binding.actvMetricTorneio.setText(metric, false)
                binding.actvPeriodTorneio.setText(period, false)
            }
        }
        suppressDropdown = false
    }

    private fun setupDropdown(view: AutoCompleteTextView, items: List<String>) {
        view.setAdapter(ArrayAdapter(requireContext(), R.layout.item_dropdown, items))
    }

    private fun metricsForScope(scope: RankingScope): List<RankingMetric> {
        val base = viewModel.metricsForScope(scope)
        return if (isLandscape && scope == RankingScope.GERAL) {
            base + RankingMetric.CLASSIFICACAO
        } else {
            base
        }
    }

    private fun periodsForScope(scope: RankingScope): List<RankingPeriod> =
        viewModel.periodsForScope(scope)

    private fun metricLabel(metric: RankingMetric): String = when (metric) {
        RankingMetric.GOLOS -> getString(R.string.golos)
        RankingMetric.JOGOS -> getString(R.string.jogos_disputados)
        RankingMetric.ASSISTENCIAS -> getString(R.string.stat_assistencias)
        RankingMetric.MINUTOS -> getString(R.string.stat_minutos_jogados)
        RankingMetric.AMARELOS -> getString(R.string.cartao_amarelo)
        RankingMetric.VERMELHOS -> getString(R.string.cartao_vermelho)
        RankingMetric.MVPS -> getString(R.string.stat_mvps)
        RankingMetric.CARTOES -> getString(R.string.rankings_cartoes)
        RankingMetric.CLASSIFICACAO -> getString(R.string.classificacao)
    }

    private fun periodLabel(period: RankingPeriod): String = when (period) {
        RankingPeriod.ESTA_JORNADA -> getString(R.string.esta_jornada)
        RankingPeriod.ESTE_MES -> getString(R.string.este_mes)
        RankingPeriod.EPOCA_TODA -> getString(R.string.epoca_toda)
        RankingPeriod.JORNADA_ATUAL -> getString(R.string.rankings_jornada_n, 4)
        RankingPeriod.TORNEIO_COMPLETO -> getString(R.string.rankings_periodo_torneio)
    }

    private fun formatTrend(entry: RankingEntry, includeValue: Boolean): String {
        if (entry.amarelos != null && entry.vermelhos != null) {
            return getString(R.string.rankings_cartoes_count, entry.amarelos, entry.vermelhos)
        }
        return when (entry.trend) {
            RankingTrend.UP -> if (includeValue) {
                getString(R.string.rankings_trend_up, entry.variacao ?: 0, entry.valor)
            } else {
                "▲ ${entry.variacao ?: 0}"
            }
            RankingTrend.DOWN -> if (includeValue) {
                getString(R.string.rankings_trend_down, abs(entry.variacao ?: 0), entry.valor)
            } else {
                "▼ ${abs(entry.variacao ?: 0)}"
            }
            RankingTrend.FLAT -> if (includeValue) {
                getString(R.string.rankings_trend_flat, entry.valor)
            } else {
                "—"
            }
        }
    }

    private fun trendColor(entry: RankingEntry): Int {
        if (entry.amarelos != null) {
            return ContextCompat.getColor(requireContext(), R.color.text_secondary)
        }
        val colorRes = when (entry.trend) {
            RankingTrend.UP -> R.color.green
            RankingTrend.DOWN -> R.color.color_trend_down
            RankingTrend.FLAT -> R.color.text_secondary
        }
        return ContextCompat.getColor(requireContext(), colorRes)
    }

    private fun findOptionalView(id: Int): View? = binding.root.findViewById(id)

    private fun findOptionalRecyclerView(id: Int): RecyclerView? =
        binding.root.findViewById(id)
}
