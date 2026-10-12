package com.starmist.core.song

import com.starmist.core.ghost.StepSpan
import com.starmist.core.world.Rng
import java.time.Instant
import java.time.ZoneId
import kotlin.math.pow

/** One note of a song; [degree] counts up the scale from 0 (low tonic) to 9 (high). */
data class Note(val atMs: Long, val lengthMs: Long, val degree: Int, val voice: Voice, val velocity: Double)

data class Song(val notes: List<Note>, val durationMs: Long, val minor: Boolean) {
    val isEmpty: Boolean get() = notes.isEmpty()
}

/** Pentatonic scales, so any run of random-walk notes sounds pleasant. */
object Scale {
    const val DEGREES = 10
    private val major = intArrayOf(0, 2, 4, 7, 9)
    private val minor = intArrayOf(0, 3, 5, 7, 10)

    /** Frequency in Hz; the scale starts at C4 (major) or A3 (minor). */
    fun frequency(degree: Int, minor: Boolean): Double {
        val d = degree.coerceIn(0, DEGREES - 1)
        val steps = (if (minor) this.minor else major)
        val semitone = steps[d % 5] + 12 * (d / 5)
        val root = if (minor) 220.0 else 261.63
        return root * 2.0.pow(semitone / 12.0)
    }
}

/** An endless melody for the steps being taken right now: a gentle random walk up and down the scale. */
class MelodyWalker(seed: Long, private var degree: Int = 4) {
    private val rng = Rng(seed)

    fun next(): Int {
        val roll = rng.nextInt(10)
        var move = when {
            roll < 3 -> 1
            roll < 6 -> -1
            roll < 7 -> 2
            roll < 8 -> -2
            else -> 0
        }
        // Drift back to the middle of the range instead of sticking to an end.
        if (degree <= 1 && move < 0) move = -move
        if (degree >= Scale.DEGREES - 2 && move > 0) move = -move
        degree = (degree + move).coerceIn(0, Scale.DEGREES - 1)
        return degree
    }
}

/**
 * Turns a day of walking into a short piece of music. Every stretch of walking becomes a phrase:
 * more steps give a longer phrase, a brisker pace gives faster and higher notes, the time of day picks
 * the instrument, and a day walked mostly at night is written in a minor key.
 */
object WalkSong {
    const val MIN_STEPS = 30L
    private const val MAX_NOTES = 220

    fun compose(spans: List<StepSpan>, seed: Long, zone: ZoneId, targetMs: Long = 45_000): Song {
        val usable = spans.filter { it.steps > 0 }.sortedBy { it.toMillis }
        val total = usable.sumOf { it.steps }
        if (total < MIN_STEPS) return Song(emptyList(), 0, minor = false)

        val night = usable.filter { hourOf(it.toMillis, zone).let { h -> h >= 19 || h < 5 } }.sumOf { it.steps }
        val minor = night * 10 > total * 4
        val notes = ArrayList<Note>()
        val rng = Rng(seed)
        var clock = 0L
        var degree = if (minor) 4 else 5

        for (span in usable) {
            val slice = (targetMs * span.steps / total).coerceAtLeast(500)
            val minutes = ((span.toMillis - span.fromMillis).coerceAtLeast(60_000)) / 60_000.0
            val pace = (span.steps / minutes).coerceIn(20.0, 140.0)
            val beat = (300 + (140 - pace) * 3.2).toLong() // 300 ms when brisk, up to ~680 ms when slow
            val voice = voiceFor(hourOf(span.toMillis, zone))
            val lift = if (pace >= 100) 2 else if (pace >= 70) 1 else 0
            val count = (slice / beat).toInt().coerceIn(1, 24)
            val gap = slice / count
            val velocity = (0.45 + pace / 140.0 * 0.5).coerceIn(0.4, 0.95)
            for (i in 0 until count) {
                val roll = rng.nextInt(10)
                var move = when {
                    roll < 3 -> 1
                    roll < 6 -> -1
                    roll < 7 -> 2
                    roll < 8 -> -2
                    else -> 0
                }
                if (degree <= 1 && move < 0) move = -move
                if (degree >= Scale.DEGREES - 3 && move > 0) move = -move
                degree = (degree + move).coerceIn(0, Scale.DEGREES - 1 - 2)
                notes += Note(clock + i * gap, (gap * 2).coerceAtMost(1_600), (degree + lift).coerceAtMost(Scale.DEGREES - 1), voice, velocity)
            }
            clock += slice
        }
        // End on the home note so the piece sounds finished.
        val last = notes.last()
        val trimmed = if (notes.size > MAX_NOTES) thin(notes) else notes
        val closing = Note(trimmed.last().atMs + 600, 2_200, 0, last.voice, 0.7)
        val all = trimmed + closing
        return Song(all, closing.atMs + closing.lengthMs, minor)
    }

    private fun thin(notes: List<Note>): List<Note> {
        val keepEvery = (notes.size + MAX_NOTES - 1) / MAX_NOTES
        return notes.filterIndexed { i, _ -> i % keepEvery == 0 }
    }

    private fun hourOf(millis: Long, zone: ZoneId): Int = Instant.ofEpochMilli(millis).atZone(zone).hour

    fun voiceFor(hour: Int): Voice = when (hour) {
        in 5..10 -> Voice.BELL
        in 11..16 -> Voice.PLUCK
        in 17..20 -> Voice.FLUTE
        else -> Voice.PAD
    }

    /** Mixes the whole song into one buffer of 16-bit mono samples, ready to play. */
    fun mix(song: Song): ShortArray {
        if (song.isEmpty) return ShortArray(0)
        val rate = Synth.SAMPLE_RATE
        val total = (song.durationMs * rate / 1000).toInt() + rate
        val acc = FloatArray(total)
        val cache = HashMap<Pair<Voice, Int>, ShortArray>()
        for (note in song.notes) {
            val pcm = cache.getOrPut(note.voice to note.degree) {
                Synth.render(note.voice, Scale.frequency(note.degree, song.minor))
            }
            val start = (note.atMs * rate / 1000).toInt()
            for (i in pcm.indices) {
                val p = start + i
                if (p >= total) break
                acc[p] += pcm[i] * note.velocity.toFloat()
            }
        }
        val peak = acc.maxOf { kotlin.math.abs(it) }.coerceAtLeast(1f)
        val scale = if (peak > Short.MAX_VALUE * 0.9f) Short.MAX_VALUE * 0.9f / peak else 1f
        return ShortArray(total) { (acc[it] * scale).toInt().coerceIn(-32768, 32767).toShort() }
    }
}
