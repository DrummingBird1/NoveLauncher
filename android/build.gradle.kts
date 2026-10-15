// v9: plugin declarations migrated to gradle/libs.versions.toml. Single source of
// truth for AGP/Kotlin/KSP/Hilt versions — see [plugins] in libs.versions.toml.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.androidx.baselineprofile) apply false
    // v10: never applied to a module (AGP 9 compiles Kotlin itself), but declaring
    // it here puts this Kotlin Gradle Plugin version on the build classpath, which
    // is how AGP 9's built-in Kotlin picks its compiler version. Remove this line
    // and AGP silently falls back to the older KGP it bundles.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint) apply false
}
