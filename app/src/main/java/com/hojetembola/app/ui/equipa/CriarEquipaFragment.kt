package com.hojetembola.app.ui.equipa

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.hojetembola.app.R
import com.hojetembola.app.databinding.FragmentCriarEquipaBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CriarEquipaFragment : Fragment() {

    private var _binding: FragmentCriarEquipaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CriarEquipaViewModel by viewModels()

    /** 8 predefined team colors */
    private val coresPredefinidas = listOf(
        "#E84C3D", "#E67E22", "#F1C40F", "#2ECC71",
        "#1ABC9C", "#3498DB", "#9B59B6", "#8A9BB8"
    )

    // Text watchers kept as references so they can be removed if needed
    private val nomeWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) { viewModel.setNome(s.toString()) }
    }
    private val iniciaisWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) { viewModel.setIniciais(s.toString()) }
    }
    private val cidadeWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) { viewModel.setCidade(s.toString()) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCriarEquipaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnCriar.setOnClickListener { viewModel.criarEquipa() }

        binding.etNome.addTextChangedListener(nomeWatcher)
        binding.etIniciais.addTextChangedListener(iniciaisWatcher)
        binding.etCidade.addTextChangedListener(cidadeWatcher)

        observeState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Observation ───────────────────────────────────────────────────────────

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Form state → update avatar preview + color picker + iniciais field
                launch {
                    viewModel.form.collect { form ->
                        renderColorPicker(coresPredefinidas, form.corAvatar)
                        updateAvatarPreview(form.corAvatar, form.iniciais)

                        // Auto-sync iniciais field only when the user is not editing it
                        val currentText = binding.etIniciais.text.toString()
                        if (!binding.etIniciais.isFocused && currentText != form.iniciais) {
                            binding.etIniciais.removeTextChangedListener(iniciaisWatcher)
                            binding.etIniciais.setText(form.iniciais)
                            binding.etIniciais.setSelection(form.iniciais.length)
                            binding.etIniciais.addTextChangedListener(iniciaisWatcher)
                        }
                    }
                }

                // UI state → show loading / success / error
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is CriarEquipaUiState.Idle    -> setLoading(false)
                            is CriarEquipaUiState.Loading -> setLoading(true)
                            is CriarEquipaUiState.Success -> {
                                setLoading(false)
                                Snackbar.make(binding.root, R.string.equipa_criada, Snackbar.LENGTH_SHORT).show()
                                val torneioId = arguments?.getString("torneioId")
                                if (torneioId != null) {
                                    // Navegar para gerir jogadores para completar a inscrição
                                    findNavController().navigate(
                                        R.id.action_criarEquipaFragment_to_gerirEquipaFragment,
                                        bundleOf(
                                            "equipaId"    to state.equipa.id,
                                            "equipaNome"  to state.equipa.nome,
                                            "torneioId"   to torneioId,
                                            "minJogadores" to (arguments?.getInt("minJogadores") ?: 0),
                                            "maxJogadores" to (arguments?.getInt("maxJogadores") ?: 99)
                                        )
                                    )
                                } else {
                                    findNavController().navigateUp()
                                }
                            }
                            is CriarEquipaUiState.Error   -> {
                                setLoading(false)
                                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                                viewModel.resetState()
                            }
                        }
                    }
                }
            }
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private fun setLoading(loading: Boolean) {
        binding.progressBar.isVisible = loading
        binding.btnCriar.isEnabled    = !loading
    }

    private fun updateAvatarPreview(corAvatar: String, iniciais: String) {
        val color = parseColorSafe(corAvatar)
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
        binding.vAvatarBg.background       = drawable
        binding.tvAvatarIniciais.text       = iniciais.ifEmpty { "?" }
    }

    /**
     * Renders the color picker row. Rebuilds only when the selected colour changes
     * (tag used as a cheap diff).
     */
    private fun renderColorPicker(cores: List<String>, selected: String) {
        if (binding.llColorPicker.tag == selected) return
        binding.llColorPicker.tag = selected
        binding.llColorPicker.removeAllViews()

        val density = resources.displayMetrics.density
        val sizePx  = (40 * density).toInt()
        val gapPx   = (10 * density).toInt()

        cores.forEach { cor ->
            val isSelected = cor == selected
            val circle = View(requireContext()).apply {
                val params = LinearLayout.LayoutParams(sizePx, sizePx).also { it.marginEnd = gapPx }
                layoutParams  = params
                background     = buildCircleDrawable(cor, isSelected, density)
                contentDescription = cor
                setOnClickListener { viewModel.setCorAvatar(cor) }
            }
            binding.llColorPicker.addView(circle)
        }
    }

    private fun buildCircleDrawable(hex: String, selected: Boolean, density: Float): Drawable {
        val color     = parseColorSafe(hex)
        val borderPx  = (3 * density).toInt()
        return if (selected) {
            val outer = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.WHITE) }
            val inner = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }
            LayerDrawable(arrayOf(outer, inner)).apply {
                setLayerInset(1, borderPx, borderPx, borderPx, borderPx)
            }
        } else {
            GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }
        }
    }

    private fun parseColorSafe(hex: String): Int =
        try { Color.parseColor(hex) } catch (_: Exception) { Color.parseColor("#E84C3D") }
}
