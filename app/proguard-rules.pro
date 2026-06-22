# ── kotlinx.serialization ────────────────────────────────────────────
# Canonical rules (https://github.com/Kotlin/kotlinx.serialization#android).
# Our @Serializable models live in domain.models (settings) + data.api
# (WeatherData), so the generic @Serializable-aware rules below cover every
# module, not just one package.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Keep the synthetic $serializer for every @Serializable class.
-if @kotlinx.serialization.Serializable class **
-keep class <1>$$serializer { *; }

# Keep Companion + serializer() accessor on @Serializable types.
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}
-keepclassmembers class * implements kotlinx.serialization.KSerializer { *; }

# Belt-and-suspenders: keep the settings model package wholesale (small, and these
# are the JSON shape of backup/restore payloads — never worth shrinking).
-keep class com.ailauncher.app.domain.models.** { *; }

# ── Misc ─────────────────────────────────────────────────────────────
-dontwarn kotlin.**
