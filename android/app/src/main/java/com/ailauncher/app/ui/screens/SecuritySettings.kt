package com.ailauncher.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ailauncher.app.R
import com.ailauncher.app.data.SettingsRepository
import com.ailauncher.app.domain.models.LockMethod
import com.ailauncher.app.domain.models.SecuritySettings
import com.ailauncher.app.security.AppLockManager
import com.ailauncher.app.security.RootDetection
import kotlinx.coroutines.launch

/**
 * v9: extracted from SettingsActivity.kt. SECURITY + APP_LOCK_LIST SettingsPages
 * and the three credential-setup dialogs (PIN / password / pattern).
 *
 * Why grouped: the dialogs are tightly coupled to the section that opens them,
 * and the AppLockList page is a one-screen drill-down from Security.
 */
@Composable
fun SecuritySection(
    security: SecuritySettings,
    appLockManager: AppLockManager,
    settingsRepo: SettingsRepository,
    onNavigate: (SettingsPage) -> Unit
) {
    val scope = rememberCoroutineScope()
    var showPinDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showPatternDialog by remember { mutableStateOf(false) }
    var pinTarget by remember { mutableStateOf("app") }
    // v9.3: informational only — see RootDetection kdoc. remember{} so the
    // (cheap but not free) file-existence checks run once per composition,
    // not on every recomposition of this section.
    val likelyRooted = remember { RootDetection.isLikelyRooted() }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionLabel(stringResource(R.string.settings_app_lock_list)) }
        item {
            val methodName = stringResource(security.appLockMethod.displayNameRes)
            Text(stringResource(R.string.security_method_label, methodName), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        items(LockMethod.entries.toList()) { method ->
            Card(
                Modifier.fillMaxWidth().clickable {
                    scope.launch {
                        when (method) {
                            LockMethod.NONE -> settingsRepo.saveSecurity(security.copy(appLockMethod = LockMethod.NONE))
                            LockMethod.PIN -> { pinTarget = "app"; showPinDialog = true }
                            LockMethod.PASSWORD -> { pinTarget = "app"; showPasswordDialog = true }
                            LockMethod.PATTERN -> { showPatternDialog = true }
                            LockMethod.FINGERPRINT -> appLockManager.setAppLockBiometric(LockMethod.FINGERPRINT)
                            LockMethod.FACE -> appLockManager.setAppLockBiometric(LockMethod.FACE)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (security.appLockMethod == method) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = security.appLockMethod == method, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(method.displayNameRes), color = MaterialTheme.colorScheme.onSurface)
                    if (method == LockMethod.FINGERPRINT || method == LockMethod.FACE) {
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Rounded.Fingerprint, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth().clickable { onNavigate(SettingsPage.APP_LOCK_LIST) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.security_choose_apps), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.security_locked_count, security.lockedAppPackages.size), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.security_isolation_notice),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        if (likelyRooted) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.security_root_warning),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        item { HorizontalDivider() }
        item { SectionLabel(stringResource(R.string.section_screen_lock)) }
        item { SwitchSetting(stringResource(R.string.security_layout_lock), security.isLayoutLocked) { scope.launch { settingsRepo.saveSecurity(security.copy(isLayoutLocked = it)) } } }

        item { HorizontalDivider() }
        item { SectionLabel(stringResource(R.string.section_safety_launcher_lock)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pinTarget = "launcher"; showPinDialog = true }) { Text(stringResource(R.string.security_method_pin)) }
                OutlinedButton(onClick = { pinTarget = "launcher"; showPasswordDialog = true }) { Text(stringResource(R.string.security_method_password)) }
                OutlinedButton(onClick = { scope.launch { appLockManager.setLauncherLockBiometric(LockMethod.FINGERPRINT) } }) { Text(stringResource(R.string.security_method_biometric)) }
            }
        }
    }

    if (showPinDialog) PinDialog({ showPinDialog = false }) { pin ->
        scope.launch {
            if (pinTarget == "app") appLockManager.setAppLockPin(pin) else appLockManager.setLauncherLockPin(pin)
            showPinDialog = false
        }
    }
    if (showPasswordDialog) PasswordDialog({ showPasswordDialog = false }) { pw ->
        scope.launch {
            if (pinTarget == "app") appLockManager.setAppLockPassword(pw) else {
                settingsRepo.saveSecurity(security.copy(launcherLockMethod = LockMethod.PASSWORD, launcherLockPassword = pw))
            }
            showPasswordDialog = false
        }
    }
    if (showPatternDialog) PatternDialog({ showPatternDialog = false }) { pattern ->
        scope.launch { appLockManager.setAppLockPattern(pattern); showPatternDialog = false }
    }
}

@Composable
fun AppLockListSection(security: SecuritySettings, appLockManager: AppLockManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apps = remember {
        val pm = context.packageManager
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply { addCategory(android.content.Intent.CATEGORY_LAUNCHER) }
        pm.queryIntentActivities(intent, 0).mapNotNull { ri ->
            val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
            if (pkg == context.packageName) return@mapNotNull null
            pkg to ri.loadLabel(pm).toString()
        }.sortedBy { it.second }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { SectionLabel(stringResource(R.string.security_choose_apps_to_lock)) }
        items(apps) { (pkg, label) ->
            val isLocked = pkg in security.lockedAppPackages
            Row(
                Modifier.fillMaxWidth().clickable { scope.launch { appLockManager.toggleAppLock(pkg) } }.padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = isLocked, onCheckedChange = { scope.launch { appLockManager.toggleAppLock(pkg) } })
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(label, color = MaterialTheme.colorScheme.onSurface)
                    Text(pkg, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isLocked) {
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Rounded.Lock, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun PinDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(1) }
    var error by remember { mutableStateOf<String?>(null) }
    val invalidLengthText = stringResource(R.string.pin_invalid_length)
    val mismatchText = stringResource(R.string.mismatch_error)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (step == 1) R.string.dialog_set_pin else R.string.dialog_confirm_pin)) },
        text = {
            Column {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp); Spacer(Modifier.height(8.dp)) }
                OutlinedTextField(
                    value = if (step == 1) pin else confirm,
                    onValueChange = { if (step == 1) pin = it else confirm = it },
                    label = { Text(stringResource(R.string.pin_input_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (step == 1) {
                    if (pin.length in 4..6 && pin.all { it.isDigit() }) { step = 2; error = null }
                    else error = invalidLengthText
                } else {
                    if (confirm == pin) onConfirm(pin)
                    else { error = mismatchText; confirm = "" }
                }
            }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
fun PasswordDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pw by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(1) }
    var error by remember { mutableStateOf<String?>(null) }
    val invalidLengthText = stringResource(R.string.password_invalid_length)
    val mismatchText = stringResource(R.string.mismatch_error)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (step == 1) R.string.dialog_set_password else R.string.dialog_confirm_password)) },
        text = {
            Column {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp); Spacer(Modifier.height(8.dp)) }
                OutlinedTextField(
                    value = if (step == 1) pw else confirm,
                    onValueChange = { if (step == 1) pw = it else confirm = it },
                    label = { Text(stringResource(R.string.password_input_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (step == 1) {
                    if (pw.length >= 4) { step = 2; error = null }
                    else error = invalidLengthText
                } else {
                    if (confirm == pw) onConfirm(pw)
                    else { error = mismatchText; confirm = "" }
                }
            }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
fun PatternDialog(
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.dialog_set_pattern),
    onConfirm: (List<Int>) -> Unit
) {
    var pattern by remember { mutableStateOf(listOf<Int>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.pattern_instructions), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    for (row in 0..2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            for (col in 0..2) {
                                val idx = row * 3 + col
                                val isSelected = idx in pattern
                                val order = pattern.indexOf(idx)
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable {
                                            pattern = if (isSelected) pattern.filter { it != idx }
                                            else pattern + idx
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Text("${order + 1}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { pattern = emptyList() }) { Text(stringResource(R.string.action_reset)) }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (pattern.size >= 4) onConfirm(pattern) }, enabled = pattern.size >= 4) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
