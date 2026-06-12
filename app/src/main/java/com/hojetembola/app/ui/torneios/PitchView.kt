package com.hojetembola.app.ui.torneios

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.ceil

/**
 * Draws a football pitch with both teams' current lineups distributed in rows.
 * Visitante team shows at the top, casa team at the bottom.
 * Call [setJogadores] to update; the view redraws automatically.
 */
class PitchView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var casaJogadores: List<String> = emptyList()
    private var visitanteJogadores: List<String> = emptyList()

    private val grassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2E7D32") }
    private val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#388E3C") }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 2.5f
    }
    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1A237E") }
    private val casaPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#BF360C") }
    private val pillStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#90CAF9"); style = Paint.Style.STROKE; strokeWidth = 1.5f
    }
    private val casaPillStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFCCBC"); style = Paint.Style.STROKE; strokeWidth = 1.5f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }

    fun setJogadores(casa: List<String>, visitante: List<String>) {
        casaJogadores = casa
        visitanteJogadores = visitante
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, (w * 1.5f).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        // Alternating grass stripes
        canvas.drawRect(0f, 0f, w, h, grassPaint)
        val stripeW = w / 10f
        for (i in 0..9 step 2) {
            canvas.drawRect(i * stripeW, 0f, (i + 1) * stripeW, h, stripePaint)
        }

        val pad = w * 0.06f

        // Pitch outline + center line + center circle
        canvas.drawRect(pad, pad, w - pad, h - pad, linePaint)
        canvas.drawLine(pad, h / 2f, w - pad, h / 2f, linePaint)
        canvas.drawCircle(w / 2f, h / 2f, w * 0.11f, linePaint)

        // Penalty areas + goal areas
        val penW = w * 0.44f; val penH = h * 0.11f
        val goalW = w * 0.18f; val goalH = h * 0.04f

        canvas.drawRect((w - penW) / 2f, pad, (w + penW) / 2f, pad + penH, linePaint)
        canvas.drawRect((w - goalW) / 2f, pad, (w + goalW) / 2f, pad + goalH, linePaint)
        canvas.drawRect((w - penW) / 2f, h - pad - penH, (w + penW) / 2f, h - pad, linePaint)
        canvas.drawRect((w - goalW) / 2f, h - pad - goalH, (w + goalW) / 2f, h - pad, linePaint)

        textPaint.textSize = w * 0.038f

        drawTeam(canvas, visitanteJogadores, w, h, isTop = true, pillPaint, pillStrokePaint)
        drawTeam(canvas, casaJogadores, w, h, isTop = false, casaPillPaint, casaPillStrokePaint)
    }

    private fun drawTeam(
        canvas: Canvas,
        jogadores: List<String>,
        w: Float, h: Float,
        isTop: Boolean,
        bg: Paint, stroke: Paint
    ) {
        if (jogadores.isEmpty()) return
        val pad = w * 0.06f
        val halfH = h / 2f
        val usableH = halfH - pad * 2.8f

        val rows = distributeRows(jogadores)
        val numRows = rows.size

        rows.forEachIndexed { rowIdx, row ->
            val frac = if (numRows == 1) 0.3f else rowIdx / (numRows - 1).toFloat()
            val y = if (isTop) pad * 1.8f + usableH * frac
                    else       h - pad * 1.8f - usableH * frac

            row.forEachIndexed { pIdx, nome ->
                val x = (pIdx + 1) * w / (row.size + 1)
                drawPill(canvas, nome, x, y, bg, stroke)
            }
        }
    }

    private fun drawPill(canvas: Canvas, nome: String, x: Float, y: Float, bg: Paint, stroke: Paint) {
        val display = nome.split(" ").first().take(10)
        val ts = textPaint.textSize
        val pillW = (textPaint.measureText(display) + ts * 1.4f).coerceAtLeast(ts * 2.4f)
        val pillH = ts * 1.8f
        val rect = RectF(x - pillW / 2f, y - pillH / 2f, x + pillW / 2f, y + pillH / 2f)
        canvas.drawRoundRect(rect, pillH / 2f, pillH / 2f, bg)
        canvas.drawRoundRect(rect, pillH / 2f, pillH / 2f, stroke)
        canvas.drawText(display, x, y + ts * 0.36f, textPaint)
    }

    private fun distributeRows(jogadores: List<String>): List<List<String>> {
        val gk = listOf(jogadores[0])
        val outfield = jogadores.drop(1)
        if (outfield.isEmpty()) return listOf(gk)
        val numRows = when {
            outfield.size <= 2 -> 1
            outfield.size <= 5 -> 2
            else -> 3
        }
        val rowSize = ceil(outfield.size / numRows.toDouble()).toInt()
        return listOf(gk) + outfield.chunked(rowSize)
    }
}
