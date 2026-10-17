package com.ailauncher.app.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ailauncher.app.R
import com.ailauncher.app.data.SettingsRepository
import com.ailauncher.app.data.backup.BackupManager
import com.ailauncher.app.data.backup.DriveAuth
import com.ailauncher.app.data.backup.DriveClient
import com.ailauncher.app.domain.models.BackupDestination
import com.ailauncher.app.domain.models.BackupSettings
import com.ailauncher.app.security.PortableBackupCrypto
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
    onUpdate: (BackupSettings) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = LocalResources.current
    var status by remember { mutableStateOf<String?>(null) }
    val backingUpText = stringResource(R.string.backup_status_in_progress)
    val restoredText = stringResource(R.string.restore_success)

    // Not persisted (not part of BackupSettings) — entered fresh per action, same
    // as any password prompt. Blank = today's plain-JSON behavior, unchanged.
    var backupPassword by remember { mutableStateOf("") }

    // Drive needs Google Play services (Huawei phones have none), so it's hidden
    // without them — connect, backup and restore alike (Pitfall #41).
    val playServices = remember { backupManager.isDriveAvailable() }

    // OneDrive/Box are declared in BackupDestination but backupManager.backup() always
    // returns an error for them until Microsoft/Box OAuth is wired up — don't offer an
    // option that can never succeed. Same for Drive without Play services.
    val availableDestinations =
        BackupDestination.entries.filter {
            it == BackupDestination.LOCAL || it == BackupDestination.NAS || (it == BackupDestination.GOOGLE_DRIVE && playServices)
        }

    val dateFormat = remember { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()) }

    // v10: Google Drive connection. Previously there was no way to connect an
    // account at all — see DriveAuth's kdoc.
    var driveEmail by remember { mutableStateOf(DriveAuth.connectedEmail(context)) }
    val driveConsentLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { res ->
            if (res.resultCode == Activity.RESULT_OK) {
                try {
                    driveEmail = DriveAuth.onAuthorized(context, DriveAuth.resultFromIntent(context, res.data))
                } catch (e: Exception) {
                    status = resources.getString(R.string.drive_connect_failed, e.message ?: "")
                }
            }
        }

    fun connectDrive() {
        DriveAuth
            .client(context)
            .authorize(DriveAuth.request(context))
            .addOnSuccessListener { r ->
                val pending = r.pendingIntent
                if (r.hasResolution() && pending != null) {
                    driveConsentLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                } else {
                    driveEmail = DriveAuth.onAuthorized(context, r)
                }
            }.addOnFailureListener { e -> status = resources.getString(R.string.drive_connect_failed, e.message ?: "") }
    }

    // Restore results show inside the Restore section — the backup [status] line
    // sits at the top of the page, out of view when a restore is tapped.
    var restoreStatus by remember { mutableStateOf<String?>(null) }
    // null = not fetched yet. Fetched on demand: it's a network call.
    var driveBackups by remember { mutableStateOf<List<DriveClient.DriveFile>?>(null) }
    var driveLoading by remember { mutableStateOf(false) }
    // A downloaded PortableBackupCrypto envelope waiting for its password.
    var encryptedPending by remember { mutableStateOf<String?>(null) }
    var encryptedWrongPassword by remember { mutableStateOf(false) }
    val downloadingText = stringResource(R.string.restore_downloading)

    fun loadDriveBackups() {
        scope.launch {
            driveLoading = true
            when (val r = backupManager.listDriveBackups()) {
                is BackupManager.DriveResult.Success -> driveBackups = r.value
                is BackupManager.DriveResult.Error -> restoreStatus = resources.getString(R.string.backup_status_error, r.message)
            }
            driveLoading = false
        }
    }

    // An encrypted backup is tried with the Encryption section's password first
    // (same as local restore); if that's blank or wrong, the dialog asks for it.
    fun restoreContent(
        content: String,
        password: String,
    ) {
        scope.launch {
            val encrypted = PortableBackupCrypto.isEncrypted(content)
            if (encrypted && password.isEmpty()) {
                restoreStatus = null
                encryptedWrongPassword = false
                encryptedPending = content
                return@launch
            }
            when (val r = backupManager.restoreFromJson(content, password)) {
                is BackupManager.BackupResult.Success -> {
                    encryptedPending = null
                    restoreStatus = restoredText
                }

                is BackupManager.BackupResult.Error -> {
                    if (encrypted) {
                        restoreStatus = null
                        encryptedWrongPassword = true
                        encryptedPending = content
                    } else {
                        restoreStatus = resources.getString(R.string.backup_status_error, r.message)
                    }
                }
            }
        }
    }

    fun restoreFromDrive(file: DriveClient.DriveFile) {
        scope.launch {
            restoreStatus = downloadingText
            when (val r = backupManager.downloadDriveBackup(file)) {
                is BackupManager.DriveResult.Success -> restoreContent(r.value, backupPassword)
                is BackupManager.DriveResult.Error -> restoreStatus = resources.getString(R.string.backup_status_error, r.message)
            }
        }
    }

    encryptedPending?.let { content ->
        RestorePasswordDialog(
            wrongPassword = encryptedWrongPassword,
            onDismiss = { encryptedPending = null },
            onConfirm = { restoreContent(content, it) },
        )
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionLabel(stringResource(R.string.settings_backup)) }
        if (backup.lastBackupTimestamp > 0L) {
            item {
                Text(
                    stringResource(
                        if (backup.lastBackupSuccess) R.string.backup_last_run_success else R.string.backup_last_run_failed,
                        dateFormat.format(java.util.Date(backup.lastBackupTimestamp)),
                    ),
                    fontSize = 12.sp,
                    color = if (backup.lastBackupSuccess) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                )
            }
        }
        items(availableDestinations) { dest ->
            Card(
                Modifier.fillMaxWidth().clickable {
                    scope.launch {
                        status = backingUpText
                        val result = backupManager.backup(dest, backupPassword)
                        status =
                            when (result) {
                                is BackupManager.BackupResult.Success -> resources.getString(R.string.backup_status_success, result.path)
                                is BackupManager.BackupResult.Error -> resources.getString(R.string.backup_status_error, result.message)
                            }
                        // A fetched Drive list no longer has the newest backup.
                        if (dest == BackupDestination.GOOGLE_DRIVE) driveBackups = null
                        onUpdate(
                            backup.copy(
                                lastBackupTimestamp = System.currentTimeMillis(),
                                lastBackupSuccess = result is BackupManager.BackupResult.Success,
                            ),
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (dest == BackupDestination.LOCAL) {
                            Icons.Rounded.PhoneAndroid
                        } else if (dest == BackupDestination.NAS) {
                            Icons.Rounded.Dns
                        } else {
                            Icons.Rounded.Cloud
                        },
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(dest.displayNameRes), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        if (status != null) item { Text(status!!, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary) }

        if (playServices) {
            item { SectionLabel(stringResource(R.string.drive_section_title)) }
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        val email = driveEmail
                        if (email != null) {
                            Text(stringResource(R.string.drive_connected_as, email), color = MaterialTheme.colorScheme.onSurface)
                            TextButton(onClick = {
                                DriveAuth.disconnect(context)
                                driveEmail = null
                                driveBackups = null
                            }) {
                                Text(stringResource(R.string.drive_disconnect), color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            Button(onClick = { connectDrive() }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Rounded.Cloud, null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.drive_connect))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.drive_scope_note),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item { SectionLabel(stringResource(R.string.backup_encryption_section)) }
        item {
            OutlinedTextField(
                value = backupPassword,
                onValueChange = { backupPassword = it },
                label = { Text(stringResource(R.string.backup_password_label)) },
                visualTransformation = PasswordVisualTransformation(),
                supportingText = { Text(stringResource(R.string.backup_password_hint), fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        item { SectionLabel("NAS") }
        item {
            OutlinedTextField(value = backup.nasAddress, onValueChange = {
                onUpdate(backup.copy(nasAddress = it))
            }, label = { Text(stringResource(R.string.nas_address_label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        item {
            OutlinedTextField(value = backup.nasPath, onValueChange = {
                onUpdate(backup.copy(nasPath = it))
            }, label = { Text(stringResource(R.string.nas_path_label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        item {
            OutlinedTextField(value = backup.nasUsername, onValueChange = {
                onUpdate(backup.copy(nasUsername = it))
            }, label = { Text(stringResource(R.string.backup_nas_username_label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        item {
            OutlinedTextField(
                value = backup.nasPassword,
                onValueChange = { onUpdate(backup.copy(nasPassword = it)) },
                label = { Text(stringResource(R.string.backup_nas_password_label)) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        item { SectionLabel(stringResource(R.string.backup_local_retention_section)) }
        item {
            val keepLabel =
                if (backup.maxLocalBackupsToKeep <= 0) {
                    stringResource(R.string.backup_retention_unlimited)
                } else {
                    stringResource(R.string.backup_retention_count, backup.maxLocalBackupsToKeep)
                }
            SliderSetting(
                stringResource(R.string.backup_retention_label),
                backup.maxLocalBackupsToKeep.toFloat(),
                0f..30f,
                keepLabel,
                steps = 29,
            ) { onUpdate(backup.copy(maxLocalBackupsToKeep = it.toInt())) }
        }

        item { SectionLabel(stringResource(R.string.section_restore)) }
        val driveRestore = playServices && driveEmail != null
        if (driveRestore) {
            item { Text(stringResource(R.string.backup_dest_local), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        item {
            val files = remember { backupManager.listLocalBackups() }
            if (files.isEmpty()) {
                Text(stringResource(R.string.no_backups), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    files.take(5).forEach { backup ->
                        RestoreFileCard(backup.displayName) {
                            scope.launch {
                                restoreStatus =
                                    when (val r = backupManager.restoreFromLocal(backup, backupPassword)) {
                                        is BackupManager.BackupResult.Success -> restoredText
                                        is BackupManager.BackupResult.Error -> resources.getString(R.string.backup_status_error, r.message)
                                    }
                            }
                        }
                    }
                }
            }
        }
        if (driveRestore) {
            item {
                Text(
                    stringResource(R.string.backup_dest_google_drive),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                val files = driveBackups
                when {
                    files == null -> {
                        OutlinedButton(
                            onClick = { loadDriveBackups() },
                            enabled = !driveLoading,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.Cloud, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(if (driveLoading) R.string.drive_restore_loading else R.string.drive_restore_show))
                        }
                    }

                    files.isEmpty() -> {
                        Text(
                            stringResource(R.string.drive_restore_empty),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            files.take(10).forEach { file ->
                                RestoreFileCard(file.name) { restoreFromDrive(file) }
                            }
                        }
                    }
                }
            }
        }
        restoreStatus?.let { item { Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary) } }
    }
}

@Composable
private fun RestoreFileCard(
    name: String,
    onClick: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(12.dp)) {
            Icon(Icons.Rounded.Restore, null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(8.dp))
            Text(name, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** Password prompt for a password-protected (PortableBackupCrypto) backup. */
@Composable
private fun RestorePasswordDialog(
    wrongPassword: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.restore_password_title)) },
        text = {
            Column {
                Text(stringResource(R.string.restore_password_message), fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                if (wrongPassword) {
                    Text(stringResource(R.string.restore_password_wrong), color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.backup_password_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password) }, enabled = password.isNotEmpty()) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
