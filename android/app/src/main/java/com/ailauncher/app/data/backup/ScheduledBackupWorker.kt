package com.ailauncher.app.data.backup

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.ailauncher.app.AILauncherApp
import com.ailauncher.app.R
import com.ailauncher.app.data.SettingsRepository
import com.ailauncher.app.domain.models.BackupSchedule
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * v6 feature: Scheduled backup worker.
 * WorkManager job that runs on the configured schedule (daily/weekly/monthly)
 * and performs backup to the configured destination.
 */
@HiltWorker
class ScheduledBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupManager: BackupManager,
    private val settingsRepo: SettingsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val settings = settingsRepo.backupFlow.first()
            if (!settings.autoBackupEnabled) return Result.success()

            when (val result = backupManager.backup(settings.autoBackupDestination)) {
                is BackupManager.BackupResult.Success -> {
                    settingsRepo.saveBackup(settings.copy(lastBackupTimestamp = System.currentTimeMillis()))
                    Result.success()
                }
                is BackupManager.BackupResult.Error -> {
                    notifyFailure(result.message)
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            notifyFailure(e.message ?: applicationContext.getString(R.string.notification_backup_failed_unknown))
            Result.retry()
        }
    }

    /** Previously silent — a scheduled backup could fail indefinitely with no
     *  user-visible signal beyond a stale lastBackupTimestamp nobody checks. */
    private fun notifyFailure(message: String) {
        try {
            val notification = NotificationCompat.Builder(applicationContext, AILauncherApp.BACKUP_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle(applicationContext.getString(R.string.notification_backup_failed_title))
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(applicationContext).notify(BACKUP_FAILURE_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted (API 33+, no runtime request flow yet) —
            // same silent outcome as before this feature existed, not a regression.
        }
    }

    companion object {
        const val WORK_NAME = "scheduled_backup"
        private const val BACKUP_FAILURE_NOTIFICATION_ID = 2001

        fun schedule(context: Context, schedule: BackupSchedule) {
            val wm = WorkManager.getInstance(context)
            if (schedule == BackupSchedule.MANUAL) {
                wm.cancelUniqueWork(WORK_NAME)
                return
            }
            val interval = when (schedule) {
                BackupSchedule.DAILY -> 1L
                BackupSchedule.WEEKLY -> 7L
                BackupSchedule.MONTHLY -> 30L
                BackupSchedule.MANUAL -> return
            }
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            val request = PeriodicWorkRequestBuilder<ScheduledBackupWorker>(interval, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()
            wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
