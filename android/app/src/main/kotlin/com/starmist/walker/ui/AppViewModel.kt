package com.starmist.walker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.starmist.core.metrics.BodyMetrics
import com.starmist.core.route.RouteEvent
import com.starmist.core.route.RouteState
import com.starmist.core.route.Routes
import com.starmist.core.stats.Aggregator
import com.starmist.core.stats.Period
import com.starmist.core.stats.PeriodSummary
import com.starmist.core.steps.StepScaler
import com.starmist.walker.container
import com.starmist.walker.data.ExploreState
import com.starmist.walker.data.RegionLog
import com.starmist.walker.data.DailyStepsEntity
import com.starmist.walker.data.DiagnosticsInfo
import com.starmist.walker.data.SnapshotResult
import com.starmist.walker.data.UserSettings
import com.starmist.walker.tracking.StepService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application.container
    private val repository = container.repository

    /** Null until DataStore has loaded, so screens never edit placeholder defaults. */
    val settings: StateFlow<UserSettings?> = container.settings.flow
        .map<UserSettings, UserSettings?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today

    private val _permissionGranted = MutableStateFlow(repository.hasPermission())
    val permissionGranted: StateFlow<Boolean> = _permissionGranted

    private val _hasHardwareCounter = MutableStateFlow(repository.reader.hasHardwareCounter)
    val hasHardwareCounter: StateFlow<Boolean> = _hasHardwareCounter

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    fun messageShown() = _message.update { null }

    val todayRow: StateFlow<DailyStepsEntity?> = _today
        .flatMapLatest { repository.observeDay(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val lastSnapshotAt: StateFlow<Long?> = repository.observeLastSnapshotAt()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ---- statistics ------------------------------------------------------------------------

    private val _period = MutableStateFlow(Period.WEEK)
    val period: StateFlow<Period> = _period

    private val _anchor = MutableStateFlow(LocalDate.now())
    val anchor: StateFlow<LocalDate> = _anchor

    val summary: StateFlow<PeriodSummary?> = combine(_period, _anchor, _today, settings) { p, a, t, s ->
        Quad(p, a, t, s?.dailyGoal ?: 0)
    }.flatMapLatest { (p, a, t, goal) ->
        val (start, end) = Aggregator.rangeOf(a, p)
        repository.observeDailyTotals(start, end).map { daily -> Aggregator.summarize(a, p, daily, goal, t) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Every day's steps since the beginning, for the daily statistics draw. */
    val history: StateFlow<Map<LocalDate, Long>> = _today
        .flatMapLatest { t -> repository.observeDailyTotals(LocalDate.of(2000, 1, 1), t) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private data class Quad(val period: Period, val anchor: LocalDate, val today: LocalDate, val goal: Int)

    fun selectPeriod(period: Period) {
        _period.value = period
        _anchor.value = _today.value
    }

    fun shiftPeriod(direction: Int) {
        _anchor.update { Aggregator.shift(it, _period.value, direction) }
    }

    // ---- readings --------------------------------------------------------------------------

    /** Reads the sensor now. Call when the app comes to the foreground or the user asks. */
    fun refresh(announce: Boolean = false): Job =
        viewModelScope.launch {
            _today.value = LocalDate.now()
            _permissionGranted.value = repository.hasPermission()
            val result = repository.takeSnapshot()
            settleRoute()
            if (announce) {
                _message.value = when (result) {
                    is SnapshotResult.Success -> when {
                        result.baseline -> "已建立計步基準，之後的步數會開始累計"
                        result.addedSteps > 0 -> "已同步，新增 ${result.addedSteps} 步"
                        else -> "已同步，沒有新的步數"
                    }
                    SnapshotResult.NoSensor -> "這台裝置沒有硬體計步器"
                    SnapshotResult.NoPermission -> "尚未授予「動作與健身」權限"
                    SnapshotResult.NoReading -> "讀取計步器逾時，請稍後再試"
                }
            }
        }

    // ---- live feed -------------------------------------------------------------------------

    private var liveJob: Job? = null
    private var tickJob: Job? = null

    /**
     * Steps the step detector has seen that the cumulative counter has not reported yet. They make
     * the number move with every step; once the counter catches up they are taken off again, so the
     * shown total never counts a step twice and never goes backwards.
     */
    private val pendingTicks = MutableStateFlow(0L)

    /** Today's steps for display: stored total plus the not-yet-confirmed ticks. */
    val todaySteps: StateFlow<Long> = combine(todayRow, pendingTicks) { row, ticks -> (row?.total ?: 0L) + ticks }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    init {
        viewModelScope.launch { container.settings.migrateDefaults() }
        viewModelScope.launch {
            var previous: Long? = null
            todayRow.collect { row ->
                val total = row?.total ?: 0L
                val before = previous
                if (before != null && total > before) {
                    val confirmed = total - before
                    pendingTicks.update { (it - confirmed).coerceAtLeast(0L) }
                    if (liveJob?.isActive == true) markWalking()
                }
                previous = total
            }
        }
    }

    /** While the app is on screen, follow the counter so new steps show up right away. */
    fun startLive() {
        if (liveJob?.isActive != true) {
            liveJob = viewModelScope.launch {
                repository.reader.liveCounter().conflate().collect { counter ->
                    repository.ingestLive(counter)
                    settleRoute()
                    delay(LIVE_MIN_INTERVAL_MS) // conflate keeps only the newest value meanwhile
                }
            }
        }
        if (tickJob?.isActive != true) {
            tickJob = viewModelScope.launch {
                repository.reader.stepTicks().collect {
                    pendingTicks.update { ticks -> minOf(ticks + 1, MAX_PENDING_TICKS) }
                    markWalking()
                }
            }
        }
    }

    fun stopLive() {
        liveJob?.cancel()
        liveJob = null
        tickJob?.cancel()
        tickJob = null
        pendingTicks.value = 0L
    }

    val lastRead = repository.lastRead

    // ---- walking state ---------------------------------------------------------------------

    private val _walking = MutableStateFlow(false)
    private var walkingTimeout: Job? = null

    /** True while steps are being taken, so the scene keeps scrolling. */
    val walking: StateFlow<Boolean> = _walking

    private fun markWalking() {
        _walking.value = true
        walkingTimeout?.cancel()
        walkingTimeout = viewModelScope.launch {
            delay(WALKING_HOLD_MS)
            _walking.value = false
        }
    }

    // ---- real-world route -----------------------------------------------------------------

    /** Null until a route has been chosen. */
    val routeState: StateFlow<RouteState?> = container.routes.flow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The tags found in each township (key "county|name"). */
    val regionLogs: StateFlow<Map<String, RegionLog>> = container.regions.logs.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val townAreas by lazy { TownAreas.load(getApplication()) }
    private val townTags by lazy { loadTownBlurbs(getApplication()) }
    private var lastRegionCheckKm = -1.0

    /** Kilometres of each route through each township, worked out once per route (it decides which townships count as long). */
    private val townLengthKm = HashMap<String, Map<Int, Double>>()

    /**
     * Gives each township walked through its feature when it is entered and, in a long one, a second feature at about
     * half the way across; says so when one is new.
     */
    private suspend fun collectRegionTags(route: com.starmist.core.route.RouteDef, walkedKm: Double) {
        if (lastRegionCheckKm >= 0 && walkedKm - lastRegionCheckKm < 0.5 && walkedKm >= lastRegionCheckKm) return
        lastRegionCheckKm = walkedKm
        val total = townLengthKm.getOrPut(route.id) { townAreas.visited(route, route.totalKm) }
        for ((id, km) in townAreas.visited(route, walkedKm)) {
            val t = townAreas.towns[id]
            val key = "${t.county}|${t.name}"
            val tags = townTags[key] ?: continue
            var found = container.regions.visit(key, tags.size, stage = 1)
            if (found == null && com.starmist.core.route.TagDiscovery.reachedHalfway(total[id] ?: 0.0, km)) {
                found = container.regions.visit(key, tags.size, stage = 2)
            }
            if (found != null) _message.value = "${t.name}：發現新特色「${tags[found].tag}」"
        }
    }

    private val _pendingRouteEvents = MutableStateFlow<List<RouteEvent>>(emptyList())

    /** Cards, stamps and crossings waiting to be shown, oldest first; the screen shows one at a time. */
    val pendingRouteEvents: StateFlow<List<RouteEvent>> = _pendingRouteEvents

    fun dismissRouteEvent() = _pendingRouteEvents.update { it.drop(1) }

    fun startRoute(routeId: String) {
        val route = Routes.byId(routeId) ?: return
        viewModelScope.launch {
            container.regions.newLap()
            container.routes.start(route, repository.allTimeTotal())
            settleRoute()
        }
    }

    fun restartRoute() {
        viewModelScope.launch {
            container.regions.newLap()
            container.routes.clear()
            _pendingRouteEvents.value = emptyList()
        }
    }

    /** Turns the steps counted since the last time into distance along the route and queues what was passed. */
    fun settleRoute() {
        viewModelScope.launch {
            val state = container.routes.flow.first() ?: return@launch
            val route = Routes.byId(state.routeId) ?: return@launch
            val stride = BodyMetrics.strideMeters(container.settings.current().profile)
            if (container.explore.read()?.active == true) {
                container.routes.skipTo(repository.allTimeTotal())
                settleExplore(stride)
                return@launch
            }
            val advance = container.routes.settle(route, repository.allTimeTotal(), stride) ?: return@launch
            collectRegionTags(route, advance.state.walkedKm)
            // small sights earn their stamps here: the township and the nearest point both have to be reached
            val before = advance.state.walkedKm - advance.addedKm
            val sights = PoiStamps.of(route, townAreas).mapNotNull { (poi, km) ->
                if (km != null && km > before + 1e-9 && km <= advance.state.walkedKm + 1e-9) RouteEvent.PoiCollected(poi, km) else null
            }
            val shown = (advance.events + sights).filter {
                it is RouteEvent.CardCollected || it is RouteEvent.StampCollected || it is RouteEvent.PoiCollected || it is RouteEvent.Transit || it is RouteEvent.Finished
            }
            if (shown.isNotEmpty()) _pendingRouteEvents.update { it + shown }
        }
    }

    // ---- free exploration ------------------------------------------------------------------

    val exploreState: StateFlow<ExploreState?> = container.explore.flow.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val neighborLists by lazy { townAreas.neighborLists() }

    /** Which townships are next to which, for the map to offer only those next to the walker. */
    fun neighborsOf(id: Int): Set<Int> = neighborLists.getOrNull(id).orEmpty()

    /** Works as the switch between the loop and free exploration (for development; later it opens after the loop is finished). */
    fun toggleExplore() {
        viewModelScope.launch {
            val steps = repository.allTimeTotal()
            val now = container.explore.read()
            if (now?.active == true) {
                settleExplore(BodyMetrics.strideMeters(container.settings.current().profile))
                container.explore.update { it?.copy(active = false) }
                container.routes.skipTo(steps)
                return@launch
            }
            val route = container.routes.flow.first()?.let { Routes.byId(it.routeId) }
            if (now != null) {
                container.explore.update { it?.copy(active = true, consumedSteps = steps) }
            } else if (route != null) {
                val walked = container.routes.flow.first()?.walkedKm ?: 0.0
                val begun = ExploreEngine.start(townAreas, route.positionAt(walked), steps, 0f)
                if (begun != null) container.explore.update { begun } else _message.value = "找不到目前所在的鄉鎮"
            }
            container.routes.skipTo(steps)
        }
    }

    /** Picks the township to walk to; only one next to the township the walker is in. */
    fun exploreGo(id: Int) {
        viewModelScope.launch {
            val s = container.explore.read() ?: return@launch
            if (!s.active || id !in neighborsOf(s.current)) return@launch
            container.explore.update { it?.let { st -> ExploreEngine.go(st, townAreas, id) } }
        }
    }

    private suspend fun exploreExplored(s: ExploreState): Set<Int> {
        val state = container.routes.flow.first()
        val loop = state?.let { Routes.byId(it.routeId)?.let { r -> townAreas.visited(r, state.walkedKm).keys } }.orEmpty()
        return loop + s.visited
    }

    /** Turns new steps into distance along the way to the picked township, collecting what each township reached holds. */
    private suspend fun settleExplore(stride: Double) {
        val s = container.explore.read()?.takeIf { it.active } ?: return
        val advance = ExploreEngine.advance(s, townAreas, neighborLists, exploreExplored(s), repository.allTimeTotal(), stride)
        container.explore.update { advance.state.copy(active = it?.active ?: false) }
        for (id in advance.reached) {
            val t = townAreas.towns[id]
            val key = "${t.county}|${t.name}"
            val tags = townTags[key] ?: continue
            val found = container.regions.visit(key, tags.size, stage = 1)
            if (found != null) _message.value = "${t.name}：發現新特色「${tags[found].tag}」"
        }
        val route = container.routes.flow.first()?.let { Routes.byId(it.routeId) } ?: return
        val reachedSet = advance.reached.toSet()
        val sights = route.pois.filter { poi -> townAreas.find(poi.at.lat, poi.at.lng) in reachedSet }
            .map { RouteEvent.PoiCollected(it, 0.0) }
        if (sights.isNotEmpty()) _pendingRouteEvents.update { it + sights }
    }

    // ---- step-detector batching trial -----------------------------------------------------

    sealed interface BatchTestState {
        data object Idle : BatchTestState
        data object Running : BatchTestState
        data class Done(val result: com.starmist.walker.sensor.BatchTestResult?) : BatchTestState
    }

    private val _batchTest = MutableStateFlow<BatchTestState>(BatchTestState.Idle)
    val batchTest: StateFlow<BatchTestState> = _batchTest

    val detectorInfo get() = repository.reader.detectorInfo

    /** Two and a half minutes with events held back for up to a minute; keeps running if the screen is turned off. */
    fun startBatchTest() {
        if (_batchTest.value is BatchTestState.Running) return
        _batchTest.value = BatchTestState.Running
        viewModelScope.launch {
            _batchTest.value = BatchTestState.Done(repository.reader.batchTest(durationMs = 150_000, latencyMs = 60_000))
        }
    }

    // ---- tuning ----------------------------------------------------------------------------

    fun updateSettings(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { container.settings.update(transform) }
    }

    fun setMultiplier(value: Double) = updateSettings { it.copy(multiplier = StepScaler.clampMultiplier(value)) }

    /** Raw hardware counter, for calibration walks. Not affected by the correction factor. */
    suspend fun readRawCounter(): Long? = repository.reader.readOnce()

    suspend fun dayTotal(date: LocalDate): Long = repository.getDay(date)?.total ?: 0L

    suspend fun dayRow(date: LocalDate): DailyStepsEntity? = repository.getDay(date)

    suspend fun setDayTotal(date: LocalDate, total: Long) {
        repository.setDayTotal(date, total)
        _message.value = "已修正 $date 為 $total 步"
    }

    suspend fun clearAdjustment(date: LocalDate) {
        repository.clearAdjustment(date)
        _message.value = "已還原 $date 的感測器數值"
    }

    suspend fun loadDiagnostics(): DiagnosticsInfo =
        repository.diagnostics(
            serviceExpected = settings.value?.stableMode == true,
            serviceRunning = StepService.running.value,
        )

    fun setStableMode(on: Boolean) = updateSettings { it.copy(stableMode = on) }

    private companion object {

        const val LIVE_MIN_INTERVAL_MS = 300L

        /** Caps how far the detector may run ahead of the counter, so a false positive cannot pile up. */
        const val MAX_PENDING_TICKS = 15L

        const val WALKING_HOLD_MS = 2_500L
    }
}
