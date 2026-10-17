package com.ailauncher.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AppShortcut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.ailauncher.app.R
import com.ailauncher.app.data.iconpack.IconPackManager
import com.ailauncher.app.domain.models.AppearanceSettings

// v10: split out of MiscSettings.kt.

/**
 * v10: packs were listed but could never be selected or applied. Now one is
 * chosen here (AppearanceSettings.iconPackPackage) and InstalledAppsRepository
 * swaps in its icons; apps the pack doesn't cover keep their own icon.
 */
@Composable
fun IconPacksSection(
    manager: IconPackManager,
    appearance: AppearanceSettings,
    onUpdate: (AppearanceSettings) -> Unit,
) {
    val packs = remember { manager.getAvailableIconPacks() }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            IconPackRow(
                label = stringResource(R.string.icon_pack_system),
                icon = null,
                selected = appearance.iconPackPackage.isBlank(),
            ) { onUpdate(appearance.copy(iconPackPackage = "")) }
        }
        if (packs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(stringResource(R.string.icon_packs_empty_title), color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.icon_packs_empty_hint),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(packs, key = { it.packageName }) { pack ->
                IconPackRow(pack.label, pack.icon, selected = appearance.iconPackPackage == pack.packageName) {
                    manager.clearCache()
                    onUpdate(appearance.copy(iconPackPackage = pack.packageName))
                }
            }
        }
    }
}

@Composable
private fun IconPackRow(
    label: String,
    icon: android.graphics.drawable.Drawable?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (selected) {
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.15f,
                        )
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.width(8.dp))
            val bmp = remember(icon) { icon?.let { runCatching { it.toBitmap(96, 96).asImageBitmap() }.getOrNull() } }
            if (bmp != null) {
                androidx.compose.foundation.Image(bmp, null, Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)))
            } else {
                Icon(Icons.Rounded.AppShortcut, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
