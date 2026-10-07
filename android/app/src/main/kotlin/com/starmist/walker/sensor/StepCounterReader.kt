package com.starmist.walker.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Reads the hardware step counter once. The counter is an "on-change" sensor: registering a
 * listener delivers the current cumulative value right away, so we register, take the first event,
 * and unregister immediately. Nothing keeps listening in the background.
 */
class StepCounterReader(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    val hasHardwareCounter: Boolean get() = sensor != null

    /** Cumulative steps since the last boot, or null if unavailable (no sensor, no permission, timeout). */
    suspend fun readOnce(timeoutMs: Long = 3_000): Long? {
        val sensor = sensor ?: return null
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Long?> { continuation ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        sensorManager.unregisterListener(this)
                        if (continuation.isActive) continuation.resume(event.values[0].toLong())
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                val registered = try {
                    sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                } catch (e: SecurityException) {
                    false // permission missing
                }
                if (!registered) {
                    if (continuation.isActive) continuation.resume(null)
                } else {
                    continuation.invokeOnCancellation { sensorManager.unregisterListener(listener) }
                }
            }
        }
    }
}
