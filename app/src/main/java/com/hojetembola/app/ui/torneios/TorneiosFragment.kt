package com.hojetembola.app.ui.torneios

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.hojetembola.app.R
import com.hojetembola.app.databinding.FragmentTorneiosBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TorneiosFragment : Fragment() {

    private var _binding: FragmentTorneiosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TorneiosViewModel by viewModels()
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
        observeState()
        binding.btnCriar.setOnClickListener {
            findNavController().navigate(R.id.action_torneiosFragment_to_criarTorneioFragment)
        }
    }

    private fun setupAdapter() {
        torneioAdapter = TorneioAdapter(
            onMeuClick     = { /* TODO: navegar para detalhe */ },
            onPublicoClick = { /* TODO: navegar para detalhe */ },
            onInscrever    = { Snackbar.make(binding.root, R.string.em_breve, Snackbar.LENGTH_SHORT).show() }
        )
        binding.rvTorneios.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = torneioAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupSearch() {
        binding.tilPesquisa.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setPesquisa(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

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

        val items = mutableListOf<TorneioListItem>()
        if (state.meusTorneios.isNotEmpty()) {
            items += TorneioListItem.Header(getString(R.string.meus_torneios))
            items += state.meusTorneios.map { TorneioListItem.Meu(it) }
        }
        if (state.torneiosPublicos.isNotEmpty()) {
            items += TorneioListItem.Header(getString(R.string.descobrir_torneios))
            items += state.torneiosPublicos.map { TorneioListItem.Publico(it) }
        }

        torneioAdapter.submitList(items)
        val isEmpty = items.isEmpty()
        binding.rvTorneios.isVisible = !isEmpty
        binding.tvEmpty.isVisible    = isEmpty
        if (isEmpty) binding.tvEmpty.text = getString(R.string.sem_torneios)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
