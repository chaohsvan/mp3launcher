package com.example.mp3launcher

import android.animation.ObjectAnimator
import android.content.*
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Rect
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.mp3launcher.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    internal lateinit var binding: ActivityMainBinding
    internal val viewModel: MainViewModel by viewModels()
    internal lateinit var audioManager: AudioManager
    internal lateinit var appsAdapter: AppsAdapter
    internal val preferences by lazy { LauncherPreferences(this) }
    internal var topScrollerGestureLockListener: RecyclerView.SimpleOnItemTouchListener? = null
    internal var topScrollerScrollListener: RecyclerView.OnScrollListener? = null
    internal var audioRouteLightAnimator: ObjectAnimator? = null
    internal var audioRouteLightKey: String? = null

    private val longPressHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var isLongPress = false
    private var lastDisplayedCategory: AppCategory? = null
    private val LONG_PRESS_DELAY = 500L // 500ms for long press definition

    private val settingsContentObserver by lazy {
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                updateVolumeDisplay()
            }
        }
    }

    private val batteryInfoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = level.toFloat() / scale.toFloat()
            updateStatusBar(binding.batteryStatusContainer, (batteryPct * 8).toInt(), 8)
        }
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            updateAudioOutputDisplay()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            updateAudioOutputDisplay()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isNotificationServiceEnabled()) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, Handler(Looper.getMainLooper()))

        setupAppDrawer()
        setupMediaControls()
        setupSearch()
        applyAlbumArtFadeFilter()
        updateVolumeDisplay()
        promptForDefaultLauncherIfNeeded()
        setupBackHandling()
        observeUiState()
        if (preferences.isBootAnimationEnabled) {
            showBootAnimation()
        }

        // Register Receivers and Observers
        registerReceiver(batteryInfoReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        applicationContext.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            settingsContentObserver
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Re-setup the layout manager when orientation changes
        setupAppDrawer()
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val componentName = ComponentName(this, MediaNotificationListenerService::class.java)
        return enabledListeners?.contains(componentName.flattenToString()) == true
    }

    private fun observeUiState() {
        // Main UI state collector
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                applyThemePreset(state.themePreset)
                applyLauncherMode(state.isMinimalModeEnabled)
                if (state.selectedCategory != lastDisplayedCategory) {
                    lastDisplayedCategory = state.selectedCategory
                    showCategoryIndicator(state.selectedCategory)
                }
                updateAudioOutputDisplay()
                binding.songTitle.text = state.songTitle
                binding.artistName.text = state.artistName
                binding.progressBar.max = state.duration.toInt()
                updateProgressDisplay(state.progress, state.duration)

                if (state.apps.isNotEmpty()) {
                    appsAdapter.updateApps(state.apps)
                }

                if (state.albumArt != null) {
                    lifecycleScope.launch(Dispatchers.Default) {
                        val pixelated = pixelate(state.albumArt, 16)
                        withContext(Dispatchers.Main) {
                            binding.albumArt.setImageBitmap(pixelated)
                        }
                    }
                } else {
                    binding.albumArt.setImageResource(R.drawable.ic_record_placeholder)
                }

                if (state.isPlaying) {
                    binding.playPauseButton.setImageResource(R.drawable.ic_pause)
                    binding.playbackStatus.text = "PLAYING"
                    binding.playbackIcon.setImageResource(R.drawable.ic_play)
                } else {
                    binding.playPauseButton.setImageResource(R.drawable.ic_play)
                    binding.playbackStatus.text = if(state.songTitle == "READY") "STOPPED" else "PAUSED"
                    binding.playbackIcon.setImageResource(R.drawable.ic_pause)
                }
            }
        }
    }

    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.searchEditText?.visibility == View.VISIBLE) {
                    hideSearch()
                } else {
                    isEnabled = false
                    try {
                        onBackPressedDispatcher.onBackPressed()
                    } finally {
                        isEnabled = true
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        viewModel.refreshSettings()
        requestMediaUpdate()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudioRouteLightBlink()
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        unregisterReceiver(batteryInfoReceiver)
        applicationContext.contentResolver.unregisterContentObserver(settingsContentObserver)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (viewModel.uiState.value.volumeKeyMode == VolumeKeyMode.SYSTEM_DEFAULT) {
                return super.onKeyDown(keyCode, event)
            }
            if (event?.repeatCount == 0) {
                requestMediaUpdate()
                isLongPress = false
                longPressRunnable = Runnable {
                    isLongPress = true
                    window.decorView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    // Toggle play/pause
                    if (viewModel.uiState.value.isPlaying) {
                        MediaNotificationListenerService.mediaController?.transportControls?.pause()
                    } else {
                        MediaNotificationListenerService.mediaController?.transportControls?.play()
                    }
                }
                longPressRunnable?.let { longPressHandler.postDelayed(it, LONG_PRESS_DELAY) }
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val volumeMode = viewModel.uiState.value.volumeKeyMode
            if (volumeMode == VolumeKeyMode.SYSTEM_DEFAULT) {
                return super.onKeyUp(keyCode, event)
            }
            longPressRunnable?.let(longPressHandler::removeCallbacks)
            longPressRunnable = null
            if (!isLongPress) {
                if (volumeMode == VolumeKeyMode.MEDIA_VOLUME) {
                    val direction = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                        AudioManager.ADJUST_RAISE
                    } else {
                        AudioManager.ADJUST_LOWER
                    }
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
                    updateVolumeDisplay()
                } else {
                    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                        MediaNotificationListenerService.mediaController?.transportControls?.skipToPrevious()
                    } else {
                        MediaNotificationListenerService.mediaController?.transportControls?.skipToNext()
                    }
                }
            }
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            binding.searchEditText?.let { searchView ->
                if (searchView.visibility == View.VISIBLE) {
                    val searchRect = Rect()
                    searchView.getGlobalVisibleRect(searchRect)

                    val recyclerRect = Rect()
                    binding.topScroller.getGlobalVisibleRect(recyclerRect)

                    val touchX = ev.rawX.toInt()
                    val touchY = ev.rawY.toInt()

                    if (!searchRect.contains(touchX, touchY) && !recyclerRect.contains(touchX, touchY)) {
                        hideSearch()
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    internal fun getCurrentAudioRouteLabel(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return "SPK"
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return when {
            outputs.any {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            } -> "BT"
            outputs.any {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET
            } -> "HP"
            else -> "SPK"
        }
    }

    private fun applyAlbumArtFadeFilter() {
        val fadeMatrix = ColorMatrix().apply {
            setSaturation(0.55f)
            postConcat(
                ColorMatrix(
                    floatArrayOf(
                        1.08f, 0f, 0f, 0f, 10f,
                        0f, 0.98f, 0f, 0f, 6f,
                        0f, 0f, 0.82f, 0f, -4f,
                        0f, 0f, 0f, 0.78f, 0f
                    )
                )
            )
        }
        binding.albumArt.colorFilter = ColorMatrixColorFilter(fadeMatrix)
    }
}
