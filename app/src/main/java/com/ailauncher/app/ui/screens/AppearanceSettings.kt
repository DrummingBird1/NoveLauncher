package com.ailauncher.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ailauncher.app.R
import com.ailauncher.app.domain.models.AppearanceSettings
import com.ailauncher.app.domain.models.ClockSettings
import com.ailauncher.app.domain.models.IconShape
import com.ailauncher.app.domain.models.LauncherFont
import com.ailauncher.app.domain.models.ThemeMode
import com.ailauncher.app.domain.models.ThemePreset
import com.ailauncher.app.ui.theme.launcherFontFamily
import com.ailauncher.app.ui.theme.parseHexColor

/**
 * v9: extracted from SettingsActivity.kt. Covers 6 SettingsPage entries:
 * APPEARANCE / THEMES / FONTS / ICON_SHAPES / CLOCK + the LivePreview card
 * shared between them.
 *
 * Why grouped: every section here reads/writes the same AppearanceSettings
 * object via the same `onUpdate` lambda. Splitting further would force callers
 * to pass that same lambda through multiple layers.
 */

@Composable
fun AppearanceSection(
    appearance: AppearanceSettings,
    onUpdate: (AppearanceSettings) -> Unit,
    onNavigate: (SettingsPage) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { LivePreview(appearance) }

        item { SectionLabel(stringResource(R.string.section_size)) }
        item { SliderSetting(stringResource(R.string.appearance_icon_size), appearance.iconSizeDp.toFloat(), 32f..80f, "${appearance.iconSizeDp}dp") { onUpdate(appearance.copy(iconSizeDp = it.toInt())) } }
        item { SliderSetting(stringResource(R.string.appearance_app_font_size), appearance.fontSizeSp.toFloat(), 8f..22f, "${appearance.fontSizeSp}sp") { onUpdate(appearance.copy(fontSizeSp = it.toInt())) } }
        item { SliderSetting(stringResource(R.string.appearance_columns), appearance.gridColumns.toFloat(), 3f..6f, "${appearance.gridColumns}", steps = 2) { onUpdate(appearance.copy(gridColumns = it.toInt())) } }
        item { SliderSetting(stringResource(R.string.appearance_folder_icon_size), appearance.folderIconSizeDp.toFloat(), 20f..48f, "${appearance.folderIconSizeDp}dp") { onUpdate(appearance.copy(folderIconSizeDp = it.toInt())) } }
        item { SliderSetting(stringResource(R.string.appearance_folder_font_size), appearance.folderFontSizeSp.toFloat(), 10f..22f, "${appearance.folderFontSizeSp}sp") { onUpdate(appearance.copy(folderFontSizeSp = it.toInt())) } }

        item { SectionLabel(stringResource(R.string.section_more_menus)) }
        val subMenus = listOf(
            Triple(Icons.Rounded.ColorLens, R.string.appearance_themes_and_colors, SettingsPage.THEMES),
            Triple(Icons.Rounded.FontDownload, R.string.settings_fonts, SettingsPage.FONTS),
            Triple(Icons.Rounded.Crop, R.string.settings_icon_shapes, SettingsPage.ICON_SHAPES),
            Triple(Icons.Rounded.Schedule, R.string.clock_settings_title, SettingsPage.CLOCK),
        )
        subMenus.forEach { (icon, labelRes, page) ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigate(page) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(labelRes), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
fun LivePreview(appearance: AppearanceSettings) {
    val preset = ThemePreset.findById(appearance.themePresetId)
    val bgColor = if (appearance.useCustomColors) parseHexColor(appearance.customBackgroundColor) else parseHexColor(preset.backgroundColor)
    val fgColor = if (appearance.useCustomColors) parseHexColor(appearance.customFontColor) else parseHexColor(preset.onBackgroundColor)
    val primary = if (appearance.useCustomColors) parseHexColor(appearance.customPrimaryColor) else parseHexColor(preset.primaryColor)
    val fontFamily = launcherFontFamily(appearance.appFont)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier.fillMaxWidth().height(200.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.preview_label), fontSize = 11.sp, color = fgColor.copy(alpha = 0.5f))
            Spacer(Modifier.height(4.dp))
            Text(
                "14:30",
                fontSize = appearance.clock.fontSize.sp,
                color = parseHexColor(appearance.clock.fontColor),
                fontFamily = launcherFontFamily(appearance.clock.font)
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(appearance.gridColumns.coerceAtMost(5)) { i ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val shape = iconShapeToCornerShape(appearance.iconShape)
                        Box(
                            modifier = Modifier
                                .size(appearance.iconSizeDp.dp * 0.6f)
                                .clip(shape)
                                .background(primary.copy(alpha = 0.3f + i * 0.15f))
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "App ${i + 1}",
                            fontSize = (appearance.fontSizeSp * 0.8f).sp,
                            color = fgColor,
                            fontFamily = fontFamily,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/** Shared shape mapping for both the preview card and the icon-shape picker. */
internal fun iconShapeToCornerShape(shape: IconShape) = when (shape) {
    IconShape.CIRCLE -> CircleShape
    IconShape.SQUARE -> RoundedCornerShape(0.dp)
    IconShape.ROUNDED_SQUARE -> RoundedCornerShape(14.dp)
    IconShape.SQUIRCLE -> RoundedCornerShape(18.dp)
    IconShape.TEARDROP -> RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    IconShape.HEXAGON -> RoundedCornerShape(8.dp)
    IconShape.DIAMOND -> RoundedCornerShape(8.dp)
    IconShape.SHIELD -> RoundedCornerShape(12.dp)
    IconShape.LEAF -> RoundedCornerShape(16.dp, 4.dp, 16.dp, 4.dp)
    IconShape.BLOB -> RoundedCornerShape(20.dp)
    IconShape.CLOVER -> RoundedCornerShape(12.dp)
    IconShape.OCTAGON -> RoundedCornerShape(10.dp)
}

@Composable
fun ThemesSection(appearance: AppearanceSettings, onUpdate: (AppearanceSettings) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionLabel(stringResource(R.string.section_theme_mode)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = appearance.themeMode == mode,
                        onClick = { onUpdate(appearance.copy(themeMode = mode)) },
                        label = { Text(stringResource(mode.displayNameRes)) }
                    )
                }
            }
        }

        item { SectionLabel(stringResource(R.string.section_theme_presets)) }
        items(ThemePreset.PRESETS.chunked(3)) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { preset ->
                    val isSelected = appearance.themePresetId == preset.id && !appearance.useCustomColors
                    Card(
                        modifier = Modifier.weight(1f).clickable {
                            onUpdate(appearance.copy(themePresetId = preset.id, useCustomColors = false))
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSelected) BorderStroke(2.dp, parseHexColor(preset.primaryColor)) else null,
                        colors = CardDefaults.cardColors(containerColor = parseHexColor(preset.backgroundColor))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(preset.primaryColor, preset.secondaryColor, preset.accentColor).forEach { c ->
                                    Box(Modifier.size(16.dp).clip(CircleShape).background(parseHexColor(c)))
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(preset.localizedName(androidx.compose.ui.platform.LocalContext.current), fontSize = 11.sp, color = parseHexColor(preset.onBackgroundColor), textAlign = TextAlign.Center)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        item { SectionLabel(stringResource(R.string.section_custom_colors)) }
        item {
            SwitchSetting(stringResource(R.string.custom_colors_toggle), appearance.useCustomColors) {
                onUpdate(appearance.copy(useCustomColors = it))
            }
        }
        if (appearance.useCustomColors) {
            item { FullColorPicker(stringResource(R.string.color_primary), appearance.customPrimaryColor) { onUpdate(appearance.copy(customPrimaryColor = it)) } }
            item { FullColorPicker(stringResource(R.string.color_background), appearance.customBackgroundColor) { onUpdate(appearance.copy(customBackgroundColor = it)) } }
            item { FullColorPicker(stringResource(R.string.color_font), appearance.customFontColor) { onUpdate(appearance.copy(customFontColor = it)) } }
        }
    }
}

@Composable
fun FullColorPicker(label: String, currentHex: String, onColorSelected: (String) -> Unit) {
    val allColors = listOf(
        // Reds
        "#FF0000", "#FF4444", "#FF6B6B", "#FF4757", "#E74C3C", "#C0392B", "#8B0000",
        // Oranges
        "#FF6348", "#FF7F50", "#FF8C42", "#FFA502", "#E67E22", "#D35400",
        // Yellows
        "#FFD93D", "#FFC312", "#F1C40F", "#FDCB6E",
        // Greens
        "#2ED573", "#00B894", "#2ECC71", "#27AE60", "#1ABC9C", "#16A085", "#20C997",
        // Teals
        "#4ECDC4", "#00CEC9", "#63E6BE",
        // Blues
        "#3742FA", "#4A90D9", "#70A1FF", "#5DADE2", "#3498DB", "#2980B9", "#1E90FF",
        // Purples
        "#7C7CFF", "#A78BFA", "#C4B5FD", "#9B59B6", "#8E44AD", "#6C5CE7",
        // Pinks
        "#FF6B9D", "#FF85B1", "#F472B6", "#E91E63", "#FF1493",
        // Grays
        "#778899", "#A4B0BE", "#95A5A6", "#7F8C8D", "#BDC3C7", "#ECF0F1",
        // Dark
        "#0A0A0F", "#14141F", "#1A1A2E", "#2C3E50", "#34495E", "#2D2D44",
        // Light
        "#F5F5FA", "#FFFFFF", "#FAFAFA", "#E8E8E8"
    )

    Column {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(parseHexColor(currentHex)).border(1.dp, Color.Gray, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(currentHex, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        var hexInput by remember { mutableStateOf(currentHex) }
        OutlinedTextField(
            value = hexInput,
            onValueChange = { v ->
                hexInput = v
                if (v.matches(Regex("^#[0-9A-Fa-f]{6}$"))) onColorSelected(v)
            },
            label = { Text(stringResource(R.string.label_hex_code)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(allColors) { hex ->
                val isSelected = hex.equals(currentHex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(parseHexColor(hex))
                        .then(if (isSelected) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
                        .clickable { onColorSelected(hex); hexInput = hex }
                )
            }
        }
    }
}

@Composable
fun FontsSection(appearance: AppearanceSettings, onUpdate: (AppearanceSettings) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val fontTargets = listOf(
            R.string.fonts_target_app_names to appearance.appFont to { f: LauncherFont -> onUpdate(appearance.copy(appFont = f)) },
            R.string.fonts_target_folders to appearance.folderFont to { f: LauncherFont -> onUpdate(appearance.copy(folderFont = f)) },
            R.string.fonts_target_pages to appearance.pageFont to { f: LauncherFont -> onUpdate(appearance.copy(pageFont = f)) },
            R.string.fonts_target_ui to appearance.uiFont to { f: LauncherFont -> onUpdate(appearance.copy(uiFont = f)) },
        )

        fontTargets.forEach { (labelAndCurrent, setter) ->
            val (labelRes, current) = labelAndCurrent
            item { SectionLabel(stringResource(labelRes)) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LauncherFont.entries.forEach { font ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { setter(font) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (current == font) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = current == font, onClick = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(font.displayNameRes),
                                    fontFamily = launcherFontFamily(font),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    stringResource(R.string.font_sample),
                                    fontFamily = launcherFontFamily(font),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IconShapesSection(appearance: AppearanceSettings, onUpdate: (AppearanceSettings) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { LivePreview(appearance) }
        item { SectionLabel(stringResource(R.string.section_choose_shape)) }
        items(IconShape.entries.toList()) { shape ->
            val isSelected = appearance.iconShape == shape
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onUpdate(appearance.copy(iconShape = shape)) },
                shape = RoundedCornerShape(12.dp),
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(iconShapeToCornerShape(shape)).background(MaterialTheme.colorScheme.primary))
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(shape.displayNameRes), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun ClockSection(appearance: AppearanceSettings, onUpdate: (AppearanceSettings) -> Unit) {
    val clock = appearance.clock
    fun updateClock(c: ClockSettings) = onUpdate(appearance.copy(clock = c))

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { LivePreview(appearance) }
        item { SliderSetting(stringResource(R.string.clock_size), clock.fontSize.toFloat(), 24f..120f, "${clock.fontSize}sp") { updateClock(clock.copy(fontSize = it.toInt())) } }
        item { SliderSetting(stringResource(R.string.clock_date_size), clock.dateFontSize.toFloat(), 10f..28f, "${clock.dateFontSize}sp") { updateClock(clock.copy(dateFontSize = it.toInt())) } }
        item { SliderSetting(stringResource(R.string.clock_opacity), clock.opacity, 0.1f..1f, "${(clock.opacity * 100).toInt()}%") { updateClock(clock.copy(opacity = it)) } }
        item { SwitchSetting(stringResource(R.string.clock_show_date), clock.showDate) { updateClock(clock.copy(showDate = it)) } }
        item { SwitchSetting(stringResource(R.string.clock_24_hour), clock.use24Hour) { updateClock(clock.copy(use24Hour = it)) } }
        item { SwitchSetting(stringResource(R.string.clock_show_seconds), clock.showSeconds) { updateClock(clock.copy(showSeconds = it)) } }
        item { FullColorPicker(stringResource(R.string.clock_color), clock.fontColor) { updateClock(clock.copy(fontColor = it)) } }
        item { SectionLabel(stringResource(R.string.section_clock_font)) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LauncherFont.entries.toList()) { font ->
                    FilterChip(
                        selected = clock.font == font,
                        onClick = { updateClock(clock.copy(font = font)) },
                        label = { Text(stringResource(font.displayNameRes), fontFamily = launcherFontFamily(font), fontSize = 12.sp) }
                    )
                }
            }
        }
    }
}
