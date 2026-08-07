package com.ailauncher.app.ui.theme

import android.graphics.Typeface
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.ailauncher.app.domain.models.*

fun parseHexColor(hex: String, fallback: Long = 0xFF7C7CFF): Color {
    return try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color(fallback) }
}

fun launcherFontFamily(font: LauncherFont): FontFamily {
    return when (font) {
        LauncherFont.SYSTEM_DEFAULT -> FontFamily.Default
        LauncherFont.MONOSPACE -> FontFamily.Monospace
        LauncherFont.SERIF -> FontFamily.Serif
        LauncherFont.CURSIVE -> FontFamily.Cursive
        else -> {
            try {
                FontFamily(Typeface.create(font.fontFamily, Typeface.NORMAL))
            } catch (_: Exception) {
                FontFamily.Default
            }
        }
    }
}

@Composable
fun AILauncherTheme(
    appearance: AppearanceSettings = AppearanceSettings(),
    content: @Composable () -> Unit
) {
    val isDark = when (appearance.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // Material You: pulled from the system wallpaper on API 31+. Only makes sense
    // when the user isn't already asking for a specific custom palette, and only
    // where the platform actually provides it.
    val canUseDynamicColor = appearance.useDynamicColor && !appearance.useCustomColors &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = if (canUseDynamicColor) {
        val context = LocalContext.current
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        buildPresetColorScheme(appearance, isDark)
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}

private fun buildPresetColorScheme(appearance: AppearanceSettings, isDark: Boolean): ColorScheme {
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
            onSurfaceVariant = onSurface.copy(alpha = 0.6f)
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
            onSurfaceVariant = onSurface.copy(alpha = 0.6f)
        )
    }
}
