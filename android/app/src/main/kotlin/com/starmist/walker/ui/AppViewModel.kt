package com.starmist.walker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.starmist.core.stats.Aggregator
import com.starmist.core.world.ItemType
import com.starmist.core.world.JourneyEngine
import com.starmist.core.world.JourneyEvent
import com.starmist.core.world.JourneyState
import com.starmist.core.stats.Period
import com.starmist.core.stats.PeriodSummary
import com.starmist.core.steps.StepScaler
import com.starmist.walker.container
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
            settleJourney()
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
                    settleJourney(live = true)
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

    // ---- world -----------------------------------------------------------------------------

    val engine = JourneyEngine()

    /** Null until the first journey has been started. */
    val journey: StateFlow<JourneyState?> = container.journey.flow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _replay = MutableStateFlow<ReplaySummary?>(null)

    /** What happened on the road since the app was last opened; shown once, then dismissed. */
    val replay: StateFlow<ReplaySummary?> = _replay
    fun dismissReplay() { _replay.value = null }

    private val _pickups = kotlinx.coroutines.flow.MutableSharedFlow<Pickup>(extraBufferCapacity = 16)

    /** Items found while the app is open; the scene plays a small animation for each. */
    val pickups: kotlinx.coroutines.flow.SharedFlow<Pickup> = _pickups

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

    /** Converts steps counted since the last time into travel along the route. */
    fun settleJourney(live: Boolean = false) {
        viewModelScope.launch {
            val result = container.journey.settle(engine, repository.allTimeTotal())
            if (live) {
                // Steps taken with the app open: show each find as it happens instead of a summary.
                result.events.forEach {
                    when (it) {
                        is JourneyEvent.Found -> _pickups.tryEmit(Pickup(it.item, it.line))
                        is JourneyEvent.ChoiceAppeared -> _pickups.tryEmit(Pickup(it.item, null))
                        else -> Unit
                    }
                }
                return@launch
            }
            if (result.events.isNotEmpty()) {
                _replay.value = ReplaySummary(
                    stepsWalked = result.stepsWalked,
                    events = result.events,
                    itemsFound = result.events.mapNotNull {
                        when (it) {
                            is JourneyEvent.Found -> it.item
                            is JourneyEvent.ChoiceAppeared -> it.item
                            else -> null
                        }
                    }.groupingBy { it }.eachCount(),
                )
            }
        }
    }

    fun resolveCard(encounterIndex: Int, chooseA: Boolean) {
        viewModelScope.launch {
            val resolution = container.journey.resolve(engine, encounterIndex, chooseA)
            if (resolution != null) {
                _message.value = "${resolution.option.result}（獲得 ${resolution.option.bonus.item.displayName}）"
            }
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

/** The road since the last time the app was opened. */
data class ReplaySummary(
    val stepsWalked: Long,
    val events: List<JourneyEvent>,
    val itemsFound: Map<ItemType, Int>,
)

/** Something just picked up on the road. [line] is the story line, if there is one. */
data class Pickup(val item: ItemType, val line: String?)
