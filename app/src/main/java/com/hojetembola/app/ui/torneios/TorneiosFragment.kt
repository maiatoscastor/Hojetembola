package com.hojetembola.app.ui.torneios

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.hojetembola.app.databinding.FragmentTorneiosBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Ecrã Torneios — lista e criação de torneios.
 * Implementação completa num passo posterior.
 */
@AndroidEntryPoint
class TorneiosFragment : Fragment() {

    private var _binding: FragmentTorneiosBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTorneiosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
