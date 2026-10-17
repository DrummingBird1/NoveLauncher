package com.ailauncher.app.domain.install

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallSourceTest {
    @Test
    fun `every store we ship to counts as a store install`() {
        listOf(
            "com.android.vending",
            "com.sec.android.app.samsungapps",
            "com.huawei.appmarket",
            "cm.aptoide.pt",
            "com.uptodown",
            "com.apkpure.aegon",
        ).forEach { assertTrue(it, InstallSource.isStoreInstall(it)) }
    }

    @Test
    fun `adb and sideloaded installs keep the update check`() {
        assertFalse(InstallSource.isStoreInstall(null))
        assertFalse(InstallSource.isStoreInstall(""))
        assertFalse(InstallSource.isStoreInstall("com.android.shell"))
        assertFalse(InstallSource.isStoreInstall("com.google.android.packageinstaller"))
        assertFalse(InstallSource.isStoreInstall("com.android.packageinstaller"))
        assertFalse(InstallSource.isStoreInstall("com.samsung.android.packageinstaller"))
    }

    @Test
    fun `Obtainium keeps the update check`() {
        assertFalse(InstallSource.isStoreInstall("dev.imranr.obtainium"))
        assertFalse(InstallSource.isStoreInstall("dev.imranr.obtainium.fdroid"))
    }

    @Test
    fun `only exact package names match`() {
        assertFalse(InstallSource.isStoreInstall("com.android.vending.fake"))
        assertFalse(InstallSource.isStoreInstall("COM.ANDROID.VENDING"))
    }
}
