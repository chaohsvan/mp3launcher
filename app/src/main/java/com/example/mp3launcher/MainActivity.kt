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
import android.view.ViewConfiguration
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
    internal var topScrollerScrollListener: RecyclerView.OnScrollListener? = null
    internal var audioRouteLightAnimator: ObjectAnimator? = null
    internal var audioRouteLightKey: String? = null
    internal var categoryIndicatorVisibleUntil: Long = 0L

    private val longPressHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var topScrollerLongPressRunnable: Runnable? = null
    private var topScrollerTouchTracking = false
    private var topScrollerHorizontalSwipe = false
    private var topScrollerLongPressTriggered = false
    private var topScrollerCategoryChanged = false
    private var topScrollerDownRawX = 0f
    private var topScrollerDownRawY = 0f
    internal var lcdSwipeDownX = 0f
    internal var lcdSwipeDownY = 0f
    internal var lcdSwipeTriggered = false
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
        binding.root.post {
            showCategoryIndicator(viewModel.uiState.value.selectedCategory)
        }
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
        if (handleTopScrollerGesture(ev)) return true
        return super.dispatchTouchEvent(ev)
    }

    private fun handleTopScrollerGesture(ev: MotionEvent): Boolean {
        val action = ev.actionMasked
        if (action == MotionEvent.ACTION_DOWN) {
            if (!isInsideTopScroller(ev)) {
                resetTopScrollerGesture()
                return false
            }
            topScrollerTouchTracking = true
            topScrollerHorizontalSwipe = false
            topScrollerLongPressTriggered = false
            topScrollerCategoryChanged = false
            topScrollerDownRawX = ev.rawX
            topScrollerDownRawY = ev.rawY
            scheduleTopScrollerLongPress(ev)
            return false
        }

        if (!topScrollerTouchTracking) return false

        val dx = ev.rawX - topScrollerDownRawX
        val dy = ev.rawY - topScrollerDownRawY
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop.toFloat()

        return when (action) {
            MotionEvent.ACTION_MOVE -> {
                if (topScrollerLongPressTriggered) return true
                if (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop) {
                    cancelTopScrollerLongPress()
                }
                if (
                    !topScrollerHorizontalSwipe &&
                    kotlin.math.abs(dx) > touchSlop &&
                    kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.2f
                ) {
                    topScrollerHorizontalSwipe = true
                    binding.topScroller.stopScroll()
                    cancelTopScrollerChildTouch(ev)
                }
                if (topScrollerHorizontalSwipe) {
                    maybeChangeTopScrollerCategory(dx)
                    true
                } else {
                    false
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val shouldConsume = topScrollerHorizontalSwipe || topScrollerLongPressTriggered
                if (topScrollerHorizontalSwipe && action == MotionEvent.ACTION_UP) {
                    maybeChangeTopScrollerCategory(dx)
                }
                resetTopScrollerGesture()
                shouldConsume
            }
            else -> false
        }
    }

    private fun isInsideTopScroller(ev: MotionEvent): Boolean {
        val rect = Rect()
        binding.topScroller.getGlobalVisibleRect(rect)
        return rect.contains(ev.rawX.toInt(), ev.rawY.toInt())
    }

    private fun scheduleTopScrollerLongPress(ev: MotionEvent) {
        cancelTopScrollerLongPress()
        val app = appUnderTopScrollerTouch(ev) ?: return
        val runnable = Runnable {
            topScrollerLongPressRunnable = null
            topScrollerLongPressTriggered = true
            binding.topScroller.stopScroll()
            window.decorView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            showAppActionMenu(app)
        }
        topScrollerLongPressRunnable = runnable
        longPressHandler.postDelayed(
            runnable,
            ViewConfiguration.getLongPressTimeout().toLong()
        )
    }

    private fun appUnderTopScrollerTouch(ev: MotionEvent): AppInfo? {
        val location = IntArray(2)
        binding.topScroller.getLocationOnScreen(location)
        val localX = ev.rawX - location[0]
        val localY = ev.rawY - location[1]
        val child = binding.topScroller.findChildViewUnder(localX, localY) ?: return null
        val position = binding.topScroller.getChildAdapterPosition(child)
        if (position == RecyclerView.NO_POSITION) return null
        return (binding.topScroller.adapter as? AppsAdapter)?.getAppAt(position)
    }

    private fun maybeChangeTopScrollerCategory(dx: Float) {
        if (topScrollerCategoryChanged || kotlin.math.abs(dx) < 56f) return
        viewModel.selectAdjacentCategory(if (dx < 0) 1 else -1)
        topScrollerCategoryChanged = true
    }

    private fun cancelTopScrollerChildTouch(ev: MotionEvent) {
        val cancelEvent = MotionEvent.obtain(ev)
        cancelEvent.action = MotionEvent.ACTION_CANCEL
        super.dispatchTouchEvent(cancelEvent)
        cancelEvent.recycle()
    }

    private fun cancelTopScrollerLongPress() {
        topScrollerLongPressRunnable?.let(longPressHandler::removeCallbacks)
        topScrollerLongPressRunnable = null
    }

    private fun resetTopScrollerGesture() {
        cancelTopScrollerLongPress()
        topScrollerTouchTracking = false
        topScrollerHorizontalSwipe = false
        topScrollerLongPressTriggered = false
        topScrollerCategoryChanged = false
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
