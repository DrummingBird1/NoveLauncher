package com.ailauncher.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.ailauncher.app.R
import com.ailauncher.app.domain.models.SmartControlSettings
import com.ailauncher.app.ui.LockGestureSetupDialog
import com.ailauncher.app.ui.gestures.GestureActions
import com.ailauncher.app.ui.gestures.LockScreenAccessibilityService

/**
 * v10: home-screen gestures. The four settings existed in SmartControlSettings
 * since v5 with no UI and no effect; LauncherRoot/HomeScreen now honour them.
 * The long-press vibration switch (hapticFeedback, read by HomeScreen and
 * AppsScreen) lives here too.
 *
 * Double-tap-to-lock needs the accessibility helper. The explanation card below
 * is the prominent disclosure Google Play requires before sending the user to
 * enable it: what it does, what it can't do, and that it's optional. Switching
 * double-tap on while the helper is off also shows it as LockGestureSetupDialog,
 * so the disclosure and consent come at the moment the user opts in.
 */
@Composable
fun GesturesSection(
    settings: SmartControlSettings,
    onUpdate: (SmartControlSettings) -> Unit,
) {
    val context = LocalContext.current
    var helperEnabled by remember { mutableStateOf(LockScreenAccessibilityService.isEnabled(context)) }
    var showDisclosure by remember { mutableStateOf(false) }
    // Re-check whenever the user comes back from system settings.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            helperEnabled = LockScreenAccessibilityService.isEnabled(context)
        }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionLabel(stringResource(R.string.settings_gestures)) }
        item {
            SwitchSetting(
                stringResource(R.string.gestures_enabled),
                settings.gesturesEnabled,
            ) { onUpdate(settings.copy(gesturesEnabled = it)) }
        }
        if (settings.gesturesEnabled) {
            item {
                SwitchSetting(
                    stringResource(R.string.gesture_swipe_up),
                    settings.swipeUpAppDrawer,
                ) { onUpdate(settings.copy(swipeUpAppDrawer = it)) }
            }
            item {
                SwitchSetting(stringResource(R.string.gesture_swipe_down), settings.swipeDownNotifications) {
                    onUpdate(settings.copy(swipeDownNotifications = it))
                }
            }
            if (LockScreenAccessibilityService.isLockSupported) {
                item {
                    SwitchSetting(stringResource(R.string.gesture_double_tap_lock), settings.doubleTapToSleep) {
                        // Switching on without the helper shows the disclosure first; the
                        // switch only turns on if the user agrees to go enable it.
                        if (it && !helperEnabled) showDisclosure = true else onUpdate(settings.copy(doubleTapToSleep = it))
                    }
                }
            }
        }
        // Not a home gesture, so it stays visible with gestures switched off.
        item {
            SwitchSetting(stringResource(R.string.gesture_haptic_long_press), settings.hapticFeedback) {
                onUpdate(settings.copy(hapticFeedback = it))
            }
        }

        if (LockScreenAccessibilityService.isLockSupported) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.gesture_lock_setup_title), color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(if (helperEnabled) R.string.gesture_helper_status_on else R.string.gesture_helper_status_off),
                            fontSize = 13.sp,
                            color = if (helperEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.gesture_lock_setup_body),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.gesture_restricted_hint),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { GestureActions.openAccessibilitySettings(context) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(if (helperEnabled) R.string.gesture_helper_manage else R.string.gesture_lock_setup_open))
                        }
                    }
                }
            }
        }
    }

    if (showDisclosure) {
        LockGestureSetupDialog(
            onDismiss = { showDisclosure = false },
            onConsent = { onUpdate(settings.copy(doubleTapToSleep = true)) },
        )
    }
}
