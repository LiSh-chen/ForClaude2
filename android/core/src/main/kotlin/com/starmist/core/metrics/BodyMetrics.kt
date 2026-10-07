package com.starmist.core.metrics

enum class Sex { MALE, FEMALE, OTHER }

/** Body data used to turn steps into distance and energy. All outputs are estimates. */
data class Profile(
    val heightCm: Double = 170.0,
    val weightKg: Double = 65.0,
    val sex: Sex = Sex.OTHER,
    /** Measured stride in metres; overrides the height-based estimate when set. */
    val strideOverrideM: Double? = null,
)

object BodyMetrics {
    private const val KCAL_PER_KG_PER_KM = 0.57

    fun strideMeters(profile: Profile): Double {
        profile.strideOverrideM?.let { if (it > 0) return it }
        val factor = when (profile.sex) {
            Sex.MALE -> 0.415
            Sex.FEMALE -> 0.413
            Sex.OTHER -> 0.414
        }
        return profile.heightCm * factor / 100.0
    }

    fun distanceKm(steps: Long, profile: Profile): Double =
        steps.coerceAtLeast(0) * strideMeters(profile) / 1000.0

    fun kcal(steps: Long, profile: Profile): Double =
        profile.weightKg * distanceKm(steps, profile) * KCAL_PER_KG_PER_KM

    /** Stride from a walk of known length, or null if the input is unusable. */
    fun strideFromKnownDistance(distanceM: Double, steps: Int): Double? {
        if (distanceM <= 0 || steps < 10) return null
        val stride = distanceM / steps
        return if (stride in 0.2..1.5) stride else null
    }

    /** 0.0 when no goal is set; may exceed 1.0 once the goal is passed. */
    fun goalProgress(steps: Long, goal: Int): Double =
        if (goal <= 0) 0.0 else steps.coerceAtLeast(0).toDouble() / goal
}
