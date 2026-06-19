package com.example.mp3launcher

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs

class MechanicalHoldSwitchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val slotRect = RectF()
    private val knobRect = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var checked = false
    private var progress = 0f
    private var downX = 0f
    private var downY = 0f
    private var downProgress = 0f
    private var dragging = false
    private var animator: ValueAnimator? = null

    private val slotColor = Color.rgb(34, 35, 36)
    private var knobColor = Color.rgb(249, 115, 22)
    private var labelColor = Color.rgb(180, 180, 180)

    var onCheckedChange: ((Boolean) -> Unit)? = null

    init {
        isClickable = true
    }

    fun setThemeColors(knob: Int, active: Int, label: Int) {
        knobColor = knob
        labelColor = label
        invalidate()
    }

    fun setSwitchLabels(primary: String, start: String = "", end: String = "") {
        invalidate()
    }

    fun setChecked(checked: Boolean, animate: Boolean = true, notify: Boolean = false) {
        if (this.checked == checked && progress == if (checked) 1f else 0f) return
        this.checked = checked
        animateTo(if (checked) 1f else 0f, animate)
        if (notify) onCheckedChange?.invoke(checked)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (108 * resources.displayMetrics.density).toInt()
        val desiredHeight = (38 * resources.displayMetrics.density).toInt()
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density

        slotRect.set(4f * density, 4f * density, width - 4f * density, height - 4f * density)
        val radius = slotRect.height() / 2f

        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = adjustColorBrightness(slotColor, 0.28f)
        canvas.drawRoundRect(slotRect, radius, radius, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f * density
        paint.color = adjustColorBrightness(labelColor, 0.42f)
        canvas.drawRoundRect(slotRect, radius, radius, paint)

        val innerInset = 3.2f * density
        val innerRect = RectF(slotRect).apply { inset(innerInset, innerInset) }
        paint.style = Paint.Style.FILL
        paint.color = adjustColorBrightness(slotColor, 0.58f)
        canvas.drawRoundRect(innerRect, innerRect.height() / 2f, innerRect.height() / 2f, paint)

        paint.color = adjustColorBrightness(slotColor, 0.18f)
        paint.strokeWidth = 1f * density
        canvas.drawLine(
            innerRect.left + 5f * density,
            innerRect.centerY() + 1f * density,
            innerRect.right - 5f * density,
            innerRect.centerY() + 1f * density,
            paint
        )

        drawScrew(canvas, innerRect.left + 6f * density, innerRect.centerY(), density)
        drawScrew(canvas, innerRect.right - 6f * density, innerRect.centerY(), density)

        val knobHeight = innerRect.height() + 8f * density
        val knobWidth = knobHeight * 1.42f
        val knobTravel = innerRect.width() - knobWidth
        val knobLeft = innerRect.left + knobTravel * progress
        val knobTop = innerRect.centerY() - knobHeight / 2f
        knobRect.set(knobLeft, knobTop, knobLeft + knobWidth, knobTop + knobHeight)

        val shadowRect = RectF(knobRect).apply { offset(0f, 2f * density) }
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            shadowRect.centerX(),
            shadowRect.centerY(),
            shadowRect.width() * 0.72f,
            adjustColorBrightness(slotColor, 0.05f),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(shadowRect, 6f * density, 6f * density, paint)
        paint.shader = null

        paint.color = adjustColorBrightness(knobColor, 0.42f)
        canvas.drawRoundRect(knobRect, 5.5f * density, 5.5f * density, paint)

        val faceRect = RectF(knobRect).apply { inset(1.6f * density, 1.5f * density) }
        paint.color = adjustColorBrightness(knobColor, if (checked) 1.15f else 0.96f)
        canvas.drawRoundRect(faceRect, 4.5f * density, 4.5f * density, paint)

        paint.color = adjustColorBrightness(knobColor, 1.35f)
        paint.strokeWidth = 1.2f * density
        val highlightY = faceRect.top + 3.4f * density
        canvas.drawLine(faceRect.left + 4f * density, highlightY, faceRect.right - 4f * density, highlightY, paint)

        paint.color = adjustColorBrightness(knobColor, 0.74f)
        paint.strokeWidth = 0.8f * density
        val grooveTop = faceRect.top + 7f * density
        repeat(3) { index ->
            val x = faceRect.left + faceRect.width() * (0.36f + index * 0.14f)
            canvas.drawLine(x, grooveTop, x, faceRect.bottom - 4f * density, paint)
        }
    }

    private fun drawScrew(canvas: Canvas, cx: Float, cy: Float, density: Float) {
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = adjustColorBrightness(labelColor, 0.28f)
        canvas.drawCircle(cx, cy, 1.8f * density, paint)
        paint.color = adjustColorBrightness(labelColor, 0.62f)
        canvas.drawCircle(cx - 0.35f * density, cy - 0.35f * density, 0.7f * density, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                downX = event.x
                downY = event.y
                downProgress = progress
                dragging = false
                animator?.cancel()
                isPressed = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (abs(event.x - downX) > touchSlop || abs(event.y - downY) > touchSlop) {
                    dragging = true
                }
                if (dragging) {
                    updateProgressFromDrag(event.x - downX)
                }
            }
            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                isPressed = false
                val nextChecked = if (dragging) progress >= 0.5f else !checked
                setChecked(nextChecked, animate = true, notify = true)
                performClick()
            }
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                isPressed = false
                animateTo(if (checked) 1f else 0f, animate = true)
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun updateProgressFromDrag(dx: Float) {
        val density = resources.displayMetrics.density
        val slotLeft = 7f * density
        val slotRight = width - 7f * density
        val innerWidth = (slotRight - slotLeft).coerceAtLeast(1f)
        progress = (downProgress + dx / innerWidth).coerceIn(0f, 1f)
        invalidate()
    }

    private fun animateTo(target: Float, animate: Boolean) {
        animator?.cancel()
        if (!animate) {
            progress = target
            invalidate()
            return
        }
        animator = ValueAnimator.ofFloat(progress, target).apply {
            duration = 180L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
}
