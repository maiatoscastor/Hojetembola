package com.hojetembola.app

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.hojetembola.app.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Actividade principal — iniciada pelo [ui.auth.SplashActivity] após autenticação.
 *
 * Responsabilidades:
 *   - Edge-to-edge: status bar transparente + bottom nav acima da gesture bar
 *   - Ligar o [NavController] do grafo nav_main às tabs do bottom nav
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets()
        setupNavigation()
    }

    /**
     * Aplica o inset da barra de sistema (gesture bar / navigation bar) como
     * padding inferior do BottomNavigationView, para que os itens fiquem sempre
     * acima da área de gestos e não fiquem cortados.
     */
    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                insets.bottom
            )
            windowInsets
        }
    }

    private fun setupNavigation() {
        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_main) as NavHostFragment
        binding.bottomNav.setupWithNavController(navHost.navController)
    }
}
