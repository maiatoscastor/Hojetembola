package com.hojetembola.app.ui.torneios

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.TextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hojetembola.app.R
import com.hojetembola.app.data.local.dao.JogoComEquipas
import com.hojetembola.app.data.local.entity.JornadaEntity
import com.hojetembola.app.databinding.FragmentCalendarioBinding
import com.hojetembola.app.databinding.ItemJogoCardBinding
import com.hojetembola.app.databinding.ItemJornadaHeaderBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class CalendarioFragment : Fragment() {

    private var _binding: FragmentCalendarioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalendarioViewModel by viewModels()
    private lateinit var adapter: CalendarioAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCalendarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        val isOrganizador = arguments?.getBoolean("isOrganizador", false) ?: false

        adapter = CalendarioAdapter { jogo ->
            findNavController().navigate(
                R.id.action_calendarioFragment_to_jogoDetalheFragment,
                bundleOf(
                    "jogoId" to jogo.jogoId,
                    "torneioId" to viewModel.torneioId,
                    "isOrganizador" to isOrganizador
                )
            )
        }

        binding.rvCalendario.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCalendario.adapter = adapter

        observeState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state is CalendarioUiState.Loading
                    binding.tvEmpty.isVisible = state is CalendarioUiState.Empty || state is CalendarioUiState.Error
                    binding.rvCalendario.isVisible = state is CalendarioUiState.Content

                    when (state) {
                        is CalendarioUiState.Content -> {
                            val items = mutableListOf<CalendarioItem>()
                            state.jornadas.forEach { jornadaComJogos ->
                                items.add(CalendarioItem.Header(jornadaComJogos.jornada))
                                jornadaComJogos.jogos.forEach { jogo ->
                                    items.add(CalendarioItem.Match(jogo))
                                }
                            }
                            adapter.submitList(items)
                        }
                        is CalendarioUiState.Error -> {
                            binding.tvEmpty.text = state.message
                        }
                        is CalendarioUiState.Empty -> {
                            binding.tvEmpty.text = getString(R.string.sem_jogos)
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

sealed class CalendarioItem {
    data class Header(val jornada: JornadaEntity) : CalendarioItem()
    data class Match(val jogo: JogoComEquipas) : CalendarioItem()
}

private const val ITEM_TYPE_HEADER = 0
private const val ITEM_TYPE_MATCH = 1

class CalendarioAdapter(
    private val onMatchClick: (JogoComEquipas) -> Unit
) : ListAdapter<CalendarioItem, RecyclerView.ViewHolder>(CalendarioDiff) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is CalendarioItem.Header -> ITEM_TYPE_HEADER
        is CalendarioItem.Match  -> ITEM_TYPE_MATCH
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ITEM_TYPE_HEADER -> HeaderViewHolder(
                ItemJornadaHeaderBinding.inflate(inflater, parent, false)
            )
            else -> MatchViewHolder(
                ItemJogoCardBinding.inflate(inflater, parent, false),
                onMatchClick
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is CalendarioItem.Header -> (holder as HeaderViewHolder).bind(item.jornada)
            is CalendarioItem.Match  -> (holder as MatchViewHolder).bind(item.jogo)
        }
    }

    class HeaderViewHolder(
        private val binding: ItemJornadaHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(jornada: JornadaEntity) {
            binding.tvJornada.text = binding.root.context.getString(R.string.jornada_n, jornada.numero)
        }
    }

    class MatchViewHolder(
        private val binding: ItemJogoCardBinding,
        private val onClick: (JogoComEquipas) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(jogo: JogoComEquipas) {
            binding.root.setOnClickListener { onClick(jogo) }

            // Casa avatar
            binding.tvAvatarCasa.text = jogo.casaIniciais
            setCircleBackground(binding.tvAvatarCasa, jogo.casaCor)

            // Visitante avatar
            binding.tvAvatarVisitante.text = jogo.visitanteIniciais
            setCircleBackground(binding.tvAvatarVisitante, jogo.visitanteCor)

            // Names
            binding.tvNomeCasa.text = jogo.casaNome
            binding.tvNomeVisitante.text = jogo.visitanteNome

            // Score or "vs"
            when (jogo.estado) {
                "agendado" -> {
                    binding.tvScore.text = binding.root.context.getString(R.string.vs)
                    binding.tvMinuto.isVisible = false
                }
                else -> {
                    val gc = jogo.golosCasa ?: 0
                    val gv = jogo.golosVisitante ?: 0
                    binding.tvScore.text = binding.root.context.getString(R.string.resultado_jogo, gc, gv)
                    if (jogo.estado == "ao_vivo" && jogo.minutoAtual != null) {
                        binding.tvMinuto.isVisible = true
                        binding.tvMinuto.text = binding.root.context.getString(R.string.minuto_jogo, jogo.minutoAtual)
                    } else {
                        binding.tvMinuto.isVisible = false
                    }
                }
            }

            // Estado badge
            when (jogo.estado) {
                "ao_vivo" -> {
                    binding.tvEstado.text = binding.root.context.getString(R.string.ao_vivo)
                    binding.tvEstado.setTextColor(Color.parseColor("#F57C00"))
                }
                "terminado" -> {
                    binding.tvEstado.text = "FIM"
                    binding.tvEstado.setTextColor(Color.parseColor("#8A9BB8"))
                }
                else -> {
                    binding.tvEstado.text = ""
                }
            }

            // Date/time
            binding.tvDataHora.text = formatDataHora(jogo.dataHora)
        }

        private fun setCircleBackground(view: TextView, hexColor: String) {
            val color = try { Color.parseColor(hexColor) } catch (_: Exception) { Color.parseColor("#3D5A80") }
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
            view.background = drawable
        }

        private fun formatDataHora(raw: String): String = try {
            val dt = LocalDateTime.parse(raw)
            val fmt = DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale("pt", "PT"))
            dt.format(fmt)
        } catch (_: Exception) { raw }
    }

    private object CalendarioDiff : DiffUtil.ItemCallback<CalendarioItem>() {
        override fun areItemsTheSame(old: CalendarioItem, new: CalendarioItem): Boolean = when {
            old is CalendarioItem.Header && new is CalendarioItem.Header -> old.jornada.id == new.jornada.id
            old is CalendarioItem.Match  && new is CalendarioItem.Match  -> old.jogo.jogoId == new.jogo.jogoId
            else -> false
        }
        override fun areContentsTheSame(old: CalendarioItem, new: CalendarioItem) = old == new
    }
}
