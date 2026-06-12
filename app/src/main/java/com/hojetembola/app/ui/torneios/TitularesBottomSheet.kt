package com.hojetembola.app.ui.torneios

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.hojetembola.app.R
import com.hojetembola.app.data.local.dao.JogadorInscricaoComNome
import com.hojetembola.app.databinding.FragmentTitularesBottomSheetBinding
import com.hojetembola.app.databinding.ItemJogadorTitularBinding

class TitularesBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentTitularesBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val jogoViewModel: JogoDetalheViewModel by lazy {
        ViewModelProvider(requireParentFragment())[JogoDetalheViewModel::class.java]
    }

    // Mutable sets of checked player IDs — modified as the user taps checkboxes
    private val casaSelectedIds     = mutableSetOf<String>()
    private val visitanteSelectedIds = mutableSetOf<String>()

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundResource(R.color.bg_surface)
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTitularesBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val state = jogoViewModel.uiState.value

        // Team name headers
        binding.tvEquipaCasaHeader.text    = state.casaNome.ifBlank    { getString(R.string.casa) }
        binding.tvEquipaVisitanteHeader.text = state.visitanteNome.ifBlank { "Visitante" }

        // Pre-populate selections:
        //   • If no lineup was saved yet → all players start (default all checked)
        //   • Otherwise restore the saved titulares
        casaSelectedIds.clear()
        casaSelectedIds.addAll(
            if (state.casaTitularIds.isEmpty()) state.casaJogadores.map { it.utilizadorId }
            else state.casaTitularIds
        )

        visitanteSelectedIds.clear()
        visitanteSelectedIds.addAll(
            if (state.visitanteTitularIds.isEmpty()) state.visitanteJogadores.map { it.utilizadorId }
            else state.visitanteTitularIds
        )

        val required = state.numJogadoresPorEquipa

        fun updateCounters() {
            if (required > 0) {
                binding.tvContadorCasa.text =
                    getString(R.string.titulares_contador, casaSelectedIds.size, required)
                binding.tvContadorVisitante.text =
                    getString(R.string.titulares_contador, visitanteSelectedIds.size, required)
            }
        }

        // Setup RecyclerViews
        val suspensosIds = state.suspensosIds
        val casaAdapter = TitularJogadorAdapter(casaSelectedIds, suspensosIds) { updateCounters() }
        val visitanteAdapter = TitularJogadorAdapter(visitanteSelectedIds, suspensosIds) { updateCounters() }

        binding.rvCasaJogadores.layoutManager      = LinearLayoutManager(requireContext())
        binding.rvCasaJogadores.adapter             = casaAdapter
        binding.rvVisitanteJogadores.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVisitanteJogadores.adapter        = visitanteAdapter

        casaAdapter.submitList(state.casaJogadores)
        visitanteAdapter.submitList(state.visitanteJogadores)

        binding.tvContadorCasa.isVisible      = required > 0
        binding.tvContadorVisitante.isVisible = required > 0
        updateCounters()

        val startAfterSave = arguments?.getBoolean(ARG_START_AFTER_SAVE, false) ?: false
        if (startAfterSave) {
            binding.btnGuardarTitulares.text = getString(R.string.guardar_e_iniciar_jogo)
        }

        binding.btnGuardarTitulares.setOnClickListener {
            val casaEquipaId      = state.jogo?.equipaCasaId      ?: return@setOnClickListener
            val visitanteEquipaId = state.jogo?.equipaVisitanteId ?: return@setOnClickListener

            if (required > 0) {
                if (casaSelectedIds.size != required || visitanteSelectedIds.size != required) {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.titulares_erro_count, required),
                        Snackbar.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
            }

            if (startAfterSave) {
                jogoViewModel.definirTitularesEIniciar(
                    casaEquipaId        = casaEquipaId,
                    casaTitularIds      = casaSelectedIds.toList(),
                    visitanteEquipaId   = visitanteEquipaId,
                    visitanteTitularIds = visitanteSelectedIds.toList()
                )
            } else {
                jogoViewModel.definirTitulares(
                    casaEquipaId        = casaEquipaId,
                    casaTitularIds      = casaSelectedIds.toList(),
                    visitanteEquipaId   = visitanteEquipaId,
                    visitanteTitularIds = visitanteSelectedIds.toList()
                )
            }
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "TitularesBottomSheet"
        private const val ARG_START_AFTER_SAVE = "start_after_save"

        fun newInstance(startAfterSave: Boolean = false) = TitularesBottomSheet().apply {
            arguments = Bundle().also { it.putBoolean(ARG_START_AFTER_SAVE, startAfterSave) }
        }
    }
}

// ── Adapter ──────────────────────────────────────────────────────────────────

/**
 * Shows a list of players with a checkbox.
 * [selectedIds] is a shared mutable set owned by the Fragment — the adapter
 * reads and writes it directly so the Fragment always has the current state.
 */
class TitularJogadorAdapter(
    private val selectedIds: MutableSet<String>,
    private val suspensosIds: Set<String> = emptySet(),
    private val onToggle: () -> Unit = {}
) : ListAdapter<JogadorInscricaoComNome, TitularJogadorAdapter.Holder>(JogadorDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemJogadorTitularBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position), selectedIds, suspensosIds, onToggle)
    }

    class Holder(private val binding: ItemJogadorTitularBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            jogador: JogadorInscricaoComNome,
            selectedIds: MutableSet<String>,
            suspensosIds: Set<String>,
            onToggle: () -> Unit
        ) {
            val suspenso = jogador.utilizadorId in suspensosIds
            if (suspenso) {
                binding.tvNomeJogador.text  = "${jogador.nome} 🚫 (suspenso)"
                binding.tvNomeJogador.alpha = 0.45f
                binding.checkboxTitular.setOnCheckedChangeListener(null)
                binding.checkboxTitular.isChecked = false
                binding.checkboxTitular.isEnabled = false
                binding.checkboxTitular.alpha = 0.3f
                selectedIds.remove(jogador.utilizadorId)
                binding.root.setOnClickListener(null)
            } else {
                binding.tvNomeJogador.text  = jogador.nome
                binding.tvNomeJogador.alpha = 1f
                binding.checkboxTitular.isEnabled = true
                binding.checkboxTitular.alpha = 1f
                binding.checkboxTitular.setOnCheckedChangeListener(null)
                binding.checkboxTitular.isChecked = jogador.utilizadorId in selectedIds
                binding.checkboxTitular.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selectedIds.add(jogador.utilizadorId)
                    else           selectedIds.remove(jogador.utilizadorId)
                    onToggle()
                }
                binding.root.setOnClickListener { binding.checkboxTitular.toggle() }
            }
        }
    }

    private object JogadorDiff : DiffUtil.ItemCallback<JogadorInscricaoComNome>() {
        override fun areItemsTheSame(a: JogadorInscricaoComNome, b: JogadorInscricaoComNome) =
            a.utilizadorId == b.utilizadorId
        override fun areContentsTheSame(a: JogadorInscricaoComNome, b: JogadorInscricaoComNome) =
            a == b
    }
}
