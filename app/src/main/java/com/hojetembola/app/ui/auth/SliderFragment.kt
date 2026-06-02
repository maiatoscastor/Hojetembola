package com.hojetembola.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.hojetembola.app.R
import com.hojetembola.app.databinding.FragmentSliderBinding
import com.hojetembola.app.databinding.ItemSlideBinding

/**
 * Ecrã de onboarding com 4 slides introdutórios.
 *
 * Mostrado APENAS na primeira abertura da app (a flag é gerida em SplashActivity).
 * Ao terminar (último slide → "Começar" ou qualquer slide → "Saltar"),
 * navega para [LoginFragment] via [R.id.action_slider_to_login].
 *
 * Layout:
 *  - Portrait : ViewPager2 ocupa a maior parte; dots + botão em baixo; "Saltar" no canto sup. dir.
 *  - Landscape: ViewPager2 à esquerda (60%); dots + botão à direita (40%)
 */
class SliderFragment : Fragment() {

    private var _binding: FragmentSliderBinding? = null
    private val binding get() = _binding!!

    // ── Dados dos 4 slides ────────────────────────────────────────────────────

    private val slides = listOf(
        SlideItem(
            iconRes   = R.drawable.ic_slider_calendar,
            titleRes  = R.string.slider_1_titulo,
            descRes   = R.string.slider_1_descricao
        ),
        SlideItem(
            iconRes   = R.drawable.ic_slider_football,
            titleRes  = R.string.slider_2_titulo,
            descRes   = R.string.slider_2_descricao
        ),
        SlideItem(
            iconRes   = R.drawable.ic_slider_star,
            titleRes  = R.string.slider_3_titulo,
            descRes   = R.string.slider_3_descricao
        ),
        SlideItem(
            iconRes   = R.drawable.ic_slider_ranking,
            titleRes  = R.string.slider_4_titulo,
            descRes   = R.string.slider_4_descricao
        )
    )

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSliderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
        buildDots()
        setupButtons()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── ViewPager2 ────────────────────────────────────────────────────────────

    private fun setupViewPager() {
        binding.viewPager.adapter = SliderAdapter(slides)
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                activateDot(position)
                binding.btnSeguinte.setText(
                    if (position == slides.lastIndex) R.string.slider_comecar
                    else R.string.slider_seguinte
                )
            }
        })
    }

    // ── Dots de progresso ─────────────────────────────────────────────────────

    private fun buildDots() {
        binding.dotsContainer.removeAllViews()
        slides.indices.forEach { index ->
            val dot = View(requireContext()).apply {
                val sizePx = dpToPx(if (index == 0) DOT_ACTIVE_DP else DOT_INACTIVE_DP)
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).also {
                    it.setMargins(dpToPx(DOT_MARGIN_DP), 0, dpToPx(DOT_MARGIN_DP), 0)
                }
                background = ContextCompat.getDrawable(
                    requireContext(),
                    if (index == 0) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive
                )
            }
            binding.dotsContainer.addView(dot)
        }
    }

    private fun activateDot(activeIndex: Int) {
        for (i in 0 until binding.dotsContainer.childCount) {
            val dot = binding.dotsContainer.getChildAt(i)
            val isActive = i == activeIndex
            val sizePx = dpToPx(if (isActive) DOT_ACTIVE_DP else DOT_INACTIVE_DP)
            (dot.layoutParams as LinearLayout.LayoutParams).apply {
                width  = sizePx
                height = sizePx
            }.also { dot.layoutParams = it }
            dot.background = ContextCompat.getDrawable(
                requireContext(),
                if (isActive) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive
            )
        }
    }

    // ── Botões ────────────────────────────────────────────────────────────────

    private fun setupButtons() {
        binding.btnSeguinte.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < slides.lastIndex) {
                binding.viewPager.currentItem = current + 1
            } else {
                goToLogin()
            }
        }
        binding.btnSaltar.setOnClickListener { goToLogin() }
    }

    private fun goToLogin() {
        findNavController().navigate(R.id.action_slider_to_login)
    }

    // ── Util ──────────────────────────────────────────────────────────────────

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density + 0.5f).toInt()

    // ── Dados ─────────────────────────────────────────────────────────────────

    data class SlideItem(
        @DrawableRes val iconRes: Int,
        @StringRes  val titleRes: Int,
        @StringRes  val descRes: Int
    )

    companion object {
        private const val DOT_ACTIVE_DP   = 10
        private const val DOT_INACTIVE_DP = 7
        private const val DOT_MARGIN_DP   = 4
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    private inner class SliderAdapter(
        private val items: List<SlideItem>
    ) : RecyclerView.Adapter<SliderAdapter.SlideViewHolder>() {

        inner class SlideViewHolder(val binding: ItemSlideBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
            val binding = ItemSlideBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return SlideViewHolder(binding)
        }

        override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
            val item = items[position]
            with(holder.binding) {
                imgSlideIcon.setImageResource(item.iconRes)
                tvSlideTitle.setText(item.titleRes)
                tvSlideDesc.setText(item.descRes)
            }
        }

        override fun getItemCount() = items.size
    }
}
