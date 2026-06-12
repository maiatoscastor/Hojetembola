package com.hojetembola.app.ui.torneios

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hojetembola.app.R
import com.hojetembola.app.data.local.dao.JogadorInscricaoComNome
import com.hojetembola.app.databinding.FragmentRegistarEventoBottomSheetBinding

class RegistarEventoBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentRegistarEventoBottomSheetBinding? = null
    private val binding get() = _binding!!

    // Shares the ViewModel already created by JogoDetalheFragment
    private val jogoViewModel: JogoDetalheViewModel by lazy {
        ViewModelProvider(requireParentFragment())[JogoDetalheViewModel::class.java]
    }

    private var selectedTipo = "golo"
    private var selectedEquipaId: String? = null
    private var selectedJogadorId: String? = null
    private var selectedJogadorSaiId: String? = null
    private var selectedJogadorEntraId: String? = null

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.setBackgroundResource(R.color.bg_surface)
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistarEventoBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val state = jogoViewModel.uiState.value
        val minutoAtual = jogoViewModel.minutoVivo.value

        // Populate team chips with real team names
        binding.chipEquipaCasa.text = state.casaNome.ifBlank { getString(R.string.casa) }
        binding.chipEquipaVisitante.text = state.visitanteNome.ifBlank { "Visitante" }

        // Pre-fill minute with live timer
        if (minutoAtual != null) binding.etMinuto.setText(minutoAtual.toString())

        // Default: Golo, Casa selected
        selectedTipo = "golo"
        selectedEquipaId = state.jogo?.equipaCasaId
        updateFieldVisibility("golo")
        populateJogadoresDropdown(state.casaJogadoresEmCampo.ifEmpty { state.casaJogadores })
        updateSubstituicaoChipState(state)

        // Tipo chip group
        binding.chipGroupTipo.setOnCheckedStateChangeListener { _, checkedIds ->
            val newTipo = when {
                checkedIds.contains(binding.chipGolo.id)         -> "golo"
                checkedIds.contains(binding.chipAmarelo.id)      -> "amarelo"
                checkedIds.contains(binding.chipVermelho.id)     -> "vermelho"
                checkedIds.contains(binding.chipSubstituicao.id) -> "substituicao"
                else -> selectedTipo
            }
            if (newTipo != selectedTipo) {
                selectedTipo = newTipo
                updateFieldVisibility(newTipo)
            }
        }

        // Equipa chip group — refreshes jogador dropdowns for the selected team
        binding.chipGroupEquipa.setOnCheckedStateChangeListener { _, checkedIds ->
            val isCasa = checkedIds.contains(binding.chipEquipaCasa.id)
            selectedEquipaId = if (isCasa) state.jogo?.equipaCasaId else state.jogo?.equipaVisitanteId

            val emCampo = if (isCasa) state.casaJogadoresEmCampo.ifEmpty { state.casaJogadores }
                          else        state.visitanteJogadoresEmCampo.ifEmpty { state.visitanteJogadores }
            val noBanco = if (isCasa) state.casaJogadoresNoBanco
                          else        state.visitanteJogadoresNoBanco

            populateJogadoresDropdown(emCampo)
            if (selectedTipo == "substituicao") populateSubstituicaoDropdowns(emCampo, noBanco)
        }

        // Confirmar
        binding.btnConfirmar.setOnClickListener {
            val minuto = binding.etMinuto.text.toString().toIntOrNull() ?: 0
            if (selectedTipo == "substituicao") {
                jogoViewModel.registarEvento(
                    tipo           = "substituicao",
                    minuto         = minuto,
                    equipaId       = selectedEquipaId,
                    jogadorSaiId   = selectedJogadorSaiId,
                    jogadorEntraId = selectedJogadorEntraId
                )
            } else {
                jogoViewModel.registarEvento(
                    tipo      = selectedTipo,
                    minuto    = minuto,
                    equipaId  = selectedEquipaId,
                    jogadorId = selectedJogadorId
                )
            }
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun updateFieldVisibility(tipo: String) {
        val isSub = tipo == "substituicao"
        binding.tilJogador.isVisible      = !isSub
        binding.tilJogadorSai.isVisible   = isSub
        binding.tilJogadorEntra.isVisible = isSub
        if (isSub) {
            val isCasa = binding.chipGroupEquipa.checkedChipId == binding.chipEquipaCasa.id
            val state = jogoViewModel.uiState.value
            val emCampo = if (isCasa) state.casaJogadoresEmCampo.ifEmpty { state.casaJogadores }
                          else        state.visitanteJogadoresEmCampo.ifEmpty { state.visitanteJogadores }
            val noBanco = if (isCasa) state.casaJogadoresNoBanco
                          else        state.visitanteJogadoresNoBanco
            populateSubstituicaoDropdowns(emCampo, noBanco)
        }
    }

    /**
     * Disable the Substituição chip when there are no bench players for the
     * currently selected team, making substitution impossible.
     */
    private fun updateSubstituicaoChipState(state: JogoDetalheUiState) {
        val isCasa = binding.chipGroupEquipa.checkedChipId == binding.chipEquipaCasa.id
        val noBanco = if (isCasa) state.casaJogadoresNoBanco else state.visitanteJogadoresNoBanco
        val canSub = noBanco.isNotEmpty()
        binding.chipSubstituicao.isEnabled = canSub
        binding.chipSubstituicao.alpha = if (canSub) 1f else 0.4f
    }

    /** Players on field can receive goals / cards (golo, amarelo, vermelho). */
    private fun populateJogadoresDropdown(emCampo: List<JogadorInscricaoComNome>) {
        val nomes = mutableListOf(getString(R.string.anonimo)).apply {
            addAll(emCampo.map { it.nome })
        }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, nomes)
        binding.spinnerJogador.setAdapter(adapter)
        binding.spinnerJogador.setText(getString(R.string.anonimo), false)
        selectedJogadorId = null
        binding.spinnerJogador.setOnItemClickListener { _, _, pos, _ ->
            selectedJogadorId = if (pos == 0) null else emCampo.getOrNull(pos - 1)?.utilizadorId
        }
    }

    /**
     * Substitution: sai must be on field, entra must be on bench.
     * If bench is empty the substituição chip is disabled so this should not be reached.
     */
    private fun populateSubstituicaoDropdowns(
        emCampo: List<JogadorInscricaoComNome>,
        noBanco: List<JogadorInscricaoComNome>
    ) {
        // Quem sai — only players currently on field
        val nomesSai = emCampo.map { it.nome }
        val adapterSai = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, nomesSai)
        binding.spinnerJogadorSai.setAdapter(adapterSai)
        binding.spinnerJogadorSai.setText(nomesSai.firstOrNull() ?: "", false)
        selectedJogadorSaiId = emCampo.firstOrNull()?.utilizadorId
        binding.spinnerJogadorSai.setOnItemClickListener { _, _, pos, _ ->
            selectedJogadorSaiId = emCampo.getOrNull(pos)?.utilizadorId
        }

        // Quem entra — only players currently on bench
        val nomesEntra = noBanco.map { it.nome }
        val adapterEntra = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, nomesEntra)
        binding.spinnerJogadorEntra.setAdapter(adapterEntra)
        binding.spinnerJogadorEntra.setText(nomesEntra.firstOrNull() ?: "", false)
        selectedJogadorEntraId = noBanco.firstOrNull()?.utilizadorId
        binding.spinnerJogadorEntra.setOnItemClickListener { _, _, pos, _ ->
            selectedJogadorEntraId = noBanco.getOrNull(pos)?.utilizadorId
        }
    }

    companion object {
        const val TAG = "RegistarEventoBottomSheet"
        fun newInstance() = RegistarEventoBottomSheet()
    }
}
