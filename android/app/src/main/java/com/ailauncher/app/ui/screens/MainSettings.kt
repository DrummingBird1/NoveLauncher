package com.ailauncher.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AppShortcut
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ailauncher.app.R

/**
 * v9: extracted from SettingsActivity.kt. Top-level "Settings" landing page —
 * the list of every other SettingsPage. Each row is a Triple<Icon, titleRes,
 * subtitleRes> → SettingsPage destination.
 */
@Composable
fun MainSettings(onNavigate: (SettingsPage) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        val items = listOf(
            Triple(Icons.Rounded.Palette, R.string.settings_appearance, R.string.settings_sub_appearance) to SettingsPage.APPEARANCE,
            Triple(Icons.Rounded.ViewCarousel, R.string.settings_pages, R.string.settings_sub_pages) to SettingsPage.PAGES,
            Triple(Icons.Rounded.Wallpaper, R.string.settings_wallpapers, R.string.settings_sub_wallpapers) to SettingsPage.WALLPAPER,
            Triple(Icons.Rounded.Newspaper, R.string.settings_news_sources, R.string.settings_sub_news) to SettingsPage.NEWS_SOURCES,
            Triple(Icons.Rounded.CloudUpload, R.string.settings_backup, R.string.settings_sub_backup) to SettingsPage.BACKUP,
            Triple(Icons.Rounded.Security, R.string.settings_security, R.string.settings_sub_security) to SettingsPage.SECURITY,
            Triple(Icons.Rounded.VisibilityOff, R.string.settings_hidden_apps, R.string.settings_sub_hidden) to SettingsPage.HIDDEN_APPS,
            Triple(Icons.Rounded.QueryStats, R.string.settings_statistics, R.string.settings_sub_statistics) to SettingsPage.STATISTICS,
            Triple(Icons.Rounded.AppShortcut, R.string.settings_icon_packs, R.string.settings_sub_icon_packs) to SettingsPage.ICON_PACKS,
            Triple(Icons.Rounded.Info, R.string.settings_about, R.string.settings_sub_about) to SettingsPage.ABOUT,
        )
        items.forEach { (info, page) ->
            item {
                SettingsMenuItem(info.first, stringResource(info.second), stringResource(info.third)) { onNavigate(page) }
            }
        }
    }
}

@Composable
fun SettingsMenuItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
