package com.starmist.walker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.starmist.core.steps.CounterSnapshot

/** Steps for one calendar day. `date` is ISO-8601 (yyyy-MM-dd), so text order is date order. */
@Entity(tableName = "daily_steps")
data class DailyStepsEntity(
    @PrimaryKey val date: String,
    /** Uncorrected sensor steps. */
    val rawSteps: Long = 0,
    /** Sensor steps after the correction factor in force when they were counted. */
    val scaledSteps: Long = 0,
    /** Manual correction added by the user; kept separate so it can be undone. */
    val manualAdjust: Long = 0,
) {
    val total: Long get() = (scaledSteps + manualAdjust).coerceAtLeast(0)
}

/** Single row (id = 1): what the previous counter reading was. */
@Entity(tableName = "snapshot_state")
data class SnapshotStateEntity(
    @PrimaryKey val id: Int = 1,
    val counter: Long,
    val takenAtMillis: Long,
    val bootTimeMillis: Long,
    /** Fractional remainder of the correction factor, carried to the next reading. */
    val carry: Double,
) {
    fun toSnapshot() = CounterSnapshot(counter, takenAtMillis, bootTimeMillis)
}

@Entity(tableName = "reboot_log")
data class RebootLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val atMillis: Long,
    val counterAfter: Long,
)
