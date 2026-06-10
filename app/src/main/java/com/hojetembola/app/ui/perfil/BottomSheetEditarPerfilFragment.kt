package com.hojetembola.app.ui.perfil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hojetembola.app.R
import com.hojetembola.app.databinding.DialogEditarPerfilBinding
import com.hojetembola.app.utils.collectFlow
import com.hojetembola.app.utils.gone
import com.hojetembola.app.utils.hideKeyboard
import com.hojetembola.app.utils.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BottomSheetEditarPerfilFragment : BottomSheetDialogFragment() {

    private var _binding: DialogEditarPerfilBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PerfilViewModel by lazy {
        (requireParentFragment() as PerfilFragment).perfilViewModel
    }

    override fun getTheme() = R.style.Theme_HojeTemBola_BottomSheet

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditarPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nome = arguments?.getString(ARG_NOME).orEmpty()
        val email = arguments?.getString(ARG_EMAIL).orEmpty()

        binding.etNome.setText(nome)
        binding.etEmail.setText(email)
        binding.tvAvatarInicial.text = buildInitials(nome)

        binding.btnFechar.setOnClickListener { dismiss() }

        binding.btnAlterarPassword.setOnClickListener {
            dismiss()
            (requireParentFragment() as PerfilFragment).showAlterarPassword()
        }

        binding.btnGuardar.setOnClickListener {
            val novoNome = binding.etNome.text?.toString()?.trim().orEmpty()
            if (novoNome.isEmpty()) {
                binding.tilNome.error = getString(R.string.erro_nome_vazio)
                return@setOnClickListener
            }
            binding.tilNome.error = null
            hideKeyboard()
            val perfilAtual = (viewModel.uiState.value as? PerfilUiState.Success)
                ?.utilizador?.perfil ?: "utilizador"
            viewModel.updateProfile(novoNome, perfilAtual)
        }

        collectFlow(viewModel.saveState) { state ->
            when (state) {
                is PerfilSaveState.Loading -> {
                    binding.progressBar.visible()
                    binding.btnGuardar.isEnabled = false
                }
                is PerfilSaveState.Success -> {
                    binding.progressBar.gone()
                    dismiss()
                }
                is PerfilSaveState.Error -> {
                    binding.progressBar.gone()
                    binding.btnGuardar.isEnabled = true
                    binding.tilNome.error = state.message
                }
                PerfilSaveState.Idle -> {
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

    private fun buildInitials(nome: String) =
        nome.split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }.ifEmpty { "?" }

    companion object {
        private const val ARG_NOME = "nome"
        private const val ARG_EMAIL = "email"

        fun newInstance(nome: String, email: String) =
            BottomSheetEditarPerfilFragment().also {
                it.arguments = Bundle().apply {
                    putString(ARG_NOME, nome)
                    putString(ARG_EMAIL, email)
                }
            }
    }
}
