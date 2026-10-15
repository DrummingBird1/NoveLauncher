// v10: generates the app's Baseline Profile and measures cold start.
//
//   ./gradlew :app:generateBaselineProfile        → writes app/src/main/generated/baselineProfiles/
//   ./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest   → StartupBenchmark
//
// Both need a connected device or emulator (API 28+; API 33+ needs no root).
// A launcher is cold-started every time the system kills it and the user
// presses Home, so the startup path is exactly what a profile should cover.
plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.ailauncher.baselineprofile"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    targetProjectPath = ":app"
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
