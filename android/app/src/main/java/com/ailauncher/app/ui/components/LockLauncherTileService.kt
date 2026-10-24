package com.ailauncher.app.ui.components

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.StringRes
import com.ailauncher.app.AppLocales
import com.ailauncher.app.R
import com.ailauncher.app.domain.models.LockMethod
import com.ailauncher.app.security.AppLockManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Quick Settings Tile: "Lock NoveLauncher now". Expires the unlock grace
 * period immediately (AppLockManager.revokeLauncherUnlock) rather than
 * toggling launcherLockMethod on/off — that field also carries which
 * credential type is configured, so flipping it to NONE would silently
 * disable the user's chosen lock instead of just re-prompting for it.
 */
@AndroidEntryPoint
class LockLauncherTileService : TileService() {
    @Inject lateinit var appLockManager: AppLockManager
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            if (appLockManager.getLauncherLockMethod() == LockMethod.NONE) {
                showToast(R.string.qs_tile_no_lock_configured)
                return@launch
            }
            appLockManager.revokeLauncherUnlock()
            showToast(R.string.qs_tile_locked)
        }
    }

    private fun refreshTile() {
        scope.launch {
            val hasLock = appLockManager.getLauncherLockMethod() != LockMethod.NONE
            qsTile?.apply {
                state = if (hasLock) Tile.STATE_INACTIVE else Tile.STATE_UNAVAILABLE
                updateTile()
            }
        }
    }

    private fun showToast(
        @StringRes message: Int,
    ) {
        // TileService callbacks run off the main thread; Toast needs Main.
        CoroutineScope(Dispatchers.Main).launch {
            // In the in-app language — below Android 13 a Service's own context doesn't carry it.
            val text = AppLocales.wrap(this@LockLauncherTileService).getString(message)
            Toast.makeText(this@LockLauncherTileService, text, Toast.LENGTH_SHORT).show()
        }
    }
}
