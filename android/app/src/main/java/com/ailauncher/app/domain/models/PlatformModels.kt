package com.ailauncher.app.domain.models

import androidx.annotation.StringRes
import com.ailauncher.app.R
import kotlinx.serialization.Serializable

/**
 * Integration with the rest of the device + app platform: backup destinations,
 * display adaptation, repair counters, onboarding state, third-party widget
 * slots, RSS news sources.
 */

@Serializable enum class BackupSchedule(@StringRes val displayNameRes: Int) {
    MANUAL(R.string.backup_schedule_manual),
    DAILY(R.string.backup_schedule_daily),
    WEEKLY(R.string.backup_schedule_weekly),
    MONTHLY(R.string.backup_schedule_monthly)
}

@Serializable enum class BackupDestination(@StringRes val displayNameRes: Int) {
    LOCAL(R.string.backup_dest_local),
    GOOGLE_DRIVE(R.string.backup_dest_google_drive),
    ONEDRIVE(R.string.backup_dest_onedrive),
    BOX(R.string.backup_dest_box),
    NAS(R.string.backup_dest_nas)
}

@Serializable data class BackupSettings(
    val autoBackupEnabled: Boolean = false,
    val autoBackupDestination: BackupDestination = BackupDestination.LOCAL,
    val schedule: BackupSchedule = BackupSchedule.MANUAL,
    val nasAddress: String = "",
    val nasPath: String = "",
    val nasUsername: String = "",
    val nasPassword: String = "",
    val lastBackupTimestamp: Long = 0L,
    // v9.3: true is the default so an install that's never attempted a backup
    // doesn't show a false "failed" state — lastBackupTimestamp == 0L is what
    // the UI checks to distinguish "never backed up" from "succeeded."
    val lastBackupSuccess: Boolean = true,
    val maxLocalBackupsToKeep: Int = 10 // 0 = unlimited, no pruning
)

@Serializable enum class ScreenType(@StringRes val displayNameRes: Int) {
    PHONE(R.string.screen_type_phone),
    TABLET(R.string.screen_type_tablet),
    FOLDABLE(R.string.screen_type_foldable),
    LANDSCAPE(R.string.screen_type_landscape)
}

@Serializable data class AdaptiveDisplaySettings(
    val autoDetect: Boolean = true,
    val forcedType: ScreenType = ScreenType.PHONE,
    val tabletColumns: Int = 6,
    val foldableColumns: Int = 5,
    val landscapeColumns: Int = 8,
    val scaleUi: Float = 1.0f
)

@Serializable data class RepairSettings(val lastRepairTimestamp: Long = 0L, val crashCount: Int = 0)

@Serializable data class OnboardingState(
    val completed: Boolean = false,
    val currentStep: Int = 0,
    val lastSeenVersionCode: Int = 0 // drives the "What's New" dialog — see WhatsNewDialog
)

@Serializable data class WidgetSlot(
    val widgetId: Int,
    val pageId: String = "home",
    val positionIndex: Int = 0,
    val spanX: Int = 4,
    val spanY: Int = 2
)

@Serializable data class NewsSource(
    val id: String,
    val name: String,
    val url: String,
    val language: String = "he",
    val enabled: Boolean = true
) {
    companion object {
        val ALL_SOURCES = listOf(
            NewsSource("ynet","ynet","https://www.ynet.co.il/Integration/StoryRss2.xml"),
            NewsSource("walla","וואלה","https://rss.walla.co.il/feed/1"),
            NewsSource("google_il","Google News IL","https://news.google.com/rss?hl=iw&gl=IL&ceid=IL:he"),
            NewsSource("google_world","Google News","https://news.google.com/rss?hl=en&gl=US&ceid=US:en","en"),
            NewsSource("yahoo","Yahoo News","https://news.yahoo.com/rss/","en"),
            NewsSource("bbc","BBC News","https://feeds.bbci.co.uk/news/rss.xml","en"),
            NewsSource("cnn","CNN","http://rss.cnn.com/rss/edition.rss","en"),
            NewsSource("techcrunch","TechCrunch","https://techcrunch.com/feed/","en"),
            NewsSource("verge","The Verge","https://www.theverge.com/rss/index.xml","en"),
            NewsSource("haaretz","הארץ","https://www.haaretz.co.il/cmlink/1.1617539"),
            NewsSource("kan","כאן","https://www.kan.org.il/feed/"),
            NewsSource("calcalist","כלכליסט","https://www.calcalist.co.il/GeneralRSS/0,16335,L-8,00.xml"),
        )
    }
}

@Serializable data class NewsSettings(
    val enabledSourceIds: Set<String> = setOf("ynet","walla","google_il"),
    val maxArticles: Int = 50,
    val useEmbeddedApp: Boolean = false,
    val embeddedAppPackage: String = "com.google.android.googlequicksearchbox",
    val customSources: List<NewsSource> = emptyList() // user-added RSS feeds, beyond NewsSource.ALL_SOURCES
)
