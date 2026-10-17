package com.ailauncher.app.ui.screens

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.ailauncher.app.R
import com.ailauncher.app.domain.models.AppProfile
import com.ailauncher.app.domain.models.RankedApp
import com.ailauncher.app.ui.LauncherViewModel
import com.ailauncher.app.ui.LocalAppLauncher
import com.ailauncher.app.ui.findLauncherActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * v8: long-press menu for an app icon. v10: shared by Home and Apps (it used
 * to be duplicated in both), shows the app's own shortcuts at the top
 * ("New chat", "Navigate home"…, read through LauncherApps — only available
 * while NoveLauncher is the default home app), and knows about profiles: work
 * and Private Space apps get App info but no in-place uninstall.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionsSheet(
    target: RankedApp,
    viewModel: LauncherViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val appLauncher = LocalAppLauncher.current
    val app = target.app

    val shortcuts by produceState(emptyList<Pair<ShortcutInfo, ImageBitmap?>>(), app.key) {
        value =
            withContext(Dispatchers.IO) {
                appLauncher.shortcutsFor(app).map { s ->
                    s to appLauncher.shortcutIcon(s)?.let { d -> runCatching { d.toBitmap(96, 96).asImageBitmap() }.getOrNull() }
                }
            }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text(app.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            profileLabel(app.profile)?.let {
                Text(stringResource(it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(12.dp))

            if (shortcuts.isNotEmpty()) {
                shortcuts.forEach { (shortcut, icon) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                context.findLauncherActivity()?.launchShortcutWithLockCheck(app, shortcut)
                                onDismiss()
                            }.padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (icon != null) {
                            Image(icon, null, Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)))
                        } else {
                            Spacer(Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            (shortcut.shortLabel ?: shortcut.longLabel ?: "").toString(),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.surfaceVariant)
            }

            ActionRow(Icons.Rounded.VisibilityOff, stringResource(R.string.action_hide_app)) {
                viewModel.hideApp(app.packageName)
                onDismiss()
            }
            if (app.profile != AppProfile.PRIVATE) {
                ActionRow(Icons.Rounded.Shield, stringResource(R.string.action_move_private)) {
                    viewModel.moveToPrivateFolder(app.packageName)
                    onDismiss()
                }
            }
            ActionRow(Icons.Rounded.Lock, stringResource(R.string.action_toggle_lock)) {
                viewModel.toggleAppLock(app.packageName)
                onDismiss()
            }
            ActionRow(Icons.Rounded.Info, stringResource(R.string.action_app_info)) {
                appLauncher.openAppInfo(app)
                onDismiss()
            }
            if (!app.isSystemApp && app.profile == AppProfile.PERSONAL) {
                ActionRow(Icons.Rounded.Delete, stringResource(R.string.action_uninstall)) {
                    appLauncher.requestUninstall(app)
                    onDismiss()
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun profileLabel(profile: AppProfile): Int? =
    when (profile) {
        AppProfile.WORK -> R.string.profile_work_app
        AppProfile.PRIVATE -> R.string.profile_private_app
        AppProfile.CLONE -> R.string.profile_clone_app
        AppProfile.PERSONAL -> null
    }

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurface)
    }
}
