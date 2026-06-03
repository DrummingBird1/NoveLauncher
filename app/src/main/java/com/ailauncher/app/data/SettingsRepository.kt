package com.ailauncher.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ailauncher.app.domain.models.*
import com.ailauncher.app.security.SecureCrypto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_settings")

/**
 * v9: now implements [com.ailauncher.app.domain.repository.SettingsRepository] so
 * unit tests can swap in a fake. Class name and package kept stable — existing
 * call sites that import `com.ailauncher.app.data.SettingsRepository` are
 * unaffected. New code that wants the domain abstraction should import the
 * interface from `com.ailauncher.app.domain.repository` instead.
 */
class SettingsRepository(private val context: Context)
    : com.ailauncher.app.domain.repository.SettingsRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

    // v8: SecuritySettings (PBKDF2 hashes + locked-package list) is encrypted before
    // landing in DataStore so it's useless if exfiltrated via Android auto-backup.
    // Legacy plain JSON values are still readable and get migrated on next save.
    private val secureCrypto by lazy { SecureCrypto() }

    companion object {
        private val K_APPEARANCE = stringPreferencesKey("appearance")
        private val K_PAGES = stringPreferencesKey("pages")
        private val K_SECURITY = stringPreferencesKey("security")
        private val K_BACKUP = stringPreferencesKey("backup")
        private val K_WIDGETS = stringPreferencesKey("widgets")
        private val K_NEWS = stringPreferencesKey("news")
        private val K_HIDDEN = stringPreferencesKey("hidden_apps")
        private val K_SMART = stringPreferencesKey("smart_control")
        private val K_ADAPTIVE = stringPreferencesKey("adaptive_display")
        private val K_REPAIR = stringPreferencesKey("repair")
        private val K_ONBOARDING = stringPreferencesKey("onboarding")
    }

    private inline fun <reified T> flow(key: Preferences.Key<String>, default: T): Flow<T> where T : Any =
        context.dataStore.data.map { prefs ->
            prefs[key]?.let { runCatching { json.decodeFromString<T>(it) }.getOrNull() } ?: default
        }

    private suspend inline fun <reified T> save(key: Preferences.Key<String>, value: T) where T : Any {
        context.dataStore.edit { it[key] = json.encodeToString(kotlinx.serialization.serializer<T>(), value) }
    }

    override val appearanceFlow: Flow<AppearanceSettings> = flow(K_APPEARANCE, AppearanceSettings())
    override val pagesFlow: Flow<PagesSettings> = flow(K_PAGES, PagesSettings())

    /**
     * v8: tries decrypt → plain-JSON fallback for legacy values. Both paths feed the
     * same SecuritySettings type so the rest of the app doesn't need to know.
     *
     * v9: per-emission memoisation. Without it, every active collector pays the
     * AES-GCM decrypt + JSON parse on every DataStore emission — and there are
     * ~6 collectors active during normal launcher use (HomeScreen, AppsScreen,
     * PersonalZoneScreen, SettingsActivity, AppLockManager.isAppLocked, plus the
     * cached StateFlow in the ViewModel). The DataStore Flow itself replays the
     * raw String on each downstream emission, so we cache (rawCiphertext → decoded
     * SecuritySettings) and skip the work when the ciphertext hasn't changed.
     */
    @Volatile private var securityCache: Pair<String, SecuritySettings>? = null
    override val securityFlow: Flow<SecuritySettings> = context.dataStore.data.map { prefs ->
        val raw = prefs[K_SECURITY] ?: return@map SecuritySettings()
        securityCache?.takeIf { it.first == raw }?.second?.let { return@map it }
        val plain = secureCrypto.decryptOrNull(raw) ?: raw  // legacy
        val decoded = runCatching { json.decodeFromString<SecuritySettings>(plain) }
            .getOrNull() ?: SecuritySettings()
        securityCache = raw to decoded
        decoded
    }
    override val backupFlow: Flow<BackupSettings> = flow(K_BACKUP, BackupSettings())
    override val widgetsFlow: Flow<List<WidgetSlot>> = flow(K_WIDGETS, emptyList<WidgetSlot>())
    override val newsFlow: Flow<NewsSettings> = flow(K_NEWS, NewsSettings())
    override val hiddenAppsFlow: Flow<HiddenAppsSettings> = flow(K_HIDDEN, HiddenAppsSettings())
    override val smartControlFlow: Flow<SmartControlSettings> = flow(K_SMART, SmartControlSettings())
    override val adaptiveDisplayFlow: Flow<AdaptiveDisplaySettings> = flow(K_ADAPTIVE, AdaptiveDisplaySettings())
    override val repairFlow: Flow<RepairSettings> = flow(K_REPAIR, RepairSettings())
    override val onboardingFlow: Flow<OnboardingState> = flow(K_ONBOARDING, OnboardingState())

    override suspend fun saveAppearance(s: AppearanceSettings) = save(K_APPEARANCE, s)
    override suspend fun savePages(s: PagesSettings) = save(K_PAGES, s)

    override suspend fun saveSecurity(s: SecuritySettings) {
        val plain = json.encodeToString(SecuritySettings.serializer(), s)
        val encrypted = try { secureCrypto.encrypt(plain) } catch (_: Exception) { plain }
        context.dataStore.edit { it[K_SECURITY] = encrypted }
    }
    override suspend fun saveBackup(s: BackupSettings) = save(K_BACKUP, s)
    override suspend fun saveWidgets(w: List<WidgetSlot>) = save(K_WIDGETS, w)
    override suspend fun saveNews(s: NewsSettings) = save(K_NEWS, s)
    override suspend fun saveHiddenApps(s: HiddenAppsSettings) = save(K_HIDDEN, s)
    override suspend fun saveSmartControl(s: SmartControlSettings) = save(K_SMART, s)
    override suspend fun saveAdaptiveDisplay(s: AdaptiveDisplaySettings) = save(K_ADAPTIVE, s)
    override suspend fun saveRepair(s: RepairSettings) = save(K_REPAIR, s)
    override suspend fun saveOnboarding(s: OnboardingState) = save(K_ONBOARDING, s)

    /**
     * v9: read the full DataStore Preferences object once, then decode every group
     * from that single snapshot. The previous implementation chained 11
     * `flow.first()` calls — each one could in theory race with a concurrent
     * `save*()` and pick up a half-modified state (e.g. new appearance + old
     * security). With one read, everything in the exported JSON describes
     * exactly the same on-disk moment.
     */
    override suspend fun exportAllSettings(): String {
        val prefs = context.dataStore.data.first()
        fun <T> decode(key: Preferences.Key<String>, default: T, serializer: kotlinx.serialization.KSerializer<T>): T {
            val raw = prefs[key] ?: return default
            return runCatching { json.decodeFromString(serializer, raw) }.getOrNull() ?: default
        }
        // SecuritySettings has the special encrypted-or-legacy decode path.
        val security = run {
            val raw = prefs[K_SECURITY] ?: return@run SecuritySettings()
            securityCache?.takeIf { it.first == raw }?.second ?: run {
                val plain = secureCrypto.decryptOrNull(raw) ?: raw
                runCatching { json.decodeFromString<SecuritySettings>(plain) }.getOrNull()
                    ?: SecuritySettings()
            }
        }
        val full = LauncherSettings(
            appearance = decode(K_APPEARANCE, AppearanceSettings(), AppearanceSettings.serializer()),
            pages = decode(K_PAGES, PagesSettings(), PagesSettings.serializer()),
            security = security,
            backup = decode(K_BACKUP, BackupSettings(), BackupSettings.serializer()),
            widgets = decode(K_WIDGETS, emptyList(), kotlinx.serialization.builtins.ListSerializer(WidgetSlot.serializer())),
            news = decode(K_NEWS, NewsSettings(), NewsSettings.serializer()),
            hiddenApps = decode(K_HIDDEN, HiddenAppsSettings(), HiddenAppsSettings.serializer()),
            smartControl = decode(K_SMART, SmartControlSettings(), SmartControlSettings.serializer()),
            adaptiveDisplay = decode(K_ADAPTIVE, AdaptiveDisplaySettings(), AdaptiveDisplaySettings.serializer()),
            repair = decode(K_REPAIR, RepairSettings(), RepairSettings.serializer()),
            onboarding = decode(K_ONBOARDING, OnboardingState(), OnboardingState.serializer())
        )
        return json.encodeToString(LauncherSettings.serializer(), full)
    }

    /**
     * v8 FIX: Decode first, then write everything in a single `edit { }` block so an
     * import either fully succeeds or leaves the previous settings untouched. Previously
     * 11 sequential save() calls left users in a half-imported state on any mid-flight
     * failure.
     */
    override suspend fun importAllSettings(jsonStr: String): Boolean {
        val s = try { json.decodeFromString<LauncherSettings>(jsonStr) }
                catch (_: Exception) { return false }
        return try {
            context.dataStore.edit { prefs ->
                fun <T> putJson(key: Preferences.Key<String>, value: T, serializer: kotlinx.serialization.KSerializer<T>) {
                    prefs[key] = json.encodeToString(serializer, value)
                }
                putJson(K_APPEARANCE, s.appearance, AppearanceSettings.serializer())
                putJson(K_PAGES, s.pages, PagesSettings.serializer())
                // SecuritySettings must go through the encrypted path.
                val secPlain = json.encodeToString(SecuritySettings.serializer(), s.security)
                prefs[K_SECURITY] = try { secureCrypto.encrypt(secPlain) } catch (_: Exception) { secPlain }
                putJson(K_BACKUP, s.backup, BackupSettings.serializer())
                putJson(K_WIDGETS, s.widgets, kotlinx.serialization.builtins.ListSerializer(WidgetSlot.serializer()))
                putJson(K_NEWS, s.news, NewsSettings.serializer())
                putJson(K_HIDDEN, s.hiddenApps, HiddenAppsSettings.serializer())
                putJson(K_SMART, s.smartControl, SmartControlSettings.serializer())
                putJson(K_ADAPTIVE, s.adaptiveDisplay, AdaptiveDisplaySettings.serializer())
                putJson(K_REPAIR, s.repair, RepairSettings.serializer())
                putJson(K_ONBOARDING, s.onboarding, OnboardingState.serializer())
            }
            true
        } catch (_: Exception) { false }
    }

    /** Reset everything to defaults */
    override suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
    }

    /** Increment crash counter for repair system */
    override suspend fun recordCrash() {
        val current = repairFlow.first()
        saveRepair(current.copy(crashCount = current.crashCount + 1))
    }

    override suspend fun repairApp() {
        saveRepair(RepairSettings(lastRepairTimestamp = System.currentTimeMillis(), crashCount = 0))
    }
}
