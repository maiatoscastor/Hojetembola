package com.hojetembola.app.ui.torneios

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.hojetembola.app.R
import com.hojetembola.app.databinding.DialogEntrarCodigoBinding
import com.hojetembola.app.databinding.FragmentTorneiosBinding
import com.hojetembola.app.data.local.entity.TorneioEntity
import com.hojetembola.app.data.local.entity.minJogadoresPorEquipa
import com.hojetembola.app.ui.equipa.InscricaoTorneioBottomSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TorneiosFragment : Fragment() {

    private var _binding: FragmentTorneiosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TorneiosViewModel by viewModels()
    private val codigoViewModel: EntrarCodigoViewModel by viewModels()
    private lateinit var torneioAdapter: TorneioAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTorneiosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        setupSearch()
        setupFilters()
        setupTabs()
        observeState()
        binding.btnCriar.setOnClickListener {
            findNavController().navigate(R.id.action_torneiosFragment_to_criarTorneioFragment)
        }
        binding.cardEntrarCodigo.setOnClickListener {
            mostrarDialogCodigo()
        }
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    private fun setupAdapter() {
        torneioAdapter = TorneioAdapter(
            onMeuClick     = { torneio -> navegarParaDetalhe(torneio) },
            onPublicoClick = { torneio -> navegarParaDetalhe(torneio) },
            onInscrever    = { torneio -> abrirInscricao(torneio.id, torneio.minJogadoresPorEquipa(), torneio.maxJogadoresPorEquipa) }
        )
        binding.rvTorneios.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = torneioAdapter
            setHasFixedSize(false)
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private fun setupSearch() {
        binding.tilPesquisa.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setPesquisa(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    // ── Estado filter chips ───────────────────────────────────────────────────

    private fun setupFilters() {
        binding.chipGroupFiltros.setOnCheckedStateChangeListener { _, checkedIds ->
            val filtro = when (checkedIds.firstOrNull()) {
                R.id.chipADecorar   -> "a_decorrer"
                R.id.chipInscricoes -> "inscricoes"
                R.id.chipTerminados -> "terminados"
                else                -> "todos"
            }
            viewModel.setFiltro(filtro)
        }
    }

    // ── Tab toggle ────────────────────────────────────────────────────────────

    private fun setupTabs() {
        binding.tabLayoutTorneios.addTab(
            binding.tabLayoutTorneios.newTab().setText(getString(R.string.tab_torneios))
        )
        binding.tabLayoutTorneios.addTab(
            binding.tabLayoutTorneios.newTab().setText(getString(R.string.tab_os_meus))
        )
        binding.tabLayoutTorneios.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                viewModel.setTabAtiva(if (tab?.position == 0) "todos" else "meus")
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    // ── State observation ─────────────────────────────────────────────────────

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is TorneiosUiState.Loading -> showLoading()
                        is TorneiosUiState.Error   -> showError(state.message)
                        is TorneiosUiState.Content -> showContent(state)
                    }
                }
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.isVisible = true
        binding.rvTorneios.isVisible  = false
        binding.tvEmpty.isVisible     = false
    }

    private fun showError(msg: String) {
        binding.progressBar.isVisible = false
        binding.rvTorneios.isVisible  = false
        binding.tvEmpty.isVisible     = true
        binding.tvEmpty.text          = msg
    }

    private fun showContent(state: TorneiosUiState.Content) {
        binding.progressBar.isVisible = false

        // Criar torneio: só organizadores
        binding.btnCriar.isVisible = state.utilizador.perfil.equals("Organizador", ignoreCase = true)

        // Items baseados na tab activa
        val items = when (state.tabAtiva) {
            "meus"  -> state.meusTorneios.map { TorneioListItem.Meu(it) }
            else    -> state.torneiosPublicos.map { TorneioListItem.Publico(it) }
        }

        torneioAdapter.submitList(items)
        val isEmpty = items.isEmpty()
        binding.rvTorneios.isVisible = !isEmpty
        binding.tvEmpty.isVisible    = isEmpty
        if (isEmpty) {
            binding.tvEmpty.text = when (state.tabAtiva) {
                "meus"  -> getString(R.string.sem_meus_torneios)
                else    -> getString(R.string.sem_torneios_publicos)
            }
        }
    }

    // ── Navegação ─────────────────────────────────────────────────────────────

    private fun navegarParaDetalhe(torneio: TorneioEntity) {
        val emCurso = torneio.estado in listOf("ADecorrer", "a_decorrer", "Terminado", "terminado")
        if (emCurso) {
            findNavController().navigate(
                R.id.action_torneiosFragment_to_torneioEmCursoFragment,
                bundleOf("torneioId" to torneio.id)
            )
        } else {
            findNavController().navigate(
                R.id.action_torneiosFragment_to_torneioDetalheFragment,
                bundleOf("torneioId" to torneio.id)
            )
        }
    }

    private fun navegarParaDetalhe(torneioId: String) {
        findNavController().navigate(
            R.id.action_torneiosFragment_to_torneioDetalheFragment,
            bundleOf("torneioId" to torneioId)
        )
    }

    private fun abrirInscricao(torneioId: String, minJogadores: Int, maxJogadores: Int) {
        if (parentFragmentManager.findFragmentByTag(InscricaoTorneioBottomSheet.TAG) != null) return
        val sheet = InscricaoTorneioBottomSheet.newInstance(torneioId)
        sheet.onNavegarParaCriarEquipa = {
            findNavController().navigate(
                R.id.action_torneiosFragment_to_criarEquipaFragment,
                bundleOf("torneioId" to torneioId, "minJogadores" to minJogadores, "maxJogadores" to maxJogadores)
            )
        }
        sheet.onNavegarParaGerirEquipa = { equipaId, equipaNome ->
            findNavController().navigate(
                R.id.action_torneiosFragment_to_gerirEquipaFragment,
                bundleOf("equipaId" to equipaId, "equipaNome" to equipaNome, "torneioId" to torneioId, "minJogadores" to minJogadores, "maxJogadores" to maxJogadores)
            )
        }
        sheet.show(parentFragmentManager, InscricaoTorneioBottomSheet.TAG)
    }

    // ── Dialog: Entrar com código ─────────────────────────────────────────────

    private fun mostrarDialogCodigo() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val db = DialogEntrarCodigoBinding.inflate(layoutInflater)
        dialog.setContentView(db.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        var dialogJob: kotlinx.coroutines.Job? = null
        dialogJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                codigoViewModel.state.collect { state ->
                    when (state) {
                        is EntrarCodigoState.Idle    -> {
                            db.progressDialog.isVisible  = false
                            db.tvDialogErro.isVisible    = false
                            db.btnDialogEntrar.isEnabled = true
                        }
                        is EntrarCodigoState.Loading -> {
                            db.progressDialog.isVisible  = true
                            db.tvDialogErro.isVisible    = false
                            db.btnDialogEntrar.isEnabled = false
                        }
                        is EntrarCodigoState.Error   -> {
                            db.progressDialog.isVisible  = false
                            db.tvDialogErro.isVisible    = true
                            db.tvDialogErro.text         = state.message
                            db.btnDialogEntrar.isEnabled = true
                        }
                        is EntrarCodigoState.Success -> {
                            dialog.dismiss()
                            codigoViewModel.resetState()
                            navegarParaDetalhe(state.torneio)
                        }
                    }
                }
            }
        }

        db.btnDialogEntrar.setOnClickListener {
            codigoViewModel.entrarComCodigo(db.etCodigo.text?.toString().orEmpty())
        }
        db.etCodigo.setOnEditorActionListener { _, _, _ ->
            codigoViewModel.entrarComCodigo(db.etCodigo.text?.toString().orEmpty())
            true
        }
        db.btnDialogCancelar.setOnClickListener {
            codigoViewModel.resetState()
            dialog.dismiss()
        }
        dialog.setOnDismissListener {
            codigoViewModel.resetState()
            dialogJob?.cancel()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
