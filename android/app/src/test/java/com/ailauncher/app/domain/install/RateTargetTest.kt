package com.ailauncher.app.domain.install

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class RateTargetTest {
    @Test
    fun `Google Play keeps the market link with the web page as fallback`() {
        assertEquals(
            RateTarget(
                "market://details?id=com.ailauncher.app",
                "https://play.google.com/store/apps/details?id=com.ailauncher.app",
            ),
            RateTarget.forInstaller("com.android.vending"),
        )
    }

    @Test
    fun `Galaxy Store opens its own app, then its web page`() {
        assertEquals(
            RateTarget(
                "samsungapps://ProductDetail/com.ailauncher.app",
                "https://galaxystore.samsung.com/detail/com.ailauncher.app",
            ),
            RateTarget.forInstaller("com.sec.android.app.samsungapps"),
        )
    }

    @Test
    fun `AppGallery opens its own app, with no web fallback`() {
        assertEquals(
            RateTarget("appmarket://details?id=com.ailauncher.app"),
            RateTarget.forInstaller("com.huawei.appmarket"),
        )
    }

    @Test
    fun `Aptoide and APKPure open their app page`() {
        assertEquals(RateTarget("aptoideinstall://package=com.ailauncher.app"), RateTarget.forInstaller("cm.aptoide.pt"))
        assertEquals(
            RateTarget("https://apkpure.com/store/apps/details?id=com.ailauncher.app"),
            RateTarget.forInstaller("com.apkpure.aegon"),
        )
    }

    @Test
    fun `Uptodown has no stable app link, so the button is hidden`() {
        assertNull(RateTarget.forInstaller("com.uptodown"))
    }

    @Test
    fun `installs with no store hide the button`() {
        assertNull(RateTarget.forInstaller(null))
        assertNull(RateTarget.forInstaller(""))
        assertNull(RateTarget.forInstaller("com.android.shell"))
        assertNull(RateTarget.forInstaller("com.google.android.packageinstaller"))
        assertNull(RateTarget.forInstaller("dev.imranr.obtainium"))
        assertNull(RateTarget.forInstaller("com.amazon.venezia"))
    }

    @Test
    fun `a store never links to Google Play`() {
        (InstallSource.STORE_INSTALLERS - InstallSource.GOOGLE_PLAY).forEach { store ->
            val target = RateTarget.forInstaller(store) ?: return@forEach
            listOfNotNull(target.uri, target.fallbackUri).forEach { link ->
                assertFalse("$store → $link", link.startsWith("market:") || "play.google.com" in link)
            }
        }
    }
}
