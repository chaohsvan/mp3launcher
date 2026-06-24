package com.example.mp3launcher

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class LockScreenVolumeKeyService : AccessibilityService() {
    private val preferences by lazy { LauncherPreferences(this) }
    private val keyguardManager by lazy {
        getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    }
    private val powerManager by lazy {
        getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    private var volumeKeyDown = false
    private var longPressTriggered = false
    private var longPressRunnable: Runnable? = null
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var lastForegroundPackage: String? = null
    private var lockSurfaceSeenAtMillis = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.packageName?.toString()?.let {
            lastForegroundPackage = it
            if (it == SYSTEM_UI_PACKAGE || it == AOD_PACKAGE || !powerManager.isInteractive) {
                lockSurfaceSeenAtMillis = SystemClock.elapsedRealtime()
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onServiceConnected() {
        super.onServiceConnected()
        ScreenOffVolumeKeySession.sync(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        clearLongPress()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false
        }
        if (preferences.volumeKeyMode != VolumeKeyMode.TRACK_CONTROL) return false
        if (!shouldHandleVolumeKey()) return false

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    volumeKeyDown = true
                    longPressTriggered = false
                    refreshMediaController()
                    longPressRunnable = Runnable {
                        longPressTriggered = true
                        MediaCommandDispatcher.togglePlayPause(this)
                    }
                    longPressRunnable?.let {
                        longPressHandler.postDelayed(it, LONG_PRESS_DELAY)
                    }
                }
                return true
            }
            KeyEvent.ACTION_UP -> {
                clearLongPress()
                if (volumeKeyDown) {
                    volumeKeyDown = false
                    if (!longPressTriggered) {
                        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                            MediaCommandDispatcher.previous(this)
                        } else {
                            MediaCommandDispatcher.next(this)
                        }
                    }
                }
                return true
            }
        }

        return true
    }

    private fun refreshMediaController() {
        startService(
            android.content.Intent(this, MediaNotificationListenerService::class.java).apply {
                action = MediaNotificationListenerService.ACTION_REQUEST_MEDIA_UPDATE
            }
        )
    }

    private fun clearLongPress() {
        longPressRunnable?.let(longPressHandler::removeCallbacks)
        longPressRunnable = null
    }

    private fun shouldHandleVolumeKey(): Boolean {
        if (isDeviceLockedOrScreenOff() || wasLockSurfaceSeenRecently()) return true
        if (!preferences.isGlobalVolumeKeyTrackEnabled) return false
        if (lastForegroundPackage == packageName) return false
        return true
    }

    private fun isDeviceLockedOrScreenOff(): Boolean {
        return keyguardManager.isKeyguardLocked || keyguardManager.isDeviceLocked || !powerManager.isInteractive
    }

    private fun wasLockSurfaceSeenRecently(): Boolean {
        return lockSurfaceSeenAtMillis > 0 &&
            SystemClock.elapsedRealtime() - lockSurfaceSeenAtMillis <= LOCK_SURFACE_GRACE_MS
    }

    companion object {
        private const val LONG_PRESS_DELAY = 500L
        private const val LOCK_SURFACE_GRACE_MS = 2_000L
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
        private const val AOD_PACKAGE = "com.samsung.android.app.aodservice"
    }
}
