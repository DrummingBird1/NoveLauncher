package com.ailauncher.app.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ailauncher.app.R
import com.ailauncher.app.data.JewishCalendarService
import com.ailauncher.app.data.SettingsRepository
import com.ailauncher.app.data.UsageStatsRepository
import com.ailauncher.app.data.backup.BackupManager
import com.ailauncher.app.data.iconpack.IconPackManager
import com.ailauncher.app.data.ml.SuggestionFeedbackRepository
import com.ailauncher.app.data.update.UpdateChecker
import com.ailauncher.app.domain.models.AppearanceSettings
import com.ailauncher.app.domain.models.BackupSettings
import com.ailauncher.app.domain.models.HiddenAppsSettings
import com.ailauncher.app.domain.models.NewsSettings
import com.ailauncher.app.domain.models.PagesSettings
import com.ailauncher.app.domain.models.SecuritySettings
import com.ailauncher.app.security.AppLockManager
import com.ailauncher.app.ui.theme.AILauncherTheme
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * v9: top-level Settings entry point. The 16 section-specific Composables were
 * extracted into per-domain files (see Settings*.kt next to this file). This
 * file is now just the Activity, the SettingsPage enum, and the Scaffold that
 * routes between sections.
 *
 * Was 1,250 lines pre-v9.
 *
 * v8: now Hilt-injects its repositories instead of building them by hand. The previous
 * manual construction bypassed Hilt's SingletonComponent — that meant SettingsActivity
 * saw a *different* SettingsRepository instance than the rest of the app, so any
 * cached/in-memory state (e.g. AppLockManager's unlock cache) was per-Activity rather
 * than process-wide. With injection, all settings views share state correctly.
 */
@dagger.hilt.android.AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    @Inject lateinit var settingsRepo: SettingsRepository

    @Inject lateinit var appLockManager: AppLockManager

    @Inject lateinit var backupManager: BackupManager

    @Inject lateinit var usageStatsRepo: UsageStatsRepository

    @Inject lateinit var iconPackManager: IconPackManager

    @Inject lateinit var jewishCalendar: JewishCalendarService

    @Inject lateinit var updateChecker: UpdateChecker

    @Inject lateinit var feedbackRepo: SuggestionFeedbackRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Static shortcuts (res/xml/shortcuts.xml) and Global Search's settings
        // results both deep-link straight to a page by SettingsPage enum name.
        val initialPage =
            try {
                SettingsPage.valueOf(intent?.getStringExtra(EXTRA_SHORTCUT_PAGE) ?: "MAIN")
            } catch (_: IllegalArgumentException) {
                SettingsPage.MAIN
            }
        setContent {
            val appearance by settingsRepo.appearanceFlow.collectAsState(initial = AppearanceSettings())
            AILauncherTheme(appearance = appearance) {
                SettingsRoot(
                    SettingsDeps(
                        settingsRepo,
                        appLockManager,
                        backupManager,
                        usageStatsRepo,
                        iconPackManager,
                        jewishCalendar,
                        updateChecker,
                        feedbackRepo,
                    ),
                    initialPage = initialPage,
                    onBack = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_SHORTCUT_PAGE = "shortcut_page"
    }
}

enum class SettingsPage {
    MAIN,
    APPEARANCE,
    THEMES,
    FONTS,
    ICON_SHAPES,
    CLOCK,
    PAGES,
    BACKUP,
    SECURITY,
    APP_LOCK_LIST,
    NEWS_SOURCES,
    HIDDEN_APPS,
    WALLPAPER,
    ABOUT,
    STATISTICS,
    ICON_PACKS,
    GESTURES,
    JEWISH_CALENDAR, // v10
}

/** v10: everything the settings pages need from Hilt, in one parameter. */
class SettingsDeps(
    val settingsRepo: SettingsRepository,
    val appLockManager: AppLockManager,
    val backupManager: BackupManager,
    val usageStatsRepo: UsageStatsRepository,
    val iconPackManager: IconPackManager,
    val jewishCalendar: JewishCalendarService,
    val updateChecker: UpdateChecker,
    val feedbackRepo: SuggestionFeedbackRepository,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoot(
    deps: SettingsDeps,
    initialPage: SettingsPage = SettingsPage.MAIN,
    onBack: () -> Unit,
) {
    val settingsRepo = deps.settingsRepo
    var currentPage by remember { mutableStateOf(initialPage) }
    val scope = rememberCoroutineScope()

    // v10: system back walks up one level, same as the toolbar arrow.
    fun goUp() {
        currentPage =
            when {
                currentPage == SettingsPage.MAIN -> {
                    onBack()
                    return
                }

                currentPage in listOf(SettingsPage.THEMES, SettingsPage.FONTS, SettingsPage.ICON_SHAPES, SettingsPage.CLOCK) -> {
                    SettingsPage.APPEARANCE
                }

                currentPage == SettingsPage.APP_LOCK_LIST -> {
                    SettingsPage.SECURITY
                }

                else -> {
                    SettingsPage.MAIN
                }
            }
    }
    androidx.activity.compose.BackHandler(enabled = currentPage != SettingsPage.MAIN) { goUp() }

    val appearance by settingsRepo.appearanceFlow.collectAsState(initial = AppearanceSettings())
    val pages by settingsRepo.pagesFlow.collectAsState(initial = PagesSettings())
    val security by settingsRepo.securityFlow.collectAsState(initial = SecuritySettings())
    val backup by settingsRepo.backupFlow.collectAsState(initial = BackupSettings())
    val smartControl by settingsRepo.smartControlFlow.collectAsState(
        initial =
            com.ailauncher.app.domain.models
                .SmartControlSettings(),
    )
    val news by settingsRepo.newsFlow.collectAsState(initial = NewsSettings())
    val hidden by settingsRepo.hiddenAppsFlow.collectAsState(initial = HiddenAppsSettings())

    val pageTitle =
        stringResource(
            when (currentPage) {
                SettingsPage.MAIN -> R.string.settings_title
                SettingsPage.APPEARANCE -> R.string.settings_appearance
                SettingsPage.THEMES -> R.string.settings_themes
                SettingsPage.FONTS -> R.string.settings_fonts
                SettingsPage.ICON_SHAPES -> R.string.settings_icon_shapes
                SettingsPage.CLOCK -> R.string.settings_clock
                SettingsPage.PAGES -> R.string.settings_pages
                SettingsPage.BACKUP -> R.string.settings_backup
                SettingsPage.SECURITY -> R.string.settings_security
                SettingsPage.APP_LOCK_LIST -> R.string.settings_app_lock_list
                SettingsPage.NEWS_SOURCES -> R.string.settings_news_sources
                SettingsPage.HIDDEN_APPS -> R.string.settings_hidden_apps
                SettingsPage.WALLPAPER -> R.string.settings_wallpapers
                SettingsPage.ABOUT -> R.string.settings_about
                SettingsPage.STATISTICS -> R.string.settings_statistics
                SettingsPage.ICON_PACKS -> R.string.settings_icon_packs
                SettingsPage.GESTURES -> R.string.settings_gestures
                SettingsPage.JEWISH_CALENDAR -> R.string.settings_jewish_calendar
            },
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pageTitle) },
                navigationIcon = {
                    IconButton(onClick = { goUp() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentPage) {
                SettingsPage.MAIN -> {
                    MainSettings(onNavigate = { currentPage = it })
                }

                SettingsPage.APPEARANCE -> {
                    AppearanceSection(
                        appearance,
                        { scope.launch { settingsRepo.saveAppearance(it) } },
                        { currentPage = it },
                        smartControl,
                        { scope.launch { settingsRepo.saveSmartControl(it) } },
                    )
                }

                SettingsPage.THEMES -> {
                    ThemesSection(appearance) { scope.launch { settingsRepo.saveAppearance(it) } }
                }

                SettingsPage.FONTS -> {
                    FontsSection(appearance) { scope.launch { settingsRepo.saveAppearance(it) } }
                }

                SettingsPage.ICON_SHAPES -> {
                    IconShapesSection(appearance) { scope.launch { settingsRepo.saveAppearance(it) } }
                }

                SettingsPage.CLOCK -> {
                    ClockSection(appearance) { scope.launch { settingsRepo.saveAppearance(it) } }
                }

                SettingsPage.PAGES -> {
                    PagesSection(pages) { scope.launch { settingsRepo.savePages(it) } }
                }

                SettingsPage.BACKUP -> {
                    BackupSection(backup, deps.backupManager, settingsRepo) { scope.launch { settingsRepo.saveBackup(it) } }
                }

                SettingsPage.SECURITY -> {
                    SecuritySection(security, deps.appLockManager, settingsRepo) { currentPage = it }
                }

                SettingsPage.APP_LOCK_LIST -> {
                    AppLockListSection(security, deps.appLockManager)
                }

                SettingsPage.NEWS_SOURCES -> {
                    NewsSourcesSection(news) { scope.launch { settingsRepo.saveNews(it) } }
                }

                SettingsPage.HIDDEN_APPS -> {
                    HiddenAppsSection(hidden) { scope.launch { settingsRepo.saveHiddenApps(it) } }
                }

                SettingsPage.WALLPAPER -> {
                    WallpaperSection(appearance, pages) { scope.launch { settingsRepo.saveAppearance(it) } }
                }

                SettingsPage.ABOUT -> {
                    AboutSection(smartControl, { scope.launch { settingsRepo.saveSmartControl(it) } }, deps.updateChecker)
                }

                SettingsPage.STATISTICS -> {
                    StatisticsScreen(deps.usageStatsRepo) { scope.launch { deps.feedbackRepo.clear() } }
                }

                SettingsPage.ICON_PACKS -> {
                    IconPacksSection(deps.iconPackManager, appearance) { scope.launch { settingsRepo.saveAppearance(it) } }
                }

                SettingsPage.GESTURES -> {
                    GesturesSection(smartControl) { scope.launch { settingsRepo.saveSmartControl(it) } }
                }

                SettingsPage.JEWISH_CALENDAR -> {
                    JewishCalendarSection(appearance, deps.jewishCalendar) { scope.launch { settingsRepo.saveAppearance(it) } }
                }
            }
        }
    }
}
