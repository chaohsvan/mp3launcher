package com.example.mp3launcher

import android.content.Context
import java.util.Locale

data class SettingsText(
    val settingsTitle: String,
    val settingsSubtitle: String,
    val systemSection: String,
    val systemSectionDescription: String,
    val launcherSection: String,
    val launcherSectionDescription: String,
    val themeSection: String,
    val themeSectionDescription: String,
    val appsSection: String,
    val appsSectionDescription: String,
    val languageLabel: String,
    val languageDescription: String,
    val notificationAccessOn: String,
    val notificationAccessOpen: String,
    val notificationAccessDescription: String,
    val accessibilitySettingsOn: String,
    val accessibilitySettingsOpen: String,
    val accessibilitySettingsDescription: String,
    val defaultLauncherPrompt: String,
    val defaultLauncherDescription: String,
    val defaultMusicApp: String,
    val defaultMusicAppDescription: String,
    val none: String,
    val volumeKeys: String,
    val volumeKeysDescription: String,
    val minimalLockMode: String,
    val minimalLockModeDescription: String,
    val bootAnimation: String,
    val bootAnimationDescription: String,
    val lcdSwipeTrack: String,
    val lcdSwipeTrackDescription: String,
    val skin: String,
    val skinDescription: String,
    val managePinnedApps: String,
    val managePinnedAppsDescription: String,
    val manageHiddenApps: String,
    val manageHiddenAppsDescription: String,
    val chooseAppsToHide: String,
    val chooseAppsToHideDescription: String,
    val appGroupTags: String,
    val appGroupTagsDescription: String,
    val chooseGroup: String,
    val createCustomGroup: String,
    val customGroupName: String,
    val customGroupLimitReached: String,
    val manageGroupApps: String,
    val renameGroup: String,
    val deleteGroup: String,
    val deleteGroupMessage: String,
    val autoGrouping: String,
    val clearGroupOverrides: String,
    val clearGroupOverridesDescription: String,
    val groupUpdated: String,
    val pinnedApps: String,
    val hiddenApps: String,
    val noPinnedApps: String,
    val noHiddenApps: String,
    val unpin: String,
    val unhide: String,
    val cancel: String,
    val ok: String,
    val hideApp: String,
    val defaultLauncherTitle: String,
    val defaultLauncherMessage: String,
    val open: String,
    val later: String,
    val pin: String,
    val hide: String,
    val appInfo: String,
    val uninstall: String,
    val loading: String
) {
    fun volumeKeyModeLabel(mode: VolumeKeyMode): String {
        return when (this) {
            ZH_CN -> when (mode) {
                VolumeKeyMode.TRACK_CONTROL -> "切歌 + 播放"
                VolumeKeyMode.MEDIA_VOLUME -> "音量 + 播放"
                VolumeKeyMode.SYSTEM_DEFAULT -> "系统默认"
            }
            else -> mode.label
        }
    }

    fun themeLabel(theme: ThemePreset): String {
        return when (this) {
            ZH_CN -> when (theme) {
                ThemePreset.BLACK_GREEN -> "黑绿 LCD"
                ThemePreset.SILVER_DISCMAN -> "银色 Discman"
                ThemePreset.TRANSPARENT_IPOD -> "透明 iPod"
                ThemePreset.GAME_BOY -> "Game Boy"
                ThemePreset.WALKMAN -> "Walkman"
            }
            else -> theme.label
        }
    }

    fun languageLabel(language: SettingsLanguage): String {
        return when (this) {
            ZH_CN -> when (language) {
                SettingsLanguage.SYSTEM -> "跟随系统"
                SettingsLanguage.ENGLISH -> "English"
                SettingsLanguage.SIMPLIFIED_CHINESE -> "简体中文"
            }
            else -> when (language) {
                SettingsLanguage.SYSTEM -> "System"
                SettingsLanguage.ENGLISH -> "English"
                SettingsLanguage.SIMPLIFIED_CHINESE -> "Simplified Chinese"
            }
        }
    }

    fun managePinnedAppsWithCount(count: Int): String = "$managePinnedApps ($count)"

    fun manageHiddenAppsWithCount(count: Int): String = "$manageHiddenApps ($count)"

    companion object {
        val EN = SettingsText(
            settingsTitle = "SETTINGS",
            settingsSubtitle = "Launcher behavior, music display, themes, and app lists.",
            systemSection = "SYSTEM",
            systemSectionDescription = "Permissions and Android-level defaults.",
            launcherSection = "LAUNCHER",
            launcherSectionDescription = "Controls that change how the home screen behaves.",
            themeSection = "THEME",
            themeSectionDescription = "Visual skin used by the retro player shell.",
            appsSection = "APPS",
            appsSectionDescription = "Pinned, hidden, and manually grouped apps.",
            languageLabel = "Language",
            languageDescription = "Choose the language used only on this settings page.",
            notificationAccessOn = "Notification access: ON",
            notificationAccessOpen = "Notification access: OPEN",
            notificationAccessDescription = "Needed to read current song, artist, progress, and album art.",
            accessibilitySettingsOn = "Accessibility settings: ON",
            accessibilitySettingsOpen = "Accessibility settings: OPEN",
            accessibilitySettingsDescription = "Open Android accessibility settings and enable MP3 Launcher lock screen volume keys.",
            defaultLauncherPrompt = "Default launcher prompt",
            defaultLauncherDescription = "Open Android home app settings and set MP3 Launcher as default.",
            defaultMusicApp = "Default music app when idle",
            defaultMusicAppDescription = "Used when pressing play while no media controller is active.",
            none = "None",
            volumeKeys = "Volume keys",
            volumeKeysDescription = "Choose what the physical volume keys do inside the launcher.",
            minimalLockMode = "Minimal lock mode",
            minimalLockModeDescription = "Hide the app bar without moving the player layout. Same as HOLD.",
            bootAnimation = "Boot animation",
            bootAnimationDescription = "Show a short LOADING screen when entering the launcher.",
            lcdSwipeTrack = "LCD swipe track control",
            lcdSwipeTrackDescription = "Swipe left or right on the simulated screen to switch tracks with a short LCD slide animation.",
            skin = "Skin",
            skinDescription = "Switch the color and material style of the simulated device.",
            managePinnedApps = "Manage pinned apps",
            managePinnedAppsDescription = "Remove apps that are pinned to the front of the app bar.",
            manageHiddenApps = "Manage hidden apps",
            manageHiddenAppsDescription = "Restore apps that are currently hidden from the launcher.",
            chooseAppsToHide = "Choose apps to hide",
            chooseAppsToHideDescription = "Hide rarely used or system apps from the app bar.",
            appGroupTags = "App group tags",
            appGroupTagsDescription = "Create custom tags and manually place apps into them.",
            chooseGroup = "Choose group",
            createCustomGroup = "Create custom tag",
            customGroupName = "Custom tag name",
            customGroupLimitReached = "Custom tag slots are full.",
            manageGroupApps = "Manage apps",
            renameGroup = "Rename tag",
            deleteGroup = "Delete tag",
            deleteGroupMessage = "Apps in this tag will return to automatic grouping.",
            autoGrouping = "Auto grouping",
            clearGroupOverrides = "Clear app tag assignments",
            clearGroupOverridesDescription = "Remove all manual app tag assignments and return to automatic grouping.",
            groupUpdated = "Group updated",
            pinnedApps = "Pinned apps",
            hiddenApps = "Hidden apps",
            noPinnedApps = "No pinned apps",
            noHiddenApps = "No hidden apps",
            unpin = "Unpin",
            unhide = "Unhide",
            cancel = "Cancel",
            ok = "OK",
            hideApp = "Hide app",
            defaultLauncherTitle = "Default launcher",
            defaultLauncherMessage = "Set MP3 Launcher as the default home app for the full launcher experience.",
            open = "Open",
            later = "Later",
            pin = "Pin",
            hide = "Hide",
            appInfo = "App info",
            uninstall = "Uninstall",
            loading = "LOADING..."
        )

        val ZH_CN = SettingsText(
            settingsTitle = "设置",
            settingsSubtitle = "管理桌面行为、音乐显示、主题皮肤和应用列表。",
            systemSection = "系统",
            systemSectionDescription = "权限、默认桌面和系统级默认行为。",
            launcherSection = "启动器",
            launcherSectionDescription = "控制桌面本身的操作方式和显示行为。",
            themeSection = "主题",
            themeSectionDescription = "切换复古播放器机身的颜色和材质风格。",
            appsSection = "应用",
            appsSectionDescription = "管理置顶、隐藏，以及手动应用分组。",
            languageLabel = "语言",
            languageDescription = "只影响设置页显示语言，不影响系统语言。",
            notificationAccessOn = "通知访问：已开启",
            notificationAccessOpen = "通知访问：去开启",
            notificationAccessDescription = "用于读取当前歌曲、歌手、进度和专辑封面。",
            accessibilitySettingsOn = "无障碍设置：已开启",
            accessibilitySettingsOpen = "无障碍设置：去开启",
            accessibilitySettingsDescription = "打开系统无障碍设置，开启 MP3 Launcher 锁屏音量键服务。",
            defaultLauncherPrompt = "默认桌面设置",
            defaultLauncherDescription = "打开系统默认桌面设置，将 MP3 Launcher 设为默认桌面。",
            defaultMusicApp = "未播放时默认音乐应用",
            defaultMusicAppDescription = "没有媒体控制器时，按播放键会打开这个音乐应用。",
            none = "无",
            volumeKeys = "实体音量键",
            volumeKeysDescription = "设置音量键在启动器内执行切歌、调音量或系统默认行为。",
            minimalLockMode = "锁屏式极简模式",
            minimalLockModeDescription = "隐藏应用栏但保持播放器布局不移动，等同于 HOLD 拨杆。",
            bootAnimation = "开机动画",
            bootAnimationDescription = "进入桌面时显示短暂 LOADING 动画。",
            lcdSwipeTrack = "模拟屏幕横滑切歌",
            lcdSwipeTrackDescription = "在模拟屏幕上左右横滑切换上一首/下一首，并显示短暂 LCD 滑动动画。",
            skin = "皮肤",
            skinDescription = "切换模拟机身的配色、按钮和屏幕风格。",
            managePinnedApps = "管理置顶应用",
            managePinnedAppsDescription = "取消已置顶的应用，让应用栏排序恢复正常。",
            manageHiddenApps = "管理隐藏应用",
            manageHiddenAppsDescription = "恢复已经从应用栏隐藏的应用。",
            chooseAppsToHide = "选择要隐藏的应用",
            chooseAppsToHideDescription = "隐藏不常用或系统应用，让应用栏更干净。",
            appGroupTags = "应用标签分组",
            appGroupTagsDescription = "新建自定义标签，并手动把应用放入标签分组。",
            chooseGroup = "选择分组",
            createCustomGroup = "新建自定义标签",
            customGroupName = "自定义标签名称",
            customGroupLimitReached = "自定义标签槽位已满。",
            manageGroupApps = "管理应用",
            renameGroup = "重命名标签",
            deleteGroup = "删除标签",
            deleteGroupMessage = "这个标签里的应用会恢复为自动分组。",
            autoGrouping = "自动分组",
            clearGroupOverrides = "清除应用标签分配",
            clearGroupOverridesDescription = "删除所有手动应用标签分配，恢复自动分类。",
            groupUpdated = "分组已更新",
            pinnedApps = "置顶应用",
            hiddenApps = "隐藏应用",
            noPinnedApps = "暂无置顶应用",
            noHiddenApps = "暂无隐藏应用",
            unpin = "取消置顶",
            unhide = "取消隐藏",
            cancel = "取消",
            ok = "确定",
            hideApp = "隐藏应用",
            defaultLauncherTitle = "默认桌面",
            defaultLauncherMessage = "将 MP3 Launcher 设为默认桌面，以获得完整启动器体验。",
            open = "打开",
            later = "稍后",
            pin = "置顶",
            hide = "隐藏",
            appInfo = "应用信息",
            uninstall = "卸载",
            loading = "载入中..."
        )

        fun resolve(context: Context, preferences: LauncherPreferences): SettingsText {
            val language = when (preferences.settingsLanguage) {
                SettingsLanguage.SYSTEM -> {
                    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
                    if (locale.language == Locale.CHINESE.language) SettingsLanguage.SIMPLIFIED_CHINESE else SettingsLanguage.ENGLISH
                }
                else -> preferences.settingsLanguage
            }
            return if (language == SettingsLanguage.SIMPLIFIED_CHINESE) ZH_CN else EN
        }
    }
}
