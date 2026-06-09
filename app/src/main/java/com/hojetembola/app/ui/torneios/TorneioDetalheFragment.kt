package com.hojetembola.app.ui.torneios

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.hojetembola.app.R
import com.hojetembola.app.data.local.entity.TorneioEntity
import com.hojetembola.app.data.local.entity.minJogadoresPorEquipa
import com.hojetembola.app.databinding.FragmentTorneioDetalheBinding
import com.hojetembola.app.ui.equipa.InscricaoTorneioBottomSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class TorneioDetalheFragment : Fragment() {

    private var _binding: FragmentTorneioDetalheBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TorneioDetalheViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTorneioDetalheBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        observeState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── State observation ─────────────────────────────────────────────────────

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is TorneioDetalheUiState.Loading -> showLoading()
                        is TorneioDetalheUiState.Error   -> showError(state.message)
                        is TorneioDetalheUiState.Content -> showContent(state)
                    }
                }
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.isVisible  = true
        binding.scrollContent.isVisible = false
        binding.tvError.isVisible       = false
        binding.btnAcao.isVisible       = false
    }

    private fun showError(msg: String) {
        binding.progressBar.isVisible  = false
        binding.scrollContent.isVisible = false
        binding.tvError.isVisible       = true
        binding.tvError.text            = msg
        binding.btnAcao.isVisible       = false
    }

    private fun showContent(state: TorneioDetalheUiState.Content) {
        val t = state.torneio
        binding.progressBar.isVisible  = false
        binding.tvError.isVisible       = false
        binding.scrollContent.isVisible = true

        // Top bar
        binding.tvTituloTopBar.text = t.nome
        applyEstadoBadge(t.estado)

        // Informações gerais
        binding.tvModalidade.text = t.modalidade.toModalidadeLabel()
        binding.tvFormato.text    = t.formato.toFormatoLabel()
        binding.tvMaxEquipas.text = t.maxEquipas.toString()
        binding.tvMaxJogadores.text = t.maxJogadoresPorEquipa.toString()

        // Datas
        binding.tvDataInicioInscricoes.text = t.dataInicioInscricoes.formatDate()
        binding.tvDataFimInscricoes.text    = t.dataFimInscricoes.formatDate()
        binding.tvDataInicio.text           = t.dataInicio.formatDate()
        if (t.dataFimPrevista.isNotBlank()) {
            binding.layoutDataFim.isVisible = true
            binding.tvDataFim.text = t.dataFimPrevista.formatDate()
        } else {
            binding.layoutDataFim.isVisible = false
        }

        // Localização
        binding.tvLocalizacaoNome.text = t.localizacaoNome
        if (!t.localizacaoMorada.isNullOrBlank()) {
            binding.tvLocalizacaoMorada.isVisible = true
            binding.tvLocalizacaoMorada.text = t.localizacaoMorada
        } else {
            binding.tvLocalizacaoMorada.isVisible = false
        }
        val mapsUrl = t.localizacaoMapsUrl
        if (!mapsUrl.isNullOrBlank() && mapsUrl.isValidUrl()) {
            binding.tvMapsLink.isVisible = true
            binding.tvMapsLink.setOnClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl)))
                } catch (_: Exception) {
                    Snackbar.make(binding.root, R.string.erro_abrir_mapa, Snackbar.LENGTH_SHORT).show()
                }
            }
        } else {
            binding.tvMapsLink.isVisible = false
        }

        // Regras
        binding.tvCriterio.text  = t.criterioDesempate.toCriterioLabel()
        if (t.criterioDesempate == "Prolongamento") {
            binding.layoutTempoExtra.isVisible = true
            binding.tvTempoExtra.text = getString(R.string.minutos_formato, t.tempoExtraMinutos)
        } else {
            binding.layoutTempoExtra.isVisible = false
        }
        binding.tvAmarelas.text = t.amarelasParaSuspensao.toString()
        val extras = buildList {
            if (t.permitirEspectadores) add(getString(R.string.permitir_espectadores))
            if (t.votacaoMvpAtiva)      add(getString(R.string.votacao_mvp_ativa))
        }
        binding.tvExtras.text = if (extras.isEmpty()) getString(R.string.nenhum) else extras.joinToString("\n")

        // Código de acesso (visível ao organizador se o torneio é privado)
        if (state.isOrganizador && t.visibilidade == "Privado" && !t.codigoAcesso.isNullOrBlank()) {
            binding.cardCodigo.isVisible   = true
            binding.tvCodigoAcesso.text    = t.codigoAcesso
        } else {
            binding.cardCodigo.isVisible = false
        }

        // Regulamento
        if (!t.regulamento.isNullOrBlank()) {
            binding.cardRegulamento.isVisible = true
            binding.tvRegulamento.text        = t.regulamento
        } else {
            binding.cardRegulamento.isVisible = false
        }

        // Botão de ação
        setupActionButton(state)
    }

    private fun setupActionButton(state: TorneioDetalheUiState.Content) {
        val t = state.torneio
        when {
            // Organizador vê o botão de gerir (apenas placeholder por agora)
            state.isOrganizador -> {
                binding.btnAcao.isVisible = true
                binding.btnAcao.text = getString(R.string.gerir_torneio)
                binding.btnAcao.setOnClickListener {
                    Snackbar.make(binding.root, R.string.em_breve, Snackbar.LENGTH_SHORT).show()
                }
            }
            // Utilizador regular pode inscrever equipa quando inscrições estão abertas
            t.estado == "InscricoesAbertas" || t.estado == "inscricoes_abertas" -> {
                binding.btnAcao.isVisible = true
                binding.btnAcao.text = getString(R.string.inscrever_equipa)
                binding.btnAcao.setOnClickListener {
                    abrirInscricaoBottomSheet(state.torneio)
                }
            }
            else -> binding.btnAcao.isVisible = false
        }
    }

    // ── Navigation helpers ────────────────────────────────────────────────────

    private fun abrirInscricaoBottomSheet(torneio: TorneioEntity) {
        // Avoid opening multiple sheets
        if (parentFragmentManager.findFragmentByTag(InscricaoTorneioBottomSheet.TAG) != null) return

        val torneioId = torneio.id
        val min = torneio.minJogadoresPorEquipa()
        val max = torneio.maxJogadoresPorEquipa

        val sheet = InscricaoTorneioBottomSheet.newInstance(torneioId)

        // Sem equipa → criar nova (navega para CriarEquipa com contexto do torneio)
        sheet.onNavegarParaCriarEquipa = {
            findNavController().navigate(
                R.id.action_torneioDetalheFragment_to_criarEquipaFragment,
                bundleOf(
                    "torneioId"    to torneioId,
                    "minJogadores" to min,
                    "maxJogadores" to max
                )
            )
        }

        // Poucos membros → gerir equipa para adicionar jogadores
        sheet.onNavegarParaGerirEquipa = { equipaId, equipaNome ->
            findNavController().navigate(
                R.id.action_torneioDetalheFragment_to_gerirEquipaFragment,
                bundleOf(
                    "equipaId"     to equipaId,
                    "equipaNome"   to equipaNome,
                    "torneioId"    to torneioId,
                    "minJogadores" to min,
                    "maxJogadores" to max
                )
            )
        }

        sheet.show(parentFragmentManager, InscricaoTorneioBottomSheet.TAG)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun applyEstadoBadge(estado: String) {
        val (label, bgRes, hexColor) = when (estado) {
            "ADecorrer",         "a_decorrer"       -> Triple(getString(R.string.estado_a_decorrer),         R.drawable.bg_badge_live, "#F57C00")
            "InscricoesAbertas", "inscricoes_abertas" -> Triple(getString(R.string.estado_inscricoes_abertas), R.drawable.bg_badge_open, "#4CAF50")
            "Terminado",         "terminado"         -> Triple(getString(R.string.estado_terminado),          R.drawable.bg_badge_done, "#8A9BB8")
            else                                     -> Triple(getString(R.string.estado_criado),             R.drawable.bg_badge_done, "#8A9BB8")
        }
        binding.tvEstadoBadge.text = label
        binding.tvEstadoBadge.setBackgroundResource(bgRes)
        binding.tvEstadoBadge.setTextColor(Color.parseColor(hexColor))
    }

    private fun String.isValidUrl() =
        startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)

    private fun String.formatDate(): String = try {
        val parsed = LocalDate.parse(this)
        val fmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("pt", "PT"))
        parsed.format(fmt)
    } catch (_: Exception) { this }

    // ── Label helpers (compatíveis com valores antigos em minúsculas) ─────────

    private fun String.toModalidadeLabel() = when (lowercase()) {
        "fut5"          -> "Fut5"
        "fut7"          -> "Fut7"
        "fut11"         -> "Fut11"
        "personalizado" -> getString(R.string.personalizado)
        else            -> this
    }

    private fun String.toFormatoLabel() = when (this) {
        "Liga",  "liga"                            -> getString(R.string.liga)
        "Eliminatorias", "eliminatorias"           -> getString(R.string.eliminatorias)
        "GruposEliminatorias", "grupos_eliminatorias" -> getString(R.string.grupos_eliminatorias)
        "TodosContraTodos", "todos_vs_todos"       -> getString(R.string.todos_vs_todos)
        else                                       -> this
    }

    private fun String.toCriterioLabel() = when (this) {
        "Penalidades",   "penalidades"   -> getString(R.string.penalidades)
        "Prolongamento", "prolongamento" -> getString(R.string.prolongamento)
        "GoloDeOuro",    "golo_de_ouro"  -> getString(R.string.golo_de_ouro)
        else                             -> this
    }

    companion object {
        fun newBundle(torneioId: String) = bundleOf("torneioId" to torneioId)
    }
}
