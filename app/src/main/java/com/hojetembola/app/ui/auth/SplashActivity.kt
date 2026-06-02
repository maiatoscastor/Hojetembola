package com.hojetembola.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.hojetembola.app.MainActivity
import com.hojetembola.app.R
import com.hojetembola.app.data.remote.SupabaseClient
import com.hojetembola.app.databinding.ActivitySplashBinding
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ecrã inicial da app.
 *
 * Exibe o logo + barra de loading durante [SPLASH_DURATION_MS] ms.
 * Em paralelo, verifica se existe uma sessão ativa no Supabase Auth.
 *
 * Decisão de navegação:
 *  - Sessão ativa          → [MainActivity] (com stack limpa)
 *  - Sem sessão + 1ª vez  → [SliderFragment] (e marca como não primeira vez)
 *  - Sem sessão + N-ª vez → [LoginFragment] (destino padrão do nav_auth.xml)
 *
 * O layout usa duas camadas sobrepostas:
 *  1. [FragmentContainerView] (fundo) — carrega o fluxo de auth silenciosamente
 *  2. [splashOverlay] (frente)        — desaparece com fade quando a decisão é tomada
 */
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    companion object {
        /** Duração mínima do ecrã de splash em milissegundos. */
        private const val SPLASH_DURATION_MS = 2_000L

        /** Nome do ficheiro de SharedPreferences. */
        private const val PREFS_NAME = "htb_prefs"

        /** Chave que indica se é a primeira abertura da app. */
        private const val KEY_FIRST_TIME = "first_time"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Não rearrancar a coroutine se a Activity for recriada por rotação
        if (savedInstanceState == null) {
            startSplashFlow()
        }
    }

    // ── Fluxo principal ──────────────────────────────────────────────────────

    private fun startSplashFlow() {
        lifecycleScope.launch {
            // Verificação de sessão corre em paralelo com a duração mínima do splash
            val sessionDeferred = async { hasActiveSession() }
            delay(SPLASH_DURATION_MS)
            val sessionActive = sessionDeferred.await()

            when {
                sessionActive -> goToMain()
                isFirstTime() -> {
                    markAsNotFirstTime()
                    goToSlider()
                    hideSplashOverlay()
                }
                else -> hideSplashOverlay() // loginFragment já carregado no NavHost
            }
        }
    }

    // ── Supabase Auth ────────────────────────────────────────────────────────

    private suspend fun hasActiveSession(): Boolean = try {
        SupabaseClient.client.auth.currentSessionOrNull() != null
    } catch (e: Exception) {
        false
    }

    // ── SharedPreferences ────────────────────────────────────────────────────

    private fun isFirstTime(): Boolean =
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getBoolean(KEY_FIRST_TIME, true)

    private fun markAsNotFirstTime() =
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FIRST_TIME, false)
            .apply()

    // ── Navegação ────────────────────────────────────────────────────────────

    private fun goToMain() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    private fun goToSlider() {
        val navHost = supportFragmentManager
            .findFragmentById(R.id.auth_nav_host) as NavHostFragment
        navHost.navController.navigate(R.id.sliderFragment)
    }

    // ── Animação ─────────────────────────────────────────────────────────────

    private fun hideSplashOverlay() {
        binding.splashOverlay.animate()
            .alpha(0f)
            .setDuration(400L)
            .withEndAction { binding.splashOverlay.visibility = View.GONE }
            .start()
    }
}
