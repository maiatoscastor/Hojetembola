package com.hojetembola.app.ui.perfil

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hojetembola.app.data.local.entity.UtilizadorEntity
import com.hojetembola.app.databinding.FragmentPerfilBinding
import com.hojetembola.app.ui.auth.SplashActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Ecrã Perfil — mostra os dados do utilizador autenticado.
 *
 * Observa [PerfilViewModel.uiState] para Loading / Success / Error
 * e [PerfilViewModel.signOutEvent] para navegar de volta ao ecrã de auth.
 */
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
        setupListeners()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private fun setupListeners() {
        binding.btnTerminarSessao.setOnClickListener { viewModel.signOut() }
        binding.btnTentarNovamente.setOnClickListener { viewModel.loadProfile() }
    }

    // ── Observadores ──────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Estado da UI
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is PerfilUiState.Loading -> showLoading()
                            is PerfilUiState.Success -> showContent(state.utilizador)
                            is PerfilUiState.Error   -> showError(state.message)
                        }
                    }
                }

                // Evento one-shot: terminar sessão → ir para auth
                launch {
                    viewModel.signOutEvent.collect {
                        navigateToAuth()
                    }
                }
            }
        }
    }

    // ── Estados visuais ───────────────────────────────────────────────────────

    private fun showLoading() {
        binding.progressBar.visibility  = View.VISIBLE
        binding.scrollContent.visibility = View.GONE
        binding.layoutErro.visibility   = View.GONE
    }

    private fun showContent(utilizador: UtilizadorEntity) {
        binding.progressBar.visibility  = View.GONE
        binding.layoutErro.visibility   = View.GONE
        binding.scrollContent.visibility = View.VISIBLE

        // Avatar: primeira letra do nome em maiúscula
        binding.tvAvatarInicial.text = utilizador.nome
            .firstOrNull()?.uppercaseChar()?.toString() ?: "?"

        binding.tvNome.text  = utilizador.nome
        binding.tvEmail.text = utilizador.email

        // Badge de tipo de conta
        binding.tvTipoConta.text = when (utilizador.perfil) {
            "organizador" -> "Organizador"
            else          -> "Utilizador"
        }
    }

    private fun showError(message: String) {
        binding.progressBar.visibility  = View.GONE
        binding.scrollContent.visibility = View.GONE
        binding.layoutErro.visibility   = View.VISIBLE
        binding.tvMensagemErro.text = message
    }

    // ── Navegação ─────────────────────────────────────────────────────────────

    private fun navigateToAuth() {
        val intent = Intent(requireContext(), SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }
}
