package com.example.mp3launcher

import android.media.session.PlaybackState

object MediaCommandDispatcher {
    fun next() {
        MediaNotificationListenerService.mediaController?.transportControls?.skipToNext()
    }

    fun previous() {
        MediaNotificationListenerService.mediaController?.transportControls?.skipToPrevious()
    }

    fun togglePlayPause() {
        val controller = MediaNotificationListenerService.mediaController ?: return
        if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun play() {
        MediaNotificationListenerService.mediaController?.transportControls?.play()
    }

    fun pause() {
        MediaNotificationListenerService.mediaController?.transportControls?.pause()
    }
}
