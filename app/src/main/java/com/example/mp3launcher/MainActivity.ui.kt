package com.example.mp3launcher

import android.animation.ObjectAnimator
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
    }, 1800)
}

fun MainActivity.showCategoryIndicator(category: AppCategory) {
    if (binding.searchEditText?.visibility == View.VISIBLE) return
    categoryIndicatorVisibleUntil = System.currentTimeMillis() + 1800L
    val label = localizedCategoryIndicator(category)
    showAlphabetIndicator(label)
}

private fun MainActivity.localizedCategoryIndicator(category: AppCategory): String {
    preferences.customCategoryLabel(category)?.let { return it }
    val isChinese = SettingsText.resolve(this, preferences) === SettingsText.ZH_CN
    return if (isChinese) {
        when (category) {
            AppCategory.RECENT,
            AppCategory.ALL -> category.label
            AppCategory.MUSIC -> "音乐"
            AppCategory.SOCIAL -> "社交"
            AppCategory.TOOLS -> "工具"
            AppCategory.GAMES -> "游戏"
            else -> category.label
        }
    } else {
        category.label
    }
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
    val bootView = PixelCassetteBootView(this).apply {
        loadingText = "LOADING..."
        themePreset = viewModel.uiState.value.themePreset
        alpha = 0f
    }
    val tapeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 900L
        interpolator = LinearInterpolator()
        addUpdateListener { animator ->
            bootView.progress = animator.animatedValue as Float
        }
    }
    binding.root.addView(
        bootView,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT
    )
    tapeAnimator.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            bootView.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction {
                    if (bootView.parent != null) {
                        binding.root.removeView(bootView)
                    }
                }
                .start()
        }
    })
    bootView.animate().alpha(1f).setDuration(80).start()
    tapeAnimator.start()
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
        GridLayoutManager(this, 1, RecyclerView.VERTICAL, false)
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

    topScrollerScrollListener?.let(binding.topScroller::removeOnScrollListener)
    topScrollerScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val firstVisiblePosition = (recyclerView.layoutManager as GridLayoutManager).findFirstVisibleItemPosition()
            if (firstVisiblePosition != RecyclerView.NO_POSITION) {
                (binding.topScroller.adapter as? AppsAdapter)?.getAppAt(firstVisiblePosition)?.let {
                    if (System.currentTimeMillis() < categoryIndicatorVisibleUntil) return
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

fun MainActivity.setupMediaControls() {
    setupLcdSwipeTrackControl()

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
        val onLongClickAction: ((View) -> Boolean)? = if (button.id == R.id.play_pause_button) {
            { openDefaultMusicApp() }
        } else {
            null
        }
        button.setOnTouchListener(
            createRetroButtonListener(
                label = label,
                onLongClickAction = onLongClickAction
            ) {
                requestMediaUpdate()
                when (it.id) {
                    R.id.play_pause_button -> handlePlayPausePressed()
                    R.id.next_button -> MediaCommandDispatcher.next()
                    R.id.prev_button -> MediaCommandDispatcher.previous()
                    R.id.rewind_button -> MediaNotificationListenerService.mediaController?.transportControls?.rewind()
                    R.id.ff_button -> MediaNotificationListenerService.mediaController?.transportControls?.fastForward()
                    R.id.vol_up_button -> audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
                    R.id.vol_down_button -> audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
                }
            }
        )
    }
}

private fun MainActivity.setupLcdSwipeTrackControl() {
    val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
    val swipeThreshold = (72f * resources.displayMetrics.density).toInt()
    binding.lcdScreen.setOnTouchListener { view, event ->
        if (!viewModel.uiState.value.isLcdSwipeTrackEnabled) {
            return@setOnTouchListener false
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lcdSwipeDownX = event.x
                lcdSwipeDownY = event.y
                lcdSwipeTriggered = false
                view.parent?.requestDisallowInterceptTouchEvent(true)
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lcdSwipeDownX
                val dy = event.y - lcdSwipeDownY
                if (!lcdSwipeTriggered && kotlin.math.abs(dx) > touchSlop && kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.3f) {
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                }
                if (!lcdSwipeTriggered && kotlin.math.abs(dx) >= swipeThreshold && kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.3f) {
                    lcdSwipeTriggered = true
                    handleLcdTrackSwipe(if (dx < 0) 1 else -1)
                }
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                view.parent?.requestDisallowInterceptTouchEvent(false)
                lcdSwipeTriggered = false
                true
            }
            else -> true
        }
    }
}

private fun MainActivity.handleLcdTrackSwipe(direction: Int) {
    requestMediaUpdate()
    window.decorView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    animateLcdTrackSwipe(direction)
    if (direction > 0) {
        MediaCommandDispatcher.next()
    } else {
        MediaCommandDispatcher.previous()
    }
}

private fun MainActivity.animateLcdTrackSwipe(direction: Int) {
    val offset = direction * -18f * resources.displayMetrics.density
    val contentViews = listOf(
        binding.albumArt,
        binding.statusBars,
        binding.progressTextCurrent,
        binding.progressTextDuration,
        binding.progressBar
    )
    contentViews.forEach { view ->
        view.animate().cancel()
        view.animate()
            .translationX(offset)
            .alpha(0.58f)
            .setDuration(90)
            .withEndAction {
                view.translationX = -offset * 0.42f
                view.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }
}

private fun MainActivity.openDefaultMusicApp(): Boolean {
    val packageName = preferences.defaultMusicPackage ?: return false
    val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
    window.decorView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    startActivity(launchIntent)
    return true
}

private fun MainActivity.handlePlayPausePressed() {
    val state = viewModel.uiState.value
    if (state.isPlaying) {
        MediaCommandDispatcher.pause()
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

    MediaCommandDispatcher.play()
}

fun MainActivity.createRetroButtonListener(
    label: View? = null,
    onLongClickAction: ((View) -> Boolean)? = null,
    onClickAction: (View) -> Unit
): View.OnTouchListener {
    var pendingLongPress: Runnable? = null
    var longPressTriggered = false
    var downX = 0f
    var downY = 0f
    val touchSlop = ViewConfiguration.get(this).scaledTouchSlop

    fun clearPendingLongPress(view: View) {
        pendingLongPress?.let(view::removeCallbacks)
        pendingLongPress = null
    }

    return View.OnTouchListener { v, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                longPressTriggered = false
                animateControlPress(v, label, pressed = true)
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                if (onLongClickAction != null) {
                    clearPendingLongPress(v)
                    pendingLongPress = Runnable {
                        pendingLongPress = null
                        longPressTriggered = onLongClickAction.invoke(v)
                    }
                    v.postDelayed(pendingLongPress, ViewConfiguration.getLongPressTimeout().toLong())
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (
                    kotlin.math.abs(event.x - downX) > touchSlop ||
                    kotlin.math.abs(event.y - downY) > touchSlop
                ) {
                    clearPendingLongPress(v)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                clearPendingLongPress(v)
                animateControlPress(v, label, pressed = false)
                if (event.actionMasked == MotionEvent.ACTION_UP && !longPressTriggered) {
                    onClickAction.invoke(v)
                }
                longPressTriggered = false
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
