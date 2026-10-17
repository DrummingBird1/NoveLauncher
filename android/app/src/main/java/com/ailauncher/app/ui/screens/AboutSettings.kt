package com.ailauncher.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.ailauncher.app.R
import kotlinx.coroutines.launch

// v10: split out of MiscSettings.kt.

/**
 * v10: links point at the live website (the old ones went to novelauncher.app,
 * a domain that doesn't exist), the real app icon replaces the placeholder
 * "N", and the opt-in update check lives here.
 */
@Composable
fun AboutSection(
    smartControl: com.ailauncher.app.domain.models.SmartControlSettings,
    onUpdateSmartControl: (com.ailauncher.app.domain.models.SmartControlSettings) -> Unit,
    updateChecker: com.ailauncher.app.data.update.UpdateChecker,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var updateStatus by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }

    fun open(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
        }
    }
    val appIcon =
        remember {
            androidx.core.content.res.ResourcesCompat
                .getDrawable(context.resources, R.mipmap.ic_launcher, context.theme)
                ?.let { runCatching { it.toBitmap(192, 192).asImageBitmap() }.getOrNull() }
        }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
                if (appIcon != null) {
                    androidx.compose.foundation.Image(appIcon, null, Modifier.size(80.dp).clip(RoundedCornerShape(22.dp)))
                } else {
                    Box(
                        Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("N", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("NoveLauncher", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                // v9: show the real build version instead of a hardcoded "1.0.0".
                Text(
                    stringResource(R.string.about_version, com.ailauncher.app.BuildConfig.VERSION_NAME),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Text(
                stringResource(R.string.about_description),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp,
            )
        }

        item { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant) }

        // Hidden when a store installed the app — the store handles updates, and
        // its review policy forbids pointing users anywhere else (Pitfall #40).
        if (updateChecker.isAvailable) {
            item { SectionLabel(stringResource(R.string.about_updates_section)) }
            item {
                SwitchSetting(stringResource(R.string.about_check_updates_auto), smartControl.checkForUpdates) {
                    onUpdateSmartControl(smartControl.copy(checkForUpdates = it))
                }
            }
            item {
                Text(
                    stringResource(R.string.about_check_updates_note),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                val upToDate = stringResource(R.string.about_up_to_date)
                OutlinedButton(onClick = {
                    checking = true
                    scope.launch {
                        val found = updateChecker.check(force = true)
                        checking = false
                        updateStatus = found?.let { context.getString(R.string.update_available, it.version) } ?: upToDate
                        found?.let { open(it.url) }
                    }
                }, enabled = !checking, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.about_check_updates_now))
                }
            }
            updateStatus?.let { item { Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary) } }

            item { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant) }
        }

        item {
            AboutButton(Icons.Rounded.Language, stringResource(R.string.about_website)) { open(SITE) }
        }
        item {
            AboutButton(
                Icons.Rounded.Code,
                stringResource(R.string.about_source_code),
            ) { open("https://github.com/DrummingBird1/NoveLauncher") }
        }
        item {
            val rateLabel = stringResource(R.string.about_rate)
            AboutButton(Icons.Rounded.Star, rateLabel) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.ailauncher.app")))
                } catch (
                    _: Exception,
                ) {
                    open("https://play.google.com/store/apps/details?id=com.ailauncher.app")
                }
            }
        }
        item {
            AboutButton(Icons.Rounded.Favorite, stringResource(R.string.about_support)) { open("https://buymeacoffee.com/novelauncher") }
        }
        item {
            AboutButton(
                Icons.Rounded.VolunteerActivism,
                stringResource(R.string.about_patreon),
            ) { open("https://www.patreon.com/cw/MrIdan") }
        }
        item {
            val contactLabel = stringResource(R.string.about_contact)
            val emailSubject = stringResource(R.string.about_contact_email_subject)
            AboutButton(Icons.Rounded.Email, contactLabel) {
                val intent =
                    Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:solvaris2@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                    }
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                }
            }
        }
        item {
            AboutButton(Icons.Rounded.Policy, stringResource(R.string.about_privacy)) { open("$SITE/privacy.html") }
        }
        item {
            AboutButton(Icons.Rounded.Description, stringResource(R.string.about_terms)) { open("$SITE/terms.html") }
        }

        item {
            Text(
                stringResource(R.string.about_copyright),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
        }
    }
}

private const val SITE = "https://drummingbird1.github.io/NoveLauncher"

@Composable
fun AboutButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
