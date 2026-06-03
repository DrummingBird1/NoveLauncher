package com.ailauncher.app.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.ailauncher.app.data.SettingsRepository
import com.ailauncher.app.data.backup.BackupManager
import com.ailauncher.app.data.db.UsageDao
import com.ailauncher.app.data.iconpack.IconPackManager
import com.ailauncher.app.domain.models.AppearanceSettings
import com.ailauncher.app.domain.models.BackupSettings
import com.ailauncher.app.domain.models.HiddenAppsSettings
import com.ailauncher.app.domain.models.NewsSettings
import com.ailauncher.app.domain.models.PagesSettings
import com.ailauncher.app.domain.models.SecuritySettings
import com.ailauncher.app.security.AppLockManager
import com.ailauncher.app.ui.theme.AILauncherTheme
import javax.inject.Inject
import kotlinx.coroutines.launch

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
    @Inject lateinit var usageDao: UsageDao
    @Inject lateinit var iconPackManager: IconPackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appearance by settingsRepo.appearanceFlow.collectAsState(initial = AppearanceSettings())
            AILauncherTheme(appearance = appearance) {
                SettingsRoot(settingsRepo, appLockManager, backupManager, usageDao, iconPackManager, onBack = { finish() })
            }
        }
    }
}

enum class SettingsPage {
    MAIN, APPEARANCE, THEMES, FONTS, ICON_SHAPES, CLOCK,
    PAGES, BACKUP, SECURITY, APP_LOCK_LIST, NEWS_SOURCES,
    HIDDEN_APPS, WALLPAPER, ABOUT, STATISTICS, ICON_PACKS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoot(
    settingsRepo: SettingsRepository,
    appLockManager: AppLockManager,
    backupManager: BackupManager,
    usageDao: UsageDao,
    iconPackManager: IconPackManager,
    onBack: () -> Unit
) {
    var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }
    val scope = rememberCoroutineScope()

    val appearance by settingsRepo.appearanceFlow.collectAsState(initial = AppearanceSettings())
    val pages by settingsRepo.pagesFlow.collectAsState(initial = PagesSettings())
    val security by settingsRepo.securityFlow.collectAsState(initial = SecuritySettings())
    val backup by settingsRepo.backupFlow.collectAsState(initial = BackupSettings())
    val news by settingsRepo.newsFlow.collectAsState(initial = NewsSettings())
    val hidden by settingsRepo.hiddenAppsFlow.collectAsState(initial = HiddenAppsSettings())

    val pageTitle = stringResource(when (currentPage) {
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
    })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pageTitle) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentPage == SettingsPage.MAIN) onBack()
                        else if (currentPage in listOf(SettingsPage.THEMES, SettingsPage.FONTS, SettingsPage.ICON_SHAPES, SettingsPage.CLOCK))
                            currentPage = SettingsPage.APPEARANCE
                        else currentPage = SettingsPage.MAIN
                    }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentPage) {
                SettingsPage.MAIN -> MainSettings(onNavigate = { currentPage = it })
                SettingsPage.APPEARANCE -> AppearanceSection(appearance, { scope.launch { settingsRepo.saveAppearance(it) } }, { currentPage = it })
                SettingsPage.THEMES -> ThemesSection(appearance) { scope.launch { settingsRepo.saveAppearance(it) } }
                SettingsPage.FONTS -> FontsSection(appearance) { scope.launch { settingsRepo.saveAppearance(it) } }
                SettingsPage.ICON_SHAPES -> IconShapesSection(appearance) { scope.launch { settingsRepo.saveAppearance(it) } }
                SettingsPage.CLOCK -> ClockSection(appearance) { scope.launch { settingsRepo.saveAppearance(it) } }
                SettingsPage.PAGES -> PagesSection(pages) { scope.launch { settingsRepo.savePages(it) } }
                SettingsPage.BACKUP -> BackupSection(backup, backupManager, settingsRepo) { scope.launch { settingsRepo.saveBackup(it) } }
                SettingsPage.SECURITY -> SecuritySection(security, appLockManager, settingsRepo) { currentPage = it }
                SettingsPage.APP_LOCK_LIST -> AppLockListSection(security, appLockManager)
                SettingsPage.NEWS_SOURCES -> NewsSourcesSection(news) { scope.launch { settingsRepo.saveNews(it) } }
                SettingsPage.HIDDEN_APPS -> HiddenAppsSection(hidden) { scope.launch { settingsRepo.saveHiddenApps(it) } }
                SettingsPage.WALLPAPER -> WallpaperSection(appearance) { scope.launch { settingsRepo.saveAppearance(it) } }
                SettingsPage.ABOUT -> AboutSection()
                SettingsPage.STATISTICS -> StatisticsScreen(usageDao, onBack = { currentPage = SettingsPage.MAIN })
                SettingsPage.ICON_PACKS -> IconPacksSection(iconPackManager)
            }
        }
    }
}
