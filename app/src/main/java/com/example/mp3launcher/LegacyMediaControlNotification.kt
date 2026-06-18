package com.example.mp3launcher

import android.app.NotificationManager
import android.content.Context
import android.os.Build

object LegacyMediaControlNotification {
    private const val NOTIFICATION_ID = 1987
    private val channelIds = listOf(
        "lock_screen_media_controls",
        "lock_screen_media_controls_v2"
    )

    fun dismiss(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelIds.forEach(notificationManager::deleteNotificationChannel)
        }
    }
}
