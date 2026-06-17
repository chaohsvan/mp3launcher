package com.example.mp3launcher

import android.graphics.drawable.Drawable

data class AppInfo(
    val label: CharSequence,
    val packageName: CharSequence,
    val icon: Drawable,
    val isPinned: Boolean = false,
    val category: AppCategory = AppCategory.TOOLS,
    val isSettingsShortcut: Boolean = false
) {
    companion object {
        const val SETTINGS_PACKAGE_NAME = "com.example.mp3launcher.SETTINGS"
    }
}

object AppCategoryResolver {
    private val musicKeywords = listOf(
        "music",
        "audio",
        "player",
        "spotify",
        "podcast",
        "sound",
        "radio",
        "网易云",
        "音乐",
        "qq音乐",
        "酷狗",
        "酷我",
        "喜马拉雅"
    )

    private val socialKeywords = listOf(
        "wechat",
        "weixin",
        "qq",
        "telegram",
        "whatsapp",
        "instagram",
        "facebook",
        "twitter",
        "x",
        "discord",
        "messages",
        "mail",
        "微信",
        "微博",
        "小红书",
        "抖音"
    )

    private val gameKeywords = listOf(
        "game",
        "games",
        "steam",
        "play",
        "arcade",
        "mihoyo",
        "hoyoverse",
        "游戏",
        "原神",
        "王者",
        "和平精英"
    )

    fun resolve(label: CharSequence, packageName: String): AppCategory {
        val haystack = "${label.toString().lowercase()} ${packageName.lowercase()}"
        return when {
            musicKeywords.any { haystack.contains(it.lowercase()) } -> AppCategory.MUSIC
            socialKeywords.any { haystack.contains(it.lowercase()) } -> AppCategory.SOCIAL
            gameKeywords.any { haystack.contains(it.lowercase()) } -> AppCategory.GAMES
            else -> AppCategory.TOOLS
        }
    }
}
