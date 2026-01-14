package com.example.mp3launcher

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.icu.text.Transliterator
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator

data class UiState(
    val songTitle: String = "READY",
    val artistName: String = "",
    val albumArt: Bitmap? = null,
    val isPlaying: Boolean = false,
    val progress: Long = 0,
    val duration: Long = 0,
    val apps: List<AppInfo> = emptyList(),
    val visualizerRelaunchKey: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private var allApps: List<AppInfo> = emptyList() // Store the full list of apps

    // Added for pinning
    private val sharedPrefs: SharedPreferences = application.getSharedPreferences("mp3launcher_prefs", Context.MODE_PRIVATE)
    private val pinnedApps = mutableSetOf<String>()

    private var lastPositionUpdateTime: Long = 0
    private var lastReceivedPosition: Long = 0
    private var currentPlaybackSpeed: Float = 1.0f

    private val transliterator by lazy {
        Transliterator.getInstance("Han-Latin")
    }

    private val progressHandler = Handler(Looper.getMainLooper())
    private val updateProgressAction = object : Runnable {
        override fun run() {
            val elapsed = (System.currentTimeMillis() - lastPositionUpdateTime) * currentPlaybackSpeed
            val currentPosition = lastReceivedPosition + elapsed.toLong()
            _uiState.value = _uiState.value.copy(progress = currentPosition)
            progressHandler.postDelayed(this, 500)
        }
    }

    private val mediaUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            processMediaIntent(intent)
        }
    }

    init {
        val intentFilter = IntentFilter().apply {
            addAction(MediaNotificationListenerService.ACTION_MEDIA_UPDATE)
            addAction(MediaNotificationListenerService.ACTION_MEDIA_CLEAR)
            addAction(MediaNotificationListenerService.ACTION_PLAYBACK_STATE_UPDATE)
        }
        LocalBroadcastManager.getInstance(getApplication()).registerReceiver(mediaUpdateReceiver, intentFilter)

        // Load pinned apps before loading all applications
        loadPinnedApps()
        loadApplications()
    }

    // Added for pinning
    private fun loadPinnedApps() {
        pinnedApps.clear()
        pinnedApps.addAll(sharedPrefs.getStringSet("pinned_apps", emptySet()) ?: emptySet())
    }

    // Added for pinning
    private fun savePinnedApps() {
        sharedPrefs.edit().putStringSet("pinned_apps", pinnedApps).apply()
    }

    // Added for pinning
    fun togglePinApp(appInfo: AppInfo) {
        val packageName = appInfo.packageName.toString()
        if (pinnedApps.contains(packageName)) {
            pinnedApps.remove(packageName)
        } else {
            pinnedApps.add(packageName)
        }
        savePinnedApps()
        // Reload and resort apps
        loadApplications()
    }

    private fun loadApplications() {
        viewModelScope.launch(Dispatchers.IO) {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
            val pm = getApplication<Application>().packageManager
            val pkgAppsList: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }
            val appInfos = pkgAppsList.mapNotNull { resolveInfo ->
                val appPackageName = resolveInfo.activityInfo.packageName
                if (appPackageName == getApplication<Application>().packageName) return@mapNotNull null

                AppInfo(
                    label = resolveInfo.loadLabel(pm),
                    packageName = appPackageName,
                    icon = resolveInfo.loadIcon(pm),
                    isPinned = appPackageName in pinnedApps
                )
            }

            val collator = Collator.getInstance()
            // Modified sorting
            val sortedApps = appInfos.sortedWith(
                compareByDescending<AppInfo> { it.isPinned } // Use the isPinned property
                    .thenBy { getSortKey(it.label.toString()) }
                    .thenBy(collator) { it.label.toString() }
            )

            allApps = sortedApps // Store the full list

            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(apps = sortedApps)
            }
        }
    }

    fun searchApps(query: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val filteredApps = if (query.isBlank()) {
                allApps
            } else {
                allApps.filter {
                    it.label.toString().contains(query, ignoreCase = true)
                }
            }
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(apps = filteredApps)
            }
        }
    }

    fun getSortKey(label: String): Char {
        if (label.isBlank()) return '#'
        val firstChar = label[0]
        val pinyin = if (firstChar.toString().matches(Regex("\\p{IsHan}"))) {
            transliterator.transliterate(firstChar.toString())
        } else {
            label
        }
        return pinyin.firstOrNull()?.uppercaseChar()?.let {
            if (it in 'A'..'Z') it else '#'
        } ?: '#'
    }

    fun processMediaIntent(intent: Intent) {
        when (intent.action) {
            MediaNotificationListenerService.ACTION_MEDIA_UPDATE -> {
                val title = intent.getStringExtra(MediaNotificationListenerService.EXTRA_TITLE)
                val artist = intent.getStringExtra(MediaNotificationListenerService.EXTRA_ARTIST)
                val albumArt = intent.getParcelableExtraCompat(MediaNotificationListenerService.EXTRA_ALBUM_ART, Bitmap::class.java)
                val duration = intent.getLongExtra(MediaNotificationListenerService.EXTRA_DURATION, 0)

                _uiState.value = _uiState.value.copy(
                    songTitle = title ?: "Unknown Title",
                    artistName = artist ?: "Unknown Artist",
                    albumArt = albumArt,
                    duration = duration,
                    visualizerRelaunchKey = _uiState.value.visualizerRelaunchKey + 1
                )
            }
            MediaNotificationListenerService.ACTION_MEDIA_CLEAR -> {
                _uiState.value = UiState(apps = _uiState.value.apps) // Reset but keep the app list
                lastReceivedPosition = 0
                progressHandler.removeCallbacks(updateProgressAction)
            }
            MediaNotificationListenerService.ACTION_PLAYBACK_STATE_UPDATE -> {
                val state = intent.getIntExtra(MediaNotificationListenerService.EXTRA_PLAYBACK_STATE, PlaybackState.STATE_NONE)
                val position = intent.getLongExtra(MediaNotificationListenerService.EXTRA_POSITION, 0)
                currentPlaybackSpeed = intent.getFloatExtra(MediaNotificationListenerService.EXTRA_PLAYBACK_SPEED, 1.0f)
                lastPositionUpdateTime = System.currentTimeMillis()
                lastReceivedPosition = position

                val isPlaying = state == PlaybackState.STATE_PLAYING
                val wasPlaying = _uiState.value.isPlaying

                _uiState.value = _uiState.value.copy(
                    isPlaying = isPlaying,
                    progress = position,
                    visualizerRelaunchKey = if (isPlaying && !wasPlaying) _uiState.value.visualizerRelaunchKey + 1 else _uiState.value.visualizerRelaunchKey
                )

                progressHandler.removeCallbacks(updateProgressAction)
                if (isPlaying) {
                    progressHandler.post(updateProgressAction)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        LocalBroadcastManager.getInstance(getApplication()).unregisterReceiver(mediaUpdateReceiver)
        progressHandler.removeCallbacks(updateProgressAction)
    }
}

inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(key: String, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, clazz)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key) as? T
    }
}
