package com.ailauncher.app.ui.screens

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.runtime.CompositionLocalProvider
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.ailauncher.app.data.IconCache
import com.ailauncher.app.domain.models.AppCategory
import com.ailauncher.app.domain.models.AppInfo
import com.ailauncher.app.domain.models.IconShape
import com.ailauncher.app.domain.models.LauncherFont
import com.ailauncher.app.domain.models.RankedApp
import com.ailauncher.app.ui.LocalIconCache
import com.ailauncher.app.ui.theme.AILauncherTheme
import org.junit.Rule
import org.junit.Test

/**
 * JVM-only screenshot test (renders via layoutlib, no device needed). Guards
 * the icon-shape rendering and the notification badge (HomeScreen.kt) against
 * silent visual regressions — neither is exercised by any existing unit test.
 * Goldens live in src/test/snapshots/; regenerate with `recordPaparazziDebug`
 * after an intentional visual change, `verifyPaparazziDebug` catches accidental ones.
 */
class HomeAppItemSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private fun rankedApp(label: String, notificationCount: Int = 0) = RankedApp(
        app = AppInfo(
            packageName = "com.example.$label",
            label = label,
            icon = ColorDrawable(Color.parseColor("#7C7CFF")),
            category = AppCategory.UTILITIES,
            isSystemApp = false,
            installTime = 0L,
            lastUpdateTime = 0L
        ),
        weightScore = 0.5f,
        usageMinutesToday = 0L,
        launchCountToday = 0,
        lastUsedTimestamp = 0L,
        smartFolder = AppCategory.UTILITIES,
        notificationCount = notificationCount
    )

    @Test
    fun `home app item without notification badge`() {
        paparazzi.snapshot {
            AILauncherTheme {
                CompositionLocalProvider(LocalIconCache provides IconCache(paparazzi.context)) {
                    HomeAppItem(
                        rankedApp = rankedApp("Camera"),
                        iconSizeDp = 56,
                        fontSizeSp = 12,
                        iconShape = IconShape.ROUNDED_SQUARE,
                        font = LauncherFont.SYSTEM_DEFAULT,
                        onClick = {}
                    )
                }
            }
        }
    }

    @Test
    fun `home app item with notification badge shows count`() {
        paparazzi.snapshot {
            AILauncherTheme {
                CompositionLocalProvider(LocalIconCache provides IconCache(paparazzi.context)) {
                    HomeAppItem(
                        rankedApp = rankedApp("Messages", notificationCount = 5),
                        iconSizeDp = 56,
                        fontSizeSp = 12,
                        iconShape = IconShape.CIRCLE,
                        font = LauncherFont.SYSTEM_DEFAULT,
                        onClick = {}
                    )
                }
            }
        }
    }
}
