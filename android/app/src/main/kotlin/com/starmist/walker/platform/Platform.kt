package com.starmist.walker.platform

import com.starmist.walker.data.RegionStore
import com.starmist.walker.data.RouteStore
import com.starmist.walker.data.SettingsStore
import com.starmist.walker.data.StepRepository
import com.starmist.walker.ui.TownAreas
import com.starmist.walker.ui.TownTag
import kotlinx.coroutines.flow.Flow

/*
 * What the app needs from the phone, as plain interfaces. Everything above this line of the code knows only these;
 * the Android classes that implement them live in the sensor, tracking and platform packages, and the iOS app will
 * bring its own (CoreMotion for the steps, the system clock, and so on).
 */

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

/** What the step detector (one event per step) says about its own buffering; for the diagnostics screen. */
data class DetectorInfo(val present: Boolean, val fifoMax: Int, val fifoReserved: Int, val wakeUp: Boolean)

/** Outcome of a batching trial: did per-step events arrive late but with their original times? */
data class BatchTestResult(
    val detectorEvents: Int,
    /** Steps the cumulative counter gained over the same period, for comparison. */
    val counterSteps: Long,
    /** How many separate bursts the events arrived in. */
    val bursts: Int,
    val largestBurst: Int,
    /** Longest gap between a step happening and its event arriving. */
    val maxDelayMs: Long,
    val accepted: Boolean,
)


/** The step counter of the phone. */
interface StepSensor {
    val hasHardwareCounter: Boolean

    /** One reading of the cumulative counter, or null when none arrived in time. */
    suspend fun read(timeoutMs: Long = 6_000L): CounterReading?

    suspend fun readOnce(timeoutMs: Long = 6_000L): Long? = read(timeoutMs)?.value

    /** The cumulative counter while the app is on screen. */
    fun liveCounter(maxReportLatencyUs: Int = 0): Flow<Long>

    /** One event per step, to make the number move with every step. */
    fun stepTicks(): Flow<Unit>

    /** For the diagnostics screen; only Android has a step detector to ask about. */
    val detectorInfo: DetectorInfo? get() = null

    suspend fun batchTest(durationMs: Long, latencyMs: Int): BatchTestResult? = null
}

/** Permission, power and clock facts that differ from one phone system to the next. */
interface PlatformInfo {
    fun hasStepPermission(): Boolean

    /** True when the system may stop the app in the background to save power. */
    fun batteryRestricted(): Boolean

    fun nowMillis(): Long

    /** Milliseconds since the phone started, to tell a reboot from a continuing count. */
    fun elapsedSinceBootMillis(): Long

    /** Whether the background step service is running (Android only). */
    val stepServiceRunning: Boolean
}

/** Everything the view model needs, so that it does not have to know which phone system it is on. */
interface AppServices {
    val settings: SettingsStore
    val routes: RouteStore
    val regions: RegionStore
    val repository: StepRepository
    val platform: PlatformInfo

    /** The township outlines and their features, read once from the app's files. */
    val townAreas: TownAreas
    val townBlurbs: Map<String, List<TownTag>>
}
