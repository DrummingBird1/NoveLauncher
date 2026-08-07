package com.ailauncher.app.data.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
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
                is BackupManager.BackupResult.Error -> Result.retry()
            }
        } catch (_: Exception) { Result.retry() }
    }

    companion object {
        const val WORK_NAME = "scheduled_backup"

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
