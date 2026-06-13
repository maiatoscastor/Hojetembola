package com.hojetembola.app.ui.auth

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
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.hojetembola.app.MainActivity
import com.hojetembola.app.R
import com.hojetembola.app.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Ecrã de autenticação — ponto de entrada depois do splash.
 *
 * Tabs:
 *   [0] Entrar   — formulário de login (email + password)
 *   [1] Registar — navega para [RegistoFragment]
 *
 * Fluxo de estado:
 *   UI → [AuthViewModel] → [AuthRepository] → Supabase
 *   AuthState: Idle / Loading / Success / Error
 *
 * Requer (compilação):
 *   - [AuthViewModel]   (Ficheiro #5)
 *   - [AuthRepository]  (Ficheiro #6)
 *   - [RegistoFragment] (Ficheiro #4) — destino da tab "Registar"
 */
@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    // Listener guardado para poder ser removido antes de selecionar a tab
    // programaticamente (evita loop de navegação no onResume)
    private val tabListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab?) {
            if (tab?.position == 1) navigateToRegisto()
        }
        override fun onTabUnselected(tab: TabLayout.Tab?) {}
        override fun onTabReselected(tab: TabLayout.Tab?) {}
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabs()
        setupButtons()
        observeAuthState()
        observeRegistoResult()
    }

    /** Garante que "Entrar" está selecionado ao regressar de RegistoFragment. */
    override fun onResume() {
        super.onResume()
        binding.tabLayout.removeOnTabSelectedListener(tabListener)
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))
        binding.tabLayout.addOnTabSelectedListener(tabListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Tabs ──────────────────────────────────────────────────────────────────

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(tabListener)
    }

    private fun navigateToRegisto() {
        findNavController().navigate(R.id.action_login_to_registo)
    }

    // ── Botões ────────────────────────────────────────────────────────────────

    private fun setupButtons() {
        binding.btnEntrar.setOnClickListener { attemptLogin() }
    }

    // ── Validação e envio ─────────────────────────────────────────────────────

    private fun attemptLogin() {
        val email    = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()

        binding.tilEmail.error    = null
        binding.tilPassword.error = null

        var valid = true

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.erro_email_invalido)
            valid = false
        }
        if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.erro_password_curta)
            valid = false
        }

        if (valid) viewModel.login(email, password)
    }

    // ── Resultado do registo ──────────────────────────────────────────────────

    /** Recebe a mensagem de sucesso passada pelo RegistoFragment quando o utilizador
     *  criou conta e foi redirecionado para aqui. */
    private fun observeRegistoResult() {
        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>("registo_sucesso")
            ?.observe(viewLifecycleOwner) { msg ->
                if (!msg.isNullOrEmpty()) {
                    Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                    findNavController().currentBackStackEntry
                        ?.savedStateHandle
                        ?.remove<String>("registo_sucesso")
                }
            }
    }

    // ── Estado de autenticação ────────────────────────────────────────────────

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collect { state -> handleAuthState(state) }
            }
        }
    }

    private fun handleAuthState(state: AuthState) {
        when (state) {
            is AuthState.Loading   -> setLoadingState(true)
            is AuthState.Success   -> goToMain()
            is AuthState.EmailSent -> {
                setLoadingState(false)
                viewModel.resetState()
                Snackbar.make(binding.root, getString(R.string.sucesso_email_reset), Snackbar.LENGTH_LONG).show()
            }
            is AuthState.Error     -> {
                setLoadingState(false)
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
            is AuthState.Idle      -> setLoadingState(false)
        }
    }

    private fun setLoadingState(loading: Boolean) {
        binding.btnEntrar.isEnabled = !loading
    }

    private fun goToMain() {
        viewModel.resetState()   // evita re-trigger se o fragmento for retomado
        startActivity(
            Intent(requireActivity(), MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        requireActivity().finish()
    }
}
