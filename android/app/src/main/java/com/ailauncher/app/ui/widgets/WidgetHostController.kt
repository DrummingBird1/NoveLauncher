package com.ailauncher.app.ui.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import timber.log.Timber
import kotlin.math.ceil

/**
 * v10: the full "add a widget" flow, which used to stop after the picker.
 *
 * 1. Allocate an id and open the system widget picker (it handles the bind
 *    permission dialog itself).
 * 2. If the widget declares a configuration activity — weather, notes, photo
 *    widgets… — run it. Skipping this left those widgets permanently empty.
 * 3. Only then save the slot, sized from the widget's own minimum height.
 *
 * Every exit path that doesn't end in a saved widget releases the id with
 * [AppWidgetHost.deleteAppWidgetId]; a cancelled picker or configuration used to
 * leak one bound id each time.
 *
 * The configuration step must go through
 * [AppWidgetHost.startAppWidgetConfigureActivityForResult] (configuration
 * activities are usually not exported, and only the host's IntentSender may
 * start them), which reports back through the Activity's onActivityResult —
 * so the Activity forwards that to [onActivityResult].
 */
class WidgetHostController(
    private val activity: ComponentActivity,
    private val host: () -> AppWidgetHost,
    private val onWidgetReady: (widgetId: Int, spanY: Int) -> Unit
) {
    private var pendingId = AppWidgetManager.INVALID_APPWIDGET_ID

    private val picker = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?.takeIf { it != AppWidgetManager.INVALID_APPWIDGET_ID } ?: pendingId
        if (result.resultCode != Activity.RESULT_OK || id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            release(pendingId)
        } else {
            configureOrFinish(id)
        }
    }

    fun pick() {
        pendingId = host().allocateAppWidgetId()
        picker.launch(
            Intent(AppWidgetManager.ACTION_APPWIDGET_PICK)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingId)
                // Some OEM pickers crash without these (empty) custom-widget lists.
                .putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, ArrayList<Parcelable>())
                .putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, ArrayList<Parcelable>())
        )
    }

    private fun configureOrFinish(id: Int) {
        val info = AppWidgetManager.getInstance(activity).getAppWidgetInfo(id)
        if (info == null) {
            release(id)
            return
        }
        val configOptional = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            (info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL) != 0
        if (info.configure != null && !configOptional) {
            pendingId = id
            try {
                host().startAppWidgetConfigureActivityForResult(activity, id, 0, REQUEST_CONFIGURE, null)
                return
            } catch (e: Exception) {
                // Broken provider (activity missing). Add it unconfigured rather than losing it.
                Timber.w(e, "Widget configuration activity failed to start")
            }
        }
        finish(id, info)
    }

    /** Forward from Activity.onActivityResult. Returns true if the result was ours. */
    fun onActivityResult(requestCode: Int, resultCode: Int): Boolean {
        if (requestCode != REQUEST_CONFIGURE) return false
        val id = pendingId
        val info = AppWidgetManager.getInstance(activity).getAppWidgetInfo(id)
        if (resultCode == Activity.RESULT_OK && info != null) finish(id, info) else release(id)
        return true
    }

    fun onSaveInstanceState(out: Bundle) = out.putInt(STATE_PENDING_ID, pendingId)

    fun onRestoreInstanceState(state: Bundle?) {
        pendingId = state?.getInt(STATE_PENDING_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
    }

    private fun finish(id: Int, info: AppWidgetProviderInfo) {
        pendingId = AppWidgetManager.INVALID_APPWIDGET_ID
        val minHeightDp = info.minHeight / activity.resources.displayMetrics.density
        onWidgetReady(id, ceil(minHeightDp / ROW_HEIGHT_DP).toInt().coerceIn(1, 4))
    }

    private fun release(id: Int) {
        if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
            try { host().deleteAppWidgetId(id) } catch (e: Exception) { Timber.w(e, "deleteAppWidgetId failed") }
        }
        pendingId = AppWidgetManager.INVALID_APPWIDGET_ID
    }

    companion object {
        /** One WidgetSlot.spanY unit, matching HomeScreen's WidgetView height. */
        const val ROW_HEIGHT_DP = 80f
        private const val REQUEST_CONFIGURE = 0x4E01
        private const val STATE_PENDING_ID = "widget_pending_id"
    }
}
