package com.hojetembola.app.ui.torneios

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
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

        // Setup RecyclerViews
        val casaAdapter      = TitularJogadorAdapter(casaSelectedIds)
        val visitanteAdapter = TitularJogadorAdapter(visitanteSelectedIds)

        binding.rvCasaJogadores.layoutManager      = LinearLayoutManager(requireContext())
        binding.rvCasaJogadores.adapter             = casaAdapter
        binding.rvVisitanteJogadores.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVisitanteJogadores.adapter        = visitanteAdapter

        casaAdapter.submitList(state.casaJogadores)
        visitanteAdapter.submitList(state.visitanteJogadores)

        // Save
        binding.btnGuardarTitulares.setOnClickListener {
            val casaEquipaId      = state.jogo?.equipaCasaId      ?: return@setOnClickListener
            val visitanteEquipaId = state.jogo?.equipaVisitanteId ?: return@setOnClickListener
            jogoViewModel.definirTitulares(
                casaEquipaId         = casaEquipaId,
                casaTitularIds       = casaSelectedIds.toList(),
                visitanteEquipaId    = visitanteEquipaId,
                visitanteTitularIds  = visitanteSelectedIds.toList()
            )
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "TitularesBottomSheet"
        fun newInstance() = TitularesBottomSheet()
    }
}

// ── Adapter ──────────────────────────────────────────────────────────────────

/**
 * Shows a list of players with a checkbox.
 * [selectedIds] is a shared mutable set owned by the Fragment — the adapter
 * reads and writes it directly so the Fragment always has the current state.
 */
class TitularJogadorAdapter(
    private val selectedIds: MutableSet<String>
) : ListAdapter<JogadorInscricaoComNome, TitularJogadorAdapter.Holder>(JogadorDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemJogadorTitularBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position), selectedIds)
    }

    class Holder(private val binding: ItemJogadorTitularBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(jogador: JogadorInscricaoComNome, selectedIds: MutableSet<String>) {
            binding.tvNomeJogador.text       = jogador.nome
            // Set state without triggering listener
            binding.checkboxTitular.setOnCheckedChangeListener(null)
            binding.checkboxTitular.isChecked = jogador.utilizadorId in selectedIds
            binding.checkboxTitular.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedIds.add(jogador.utilizadorId)
                else           selectedIds.remove(jogador.utilizadorId)
            }
            // Row tap also toggles the checkbox
            binding.root.setOnClickListener {
                binding.checkboxTitular.toggle()
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
