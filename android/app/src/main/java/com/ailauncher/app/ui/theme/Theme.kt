package com.ailauncher.app.ui.theme

import android.app.Activity
import android.content.ContextWrapper
import android.os.Build
import androidx.annotation.FontRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import com.ailauncher.app.R
import com.ailauncher.app.domain.models.AppearanceSettings
import com.ailauncher.app.domain.models.LauncherFont
import com.ailauncher.app.domain.models.ThemeMode
import com.ailauncher.app.domain.models.ThemePreset

fun parseHexColor(
    hex: String,
    fallback: Long = 0xFF7C7CFF,
): Color =
    try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(fallback)
    }

/**
 * The bundled fonts live in res/font, built by assets/fonts/prepare_fonts.py.
 * Most are variable fonts with a weight axis, so one file serves every weight:
 * each [Font] entry pins `wght` explicitly, because several files' default
 * instance is not Regular (Rubik's is Light, Assistant's ExtraLight, Montserrat's
 * Thin). Up to 10.0.0 these entries asked Typeface.create for names the system
 * doesn't have and silently got the default font.
 */
private val variableWeights = listOf(FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

private fun variableFamily(
    @FontRes res: Int,
): FontFamily =
    FontFamily(
        variableWeights.map { w ->
            Font(res, w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)))
        },
    )

private val heebo = variableFamily(R.font.heebo)
private val rubik = variableFamily(R.font.rubik)
private val assistant = variableFamily(R.font.assistant)
private val openSans = variableFamily(R.font.open_sans)
private val montserrat = variableFamily(R.font.montserrat)
private val cairo = variableFamily(R.font.cairo)
private val frankRuhl = variableFamily(R.font.frank_ruhl_libre)
private val notoSans = variableFamily(R.font.noto_sans)
private val poppins =
    FontFamily(
        Font(R.font.poppins_light, FontWeight.Light),
        Font(R.font.poppins_regular, FontWeight.Normal),
        Font(R.font.poppins_bold, FontWeight.Bold),
    )

// Single-weight families: Compose synthesises bold from them when asked.
private val varelaRound = FontFamily(Font(R.font.varela_round))
private val secularOne = FontFamily(Font(R.font.secular_one))

fun launcherFontFamily(font: LauncherFont): FontFamily =
    when (font) {
        LauncherFont.SYSTEM_DEFAULT -> FontFamily.Default
        LauncherFont.ROBOTO -> FontFamily.SansSerif
        LauncherFont.MONOSPACE -> FontFamily.Monospace
        LauncherFont.SERIF -> FontFamily.Serif
        LauncherFont.CURSIVE -> FontFamily.Cursive
        LauncherFont.NOTO_SANS -> notoSans
        LauncherFont.RUBIK -> rubik
        LauncherFont.HEEBO -> heebo
        LauncherFont.ASSISTANT -> assistant
        LauncherFont.VARELA_ROUND -> varelaRound
        LauncherFont.OPEN_SANS -> openSans
        LauncherFont.MONTSERRAT -> montserrat
        LauncherFont.POPPINS -> poppins
        LauncherFont.CAIRO -> cairo
        LauncherFont.FRANK_RUHL -> frankRuhl
        LauncherFont.SECULAR_ONE -> secularOne
    }

/**
 * [AppearanceSettings.uiFont] for every Material text style — the settings
 * screens, dialogs, sheets and any Text without its own fontFamily. App names,
 * folders, page names and the clock keep their own font settings.
 */
internal fun launcherTypography(uiFont: LauncherFont): Typography {
    val base = Typography()
    if (uiFont == LauncherFont.SYSTEM_DEFAULT) return base
    val f = launcherFontFamily(uiFont)
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = f),
        displayMedium = base.displayMedium.copy(fontFamily = f),
        displaySmall = base.displaySmall.copy(fontFamily = f),
        headlineLarge = base.headlineLarge.copy(fontFamily = f),
        headlineMedium = base.headlineMedium.copy(fontFamily = f),
        headlineSmall = base.headlineSmall.copy(fontFamily = f),
        titleLarge = base.titleLarge.copy(fontFamily = f),
        titleMedium = base.titleMedium.copy(fontFamily = f),
        titleSmall = base.titleSmall.copy(fontFamily = f),
        bodyLarge = base.bodyLarge.copy(fontFamily = f),
        bodyMedium = base.bodyMedium.copy(fontFamily = f),
        bodySmall = base.bodySmall.copy(fontFamily = f),
        labelLarge = base.labelLarge.copy(fontFamily = f),
        labelMedium = base.labelMedium.copy(fontFamily = f),
        labelSmall = base.labelSmall.copy(fontFamily = f),
    )
}

@Composable
fun AILauncherTheme(
    appearance: AppearanceSettings = AppearanceSettings(),
    content: @Composable () -> Unit,
) {
    val isDark =
        when (appearance.themeMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }

    // Material You: pulled from the system wallpaper on API 31+. Only makes sense
    // when the user isn't already asking for a specific custom palette, and only
    // where the platform actually provides it.
    val canUseDynamicColor =
        appearance.useDynamicColor && !appearance.useCustomColors &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme =
        if (canUseDynamicColor) {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            buildPresetColorScheme(appearance, isDark)
        }

    // v10: status/navigation bar icons follow the *theme's* background, not the
    // system's light/dark setting. enableEdgeToEdge() alone picks icon colour from
    // the system mode, so a dark launcher theme on a phone in light mode drew
    // dark icons on a dark background — the status-bar clock was unreadable.
    val view = LocalView.current
    if (!view.isInEditMode) {
        val lightBackground = colorScheme.background.luminance() > 0.5f
        SideEffect {
            val window =
                generateSequence(view.context) { (it as? ContextWrapper)?.baseContext }
                    .filterIsInstance<Activity>()
                    .firstOrNull()
                    ?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = lightBackground
                isAppearanceLightNavigationBars = lightBackground
            }
        }
    }

    val typography = remember(appearance.uiFont) { launcherTypography(appearance.uiFont) }
    MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
}

private fun buildPresetColorScheme(
    appearance: AppearanceSettings,
    isDark: Boolean,
): ColorScheme {
    val preset = ThemePreset.findById(appearance.themePresetId)

    val primary: Color
    val secondary: Color
    val background: Color
    val surface: Color
    val onBackground: Color
    val onSurface: Color

    if (appearance.useCustomColors) {
        primary = parseHexColor(appearance.customPrimaryColor)
        secondary = parseHexColor(appearance.customPrimaryColor).copy(alpha = 0.7f)
        background = parseHexColor(appearance.customBackgroundColor)
        surface = parseHexColor(appearance.customBackgroundColor, 0xFF14141F)
        onBackground = parseHexColor(appearance.customFontColor, 0xFFFFFFFF)
        onSurface = parseHexColor(appearance.customFontColor, 0xFFE0E0E8)
    } else {
        if (isDark) {
            primary = parseHexColor(preset.primaryColor)
            secondary = parseHexColor(preset.secondaryColor)
            background = parseHexColor(preset.backgroundColor)
            surface = parseHexColor(preset.surfaceColor)
            onBackground = parseHexColor(preset.onBackgroundColor)
            onSurface = parseHexColor(preset.onSurfaceColor)
        } else {
            // Light variant: invert bg/surface, darken text
            primary = parseHexColor(preset.primaryColor)
            secondary = parseHexColor(preset.secondaryColor)
            background = Color(0xFFF5F5FA)
            surface = Color.White
            onBackground = Color(0xFF1A1A2E)
            onSurface = Color(0xFF2A2A3E)
        }
    }

    return if (isDark) {
        darkColorScheme(
            primary = primary,
            secondary = secondary,
            background = background,
            surface = surface,
            surfaceVariant = surface.copy(alpha = 0.85f),
            onBackground = onBackground,
            onSurface = onSurface,
            onSurfaceVariant = onSurface.copy(alpha = 0.6f),
        )
    } else {
        lightColorScheme(
            primary = primary,
            secondary = secondary,
            background = background,
            surface = surface,
            surfaceVariant = Color(0xFFEEEEF5),
            onBackground = onBackground,
            onSurface = onSurface,
            onSurfaceVariant = onSurface.copy(alpha = 0.6f),
        )
    }
}
