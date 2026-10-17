package com.ailauncher.app.domain.install

/**
 * Which app store, if any, installed this APK. Pure — the installer's package
 * name itself is read from PackageManager by the caller
 * ([com.ailauncher.app.data.install.installerPackage]).
 *
 * The same signed APK ships through GitHub Releases, Obtainium, Google Play and
 * the alternative stores. A store updates the app itself, and store review
 * policies forbid pointing users to updates anywhere else, so the GitHub update
 * check switches off when a store is the installer of record. Everything else —
 * no installer (adb), the system package installer (a downloaded APK),
 * Obtainium (`dev.imranr.obtainium*`) — keeps it.
 */
object InstallSource {
    const val GOOGLE_PLAY = "com.android.vending"
    const val GALAXY_STORE = "com.sec.android.app.samsungapps"
    const val APPGALLERY = "com.huawei.appmarket"
    const val APTOIDE = "cm.aptoide.pt"
    const val UPTODOWN = "com.uptodown"
    const val APKPURE = "com.apkpure.aegon"

    val STORE_INSTALLERS = setOf(GOOGLE_PLAY, GALAXY_STORE, APPGALLERY, APTOIDE, UPTODOWN, APKPURE)

    fun isStoreInstall(installerPackage: String?): Boolean = installerPackage in STORE_INSTALLERS
}
