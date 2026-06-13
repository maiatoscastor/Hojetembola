package com.hojetembola.app.ui.perfil

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.hojetembola.app.R
import com.hojetembola.app.databinding.DialogDefinicoesBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BottomSheetDefinicoesFragment : BottomSheetDialogFragment() {

    private var _binding: DialogDefinicoesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PerfilViewModel by lazy {
        (requireParentFragment() as PerfilFragment).perfilViewModel
    }

    private var isInitializing = true

    override fun getTheme() = R.style.Theme_HojeTemBola_BottomSheet

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDefinicoesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Versão da app lida dinamicamente
        binding.tvVersao.text = runCatching {
            requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0).versionName
        }.getOrDefault("1.0.0")

        // Toggle de notificações persiste localmente
        val prefs = requireContext().getSharedPreferences("htb_prefs", Context.MODE_PRIVATE)
        binding.switchNotificacoes.isChecked = prefs.getBoolean("notif_enabled", true)
        binding.switchNotificacoes.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("notif_enabled", checked).apply()
        }

        // Seletor de idioma
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val currentTag = currentLocales.toLanguageTags()
        val isEnglish = currentTag.startsWith("en")
        binding.toggleIdioma.check(
            if (isEnglish) R.id.btnIdiomaIngles else R.id.btnIdiomaPortugues
        )
        var suppressLocale = true
        binding.toggleIdioma.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || suppressLocale) return@addOnButtonCheckedListener
            val tag = if (checkedId == R.id.btnIdiomaIngles) "en" else "pt-PT"
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
        suppressLocale = false

        // Inicializa toggle com perfil atual
        val perfilAtual = (viewModel.uiState.value as? PerfilUiState.Success)
            ?.utilizador?.perfil ?: "utilizador"
        val initialId = if (perfilAtual == "organizador") R.id.btnPerfilOrganizador
                        else R.id.btnPerfilUtilizador
        binding.togglePerfil.check(initialId)
        isInitializing = false

        binding.togglePerfil.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || isInitializing) return@addOnButtonCheckedListener

            val novoPerfil = if (checkedId == R.id.btnPerfilOrganizador) "organizador" else "utilizador"
            val atual = (viewModel.uiState.value as? PerfilUiState.Success)
                ?.utilizador?.perfil ?: "utilizador"

            if (novoPerfil == atual) return@addOnButtonCheckedListener

            // Reverte visualmente até confirmação
            isInitializing = true
            binding.togglePerfil.check(if (atual == "organizador") R.id.btnPerfilOrganizador else R.id.btnPerfilUtilizador)
            isInitializing = false

            showConfirmacaoTrocaPerfil(novoPerfil) {
                val nome = (viewModel.uiState.value as? PerfilUiState.Success)
                    ?.utilizador?.nome ?: ""
                viewModel.updateProfile(nome, novoPerfil)
                dismiss()
            }
        }

        // Terminar sessão via ViewModel do pai
        binding.btnTerminarSessao.setOnClickListener {
            dismiss()
            viewModel.signOut()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showConfirmacaoTrocaPerfil(novoPerfil: String, onConfirmed: () -> Unit) {
        val ctx = requireContext()
        val density = resources.displayMetrics.density
        val padH = (20 * density).toInt()
        val padV = (8 * density).toInt()

        val dialogCtx = android.view.ContextThemeWrapper(ctx, R.style.Theme_HojeTemBola_Dialog)
        val inputLayout = TextInputLayout(dialogCtx, null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            hint = novoPerfil
            setHintTextColor(android.content.res.ColorStateList.valueOf(
                resources.getColor(R.color.text_secondary, null)))
            boxStrokeColor = resources.getColor(R.color.orange, null)
        }
        val input = TextInputEditText(inputLayout.context).apply {
            setTextColor(resources.getColor(R.color.text_primary, null))
        }
        inputLayout.addView(input)

        val container = FrameLayout(ctx).apply {
            setPadding(padH, padV, padH, 0)
            addView(inputLayout)
        }

        val dialog = MaterialAlertDialogBuilder(ctx, R.style.Theme_HojeTemBola_Dialog)
            .setTitle(getString(R.string.confirmar_troca_titulo))
            .setMessage(getString(R.string.confirmar_troca_msg, novoPerfil))
            .setView(container)
            .setPositiveButton(getString(R.string.confirmar), null)
            .setNegativeButton(getString(R.string.cancelar)) { d, _ -> d.dismiss() }
            .create()

        dialog.show()

        // Força o fundo escuro com cantos arredondados (M3 usa colorSurfaceContainerHigh que
        // pode não ser herdado corretamente pelo ThemeOverlay — forçamos via window background)
        val bgColor = resources.getColor(R.color.bg_surface, null)
        val cornerPx = 28f * resources.displayMetrics.density
        dialog.window?.setBackgroundDrawable(GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = cornerPx
        })

        // Botão confirmar só ativo quando o texto bate certo
        val btnConfirmar = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
        btnConfirmar.isEnabled = false
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                btnConfirmar.isEnabled = s?.toString()?.trim().equals(novoPerfil, ignoreCase = true)
            }
        })

        btnConfirmar.setOnClickListener {
            dialog.dismiss()
            onConfirmed()
        }
    }

    companion object {
        fun newInstance() = BottomSheetDefinicoesFragment()
    }
}
