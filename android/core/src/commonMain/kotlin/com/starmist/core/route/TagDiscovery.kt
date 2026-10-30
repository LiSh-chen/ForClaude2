package com.starmist.core.route

/**
 * Which township feature is found next. In one lap a township gives one feature when it is entered
 * (stage 1) and, if the walk through it is long, a second one about halfway (stage 2); the third waits
 * for the next lap.
 */
object TagDiscovery {
    /** Length of route through a township from which it gives a second feature on the same lap. */
    const val LONG_TOWN_KM = 8.0

    /**
     * The index of the feature found now, or null. [mask] has a bit per feature already found, [foundThisLap]
     * is how many were found in this lap, [tagCount] how many the township has, [stage] 1 or 2.
     */
    fun next(mask: Int, foundThisLap: Int, tagCount: Int, stage: Int): Int? {
        if (foundThisLap >= stage.coerceIn(1, 2)) return null
        return (0 until tagCount).firstOrNull { mask and (1 shl it) == 0 }
    }

    /** Whether the walker is far enough into a township to reach stage 2. */
    fun reachedHalfway(totalKm: Double, walkedKm: Double) = totalKm >= LONG_TOWN_KM && walkedKm >= totalKm / 2
}
