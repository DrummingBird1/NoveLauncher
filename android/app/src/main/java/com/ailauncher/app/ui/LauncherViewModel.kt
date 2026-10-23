package com.ailauncher.app.ui

import android.appwidget.AppWidgetHost
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ailauncher.app.data.IconCache
import com.ailauncher.app.data.InstalledAppsRepository
import com.ailauncher.app.data.JewishCalendarService
import com.ailauncher.app.data.SettingsRepository
import com.ailauncher.app.data.api.WeatherData
import com.ailauncher.app.data.api.WeatherService
import com.ailauncher.app.data.backup.BackupManager
import com.ailauncher.app.data.db.NotificationDao
import com.ailauncher.app.data.ml.AppPredictionEngine
import com.ailauncher.app.data.ml.SuggestionFeedbackRepository
import com.ailauncher.app.data.update.UpdateChecker
import com.ailauncher.app.domain.calendar.JewishDayInfo
import com.ailauncher.app.domain.models.AdaptiveDisplaySettings
import com.ailauncher.app.domain.models.AppCategory
import com.ailauncher.app.domain.models.AppProfile
import com.ailauncher.app.domain.models.AppearanceSettings
import com.ailauncher.app.domain.models.BackupDestination
import com.ailauncher.app.domain.models.BackupSettings
import com.ailauncher.app.domain.models.HiddenAppsSettings
import com.ailauncher.app.domain.models.LauncherPage
import com.ailauncher.app.domain.models.NewsSettings
import com.ailauncher.app.domain.models.OnboardingState
import com.ailauncher.app.domain.models.PagesSettings
import com.ailauncher.app.domain.models.ProfileState
import com.ailauncher.app.domain.models.RankedApp
import com.ailauncher.app.domain.models.SecuritySettings
import com.ailauncher.app.domain.models.SmartControlSettings
import com.ailauncher.app.domain.models.SmartFolder
import com.ailauncher.app.domain.models.WidgetSlot
import com.ailauncher.app.domain.models.toggleDockApp
import com.ailauncher.app.domain.search.SearchMatcher
import com.ailauncher.app.domain.usecases.GetRankedAppsUseCase
import com.ailauncher.app.security.AppLockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * v7 FIX #4: Now uses @HiltViewModel + @Inject constructor.
 * No more manual instantiation in Activity.
 */
@HiltViewModel
class LauncherViewModel
    @Inject
    constructor(
        private val getRankedApps: GetRankedAppsUseCase,
        val settingsRepo: SettingsRepository,
        val appLockManager: AppLockManager,
        val backupManager: BackupManager,
        val notificationDao: NotificationDao,
        private val predictionEngine: AppPredictionEngine,
        private val weatherService: WeatherService,
        private val iconCache: IconCache,
        private val installedApps: InstalledAppsRepository,
        private val widgetHost: AppWidgetHost,
        private val feedback: SuggestionFeedbackRepository,
        private val jewishCalendar: JewishCalendarService,
        private val updateChecker: UpdateChecker,
    ) : ViewModel() {
        data class AppListState(
            val isLoading: Boolean = true,
            val rankedApps: List<RankedApp> = emptyList(),
            val smartFolders: List<SmartFolder> = emptyList(),
            val hasUsagePermission: Boolean = false,
            val expandedFolder: AppCategory? = null,
            val searchQuery: String = "",
            val error: String? = null,
            // v10: Private Space apps (only while it's unlocked) and the user's other profiles.
            val privateSpaceApps: List<RankedApp> = emptyList(),
            val profiles: List<ProfileState> = emptyList(),
        )

        enum class ViewMode { GRID, CATEGORIES }

        private val _appState = MutableStateFlow(AppListState())
        val appState: StateFlow<AppListState> = _appState.asStateFlow()

        // v8 perf: search is debounced — typing in the apps screen no longer recomputes
        // the filter on every keystroke. The raw query updates `appState.searchQuery`
        // immediately so the text field stays responsive; the *filtered* list comes from
        // [debouncedSearchQuery].
        @OptIn(kotlinx.coroutines.FlowPreview::class)
        val debouncedSearchQuery: StateFlow<String> =
            appState
                .map { it.searchQuery }
                .debounce(180)
                .distinctUntilChanged()
                .stateIn(viewModelScope, SharingStarted.Eagerly, "")

        private val _viewMode = MutableStateFlow(ViewMode.GRID)
        val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

        val appearance = settingsRepo.appearanceFlow.stateIn(viewModelScope, SharingStarted.Eagerly, AppearanceSettings())
        val pagesSettings = settingsRepo.pagesFlow.stateIn(viewModelScope, SharingStarted.Eagerly, PagesSettings())
        val security = settingsRepo.securityFlow.stateIn(viewModelScope, SharingStarted.Eagerly, SecuritySettings())
        val backupSettings = settingsRepo.backupFlow.stateIn(viewModelScope, SharingStarted.Eagerly, BackupSettings())
        val widgets = settingsRepo.widgetsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        val newsSettings = settingsRepo.newsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, NewsSettings())
        val hiddenApps = settingsRepo.hiddenAppsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, HiddenAppsSettings())
        val smartControl = settingsRepo.smartControlFlow.stateIn(viewModelScope, SharingStarted.Eagerly, SmartControlSettings())
        val adaptiveDisplay = settingsRepo.adaptiveDisplayFlow.stateIn(viewModelScope, SharingStarted.Eagerly, AdaptiveDisplaySettings())
        val onboarding = settingsRepo.onboardingFlow.stateIn(viewModelScope, SharingStarted.Eagerly, OnboardingState())

        /**
         * v10: false until the persisted appearance + onboarding state have loaded.
         * The StateFlows above start from defaults, so without this the first frame
         * of every cold start showed the onboarding screen (completed = false) in the
         * default theme before snapping to the real home screen. LauncherActivity
         * keeps the splash screen up until this turns true.
         */
        val settingsReady: StateFlow<Boolean> =
            combine(settingsRepo.appearanceFlow, settingsRepo.onboardingFlow) { _, _ -> true }
                .stateIn(viewModelScope, SharingStarted.Eagerly, false)

        private val _currentPage = MutableStateFlow(LauncherPage.HOME)
        val currentPage: StateFlow<LauncherPage> = _currentPage.asStateFlow()

        private val _toastMessage = MutableSharedFlow<String>()
        val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

        /** v10: toasts that need a string resource (the ViewModel holds no Context). */
        private val _toastRes = MutableSharedFlow<Int>()
        val toastRes: SharedFlow<Int> = _toastRes.asSharedFlow()

        private var refreshJob: Job? = null
        private var lastRefreshAt = 0L

        // v8: Weather state for ClockHeader. Updates every 30 min, cached in memory.
        private val _weather = MutableStateFlow<WeatherData?>(null)
        val weather: StateFlow<WeatherData?> = _weather.asStateFlow()
        private var weatherLastFetchedAt = 0L

        /**
         * v10: today's Jewish-calendar info, recomputed every minute — but only while
         * something on screen collects it *and* at least one calendar feature is on.
         */
        @OptIn(ExperimentalCoroutinesApi::class)
        val jewishDay: StateFlow<JewishDayInfo?> =
            appearance
                .map { it.jewishCalendar }
                .distinctUntilChanged()
                .flatMapLatest { s ->
                    if (!(s.showHebrewDate || s.showHolidays || s.showShabbatTimes || s.shabbatMode)) {
                        flowOf<JewishDayInfo?>(null)
                    } else {
                        flow<JewishDayInfo?> {
                            while (true) {
                                emit(jewishCalendar.dayInfo(s))
                                delay(60_000)
                            }
                        }
                    }
                }.flowOn(Dispatchers.Default)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        /** v10: Shabbat mode is on and it is currently Shabbat/Yom Tov. */
        val shabbatModeActive: StateFlow<Boolean> =
            combine(jewishDay, appearance) { day, a ->
                a.jewishCalendar.shabbatMode && day?.isRestPeriodNow == true
            }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

        /** v10: a newer release found by the opt-in update check (not dismissed). */
        private val _update = MutableStateFlow<UpdateChecker.UpdateInfo?>(null)
        val update: StateFlow<UpdateChecker.UpdateInfo?> = _update.asStateFlow()

        fun refreshWeather(force: Boolean = false) {
            val now = System.currentTimeMillis()
            if (!force && now - weatherLastFetchedAt < WEATHER_TTL_MS && _weather.value != null) return
            viewModelScope.launch {
                try {
                    _weather.value = weatherService.fetchWeather()
                    weatherLastFetchedAt = System.currentTimeMillis()
                } catch (e: Exception) {
                    Timber.w(e, "Weather fetch failed")
                }
            }
        }

        init {
            refreshInternal()
            // v9: periodic refresh is no longer kicked off here. The launcher Activity
            // owns the lifecycle gate via repeatOnLifecycle(STARTED) — see
            // LauncherActivity.kt. Without that gate, the 15-minute UsageStats + ML
            // wakeups were still firing while the launcher was buried under another
            // foreground app, burning battery for state the user can't see anyway.
            refreshWeather()

            // v10: app installs/removals and profile changes (work apps paused,
            // Private Space unlocked) refresh right away — the user may still be
            // looking at the launcher when they happen.
            viewModelScope.launch { installedApps.changes.collect { refreshInternal() } }
            // Shabbat mode starting/ending changes which badges should show.
            viewModelScope.launch { shabbatModeActive.drop(1).collect { refreshInternal() } }
            // Icon-pack switch → rebuild the list with the new icons. Reads the
            // repository flow, not the `appearance` StateFlow: that one starts from a
            // default value, which would look like a switch on every cold start and
            // wipe the disk icon cache the cold start depends on.
            viewModelScope.launch {
                settingsRepo.appearanceFlow.map { it.iconPackPackage }.distinctUntilChanged().drop(1).collect {
                    iconCache.invalidateAll()
                    refreshInternal()
                }
            }
            viewModelScope.launch {
                smartControl.map { it.checkForUpdates }.distinctUntilChanged().collect { enabled ->
                    if (enabled) checkForUpdates(force = false)
                }
            }
        }

        /** Force a refresh ignoring the throttle. Call sites that explicitly need fresh data. */
        fun forceRefresh() = refreshInternal()

        /**
         * Throttled refresh — used by onResume. Skips work if the previous successful refresh
         * completed within [MIN_REFRESH_INTERVAL_MS]. Reduces UsageStatsManager churn when the
         * user just bounces in/out of an app.
         */
        fun refresh() {
            val now = System.currentTimeMillis()
            if (now - lastRefreshAt < MIN_REFRESH_INTERVAL_MS) return
            refreshInternal()
        }

        private fun refreshInternal() {
            refreshJob?.cancel()
            refreshJob =
                viewModelScope.launch {
                    try {
                        _appState.value = _appState.value.copy(isLoading = _appState.value.rankedApps.isEmpty())
                        val result = getRankedApps.execute()
                        val hidden = hiddenApps.value

                        fun RankedApp.visible() =
                            app.packageName !in hidden.hiddenPackages &&
                                app.packageName !in hidden.privateFolderPackages
                        val visible = result.rankedApps.filter { it.visible() }
                        val visibleFolders =
                            result.smartFolders
                                .map { f ->
                                    f.copy(apps = f.apps.filter { it.visible() })
                                }.filter { it.apps.isNotEmpty() }

                        // Warm the in-memory icon cache from disk before the new list
                        // reaches composition, so a cold-started process doesn't re-decode
                        // every icon from PackageManager on the very first render. No-op
                        // for anything already resident (same-session refreshes).
                        iconCache.preloadFromDisk((visible + result.privateSpaceApps).map { it.app.key })

                        _appState.value =
                            _appState.value.copy(
                                isLoading = false,
                                rankedApps = visible,
                                smartFolders = visibleFolders,
                                hasUsagePermission = result.hasUsagePermission,
                                error = null,
                                privateSpaceApps = result.privateSpaceApps,
                                profiles = result.profiles,
                            )
                        lastRefreshAt = System.currentTimeMillis()
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _appState.value = _appState.value.copy(isLoading = false, error = e.message)
                    }
                }
        }

        fun toggleViewMode() {
            _viewMode.value = if (_viewMode.value == ViewMode.GRID) ViewMode.CATEGORIES else ViewMode.GRID
        }

        fun expandFolder(c: AppCategory?) {
            _appState.value = _appState.value.copy(expandedFolder = c)
        }

        fun updateSearch(q: String) {
            _appState.value = _appState.value.copy(searchQuery = q)
        }

        fun filteredApps(): List<RankedApp> {
            // v8: use debounced query for filtering so AppsScreen doesn't churn on every keystroke.
            val q = debouncedSearchQuery.value
            if (q.isBlank()) return _appState.value.rankedApps
            return searchApps(q)
        }

        /**
         * v10: ranked search over every visible app — SearchMatcher handles Hebrew ↔
         * English spellings, typos and initials; ties go to the app you use more.
         * Private Space apps are only in the pool while the space is unlocked.
         */
        fun searchApps(
            query: String,
            limit: Int = Int.MAX_VALUE,
        ): List<RankedApp> {
            if (query.isBlank()) return emptyList()
            val s = _appState.value
            return (s.rankedApps + s.privateSpaceApps)
                .map { it to SearchMatcher.score(query, it.app.label, it.app.packageName) }
                .filter { it.second > 0 }
                .sortedWith(compareByDescending<Pair<RankedApp, Int>> { it.second }.thenByDescending { it.first.weightScore })
                .take(limit)
                .map { it.first }
        }

        /**
         * v10: what "Recommended now" shows. In Shabbat mode, social, entertainment,
         * game and shopping apps step aside (nothing is blocked — they're still in
         * the Apps page).
         */
        fun homeSuggestions(
            apps: List<RankedApp>,
            shabbatActive: Boolean,
            count: Int = 8,
        ): List<RankedApp> = (if (shabbatActive) apps.filter { it.app.category !in SHABBAT_QUIET_CATEGORIES } else apps).take(count)

        fun navigateToPage(p: LauncherPage) {
            _currentPage.value = p
        }

        fun updateAppearance(u: (AppearanceSettings) -> AppearanceSettings) {
            viewModelScope.launch { settingsRepo.saveAppearance(u(appearance.value)) }
        }

        fun toggleAppLock(p: String) {
            viewModelScope.launch { appLockManager.toggleAppLock(p) }
        }

        fun setLayoutLocked(l: Boolean) {
            viewModelScope.launch { appLockManager.setLayoutLocked(l) }
        }

        /** v10: pause/resume work apps, lock/unlock Private Space. */
        fun setProfileQuietMode(
            userSerial: Long,
            enabled: Boolean,
        ) {
            if (!installedApps.setQuietMode(userSerial, enabled)) {
                viewModelScope.launch { _toastRes.emit(com.ailauncher.app.R.string.profile_toggle_failed) }
            }
        }

        fun performBackup(d: BackupDestination) {
            viewModelScope.launch {
                when (val r = backupManager.backup(d)) {
                    is BackupManager.BackupResult.Success -> _toastMessage.emit("✓ ${r.path}")
                    is BackupManager.BackupResult.Error -> _toastMessage.emit("✗ ${r.message}")
                }
            }
        }

        fun addWidget(
            wid: Int,
            spanY: Int = 2,
            pid: String = "home",
        ) {
            viewModelScope.launch {
                settingsRepo.saveWidgets(
                    widgets.value +
                        WidgetSlot(widgetId = wid, pageId = pid, positionIndex = widgets.value.size, spanY = spanY.coerceIn(1, 4)),
                )
            }
        }

        /** v10: also releases the widget id with the host — it used to leak on every removal. */
        fun removeWidget(wid: Int) {
            viewModelScope.launch { settingsRepo.saveWidgets(widgets.value.filter { it.widgetId != wid }) }
            try {
                widgetHost.deleteAppWidgetId(wid)
            } catch (e: Exception) {
                Timber.w(e, "deleteAppWidgetId failed")
            }
        }

        fun resizeWidget(
            wid: Int,
            spanX: Int,
            spanY: Int,
        ) {
            viewModelScope.launch {
                settingsRepo.saveWidgets(
                    widgets.value.map {
                        if (it.widgetId == wid) it.copy(spanX = spanX.coerceIn(1, 6), spanY = spanY.coerceIn(1, 4)) else it
                    },
                )
            }
        }

        fun hideApp(p: String) {
            viewModelScope.launch {
                val c = hiddenApps.value
                settingsRepo.saveHiddenApps(c.copy(hiddenPackages = c.hiddenPackages + p))
                forceRefresh()
            }
        }

        fun unhideApp(p: String) {
            viewModelScope.launch {
                val c = hiddenApps.value
                settingsRepo.saveHiddenApps(c.copy(hiddenPackages = c.hiddenPackages - p))
                forceRefresh()
            }
        }

        fun moveToPrivateFolder(p: String) {
            viewModelScope.launch {
                val c = hiddenApps.value
                settingsRepo.saveHiddenApps(c.copy(privateFolderPackages = c.privateFolderPackages + p))
                forceRefresh()
            }
        }

        /** Personal-profile packages the Home dock can show right now (installed, not hidden). */
        fun dockablePackages(): Set<String> =
            appState.value.rankedApps
                .filter { it.app.profile == AppProfile.PERSONAL }
                .mapTo(HashSet()) { it.app.packageName }

        /** Long-press → Pin to dock / Unpin. At most PageLayoutSettings.MAX_DOCK_APPS. */
        fun toggleDockApp(p: String) {
            viewModelScope.launch { settingsRepo.savePages(pagesSettings.value.toggleDockApp(p, dockablePackages())) }
        }

        fun completeOnboarding() {
            // lastSeenVersionCode = current version: someone who just finished onboarding
            // has seen everything there is to see — the "What's New" dialog is for
            // existing users after an update, not a redundant popup right after setup.
            viewModelScope.launch {
                settingsRepo.saveOnboarding(
                    OnboardingState(completed = true, lastSeenVersionCode = com.ailauncher.app.BuildConfig.VERSION_CODE),
                )
            }
        }

        /** Marks the "What's New" dialog as seen for the currently-installed version. */
        fun markWhatsNewSeen(versionCode: Int) {
            viewModelScope.launch { settingsRepo.saveOnboarding(onboarding.value.copy(lastSeenVersionCode = versionCode)) }
        }

        /**
         * v7 FIX #13: Reset notification badge count when user opens an app.
         */
        fun markAppOpened(packageName: String) {
            viewModelScope.launch {
                try {
                    notificationDao.markOpened(packageName, System.currentTimeMillis())
                } catch (
                    e: Exception,
                ) {
                    Timber.w(e, "markOpened failed for %s", packageName)
                }
                refresh()
            }
        }

        // ── v10: learning from "Recommended now" ───────────────────────────

        private var lastImpressionAt = 0L

        /**
         * Called when the home screen comes into view. Counts one impression per
         * suggestion per visit (at most every 5 minutes, so bouncing in and out of
         * an app doesn't count as ignoring everything on screen).
         */
        fun recordHomeImpressions() {
            val now = System.currentTimeMillis()
            if (now - lastImpressionAt < IMPRESSION_INTERVAL_MS) return
            val shown = homeSuggestions(_appState.value.rankedApps, shabbatModeActive.value)
            if (shown.isEmpty()) return
            lastImpressionAt = now
            viewModelScope.launch { feedback.recordImpressions(shown.map { it.app.packageName }, now) }
        }

        fun recordSuggestionTap(packageName: String) {
            viewModelScope.launch { feedback.recordTap(packageName) }
        }

        fun resetLearnedSuggestions() {
            viewModelScope.launch {
                feedback.clear()
                forceRefresh()
            }
        }

        // ── v10: update check ──────────────────────────────────────────────

        fun checkForUpdates(force: Boolean) {
            viewModelScope.launch {
                val found = updateChecker.check(force)
                _update.value = found?.takeIf { !updateChecker.isDismissed(it.version) || force }
            }
        }

        fun dismissUpdate() {
            _update.value?.let { updateChecker.dismiss(it.version) }
            _update.value = null
        }

        /**
         * Trigger fresh ML prediction.
         */
        fun runPrediction() {
            viewModelScope.launch {
                try {
                    predictionEngine.predict()
                } catch (e: Exception) {
                    Timber.w(e, "Prediction failed")
                }
            }
        }

        /**
         * Single periodic-refresh tick. Called by the lifecycle-aware loop in
         * LauncherActivity. We expose it (rather than the loop itself) so the
         * Activity can cancel it cleanly via repeatOnLifecycle.
         */
        suspend fun periodicRefreshTick() {
            delay(15 * 60 * 1000L)
            refresh()
            runPrediction()
        }

        companion object {
            private const val MIN_REFRESH_INTERVAL_MS = 60_000L // 1 min throttle for onResume
            private const val WEATHER_TTL_MS = 30 * 60 * 1000L
            private const val IMPRESSION_INTERVAL_MS = 5 * 60 * 1000L
            val SHABBAT_QUIET_CATEGORIES =
                setOf(
                    AppCategory.SOCIAL,
                    AppCategory.ENTERTAINMENT,
                    AppCategory.GAMES,
                    AppCategory.SHOPPING,
                )
        }
    }
