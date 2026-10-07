package com.starmist.core

import com.starmist.core.sensor.AccelStepDetector
import com.starmist.core.sensor.FilterLevel
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccelStepDetectorTest {
    private val sampleRateHz = 50
    private val gravity = 9.81

    /** Feeds a sinusoidal bounce along the vertical axis for [seconds]; returns confirmed steps. */
    private fun feedSine(
        detector: AccelStepDetector,
        freqHz: Double,
        amplitude: Double,
        seconds: Double,
        startMs: Long = 0,
    ): Int {
        val samples = (seconds * sampleRateHz).toInt()
        var steps = 0
        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRateHz
            val z = gravity + amplitude * sin(2 * PI * freqHz * t)
            steps += detector.onSample(startMs + (t * 1000).toLong(), 0.0, 0.0, z)
        }
        return steps
    }

    @Test
    fun `normal walking is counted`() {
        val detector = AccelStepDetector()
        val steps = feedSine(detector, freqHz = 2.0, amplitude = 3.0, seconds = 10.0)
        // 20 peaks in 10 s; allow for the first/last partial cycle.
        assertTrue(steps in 17..21, "counted $steps")
        assertEquals(steps.toLong(), detector.totalSteps)
    }

    @Test
    fun `fast vibration is rejected`() {
        val detector = AccelStepDetector()
        assertEquals(0, feedSine(detector, freqHz = 8.0, amplitude = 6.0, seconds = 10.0))
    }

    @Test
    fun `a few isolated bumps do not count`() {
        val detector = AccelStepDetector()
        var steps = 0
        // Three single jolts, 5 s apart: each is a lone peak, never a walking rhythm.
        for (jolt in 0 until 3) {
            val base = jolt * 5_000L
            steps += feedSine(detector, freqHz = 2.0, amplitude = 5.0, seconds = 0.5, startMs = base)
            for (i in 0 until 4 * sampleRateHz) {
                steps += detector.onSample(base + 500 + i * 1000L / sampleRateHz, 0.0, 0.0, gravity)
            }
        }
        assertEquals(0, steps)
    }

    @Test
    fun `random noise around gravity is rejected on standard`() {
        val detector = AccelStepDetector()
        val random = Random(42)
        var steps = 0
        for (i in 0 until 20 * sampleRateHz) {
            val noise = random.nextDouble(-0.4, 0.4)
            steps += detector.onSample(i * 1000L / sampleRateHz, 0.0, 0.0, gravity + noise)
        }
        assertEquals(0, steps)
    }

    @Test
    fun `strict level ignores a weak bounce that loose level accepts`() {
        val weak = 1.6
        val loose = AccelStepDetector(AccelStepDetector.Config.of(FilterLevel.LOOSE))
        val strict = AccelStepDetector(AccelStepDetector.Config.of(FilterLevel.STRICT))
        val looseSteps = feedSine(loose, 2.0, weak, 10.0)
        val strictSteps = feedSine(strict, 2.0, weak, 10.0)
        assertTrue(looseSteps > 10, "loose counted $looseSteps")
        assertEquals(0, strictSteps)
    }

    @Test
    fun `walking sequence interrupted by a pause starts over`() {
        val detector = AccelStepDetector()
        val first = feedSine(detector, 2.0, 3.0, 5.0, startMs = 0)
        // Standing still for 5 s.
        for (i in 0 until 5 * sampleRateHz) {
            detector.onSample(5_000 + i * 1000L / sampleRateHz, 0.0, 0.0, gravity)
        }
        val second = feedSine(detector, 2.0, 3.0, 5.0, startMs = 10_000)
        assertTrue(first in 7..11, "first $first")
        assertTrue(second in 7..11, "second $second")
    }

    @Test
    fun `reset clears the count`() {
        val detector = AccelStepDetector()
        feedSine(detector, 2.0, 3.0, 5.0)
        detector.reset()
        assertEquals(0, detector.totalSteps)
    }

    @Test
    fun `config overrides apply`() {
        val config = AccelStepDetector.Config.of(FilterLevel.STANDARD, threshold = 0.5, minConsecutive = 0)
        assertEquals(0.5, config.peakThreshold)
        assertEquals(1, config.minConsecutive) // never below one
    }
}
