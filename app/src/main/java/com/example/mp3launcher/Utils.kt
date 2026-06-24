package com.example.mp3launcher

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import java.util.concurrent.TimeUnit

fun formatMillis(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%d:%02d", minutes, seconds)
}

fun pixelate(bitmap: Bitmap, pixelSize: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val sampleWidth = (width / pixelSize).coerceAtLeast(1)
    val sampleHeight = (height / pixelSize).coerceAtLeast(1)
    val smallBitmap = Bitmap.createScaledBitmap(bitmap, sampleWidth, sampleHeight, true)
    return Bitmap.createScaledBitmap(smallBitmap, width, height, false)
}

inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelable(key) as? T
    }
}

fun roundedRectDrawable(color: Int, radius: Float, strokeColor: Int? = null, strokeWidth: Int = 0): GradientDrawable {
    return GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadius = radius
        if (strokeColor != null && strokeWidth > 0) {
            setStroke(strokeWidth, strokeColor)
        }
    }
}

fun verticalGradientDrawable(
    topColor: Int,
    bottomColor: Int,
    radius: Float,
    strokeColor: Int? = null,
    strokeWidth: Int = 0
): GradientDrawable {
    return GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(topColor, bottomColor)).apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = radius
        if (strokeColor != null && strokeWidth > 0) {
            setStroke(strokeWidth, strokeColor)
        }
    }
}

fun insetPanelDrawable(baseColor: Int, radius: Float): LayerDrawable {
    val highlight = adjustColorBrightness(baseColor, 1.16f)
    val shadow = adjustColorBrightness(baseColor, 0.62f)
    return LayerDrawable(
        arrayOf(
            roundedRectDrawable(shadow, radius + 4f),
            verticalGradientDrawable(highlight, baseColor, radius)
        )
    ).apply {
        setLayerInset(1, 3, 3, 3, 5)
    }
}

fun recessedDrawable(surfaceColor: Int, radius: Float): LayerDrawable {
    val shadow = adjustColorBrightness(surfaceColor, 0.34f)
    val face = adjustColorBrightness(surfaceColor, 0.88f)
    return LayerDrawable(
        arrayOf(
            roundedRectDrawable(shadow, radius + 3f),
            verticalGradientDrawable(face, surfaceColor, radius)
        )
    ).apply {
        setLayerInset(1, 5, 5, 5, 3)
    }
}

fun buttonStateDrawable(normalColor: Int, pressedColor: Int, radius: Float): StateListDrawable {
    return StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_pressed), recessedDrawable(pressedColor, radius))
        addState(intArrayOf(), insetPanelDrawable(normalColor, radius))
    }
}

fun ovalDrawable(color: Int, strokeColor: Int? = null, strokeWidth: Int = 0): GradientDrawable {
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        if (strokeColor != null && strokeWidth > 0) {
            setStroke(strokeWidth, strokeColor)
        }
    }
}

fun indicatorLightDrawable(lightColor: Int, housingColor: Int): LayerDrawable {
    return LayerDrawable(
        arrayOf(
            ovalDrawable(adjustColorBrightness(housingColor, 0.48f)),
            ovalDrawable(housingColor),
            ovalDrawable(adjustColorBrightness(lightColor, 1.28f)),
            ovalDrawable(lightColor)
        )
    ).apply {
        setLayerInset(1, 2, 2, 2, 2)
        setLayerInset(2, 5, 5, 5, 5)
        setLayerInset(3, 7, 7, 7, 7)
    }
}

fun adjustColorBrightness(color: Int, factor: Float): Int {
    return Color.argb(
        Color.alpha(color),
        (Color.red(color) * factor).toInt().coerceIn(0, 255),
        (Color.green(color) * factor).toInt().coerceIn(0, 255),
        (Color.blue(color) * factor).toInt().coerceIn(0, 255)
    )
}
