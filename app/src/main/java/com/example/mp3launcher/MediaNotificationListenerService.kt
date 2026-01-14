package com.example.mp3launcher

import android.app.Notification
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MediaNotificationListenerService : NotificationListenerService() {

    companion object {
        const val ACTION_MEDIA_UPDATE = "com.example.mp3launcher.MEDIA_UPDATE"
        const val ACTION_MEDIA_CLEAR = "com.example.mp3launcher.MEDIA_CLEAR"
        const val ACTION_PLAYBACK_STATE_UPDATE = "com.example.mp3launcher.PLAYBACK_STATE_UPDATE"
        const val ACTION_REQUEST_MEDIA_UPDATE = "com.example.mp3launcher.REQUEST_MEDIA_UPDATE"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ARTIST = "artist"
        const val EXTRA_ALBUM_ART = "album_art"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_PLAYBACK_STATE = "playback_state"
        const val EXTRA_POSITION = "position"
        const val EXTRA_PLAYBACK_SPEED = "playback_speed"
        var mediaController: MediaController? = null
    }

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            state?.let { sendPlaybackStateUpdate(it) }
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            metadata?.let { sendMediaUpdate(it) }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        // When the service is connected, check for any active media notifications
        activeNotifications.forEach { sbn ->
            if (sbn.notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) {
                handleNotification(sbn)
                return // Process the first found media notification and exit
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_REQUEST_MEDIA_UPDATE) {
            mediaController?.let {
                it.metadata?.let { metadata -> sendMediaUpdate(metadata) }
                it.playbackState?.let { state -> sendPlaybackStateUpdate(state) }
            }
        }
        return START_STICKY
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        handleNotification(sbn)
    }

    private fun handleNotification(sbn: StatusBarNotification) {
        if (sbn.notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) {
            val token = sbn.notification.extras.getParcelable(Notification.EXTRA_MEDIA_SESSION) as android.media.session.MediaSession.Token?
            token?.let {
                val newController = MediaController(applicationContext, it)

                if (mediaController?.sessionToken != newController.sessionToken) {
                    mediaController?.unregisterCallback(callback)
                    mediaController = newController
                    mediaController?.registerCallback(callback)
                }
                
                // Always send an update when a notification is handled
                newController.metadata?.let { metadata -> sendMediaUpdate(metadata) }
                newController.playbackState?.let { state -> sendPlaybackStateUpdate(state) }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn?.notification?.extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true) {
            val token = sbn.notification.extras.getParcelable(Notification.EXTRA_MEDIA_SESSION) as android.media.session.MediaSession.Token?
            if (token == mediaController?.sessionToken) {
                mediaController?.unregisterCallback(callback)
                mediaController = null
                LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_MEDIA_CLEAR))
            }
        }
    }

    private fun sendMediaUpdate(metadata: MediaMetadata) {
        val intent = Intent(ACTION_MEDIA_UPDATE)
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
        val albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)

        intent.putExtra(EXTRA_TITLE, title)
        intent.putExtra(EXTRA_ARTIST, artist)
        intent.putExtra(EXTRA_ALBUM_ART, albumArt)
        intent.putExtra(EXTRA_DURATION, duration)

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendPlaybackStateUpdate(state: PlaybackState) {
        val intent = Intent(ACTION_PLAYBACK_STATE_UPDATE)
        intent.putExtra(EXTRA_PLAYBACK_STATE, state.state)
        intent.putExtra(EXTRA_POSITION, state.position)
        intent.putExtra(EXTRA_PLAYBACK_SPEED, state.playbackSpeed)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}
