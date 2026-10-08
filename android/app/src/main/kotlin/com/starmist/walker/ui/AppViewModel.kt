package com.starmist.walker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.starmist.core.stats.Aggregator
import com.starmist.core.stats.Period
import com.starmist.core.stats.PeriodSummary
import com.starmist.core.steps.StepScaler
import com.starmist.walker.container
import com.starmist.walker.data.DailyStepsEntity
import com.starmist.walker.data.DiagnosticsInfo
import com.starmist.walker.data.SnapshotResult
import com.starmist.walker.data.UserSettings
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

    /** While the app is on screen, follow the counter so new steps show up right away. */
    fun startLive() {
        if (liveJob?.isActive == true) return
        liveJob = viewModelScope.launch {
            repository.reader.liveCounter().conflate().collect { counter ->
                repository.ingestLive(counter)
                delay(LIVE_MIN_INTERVAL_MS) // conflate keeps only the newest value meanwhile
            }
        }
    }

    fun stopLive() {
        liveJob?.cancel()
        liveJob = null
    }

    val lastRead = repository.lastRead

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

    suspend fun loadDiagnostics(): DiagnosticsInfo = repository.diagnostics()

    private companion object {
        const val LIVE_MIN_INTERVAL_MS = 2_000L
    }
}
