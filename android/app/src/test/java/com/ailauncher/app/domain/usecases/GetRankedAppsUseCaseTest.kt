package com.ailauncher.app.domain.usecases

import com.ailauncher.app.data.AppCategoryProvider
import com.ailauncher.app.data.ContextSignals
import com.ailauncher.app.data.DeviceContext
import com.ailauncher.app.data.InstalledAppsRepository
import com.ailauncher.app.data.JewishCalendarService
import com.ailauncher.app.data.SettingsRepository
import com.ailauncher.app.data.UsageStatsRepository
import com.ailauncher.app.data.db.NotificationDao
import com.ailauncher.app.data.db.NotificationEntity
import com.ailauncher.app.data.ml.AppPredictionEngine
import com.ailauncher.app.data.ml.SuggestionFeedbackRepository
import com.ailauncher.app.domain.models.AppCategory
import com.ailauncher.app.domain.models.AppInfo
import com.ailauncher.app.domain.models.AppProfile
import com.ailauncher.app.domain.models.AppearanceSettings
import com.ailauncher.app.domain.models.RankedApp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Covers GetRankedAppsUseCase.execute() itself — AppCategoryProviderTest already
 * covers the scoring math it delegates to, but not the notification-badge
 * suppression logic layered on top (see Pitfall #17 in CLAUDE.md: zero counts
 * when suppressed, not filtered-at-render).
 */
class GetRankedAppsUseCaseTest {

    private val installedAppsRepo = mockk<InstalledAppsRepository>()
    private val usageStatsRepo = mockk<UsageStatsRepository>()
    private val categoryProvider = mockk<AppCategoryProvider>()
    private val notificationDao = mockk<NotificationDao>()
    private val predictionEngine = mockk<AppPredictionEngine>(relaxed = true)
    private val settingsRepo = mockk<SettingsRepository>()
    private val feedbackRepo = mockk<SuggestionFeedbackRepository>()
    private val contextSignals = mockk<ContextSignals>()
    private val jewishCalendar = mockk<JewishCalendarService>()

    private lateinit var useCase: GetRankedAppsUseCase

    private val appInfo = AppInfo(
        packageName = "com.example.app",
        label = "Example",
        icon = null,
        category = AppCategory.UTILITIES,
        isSystemApp = false,
        installTime = 0L,
        lastUpdateTime = 0L
    )

    private val rankedApp = RankedApp(
        app = appInfo,
        weightScore = 0.5f,
        usageMinutesToday = 0L,
        launchCountToday = 0,
        lastUsedTimestamp = 0L,
        smartFolder = AppCategory.UTILITIES
    )

    @Before
    fun setUp() {
        useCase = GetRankedAppsUseCase(
            installedAppsRepo, usageStatsRepo, categoryProvider, notificationDao, predictionEngine, settingsRepo,
            feedbackRepo, contextSignals, jewishCalendar
        )
        every { usageStatsRepo.hasPermission() } returns true
        coEvery { installedAppsRepo.getInstalledApps(any()) } returns listOf(appInfo)
        every { installedAppsRepo.getProfiles() } returns emptyList()
        coEvery { usageStatsRepo.getUsageSnapshots() } returns emptyMap()
        every { categoryProvider.rankApps(any(), any(), any(), any()) } returns listOf(rankedApp)
        every { categoryProvider.autoGroup(any()) } answers { emptyList() }
        coEvery { feedbackRepo.counts(any()) } returns emptyMap()
        every { contextSignals.current() } returns DeviceContext()
        every { jewishCalendar.isShabbatModeActive(any(), any()) } returns false
    }

    @Test
    fun `notification counts are zero when badges are turned off`() = runTest {
        every { settingsRepo.appearanceFlow } returns flowOf(AppearanceSettings(showNotificationBadges = false))
        coEvery { notificationDao.getAppsWithUnread() } returns listOf("com.example.app")
        coEvery { notificationDao.get("com.example.app") } returns NotificationEntity("com.example.app", unreadCount = 3)

        val result = useCase.execute()

        assertEquals(0, result.rankedApps.first().notificationCount)
    }

    @Test
    fun `notification counts are zero while snoozed`() = runTest {
        val future = System.currentTimeMillis() + 3_600_000L
        every { settingsRepo.appearanceFlow } returns
            flowOf(AppearanceSettings(showNotificationBadges = true, badgeSnoozedUntil = future))

        val result = useCase.execute()

        assertEquals(0, result.rankedApps.first().notificationCount)
        coVerify(exactly = 0) { notificationDao.getAppsWithUnread() }
    }

    @Test
    fun `notification counts pass through when badges are enabled and not snoozed`() = runTest {
        every { settingsRepo.appearanceFlow } returns
            flowOf(AppearanceSettings(showNotificationBadges = true, badgeSnoozedUntil = 0L))
        coEvery { notificationDao.getAppsWithUnread() } returns listOf("com.example.app")
        coEvery { notificationDao.get("com.example.app") } returns NotificationEntity("com.example.app", unreadCount = 3)

        val result = useCase.execute()

        assertEquals(3, result.rankedApps.first().notificationCount)
    }

    @Test
    fun `usage snapshots are skipped entirely without permission`() = runTest {
        every { settingsRepo.appearanceFlow } returns flowOf(AppearanceSettings())
        every { usageStatsRepo.hasPermission() } returns false

        val result = useCase.execute()

        assertEquals(false, result.hasUsagePermission)
        coVerify(exactly = 0) { usageStatsRepo.getUsageSnapshots() }
    }

    @Test
    fun `notification counts are zero while Shabbat mode is active`() = runTest {
        every { settingsRepo.appearanceFlow } returns flowOf(AppearanceSettings(showNotificationBadges = true))
        every { jewishCalendar.isShabbatModeActive(any(), any()) } returns true

        val result = useCase.execute()

        assertEquals(0, result.rankedApps.first().notificationCount)
        coVerify(exactly = 0) { notificationDao.getAppsWithUnread() }
    }

    @Test
    fun `private space apps are kept out of suggestions and folders`() = runTest {
        every { settingsRepo.appearanceFlow } returns flowOf(AppearanceSettings(showNotificationBadges = false))
        val privateApp = rankedApp.copy(app = appInfo.copy(packageName = "com.secret", profile = AppProfile.PRIVATE, userSerial = 11))
        every { categoryProvider.rankApps(any(), any(), any(), any()) } returns listOf(rankedApp, privateApp)

        val result = useCase.execute()

        assertEquals(listOf("com.example.app"), result.rankedApps.map { it.app.packageName })
        assertEquals(listOf("com.secret"), result.privateSpaceApps.map { it.app.packageName })
        io.mockk.verify { categoryProvider.autoGroup(listOf(rankedApp)) }
    }

    @Test
    fun `the selected icon pack is passed to the app query`() = runTest {
        every { settingsRepo.appearanceFlow } returns flowOf(AppearanceSettings(iconPackPackage = "com.pack", showNotificationBadges = false))

        useCase.execute()

        coVerify { installedAppsRepo.getInstalledApps("com.pack") }
    }
}
