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
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.hojetembola.app.R
import com.hojetembola.app.databinding.FragmentCriarTorneioBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset

@AndroidEntryPoint
class CriarTorneioFragment : Fragment() {

    private var _binding: FragmentCriarTorneioBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CriarTorneioViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCriarTorneioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        setupTextFields()
        setupChips()
        setupSteppers()
        setupDatePickers()
        setupSwitches()
        binding.btnCriarTorneio.setOnClickListener { viewModel.criarTorneio() }
        observeForm()
        observeState()
    }

    // ── Text fields ───────────────────────────────────────────────────────────

    private fun setupTextFields() {
        binding.tilNome.editText?.addTextChangedListener(simpleWatcher { viewModel.setNome(it) })
        binding.tilLocalizacao.editText?.addTextChangedListener(simpleWatcher { viewModel.setLocalizacao(it) })
        binding.tilLocalizacaoLink.editText?.addTextChangedListener(simpleWatcher { viewModel.setLocalizacaoLink(it) })
        binding.tilRegulamento.editText?.addTextChangedListener(simpleWatcher { viewModel.setRegulamento(it) })
        binding.tilNumJogadores.editText?.addTextChangedListener(simpleWatcher { viewModel.setNumPersonalizado(it.toIntOrNull()) })
    }

    // ── Chips ─────────────────────────────────────────────────────────────────

    private fun setupChips() {
        binding.cgModalidade.setOnCheckedStateChangeListener { _, ids ->
            viewModel.setModalidade(when (ids.firstOrNull()) {
                R.id.chipFut5          -> "fut5"
                R.id.chipFut7          -> "fut7"
                R.id.chipFut11         -> "fut11"
                R.id.chipPersonalizado -> "personalizado"
                else                   -> "fut7"
            })
        }
        binding.cgFormato.setOnCheckedStateChangeListener { _, ids ->
            viewModel.setFormato(when (ids.firstOrNull()) {
                R.id.chipLiga         -> "liga"
                R.id.chipEliminatorias -> "eliminatorias"
                R.id.chipGruposElim   -> "grupos_eliminatorias"
                R.id.chipTodosVsTodos -> "todos_vs_todos"
                else                  -> "liga"
            })
        }
        binding.cgCriterio.setOnCheckedStateChangeListener { _, ids ->
            viewModel.setCriterioDesempate(when (ids.firstOrNull()) {
                R.id.chipPenalidades   -> "penalidades"
                R.id.chipProlongamento -> "prolongamento"
                R.id.chipGoloOuro      -> "golo_de_ouro"
                else                   -> "penalidades"
            })
        }
        binding.cgVisibilidade.setOnCheckedStateChangeListener { _, ids ->
            viewModel.setPublico(ids.firstOrNull() == R.id.chipPublico)
        }
    }

    // ── Steppers ──────────────────────────────────────────────────────────────

    private fun setupSteppers() {
        binding.btnDecEquipas.setOnClickListener   { viewModel.setMaxEquipas(viewModel.form.value.maxEquipas - 1) }
        binding.btnIncEquipas.setOnClickListener   { viewModel.setMaxEquipas(viewModel.form.value.maxEquipas + 1) }
        binding.btnDecJogadores.setOnClickListener { viewModel.setMaxJogadores(viewModel.form.value.maxJogadoresPorEquipa - 1) }
        binding.btnIncJogadores.setOnClickListener { viewModel.setMaxJogadores(viewModel.form.value.maxJogadoresPorEquipa + 1) }
        binding.btnDecAmarelos.setOnClickListener  { viewModel.setAmarelasParaSuspensao(viewModel.form.value.amarelasParaSuspensao - 1) }
        binding.btnIncAmarelos.setOnClickListener  { viewModel.setAmarelasParaSuspensao(viewModel.form.value.amarelasParaSuspensao + 1) }
    }

    // ── Date pickers ──────────────────────────────────────────────────────────

    private fun setupDatePickers() {
        fun openPicker(onSelected: (String) -> Unit) {
            val picker = MaterialDatePicker.Builder.datePicker().build()
            picker.addOnPositiveButtonClickListener { ms ->
                val iso = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toString()
                onSelected(iso)
            }
            picker.show(parentFragmentManager, "date_picker")
        }
        binding.etInicioInscricoes.setOnClickListener  { openPicker { viewModel.setDataInicioInscricoes(it) } }
        binding.tilInicioInscricoes.setEndIconOnClickListener { openPicker { viewModel.setDataInicioInscricoes(it) } }
        binding.etFimInscricoes.setOnClickListener     { openPicker { viewModel.setDataFimInscricoes(it) } }
        binding.tilFimInscricoes.setEndIconOnClickListener    { openPicker { viewModel.setDataFimInscricoes(it) } }
        binding.etDataInicio.setOnClickListener        { openPicker { viewModel.setDataInicio(it) } }
        binding.tilDataInicio.setEndIconOnClickListener       { openPicker { viewModel.setDataInicio(it) } }
        binding.etDataFim.setOnClickListener           { openPicker { viewModel.setDataFimPrevista(it) } }
        binding.tilDataFim.setEndIconOnClickListener          { openPicker { viewModel.setDataFimPrevista(it) } }
    }

    // ── Switches ──────────────────────────────────────────────────────────────

    private fun setupSwitches() {
        binding.switchEspectadores.setOnCheckedChangeListener { _, c -> viewModel.setPermitirEspectadores(c) }
        binding.switchMvp.setOnCheckedChangeListener          { _, c -> viewModel.setVotacaoMvpAtiva(c) }
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun observeForm() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.form.collect { f ->
                    binding.tvMaxEquipas.text    = f.maxEquipas.toString()
                    binding.tvMaxJogadores.text  = f.maxJogadoresPorEquipa.toString()
                    binding.tvAmarelos.text      = f.amarelasParaSuspensao.toString()
                    binding.tilNumJogadores.isVisible = f.modalidade == "personalizado"
                    // Update date display without triggering the TextWatcher
                    binding.etInicioInscricoes.setTextSilently(f.dataInicioInscricoes.toDisplayDate())
                    binding.etFimInscricoes.setTextSilently(f.dataFimInscricoes.toDisplayDate())
                    binding.etDataInicio.setTextSilently(f.dataInicio.toDisplayDate())
                    binding.etDataFim.setTextSilently(f.dataFimPrevista.toDisplayDate())
                }
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val loading = state is CriarTorneioUiState.Loading
                    binding.progressBar.isVisible      = loading
                    binding.btnCriarTorneio.isEnabled  = !loading
                    when (state) {
                        is CriarTorneioUiState.Success -> {
                            Snackbar.make(binding.root, R.string.sucesso_torneio_criado, Snackbar.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }
                        is CriarTorneioUiState.Error -> {
                            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                            viewModel.resetState()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun android.widget.EditText.setTextSilently(text: String) {
        if (this.text.toString() != text) setText(text)
    }

    private fun String.toDisplayDate(): String {
        if (isBlank()) return ""
        return try {
            val p = split("-")
            if (p.size == 3) "${p[2]}/${p[1]}/${p[0]}" else this
        } catch (_: Exception) { this }
    }

    private fun simpleWatcher(block: (String) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
        override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = Unit
        override fun afterTextChanged(s: Editable?) { block(s?.toString() ?: "") }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
