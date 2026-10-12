package com.starmist.core.song

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class Voice { BELL, PLUCK, FLUTE, PAD }

/** Tiny additive synthesiser: each voice is a few sine partials under an envelope. */
object Synth {
    const val SAMPLE_RATE = 22_050

    private class Recipe(val partials: List<Pair<Double, Double>>, val attack: Double, val decay: Double, val length: Double, val vibrato: Double)

    private fun recipe(voice: Voice) = when (voice) {
        Voice.BELL -> Recipe(listOf(1.0 to 1.0, 2.76 to 0.4, 5.4 to 0.18), 0.004, 2.4, 1.4, 0.0)
        Voice.PLUCK -> Recipe(listOf(1.0 to 1.0, 2.0 to 0.5, 3.0 to 0.25, 4.0 to 0.12), 0.003, 6.5, 0.7, 0.0)
        Voice.FLUTE -> Recipe(listOf(1.0 to 1.0, 2.0 to 0.28, 3.0 to 0.1), 0.06, 2.2, 1.1, 0.004)
        Voice.PAD -> Recipe(listOf(1.0 to 0.8, 1.005 to 0.8, 2.0 to 0.25), 0.15, 1.6, 1.8, 0.0)
    }

    /** Length in samples of a note of [voice]. */
    fun lengthSamples(voice: Voice): Int = (recipe(voice).length * SAMPLE_RATE).toInt()

    /** One note as 16-bit mono samples at [SAMPLE_RATE]. */
    fun render(voice: Voice, frequency: Double, gain: Double = 0.6): ShortArray {
        val r = recipe(voice)
        val n = lengthSamples(voice)
        val out = ShortArray(n)
        val norm = r.partials.sumOf { it.second }
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = (if (t < r.attack) t / r.attack else 1.0) * exp(-r.decay * t) *
                (if (n - i < 400) (n - i) / 400.0 else 1.0)
            val bend = 1.0 + r.vibrato * sin(2 * PI * 5.0 * t)
            var s = 0.0
            for ((ratio, amp) in r.partials) s += amp * sin(2 * PI * frequency * ratio * bend * t)
            out[i] = (s / norm * env * gain * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }
}
