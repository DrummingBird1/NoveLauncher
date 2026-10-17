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
import com.ailauncher.app.data.db.MIGRATION_1_2
import com.ailauncher.app.data.db.MIGRATION_2_3
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
    fun provideAppWidgetHost(
        @ApplicationContext context: Context,
    ): AppWidgetHost = AppWidgetHost(context, AILauncherApp.WIDGET_HOST_ID)

    @Provides @Singleton
    fun provideLauncherDatabase(
        @ApplicationContext context: Context,
    ): LauncherDatabase =
        Room
            .databaseBuilder(context, LauncherDatabase::class.java, "launcher.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            // Allow data loss only on downgrade; future upgrades MUST add a Migration.
            // The DB only holds derived caches (usage, notifications, predictions), so a
            // user-initiated reinstall is the recovery path if schema changes break it.
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides fun provideUsageDao(db: LauncherDatabase): UsageDao = db.usageDao()

    @Provides fun provideNotificationDao(db: LauncherDatabase): NotificationDao = db.notificationDao()

    @Provides fun provideSuggestionFeedbackDao(db: LauncherDatabase): com.ailauncher.app.data.db.SuggestionFeedbackDao =
        db
            .suggestionFeedbackDao()

    @Provides @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context,
    ): SettingsRepository = SettingsRepository(context)

    @Provides @Singleton
    fun provideAppCategoryProvider(
        @ApplicationContext context: Context,
    ): AppCategoryProvider = AppCategoryProvider(context)

    @Provides @Singleton
    fun provideUsageStatsRepository(
        @ApplicationContext context: Context,
    ): UsageStatsRepository = UsageStatsRepository(context)

    @Provides @Singleton
    fun provideInstalledAppsRepository(
        @ApplicationContext context: Context,
        categoryProvider: AppCategoryProvider,
        iconCache: com.ailauncher.app.data.IconCache,
        iconPackManager: com.ailauncher.app.data.iconpack.IconPackManager,
    ): InstalledAppsRepository = InstalledAppsRepository(context, categoryProvider, iconCache, iconPackManager)

    @Provides @Singleton
    fun provideAppLockManager(
        @ApplicationContext context: Context,
        settingsRepo: SettingsRepository,
    ): AppLockManager = AppLockManager(context, settingsRepo)

    @Provides @Singleton
    fun provideBiometricHelper(
        @ApplicationContext context: Context,
    ): BiometricHelper = BiometricHelper(context)

    @Provides @Singleton
    fun provideBackupManager(
        @ApplicationContext context: Context,
        settingsRepo: SettingsRepository,
    ): BackupManager = BackupManager(context, settingsRepo)

    // FIX v7: Use case now correctly injects new dependencies
    @Provides @Singleton
    fun provideGetRankedAppsUseCase(
        appsRepo: InstalledAppsRepository,
        usageRepo: UsageStatsRepository,
        categoryProvider: AppCategoryProvider,
        notificationDao: NotificationDao,
        predictionEngine: AppPredictionEngine,
        settingsRepo: SettingsRepository,
        feedbackRepo: com.ailauncher.app.data.ml.SuggestionFeedbackRepository,
        contextSignals: com.ailauncher.app.data.ContextSignals,
        jewishCalendar: com.ailauncher.app.data.JewishCalendarService,
    ): GetRankedAppsUseCase =
        GetRankedAppsUseCase(
            appsRepo,
            usageRepo,
            categoryProvider,
            notificationDao,
            predictionEngine,
            settingsRepo,
            feedbackRepo,
            contextSignals,
            jewishCalendar,
        )
}
