package com.example.mp3launcher

import android.content.Context
import android.graphics.Color

enum class AppCategory(val label: String) {
    RECENT("RECENT"),
    ALL("ALL"),
    MUSIC("MUSIC"),
    SOCIAL("SOCIAL"),
    TOOLS("TOOLS"),
    GAMES("GAMES"),
    CUSTOM_1("CUSTOM 1"),
    CUSTOM_2("CUSTOM 2"),
    CUSTOM_3("CUSTOM 3"),
    CUSTOM_4("CUSTOM 4"),
    CUSTOM_5("CUSTOM 5");

    val isCustom: Boolean
        get() = this in customSlots

    companion object {
        val fixedAssignableGroups = emptyList<AppCategory>()
        val baseNavigationGroups = listOf(ALL, RECENT)
        val customSlots = listOf(CUSTOM_1, CUSTOM_2, CUSTOM_3, CUSTOM_4, CUSTOM_5)

        fun fromName(value: String?): AppCategory {
            return findByName(value) ?: ALL
        }

        fun findByName(value: String?): AppCategory? {
            return entries.firstOrNull { it.name == value }
        }
    }
}

enum class VolumeKeyMode(val label: String) {
    TRACK_CONTROL("Track + play"),
    MEDIA_VOLUME("Volume + play"),
    SYSTEM_DEFAULT("System default");

    companion object {
        fun fromName(value: String?): VolumeKeyMode {
            return entries.firstOrNull { it.name == value } ?: TRACK_CONTROL
        }
    }
}

enum class SettingsLanguage {
    SYSTEM,
    ENGLISH,
    SIMPLIFIED_CHINESE;

    companion object {
        fun fromName(value: String?): SettingsLanguage {
            return entries.firstOrNull { it.name == value } ?: SYSTEM
        }
    }
}

enum class ThemePreset(
    val label: String,
    val rootBackground: Int,
    val deviceBackground: Int,
    val deviceStroke: Int,
    val controlBackground: Int,
    val controlStroke: Int,
    val buttonBackground: Int,
    val buttonPressed: Int,
    val playButtonBackground: Int,
    val buttonIconTint: Int,
    val labelText: Int,
    val lcdBackground: Int,
    val lcdText: Int,
    val lcdTextFaded: Int,
    val accent: Int,
    val indicatorHousing: Int
) {
    BLACK_GREEN(
        label = "Black Green LCD",
        rootBackground = Color.rgb(0, 0, 0),
        deviceBackground = Color.rgb(18, 20, 18),
        deviceStroke = Color.rgb(42, 50, 42),
        controlBackground = Color.rgb(14, 16, 14),
        controlStroke = Color.rgb(48, 64, 48),
        buttonBackground = Color.rgb(32, 40, 32),
        buttonPressed = Color.rgb(42, 58, 42),
        playButtonBackground = Color.rgb(70, 118, 34),
        buttonIconTint = Color.rgb(210, 234, 176),
        labelText = Color.rgb(132, 168, 112),
        lcdBackground = Color.rgb(151, 188, 82),
        lcdText = Color.rgb(27, 48, 15),
        lcdTextFaded = Color.argb(150, 27, 48, 15),
        accent = Color.rgb(122, 178, 54),
        indicatorHousing = Color.rgb(6, 8, 6)
    ),
    SILVER_DISCMAN(
        label = "Silver Discman",
        rootBackground = Color.rgb(5, 5, 7),
        deviceBackground = Color.rgb(185, 190, 193),
        deviceStroke = Color.rgb(95, 100, 104),
        controlBackground = Color.rgb(36, 38, 42),
        controlStroke = Color.rgb(126, 130, 136),
        buttonBackground = Color.rgb(63, 63, 70),
        buttonPressed = Color.rgb(82, 82, 91),
        playButtonBackground = Color.rgb(159, 58, 0),
        buttonIconTint = Color.rgb(238, 238, 238),
        labelText = Color.rgb(189, 189, 189),
        lcdBackground = Color.rgb(248, 156, 47),
        lcdText = Color.rgb(62, 27, 4),
        lcdTextFaded = Color.argb(150, 62, 27, 4),
        accent = Color.rgb(249, 115, 22),
        indicatorHousing = Color.rgb(30, 30, 34)
    ),
    TRANSPARENT_IPOD(
        label = "Transparent iPod",
        rootBackground = Color.rgb(8, 12, 16),
        deviceBackground = Color.argb(210, 224, 232, 238),
        deviceStroke = Color.argb(220, 122, 150, 165),
        controlBackground = Color.rgb(235, 239, 242),
        controlStroke = Color.rgb(164, 184, 194),
        buttonBackground = Color.rgb(214, 224, 230),
        buttonPressed = Color.rgb(190, 204, 214),
        playButtonBackground = Color.rgb(58, 130, 180),
        buttonIconTint = Color.rgb(32, 52, 58),
        labelText = Color.rgb(52, 75, 84),
        lcdBackground = Color.rgb(222, 240, 238),
        lcdText = Color.rgb(32, 52, 58),
        lcdTextFaded = Color.argb(145, 32, 52, 58),
        accent = Color.rgb(58, 130, 180),
        indicatorHousing = Color.rgb(198, 210, 218)
    ),
    GAME_BOY(
        label = "Game Boy",
        rootBackground = Color.rgb(8, 10, 8),
        deviceBackground = Color.rgb(142, 148, 134),
        deviceStroke = Color.rgb(66, 70, 68),
        controlBackground = Color.rgb(42, 44, 42),
        controlStroke = Color.rgb(84, 82, 86),
        buttonBackground = Color.rgb(58, 60, 62),
        buttonPressed = Color.rgb(74, 76, 78),
        playButtonBackground = Color.rgb(139, 56, 134),
        buttonIconTint = Color.rgb(232, 232, 226),
        labelText = Color.rgb(74, 56, 86),
        lcdBackground = Color.rgb(155, 188, 15),
        lcdText = Color.rgb(15, 56, 15),
        lcdTextFaded = Color.argb(150, 15, 56, 15),
        accent = Color.rgb(139, 56, 134),
        indicatorHousing = Color.rgb(54, 58, 54)
    ),
    WALKMAN(
        label = "Walkman",
        rootBackground = Color.rgb(4, 6, 8),
        deviceBackground = Color.rgb(24, 28, 34),
        deviceStroke = Color.rgb(56, 64, 78),
        controlBackground = Color.rgb(13, 16, 22),
        controlStroke = Color.rgb(74, 84, 102),
        buttonBackground = Color.rgb(42, 48, 60),
        buttonPressed = Color.rgb(54, 64, 78),
        playButtonBackground = Color.rgb(255, 205, 64),
        buttonIconTint = Color.rgb(232, 240, 244),
        labelText = Color.rgb(150, 164, 178),
        lcdBackground = Color.rgb(98, 186, 206),
        lcdText = Color.rgb(5, 32, 42),
        lcdTextFaded = Color.argb(145, 5, 32, 42),
        accent = Color.rgb(255, 205, 64),
        indicatorHousing = Color.rgb(8, 10, 14)
    ),
    RED_CASSETTE(
        label = "Red Cassette",
        rootBackground = Color.rgb(9, 6, 8),
        deviceBackground = Color.rgb(92, 24, 32),
        deviceStroke = Color.rgb(38, 12, 18),
        controlBackground = Color.rgb(24, 22, 26),
        controlStroke = Color.rgb(112, 52, 58),
        buttonBackground = Color.rgb(58, 56, 64),
        buttonPressed = Color.rgb(82, 72, 78),
        playButtonBackground = Color.rgb(198, 52, 48),
        buttonIconTint = Color.rgb(245, 228, 206),
        labelText = Color.rgb(232, 196, 184),
        lcdBackground = Color.rgb(234, 201, 142),
        lcdText = Color.rgb(54, 25, 26),
        lcdTextFaded = Color.argb(150, 54, 25, 26),
        accent = Color.rgb(224, 75, 58),
        indicatorHousing = Color.rgb(18, 12, 15)
    ),
    ARCTIC_MINIDISC(
        label = "Arctic MiniDisc",
        rootBackground = Color.rgb(6, 10, 14),
        deviceBackground = Color.rgb(216, 228, 235),
        deviceStroke = Color.rgb(94, 118, 132),
        controlBackground = Color.rgb(28, 42, 54),
        controlStroke = Color.rgb(86, 122, 146),
        buttonBackground = Color.rgb(76, 98, 116),
        buttonPressed = Color.rgb(100, 126, 144),
        playButtonBackground = Color.rgb(38, 145, 190),
        buttonIconTint = Color.rgb(236, 247, 250),
        labelText = Color.rgb(92, 128, 144),
        lcdBackground = Color.rgb(205, 241, 244),
        lcdText = Color.rgb(22, 56, 66),
        lcdTextFaded = Color.argb(145, 22, 56, 66),
        accent = Color.rgb(52, 176, 218),
        indicatorHousing = Color.rgb(174, 194, 204)
    ),
    MINT_CREAM(
        label = "Mint Cream",
        rootBackground = Color.rgb(7, 12, 11),
        deviceBackground = Color.rgb(165, 202, 188),
        deviceStroke = Color.rgb(70, 104, 96),
        controlBackground = Color.rgb(24, 42, 42),
        controlStroke = Color.rgb(82, 126, 118),
        buttonBackground = Color.rgb(52, 76, 78),
        buttonPressed = Color.rgb(68, 98, 98),
        playButtonBackground = Color.rgb(214, 89, 66),
        buttonIconTint = Color.rgb(240, 244, 226),
        labelText = Color.rgb(196, 226, 210),
        lcdBackground = Color.rgb(238, 223, 166),
        lcdText = Color.rgb(36, 61, 48),
        lcdTextFaded = Color.argb(150, 36, 61, 48),
        accent = Color.rgb(214, 89, 66),
        indicatorHousing = Color.rgb(12, 24, 24)
    ),
    MIDNIGHT_PURPLE(
        label = "Midnight Purple",
        rootBackground = Color.rgb(3, 3, 8),
        deviceBackground = Color.rgb(30, 25, 48),
        deviceStroke = Color.rgb(82, 70, 118),
        controlBackground = Color.rgb(13, 12, 22),
        controlStroke = Color.rgb(92, 72, 136),
        buttonBackground = Color.rgb(50, 42, 72),
        buttonPressed = Color.rgb(72, 58, 96),
        playButtonBackground = Color.rgb(180, 64, 154),
        buttonIconTint = Color.rgb(236, 230, 244),
        labelText = Color.rgb(170, 154, 205),
        lcdBackground = Color.rgb(132, 210, 190),
        lcdText = Color.rgb(20, 36, 44),
        lcdTextFaded = Color.argb(145, 20, 36, 44),
        accent = Color.rgb(202, 84, 174),
        indicatorHousing = Color.rgb(8, 7, 14)
    ),
    SUNSET_RADIO(
        label = "Sunset Radio",
        rootBackground = Color.rgb(8, 7, 4),
        deviceBackground = Color.rgb(213, 167, 76),
        deviceStroke = Color.rgb(86, 61, 28),
        controlBackground = Color.rgb(31, 39, 43),
        controlStroke = Color.rgb(96, 86, 62),
        buttonBackground = Color.rgb(62, 78, 80),
        buttonPressed = Color.rgb(82, 98, 98),
        playButtonBackground = Color.rgb(174, 63, 42),
        buttonIconTint = Color.rgb(248, 234, 202),
        labelText = Color.rgb(236, 210, 156),
        lcdBackground = Color.rgb(252, 185, 68),
        lcdText = Color.rgb(52, 30, 12),
        lcdTextFaded = Color.argb(150, 52, 30, 12),
        accent = Color.rgb(221, 81, 44),
        indicatorHousing = Color.rgb(21, 25, 26)
    );

    companion object {
        fun fromName(value: String?): ThemePreset {
            return entries.firstOrNull { it.name == value } ?: SILVER_DISCMAN
        }
    }
}

class LauncherPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var selectedCategory: AppCategory
        get() {
            val saved = AppCategory.fromName(prefs.getString(KEY_SELECTED_CATEGORY, AppCategory.RECENT.name))
            if (saved in AppCategory.baseNavigationGroups) return saved
            if (saved.isCustom && customCategoryLabel(saved) != null) return saved
            return AppCategory.RECENT
        }
        set(value) = prefs.edit().putString(KEY_SELECTED_CATEGORY, value.name).apply()

    var volumeKeyMode: VolumeKeyMode
        get() = VolumeKeyMode.fromName(prefs.getString(KEY_VOLUME_KEY_MODE, VolumeKeyMode.TRACK_CONTROL.name))
        set(value) = prefs.edit().putString(KEY_VOLUME_KEY_MODE, value.name).apply()

    var themePreset: ThemePreset
        get() = ThemePreset.fromName(prefs.getString(KEY_THEME_PRESET, ThemePreset.SILVER_DISCMAN.name))
        set(value) = prefs.edit().putString(KEY_THEME_PRESET, value.name).apply()

    var settingsLanguage: SettingsLanguage
        get() = SettingsLanguage.fromName(prefs.getString(KEY_SETTINGS_LANGUAGE, SettingsLanguage.SYSTEM.name))
        set(value) = prefs.edit().putString(KEY_SETTINGS_LANGUAGE, value.name).apply()

    var isMinimalModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_MINIMAL_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_MINIMAL_MODE, value).apply()

    var isBootAnimationEnabled: Boolean
        get() = prefs.getBoolean(KEY_BOOT_ANIMATION, true)
        set(value) = prefs.edit().putBoolean(KEY_BOOT_ANIMATION, value).apply()

    var isLcdSwipeTrackEnabled: Boolean
        get() = prefs.getBoolean(KEY_LCD_SWIPE_TRACK, false)
        set(value) = prefs.edit().putBoolean(KEY_LCD_SWIPE_TRACK, value).apply()

    var isGlobalVolumeKeyTrackEnabled: Boolean
        get() = prefs.getBoolean(KEY_GLOBAL_VOLUME_KEY_TRACK, false)
        set(value) = prefs.edit().putBoolean(KEY_GLOBAL_VOLUME_KEY_TRACK, value).apply()

    var defaultMusicPackage: String?
        get() = prefs.getString(KEY_DEFAULT_MUSIC_PACKAGE, null)
        set(value) = prefs.edit().putString(KEY_DEFAULT_MUSIC_PACKAGE, value).apply()

    var hasPromptedDefaultLauncher: Boolean
        get() = prefs.getBoolean(KEY_PROMPTED_DEFAULT_LAUNCHER, false)
        set(value) = prefs.edit().putBoolean(KEY_PROMPTED_DEFAULT_LAUNCHER, value).apply()

    val pinnedApps: Set<String>
        get() = prefs.stringSet(KEY_PINNED_APPS)

    val hiddenApps: Set<String>
        get() = prefs.stringSet(KEY_HIDDEN_APPS)

    val recentApps: List<String>
        get() = prefs.getString(KEY_RECENT_APPS, "")
            .orEmpty()
            .split(RECENT_SEPARATOR)
            .filter { it.isNotBlank() }

    val appCategoryOverrides: Map<String, AppCategory>
        get() {
            val activeCustomGroups = customCategoryLabels.keys
            return prefs.getString(KEY_CATEGORY_OVERRIDES, "")
                .orEmpty()
                .split(CATEGORY_PAIR_SEPARATOR)
                .filter { it.isNotBlank() }
                .mapNotNull { item ->
                    val parts = item.split(CATEGORY_VALUE_SEPARATOR, limit = 2)
                    val packageName = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val category = AppCategory.findByName(parts.getOrNull(1)) ?: return@mapNotNull null
                    if (category.isCustom && category !in activeCustomGroups) return@mapNotNull null
                    packageName to category
                }
                .toMap()
        }

    val customCategoryLabels: Map<AppCategory, String>
        get() = prefs.getString(KEY_CUSTOM_CATEGORY_LABELS, "")
            .orEmpty()
            .split(CUSTOM_CATEGORY_PAIR_SEPARATOR)
            .filter { it.isNotBlank() }
            .mapNotNull { item ->
                val parts = item.split(CUSTOM_CATEGORY_VALUE_SEPARATOR, limit = 2)
                val category = AppCategory.findByName(parts.getOrNull(0))?.takeIf { it.isCustom }
                    ?: return@mapNotNull null
                val label = decodeCustomCategoryLabel(parts.getOrNull(1).orEmpty()).trim()
                    .takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                category to label
            }
            .toMap()

    fun isPinned(packageName: String): Boolean = packageName in pinnedApps

    fun categoryOverrideFor(packageName: String): AppCategory? = appCategoryOverrides[packageName]

    fun activeCustomCategories(): List<AppCategory> {
        val labels = customCategoryLabels
        return AppCategory.customSlots.filter { it in labels }
    }

    fun assignableCategories(): List<AppCategory> = AppCategory.fixedAssignableGroups + activeCustomCategories()

    fun navigationCategories(): List<AppCategory> = AppCategory.baseNavigationGroups + activeCustomCategories()

    fun customCategoryLabel(category: AppCategory): String? {
        if (!category.isCustom) return null
        return customCategoryLabels[category]
    }

    fun createCustomCategory(label: String): AppCategory? {
        val trimmed = label.trim().takeIf { it.isNotBlank() } ?: return null
        val nextSlot = AppCategory.customSlots.firstOrNull { customCategoryLabel(it) == null } ?: return null
        setCustomCategoryLabel(nextSlot, trimmed)
        return nextSlot
    }

    fun setCustomCategoryLabel(category: AppCategory, label: String) {
        if (!category.isCustom) return
        val trimmed = label.trim()
        if (trimmed.isBlank()) return
        val next = customCategoryLabels.toMutableMap()
        next[category] = trimmed
        saveCustomCategoryLabels(next)
    }

    fun deleteCustomCategory(category: AppCategory) {
        if (!category.isCustom) return
        val nextLabels = customCategoryLabels.toMutableMap()
        nextLabels.remove(category)
        saveCustomCategoryLabels(nextLabels)

        val nextOverrides = appCategoryOverrides.toMutableMap()
        nextOverrides.entries.removeAll { it.value == category }
        saveCategoryOverrides(nextOverrides)

        if (selectedCategory == category) {
            selectedCategory = AppCategory.RECENT
        }
    }

    fun togglePinned(packageName: String) {
        val next = pinnedApps.toMutableSet()
        if (!next.add(packageName)) {
            next.remove(packageName)
        }
        prefs.edit().putStringSet(KEY_PINNED_APPS, next).apply()
    }

    fun hideApp(packageName: String) {
        val next = hiddenApps.toMutableSet()
        next.add(packageName)
        prefs.edit().putStringSet(KEY_HIDDEN_APPS, next).apply()
    }

    fun unhideApp(packageName: String) {
        val next = hiddenApps.toMutableSet()
        next.remove(packageName)
        prefs.edit().putStringSet(KEY_HIDDEN_APPS, next).apply()
    }

    fun unpinApp(packageName: String) {
        val next = pinnedApps.toMutableSet()
        next.remove(packageName)
        prefs.edit().putStringSet(KEY_PINNED_APPS, next).apply()
    }

    fun recordLaunch(packageName: String) {
        val next = buildList {
            add(packageName)
            addAll(recentApps.filterNot { it == packageName })
        }.take(MAX_RECENT_APPS)
        prefs.edit().putString(KEY_RECENT_APPS, next.joinToString(RECENT_SEPARATOR)).apply()
    }

    fun setAppCategoryOverride(packageName: String, category: AppCategory) {
        val next = appCategoryOverrides.toMutableMap()
        next[packageName] = category
        saveCategoryOverrides(next)
    }

    fun clearAppCategoryOverride(packageName: String) {
        val next = appCategoryOverrides.toMutableMap()
        next.remove(packageName)
        saveCategoryOverrides(next)
    }

    fun clearAppCategoryOverrides() {
        prefs.edit().remove(KEY_CATEGORY_OVERRIDES).apply()
    }

    private fun saveCategoryOverrides(overrides: Map<String, AppCategory>) {
        val encoded = overrides.entries.joinToString(CATEGORY_PAIR_SEPARATOR) {
            "${it.key}$CATEGORY_VALUE_SEPARATOR${it.value.name}"
        }
        prefs.edit().putString(KEY_CATEGORY_OVERRIDES, encoded).apply()
    }

    private fun saveCustomCategoryLabels(labels: Map<AppCategory, String>) {
        val encoded = labels.entries
            .filter { it.key.isCustom && it.value.isNotBlank() }
            .joinToString(CUSTOM_CATEGORY_PAIR_SEPARATOR) {
                "${it.key.name}$CUSTOM_CATEGORY_VALUE_SEPARATOR${encodeCustomCategoryLabel(it.value)}"
            }
        prefs.edit().putString(KEY_CUSTOM_CATEGORY_LABELS, encoded).apply()
    }

    private fun encodeCustomCategoryLabel(value: String): String {
        return java.net.URLEncoder.encode(value, Charsets.UTF_8.name())
    }

    private fun decodeCustomCategoryLabel(value: String): String {
        return runCatching {
            java.net.URLDecoder.decode(value, Charsets.UTF_8.name())
        }.getOrDefault(value)
    }

    private fun android.content.SharedPreferences.stringSet(key: String): Set<String> {
        return getStringSet(key, emptySet()).orEmpty().toSet()
    }

    companion object {
        const val PREFS_NAME = "mp3launcher_prefs"
        private const val KEY_PINNED_APPS = "pinned_apps"
        private const val KEY_HIDDEN_APPS = "hidden_apps"
        private const val KEY_RECENT_APPS = "recent_apps"
        private const val KEY_SELECTED_CATEGORY = "selected_category"
        private const val KEY_VOLUME_KEY_MODE = "volume_key_mode"
        private const val KEY_THEME_PRESET = "theme_preset"
        private const val KEY_SETTINGS_LANGUAGE = "settings_language"
        private const val KEY_CATEGORY_OVERRIDES = "category_overrides"
        private const val KEY_CUSTOM_CATEGORY_LABELS = "custom_category_labels"
        private const val KEY_MINIMAL_MODE = "minimal_mode"
        private const val KEY_BOOT_ANIMATION = "boot_animation"
        private const val KEY_LCD_SWIPE_TRACK = "lcd_swipe_track"
        private const val KEY_GLOBAL_VOLUME_KEY_TRACK = "global_volume_key_track"
        private const val KEY_DEFAULT_MUSIC_PACKAGE = "default_music_package"
        private const val KEY_PROMPTED_DEFAULT_LAUNCHER = "prompted_default_launcher"
        private const val RECENT_SEPARATOR = "|"
        private const val CATEGORY_PAIR_SEPARATOR = "|"
        private const val CATEGORY_VALUE_SEPARATOR = "="
        private const val CUSTOM_CATEGORY_PAIR_SEPARATOR = "|"
        private const val CUSTOM_CATEGORY_VALUE_SEPARATOR = "="
        private const val MAX_RECENT_APPS = 24
    }
}
