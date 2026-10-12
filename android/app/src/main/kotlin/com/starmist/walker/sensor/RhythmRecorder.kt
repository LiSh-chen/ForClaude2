package com.starmist.walker.sensor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecorderState(
    val running: Boolean = false,
    val startedAtMillis: Long = 0,
    val steps: Int = 0,
    val batches: Int = 0,
    val file: String? = null,
)

/**
 * Records when each step happened, for composing music from real walking (test builds).
 *
 * The step detector is registered with its events batched in the sensor hub, so it keeps working with
 * the screen off as long as the app process stays alive (the stable-mode service does that). Each batch
 * is written as one line with the steps' own times, so the file shows both the rhythm and how the
 * system delivered it. Only times are stored: no location, no sensor values other than step times and
 * the counter total.
 *
 * File format (text, one record per line):
 *   # header lines            device, Android version, buffer size, batching delay
 *   B,<arrival>,<n>,<t0>,<d1>,<d2>...   a batch: arrived at <arrival>; first step at <t0>, then gaps in ms
 *   C,<time>,<value>                    step counter reading (thinned)
 *   V,<time>,on|off                     screen state
 *   M,<time>,<label>                    a label pressed by the user
 * All times are milliseconds since 1970 (wall clock).
 */
class RhythmRecorder(private val context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val handler = Handler(Looper.getMainLooper())
    private val dir = File(context.filesDir, "rhythm").apply { mkdirs() }

    private val _state = MutableStateFlow(RecorderState())
    val state: StateFlow<RecorderState> = _state

    private var writer: FileWriter? = null
    private var bootOffset = 0L
    private val pending = ArrayList<Long>()
    private var lastArrival = 0L
    private var lastCounterWritten = -1L
    private var lastCounterAt = 0L
    private var steps = 0
    private var batches = 0

    private val flushRunnable = Runnable { flushBatch() }

    private val detectorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val arrival = System.currentTimeMillis()
            if (pending.isNotEmpty() && arrival - lastArrival > BURST_GAP_MS) flushBatch()
            pending += event.timestamp / 1_000_000 + bootOffset
            lastArrival = arrival
            handler.removeCallbacks(flushRunnable)
            handler.postDelayed(flushRunnable, BURST_GAP_MS)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val counterListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val value = event.values[0].toLong()
            val now = System.currentTimeMillis()
            if (lastCounterWritten < 0 || now - lastCounterAt >= 10_000 || value - lastCounterWritten >= 30) {
                line("C,${event.timestamp / 1_000_000 + bootOffset},$value")
                lastCounterWritten = value
                lastCounterAt = now
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> line("V,${System.currentTimeMillis()},on")
                Intent.ACTION_SCREEN_OFF -> line("V,${System.currentTimeMillis()},off")
            }
        }
    }

    fun start(): Boolean {
        if (_state.value.running) return true
        val detector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) ?: return false
        val started = System.currentTimeMillis()
        bootOffset = started - SystemClock.elapsedRealtime()
        val name = "rec_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(started)) + ".txt"
        val out = FileWriter(File(dir, name), true)
        writer = out
        pending.clear()
        steps = 0
        batches = 0
        lastCounterWritten = -1
        out.write("# starmist-rhythm v1\n")
        out.write("# device=${Build.MANUFACTURER} ${Build.MODEL}\n")
        out.write("# android=${Build.VERSION.RELEASE} sdk=${Build.VERSION.SDK_INT}\n")
        out.write("# detector_fifo_max=${detector.fifoMaxEventCount} reserved=${detector.fifoReservedEventCount} wakeup=${detector.isWakeUpSensor}\n")
        out.write("# batch_latency_ms=$LATENCY_MS\n")
        out.write("# start=$started\n")
        out.flush()
        val ok = sensorManager.registerListener(detectorListener, detector, SensorManager.SENSOR_DELAY_NORMAL, LATENCY_MS * 1000)
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let {
            sensorManager.registerListener(counterListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        context.registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        })
        _state.value = RecorderState(running = true, startedAtMillis = started, file = name)
        if (!ok) {
            stop()
            return false
        }
        return true
    }

    fun stop() {
        if (!_state.value.running) return
        sensorManager.flush(detectorListener)
        // Give the hub a moment to hand over what it still holds, then close the file.
        handler.postDelayed({
            sensorManager.unregisterListener(detectorListener)
            sensorManager.unregisterListener(counterListener)
            runCatching { context.unregisterReceiver(screenReceiver) }
            handler.removeCallbacks(flushRunnable)
            flushBatch()
            runCatching { writer?.close() }
            writer = null
            _state.value = _state.value.copy(running = false)
        }, 2_000)
    }

    fun mark(label: String) {
        if (_state.value.running) line("M,${System.currentTimeMillis()},${label.replace(',', ' ')}")
    }

    fun files(): List<File> = dir.listFiles { f -> f.extension == "txt" }?.sortedByDescending { it.name } ?: emptyList()

    private fun flushBatch() {
        if (pending.isEmpty()) return
        val first = pending.first()
        val sb = StringBuilder("B,").append(lastArrival).append(',').append(pending.size).append(',').append(first)
        var previous = first
        for (i in 1 until pending.size) {
            sb.append(',').append(pending[i] - previous)
            previous = pending[i]
        }
        steps += pending.size
        batches++
        pending.clear()
        line(sb.toString())
        _state.value = _state.value.copy(steps = steps, batches = batches)
    }

    private fun line(text: String) {
        runCatching {
            writer?.apply {
                write(text)
                write("\n")
                flush()
            }
        }
    }

    private companion object {
        const val LATENCY_MS = 30_000
        const val BURST_GAP_MS = 400L
    }
}
