package com.starmist.walker.platform

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreMotion.CMAuthorizationStatusAuthorized
import platform.CoreMotion.CMPedometer
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import kotlin.coroutines.resume

private fun nowMs(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
private fun dateOf(ms: Long): NSDate = NSDate.dateWithTimeIntervalSince1970(ms / 1000.0)

/**
 * The iPhone has no "steps since boot" counter. CoreMotion answers "how many steps between two times" for the last seven
 * days, so this makes a counter that only goes up: the value at the last reading plus the steps since then. The ledger above
 * treats it like the Android counter. (If the app is not opened for more than a week, the older steps are not recoverable.)
 */
@OptIn(ExperimentalForeignApi::class)
class IosStepSensor(private val store: DataStore<Preferences>) : StepSensor {
    private val pedometer = CMPedometer()
    private val lock = Mutex()
    private val lastAtKey = longPreferencesKey("lastAtMs")
    private val lastCounterKey = longPreferencesKey("lastCounter")

    override val hasHardwareCounter: Boolean get() = CMPedometer.isStepCountingAvailable()

    private suspend fun stepsBetween(fromMs: Long, toMs: Long): Long? = suspendCancellableCoroutine { cont ->
        pedometer.queryPedometerDataFromDate(dateOf(fromMs), toDate = dateOf(toMs)) { data, _ ->
            if (cont.isActive) cont.resume(data?.numberOfSteps?.longLongValue)
        }
    }

    private suspend fun counterNow(): Long? = lock.withLock {
        val prefs = store.data.first()
        val now = nowMs()
        val lastAt = prefs[lastAtKey]
        val lastCounter = prefs[lastCounterKey] ?: 0L
        // first use: start counting from now; later: from the last reading (never further back than the seven days CoreMotion keeps)
        val from = if (lastAt == null) now else maxOf(lastAt, now - SIX_AND_A_HALF_DAYS_MS)
        val steps = if (from >= now) 0L else stepsBetween(from, now) ?: return@withLock null
        val counter = lastCounter + steps
        store.edit { it[lastAtKey] = now; it[lastCounterKey] = counter }
        counter
    }

    override suspend fun read(timeoutMs: Long): CounterReading? =
        withTimeoutOrNull(timeoutMs) { counterNow() }?.let { CounterReading(it, events = 1, flushCompleted = true, atMillis = nowMs()) }

    override fun liveCounter(maxReportLatencyUs: Int): Flow<Long> = callbackFlow {
        val base = counterNow() ?: 0L
        val start = nowMs()
        pedometer.startPedometerUpdatesFromDate(dateOf(start)) { data, _ ->
            data?.numberOfSteps?.longLongValue?.let { trySend(base + it) }
        }
        awaitClose { pedometer.stopPedometerUpdates() }
    }

    override fun stepTicks(): Flow<Unit> = liveCounter().map { }

    private companion object {
        const val SIX_AND_A_HALF_DAYS_MS = 6L * 24 * 3600 * 1000 + 12L * 3600 * 1000
    }
}

@OptIn(ExperimentalForeignApi::class)
class IosPlatformInfo : PlatformInfo {
    override fun hasStepPermission(): Boolean = CMPedometer.authorizationStatus() == CMAuthorizationStatusAuthorized

    /** iOS decides about background time itself; there is nothing for the user to switch off. */
    override fun batteryRestricted(): Boolean = false

    override fun nowMillis(): Long = nowMs()

    /** The counter above never resets, so the "boot time" it implies (now minus this) must stay constant: that is zero. */
    override fun elapsedSinceBootMillis(): Long = nowMs()

    override val stepServiceRunning: Boolean get() = false
}
