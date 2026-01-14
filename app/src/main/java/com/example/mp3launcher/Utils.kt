package com.example.mp3launcher

import android.graphics.Bitmap
import java.util.concurrent.TimeUnit

fun formatMillis(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%d:%02d", minutes, seconds)
}

fun pixelate(bitmap: Bitmap, pixelSize: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val smallBitmap = Bitmap.createScaledBitmap(bitmap, width / pixelSize, height / pixelSize, true)
    return Bitmap.createScaledBitmap(smallBitmap, width, height, false)
}
