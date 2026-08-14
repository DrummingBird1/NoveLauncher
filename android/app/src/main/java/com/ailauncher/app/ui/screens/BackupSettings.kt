package com.ailauncher.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ailauncher.app.R
import com.ailauncher.app.data.SettingsRepository
import com.ailauncher.app.data.backup.BackupManager
import com.ailauncher.app.domain.models.BackupDestination
import com.ailauncher.app.domain.models.BackupSettings
import kotlinx.coroutines.launch

/**
 * v9: extracted from SettingsActivity.kt. BACKUP SettingsPage — destinations,
 * NAS config (address + path + credentials), restore list.
 */
@Composable
fun BackupSection(
    backup: BackupSettings,
    backupManager: BackupManager,
    settingsRepo: SettingsRepository,
    onUpdate: (BackupSettings) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var status by remember { mutableStateOf<String?>(null) }
    val backingUpText = stringResource(R.string.backup_status_in_progress)
    val restoredText = stringResource(R.string.restore_success)

    // Not persisted (not part of BackupSettings) — entered fresh per action, same
    // as any password prompt. Blank = today's plain-JSON behavior, unchanged.
    var backupPassword by remember { mutableStateOf("") }

    // OneDrive/Box are declared in BackupDestination but backupManager.backup() always
    // returns an error for them until Microsoft/Box OAuth is wired up — don't offer an
    // option that can never succeed.
    val availableDestinations = BackupDestination.entries.filter {
        it == BackupDestination.LOCAL || it == BackupDestination.GOOGLE_DRIVE || it == BackupDestination.NAS
    }

    val dateFormat = remember { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionLabel(stringResource(R.string.settings_backup)) }
        if (backup.lastBackupTimestamp > 0L) {
            item {
                Text(
                    stringResource(
                        if (backup.lastBackupSuccess) R.string.backup_last_run_success else R.string.backup_last_run_failed,
                        dateFormat.format(java.util.Date(backup.lastBackupTimestamp))
                    ),
                    fontSize = 12.sp,
                    color = if (backup.lastBackupSuccess) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                )
            }
        }
        items(availableDestinations) { dest ->
            Card(Modifier.fillMaxWidth().clickable {
                scope.launch {
                    status = backingUpText
                    val result = backupManager.backup(dest, backupPassword)
                    status = when (result) {
                        is BackupManager.BackupResult.Success -> context.getString(R.string.backup_status_success, result.path)
                        is BackupManager.BackupResult.Error -> context.getString(R.string.backup_status_error, result.message)
                    }
                    onUpdate(
                        backup.copy(
                            lastBackupTimestamp = System.currentTimeMillis(),
                            lastBackupSuccess = result is BackupManager.BackupResult.Success
                        )
                    )
                }
            }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (dest == BackupDestination.LOCAL) Icons.Rounded.PhoneAndroid
                        else if (dest == BackupDestination.NAS) Icons.Rounded.Dns
                        else Icons.Rounded.Cloud,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(dest.displayNameRes), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        if (status != null) item { Text(status!!, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary) }

        item { SectionLabel(stringResource(R.string.backup_encryption_section)) }
        item {
            OutlinedTextField(
                value = backupPassword,
                onValueChange = { backupPassword = it },
                label = { Text(stringResource(R.string.backup_password_label)) },
                visualTransformation = PasswordVisualTransformation(),
                supportingText = { Text(stringResource(R.string.backup_password_hint), fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item { SectionLabel("NAS") }
        item { OutlinedTextField(value = backup.nasAddress, onValueChange = { onUpdate(backup.copy(nasAddress = it)) }, label = { Text(stringResource(R.string.nas_address_label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = backup.nasPath, onValueChange = { onUpdate(backup.copy(nasPath = it)) }, label = { Text(stringResource(R.string.nas_path_label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = backup.nasUsername, onValueChange = { onUpdate(backup.copy(nasUsername = it)) }, label = { Text(stringResource(R.string.backup_nas_username_label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item {
            OutlinedTextField(
                value = backup.nasPassword,
                onValueChange = { onUpdate(backup.copy(nasPassword = it)) },
                label = { Text(stringResource(R.string.backup_nas_password_label)) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item { SectionLabel(stringResource(R.string.backup_local_retention_section)) }
        item {
            val keepLabel = if (backup.maxLocalBackupsToKeep <= 0) stringResource(R.string.backup_retention_unlimited)
                else stringResource(R.string.backup_retention_count, backup.maxLocalBackupsToKeep)
            SliderSetting(
                stringResource(R.string.backup_retention_label),
                backup.maxLocalBackupsToKeep.toFloat(),
                0f..30f,
                keepLabel,
                steps = 29
            ) { onUpdate(backup.copy(maxLocalBackupsToKeep = it.toInt())) }
        }

        item { SectionLabel(stringResource(R.string.section_restore)) }
        item {
            val files = remember { backupManager.listLocalBackups() }
            if (files.isEmpty()) {
                Text(stringResource(R.string.no_backups), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                files.take(5).forEach { backup ->
                    Card(Modifier.fillMaxWidth().clickable {
                        scope.launch {
                            status = when (val r = backupManager.restoreFromLocal(backup, backupPassword)) {
                                is BackupManager.BackupResult.Success -> restoredText
                                is BackupManager.BackupResult.Error -> context.getString(R.string.backup_status_error, r.message)
                            }
                        }
                    }, shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.padding(12.dp)) {
                            Icon(Icons.Rounded.Restore, null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Text(backup.displayName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}
