package com.starmist.core.steps

/** One calibration walk: what the sensor counted (before any correction) vs. what the user counted. */
data class CalibrationTrial(val sensorRawSteps: Int, val actualSteps: Int)

object Calibration {
    private const val MIN_RAW_STEPS_PER_TRIAL = 10

    /** A trial is usable when the walk was long enough to mean something. */
    fun isUsable(trial: CalibrationTrial): Boolean =
        trial.sensorRawSteps >= MIN_RAW_STEPS_PER_TRIAL && trial.actualSteps > 0

    /**
     * Multiplier that would make the sensor agree with the user, from the sum of all usable trials
     * (longer walks weigh more). Returns null when there is nothing usable.
     */
    fun multiplierFrom(trials: List<CalibrationTrial>): Double? {
        val usable = trials.filter(::isUsable)
        if (usable.isEmpty()) return null
        val raw = usable.sumOf { it.sensorRawSteps }
        val actual = usable.sumOf { it.actualSteps }
        return StepScaler.clampMultiplier(actual.toDouble() / raw)
    }
}
