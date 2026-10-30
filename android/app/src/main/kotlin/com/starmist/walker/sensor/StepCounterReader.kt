package com.starmist.walker.sensor

import android.content.Context
import com.starmist.walker.platform.BatchTestResult
import com.starmist.walker.platform.CounterReading
import com.starmist.walker.platform.DetectorInfo
import com.starmist.walker.platform.StepSensor
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
class StepCounterReader(context: Context) : StepSensor {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    override val hasHardwareCounter: Boolean get() = sensor != null

    override val detectorInfo: DetectorInfo
        get() {
            val detector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) ?: return DetectorInfo(false, 0, 0, false)
            return DetectorInfo(true, detector.fifoMaxEventCount, detector.fifoReservedEventCount, detector.isWakeUpSensor)
        }

    /**
     * Listens to the step detector for [durationMs] with its events batched in the sensor hub for up to
     * [latencyMs]. If events arrive in bursts, long after the steps happened, per-step timing can be
     * recovered in the background. Compares the event count with the counter's gain to spot dropped events.
     */
    override suspend fun batchTest(durationMs: Long, latencyMs: Int): BatchTestResult? {
        val detector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) ?: return null
        val events = AtomicInteger(0)
        val bursts = AtomicInteger(0)
        val largest = AtomicInteger(0)
        val current = AtomicInteger(0)
        val lastArrivalMs = AtomicLong(0)
        val maxDelayMs = AtomicLong(0)
        val firstCounter = AtomicLong(-1)
        val lastCounter = AtomicLong(-1)

        val detectorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val nowNs = android.os.SystemClock.elapsedRealtimeNanos()
                maxDelayMs.updateAndGet { maxOf(it, (nowNs - event.timestamp) / 1_000_000) }
                val nowMs = nowNs / 1_000_000
                if (nowMs - lastArrivalMs.get() > 400) {
                    bursts.incrementAndGet()
                    current.set(0)
                }
                lastArrivalMs.set(nowMs)
                largest.updateAndGet { maxOf(it, current.incrementAndGet()) }
                events.incrementAndGet()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        val counterListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val value = event.values[0].toLong()
                firstCounter.compareAndSet(-1, value)
                lastCounter.updateAndGet { maxOf(it, value) }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        val accepted = try {
            val ok = sensorManager.registerListener(detectorListener, detector, SensorManager.SENSOR_DELAY_NORMAL, latencyMs * 1000)
            sensor?.let { sensorManager.registerListener(counterListener, it, SensorManager.SENSOR_DELAY_NORMAL) }
            ok
        } catch (e: SecurityException) {
            false
        }
        try {
            delay(durationMs)
            sensorManager.flush(detectorListener)
            sensorManager.flush(counterListener)
            delay(1_500)
        } finally {
            sensorManager.unregisterListener(detectorListener)
            sensorManager.unregisterListener(counterListener)
        }
        val gain = if (firstCounter.get() < 0) 0L else lastCounter.get() - firstCounter.get()
        return BatchTestResult(events.get(), gain, bursts.get(), largest.get(), maxDelayMs.get(), accepted)
    }

    /** Cumulative steps since boot, or null if unavailable (no sensor, no permission, no events). */
    override suspend fun readOnce(timeoutMs: Long): Long? = read(timeoutMs)?.value

    override suspend fun read(timeoutMs: Long): CounterReading? {
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
    override fun liveCounter(maxReportLatencyUs: Int): Flow<Long> = callbackFlow {
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

    /**
     * One "tick" per step, as soon as the step detector reports it. Used only to make the number on
     * screen move with every step; the cumulative counter stays the source of truth.
     */
    override fun stepTicks(): Flow<Unit> = callbackFlow {
        val detector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        if (detector == null) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(Unit)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        val registered = try {
            sensorManager.registerListener(listener, detector, SensorManager.SENSOR_DELAY_UI)
        } catch (e: SecurityException) {
            false
        }
        if (!registered) close()
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    /** A long-lived listener whose batching can be changed while it is running. */
    fun openSession(onValue: (Long) -> Unit): LiveSession = LiveSession(onValue)

    inner class LiveSession(private val onValue: (Long) -> Unit) {
        private var registered = false
        private val listener = object : SensorEventListener2 {
            override fun onSensorChanged(event: SensorEvent) {
                onValue(event.values[0].toLong())
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

            override fun onFlushCompleted(sensor: Sensor?) = Unit
        }

        /** (Re)registers with the given batching; 0 means deliver every event immediately. */
        fun start(maxReportLatencyUs: Int): Boolean {
            val sensor = sensor ?: return false
            stop()
            registered = try {
                sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI, maxReportLatencyUs)
            } catch (e: SecurityException) {
                false
            }
            return registered
        }

        /** Asks the sensor to hand over whatever it is holding right now. */
        fun flush() {
            if (registered) sensorManager.flush(listener)
        }

        fun stop() {
            if (registered) {
                sensorManager.unregisterListener(listener)
                registered = false
            }
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 6_000L
        const val BATCH_LATENCY_US = 5_000_000
        const val GRACE_MS = 500L
        const val SETTLE_MS = 3_000L
    }
}
