package com.ailauncher.app.domain.repository

import com.ailauncher.app.domain.models.AdaptiveDisplaySettings
import com.ailauncher.app.domain.models.AppearanceSettings
import com.ailauncher.app.domain.models.BackupSettings
import com.ailauncher.app.domain.models.HiddenAppsSettings
import com.ailauncher.app.domain.models.NewsSettings
import com.ailauncher.app.domain.models.OnboardingState
import com.ailauncher.app.domain.models.PagesSettings
import com.ailauncher.app.domain.models.RepairSettings
import com.ailauncher.app.domain.models.SecuritySettings
import com.ailauncher.app.domain.models.SmartControlSettings
import com.ailauncher.app.domain.models.WidgetSlot
import kotlinx.coroutines.flow.Flow

/**
 * v9: Domain-layer contract for the launcher's settings store. The single
 * implementation lives in [com.ailauncher.app.data.SettingsRepository] (the
 * existing class was kept in place — only the abstraction was added).
 *
 * Why now: lets unit tests construct a fake that emits canned Flows without
 * spinning up DataStore, AndroidKeyStore, or coroutines plumbing. Existing
 * production wiring through Hilt is unchanged.
 *
 * Why not split the class itself: every call site already named the type as a
 * `class SettingsRepository`. Renaming would touch ~50 call sites and the only
 * win is naming purity — not worth the diff. The interface and the implementation
 * share a fully-qualified name (`...repository.SettingsRepository` vs.
 * `...data.SettingsRepository`); imports disambiguate.
 */
interface SettingsRepository {
    val appearanceFlow: Flow<AppearanceSettings>
    val pagesFlow: Flow<PagesSettings>
    val securityFlow: Flow<SecuritySettings>
    val backupFlow: Flow<BackupSettings>
    val widgetsFlow: Flow<List<WidgetSlot>>
    val newsFlow: Flow<NewsSettings>
    val hiddenAppsFlow: Flow<HiddenAppsSettings>
    val smartControlFlow: Flow<SmartControlSettings>
    val adaptiveDisplayFlow: Flow<AdaptiveDisplaySettings>
    val repairFlow: Flow<RepairSettings>
    val onboardingFlow: Flow<OnboardingState>

    suspend fun saveAppearance(s: AppearanceSettings)
    suspend fun savePages(s: PagesSettings)
    suspend fun saveSecurity(s: SecuritySettings)
    suspend fun saveBackup(s: BackupSettings)
    suspend fun saveWidgets(w: List<WidgetSlot>)
    suspend fun saveNews(s: NewsSettings)
    suspend fun saveHiddenApps(s: HiddenAppsSettings)
    suspend fun saveSmartControl(s: SmartControlSettings)
    suspend fun saveAdaptiveDisplay(s: AdaptiveDisplaySettings)
    suspend fun saveRepair(s: RepairSettings)
    suspend fun saveOnboarding(s: OnboardingState)

    suspend fun exportAllSettings(): String
    suspend fun importAllSettings(jsonStr: String): Boolean

    /** Same export, wrapped in password-derived AES-256-GCM (see PortableBackupCrypto). */
    suspend fun exportAllSettingsEncrypted(password: String): String

    /**
     * Accepts both a [PortableBackupCrypto]-encrypted export and a legacy plain-JSON
     * one — [password] is ignored for plain-JSON content. Returns false on a wrong
     * password, a corrupt file, or any of [importAllSettings]'s existing failure modes.
     */
    suspend fun importAllSettingsEncrypted(content: String, password: String): Boolean

    suspend fun resetAll()
    suspend fun recordCrash()
    suspend fun repairApp()
}
