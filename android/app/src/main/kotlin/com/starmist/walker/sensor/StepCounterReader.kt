package com.starmist.walker.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/** What one read of the hardware counter produced; kept for the diagnostics screen. */
data class CounterReading(
    /** Cumulative steps since boot (the highest value seen during the read). */
    val value: Long,
    /** How many sensor events arrived while listening. */
    val events: Int,
    /** True when the sensor confirmed it had pushed out everything it was holding. */
    val flushCompleted: Boolean,
    val atMillis: Long,
)

/**
 * Reads the hardware step counter.
 *
 * The counter is an "on-change" sensor. The system replays the last value it saw the moment a
 * listener registers, and that value can be stale: if nobody was listening, it is whatever the
 * sensor reported the last time anyone was. Trusting the first event therefore reads old numbers
 * and misses the steps taken since. So a read asks the sensor hub to flush what it has buffered
 * (which delivers the current count), keeps listening briefly, and uses the highest value seen -
 * the counter only ever goes up between reboots, so the highest value is the freshest.
 */
class StepCounterReader(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    val hasHardwareCounter: Boolean get() = sensor != null

    /** Cumulative steps since boot, or null if unavailable (no sensor, no permission, no events). */
    suspend fun readOnce(timeoutMs: Long = DEFAULT_TIMEOUT_MS): Long? = read(timeoutMs)?.value

    suspend fun read(timeoutMs: Long = DEFAULT_TIMEOUT_MS): CounterReading? {
        val sensor = sensor ?: return null
        val flushDone = CompletableDeferred<Unit>()
        val best = AtomicLong(-1)
        val events = AtomicInteger(0)

        val listener = object : SensorEventListener2 {
            override fun onSensorChanged(event: SensorEvent) {
                events.incrementAndGet()
                val value = event.values[0].toLong()
                best.updateAndGet { maxOf(it, value) }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

            override fun onFlushCompleted(sensor: Sensor?) {
                flushDone.complete(Unit)
            }
        }

        val registered = try {
            // A non-zero report latency puts the sensor in batching mode, which is what makes flush() meaningful.
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL, BATCH_LATENCY_US)
        } catch (e: SecurityException) {
            false // permission missing
        }
        if (!registered) return null

        var flushCompleted = false
        try {
            val flushing = sensorManager.flush(listener)
            if (flushing) {
                flushCompleted = withTimeoutOrNull(timeoutMs) { flushDone.await() } != null
            }
            // Give a fresh value a moment to follow the replayed one, even if flush is unsupported.
            delay(if (flushing && flushCompleted) GRACE_MS else SETTLE_MS)
        } finally {
            sensorManager.unregisterListener(listener)
        }

        val value = best.get()
        return if (value < 0) null else CounterReading(value, events.get(), flushCompleted, System.currentTimeMillis())
    }

    /**
     * Live counter values while collected. Only collect while the app is on screen; stopping the
     * collection unregisters the listener, so nothing keeps running in the background.
     *
     * @param maxReportLatencyUs how long the sensor hub may hold events before waking the CPU to
     *   deliver them; a long value keeps the CPU asleep while walking. 0 delivers immediately.
     */
    fun liveCounter(maxReportLatencyUs: Int = 0): Flow<Long> = callbackFlow {
        val sensor = sensor
        if (sensor == null) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(event.values[0].toLong())
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        val registered = try {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI, maxReportLatencyUs)
        } catch (e: SecurityException) {
            false
        }
        if (!registered) close()
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 6_000L
        const val BATCH_LATENCY_US = 5_000_000
        const val GRACE_MS = 500L
        const val SETTLE_MS = 3_000L
    }
}
