package com.hojetembola.app.ui.perfil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hojetembola.app.R
import com.hojetembola.app.databinding.DialogAlterarPasswordBinding
import com.hojetembola.app.utils.collectFlow
import com.hojetembola.app.utils.gone
import com.hojetembola.app.utils.hideKeyboard
import com.hojetembola.app.utils.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BottomSheetAlterarPasswordFragment : BottomSheetDialogFragment() {

    private var _binding: DialogAlterarPasswordBinding? = null
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
        _binding = DialogAlterarPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnFechar.setOnClickListener { dismiss() }

        binding.btnGuardar.setOnClickListener {
            val nova = binding.etNovaPassword.text?.toString().orEmpty()
            val confirmar = binding.etConfirmarPassword.text?.toString().orEmpty()

            binding.tilNovaPassword.error = null
            binding.tilConfirmarPassword.error = null

            if (nova.length < 6) {
                binding.tilNovaPassword.error = getString(R.string.erro_password_curta)
                return@setOnClickListener
            }
            if (nova != confirmar) {
                binding.tilConfirmarPassword.error = getString(R.string.erro_passwords_nao_coincidem)
                return@setOnClickListener
            }

            hideKeyboard()
            viewModel.changePassword(nova)
        }

        collectFlow(viewModel.passState) { state ->
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
                    binding.tilNovaPassword.error = state.message
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
        viewModel.resetPassState()
        _binding = null
    }

    companion object {
        fun newInstance() = BottomSheetAlterarPasswordFragment()
    }
}
