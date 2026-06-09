package com.hojetembola.app.ui.perfil

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.core.view.isVisible
import androidx.lifecycle.repeatOnLifecycle
import com.hojetembola.app.R
import com.hojetembola.app.data.local.entity.EquipaEntity
import com.hojetembola.app.data.local.entity.UtilizadorEntity
import com.hojetembola.app.databinding.FragmentPerfilBinding
import com.hojetembola.app.ui.auth.SplashActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PerfilViewModel by viewModels()


    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDropdowns()
        setupListeners()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Dropdowns ─────────────────────────────────────────────────────────────

    private fun setupDropdowns() {
        val periodos = listOf(
            getString(R.string.periodo_esta_semana),
            getString(R.string.periodo_este_mes),
            getString(R.string.periodo_epoca_atual),
            getString(R.string.periodo_desde_sempre)
        )
        val escopos = listOf(getString(R.string.escopo_geral))

        binding.actvPeriodo.setAdapter(
            ArrayAdapter(requireContext(), R.layout.item_dropdown, periodos)
        )
        binding.actvEscopo.setAdapter(
            ArrayAdapter(requireContext(), R.layout.item_dropdown, escopos)
        )

        binding.actvPeriodo.setText(getString(R.string.periodo_este_mes), false)
        binding.actvEscopo.setText(getString(R.string.escopo_geral), false)
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private fun setupListeners() {
        binding.btnTerminarSessao.setOnClickListener { viewModel.signOut() }
        binding.btnTentarNovamente.setOnClickListener { viewModel.loadProfile() }
        binding.btnEditarPerfil.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.em_breve), Toast.LENGTH_SHORT).show()
        }
        binding.btnDefinicoes.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.em_breve), Toast.LENGTH_SHORT).show()
        }
    }

    // ── Observadores ──────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is PerfilUiState.Loading -> showLoading()
                            is PerfilUiState.Success -> showContent(state.utilizador)
                            is PerfilUiState.Error   -> showError(state.message)
                        }
                    }
                }
                launch {
                    viewModel.equipas.collect { equipas -> renderEquipas(equipas) }
                }
                launch {
                    viewModel.signOutEvent.collect { navigateToAuth() }
                }
            }
        }
    }

    // ── Estados visuais ───────────────────────────────────────────────────────

    private fun showLoading() {
        binding.progressBar.visibility   = View.VISIBLE
        binding.scrollContent.visibility = View.GONE
        binding.layoutErro.visibility    = View.GONE
    }

    private fun showContent(u: UtilizadorEntity) {
        binding.progressBar.visibility   = View.GONE
        binding.layoutErro.visibility    = View.GONE
        binding.scrollContent.visibility = View.VISIBLE

        binding.tvAvatarInicial.text = buildInitials(u.nome)
        binding.tvNome.text          = u.nome
        binding.tvPerfilSub.text     = perfilLabel(u.perfil)
        binding.tvTagPerfil.text     = perfilLabel(u.perfil)

        // Estatísticas a 0 — dados reais virão quando os jogos forem implementados
        binding.tvKpiGolos.text       = "0"
        binding.tvKpiGolosMeta.text   = "—"
        binding.tvKpiJogos.text       = "0"
        binding.tvKpiJogosMeta.text   = "—"
        binding.tvKpiMvps.text        = "0"
        binding.tvKpiMvpsMeta.text    = "—"
        binding.tvAssistencias.text   = "0"
        binding.tvCartoesAmarelos.text = "0"
        binding.tvMinutosJogados.text = "0"
        binding.tvMelhorPeriodo.text  = "—"

        // Teams rendered separately via renderEquipas() observer
    }

    private fun showError(message: String) {
        binding.progressBar.visibility   = View.GONE
        binding.scrollContent.visibility = View.GONE
        binding.layoutErro.visibility    = View.VISIBLE
        binding.tvMensagemErro.text = message
    }

    // ── Equipas ───────────────────────────────────────────────────────────────

    private fun renderEquipas(equipas: List<EquipaEntity>) {
        if (equipas.isEmpty()) {
            binding.tvSemEquipas.visibility     = View.VISIBLE
            binding.containerEquipas.visibility = View.GONE
            return
        }

        binding.tvSemEquipas.visibility     = View.GONE
        binding.containerEquipas.visibility = View.VISIBLE
        binding.containerEquipas.removeAllViews()

        val density = resources.displayMetrics.density

        equipas.forEach { equipa ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity     = android.view.Gravity.CENTER_VERTICAL
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (10 * density).toInt() }
                layoutParams = lp
            }

            // Círculo colorido com iniciais
            val avatarSize = (40 * density).toInt()
            val avatar = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(avatarSize, avatarSize).apply {
                    marginEnd = (12 * density).toInt()
                }
                gravity = android.view.Gravity.CENTER
                text = equipa.iniciais.ifEmpty { equipa.nome.take(2).uppercase() }
                setTextColor(Color.WHITE)
                textSize = 13f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(parseColorSafe(equipa.corAvatar))
                }
            }

            // Nome + cidade
            val textos = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvNome = TextView(requireContext()).apply {
                text = equipa.nome
                setTextColor(resources.getColor(R.color.text_primary, null))
                textSize = 13f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            val tvSub = TextView(requireContext()).apply {
                text = equipa.cidade ?: ""
                setTextColor(resources.getColor(R.color.text_muted, null))
                textSize = 11f
                isVisible = !equipa.cidade.isNullOrBlank()
            }
            textos.addView(tvNome)
            textos.addView(tvSub)

            row.addView(avatar)
            row.addView(textos)
            binding.containerEquipas.addView(row)
        }
    }

    private fun parseColorSafe(hex: String): Int =
        try { Color.parseColor(hex) } catch (_: Exception) { Color.parseColor("#E84C3D") }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildInitials(nome: String): String =
        nome.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { "?" }

    private fun perfilLabel(perfil: String) = when (perfil) {
        "organizador" -> "Organizador"
        else          -> "Utilizador"
    }

    // ── Navegação ─────────────────────────────────────────────────────────────

    private fun navigateToAuth() {
        startActivity(Intent(requireContext(), SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        requireActivity().finish()
    }
}
