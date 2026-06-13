package com.hojetembola.app.ui.torneios

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hojetembola.app.R
import com.hojetembola.app.databinding.FragmentVotarMvpBottomSheetBinding
import com.hojetembola.app.databinding.ItemMvpCandidatoBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VotarMvpBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentVotarMvpBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val jogoViewModel: JogoDetalheViewModel by lazy {
        ViewModelProvider(requireParentFragment())[JogoDetalheViewModel::class.java]
    }

    private val viewModel: VotarMvpViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.setBackgroundResource(R.color.bg_primary)
            val behavior = BottomSheetBehavior.from(sheet!!)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVotarMvpBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnFechar.setOnClickListener { dismiss() }

        // Initialise tabs
        binding.tabGroupMvp.check(R.id.btnTabJogadores)
        updateTabColors(R.id.btnTabJogadores)

        binding.tabGroupMvp.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            updateTabColors(checkedId)
            binding.sectionJogadores.isVisible   = checkedId == R.id.btnTabJogadores
            binding.sectionPublico.isVisible     = checkedId == R.id.btnTabPublico
            binding.sectionOrganizador.isVisible = checkedId == R.id.btnTabOrganizador
        }

        // Pass game context to ViewModel
        val parentState = jogoViewModel.uiState.value
        binding.tvHeaderPartida.text = "${parentState.casaNome} vs ${parentState.visitanteNome}"

        viewModel.init(
            casaNome           = parentState.casaNome,
            visitanteNome      = parentState.visitanteNome,
            isOrganizador      = jogoViewModel.isOrganizador,
            votacaoAtiva       = parentState.votacaoMvpAtiva,
            casaJogadores      = parentState.casaJogadores,
            visitanteJogadores = parentState.visitanteJogadores
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (!state.loading) render(state)
                }
            }
        }
    }

    private fun render(state: VotarMvpUiState) {
        binding.tvCasaJogadores.text   = state.casaNome
        binding.tvVisitanteJogadores.text = state.visitanteNome
        binding.tvCasaPublico.text     = state.casaNome
        binding.tvVisitantePublico.text = state.visitanteNome
        binding.tvCasaOrganizador.text = state.casaNome
        binding.tvVisitanteOrganizador.text = state.visitanteNome

        val podeVotarJogador     = state.votacaoAtiva && state.tipoVotante == "jogador"
        val podeVotarPublico     = state.votacaoAtiva && state.tipoVotante == "publico"
        val podeVotarOrganizador = state.votacaoAtiva && state.tipoVotante == "organizador"

        // --- Jogadores tab ---
        renderCandidatos(
            container  = binding.layoutCasaJogadores,
            candidatos = state.casaJogadores,
            votosMap   = state.votosJogadores,
            meuVotoId  = state.meuVotoJogadorId,
            tipoVotante = "jogador",
            podeVotar  = podeVotarJogador
        )
        renderCandidatos(
            container  = binding.layoutVisitanteJogadores,
            candidatos = state.visitanteJogadores,
            votosMap   = state.votosJogadores,
            meuVotoId  = state.meuVotoJogadorId,
            tipoVotante = "jogador",
            podeVotar  = podeVotarJogador
        )

        // --- Público tab ---
        renderCandidatos(
            container  = binding.layoutCasaPublico,
            candidatos = state.casaJogadores,
            votosMap   = state.votosPublico,
            meuVotoId  = state.meuVotoPublicoId,
            tipoVotante = "publico",
            podeVotar  = podeVotarPublico
        )
        renderCandidatos(
            container  = binding.layoutVisitantePublico,
            candidatos = state.visitanteJogadores,
            votosMap   = state.votosPublico,
            meuVotoId  = state.meuVotoPublicoId,
            tipoVotante = "publico",
            podeVotar  = podeVotarPublico
        )

        // --- Organizador tab ---
        val orgVotadoId = state.votosOrganizador.maxByOrNull { it.value }?.key
        binding.tvInfoOrganizador.text = when {
            orgVotadoId != null -> {
                val nome = (state.casaJogadores + state.visitanteJogadores)
                    .find { it.utilizadorId == orgVotadoId }?.nome ?: "—"
                getString(R.string.mvp_org_escolheu, nome)
            }
            else -> getString(R.string.mvp_org_info)
        }
        renderCandidatos(
            container  = binding.layoutCasaOrganizador,
            candidatos = state.casaJogadores,
            votosMap   = state.votosOrganizador,
            meuVotoId  = state.meuVotoOrgId,
            tipoVotante = "organizador",
            podeVotar  = podeVotarOrganizador
        )
        renderCandidatos(
            container  = binding.layoutVisitanteOrganizador,
            candidatos = state.visitanteJogadores,
            votosMap   = state.votosOrganizador,
            meuVotoId  = state.meuVotoOrgId,
            tipoVotante = "organizador",
            podeVotar  = podeVotarOrganizador
        )
    }

    private fun renderCandidatos(
        container: LinearLayout,
        candidatos: List<MvpCandidato>,
        votosMap: Map<String, Int>,
        meuVotoId: String?,
        tipoVotante: String,
        podeVotar: Boolean
    ) {
        container.removeAllViews()
        val totalVotos = votosMap.values.sum()
        candidatos.forEach { c ->
            val itemBinding = ItemMvpCandidatoBinding.inflate(layoutInflater, container, false)
            val votos = votosMap[c.utilizadorId] ?: 0
            val pct   = if (totalVotos > 0) votos * 100 / totalVotos else 0
            val jaVotouEste = meuVotoId == c.utilizadorId
            val jaVotouOutro = meuVotoId != null && !jaVotouEste

            // Avatar
            val avatarBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(try { Color.parseColor(c.avatarColor) } catch (_: Exception) { Color.parseColor("#3D5A80") })
            }
            itemBinding.viewAvatarBg.background = avatarBg
            itemBinding.tvIniciais.text = c.iniciais

            // Name + meta
            itemBinding.tvNomeCandidato.text = c.nome
            val meta = buildString {
                if (c.golosNoJogo > 0) append("${c.golosNoJogo}G ")
                if (c.assistenciasNoJogo > 0) append("${c.assistenciasNoJogo}A ")
                if (votos > 0) append("· $votos ${if (votos == 1) "voto" else "votos"}")
            }.trim()
            itemBinding.tvMetaCandidato.text = meta.ifBlank { "—" }

            // Progress
            itemBinding.tvPctVotos.text = "$pct%"
            itemBinding.progressVotos.progress = pct

            // Buttons
            when {
                !podeVotar -> {
                    itemBinding.btnVotar.isVisible  = false
                    itemBinding.btnVotado.isVisible = jaVotouEste
                    if (jaVotouEste) itemBinding.btnVotado.text = getString(R.string.votado)
                }
                jaVotouEste -> {
                    itemBinding.btnVotar.isVisible  = false
                    itemBinding.btnVotado.isVisible = true
                    itemBinding.btnVotado.text      = getString(R.string.votado)
                    itemBinding.btnVotado.setOnClickListener {
                        viewModel.votar(c.utilizadorId, tipoVotante)
                    }
                }
                else -> {
                    itemBinding.btnVotar.isVisible  = true
                    itemBinding.btnVotado.isVisible = false
                    itemBinding.btnVotar.setOnClickListener {
                        viewModel.votar(c.utilizadorId, tipoVotante)
                    }
                }
            }

            container.addView(itemBinding.root)
        }

        // Show empty hint if no players
        if (candidatos.isEmpty()) {
            val tv = TextView(requireContext()).apply {
                text = "—"
                setTextColor(resources.getColor(R.color.text_muted, null))
                textSize = 12f
                setPadding(0, 8, 0, 8)
            }
            container.addView(tv)
        }
    }

    private fun updateTabColors(checkedId: Int) {
        val orange = requireContext().getColor(R.color.orange)
        val transparent = Color.TRANSPARENT
        val white = Color.WHITE
        val unselected = Color.parseColor("#8A9BB8")
        val border = Color.parseColor("#1E3260")
        listOf(R.id.btnTabJogadores, R.id.btnTabPublico, R.id.btnTabOrganizador).forEach { id ->
            val btn = binding.tabGroupMvp.findViewById<com.google.android.material.button.MaterialButton>(id)
            val sel = id == checkedId
            btn.backgroundTintList = ColorStateList(arrayOf(intArrayOf()), intArrayOf(if (sel) orange else transparent))
            btn.strokeColor        = ColorStateList(arrayOf(intArrayOf()), intArrayOf(if (sel) orange else border))
            btn.setTextColor(if (sel) white else unselected)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "VotarMvpBottomSheet"
        fun newInstance(jogoId: String) = VotarMvpBottomSheet().apply {
            arguments = bundleOf("jogoId" to jogoId)
        }
    }
}
