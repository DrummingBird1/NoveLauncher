plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.ailauncher.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ailauncher.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "8.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug { isDebuggable = true }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/INDEX.LIST"
        }
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
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
