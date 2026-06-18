package com.example.mp3launcher

import android.content.Context
import android.media.AudioManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.view.KeyEvent

object MediaCommandDispatcher {
    fun next(context: Context? = null) {
        val controller = MediaNotificationListenerService.mediaController
        if (controller != null) {
            controller.transportControls.skipToNext()
        } else {
            dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_NEXT)
        }
    }

    fun previous(context: Context? = null) {
        val controller = MediaNotificationListenerService.mediaController
        if (controller != null) {
            controller.transportControls.skipToPrevious()
        } else {
            dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        }
    }

    fun togglePlayPause(context: Context? = null) {
        val controller = MediaNotificationListenerService.mediaController ?: run {
            dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            return
        }
        if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun play(context: Context? = null) {
        val controller = MediaNotificationListenerService.mediaController
        if (controller != null) {
            controller.transportControls.play()
        } else {
            dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY)
        }
    }

    fun pause(context: Context? = null) {
        val controller = MediaNotificationListenerService.mediaController
        if (controller != null) {
            controller.transportControls.pause()
        } else {
            dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PAUSE)
        }
    }

    private fun dispatchMediaKey(context: Context?, keyCode: Int) {
        context ?: return
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val now = SystemClock.uptimeMillis()
        audioManager.dispatchMediaKeyEvent(
            KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0)
        )
        audioManager.dispatchMediaKeyEvent(
            KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0)
        )
    }
}
