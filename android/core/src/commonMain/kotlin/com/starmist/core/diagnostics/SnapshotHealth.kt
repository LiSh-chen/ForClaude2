package com.starmist.core.diagnostics

enum class Issue {
    /** The "activity recognition" permission has not been granted. */
    PERMISSION_MISSING,
    /** The device has no hardware step counter. */
    NO_HARDWARE_COUNTER,
    /** No reading has ever been taken. */
    NEVER_READ,
    /** Stable mode is on but its foreground service is not running (killed by the system, or never started). */
    SERVICE_NOT_RUNNING,
    /** The last reading is older than expected, so the system probably limits background work. */
    STALE_SNAPSHOT,
    /** The app is subject to battery optimisation, which can delay background readings. */
    BATTERY_RESTRICTED,
}

data class HealthInput(
    val permissionGranted: Boolean,
    val hasHardwareCounter: Boolean,
    val lastSnapshotAtMillis: Long?,
    val nowMillis: Long,
    val batteryRestricted: Boolean,
    /** The user has stable mode switched on. */
    val serviceExpected: Boolean = false,
    val serviceRunning: Boolean = false,
    /** How old a reading may get before it is suspicious. */
    val staleAfterMillis: Long = DEFAULT_STALE_AFTER_MS,
) {
    companion object {
        const val DEFAULT_STALE_AFTER_MS = 6 * 60 * 60 * 1000L
    }
}

object SnapshotHealth {
    /** Most severe first. Empty means everything looks fine. */
    fun evaluate(input: HealthInput): List<Issue> = buildList {
        if (!input.permissionGranted) add(Issue.PERMISSION_MISSING)
        if (!input.hasHardwareCounter) add(Issue.NO_HARDWARE_COUNTER)
        val last = input.lastSnapshotAtMillis
        if (input.permissionGranted && input.hasHardwareCounter) {
            if (input.serviceExpected && !input.serviceRunning) add(Issue.SERVICE_NOT_RUNNING)
            if (last == null) add(Issue.NEVER_READ)
            else if (input.nowMillis - last > input.staleAfterMillis) add(Issue.STALE_SNAPSHOT)
        }
        if (input.batteryRestricted) add(Issue.BATTERY_RESTRICTED)
    }
}
