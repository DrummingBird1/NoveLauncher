pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AILauncher"
include(":app")
// v10: Baseline Profile generator + startup Macrobenchmark — see baselineprofile/build.gradle.kts.
include(":baselineprofile")
