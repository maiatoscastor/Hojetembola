package com.hojetembola.app.ui.home

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.hojetembola.app.R
import com.hojetembola.app.data.local.entity.TorneioEntity
import com.hojetembola.app.databinding.FragmentHomeBinding
import com.hojetembola.app.databinding.ItemTorneioMeuBinding
import com.hojetembola.app.utils.collectFlow
import com.hojetembola.app.utils.gone
import com.hojetembola.app.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private fun setupListeners() {
        binding.btnTentarNovamente.setOnClickListener { viewModel.load() }
        binding.btnNotificacoes.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.em_breve), Toast.LENGTH_SHORT).show()
        }
        binding.btnCriarTorneioOrg.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_criarTorneioFragment)
        }
        binding.btnVerTorneios.setOnClickListener {
            findNavController().navigate(R.id.torneiosFragment)
        }
        binding.btnIrParaJogoVivo.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.em_breve), Toast.LENGTH_SHORT).show()
        }
        binding.cardJogoVivo.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.em_breve), Toast.LENGTH_SHORT).show()
        }
        binding.cardProximoJogo.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.em_breve), Toast.LENGTH_SHORT).show()
        }
    }

    // ── Observadores ──────────────────────────────────────────────────────────

    private fun observeViewModel() {
        collectFlow(viewModel.uiState) { state ->
            when (state) {
                is HomeUiState.Loading          -> showLoading()
                is HomeUiState.JogadorState     -> showJogador(state)
                is HomeUiState.OrganizadorState -> showOrganizador(state)
                is HomeUiState.Error            -> showError(state.message)
            }
        }
    }

    // ── Estados visuais ───────────────────────────────────────────────────────

    private fun showLoading() {
        binding.progressBar.visible()
        binding.scrollContent.gone()
        binding.layoutErro.gone()
    }

    private fun showError(message: String) {
        binding.progressBar.gone()
        binding.scrollContent.gone()
        binding.layoutErro.visible()
        binding.tvMensagemErro.text = message
    }

    private fun showJogador(state: HomeUiState.JogadorState) {
        binding.progressBar.gone()
        binding.layoutErro.gone()
        binding.scrollContent.visible()

        binding.layoutJogador.visible()
        binding.layoutOrganizador.gone()

        // Header
        binding.tvSaudacao.text = getSaudacao()
        binding.tvNomeJogador.text = state.utilizador.nome

        // KPIs
        binding.tvKpiTorneios.text = state.totalTorneios.toString()
        binding.tvKpiTorneiosSub.text = getString(R.string.torneios_ativos_sub, state.torneiosAtivos)
        binding.tvKpiGolos.text = state.totalGolos.toString()
        binding.tvKpiGolosSub.text = "—"

        // Jogo ao vivo
        val jogoVivo = state.jogoAoVivo
        if (jogoVivo != null) {
            binding.cardJogoVivo.visible()
            binding.tvSemJogoVivo.gone()
            val minuto = jogoVivo.minutoAtual
            binding.tvJogoVivoMinuto.text = if (minuto != null) getString(R.string.minuto_jogo, minuto) else ""
            binding.tvJogoVivoLocal.text = if (jogoVivo.local.isNotBlank()) "· ${jogoVivo.local}" else ""
            binding.tvJogoVivoEquipaCasa.text = jogoVivo.equipaCasaNome
            binding.tvJogoVivoResultado.text = getString(
                R.string.resultado_jogo, jogoVivo.golosCasa, jogoVivo.golosVisitante
            )
            binding.tvJogoVivoEquipaVisitante.text = jogoVivo.equipaVisitanteNome
        } else {
            binding.cardJogoVivo.gone()
            binding.tvSemJogoVivo.visible()
        }

        // Próximo jogo
        val proximoJogo = state.proximoJogo
        if (proximoJogo != null) {
            binding.cardProximoJogo.visible()
            binding.tvProximoEquipaCasa.text = proximoJogo.equipaCasaNome
            binding.tvProximoEquipaVisitante.text = proximoJogo.equipaVisitanteNome
            binding.tvProximoDataHora.text = buildString {
                val hora = formatHora(proximoJogo.dataHora)
                if (hora.isNotBlank()) append("$hora · ")
                append(proximoJogo.local)
            }
        } else {
            binding.cardProximoJogo.gone()
        }

        // Meus torneios
        binding.containerMeusTorneios.removeAllViews()
        if (state.meusTorneios.isEmpty()) {
            binding.tvSemTorneiosJogador.visible()
        } else {
            binding.tvSemTorneiosJogador.gone()
            state.meusTorneios.forEach { torneio ->
                inflateTorneioCard(binding.containerMeusTorneios, torneio)
            }
        }
    }

    private fun showOrganizador(state: HomeUiState.OrganizadorState) {
        binding.progressBar.gone()
        binding.layoutErro.gone()
        binding.scrollContent.visible()

        binding.layoutOrganizador.visible()
        binding.layoutJogador.gone()

        // Header
        binding.tvNomeOrganizador.text = state.utilizador.nome

        // KPIs
        binding.tvOrgTorneiosAtivos.text = state.torneiosAtivos.toString()
        binding.tvOrgTorneiosAtivosSub.text = getString(R.string.inscricoes_abertas_sub, state.torneiosComInscricoes)
        binding.tvOrgParticipantes.text = state.torneiosGeridos.sumOf { it.maxEquipas }.toString()

        // Alertas
        binding.containerAlertas.removeAllViews()
        if (state.torneiosComInscricoes > 0) {
            addAlertaItem(
                getString(R.string.alerta_inscricoes_abertas, state.torneiosComInscricoes)
            )
        }
        if (state.jogoAoVivo != null) {
            addAlertaItem(getString(R.string.alerta_jogo_ao_vivo))
        }
        if (state.torneiosComInscricoes == 0 && state.jogoAoVivo == null) {
            addAlertaItem(getString(R.string.sem_alertas))
        }

        // Torneios geridos
        binding.containerTorneiosOrg.removeAllViews()
        if (state.torneiosGeridos.isEmpty()) {
            binding.tvSemTorneiosOrg.visible()
        } else {
            binding.tvSemTorneiosOrg.gone()
            state.torneiosGeridos.forEach { torneio ->
                inflateTorneioCard(binding.containerTorneiosOrg, torneio)
            }
        }

        // Próximos jogos (card do organizador com jogo ao vivo se houver)
        if (state.jogoAoVivo != null) {
            binding.cardProximosJogosOrg.visible()
            binding.containerProximosJogos.removeAllViews()
            addJogoItem(
                buildString {
                    val hora = formatHora(state.jogoAoVivo.dataHora)
                    if (hora.isNotBlank()) append("$hora · ")
                    append("${state.jogoAoVivo.equipaCasaNome} vs ${state.jogoAoVivo.equipaVisitanteNome}")
                    if (state.jogoAoVivo.local.isNotBlank()) append(" · ${state.jogoAoVivo.local}")
                }
            )
        } else {
            binding.cardProximosJogosOrg.gone()
        }
    }

    // ── Helpers de UI ─────────────────────────────────────────────────────────

    private fun inflateTorneioCard(container: ViewGroup, torneio: TorneioEntity) {
        val ctx = requireContext()
        val b = ItemTorneioMeuBinding.inflate(layoutInflater, container, false)
        b.tvNomeTorneio.text = torneio.nome
        b.tvSubtitulo.text = "${torneio.formato.toFormatoLabel(ctx)} · ${torneio.modalidade.toModalidadeLabel()}"
        b.tvLocalizacao.text = torneio.localizacao
        applyBadge(b.tvEstado, torneio.estado, ctx)
        b.root.setOnClickListener {
            Toast.makeText(ctx, getString(R.string.em_breve), Toast.LENGTH_SHORT).show()
        }
        container.addView(b.root)
    }

    private fun addAlertaItem(texto: String) {
        val tv = TextView(requireContext()).apply {
            text = texto
            setTextColor(Color.parseColor("#8A9BB8"))
            textSize = 11f
            typeface = resources.getFont(R.font.poppins_regular)
            val pad = resources.getDimensionPixelSize(R.dimen.spacing_xs)
            setPadding(0, pad, 0, 0)
        }
        binding.containerAlertas.addView(tv)
    }

    private fun addJogoItem(texto: String) {
        val tv = TextView(requireContext()).apply {
            text = texto
            setTextColor(Color.parseColor("#8A9BB8"))
            textSize = 11f
            typeface = resources.getFont(R.font.poppins_regular)
            val pad = resources.getDimensionPixelSize(R.dimen.spacing_xs)
            setPadding(0, pad, 0, 0)
        }
        binding.containerProximosJogos.addView(tv)
    }

    private fun applyBadge(tv: TextView, estado: String, ctx: Context) {
        val (label, bgRes, hexColor) = when (estado) {
            "a_decorrer"          -> Triple(ctx.getString(R.string.estado_a_decorrer),         R.drawable.bg_badge_live, "#F57C00")
            "inscricoes_abertas"  -> Triple(ctx.getString(R.string.estado_inscricoes_abertas), R.drawable.bg_badge_open, "#4CAF50")
            "terminado"           -> Triple(ctx.getString(R.string.estado_terminado),          R.drawable.bg_badge_done, "#8A9BB8")
            else                  -> Triple(ctx.getString(R.string.estado_criado),             R.drawable.bg_badge_done, "#8A9BB8")
        }
        tv.text = label
        tv.setBackgroundResource(bgRes)
        tv.setTextColor(Color.parseColor(hexColor))
    }

    private fun getSaudacao(): String {
        val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hora < 12 -> getString(R.string.bom_dia)
            hora < 19 -> getString(R.string.boa_tarde)
            else      -> getString(R.string.boa_noite)
        }
    }

    private fun formatHora(dataHora: String): String = try {
        dataHora.substringAfter("T").take(5)
    } catch (_: Exception) { "" }

    private fun String.toFormatoLabel(ctx: Context) = when (this) {
        "liga"                 -> ctx.getString(R.string.liga)
        "eliminatorias"        -> ctx.getString(R.string.eliminatorias)
        "grupos_eliminatorias" -> ctx.getString(R.string.grupos_eliminatorias)
        "todos_vs_todos"       -> ctx.getString(R.string.todos_vs_todos)
        else                   -> this
    }

    private fun String.toModalidadeLabel() = when (this) {
        "fut5"          -> "Fut5"
        "fut7"          -> "Fut7"
        "fut11"         -> "Fut11"
        "personalizado" -> getString(R.string.personalizado)
        else            -> this
    }
}

