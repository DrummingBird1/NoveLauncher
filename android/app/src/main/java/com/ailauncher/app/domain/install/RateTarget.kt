package com.ailauncher.app.domain.install

/**
 * Where Settings → About's Rate button sends the user: the page of the store that
 * installed the app, never another store's. Galaxy Store and AppGallery reviews
 * reject apps that link to a competing store, and a review left there wouldn't
 * count for the store the user actually got the app from. Pure — the installer
 * package comes from [com.ailauncher.app.data.install.installerPackage].
 *
 * [uri] is tried first. [fallbackUri], if any, is opened when no app takes [uri]
 * (the store app was disabled or removed after installing us).
 */
data class RateTarget(
    val uri: String,
    val fallbackUri: String? = null,
) {
    companion object {
        /** applicationId, the same in every store. Debug builds don't add a suffix. */
        const val APP_ID = "com.ailauncher.app"
        private const val PLAY_WEB = "https://play.google.com/store/apps/details?id=$APP_ID"
        private const val GALAXY_WEB = "https://galaxystore.samsung.com/detail/$APP_ID"

        /**
         * Null hides the button. That's Uptodown — its app pages are addressed by
         * a slug Uptodown picks from the app name, not by package name, so there's
         * no link we can know in advance — and every install with no store
         * (GitHub, Obtainium, adb, an unknown installer): GitHub has no ratings,
         * and "Source code" right above already opens the repo.
         */
        fun forInstaller(installerPackage: String?): RateTarget? =
            when (installerPackage) {
                // What the button always did: Play's own scheme, then its web page.
                InstallSource.GOOGLE_PLAY -> RateTarget("market://details?id=$APP_ID", PLAY_WEB)

                InstallSource.GALAXY_STORE -> RateTarget("samsungapps://ProductDetail/$APP_ID", GALAXY_WEB)

                // AppGallery's web page is keyed by an app id it assigns ("C1234…"),
                // not the package name, so there's no web fallback.
                InstallSource.APPGALLERY -> RateTarget("appmarket://details?id=$APP_ID")

                // The link Aptoide's own store-link-mapper hands out for its client;
                // it opens the app page (no install pop-up by default). The web page
                // is slug-based like Uptodown's, so no fallback.
                InstallSource.APTOIDE -> RateTarget("aptoideinstall://package=$APP_ID")

                // APKPure serves the app page at its Play-style URL (404 for an
                // unknown package), in its app or the browser.
                InstallSource.APKPURE -> RateTarget("https://apkpure.com/store/apps/details?id=$APP_ID")

                else -> null
            }
    }
}
