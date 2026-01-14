package com.example.mp3launcher

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioManager
import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
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
        val params = LinearLayout.LayoutParams(10, 20)
        params.setMargins(2, 0, 2, 0)
        bar.layoutParams = params
        bar.setBackgroundColor(ContextCompat.getColor(this, R.color.lcd_screen_text_color))
        if (i >= activeCount) {
            bar.alpha = 0.2f
        }
        container.addView(bar)
    }
}

fun MainActivity.showAlphabetIndicator(letter: String) {
    binding.alphabetIndicator.text = letter
    binding.alphabetIndicator.alpha = 1f
    binding.alphabetIndicator.handler?.removeCallbacksAndMessages(null) // Cancel previous fade out
    binding.alphabetIndicator.postDelayed({
        binding.alphabetIndicator.animate().alpha(0f).setDuration(500).start()
    }, 1000) // Show for 1 second before fading
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
            } else {
                hideSearch()
                val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName.toString())
                startActivity(launchIntent)
            }
        },
        onItemLongClicked = { app ->
            window.decorView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            viewModel.togglePinApp(app)
        }
    )
    binding.topScroller.adapter = appsAdapter

    binding.topScroller.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
    })
}

fun MainActivity.setupMediaControls() {
    val buttons = listOf(
        binding.playPauseButton,
        binding.nextButton,
        binding.prevButton,
        binding.rewindButton,
        binding.ffButton,
        binding.volUpButton,
        binding.volDownButton,
    )

    buttons.forEach { button ->
        button.setOnTouchListener(createRetroButtonListener {
            requestMediaUpdate()
            when (it.id) {
                R.id.play_pause_button -> if (viewModel.uiState.value.isPlaying) MediaNotificationListenerService.mediaController?.transportControls?.pause() else MediaNotificationListenerService.mediaController?.transportControls?.play()
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

fun MainActivity.createRetroButtonListener(onClickAction: (View) -> Unit): View.OnTouchListener {
    return View.OnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(100).start()
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                if (event.action == MotionEvent.ACTION_UP) onClickAction.invoke(v)
            }
        }
        true
    }
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
