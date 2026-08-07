package com.ailauncher.app.data

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ailauncher.app.data.db.NotificationDao
import com.ailauncher.app.data.db.NotificationEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * v6 feature #15: Captures notifications to show unread badges on app icons.
 * v8 FIX: onNotificationRemoved now decrements, and onListenerConnected
 * reconciles the DB with the current active set so badges don't drift up
 * forever when notifications are dismissed from elsewhere.
 *
 * User must grant notification listener permission manually.
 * This runs as a bound service — automatically managed by Android.
 */
@AndroidEntryPoint
class LauncherNotificationListener : NotificationListenerService() {

    @Inject lateinit var notificationDao: NotificationDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        reconcileActive()
    }

    override fun onDestroy() {
        // v8 FIX: previously the SupervisorJob lived forever even after Android
        // tore down the service (e.g. when the user revokes notification access),
        // leaking the IO dispatcher slot. Cancel on destroy.
        scope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.isOngoing) return // skip persistent notifications
        val pkg = sbn.packageName ?: return
        scope.launch {
            val existing = notificationDao.get(pkg)
            if (existing == null) {
                notificationDao.upsert(NotificationEntity(
                    packageName = pkg,
                    unreadCount = 1,
                    lastNotificationTime = System.currentTimeMillis()
                ))
            } else {
                notificationDao.incrementUnread(pkg, System.currentTimeMillis())
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Re-reconcile rather than decrement blindly: a single package may have
        // many active notifications, and removal can be triggered by the OS,
        // the originating app, or the user. Counting from the active set is
        // the only way to stay correct.
        reconcileActive()
    }

    /**
     * Walks [getActiveNotifications] (excluding ongoing) and rewrites the unread
     * count for every package — both those with active notifications and those
     * tracked in DB whose notifications have all been dismissed. This is what
     * keeps badges in sync after the user clears the shade outside our control.
     */
    private fun reconcileActive() {
        scope.launch {
            try {
                val active = try { activeNotifications } catch (_: Exception) { return@launch }
                val counts = mutableMapOf<String, Int>()
                for (sbn in active) {
                    if (sbn.isOngoing) continue
                    val pkg = sbn.packageName ?: continue
                    counts[pkg] = (counts[pkg] ?: 0) + 1
                }
                val now = System.currentTimeMillis()
                // Zero out anything currently tracked that no longer has notifications.
                notificationDao.getAppsWithUnread()
                    .filter { it !in counts }
                    .forEach { notificationDao.markOpened(it, now) }
                // Update counts for currently active.
                counts.forEach { (pkg, count) ->
                    val existing = notificationDao.get(pkg)
                    notificationDao.upsert(
                        NotificationEntity(
                            packageName = pkg,
                            unreadCount = count,
                            lastNotificationTime = existing?.lastNotificationTime ?: now,
                            lastOpenedTime = existing?.lastOpenedTime ?: 0L
                        )
                    )
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Called by LauncherActivity when the user launches an app.
     * Resets the unread count for that package.
     */
    companion object {
        suspend fun markAppOpened(dao: NotificationDao, packageName: String) {
            dao.markOpened(packageName, System.currentTimeMillis())
        }
    }
}
