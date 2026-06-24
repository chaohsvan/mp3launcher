package com.example.mp3launcher

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioManager
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.PowerManager
import android.os.SystemClock

object ScreenOffVolumeKeySession {
    private const val SESSION_TAG = "MP3 Launcher Screen Off Volume Keys"
    private const val VOLUME_MAX = 100
    private const val VOLUME_CURRENT = 50

    private var session: MediaSession? = null

    fun sync(context: Context) {
        if (LauncherPreferences(context).volumeKeyMode == VolumeKeyMode.TRACK_CONTROL) {
            start(context)
        } else {
            stop()
        }
    }

    fun start(context: Context) {
        val appContext = context.applicationContext
        if (session != null) return

        session = MediaSession(appContext, SESSION_TAG).apply {
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    MediaCommandDispatcher.play(appContext)
                }

                override fun onPause() {
                    MediaCommandDispatcher.pause(appContext)
                }

                override fun onSkipToPrevious() {
                    MediaCommandDispatcher.previous(appContext)
                }

                override fun onSkipToNext() {
                    MediaCommandDispatcher.next(appContext)
                }
            })
            setPlaybackState(playbackState())
            setPlaybackToRemote(volumeProvider(appContext))
            isActive = true
        }
    }

    fun stop() {
        session?.release()
        session = null
    }

    private fun volumeProvider(context: Context): VolumeProvider {
        return object : VolumeProvider(VolumeProvider.VOLUME_CONTROL_RELATIVE, VOLUME_MAX, VOLUME_CURRENT) {
            override fun onAdjustVolume(direction: Int) {
                when (direction) {
                    AudioManager.ADJUST_RAISE -> handleVolumeDirection(context, direction) {
                        MediaCommandDispatcher.previous(context)
                    }
                    AudioManager.ADJUST_LOWER -> handleVolumeDirection(context, direction) {
                        MediaCommandDispatcher.next(context)
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun handleVolumeDirection(
        context: Context,
        direction: Int,
        controlTrack: () -> Unit
    ) {
        if (shouldControlTracks(context)) {
            controlTrack()
        } else {
            adjustMediaVolume(context, direction)
        }
    }

    private fun shouldControlTracks(context: Context): Boolean {
        val preferences = LauncherPreferences(context)
        if (preferences.volumeKeyMode != VolumeKeyMode.TRACK_CONTROL) return false

        if (!isDeviceLockedOrScreenOff(context)) return false

        return true
    }

    private fun isDeviceLockedOrScreenOff(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val locked = keyguardManager.isKeyguardLocked || keyguardManager.isDeviceLocked
        val screenOff = !powerManager.isInteractive
        return locked || screenOff
    }

    private fun adjustMediaVolume(context: Context, direction: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
    }

    private fun playbackState(): PlaybackState {
        val actions = PlaybackState.ACTION_PLAY or
            PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_SKIP_TO_PREVIOUS or
            PlaybackState.ACTION_SKIP_TO_NEXT
        return PlaybackState.Builder()
            .setActions(actions)
            .setState(
                PlaybackState.STATE_PLAYING,
                PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                1f,
                SystemClock.elapsedRealtime()
            )
            .build()
    }
}
