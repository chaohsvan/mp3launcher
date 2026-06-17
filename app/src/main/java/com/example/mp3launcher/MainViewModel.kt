package com.example.mp3launcher

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
    val selectedCategory: AppCategory = AppCategory.ALL,
    val volumeKeyMode: VolumeKeyMode = VolumeKeyMode.TRACK_CONTROL,
    val themePreset: ThemePreset = ThemePreset.SILVER_DISCMAN,
    val isMinimalModeEnabled: Boolean = false,
    val isBootAnimationEnabled: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private var allApps: List<AppInfo> = emptyList()
    private var searchQuery: String = ""
    private val preferences = LauncherPreferences(application)

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

        syncSettingsState()
        loadApplications()
    }

    fun togglePinApp(appInfo: AppInfo) {
        if (appInfo.isSettingsShortcut) return
        preferences.togglePinned(appInfo.packageName.toString())
        loadApplications()
    }

    fun hideApp(appInfo: AppInfo) {
        if (appInfo.isSettingsShortcut) return
        preferences.hideApp(appInfo.packageName.toString())
        loadApplications()
    }

    fun recordAppLaunch(appInfo: AppInfo) {
        preferences.recordLaunch(appInfo.packageName.toString())
        loadApplications()
    }

    fun selectCategory(category: AppCategory) {
        preferences.selectedCategory = category
        applyAppFilters()
    }

    fun selectAdjacentCategory(direction: Int) {
        val categories = AppCategory.entries
        val currentIndex = categories.indexOf(preferences.selectedCategory).coerceAtLeast(0)
        val nextIndex = (currentIndex + direction).coerceIn(0, categories.lastIndex)
        selectCategory(categories[nextIndex])
    }

    fun refreshSettings() {
        syncSettingsState()
        applyAppFilters()
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
                if (appPackageName in preferences.hiddenApps) return@mapNotNull null

                val label = resolveInfo.loadLabel(pm)

                AppInfo(
                    label = label,
                    packageName = appPackageName,
                    icon = resolveInfo.loadIcon(pm),
                    isPinned = preferences.isPinned(appPackageName),
                    category = preferences.categoryOverrideFor(appPackageName)
                        ?: AppCategoryResolver.resolve(label, appPackageName)
                )
            }
            val settingsApp = AppInfo(
                label = "SETTINGS",
                packageName = AppInfo.SETTINGS_PACKAGE_NAME,
                icon = requireNotNull(getApplication<Application>().getDrawable(R.drawable.ic_settings)),
                isPinned = true,
                category = AppCategory.TOOLS,
                isSettingsShortcut = true
            )

            val collator = Collator.getInstance()
            val sortedApps = appInfos.sortedWith(
                compareByDescending<AppInfo> { it.isPinned }
                    .thenBy { getSortKey(it.label.toString()) }
                    .thenBy(collator) { it.label.toString() }
            )

            allApps = listOf(settingsApp) + sortedApps

            withContext(Dispatchers.Main) {
                applyAppFilters()
            }
        }
    }

    fun searchApps(query: String) {
        searchQuery = query
        applyAppFilters()
    }

    private fun applyAppFilters() {
        viewModelScope.launch(Dispatchers.Default) {
            val selectedCategory = preferences.selectedCategory
            val recentPackages = preferences.recentApps
            val appsForCategory = when (selectedCategory) {
                AppCategory.ALL -> allApps
                AppCategory.RECENT -> recentPackages.mapNotNull { packageName ->
                    allApps.firstOrNull { it.packageName == packageName }
                }
                else -> allApps.filter { it.isSettingsShortcut || it.category == selectedCategory }
            }
            val filteredApps = if (searchQuery.isBlank()) {
                appsForCategory
            } else {
                appsForCategory.filter {
                    it.label.toString().contains(searchQuery, ignoreCase = true) ||
                        it.packageName.toString().contains(searchQuery, ignoreCase = true)
                }
            }
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    apps = filteredApps,
                    selectedCategory = selectedCategory
                )
            }
        }
    }

    private fun syncSettingsState() {
        _uiState.value = _uiState.value.copy(
            selectedCategory = preferences.selectedCategory,
            volumeKeyMode = preferences.volumeKeyMode,
            themePreset = preferences.themePreset,
            isMinimalModeEnabled = preferences.isMinimalModeEnabled,
            isBootAnimationEnabled = preferences.isBootAnimationEnabled
        )
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
                    duration = duration
                )
            }
            MediaNotificationListenerService.ACTION_MEDIA_CLEAR -> {
                _uiState.value = UiState(
                    apps = _uiState.value.apps,
                    selectedCategory = preferences.selectedCategory,
                    volumeKeyMode = preferences.volumeKeyMode,
                    themePreset = preferences.themePreset,
                    isMinimalModeEnabled = preferences.isMinimalModeEnabled,
                    isBootAnimationEnabled = preferences.isBootAnimationEnabled
                )
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

                _uiState.value = _uiState.value.copy(
                    isPlaying = isPlaying,
                    progress = position
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
