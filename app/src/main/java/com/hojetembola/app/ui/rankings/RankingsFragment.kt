package com.hojetembola.app.ui.rankings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.hojetembola.app.databinding.FragmentRankingsBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Ecrã Rankings — classificações e estatísticas.
 * Implementação completa num passo posterior.
 */
@AndroidEntryPoint
class RankingsFragment : Fragment() {

    private var _binding: FragmentRankingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRankingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
