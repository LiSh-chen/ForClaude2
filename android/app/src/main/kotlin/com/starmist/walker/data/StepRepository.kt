package com.starmist.walker.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import com.starmist.core.diagnostics.HealthInput
import com.starmist.core.diagnostics.Issue
import com.starmist.core.diagnostics.SnapshotHealth
import com.starmist.core.steps.CounterSnapshot
import com.starmist.core.steps.LedgerState
import com.starmist.core.steps.StepLedger
import com.starmist.walker.sensor.CounterReading
import com.starmist.walker.sensor.StepCounterReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.ZoneId

sealed interface SnapshotResult {
    data class Success(val addedSteps: Long, val rebooted: Boolean, val baseline: Boolean) : SnapshotResult
    data object NoSensor : SnapshotResult
    data object NoPermission : SnapshotResult
    data object NoReading : SnapshotResult
}

data class DiagnosticsInfo(
    val issues: List<Issue>,
    val hasHardwareCounter: Boolean,
    val permissionGranted: Boolean,
    val lastSnapshotAtMillis: Long?,
    val recentReboots: List<RebootLogEntity>,
)

class StepRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val settings: SettingsStore,
    val reader: StepCounterReader,
) {
    private val dao = database.stepDao()

    /** The worker and the UI can both ask for a reading; only one may update the ledger at a time. */
    private val snapshotLock = Mutex()

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    private val _lastRead = MutableStateFlow<CounterReading?>(null)

    /** The most recent hardware read, for the diagnostics screen. */
    val lastRead: StateFlow<CounterReading?> = _lastRead

    /** Reads the counter and books the steps since the previous reading onto the right days. */
    suspend fun takeSnapshot(): SnapshotResult {
        if (!reader.hasHardwareCounter) return SnapshotResult.NoSensor
        if (!hasPermission()) return SnapshotResult.NoPermission
        val reading = reader.read() ?: return SnapshotResult.NoReading
        _lastRead.value = reading
        return ingest(reading.value)
    }

    /** Books a counter value that arrived from the live feed while the app is on screen. */
    suspend fun ingestLive(counter: Long): SnapshotResult {
        if (!hasPermission()) return SnapshotResult.NoPermission
        return ingest(counter)
    }

    private suspend fun ingest(counter: Long): SnapshotResult = snapshotLock.withLock {
        val now = System.currentTimeMillis()
        val snapshot = CounterSnapshot(
            counter = counter,
            takenAtMillis = now,
            bootTimeMillis = now - SystemClock.elapsedRealtime(),
        )
        val multiplier = settings.current().multiplier
        val ledger = StepLedger(ZoneId.systemDefault())

        database.withTransaction {
            val stored = dao.getState()
            val state = LedgerState(stored?.toSnapshot(), stored?.carry ?: 0.0)
            val update = ledger.process(state, snapshot, multiplier)
            if (update.isStale) {
                return@withTransaction SnapshotResult.Success(addedSteps = 0, rebooted = false, baseline = false)
            }

            for ((day, raw) in update.rawByDay) {
                val key = day.toString()
                dao.insertIfAbsent(DailyStepsEntity(key))
                dao.addSteps(key, raw, update.scaledByDay[day] ?: 0L)
            }
            dao.putState(
                SnapshotStateEntity(
                    counter = snapshot.counter,
                    takenAtMillis = snapshot.takenAtMillis,
                    bootTimeMillis = snapshot.bootTimeMillis,
                    carry = update.state.carry,
                ),
            )
            if (update.rebooted) dao.insertReboot(RebootLogEntity(atMillis = now, counterAfter = counter))

            SnapshotResult.Success(
                addedSteps = update.scaledByDay.values.sum(),
                rebooted = update.rebooted,
                baseline = update.isBaseline,
            )
        }
    }

    /** Total per day (corrected + manual adjustment) for every day in the range that has data. */
    fun observeDailyTotals(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Long>> =
        dao.observeRange(from.toString(), to.toString()).map { rows ->
            rows.associate { LocalDate.parse(it.date) to it.total }
        }

    fun observeDay(date: LocalDate): Flow<DailyStepsEntity?> =
        dao.observeRange(date.toString(), date.toString()).map { it.firstOrNull() }

    fun observeLastSnapshotAt(): Flow<Long?> = dao.observeState().map { it?.takenAtMillis }

    suspend fun allTimeTotal(): Long = dao.allTimeTotal()

    suspend fun getDay(date: LocalDate): DailyStepsEntity? = dao.getDay(date.toString())

    /** Makes the day's total equal [desiredTotal] by storing the difference as a manual adjustment. */
    suspend fun setDayTotal(date: LocalDate, desiredTotal: Long) {
        val key = date.toString()
        database.withTransaction {
            dao.insertIfAbsent(DailyStepsEntity(key))
            val row = dao.getDay(key) ?: return@withTransaction
            dao.setManualAdjust(key, desiredTotal.coerceAtLeast(0) - row.scaledSteps)
        }
    }

    /** Drops the manual adjustment so the day shows what the sensor counted. */
    suspend fun clearAdjustment(date: LocalDate) {
        val key = date.toString()
        if (dao.getDay(key) != null) dao.setManualAdjust(key, 0)
    }

    suspend fun diagnostics(serviceExpected: Boolean, serviceRunning: Boolean): DiagnosticsInfo {
        val state = dao.getState()
        val permission = hasPermission()
        val hasCounter = reader.hasHardwareCounter
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val restricted = !power.isIgnoringBatteryOptimizations(context.packageName)
        val issues = SnapshotHealth.evaluate(
            HealthInput(
                permissionGranted = permission,
                hasHardwareCounter = hasCounter,
                lastSnapshotAtMillis = state?.takenAtMillis,
                nowMillis = System.currentTimeMillis(),
                batteryRestricted = restricted,
                serviceExpected = serviceExpected,
                serviceRunning = serviceRunning,
            ),
        )
        return DiagnosticsInfo(
            issues = issues,
            hasHardwareCounter = hasCounter,
            permissionGranted = permission,
            lastSnapshotAtMillis = state?.takenAtMillis,
            recentReboots = dao.observeRecentReboots(5).first(),
        )
    }
}
