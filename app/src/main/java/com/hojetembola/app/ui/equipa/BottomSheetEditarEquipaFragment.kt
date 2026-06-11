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
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.hojetembola.app.R
import com.hojetembola.app.databinding.DialogEditarEquipaBinding
import com.hojetembola.app.utils.collectFlow
import com.hojetembola.app.utils.gone
import com.hojetembola.app.utils.hideKeyboard
import com.hojetembola.app.utils.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BottomSheetEditarEquipaFragment : BottomSheetDialogFragment() {

    private var _binding: DialogEditarEquipaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GerirEquipaViewModel by viewModels(ownerProducer = { requireParentFragment() })

    private val coresPredefinidas = listOf(
        "#E84C3D", "#E67E22", "#F1C40F", "#2ECC71",
        "#1ABC9C", "#3498DB", "#9B59B6", "#8A9BB8"
    )
    private var corSelecionada: String = "#E84C3D"

    private val iniciaisWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) { updateAvatarPreview() }
    }

    override fun getTheme() = R.style.Theme_HojeTemBola_BottomSheet

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogEditarEquipaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-fill from current equipa info
        val equipa = viewModel.equipaInfo.value
        if (equipa != null) {
            corSelecionada = equipa.corAvatar
            binding.etNome.setText(equipa.nome)
            binding.etIniciais.setText(equipa.iniciais)
            binding.etCidade.setText(equipa.cidade ?: "")
        }

        binding.etIniciais.addTextChangedListener(iniciaisWatcher)
        renderColorPicker()
        updateAvatarPreview()

        binding.btnFechar.setOnClickListener { dismiss() }

        binding.btnGuardar.setOnClickListener {
            val nome = binding.etNome.text?.toString()?.trim().orEmpty()
            val iniciais = binding.etIniciais.text?.toString()?.trim().orEmpty()
            if (nome.isEmpty()) {
                binding.tilNome.error = getString(R.string.erro_nome_vazio)
                return@setOnClickListener
            }
            if (iniciais.isEmpty()) {
                binding.tilIniciais.error = getString(R.string.erro_campos_obrigatorios)
                return@setOnClickListener
            }
            binding.tilNome.error = null
            binding.tilIniciais.error = null
            hideKeyboard()
            val cidade = binding.etCidade.text?.toString()?.trim()
            viewModel.updateEquipa(nome, iniciais, corSelecionada, cidade)
        }

        collectFlow(viewModel.editarState) { state ->
            when (state) {
                is EditarEquipaState.Loading -> {
                    binding.progressBar.visible()
                    binding.btnGuardar.isEnabled = false
                }
                is EditarEquipaState.Success -> {
                    binding.progressBar.gone()
                    Snackbar.make(
                        requireParentFragment().requireView(),
                        getString(R.string.equipa_atualizada),
                        Snackbar.LENGTH_SHORT
                    ).show()
                    viewModel.resetEditarState()
                    dismiss()
                }
                is EditarEquipaState.Error -> {
                    binding.progressBar.gone()
                    binding.btnGuardar.isEnabled = true
                    binding.tilNome.error = state.message
                }
                EditarEquipaState.Idle -> {
                    binding.progressBar.gone()
                    binding.btnGuardar.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateAvatarPreview() {
        val iniciais = binding.etIniciais.text?.toString()?.trim()?.ifEmpty { "?" } ?: "?"
        val color = parseColorSafe(corSelecionada)
        binding.vAvatarBg.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
        binding.tvAvatarIniciais.text = iniciais
        binding.tvAvatarIniciais.bringToFront()
    }

    private fun renderColorPicker() {
        if (binding.llColorPicker.tag == corSelecionada) return
        binding.llColorPicker.tag = corSelecionada
        binding.llColorPicker.removeAllViews()

        val density = resources.displayMetrics.density
        val sizePx = (40 * density).toInt()
        val gapPx = (10 * density).toInt()

        coresPredefinidas.forEach { cor ->
            val isSelected = cor == corSelecionada
            val circle = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).also { it.marginEnd = gapPx }
                background = buildCircleDrawable(cor, isSelected, density)
                setOnClickListener {
                    corSelecionada = cor
                    renderColorPicker()
                    updateAvatarPreview()
                }
            }
            binding.llColorPicker.addView(circle)
        }
    }

    private fun buildCircleDrawable(hex: String, selected: Boolean, density: Float): Drawable {
        val color = parseColorSafe(hex)
        val borderPx = (3 * density).toInt()
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

    companion object {
        fun newInstance() = BottomSheetEditarEquipaFragment()
    }
}
