package com.ailauncher.app.di

import android.appwidget.AppWidgetHost
import android.content.Context
import androidx.room.Room
import com.ailauncher.app.AILauncherApp
import com.ailauncher.app.data.AppCategoryProvider
import com.ailauncher.app.data.InstalledAppsRepository
import com.ailauncher.app.data.SettingsRepository
import com.ailauncher.app.data.UsageStatsRepository
import com.ailauncher.app.data.backup.BackupManager
import com.ailauncher.app.data.db.LauncherDatabase
import com.ailauncher.app.data.db.NotificationDao
import com.ailauncher.app.data.db.UsageDao
import com.ailauncher.app.data.ml.AppPredictionEngine
import com.ailauncher.app.domain.usecases.GetRankedAppsUseCase
import com.ailauncher.app.security.AppLockManager
import com.ailauncher.app.security.BiometricHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideAppWidgetHost(@ApplicationContext context: Context): AppWidgetHost =
        AppWidgetHost(context, AILauncherApp.WIDGET_HOST_ID)

    @Provides @Singleton
    fun provideLauncherDatabase(@ApplicationContext context: Context): LauncherDatabase =
        Room.databaseBuilder(context, LauncherDatabase::class.java, "launcher.db")
            // Allow data loss only on downgrade; future upgrades MUST add a Migration.
            // The DB only holds derived caches (usage, notifications, predictions), so a
            // user-initiated reinstall is the recovery path if schema changes break it.
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides fun provideUsageDao(db: LauncherDatabase): UsageDao = db.usageDao()
    @Provides fun provideNotificationDao(db: LauncherDatabase): NotificationDao = db.notificationDao()

    @Provides @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepository(context)

    @Provides @Singleton
    fun provideAppCategoryProvider(@ApplicationContext context: Context): AppCategoryProvider =
        AppCategoryProvider(context)

    @Provides @Singleton
    fun provideUsageStatsRepository(@ApplicationContext context: Context): UsageStatsRepository =
        UsageStatsRepository(context)

    @Provides @Singleton
    fun provideInstalledAppsRepository(
        @ApplicationContext context: Context,
        categoryProvider: AppCategoryProvider
    ): InstalledAppsRepository = InstalledAppsRepository(context, categoryProvider)

    @Provides @Singleton
    fun provideAppLockManager(
        @ApplicationContext context: Context,
        settingsRepo: SettingsRepository
    ): AppLockManager = AppLockManager(context, settingsRepo)

    @Provides @Singleton
    fun provideBiometricHelper(@ApplicationContext context: Context): BiometricHelper =
        BiometricHelper(context)

    @Provides @Singleton
    fun provideBackupManager(
        @ApplicationContext context: Context,
        settingsRepo: SettingsRepository
    ): BackupManager = BackupManager(context, settingsRepo)

    // FIX v7: Use case now correctly injects new dependencies
    @Provides @Singleton
    fun provideGetRankedAppsUseCase(
        appsRepo: InstalledAppsRepository,
        usageRepo: UsageStatsRepository,
        categoryProvider: AppCategoryProvider,
        notificationDao: NotificationDao,
        predictionEngine: AppPredictionEngine
    ): GetRankedAppsUseCase = GetRankedAppsUseCase(
        appsRepo, usageRepo, categoryProvider, notificationDao, predictionEngine
    )
}
