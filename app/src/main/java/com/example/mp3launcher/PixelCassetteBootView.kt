package com.example.mp3launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class PixelCassetteBootView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var themePreset: ThemePreset = ThemePreset.SILVER_DISCMAN
        set(value) {
            field = value
            invalidate()
        }

    var loadingText: String = "LOADING..."
        set(value) {
            field = value
            invalidate()
        }

    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    private val pixelTypeface =
        ResourcesCompat.getFont(context, R.font.press_start_2p_regular)
            ?: Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    private val fillPaint = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val textPaint = Paint().apply {
        isAntiAlias = false
        textAlign = Paint.Align.CENTER
    }
    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val theme = themePreset
        canvas.drawColor(theme.rootBackground)

        val minSide = min(width, height).coerceAtLeast(1)
        val scale = (minSide / 360f).coerceAtLeast(0.92f)
        val deckWidth = min(width * 0.78f, 460f * scale)
        val deckHeight = deckWidth * 0.58f
        val left = (width - deckWidth) / 2f
        val top = (height - deckHeight) / 2f
        val right = left + deckWidth
        val bottom = top + deckHeight
        val unit = 4f * scale

        drawPixelShadow(canvas, left, top, right, bottom, unit)
        drawPixelPanel(canvas, left, top, right, bottom, theme.deviceBackground, theme.deviceStroke, unit)
        drawCassetteFace(canvas, left, top, right, bottom, theme, scale, unit)
        drawLoadingTrack(canvas, left, bottom, deckWidth, theme, scale, unit)
        drawBootLabels(canvas, left, top, right, bottom, theme, scale)
    }

    private fun drawPixelShadow(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        unit: Float
    ) {
        fillPaint.color = Color.argb(105, 0, 0, 0)
        canvas.drawRect(left + unit * 2, top + unit * 2, right + unit * 2, bottom + unit * 2, fillPaint)
    }

    private fun drawCassetteFace(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        theme: ThemePreset,
        scale: Float,
        unit: Float
    ) {
        val deckWidth = right - left
        val deckHeight = bottom - top
        val inset = 22f * scale
        val headerHeight = 42f * scale
        val labelTop = top + 14f * scale

        fillPaint.color = withAlpha(theme.lcdBackground, 230)
        canvas.drawRect(left + inset, labelTop, right - inset, labelTop + headerHeight, fillPaint)
        strokePaint.color = theme.deviceStroke
        strokePaint.strokeWidth = unit * 0.75f
        canvas.drawRect(left + inset, labelTop, right - inset, labelTop + headerHeight, strokePaint)

        val windowLeft = left + deckWidth * 0.16f
        val windowTop = top + deckHeight * 0.34f
        val windowRight = right - deckWidth * 0.16f
        val windowBottom = top + deckHeight * 0.68f
        drawPixelPanel(
            canvas,
            windowLeft,
            windowTop,
            windowRight,
            windowBottom,
            withAlpha(theme.lcdBackground, 245),
            theme.deviceStroke,
            unit
        )

        val reelRadius = (windowBottom - windowTop) * 0.31f
        val leftReelX = windowLeft + (windowRight - windowLeft) * 0.29f
        val rightReelX = windowRight - (windowRight - windowLeft) * 0.29f
        val reelY = (windowTop + windowBottom) / 2f
        val rotation = progress * 18f * PI.toFloat()
        drawReel(canvas, leftReelX, reelY, reelRadius, rotation, theme, scale)
        drawReel(canvas, rightReelX, reelY, reelRadius, -rotation * 0.82f, theme, scale)

        strokePaint.color = theme.lcdText
        strokePaint.strokeWidth = unit * 0.75f
        val tapeY = reelY + reelRadius * 0.74f
        canvas.drawLine(leftReelX, tapeY, rightReelX, tapeY, strokePaint)

        drawTapePixels(canvas, windowLeft, windowRight, windowBottom, theme, scale)

        val screwSize = 9f * scale
        drawScrew(canvas, left + inset, top + deckHeight - inset, screwSize, theme)
        drawScrew(canvas, right - inset, top + deckHeight - inset, screwSize, theme)
        drawScrew(canvas, left + inset, top + inset, screwSize, theme)
        drawScrew(canvas, right - inset, top + inset, screwSize, theme)
    }

    private fun drawLoadingTrack(
        canvas: Canvas,
        left: Float,
        bottom: Float,
        deckWidth: Float,
        theme: ThemePreset,
        scale: Float,
        unit: Float
    ) {
        val trackLeft = left + deckWidth * 0.18f
        val trackRight = left + deckWidth * 0.82f
        val trackTop = bottom - 34f * scale
        val trackHeight = 10f * scale
        fillPaint.color = withAlpha(theme.controlStroke, 120)
        canvas.drawRect(trackLeft, trackTop, trackRight, trackTop + trackHeight, fillPaint)

        val segments = 18
        val gap = 3f * scale
        val segmentWidth = ((trackRight - trackLeft) - gap * (segments - 1)) / segments
        val activeSegments = (progress * segments).toInt().coerceIn(0, segments)
        for (i in 0 until segments) {
            val segmentLeft = trackLeft + i * (segmentWidth + gap)
            fillPaint.color = if (i < activeSegments) theme.accent else withAlpha(theme.lcdTextFaded, 80)
            canvas.drawRect(segmentLeft, trackTop, segmentLeft + segmentWidth, trackTop + trackHeight, fillPaint)
        }

        strokePaint.color = theme.deviceStroke
        strokePaint.strokeWidth = unit * 0.6f
        canvas.drawRect(trackLeft, trackTop, trackRight, trackTop + trackHeight, strokePaint)
    }

    private fun drawBootLabels(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        theme: ThemePreset,
        scale: Float
    ) {
        textPaint.typeface = pixelTypeface
        textPaint.color = theme.lcdText
        textPaint.textSize = 10f * scale
        canvas.drawText("PIXEL CASSETTE", (left + right) / 2f, top + 42f * scale, textPaint)

        textPaint.typeface = pixelTypeface
        textPaint.color = theme.lcdTextFaded
        textPaint.textSize = 6f * scale
        canvas.drawText("AUTO REVERSE // 8BIT TAPE", (left + right) / 2f, top + 78f * scale, textPaint)

        textPaint.typeface = pixelTypeface
        textPaint.color = theme.lcdText
        textPaint.textSize = 9f * scale
        canvas.drawText(loadingText, (left + right) / 2f, bottom - 48f * scale, textPaint)
    }

    private fun drawReel(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        rotation: Float,
        theme: ThemePreset,
        scale: Float
    ) {
        fillPaint.color = theme.deviceStroke
        canvas.drawCircle(cx, cy, radius, fillPaint)
        fillPaint.color = theme.controlBackground
        canvas.drawCircle(cx, cy, radius * 0.78f, fillPaint)
        fillPaint.color = theme.lcdBackground
        canvas.drawCircle(cx, cy, radius * 0.48f, fillPaint)
        fillPaint.color = theme.lcdText
        canvas.drawCircle(cx, cy, radius * 0.16f, fillPaint)

        val pixel = 5f * scale
        fillPaint.color = theme.lcdText
        for (i in 0 until 6) {
            val angle = rotation + i * PI.toFloat() / 3f
            val x = cx + cos(angle) * radius * 0.46f
            val y = cy + sin(angle) * radius * 0.46f
            canvas.drawRect(x - pixel / 2f, y - pixel / 2f, x + pixel / 2f, y + pixel / 2f, fillPaint)
        }
    }

    private fun drawTapePixels(
        canvas: Canvas,
        left: Float,
        right: Float,
        bottom: Float,
        theme: ThemePreset,
        scale: Float
    ) {
        val pixel = 5f * scale
        val count = 12
        val start = left + 18f * scale
        val available = right - left - 36f * scale
        val y = bottom - 17f * scale
        for (i in 0 until count) {
            val x = start + available * i / (count - 1)
            val active = ((progress * count).toInt() + i) % 3 != 0
            fillPaint.color = if (active) theme.lcdText else withAlpha(theme.lcdTextFaded, 70)
            canvas.drawRect(x, y, x + pixel, y + pixel, fillPaint)
        }
    }

    private fun drawScrew(canvas: Canvas, cx: Float, cy: Float, size: Float, theme: ThemePreset) {
        fillPaint.color = theme.deviceStroke
        canvas.drawRect(cx - size, cy - size, cx + size, cy + size, fillPaint)
        fillPaint.color = theme.controlStroke
        canvas.drawRect(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f, fillPaint)
    }

    private fun drawPixelPanel(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        fillColor: Int,
        strokeColor: Int,
        unit: Float
    ) {
        fillPaint.color = strokeColor
        canvas.drawRect(left, top, right, bottom, fillPaint)
        fillPaint.color = fillColor
        rect.set(left + unit, top + unit, right - unit, bottom - unit)
        canvas.drawRect(rect, fillPaint)
        fillPaint.color = withAlpha(Color.WHITE, 34)
        canvas.drawRect(left + unit, top + unit, right - unit, top + unit * 2f, fillPaint)
        fillPaint.color = withAlpha(Color.BLACK, 58)
        canvas.drawRect(left + unit, bottom - unit * 2f, right - unit, bottom - unit, fillPaint)
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        val combinedAlpha = (Color.alpha(color) * alpha / 255f).toInt().coerceIn(0, 255)
        return Color.argb(combinedAlpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
