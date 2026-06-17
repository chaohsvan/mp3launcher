package com.example.mp3launcher

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
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
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
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
        val labelSize = 8f * density
        textPaint.textSize = labelSize

        textPaint.color = adjustColorBrightness(labelColor, if (checked) 1.18f else 0.82f)
        canvas.drawText("HOLD", 4f * density, 9f * density, textPaint)

        val slotTop = 14f * density
        val slotBottom = height - 4f * density
        slotRect.set(4f * density, slotTop, width - 4f * density, slotBottom)
        val radius = slotRect.height() / 2f

        paint.style = Paint.Style.FILL
        paint.color = adjustColorBrightness(slotColor, 0.42f)
        canvas.drawRoundRect(slotRect, radius, radius, paint)

        val innerInset = 3f * density
        val innerRect = RectF(slotRect).apply { inset(innerInset, innerInset) }
        paint.color = adjustColorBrightness(slotColor, 0.72f)
        canvas.drawRoundRect(innerRect, innerRect.height() / 2f, innerRect.height() / 2f, paint)

        val knobHeight = innerRect.height() + 7f * density
        val knobWidth = knobHeight * 1.48f
        val knobTravel = innerRect.width() - knobWidth
        val knobLeft = innerRect.left + knobTravel * progress
        val knobTop = innerRect.centerY() - knobHeight / 2f
        knobRect.set(knobLeft, knobTop, knobLeft + knobWidth, knobTop + knobHeight)

        paint.color = adjustColorBrightness(knobColor, 0.52f)
        canvas.drawRoundRect(knobRect, 5f * density, 5f * density, paint)

        knobRect.offset(0f, -1.5f * density)
        paint.color = adjustColorBrightness(knobColor, if (checked) 1.18f else 1.02f)
        canvas.drawRoundRect(knobRect, 5f * density, 5f * density, paint)

        paint.color = adjustColorBrightness(knobColor, 1.35f)
        paint.strokeWidth = 1.2f * density
        val highlightY = knobRect.top + 4f * density
        canvas.drawLine(knobRect.left + 4f * density, highlightY, knobRect.right - 4f * density, highlightY, paint)
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
