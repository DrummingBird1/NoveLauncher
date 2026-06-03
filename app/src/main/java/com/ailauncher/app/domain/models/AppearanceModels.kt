package com.ailauncher.app.domain.models

import android.content.Context
import androidx.annotation.StringRes
import com.ailauncher.app.R
import kotlinx.serialization.Serializable

/**
 * Theme / icon / clock / wallpaper / search-bar settings. Everything visual.
 */

@Serializable enum class ThemeMode(@StringRes val displayNameRes: Int) {
    DARK(R.string.theme_mode_dark),
    LIGHT(R.string.theme_mode_light),
    SYSTEM(R.string.theme_mode_system)
}

/**
 * v9: Hybrid name. Built-in presets carry [nameRes]; if a future API lets users
 * create custom presets, leave [nameRes] null and the user-typed [name] is shown.
 */
@Serializable
data class ThemePreset(
    val id: String,
    val name: String,
    val primaryColor: String,
    val secondaryColor: String,
    val backgroundColor: String,
    val surfaceColor: String,
    val onBackgroundColor: String,
    val onSurfaceColor: String,
    val accentColor: String,
    @StringRes val nameRes: Int? = null
) {
    fun localizedName(context: Context): String =
        nameRes?.let { context.getString(it) } ?: name

    companion object {
        val PRESETS = listOf(
            ThemePreset("indigo", "Indigo", "#7C7CFF", "#4ECDC4", "#0A0A0F", "#14141F", "#FFFFFF", "#E0E0E8", "#7C7CFF", R.string.theme_preset_indigo),
            ThemePreset("crimson", "Crimson", "#FF4757", "#FF6B81", "#0F0A0A", "#1F1414", "#FFFFFF", "#E8E0E0", "#FF4757", R.string.theme_preset_crimson),
            ThemePreset("emerald", "Emerald", "#2ED573", "#7BED9F", "#0A0F0A", "#141F14", "#FFFFFF", "#E0E8E0", "#2ED573", R.string.theme_preset_emerald),
            ThemePreset("amber", "Amber", "#FFA502", "#FFD93D", "#0F0E0A", "#1F1D14", "#FFFFFF", "#E8E6E0", "#FFA502", R.string.theme_preset_amber),
            ThemePreset("ocean", "Ocean", "#3742FA", "#70A1FF", "#0A0A0F", "#14141F", "#FFFFFF", "#E0E0E8", "#3742FA", R.string.theme_preset_ocean),
            ThemePreset("rose", "Rose", "#FF6B9D", "#FF85B1", "#0F0A0C", "#1F141A", "#FFFFFF", "#E8E0E4", "#FF6B9D", R.string.theme_preset_rose),
            ThemePreset("slate", "Slate", "#778899", "#A4B0BE", "#0E0E10", "#1C1C20", "#FFFFFF", "#E0E0E2", "#778899", R.string.theme_preset_slate),
            ThemePreset("lavender", "Lavender", "#A78BFA", "#C4B5FD", "#0C0A0F", "#1A141F", "#FFFFFF", "#E4E0E8", "#A78BFA", R.string.theme_preset_lavender),
            ThemePreset("teal", "Teal", "#20C997", "#63E6BE", "#0A0F0E", "#141F1C", "#FFFFFF", "#E0E8E6", "#20C997", R.string.theme_preset_teal),
            ThemePreset("sunset", "Sunset", "#FF6348", "#FF7F50", "#0F0B0A", "#1F1814", "#FFFFFF", "#E8E4E0", "#FF6348", R.string.theme_preset_sunset),
            ThemePreset("midnight", "Midnight", "#2C3E50", "#34495E", "#060809", "#0E1218", "#FFFFFF", "#D8DCE0", "#5DADE2", R.string.theme_preset_midnight),
            ThemePreset("snow", "Snow", "#4A90D9", "#6BB3E0", "#F5F5FA", "#FFFFFF", "#1A1A2E", "#2A2A3E", "#4A90D9", R.string.theme_preset_snow),
        )
        fun findById(id: String) = PRESETS.find { it.id == id } ?: PRESETS[0]
    }
}

// v5: Added more icon shapes
@Serializable enum class IconShape(@StringRes val displayNameRes: Int) {
    ROUNDED_SQUARE(R.string.icon_shape_rounded_square),
    CIRCLE(R.string.icon_shape_circle),
    SQUIRCLE(R.string.icon_shape_squircle),
    TEARDROP(R.string.icon_shape_teardrop),
    HEXAGON(R.string.icon_shape_hexagon),
    SQUARE(R.string.icon_shape_square),
    DIAMOND(R.string.icon_shape_diamond),
    SHIELD(R.string.icon_shape_shield),
    LEAF(R.string.icon_shape_leaf),
    BLOB(R.string.icon_shape_blob),
    CLOVER(R.string.icon_shape_clover),
    OCTAGON(R.string.icon_shape_octagon)
}

@Serializable enum class LauncherFont(@StringRes val displayNameRes: Int, val fontFamily: String) {
    SYSTEM_DEFAULT(R.string.font_system_default, "sans-serif"),
    ROBOTO(R.string.font_roboto, "sans-serif"),
    NOTO_SANS(R.string.font_noto_sans, "noto-sans"),
    RUBIK(R.string.font_rubik, "rubik"),
    HEEBO(R.string.font_heebo, "heebo"),
    ASSISTANT(R.string.font_assistant, "assistant"),
    VARELA_ROUND(R.string.font_varela_round, "varela-round"),
    OPEN_SANS(R.string.font_open_sans, "open-sans"),
    MONTSERRAT(R.string.font_montserrat, "montserrat"),
    POPPINS(R.string.font_poppins, "poppins"),
    CAIRO(R.string.font_cairo, "cairo"),
    FRANK_RUHL(R.string.font_frank_ruhl, "frank-ruhl"),
    SECULAR_ONE(R.string.font_secular_one, "secular-one"),
    MONOSPACE(R.string.font_monospace, "monospace"),
    SERIF(R.string.font_serif, "serif"),
    CURSIVE(R.string.font_cursive, "cursive")
}

// v5: Analog clock option + more settings
@Serializable enum class ClockStyle(@StringRes val displayNameRes: Int) {
    DIGITAL(R.string.clock_style_digital),
    ANALOG(R.string.clock_style_analog),
    MINIMAL(R.string.clock_style_minimal)
}

@Serializable
data class ClockSettings(
    val fontSize: Int = 64, val fontColor: String = "#FFFFFF", val showDate: Boolean = true,
    val dateFontSize: Int = 16, val use24Hour: Boolean = true, val showSeconds: Boolean = false,
    val font: LauncherFont = LauncherFont.SYSTEM_DEFAULT, val opacity: Float = 1f,
    val showSearch: Boolean = true, val showReminders: Boolean = false,
    val clockStyle: ClockStyle = ClockStyle.DIGITAL,
    val analogSize: Int = 160, val analogColor: String = "#FFFFFF",
    val analogAccentColor: String = "#7C7CFF"
)

@Serializable
data class SearchBarSettings(
    val visible: Boolean = true, val heightDp: Int = 48, val cornerRadius: Int = 24,
    val showMicButton: Boolean = true, val showCameraButton: Boolean = false,
    val backgroundColor: String = "", val textColor: String = ""
)

@Serializable enum class WallpaperMode(@StringRes val displayNameRes: Int) {
    SINGLE(R.string.wallpaper_mode_single),
    PER_PAGE(R.string.wallpaper_mode_per_page),
    HOURLY(R.string.wallpaper_mode_hourly),
    DAILY(R.string.wallpaper_mode_daily),
    LIVE(R.string.wallpaper_mode_live)
}

@Serializable
data class WallpaperEntry(
    val id: String = "", val uri: String = "", val brightness: Float = 1f,
    val blur: Float = 0f, val saturation: Float = 1f, val opacity: Float = 1f,
    val contrast: Float = 1f, val warmth: Float = 0f, val vignette: Float = 0f,
    val page: String = "", val hour: Int = -1, val dayOfWeek: Int = -1,
    val isBuiltIn: Boolean = false, val builtInId: String = ""
)

// v5: Built-in wallpapers.
// v9: Names switched to English base — this feature isn't wired up to the UI yet
// (no Composable consumes BuiltInWallpaper.ALL), so we avoid the overhead of
// adding 12 × 5 string-resource keys. If/when a wallpaper picker ships, switch
// to the same hybrid pattern as ThemePreset (add nameRes + localizedName helper).
@Serializable
data class BuiltInWallpaper(val id: String, val name: String, val colors: List<String>, val type: String = "gradient") {
    companion object {
        val ALL = listOf(
            BuiltInWallpaper("midnight_aurora", "Midnight aurora", listOf("#0F0C29","#302B63","#24243E")),
            BuiltInWallpaper("ocean_deep", "Deep ocean", listOf("#000428","#004E92")),
            BuiltInWallpaper("sunset_warm", "Warm sunset", listOf("#f12711","#f5af19")),
            BuiltInWallpaper("forest_mist", "Forest mist", listOf("#0B8793","#360033")),
            BuiltInWallpaper("lavender_dream", "Lavender dream", listOf("#a18cd1","#fbc2eb")),
            BuiltInWallpaper("arctic_blue", "Arctic blue", listOf("#2193b0","#6dd5ed")),
            BuiltInWallpaper("volcanic", "Volcanic", listOf("#1a1a2e","#e94560")),
            BuiltInWallpaper("emerald_night", "Emerald night", listOf("#0d1117","#238636")),
            BuiltInWallpaper("rose_gold", "Rose gold", listOf("#f4c4f3","#fc67fa")),
            BuiltInWallpaper("charcoal", "Charcoal", listOf("#141E30","#243B55")),
            BuiltInWallpaper("sahara", "Sahara", listOf("#c2956b","#8b6914","#4a3728")),
            BuiltInWallpaper("cyber_punk", "Cyberpunk", listOf("#0a0a0f","#ff00ff","#00ffff"), "mesh"),
        )
    }
}

@Serializable
data class WallpaperSettings(
    val mode: WallpaperMode = WallpaperMode.SINGLE, val entries: List<WallpaperEntry> = emptyList(),
    val dimOnScroll: Boolean = false, val lockScreenSameAsHome: Boolean = true,
    val lockScreenWallpaper: WallpaperEntry? = null
)

@Serializable data class LockScreenSettings(
    val customEnabled: Boolean = false, val showClock: Boolean = true,
    val alwaysOnDisplay: Boolean = false, val aodBrightness: Float = 0.3f,
    val clockFont: LauncherFont = LauncherFont.SYSTEM_DEFAULT, val clockColor: String = "#FFFFFF"
)

// v5: max gridColumns = 8
@Serializable data class AppearanceSettings(
    val iconSizeDp: Int = 56, val fontSizeSp: Int = 12, val gridColumns: Int = 4,
    val folderIconSizeDp: Int = 28, val folderFontSizeSp: Int = 14,
    val iconShape: IconShape = IconShape.ROUNDED_SQUARE,
    val appFont: LauncherFont = LauncherFont.SYSTEM_DEFAULT, val folderFont: LauncherFont = LauncherFont.SYSTEM_DEFAULT,
    val pageFont: LauncherFont = LauncherFont.SYSTEM_DEFAULT, val uiFont: LauncherFont = LauncherFont.SYSTEM_DEFAULT,
    val themeMode: ThemeMode = ThemeMode.DARK, val themePresetId: String = "indigo",
    val customPrimaryColor: String = "#7C7CFF", val customBackgroundColor: String = "#0A0A0F",
    val customFontColor: String = "#FFFFFF", val useCustomColors: Boolean = false,
    val clock: ClockSettings = ClockSettings(), val wallpaper: WallpaperSettings = WallpaperSettings(),
    val lockScreen: LockScreenSettings = LockScreenSettings(), val searchBar: SearchBarSettings = SearchBarSettings(),
    val sortAppsByCategory: Boolean = true, val showNotificationBadges: Boolean = true,
    val categoryTabs: CategoryTabSettings = CategoryTabSettings(),
    val appLanguage: String = "" // empty = system default
)
