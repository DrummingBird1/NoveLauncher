package com.ailauncher.app.domain.install

/**
 * Which app store, if any, installed this APK. Pure — the installer's package
 * name itself is read from PackageManager by the caller (UpdateChecker).
 *
 * The same signed APK ships through GitHub Releases, Obtainium, Google Play and
 * the alternative stores. A store updates the app itself, and store review
 * policies forbid pointing users to updates anywhere else, so the GitHub update
 * check switches off when a store is the installer of record. Everything else —
 * no installer (adb), the system package installer (a downloaded APK),
 * Obtainium (`dev.imranr.obtainium*`) — keeps it.
 */
object InstallSource {
    val STORE_INSTALLERS =
        setOf(
            "com.android.vending", // Google Play
            "com.sec.android.app.samsungapps", // Samsung Galaxy Store
            "com.huawei.appmarket", // Huawei AppGallery
            "cm.aptoide.pt", // Aptoide
            "com.uptodown", // Uptodown
            "com.apkpure.aegon", // APKPure
        )

    fun isStoreInstall(installerPackage: String?): Boolean = installerPackage in STORE_INSTALLERS
}
