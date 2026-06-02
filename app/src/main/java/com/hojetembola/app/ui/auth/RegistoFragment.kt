package com.hojetembola.app.ui.auth

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.hojetembola.app.R
import com.hojetembola.app.databinding.FragmentRegistoBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegistoFragment : Fragment() {

    private var _binding: FragmentRegistoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    private var selectedPerfil = "utilizador"

    private val tabListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab?) {
            if (tab?.position == 0) navigateToLogin()
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
        _binding = FragmentRegistoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabs()
        setupPerfilCards()
        setupButtons()
        observeAuthState()
    }

    override fun onResume() {
        super.onResume()
        binding.tabLayout.removeOnTabSelectedListener(tabListener)
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(1))
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

    private fun navigateToLogin() {
        findNavController().navigate(R.id.action_registo_to_login)
    }

    // ── Cards de perfil ───────────────────────────────────────────────────────

    private fun setupPerfilCards() {
        selectPerfil("utilizador")
        binding.cardUtilizador.setOnClickListener { selectPerfil("utilizador") }
        binding.cardOrganizador.setOnClickListener { selectPerfil("organizador") }
    }

    private fun selectPerfil(perfil: String) {
        selectedPerfil = perfil
        val isUtilizador = perfil == "utilizador"
        applyCardState(binding.cardUtilizador, binding.imgUtilizador, isUtilizador)
        applyCardState(binding.cardOrganizador, binding.imgOrganizador, !isUtilizador)
    }

    private fun applyCardState(card: MaterialCardView, icon: android.widget.ImageView, selected: Boolean) {
        val color = ContextCompat.getColor(
            requireContext(),
            if (selected) R.color.orange else R.color.bg_border
        )
        val strokePx = resources.getDimensionPixelSize(
            if (selected) R.dimen.border_width_selected else R.dimen.border_width
        )
        card.setStrokeColor(color)
        card.strokeWidth = strokePx
        ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(color))
    }

    // ── Botões ────────────────────────────────────────────────────────────────

    private fun setupButtons() {
        binding.btnCriarConta.setOnClickListener { attemptRegisto() }
    }

    // ── Validação e envio ─────────────────────────────────────────────────────

    private fun attemptRegisto() {
        val nome    = binding.etNome.text?.toString()?.trim().orEmpty()
        val email   = binding.etEmail.text?.toString()?.trim().orEmpty()
        val pass    = binding.etPassword.text?.toString().orEmpty()
        val confirm = binding.etConfirmarPassword.text?.toString().orEmpty()

        binding.tilNome.error              = null
        binding.tilEmail.error             = null
        binding.tilPassword.error          = null
        binding.tilConfirmarPassword.error = null

        var valid = true

        if (nome.isEmpty()) {
            binding.tilNome.error = getString(R.string.erro_nome_obrigatorio)
            valid = false
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.erro_email_invalido)
            valid = false
        }
        if (pass.length < 6) {
            binding.tilPassword.error = getString(R.string.erro_password_curta)
            valid = false
        }
        if (pass != confirm) {
            binding.tilConfirmarPassword.error = getString(R.string.erro_passwords_diferentes)
            valid = false
        }

        if (valid) viewModel.register(email, pass, nome, selectedPerfil)
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
            is AuthState.Success   -> onRegistoSuccess()
            is AuthState.EmailSent -> setLoadingState(false)
            is AuthState.Error     -> {
                setLoadingState(false)
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
            is AuthState.Idle      -> setLoadingState(false)
        }
    }

    private fun onRegistoSuccess() {
        setLoadingState(false)
        Snackbar.make(binding.root, getString(R.string.sucesso_registo), Snackbar.LENGTH_LONG)
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    viewModel.resetState()
                    navigateToLogin()
                }
            })
            .show()
    }

    private fun setLoadingState(loading: Boolean) {
        binding.btnCriarConta.isEnabled = !loading
    }
}
