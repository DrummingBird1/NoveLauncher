package com.ailauncher.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ailauncher.app.data.SettingsRepository
import com.ailauncher.app.data.api.WeatherData
import com.ailauncher.app.data.api.WeatherService
import com.ailauncher.app.data.backup.BackupManager
import com.ailauncher.app.data.db.NotificationDao
import com.ailauncher.app.data.ml.AppPredictionEngine
import com.ailauncher.app.domain.models.*
import com.ailauncher.app.domain.usecases.GetRankedAppsUseCase
import com.ailauncher.app.security.AppLockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * v7 FIX #4: Now uses @HiltViewModel + @Inject constructor.
 * No more manual instantiation in Activity.
 */
@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val getRankedApps: GetRankedAppsUseCase,
    val settingsRepo: SettingsRepository,
    val appLockManager: AppLockManager,
    val backupManager: BackupManager,
    val notificationDao: NotificationDao,
    private val predictionEngine: AppPredictionEngine,
    private val weatherService: WeatherService
) : ViewModel() {

    data class AppListState(
        val isLoading: Boolean = true,
        val rankedApps: List<RankedApp> = emptyList(),
        val smartFolders: List<SmartFolder> = emptyList(),
        val hasUsagePermission: Boolean = false,
        val expandedFolder: AppCategory? = null,
        val searchQuery: String = "",
        val error: String? = null
    )

    enum class ViewMode { GRID, CATEGORIES }

    private val _appState = MutableStateFlow(AppListState())
    val appState: StateFlow<AppListState> = _appState.asStateFlow()

    // v8 perf: search is debounced — typing in the apps screen no longer recomputes
    // the filter on every keystroke. The raw query updates `appState.searchQuery`
    // immediately so the text field stays responsive; the *filtered* list comes from
    // [debouncedSearchQuery].
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val debouncedSearchQuery: StateFlow<String> = appState
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

    private val _currentPage = MutableStateFlow(LauncherPage.HOME)
    val currentPage: StateFlow<LauncherPage> = _currentPage.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private var refreshJob: Job? = null
    private var lastRefreshAt = 0L
    private val MIN_REFRESH_INTERVAL_MS = 60_000L  // 1 min throttle for onResume

    // v8: Weather state for ClockHeader. Updates every 30 min, cached in memory.
    private val _weather = MutableStateFlow<WeatherData?>(null)
    val weather: StateFlow<WeatherData?> = _weather.asStateFlow()
    private var weatherLastFetchedAt = 0L
    private val WEATHER_TTL_MS = 30 * 60 * 1000L

    fun refreshWeather(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - weatherLastFetchedAt < WEATHER_TTL_MS && _weather.value != null) return
        viewModelScope.launch {
            try {
                _weather.value = weatherService.fetchWeather()
                weatherLastFetchedAt = System.currentTimeMillis()
            } catch (e: Exception) { Timber.w(e, "Weather fetch failed") }
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
        refreshJob = viewModelScope.launch {
            try {
                _appState.value = _appState.value.copy(isLoading = true)
                val result = getRankedApps.execute()
                val hidden = hiddenApps.value
                val visible = result.rankedApps.filter {
                    it.app.packageName !in hidden.hiddenPackages &&
                    it.app.packageName !in hidden.privateFolderPackages
                }
                val visibleFolders = result.smartFolders.map { f ->
                    f.copy(apps = f.apps.filter {
                        it.app.packageName !in hidden.hiddenPackages &&
                        it.app.packageName !in hidden.privateFolderPackages
                    })
                }.filter { it.apps.isNotEmpty() }

                _appState.value = _appState.value.copy(
                    isLoading = false, rankedApps = visible,
                    smartFolders = visibleFolders, hasUsagePermission = result.hasUsagePermission, error = null
                )
                lastRefreshAt = System.currentTimeMillis()
            } catch (e: Exception) {
                _appState.value = _appState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun toggleViewMode() { _viewMode.value = if (_viewMode.value == ViewMode.GRID) ViewMode.CATEGORIES else ViewMode.GRID }
    fun expandFolder(c: AppCategory?) { _appState.value = _appState.value.copy(expandedFolder = c) }
    fun updateSearch(q: String) { _appState.value = _appState.value.copy(searchQuery = q) }

    fun filteredApps(): List<RankedApp> {
        val s = _appState.value
        // v8: use debounced query for filtering so AppsScreen doesn't churn on every keystroke.
        val q = debouncedSearchQuery.value
        if (q.isBlank()) return s.rankedApps
        val lower = q.lowercase()
        return s.rankedApps.filter { it.app.label.lowercase().contains(lower) }
    }

    fun navigateToPage(p: LauncherPage) { _currentPage.value = p }

    fun updateAppearance(u: (AppearanceSettings) -> AppearanceSettings) {
        viewModelScope.launch { settingsRepo.saveAppearance(u(appearance.value)) }
    }

    fun toggleAppLock(p: String) { viewModelScope.launch { appLockManager.toggleAppLock(p) } }
    fun setLayoutLocked(l: Boolean) { viewModelScope.launch { appLockManager.setLayoutLocked(l) } }

    fun performBackup(d: BackupDestination) {
        viewModelScope.launch {
            when (val r = backupManager.backup(d)) {
                is BackupManager.BackupResult.Success -> _toastMessage.emit("✓ ${r.path}")
                is BackupManager.BackupResult.Error -> _toastMessage.emit("✗ ${r.message}")
            }
        }
    }

    fun addWidget(wid: Int, pid: String = "home") {
        viewModelScope.launch {
            settingsRepo.saveWidgets(widgets.value + WidgetSlot(widgetId = wid, pageId = pid, positionIndex = widgets.value.size))
        }
    }

    fun removeWidget(wid: Int) {
        viewModelScope.launch { settingsRepo.saveWidgets(widgets.value.filter { it.widgetId != wid }) }
    }

    fun resizeWidget(wid: Int, spanX: Int, spanY: Int) {
        viewModelScope.launch {
            settingsRepo.saveWidgets(widgets.value.map {
                if (it.widgetId == wid) it.copy(spanX = spanX.coerceIn(1, 6), spanY = spanY.coerceIn(1, 4)) else it
            })
        }
    }

    fun hideApp(p: String) {
        viewModelScope.launch {
            val c = hiddenApps.value
            settingsRepo.saveHiddenApps(c.copy(hiddenPackages = c.hiddenPackages + p))
            refresh()
        }
    }

    fun unhideApp(p: String) {
        viewModelScope.launch {
            val c = hiddenApps.value
            settingsRepo.saveHiddenApps(c.copy(hiddenPackages = c.hiddenPackages - p))
            refresh()
        }
    }

    fun moveToPrivateFolder(p: String) {
        viewModelScope.launch {
            val c = hiddenApps.value
            settingsRepo.saveHiddenApps(c.copy(privateFolderPackages = c.privateFolderPackages + p))
            refresh()
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch { settingsRepo.saveOnboarding(OnboardingState(completed = true)) }
    }

    /**
     * v7 FIX #13: Reset notification badge count when user opens an app.
     */
    fun markAppOpened(packageName: String) {
        viewModelScope.launch {
            try { notificationDao.markOpened(packageName, System.currentTimeMillis()) }
            catch (e: Exception) { Timber.w(e, "markOpened failed for %s", packageName) }
            refresh()
        }
    }

    /**
     * Trigger fresh ML prediction.
     */
    fun runPrediction() {
        viewModelScope.launch {
            try { predictionEngine.predict() }
            catch (e: Exception) { Timber.w(e, "Prediction failed") }
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
}
