package com.example.mp3launcher

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var preferences: LauncherPreferences
    private lateinit var apps: List<AppInfo>
    private val text: SettingsText
        get() = SettingsText.resolve(this, preferences)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = LauncherPreferences(this)
        apps = loadLaunchableApps()
        buildContent()
    }

    override fun onResume() {
        super.onResume()
        apps = loadLaunchableApps()
        buildContent()
    }

    private fun buildContent() {
        val theme = preferences.themePreset
        val root = ScrollView(this).apply {
            setBackgroundColor(theme.rootBackground)
            isFillViewport = true
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp(), 24.dp(), 24.dp(), 36.dp())
        }
        root.addView(container)

        container.addView(TextView(this).apply {
            text = this@SettingsActivity.text.settingsTitle
            setTextColor(theme.accent)
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
        })

        container.addView(TextView(this).apply {
            text = this@SettingsActivity.text.settingsSubtitle
            setTextColor(theme.labelText)
            textSize = 13f
            setPadding(0, 6.dp(), 0, 2.dp())
        })

        container.addView(section(text.systemSection, text.systemSectionDescription) {
            addEnumSpinner(
                label = text.languageLabel,
                description = text.languageDescription,
                values = SettingsLanguage.entries,
                selected = preferences.settingsLanguage,
                text = { text.languageLabel(it) },
                onSelected = {
                    if (preferences.settingsLanguage != it) {
                        preferences.settingsLanguage = it
                        buildContent()
                    }
                }
            )
            addButton(
                label = if (isNotificationServiceEnabled()) text.notificationAccessOn else text.notificationAccessOpen,
                description = text.notificationAccessDescription
            ) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            addButton(text.defaultLauncherPrompt, text.defaultLauncherDescription) {
                openDefaultLauncherSettings()
                preferences.hasPromptedDefaultLauncher = true
            }
            addMusicAppSpinner()
        })

        container.addView(section(text.launcherSection, text.launcherSectionDescription) {
            addEnumSpinner(
                label = text.volumeKeys,
                description = text.volumeKeysDescription,
                values = VolumeKeyMode.entries,
                selected = preferences.volumeKeyMode,
                text = { text.volumeKeyModeLabel(it) },
                onSelected = { preferences.volumeKeyMode = it }
            )
            addCheckBox(text.minimalLockMode, text.minimalLockModeDescription, preferences.isMinimalModeEnabled) {
                preferences.isMinimalModeEnabled = it
            }
            addCheckBox(text.bootAnimation, text.bootAnimationDescription, preferences.isBootAnimationEnabled) {
                preferences.isBootAnimationEnabled = it
            }
            addCheckBox(text.lcdSwipeTrack, text.lcdSwipeTrackDescription, preferences.isLcdSwipeTrackEnabled) {
                preferences.isLcdSwipeTrackEnabled = it
            }
        })

        container.addView(section(text.themeSection, text.themeSectionDescription) {
            addEnumSpinner(
                label = text.skin,
                description = text.skinDescription,
                values = ThemePreset.entries,
                selected = preferences.themePreset,
                text = { text.themeLabel(it) },
                onSelected = {
                    preferences.themePreset = it
                    buildContent()
                }
            )
        })

        container.addView(section(text.appsSection, text.appsSectionDescription) {
            addButton(text.managePinnedAppsWithCount(preferences.pinnedApps.size), text.managePinnedAppsDescription) {
                showAppSetManager(
                    title = text.pinnedApps,
                    packageNames = preferences.pinnedApps.toList(),
                    emptyMessage = text.noPinnedApps,
                    actionLabel = text.unpin
                ) { preferences.unpinApp(it) }
            }
            addButton(text.manageHiddenAppsWithCount(preferences.hiddenApps.size), text.manageHiddenAppsDescription) {
                showAppSetManager(
                    title = text.hiddenApps,
                    packageNames = preferences.hiddenApps.toList(),
                    emptyMessage = text.noHiddenApps,
                    actionLabel = text.unhide
                ) { preferences.unhideApp(it) }
            }
            addButton(text.chooseAppsToHide, text.chooseAppsToHideDescription) {
                showHidePicker()
            }
            addButton(text.appGroupTags, text.appGroupTagsDescription) {
                showGroupPicker()
            }
            addButton(text.clearGroupOverrides, text.clearGroupOverridesDescription) {
                preferences.clearAppCategoryOverrides()
                apps = loadLaunchableApps()
                buildContent()
            }
        })

        setContentView(root)
    }

    private fun section(title: String, description: String, build: LinearLayout.() -> Unit): View {
        val theme = preferences.themePreset
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = recessedDrawable(theme.controlBackground, 18f)
            setPadding(16.dp(), 14.dp(), 16.dp(), 16.dp())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 16.dp()
            }
            addView(TextView(context).apply {
                text = title
                textSize = 12f
                letterSpacing = 0.12f
                setTextColor(theme.accent)
                typeface = Typeface.DEFAULT_BOLD
            })
            addView(TextView(context).apply {
                text = description
                textSize = 12f
                setTextColor(theme.labelText)
                setPadding(0, 4.dp(), 0, 8.dp())
            })
            build()
        }
    }

    private fun LinearLayout.addButton(label: String, description: String, onClick: () -> Unit) {
        val theme = preferences.themePreset
        addView(Button(context).apply {
            text = label
            isAllCaps = false
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
            minHeight = 44.dp()
            setPadding(14.dp(), 8.dp(), 14.dp(), 8.dp())
            setTextColor(theme.buttonIconTint)
            background = buttonStateDrawable(theme.buttonBackground, theme.buttonPressed, 14f)
            setOnClickListener { onClick() }
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 10.dp()
        })
        addSettingHint(description, startPadding = 2.dp(), bottomPadding = 2.dp())
    }

    private fun LinearLayout.addCheckBox(label: String, description: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
        val theme = preferences.themePreset
        addView(CheckBox(context).apply {
            text = label
            isChecked = checked
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(theme.accent)
            buttonTintList = ColorStateList.valueOf(theme.accent)
            setPadding(0, 10.dp(), 0, 0)
            setOnCheckedChangeListener { _, isChecked -> onChanged(isChecked) }
        })
        addSettingHint(description, startPadding = 42.dp(), bottomPadding = 4.dp())
    }

    private fun <T> LinearLayout.addEnumSpinner(
        label: String,
        description: String,
        values: List<T>,
        selected: T,
        text: (T) -> String,
        onSelected: (T) -> Unit
    ) {
        addSettingDescription(label, description)
        addView(Spinner(context).apply {
            adapter = themedSpinnerAdapter(values.map(text))
            background = spinnerBackground(preferences.themePreset)
            minimumHeight = 44.dp()
            setPadding(10.dp(), 0, 10.dp(), 0)
            setSelection(values.indexOf(selected).coerceAtLeast(0), false)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    onSelected(values[position])
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        })
    }

    private fun LinearLayout.addMusicAppSpinner() {
        val candidates = apps.filterNot { it.isSettingsShortcut }
        val labels = listOf(text.none) + candidates.map { it.label.toString() }
        val selectedIndex = candidates.indexOfFirst {
            it.packageName.toString() == preferences.defaultMusicPackage
        }.let { if (it >= 0) it + 1 else 0 }

        addSettingDescription(
            this@SettingsActivity.text.defaultMusicApp,
            this@SettingsActivity.text.defaultMusicAppDescription
        )
        addView(Spinner(context).apply {
            adapter = themedSpinnerAdapter(labels)
            background = spinnerBackground(preferences.themePreset)
            minimumHeight = 44.dp()
            setPadding(10.dp(), 0, 10.dp(), 0)
            setSelection(selectedIndex, false)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    preferences.defaultMusicPackage = candidates.getOrNull(position - 1)?.packageName?.toString()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        })
    }

    private fun LinearLayout.addSettingDescription(label: String, description: String) {
        val theme = preferences.themePreset
        addView(TextView(context).apply {
            text = label
            textSize = 14f
            setTextColor(theme.accent)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 12.dp(), 0, 2.dp())
        })
        addSettingHint(description, startPadding = 0, bottomPadding = 6.dp())
    }

    private fun LinearLayout.addSettingHint(description: String, startPadding: Int, bottomPadding: Int) {
        val theme = preferences.themePreset
        addView(TextView(context).apply {
            text = description
            textSize = 12f
            setTextColor(theme.lcdTextFaded)
            setPadding(startPadding, 3.dp(), 0, bottomPadding)
        })
    }

    private fun themedSpinnerAdapter(labels: List<String>): ArrayAdapter<String> {
        val theme = preferences.themePreset
        return object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, labels) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return (super.getView(position, convertView, parent) as TextView).apply {
                    setTextColor(theme.lcdText)
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(4.dp(), 0, 4.dp(), 0)
                }
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                return (super.getDropDownView(position, convertView, parent) as TextView).apply {
                    setTextColor(theme.lcdText)
                    setBackgroundColor(theme.lcdBackground)
                    textSize = 14f
                    setPadding(16.dp(), 12.dp(), 16.dp(), 12.dp())
                }
            }
        }.apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun spinnerBackground(theme: ThemePreset) = recessedDrawable(
        adjustColorBrightness(theme.lcdBackground, 0.92f),
        12f
    )

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private fun showAppSetManager(
        title: String,
        packageNames: List<String>,
        emptyMessage: String,
        actionLabel: String,
        onAction: (String) -> Unit
    ) {
        if (packageNames.isEmpty()) {
            AlertDialog.Builder(this).setTitle(title).setMessage(emptyMessage).setPositiveButton(text.ok, null).show()
            return
        }

        val items = packageNames.map { packageName ->
            apps.firstOrNull { it.packageName == packageName }?.label?.toString() ?: packageName
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(items) { _, which ->
                val packageName = packageNames[which]
                AlertDialog.Builder(this)
                    .setTitle(items[which])
                    .setPositiveButton(actionLabel) { _, _ ->
                        onAction(packageName)
                        buildContent()
                    }
                    .setNegativeButton(text.cancel, null)
                    .show()
            }
            .show()
    }

    private fun showHidePicker() {
        val visibleApps = apps.filterNot { it.packageName.toString() in preferences.hiddenApps }
        val items = visibleApps.map { it.label.toString() }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(text.hideApp)
            .setItems(items) { _, which ->
                preferences.hideApp(visibleApps[which].packageName.toString())
                buildContent()
            }
            .show()
    }

    private fun loadLaunchableApps(): List<AppInfo> {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val pm = packageManager
        val resolved: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }
        return resolved.mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            if (packageName == this.packageName) return@mapNotNull null
            val label = resolveInfo.loadLabel(pm)
            AppInfo(
                label = label,
                packageName = packageName,
                icon = resolveInfo.loadIcon(pm),
                isPinned = preferences.isPinned(packageName),
                category = preferences.categoryOverrideFor(packageName)
                    ?: AppCategoryResolver.resolve(label, packageName)
            )
        }.sortedBy { it.label.toString().lowercase() }
    }

    private fun showGroupPicker() {
        val groups = preferences.assignableCategories()
        val labels = (listOf(text.createCustomGroup) + groups.map { localizedCategoryName(it) }).toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(text.chooseGroup)
            .setItems(labels) { _, which ->
                if (which == 0) {
                    showCreateCustomGroupDialog()
                } else {
                    showGroupActions(groups[which - 1])
                }
            }
            .show()
    }

    private fun showCreateCustomGroupDialog() {
        val input = EditText(this).apply {
            hint = this@SettingsActivity.text.customGroupName
            setSingleLine(true)
            setPadding(32.dp(), 16.dp(), 32.dp(), 16.dp())
        }
        AlertDialog.Builder(this)
            .setTitle(text.createCustomGroup)
            .setView(input)
            .setPositiveButton(text.ok) { _, _ ->
                val created = preferences.createCustomCategory(input.text.toString())
                if (created != null) {
                    apps = loadLaunchableApps()
                    showAppsForGroup(created)
                } else {
                    AlertDialog.Builder(this)
                        .setMessage(text.customGroupLimitReached)
                        .setPositiveButton(text.ok, null)
                        .show()
                }
            }
            .setNegativeButton(text.cancel, null)
            .show()
    }

    private fun showGroupActions(category: AppCategory) {
        if (!category.isCustom) {
            showAppsForGroup(category)
            return
        }

        val actions = arrayOf(text.manageGroupApps, text.renameGroup, text.deleteGroup)
        AlertDialog.Builder(this)
            .setTitle(localizedCategoryName(category))
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> showAppsForGroup(category)
                    1 -> showRenameCustomGroupDialog(category)
                    2 -> confirmDeleteCustomGroup(category)
                }
            }
            .show()
    }

    private fun showRenameCustomGroupDialog(category: AppCategory) {
        val input = EditText(this).apply {
            setText(localizedCategoryName(category))
            selectAll()
            setSingleLine(true)
            setPadding(32.dp(), 16.dp(), 32.dp(), 16.dp())
        }
        AlertDialog.Builder(this)
            .setTitle(text.renameGroup)
            .setView(input)
            .setPositiveButton(text.ok) { _, _ ->
                preferences.setCustomCategoryLabel(category, input.text.toString())
                apps = loadLaunchableApps()
                buildContent()
            }
            .setNegativeButton(text.cancel, null)
            .show()
    }

    private fun confirmDeleteCustomGroup(category: AppCategory) {
        AlertDialog.Builder(this)
            .setTitle(text.deleteGroup)
            .setMessage(text.deleteGroupMessage)
            .setPositiveButton(text.ok) { _, _ ->
                preferences.deleteCustomCategory(category)
                apps = loadLaunchableApps()
                buildContent()
            }
            .setNegativeButton(text.cancel, null)
            .show()
    }

    private fun showAppsForGroup(category: AppCategory) {
        val selectableApps = apps.filterNot { it.isSettingsShortcut }
        val labels = selectableApps.map { it.label.toString() }.toTypedArray()
        val checked = selectableApps.map { it.category == category }.toBooleanArray()

        AlertDialog.Builder(this)
            .setTitle(localizedCategoryName(category))
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                val packageName = selectableApps[which].packageName.toString()
                if (isChecked) {
                    preferences.setAppCategoryOverride(packageName, category)
                } else {
                    preferences.clearAppCategoryOverride(packageName)
                }
            }
            .setPositiveButton(text.ok) { _, _ ->
                apps = loadLaunchableApps()
                buildContent()
            }
            .setNeutralButton(text.autoGrouping) { _, _ ->
                selectableApps
                    .filter { it.category == category }
                    .forEach { preferences.clearAppCategoryOverride(it.packageName.toString()) }
                apps = loadLaunchableApps()
                buildContent()
            }
            .setNegativeButton(text.cancel, null)
            .show()
    }

    private fun localizedCategoryName(category: AppCategory): String {
        val isChinese = text === SettingsText.ZH_CN
        return if (isChinese) {
            when (category) {
                AppCategory.RECENT -> "最近"
                AppCategory.ALL -> "全部"
                AppCategory.MUSIC -> "音乐"
                AppCategory.SOCIAL -> "社交"
                AppCategory.TOOLS -> "工具"
                AppCategory.GAMES -> "游戏"
                else -> preferences.customCategoryLabel(category) ?: category.label
            }
        } else {
            preferences.customCategoryLabel(category) ?: category.label
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val componentName = ComponentName(this, MediaNotificationListenerService::class.java)
        return enabledListeners?.contains(componentName.flattenToString()) == true
    }

    private fun openDefaultLauncherSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Intent(Settings.ACTION_HOME_SETTINGS)
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
        startActivity(intent)
    }
}
