package com.starmist.core

import com.starmist.core.metrics.BodyMetrics
import com.starmist.core.metrics.Profile
import com.starmist.core.metrics.Sex
import com.starmist.core.steps.Calibration
import com.starmist.core.steps.CalibrationTrial
import com.starmist.core.steps.StepScaler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalibrationAndMetricsTest {

    @Test
    fun `sensor undercounting gives a multiplier above one`() {
        val m = Calibration.multiplierFrom(listOf(CalibrationTrial(sensorRawSteps = 45, actualSteps = 50)))
        assertNotNull(m)
        assertEquals(50.0 / 45.0, m, 1e-9)
    }

    @Test
    fun `several trials are combined by total, longer walks weigh more`() {
        val m = Calibration.multiplierFrom(
            listOf(CalibrationTrial(20, 22), CalibrationTrial(100, 100)),
        )
        assertNotNull(m)
        assertEquals(122.0 / 120.0, m, 1e-9)
    }

    @Test
    fun `too short or empty trials are ignored`() {
        assertNull(Calibration.multiplierFrom(emptyList()))
        assertNull(Calibration.multiplierFrom(listOf(CalibrationTrial(5, 8))))
        assertNull(Calibration.multiplierFrom(listOf(CalibrationTrial(50, 0))))
    }

    @Test
    fun `calibration result is clamped to the allowed range`() {
        val m = Calibration.multiplierFrom(listOf(CalibrationTrial(20, 200)))
        assertEquals(StepScaler.MAX_MULTIPLIER, m)
    }

    @Test
    fun `scaler carries the fraction`() {
        val first = StepScaler.scale(10, 1.25, 0.0) // 12.5
        assertEquals(12, first.steps)
        val second = StepScaler.scale(10, 1.25, first.carry) // 12.5 + 0.5
        assertEquals(13, second.steps)
    }

    @Test
    fun `nan multiplier falls back to one`() {
        assertEquals(1.0, StepScaler.clampMultiplier(Double.NaN))
    }

    @Test
    fun `stride and distance follow height`() {
        val profile = Profile(heightCm = 170.0, sex = Sex.MALE)
        assertEquals(0.7055, BodyMetrics.strideMeters(profile), 1e-9)
        assertEquals(7.055, BodyMetrics.distanceKm(10_000, profile), 1e-9)
    }

    @Test
    fun `measured stride overrides the estimate`() {
        val profile = Profile(heightCm = 170.0, strideOverrideM = 0.6)
        assertEquals(0.6, BodyMetrics.strideMeters(profile), 1e-9)
    }

    @Test
    fun `calories scale with weight and distance`() {
        val profile = Profile(heightCm = 170.0, weightKg = 70.0, sex = Sex.MALE)
        val km = BodyMetrics.distanceKm(10_000, profile)
        assertEquals(70.0 * km * 0.57, BodyMetrics.kcal(10_000, profile), 1e-9)
    }

    @Test
    fun `stride from a known distance is validated`() {
        assertEquals(0.75, BodyMetrics.strideFromKnownDistance(75.0, 100)!!, 1e-9)
        assertNull(BodyMetrics.strideFromKnownDistance(75.0, 3))
        assertNull(BodyMetrics.strideFromKnownDistance(500.0, 100)) // 5 m per step is not walking
    }

    @Test
    fun `goal progress can pass one hundred percent`() {
        assertEquals(0.5, BodyMetrics.goalProgress(4_000, 8_000), 1e-9)
        assertTrue(BodyMetrics.goalProgress(12_000, 8_000) > 1.0)
        assertEquals(0.0, BodyMetrics.goalProgress(100, 0))
    }
}
