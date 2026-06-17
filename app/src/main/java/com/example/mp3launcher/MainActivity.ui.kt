package com.example.mp3launcher

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

fun MainActivity.updateProgressDisplay(currentPosition: Long, duration: Long) {
    if (duration > 0 && currentPosition <= duration) {
        binding.progressTextCurrent.text = formatMillis(currentPosition)
        binding.progressTextDuration.text = formatMillis(duration)
        binding.progressBar.progress = currentPosition.toInt()
    } else {
        binding.progressTextCurrent.text = "0:00"
        binding.progressTextDuration.text = "0:00"
        binding.progressBar.progress = 0
    }
}

fun MainActivity.updateVolumeDisplay() {
    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val volumePercent = currentVolume.toFloat() / maxVolume.toFloat()
    updateStatusBar(binding.volumeStatusContainer, (volumePercent * 8).toInt(), 8)
}

fun MainActivity.updateStatusBar(container: LinearLayout, activeCount: Int, totalCount: Int) {
    container.removeAllViews()
    for (i in 0 until totalCount) {
        val bar = View(this)
        val params = LinearLayout.LayoutParams(8, 11)
        params.setMargins(1, 0, 1, 0)
        bar.layoutParams = params
        bar.setBackgroundColor(viewModel.uiState.value.themePreset.lcdText)
        if (i >= activeCount) {
            bar.alpha = 0.2f
        }
        container.addView(bar)
    }
}

fun MainActivity.showAlphabetIndicator(letter: String) {
    binding.alphabetIndicator.animate().cancel()
    binding.alphabetIndicator.text = letter
    binding.alphabetIndicator.alpha = 1f
    binding.alphabetIndicator.handler?.removeCallbacksAndMessages(null) // Cancel previous fade out
    binding.alphabetIndicator.postDelayed({
        binding.alphabetIndicator.animate().alpha(0f).setDuration(500).start()
    }, 1000) // Show for 1 second before fading
}

fun MainActivity.showCategoryIndicator(category: AppCategory) {
    if (binding.searchEditText?.visibility == View.VISIBLE) return
    showAlphabetIndicator(category.label)
}

fun MainActivity.updateAudioOutputDisplay() {
    updateAudioRouteLight()
}

fun MainActivity.applyThemePreset(theme: ThemePreset) {
    binding.root.setBackgroundColor(theme.rootBackground)
    binding.topBarBackground?.setBackgroundColor(theme.rootBackground)
    binding.topScroller.setBackgroundColor(theme.rootBackground)
    binding.deviceChassis?.background = insetPanelDrawable(theme.deviceBackground, 34f)
    binding.mainDeviceBody?.background = insetPanelDrawable(theme.deviceBackground, 34f)
    binding.lcdScreen.background = recessedDrawable(theme.lcdBackground, 18f)
    binding.albumArt.background = recessedDrawable(theme.lcdText, 8f)
    binding.controlsContainer.background = insetPanelDrawable(theme.controlBackground, 22f)
    binding.controlPanel?.background = recessedDrawable(theme.controlBackground, 18f)
    binding.holdSwitch.setThemeColors(
        knob = theme.accent,
        active = theme.accent,
        label = theme.labelText
    )
    binding.songTitle.setTextColor(theme.lcdText)
    binding.artistName.setTextColor(theme.lcdTextFaded)
    binding.playbackStatus.setTextColor(theme.lcdText)
    binding.progressTextCurrent.setTextColor(theme.lcdText)
    binding.progressTextDuration.setTextColor(theme.lcdTextFaded)
    binding.searchEditText?.setTextColor(theme.accent)
    binding.searchEditText?.setHintTextColor(theme.lcdTextFaded)
    binding.alphabetIndicator.setTextColor(theme.lcdBackground)
    binding.progressBar.progressTintList = ColorStateList.valueOf(theme.lcdText)
    binding.progressBar.progressBackgroundTintList = ColorStateList.valueOf(theme.lcdTextFaded)
    binding.playPauseButton.background = buttonStateDrawable(theme.playButtonBackground, theme.buttonPressed, 16f)
    listOf(
        binding.nextButton,
        binding.prevButton,
        binding.rewindButton,
        binding.ffButton,
        binding.volUpButton,
        binding.volDownButton
    ).forEach { button ->
        button.background = buttonStateDrawable(theme.buttonBackground, theme.buttonPressed, 12f)
    }
    listOf(
        binding.playPauseButton,
        binding.nextButton,
        binding.prevButton,
        binding.rewindButton,
        binding.ffButton,
        binding.volUpButton,
        binding.volDownButton
    ).forEach { button ->
        button.imageTintList = ColorStateList.valueOf(theme.buttonIconTint)
    }
    colorControlLabels(binding.root, theme.labelText)
    updateAudioRouteLight()
}

fun MainActivity.applyLauncherMode(minimalMode: Boolean) {
    binding.topScroller.visibility = if (minimalMode) View.INVISIBLE else View.VISIBLE
    binding.searchEditText?.visibility = if (minimalMode) View.INVISIBLE else binding.searchEditText?.visibility ?: View.INVISIBLE
    binding.alphabetIndicator.visibility = if (minimalMode) View.INVISIBLE else View.VISIBLE
    binding.holdSwitch.setChecked(minimalMode, animate = true, notify = false)
}

fun MainActivity.updateAudioRouteLight() {
    val routeLabel = getCurrentAudioRouteLabel()
    val lightColor = when (routeLabel) {
        "BT" -> Color.rgb(45, 118, 255)
        "HP" -> Color.rgb(30, 210, 96)
        else -> Color.rgb(238, 54, 54)
    }
    val state = viewModel.uiState.value
    val housingColor = state.themePreset.indicatorHousing
    val lightKey = "$routeLabel:$lightColor:$housingColor:${state.isPlaying}"
    if (audioRouteLightKey == lightKey) return

    audioRouteLightKey = lightKey
    binding.audioRouteLight.background = indicatorLightDrawable(
        lightColor = lightColor,
        housingColor = housingColor
    )
    startAudioRouteLightBlink(state.isPlaying)
}

private fun MainActivity.startAudioRouteLightBlink(isPlaying: Boolean) {
    audioRouteLightAnimator?.cancel()
    val pulseValues = if (isPlaying) {
        floatArrayOf(0.58f, 1f, 0.78f, 0.96f, 0.62f)
    } else {
        floatArrayOf(0.38f, 0.78f, 0.48f, 0.7f, 0.42f)
    }
    binding.audioRouteLight.alpha = pulseValues.first()
    audioRouteLightAnimator = ObjectAnimator.ofFloat(
        binding.audioRouteLight,
        View.ALPHA,
        *pulseValues
    ).apply {
        duration = if (isPlaying) 1180L else 1650L
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        interpolator = LinearInterpolator()
        start()
    }
}

fun MainActivity.stopAudioRouteLightBlink() {
    audioRouteLightAnimator?.cancel()
    audioRouteLightAnimator = null
    audioRouteLightKey = null
    binding.audioRouteLight.alpha = 1f
}

private fun colorControlLabels(root: View, color: Int) {
    if (root is TextView) {
        val label = root.text?.toString().orEmpty()
        if (label in setOf("VOL-", "VOL+", "PREV", "NEXT", "REW", "FF")) {
            root.setTextColor(color)
        }
    }
    if (root is android.view.ViewGroup) {
        for (i in 0 until root.childCount) {
            colorControlLabels(root.getChildAt(i), color)
        }
    }
}

fun MainActivity.showBootAnimation() {
    val text = SettingsText.resolve(this, preferences)
    val loadingView = TextView(this).apply {
        this.text = text.loading
        textSize = 28f
        setTextColor(viewModel.uiState.value.themePreset.lcdText)
        setBackgroundColor(viewModel.uiState.value.themePreset.lcdBackground)
        gravity = android.view.Gravity.CENTER
        typeface = android.graphics.Typeface.MONOSPACE
        alpha = 0f
    }
    binding.root.addView(
        loadingView,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT
    )
    loadingView.animate().alpha(1f).setDuration(120).withEndAction {
        loadingView.postDelayed({
            loadingView.animate().alpha(0f).setDuration(260).withEndAction {
                binding.root.removeView(loadingView)
            }.start()
        }, 540)
    }.start()
}

fun MainActivity.promptForDefaultLauncherIfNeeded() {
    if (preferences.hasPromptedDefaultLauncher) return
    preferences.hasPromptedDefaultLauncher = true
    val text = SettingsText.resolve(this, preferences)
    AlertDialog.Builder(this)
        .setTitle(text.defaultLauncherTitle)
        .setMessage(text.defaultLauncherMessage)
        .setPositiveButton(text.open) { _, _ ->
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        }
        .setNegativeButton(text.later, null)
        .show()
}

fun MainActivity.setupAppDrawer() {
    val orientation = resources.configuration.orientation
    val layoutManager = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
        GridLayoutManager(this, 6, RecyclerView.HORIZONTAL, false)
    } else {
        GridLayoutManager(this, 6)
    }
    binding.topScroller.layoutManager = layoutManager

    appsAdapter = AppsAdapter(
        apps = emptyList(),
        onItemClicked = { app ->
            if (app == null) {
                showSearch()
            } else if (app.isSettingsShortcut) {
                hideSearch()
                startActivity(Intent(this, SettingsActivity::class.java))
            } else {
                hideSearch()
                val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName.toString())
                viewModel.recordAppLaunch(app)
                startActivity(launchIntent)
            }
        }
    )
    binding.topScroller.adapter = appsAdapter
    topScrollerGestureLockListener?.let(binding.topScroller::removeOnItemTouchListener)
    topScrollerGestureLockListener = createCategoryGestureLockListener()
    topScrollerGestureLockListener?.let(binding.topScroller::addOnItemTouchListener)

    topScrollerScrollListener?.let(binding.topScroller::removeOnScrollListener)
    topScrollerScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val firstVisiblePosition = (recyclerView.layoutManager as GridLayoutManager).findFirstVisibleItemPosition()
            if (firstVisiblePosition != RecyclerView.NO_POSITION) {
                (binding.topScroller.adapter as? AppsAdapter)?.getAppAt(firstVisiblePosition)?.let {
                    val key = viewModel.getSortKey(it.label.toString())
                    showAlphabetIndicator(key.toString())
                }
            }
        }
    }
    topScrollerScrollListener?.let(binding.topScroller::addOnScrollListener)
}

fun MainActivity.showAppActionMenu(app: AppInfo) {
    if (app.isSettingsShortcut) {
        startActivity(Intent(this, SettingsActivity::class.java))
        return
    }

    val text = SettingsText.resolve(this, preferences)
    val actions = arrayOf(
        if (app.isPinned) text.unpin else text.pin,
        text.hide,
        text.appInfo,
        text.uninstall
    )

    AlertDialog.Builder(this)
        .setTitle(app.label)
        .setItems(actions) { _, which ->
            when (which) {
                0 -> viewModel.togglePinApp(app)
                1 -> viewModel.hideApp(app)
                2 -> openAppInfo(app.packageName.toString())
                3 -> requestUninstall(app.packageName.toString())
            }
        }
        .show()
}

private fun MainActivity.openAppInfo(packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.parse("package:$packageName"))
    startActivity(intent)
}

private fun MainActivity.requestUninstall(packageName: String) {
    val intent = Intent(Intent.ACTION_DELETE)
        .setData(Uri.parse("package:$packageName"))
    startActivity(intent)
}

private fun MainActivity.createCategoryGestureLockListener(): RecyclerView.SimpleOnItemTouchListener {
    var downX = 0f
    var downY = 0f
    var gestureMode = GestureMode.UNDECIDED
    var categoryChanged = false
    var longPressTriggered = false
    var pendingLongPress: Runnable? = null
    val touchSlop = 28f

    fun clearPendingLongPress(rv: RecyclerView) {
        pendingLongPress?.let(rv::removeCallbacks)
        pendingLongPress = null
    }

    fun appUnderTouch(rv: RecyclerView, event: MotionEvent): AppInfo? {
        val child = rv.findChildViewUnder(event.x, event.y) ?: return null
        val position = rv.getChildAdapterPosition(child)
        if (position == RecyclerView.NO_POSITION) return null
        return (rv.adapter as? AppsAdapter)?.getAppAt(position)
    }

    return object : RecyclerView.SimpleOnItemTouchListener() {
        override fun onInterceptTouchEvent(rv: RecyclerView, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    gestureMode = GestureMode.UNDECIDED
                    categoryChanged = false
                    longPressTriggered = false
                    clearPendingLongPress(rv)
                    appUnderTouch(rv, event)?.let { app ->
                        pendingLongPress = Runnable {
                            pendingLongPress = null
                            longPressTriggered = true
                            gestureMode = GestureMode.LONG_PRESS_MENU
                            rv.parent?.requestDisallowInterceptTouchEvent(true)
                            window.decorView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            showAppActionMenu(app)
                        }
                        rv.postDelayed(
                            pendingLongPress,
                            ViewConfiguration.getLongPressTimeout().toLong()
                        )
                    }
                    return false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - downX
                    val dy = event.y - downY
                    if (longPressTriggered) return true
                    if (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop) {
                        clearPendingLongPress(rv)
                    }
                    if (gestureMode == GestureMode.UNDECIDED && (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop)) {
                        gestureMode = if (kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.2f) {
                            GestureMode.HORIZONTAL_CATEGORY
                        } else {
                            GestureMode.SCROLLER
                        }
                        if (gestureMode == GestureMode.HORIZONTAL_CATEGORY) {
                            rv.parent?.requestDisallowInterceptTouchEvent(true)
                        }
                    }
                    return gestureMode == GestureMode.HORIZONTAL_CATEGORY
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    clearPendingLongPress(rv)
                    if (longPressTriggered) {
                        gestureMode = GestureMode.UNDECIDED
                        categoryChanged = false
                        longPressTriggered = false
                        return true
                    }
                    val shouldIntercept = gestureMode == GestureMode.HORIZONTAL_CATEGORY
                    if (shouldIntercept && event.actionMasked == MotionEvent.ACTION_UP) {
                        changeCategoryFromSwipe(event.x - downX)
                    }
                    gestureMode = GestureMode.UNDECIDED
                    categoryChanged = false
                    return shouldIntercept
                }
            }
            return false
        }

        override fun onTouchEvent(rv: RecyclerView, event: MotionEvent) {
            if (gestureMode == GestureMode.LONG_PRESS_MENU) {
                if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                    clearPendingLongPress(rv)
                    gestureMode = GestureMode.UNDECIDED
                    longPressTriggered = false
                }
                return
            }
            if (gestureMode != GestureMode.HORIZONTAL_CATEGORY) return
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    if (!categoryChanged) {
                        val dx = event.x - downX
                        if (kotlin.math.abs(dx) > 80f) {
                            changeCategoryFromSwipe(dx)
                            categoryChanged = true
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    clearPendingLongPress(rv)
                    gestureMode = GestureMode.UNDECIDED
                    categoryChanged = false
                }
            }
        }

        private fun changeCategoryFromSwipe(dx: Float) {
            if (kotlin.math.abs(dx) <= 80f) return
            viewModel.selectAdjacentCategory(if (dx < 0) 1 else -1)
        }
    }
}

private enum class GestureMode {
    UNDECIDED,
    SCROLLER,
    HORIZONTAL_CATEGORY,
    LONG_PRESS_MENU
}

fun MainActivity.setupMediaControls() {
    binding.holdSwitch.onCheckedChange = { enabled ->
        binding.holdSwitch.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        preferences.isMinimalModeEnabled = enabled
        viewModel.refreshSettings()
    }

    val controls = listOf(
        binding.playPauseButton to null,
        binding.nextButton to binding.nextLabel,
        binding.prevButton to binding.prevLabel,
        binding.rewindButton to binding.rewindLabel,
        binding.ffButton to binding.ffLabel,
        binding.volUpButton to binding.volUpLabel,
        binding.volDownButton to binding.volDownLabel,
    )

    controls.forEach { (button, label) ->
        button.setOnTouchListener(createRetroButtonListener(label) {
            requestMediaUpdate()
            when (it.id) {
                R.id.play_pause_button -> handlePlayPausePressed()
                R.id.next_button -> MediaNotificationListenerService.mediaController?.transportControls?.skipToNext()
                R.id.prev_button -> MediaNotificationListenerService.mediaController?.transportControls?.skipToPrevious()
                R.id.rewind_button -> MediaNotificationListenerService.mediaController?.transportControls?.rewind()
                R.id.ff_button -> MediaNotificationListenerService.mediaController?.transportControls?.fastForward()
                R.id.vol_up_button -> audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
                R.id.vol_down_button -> audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
            }
        })
    }
}

private fun MainActivity.handlePlayPausePressed() {
    val state = viewModel.uiState.value
    if (state.isPlaying) {
        MediaNotificationListenerService.mediaController?.transportControls?.pause()
        return
    }

    if (state.songTitle == "READY" || MediaNotificationListenerService.mediaController == null) {
        preferences.defaultMusicPackage?.let { packageName ->
            packageManager.getLaunchIntentForPackage(packageName)?.let {
                startActivity(it)
                return
            }
        }
    }

    MediaNotificationListenerService.mediaController?.transportControls?.play()
}

fun MainActivity.createRetroButtonListener(label: View? = null, onClickAction: (View) -> Unit): View.OnTouchListener {
    return View.OnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                animateControlPress(v, label, pressed = true)
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                animateControlPress(v, label, pressed = false)
                if (event.action == MotionEvent.ACTION_UP) onClickAction.invoke(v)
            }
        }
        true
    }
}

private fun animateControlPress(button: View, label: View?, pressed: Boolean) {
    val scale = if (pressed) 0.92f else 1f
    val labelScale = if (pressed) 0.9f else 1f
    val labelTranslation = if (pressed) 5f else 0f
    button.animate()
        .scaleX(scale)
        .scaleY(scale)
        .translationY(if (pressed) 3f else 0f)
        .setDuration(100)
        .start()
    label?.animate()
        ?.scaleX(labelScale)
        ?.scaleY(labelScale)
        ?.translationY(labelTranslation)
        ?.alpha(if (pressed) 0.72f else 1f)
        ?.setDuration(100)
        ?.start()
}

fun MainActivity.hideSystemBars() {
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
}

fun MainActivity.requestMediaUpdate() {
    val intent = Intent(this, MediaNotificationListenerService::class.java)
    intent.action = MediaNotificationListenerService.ACTION_REQUEST_MEDIA_UPDATE
    startService(intent)
}

fun MainActivity.setupSearch() {
    binding.searchEditText?.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            viewModel.searchApps(s.toString())
        }
        override fun afterTextChanged(s: Editable?) {}
    })

    binding.searchEditText?.setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            hideSearch()
            return@setOnEditorActionListener true
        }
        return@setOnEditorActionListener false
    }
}

fun MainActivity.showSearch() {
    binding.searchEditText?.visibility = View.VISIBLE
    binding.searchEditText?.requestFocus()
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(binding.searchEditText, InputMethodManager.SHOW_IMPLICIT)
}

fun MainActivity.hideSearch() {
    binding.searchEditText?.let { searchView ->
        if (searchView.visibility == View.VISIBLE) {
            searchView.visibility = View.GONE
            searchView.text.clear()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(searchView.windowToken, 0)
            binding.root.requestFocus()
        }
    }
}
