package com.hojetembola.app.ui.notificacoes

import android.graphics.Color
import android.os.Bundle
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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.hojetembola.app.R
import com.hojetembola.app.databinding.FragmentNotificacoesBinding
import com.hojetembola.app.databinding.ItemNotificacaoBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificacoesFragment : Fragment() {

    private var _binding: FragmentNotificacoesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificacoesViewModel by viewModels()

    private val adapter = NotificacaoAdapter(
        onAceitar = { conviteId -> viewModel.aceitarConvite(conviteId) },
        onRecusar = { conviteId -> viewModel.recusarConvite(conviteId) },
        onLida    = { notifId   -> viewModel.marcarComoLida(notifId) }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificacoesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnMarcarLidas.setOnClickListener { viewModel.marcarTodasComoLidas() }

        binding.rvNotificacoes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotificacoes.adapter = adapter

        observeState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.uiState.collect { state ->
                        binding.progressBar.isVisible = state.isLoading
                        binding.rvNotificacoes.isVisible = !state.isLoading && state.items.isNotEmpty()
                        binding.layoutVazio.isVisible   = !state.isLoading && state.items.isEmpty()

                        // "Marcar todas como lidas" só visível quando há notificações não lidas
                        val temNaoLidas = state.items.any { item ->
                            item is NotificacaoItem.Notificacao && !item.notificacao.lida
                        }
                        binding.btnMarcarLidas.isVisible = temNaoLidas

                        if (!state.isLoading) adapter.submitList(state.items)

                        state.mensagemAcao?.let { msg ->
                            Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                            viewModel.consumirMensagemAcao()
                        }
                    }
                }
            }
        }
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    inner class NotificacaoAdapter(
        private val onAceitar: (String) -> Unit,
        private val onRecusar: (String) -> Unit,
        private val onLida:    (String) -> Unit
    ) : ListAdapter<NotificacaoItem, NotificacaoAdapter.VH>(
        object : DiffUtil.ItemCallback<NotificacaoItem>() {
            override fun areItemsTheSame(old: NotificacaoItem, new: NotificacaoItem): Boolean {
                return when {
                    old is NotificacaoItem.Convite    && new is NotificacaoItem.Convite    -> old.convite.id == new.convite.id
                    old is NotificacaoItem.Notificacao && new is NotificacaoItem.Notificacao -> old.notificacao.id == new.notificacao.id
                    else -> false
                }
            }
            override fun areContentsTheSame(old: NotificacaoItem, new: NotificacaoItem) = old == new
        }
    ) {
        inner class VH(val b: ItemNotificacaoBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemNotificacaoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            when (val item = getItem(position)) {
                is NotificacaoItem.Convite -> bindConvite(holder.b, item)
                is NotificacaoItem.Notificacao -> bindNotificacao(holder.b, item)
            }
        }

        private fun bindConvite(b: ItemNotificacaoBinding, item: NotificacaoItem.Convite) {
            val convite = item.convite
            b.tvTitulo.text  = getString(R.string.convite_equipa_titulo)
            b.tvMensagem.text = getString(R.string.convite_equipa_mensagem, convite.nomeEquipa)
            b.ivTipo.setImageResource(R.drawable.ic_notification)
            b.ivTipo.setColorFilter(Color.parseColor("#F57C00"))
            b.dotNaoLida.isVisible  = true  // convites são sempre "não lidos"
            b.layoutAcoes.isVisible = true

            b.btnAceitar.setOnClickListener { onAceitar(convite.id) }
            b.btnRecusar.setOnClickListener { onRecusar(convite.id) }
        }

        private fun bindNotificacao(b: ItemNotificacaoBinding, item: NotificacaoItem.Notificacao) {
            val notif = item.notificacao
            b.tvTitulo.text   = notif.titulo
            b.tvMensagem.text  = notif.mensagem
            b.dotNaoLida.isVisible  = !notif.lida
            b.layoutAcoes.isVisible = false

            val (iconRes, tintHex) = iconForTipo(notif.tipo)
            b.ivTipo.setImageResource(iconRes)
            b.ivTipo.setColorFilter(Color.parseColor(tintHex))

            // Marca como lida ao tocar
            if (!notif.lida) {
                b.root.setOnClickListener { onLida(notif.id) }
            }
        }

        private fun iconForTipo(tipo: String): Pair<Int, String> = when (tipo) {
            "convite"               -> Pair(R.drawable.ic_notification, "#F57C00")
            "jogo_marcado"          -> Pair(R.drawable.ic_calendar,     "#4CAF50")
            "resultado"             -> Pair(R.drawable.ic_notification, "#1E3260")
            "suspensao"             -> Pair(R.drawable.ic_notification, "#E53935")
            "inscricao_aceite"      -> Pair(R.drawable.ic_notification, "#4CAF50")
            "inscricao_rejeitada"   -> Pair(R.drawable.ic_notification, "#E53935")
            "nova_inscricao"        -> Pair(R.drawable.ic_notification, "#1E3260")
            else                    -> Pair(R.drawable.ic_notification, "#8A9BB8")
        }
    }
}
