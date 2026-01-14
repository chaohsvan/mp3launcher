package com.example.mp3launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.util.Random

class AudioVisualizerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs) {

    private val barPaints = mutableListOf<Paint>()
    private val barHeights = mutableListOf<Float>()
    var isAnimating = false
        private set
    private val random = Random()
    private val animator: Runnable

    private val numBars = 8
    private val barWidthRatio = 0.6f // 60% of available space for the bar itself
    private val minHeightRatio = 0.15f

    init {
        // Create paints for different colors, similar to reference images
        val colors = listOf("#ef4444", "#f97316", "#eab308", "#ef4444", "#f97316", "#eab308", "#ef4444", "#f97316")
        colors.forEach {
            barPaints.add(Paint().apply {
                color = Color.parseColor(it)
                style = Paint.Style.FILL
                isAntiAlias = true
            })
            barHeights.add(minHeightRatio)
        }

        animator = object : Runnable {
            override fun run() {
                if (isAnimating) {
                    updateBars()
                    invalidate() // Request a redraw
                    postDelayed(this, 120) // Update frame every 120ms
                }
            }
        }
    }

    fun setAnimating(animating: Boolean) {
        if (animating == isAnimating) return
        isAnimating = animating
        if (isAnimating) {
            removeCallbacks(animator)
            post(animator)
        } else {
            removeCallbacks(animator)
            resetBars()
            invalidate() // Request a redraw for the reset state
        }
    }

    private fun updateBars() {
        for (i in 0 until numBars) {
            val randomFactor = minHeightRatio + (1.0f - minHeightRatio) * random.nextFloat()
            barHeights[i] = randomFactor
        }
    }

    private fun resetBars() {
        for (i in 0 until numBars) {
            barHeights[i] = minHeightRatio
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (barHeights.isEmpty()) return

        val totalBarAndGapWidth = width.toFloat() / numBars
        val barWidth = totalBarAndGapWidth * barWidthRatio
        val barGap = totalBarAndGapWidth - barWidth
        var currentX = barGap / 2

        for (i in 0 until numBars) {
            val barHeight = height * barHeights[i]
            val top = height - barHeight
            // Draw a rounded rect for better visuals
            canvas.drawRoundRect(currentX, top, currentX + barWidth, height.toFloat(), 4f, 4f, barPaints[i])
            currentX += totalBarAndGapWidth
        }
    }
}
