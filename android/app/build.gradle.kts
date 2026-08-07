plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    jacoco
    alias(libs.plugins.ktlint)
}

// Release signing: opt-in via environment variables, never via committed secrets.
// Locally: set these in your shell profile (never in gradle.properties, which is
// tracked) before running `assembleRelease`/`bundleRelease`. In CI: the same names
// are populated from GitHub Secrets — see .github/workflows/android.yml. Missing
// or partial values fall back to today's behavior (an unsigned release build) so
// this is purely additive; nothing changes until someone actually sets a keystore.
val releaseKeystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
val releaseKeystorePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("RELEASE_KEY_ALIAS")
val releaseKeyPassword = System.getenv("RELEASE_KEY_PASSWORD")
val hasReleaseSigning = !releaseKeystorePath.isNullOrBlank() &&
    !releaseKeystorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank() &&
    file(releaseKeystorePath).exists()

android {
    namespace = "com.ailauncher.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ailauncher.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "9.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug { isDebuggable = true }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }

    // v9: lint is advisory, not a build gate. The codebase has correctly-guarded
    // API-29 calls (e.g. MediaStore.Downloads behind SDK_INT >= Q) that lint's
    // interprocedural analysis can't always verify across method boundaries —
    // those are annotated with @RequiresApi, but we don't want a future false
    // positive to break CI. The Android CI workflow still runs `lint` and uploads
    // the HTML report as an artefact for review.
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/INDEX.LIST"
        }
    }

    // v9: Robolectric needs Android resources on the unit-test classpath
    // (SettingsRepositoryTest exercises DataStore against a temp file).
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    // Compose BOM keeps the Compose libraries on matched versions.
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Navigation + Hilt navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.kotlinx.coroutines.android)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Security
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)

    implementation(libs.play.services.auth)

    // v9: androidx.glance:glance-appwidget removed — we are a host for third-party
    // widgets via AppWidgetHost (see ui/components/LauncherWidgetProvider), but we
    // don't provide any GlanceAppWidget of our own. Re-add when we ship one.

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // NOTE: TFLite was previously listed for a "future ML upgrade" but never wired up.
    // AppPredictionEngine is a pure-Kotlin heuristic. Re-add `org.tensorflow:tensorflow-lite`
    // when an actual model arrives — until then it just bloats the APK by ~3 MB.

    // Image loading
    implementation(libs.coil.compose)

    // v9: com.google.android.material removed — themes.xml uses android:Theme.Material
    // (framework) not Theme.MaterialComponents, and Compose UI uses Material 3
    // (compose-material3) directly. Re-add only if a BiometricPrompt theme or other
    // legacy widget surfaces a runtime error.

    // Permissions
    implementation(libs.accompanist.permissions)

    // Logging — Timber. DebugTree only planted in debug builds.
    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    // Robolectric + Compose UI testing (JVM only, no device/emulator needed).
    // ui-test-manifest must be debugImplementation, not testImplementation: it merges
    // a synthetic ComponentActivity into the debug variant's manifest, which is what
    // Robolectric resolves createComposeRule()'s host activity against.
    testImplementation(composeBom)
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(composeBom)
    debugImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

jacoco {
    toolVersion = "0.8.12"
}

// v9: advisory only (like AGP lint above) — the codebase predates ktlint and
// hasn't been auto-formatted, so a hard gate would fail on pre-existing code
// rather than catching new violations. CI runs `ktlintCheck` with
// continue-on-error, same policy as `lint`.
ktlint {
    version.set("1.3.1")
}

// v9: coverage report for testDebugUnitTest, uploaded as a CI artifact. Not a
// gate (no minimum % enforced) — just visibility into what the JVM test suite
// actually exercises.
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    group = "verification"
    description = "Generates a Jacoco HTML/XML coverage report from testDebugUnitTest."

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter = listOf(
        "**/R.class", "**/R\$*.class", "**/BuildConfig.*", "**/Manifest*.*",
        "**/*Test*.*", "android/**/*.*",
        "**/*_Hilt*.*", "**/Hilt_*.*", "**/*_Factory.*", "**/*_MembersInjector.*",
        "**/*Module_*Factory.*", "**/dagger/**/*.*", "**/*_ComponentTreeDeps.*",
        "**/hilt_aggregated_deps/**", "**/*\$serializer.*", "**/ComposableSingletons\$*.*"
    )
    val kotlinClasses = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") { exclude(fileFilter) }
    val javaClasses = fileTree("${layout.buildDirectory.get()}/intermediates/javac/debug/compileDebugJavaWithJavac/classes") { exclude(fileFilter) }

    sourceDirectories.setFrom(files("$projectDir/src/main/java", "$projectDir/src/main/kotlin"))
    classDirectories.setFrom(files(kotlinClasses, javaClasses))
    executionData.setFrom(fileTree(layout.buildDirectory.get()) { include("jacoco/testDebugUnitTest.exec") })
}
