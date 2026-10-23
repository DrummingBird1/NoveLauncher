package com.ailauncher.app.ui.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.ailauncher.app.R
import com.ailauncher.app.data.location.CoarseLocation
import timber.log.Timber

/**
 * Approximate location (ACCESS_COARSE_LOCATION — never FINE), asked for in
 * context. Weather and the Jewish-calendar times use the default city until it's
 * granted ([CoarseLocation]), so the app only asks when the user does something
 * that needs it: the onboarding step, switching on Shabbat times or Shabbat
 * mode, or a "Use my location" button. A switch asks at most once per install;
 * after that only the buttons ask, and once Android stops showing its dialog
 * (denied twice, or "don't ask again") they open the app's system settings page.
 */
@Stable
class LocationPermissionState internal constructor(
    private val context: Context,
) {
    var granted by mutableStateOf(CoarseLocation.isGranted(context))
        private set

    /** Android won't show its dialog again; only the system settings page can grant it now. */
    var blocked by mutableStateOf(false)
        private set

    internal var launcher: ActivityResultLauncher<String>? = null

    /** For the buttons: Android's dialog, or the app's settings page once Android won't show it. */
    fun request() {
        when {
            granted -> Unit
            blocked -> openAppSettings()
            else -> launch()
        }
    }

    /** For a switch being turned on: asks only if this install never asked before. */
    fun requestIfNeverAsked() {
        if (!granted && !wasAsked()) launch()
    }

    internal fun onResult() {
        prefs().edit().putBoolean(KEY_ASKED, true).apply()
        refresh()
    }

    internal fun refresh() {
        granted = CoarseLocation.isGranted(context)
        val activity = context.findActivity()
        blocked = !granted && wasAsked() && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, PERMISSION)
    }

    private fun launch() {
        launcher?.launch(PERMISSION)
    }

    private fun openAppSettings() {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
            )
        } catch (e: Exception) {
            Timber.w(e, "Could not open the app's settings page")
        }
    }

    private fun wasAsked() = prefs().getBoolean(KEY_ASKED, false)

    // Per device, not a setting: not in the settings export, and excluded from auto-backup.
    private fun prefs() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private companion object {
        const val PERMISSION = Manifest.permission.ACCESS_COARSE_LOCATION
        const val PREFS = "location_permission"
        const val KEY_ASKED = "asked"
    }
}

@Composable
fun rememberLocationPermission(): LocationPermissionState {
    val context = LocalContext.current
    val state = remember(context) { LocationPermissionState(context) }
    state.launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { state.onResult() }
    // Re-check on every resume: the user may have changed it in system settings.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, state) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { state.refresh() }
    }
    return state
}

/** While location isn't granted: says the default city is in use, with a button to change that. */
@Composable
fun DefaultCityNote(
    permission: LocationPermissionState,
    modifier: Modifier = Modifier,
) {
    if (permission.granted) return
    Column(modifier) {
        Text(stringResource(R.string.location_default_city), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = permission::request) {
            Text(stringResource(if (permission.blocked) R.string.location_allow_in_settings else R.string.location_allow))
        }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
