package com.ailauncher.app.ui

import android.app.Activity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ailauncher.app.R
import com.ailauncher.app.domain.models.LockMethod
import com.ailauncher.app.security.AppLockManager
import com.ailauncher.app.ui.screens.PatternDialog
import kotlinx.coroutines.launch

/**
 * v7 FIX #8 / v10: an app (or one of its shortcuts) waiting on a PIN, password
 * or pattern before it opens. [onUnlocked] is whatever was going to happen —
 * launch the app, start the shortcut — so one dialog serves every entry point.
 */
data class PendingUnlock(
    val packageName: String,
    val method: LockMethod,
    val onUnlocked: () -> Unit
)

/**
 * v10: moved out of LauncherActivity. Verifies the credential through
 * [AppLockManager], grants the 5-minute unlock window, then runs the pending
 * action. FLAG_SECURE while shown keeps credential entry out of screenshots and
 * the recents thumbnail.
 */
@Composable
fun UnlockDialog(pending: PendingUnlock, appLockManager: AppLockManager, onDismiss: () -> Unit) {
    var input by remember(pending) { mutableStateOf("") }
    var error by remember(pending) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val wrongText = stringResource(R.string.auth_wrong_try_again)

    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    fun succeed() {
        appLockManager.grantAppUnlock(pending.packageName)
        onDismiss()
        pending.onUnlocked()
    }

    // v9: PATTERN unlock previously fell through to `else -> false`, so a
    // pattern-locked app could never be opened. Render the 3x3 grid and verify it.
    if (pending.method == LockMethod.PATTERN) {
        PatternDialog(onDismiss = onDismiss, title = stringResource(R.string.auth_required_title)) { drawn ->
            scope.launch {
                if (appLockManager.verifyPattern(drawn)) succeed()
                else {
                    Toast.makeText(context, wrongText, Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            }
        }
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.auth_required_title)) },
        text = {
            Column {
                Text(
                    stringResource(
                        if (pending.method == LockMethod.PIN) R.string.auth_app_locked_with_pin
                        else R.string.auth_app_locked_with_password
                    ),
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; error = null },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = error != null
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    val ok = when (pending.method) {
                        LockMethod.PIN -> appLockManager.verifyPin(input)
                        LockMethod.PASSWORD -> appLockManager.verifyPassword(input)
                        else -> false
                    }
                    if (ok) succeed() else {
                        error = wrongText
                        input = ""
                    }
                }
            }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
