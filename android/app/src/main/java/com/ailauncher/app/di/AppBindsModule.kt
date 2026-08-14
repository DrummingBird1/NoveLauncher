package com.ailauncher.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * v9.3: @Binds aliases for the two repositories that have domain-layer interfaces
 * (see domain/repository/). [AppModule]'s @Provides functions remain the only
 * place that knows how to *construct* the concrete classes — this module just
 * makes the interface type resolvable too, so a test module can swap in a fake
 * by providing `domain.repository.SettingsRepository`/`InstalledAppsRepository`
 * without touching AppModule at all. Existing call sites that inject the
 * concrete `data.*` types are unaffected; both bindings coexist.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindsModule {

    @Binds
    abstract fun bindSettingsRepository(
        impl: com.ailauncher.app.data.SettingsRepository
    ): com.ailauncher.app.domain.repository.SettingsRepository

    @Binds
    abstract fun bindInstalledAppsRepository(
        impl: com.ailauncher.app.data.InstalledAppsRepository
    ): com.ailauncher.app.domain.repository.InstalledAppsRepository
}
