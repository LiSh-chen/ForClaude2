package com.starmist.walker.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * A car's instrument cluster in pen sketch: two big round dials with a needle (the way to the next card, the whole island),
 * between them two half dials (fuel for today's steps, temperature for the way through the township), and a row of warning
 * lamps. What is going on is read from where the needles point and which lamps are lit, not from words.
 */

private val INK_RED = Color(0xFFB3202A)
private val AMBER = Color(0xFFE0A21B)
private val OK_GREEN = Color(0xFF3C8D5A)
private val COLD_BLUE = Color(0xFF0E86D4)
private val PAPER = Color(0xFFFFFBEA)

private fun rad(deg: Float) = (deg * PI / 180.0)

/** The point at angle [deg] (degrees, clockwise from east) and radius [r] around ([cx], [cy]), in the pen's units. */
private fun Pen.at(cx: Float, cy: Float, r: Float, deg: Float) = p(cx + (cos(rad(deg)) * r).toFloat(), cy + (sin(rad(deg)) * r).toFloat())

private fun Pen.arcPts(cx: Float, cy: Float, r: Float, d0: Float, d1: Float, n: Int = 36) = (0..n).map { at(cx, cy, r, d0 + (d1 - d0) * it / n) }

/** A coloured band along the dial between two radii, from fraction [f0] to [f1] of the sweep. */
private fun Pen.band(cx: Float, cy: Float, r0: Float, r1: Float, start: Float, sweep: Float, f0: Float, f1: Float, color: Color, seed: Int, alpha: Float = 0.55f) {
    if (f1 - f0 < 0.004f) return
    val a = start + sweep * f0; val b = start + sweep * f1
    val shape = arcPts(cx, cy, r1, a, b, 24) + arcPts(cx, cy, r0, b, a, 24)
    wash(shape, color, alpha, 0f, 0f)
    hatch(shape, color, 0f, 100f, 0f, 100f, gap = 2.6f, lean = 4f, alpha = 0.6f)
    stroke(shape, ink, seed, width = line * 0.7f, closed = true, alpha = 0.8f)
}

/** The rim, ticks and paper of a dial. */
private fun Pen.face(cx: Float, cy: Float, r: Float, start: Float, sweep: Float, majors: Int, seed: Int, whole: Boolean) {
    val rim = if (whole) arcPts(cx, cy, r, 0f, 360f, 40) else arcPts(cx, cy, r, start, start + sweep, 40) + listOf(at(cx, cy, r, start))
    wash(rim, PAPER, 1f, 0f, 0f)
    inked(rim, seed, closed = true, width = line * 1.4f)
    if (whole) inked(arcPts(cx, cy, r * 0.93f, 0f, 360f, 40), seed + 1, closed = true, width = line * 0.6f)
    for (i in 0..majors * 2) {
        val major = i % 2 == 0
        val deg = start + sweep * i / (majors * 2f)
        stroke(listOf(at(cx, cy, r * (if (major) 0.85f else 0.90f), deg), at(cx, cy, r * 0.95f, deg)), ink, seed + 10 + i, width = line * (if (major) 1.1f else 0.7f))
    }
}

private fun Pen.needle(cx: Float, cy: Float, len: Float, deg: Float, color: Color, seed: Int, wide: Float = 0.09f, hub: Boolean = true) {
    val tip = at(cx, cy, len, deg)
    val l = at(cx, cy, len * wide, deg + 90f); val r = at(cx, cy, len * wide, deg - 90f)
    val tail = at(cx, cy, len * 0.18f, deg + 180f)
    val body = listOf(l, tip, r, tail)
    wash(body, color, 0.95f, 0.5f, 0.5f)
    inked(body, seed, closed = true, width = line * 0.9f)
    if (hub) {
        val h = circle(p(cx, cy), 4.2f * u, 18)
        wash(h, Color.White, 1f, 0f, 0f); inked(h, seed + 3, closed = true, width = line)
        wash(circle(p(cx, cy), 1.6f * u, 10), ink, 1f, 0f, 0f)
    }
}

private fun Pen.label(tm: TextMeasurer, s: String, style: TextStyle, c: Offset) {
    val l = tm.measure(s, style)
    d.drawText(l, topLeft = Offset(c.x - l.size.width / 2f, c.y - l.size.height / 2f))
}

private val needleSpring = spring<Float>(dampingRatio = 0.42f, stiffness = Spring.StiffnessVeryLow)

/** The step scale: a round step between ticks, and the labels in thousands when that reads better. */
private fun stepScale(max: Int): Int = listOf(500, 1000, 2000, 2500, 5000, 10000, 20000).first { max / it <= 6 }

/** Big dial: today's steps. The scale is labelled and stretches to 120% of the goal; the red stretch is what lies beyond the goal. A digital window gives the kilometres walked in all. */
@Composable
internal fun StepsDial(steps: Long, goal: Int, walkedKm: Double, modifier: Modifier = Modifier) {
    val max = (goal * 1.2f).coerceAtLeast(1f)
    val v by animateFloatAsState((steps / max).coerceIn(0f, 1f), needleSpring, label = "steps")
    val tm = rememberTextMeasurer()
    Canvas(modifier.aspectRatio(1f)) {
        val pen = Pen(this, size)
        with(pen) {
            val cx = 50f; val cy = 52f; val r = 46f; val start = 135f; val sweep = 270f
            face(cx, cy, r, start, sweep, 0, 200, whole = true)
            val step = stepScale(max.toInt())
            val thousands = step >= 1000
            val small = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Vintage.ink)
            var value = 0
            while (value <= max + 1) {
                val deg = start + sweep * value / max
                stroke(listOf(at(cx, cy, r * 0.84f, deg), at(cx, cy, r * 0.95f, deg)), ink, 210 + value / 100, width = line * 1.2f)
                val half = value + step / 2
                if (half < max) { val d2 = start + sweep * half / max; stroke(listOf(at(cx, cy, r * 0.89f, d2), at(cx, cy, r * 0.95f, d2)), ink, 260 + value / 100, width = line * 0.7f) }
                val txt = if (thousands) (value / 1000f).let { if (it % 1f == 0f) "%d".format(it.toInt()) else "%.1f".format(it) } else value.toString()
                label(tm, txt, small, at(cx, cy, r * 0.66f, deg))
                value += step
            }
            // beyond the goal
            band(cx, cy, r * 0.76f, r * 0.82f, start, sweep, 1f / 1.2f, 1f, INK_RED, 230, alpha = 0.6f)
            label(tm, if (thousands) "x1000" else "STEPS", TextStyle(fontSize = 7.sp, color = Vintage.inkSoft), p(cx, 30f))
            // the kilometre counter, a recessed window in the face of the dial
            val box = roundRect(23f, 77f, 77f, 88f, 3f)
            wash(box, ink, 0.07f, 0f, 0f); inked(box, 304, closed = true, width = line * 0.8f)
            label(tm, "Total", TextStyle(fontSize = 6.sp, fontWeight = FontWeight.Bold, color = Vintage.inkSoft), p(33f, 82.5f))
            label(tm, "%.1f".format(walkedKm), TextStyle(fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Vintage.ink), p(54f, 82.5f))
            label(tm, "km", TextStyle(fontSize = 6.sp, color = Vintage.inkSoft), p(71f, 83.5f))
            needle(cx, cy, r * 0.72f, start + sweep * v, INK_RED, 240)
        }
    }
}

/** Big dial: the way round, scaled in percent. The short blue needle is how far round the island you are, the long red one how far through the township; the two values are written in the window below. */
@Composable
internal fun RouteDial(overall: Float, region: Float?, modifier: Modifier = Modifier) {
    val v by animateFloatAsState(overall.coerceIn(0f, 1f), needleSpring, label = "island")
    val w by animateFloatAsState((region ?: 0f).coerceIn(0f, 1f), needleSpring, label = "region")
    val tm = rememberTextMeasurer()
    Canvas(modifier.aspectRatio(1f)) {
        val pen = Pen(this, size)
        with(pen) {
            val cx = 50f; val cy = 52f; val r = 46f; val start = 135f; val sweep = 270f
            face(cx, cy, r, start, sweep, 10, 300, whole = true)
            band(cx, cy, r * 0.76f, r * 0.82f, start, sweep, 0f, v, COLD_BLUE, 330, alpha = 0.5f)
            val num = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Vintage.ink)
            val word = TextStyle(fontSize = 6.sp, fontWeight = FontWeight.Black, color = Vintage.ink)
            for (i in 0..5) {
                val f = i / 5f
                val c = at(cx, cy, r * 0.64f, start + sweep * f)
                when (i) { 0 -> label(tm, "START", word, Offset(c.x + 3f * u, c.y - 2f * u)); 5 -> label(tm, "END", word, Offset(c.x - 3f * u, c.y - 2f * u)); else -> label(tm, "${i * 20}", num, c) }
            }
            label(tm, "%", TextStyle(fontSize = 7.sp, color = Vintage.inkSoft), p(cx, 30f))
            // the two readings
            val box = roundRect(23f, 74f, 77f, 90f, 3f)
            wash(box, ink, 0.07f, 0f, 0f); inked(box, 304, closed = true, width = line * 0.8f)
            label(tm, "環島 ${(v * 100).toInt()}%", TextStyle(fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B5E96)), p(50f, 79.5f))
            label(tm, if (region != null) "區域 ${(w * 100).toInt()}%" else "區域 --", TextStyle(fontSize = 7.sp, fontWeight = FontWeight.Bold, color = INK_RED), p(50f, 85.5f))
            // long needle: through the township; short one: round the island
            needle(cx, cy, r * 0.80f, start + sweep * w, INK_RED, 310, wide = 0.05f, hub = false)
            needle(cx, cy, r * 0.50f, start + sweep * v, COLD_BLUE, 320, wide = 0.13f)
        }
    }
}

/** Between the dials: how far the next card is, as a driving-assist display. The card is up the road, the traveller at the bottom, and stripes across the road are the gap: the farther away, the more of them. The distance is written below. */
@Composable
internal fun GapDisplay(km: Double?, modifier: Modifier = Modifier) {
    val tm = rememberTextMeasurer()
    val lit = when { km == null -> 0; km <= 2.0 -> 1; km <= 5.0 -> 2; km <= 10.0 -> 3; km <= 20.0 -> 4; else -> 5 }
    Canvas(modifier.aspectRatio(0.75f)) {
        val pen = Pen(this, size)
        with(pen) {
            // the road: two edges running up to the card, and a dashed middle
            val top = 14f; val bottom = 92f
            fun half(y: Float) = 8f + (y - top) / (bottom - top) * 38f
            val left = listOf(p(50f - half(top), top), p(50f - half(bottom), bottom))
            val right = listOf(p(50f + half(top), top), p(50f + half(bottom), bottom))
            val road = listOf(left[0], right[0], right[1], left[1])
            wash(road, ink, 0.06f, 0f, 0f)
            inked(left, 800, width = line * 1.2f); inked(right, 801, width = line * 1.2f)
            inked(listOf(p(50f, top), p(50f, bottom)), 802, width = line * 0.7f, dash = dashed(this, 5f, 6f))
            // the stripes across the road: the nearest to the traveller first
            for (i in 0 until 5) {
                val y1 = 78f - i * 12f; val y0 = y1 - 5.5f
                val s = listOf(p(50f - half(y0) + 2f, y0), p(50f + half(y0) - 2f, y0), p(50f + half(y1) - 2f, y1), p(50f - half(y1) + 2f, y1))
                val on = i < lit
                wash(s, if (on) COLD_BLUE else ink, if (on) 0.8f else 0.1f, 0f, 0f)
                inked(s, 810 + i, closed = true, width = line * 0.6f)
            }
            // the card at the far end
            val pin = listOf(p(50f, 12f), p(42f, 1f), p(44f, -6f), p(50f, -9f), p(56f, -6f), p(58f, 1f))
            wash(pin, INK_RED, 0.9f, 0f, 0f); inked(pin, 820, closed = true, width = line * 0.8f)
            wash(circle(p(50f, -2.5f), 2.4f * u, 10), Color.White, 1f, 0f, 0f)
            // the traveller, with a hat and a pack
            val hat = listOf(p(43f, 75f), p(57f, 75f), p(54f, 70f), p(46f, 70f))
            wash(circle(p(50f, 77f), 4f * u, 12), Color.White, 1f, 0f, 0f)
            wash(hat, Color(0xFF6B4A24), 0.9f, 0f, 0f); inked(hat, 830, closed = true, width = line * 0.6f)
            stroke(listOf(p(50f, 81f), p(50f, 93f)), ink, 831, width = line * 1.5f)
            stroke(listOf(p(41f, 86f), p(50f, 83f), p(59f, 86f)), ink, 832, width = line * 1.3f)
            stroke(listOf(p(50f, 93f), p(43f, 102f)), ink, 833, width = line * 1.3f)
            stroke(listOf(p(50f, 93f), p(57f, 102f)), ink, 834, width = line * 1.3f)
            val pack = roundRect(53f, 82f, 61f, 92f, 2f)
            wash(pack, Color(0xFF6B4A24), 0.7f, 0f, 0f); inked(pack, 835, closed = true, width = line * 0.6f)
            // the distance
            val txt = if (km != null) "%.1f km".format(km) else "-- km"
            label(tm, txt, TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Vintage.ink), p(50f, 110f))
        }
    }
}

/** The warning lamps: a pill that glows in its colour while lit. */
@Composable
internal fun Telltale(kind: Int, on: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(1.5f)) {
        val pen = Pen(this, size)
        with(pen) {
            val pill = roundRect(-20f, 6f, 120f, 94f, 30f)
            wash(pill, color, if (on) 0.85f else 0.10f, 0f, 0f)
            if (on) wash(roundRect(-34f, -8f, 134f, 108f, 40f), color, 0.18f, 0f, 0f)
            inked(pill, 600 + kind, closed = true, width = line * (if (on) 1.1f else 0.8f))
            val g = if (on) Color.White else ink.copy(alpha = 0.35f)
            val gl = if (on) ink else ink.copy(alpha = 0.35f)
            when (kind) {
                0 -> { // walker
                    wash(circle(p(50f, 28f), 7f * u, 12), g, 1f, 0f, 0f)
                    stroke(listOf(p(50f, 36f), p(50f, 62f)), gl, 610, width = line * 1.6f)
                    stroke(listOf(p(36f, 46f), p(50f, 42f), p(64f, 48f)), gl, 611, width = line * 1.4f)
                    stroke(listOf(p(50f, 62f), p(38f, 80f)), gl, 612, width = line * 1.4f)
                    stroke(listOf(p(50f, 62f), p(64f, 80f)), gl, 613, width = line * 1.4f)
                }
                1 -> { // star: goal reached
                    val star = (0 until 10).map { val rr = if (it % 2 == 0) 30f else 13f; at(50f, 52f, rr, -90f + it * 36f) }
                    wash(star, g, 1f, 0f, 0f); inked(star, 620, closed = true, width = line * 0.7f)
                }
                2 -> { // pin: a card is near
                    val pin = listOf(p(50f, 82f), p(34f, 46f), p(38f, 28f), p(50f, 20f), p(62f, 28f), p(66f, 46f))
                    wash(pin, g, 1f, 0f, 0f); inked(pin, 630, closed = true, width = line * 0.7f)
                    wash(circle(p(50f, 40f), 5f * u, 10), gl, 0.9f, 0f, 0f)
                }
                else -> { // boundary: a dashed line crossing a road
                    inked(listOf(p(50f, 16f), p(50f, 86f)), 640, width = line * 1.2f, dash = dashed(this, 6f, 5f))
                    val arrow = listOf(p(20f, 52f), p(42f, 52f)); stroke(arrow, gl, 641, width = line * 1.4f)
                    stroke(listOf(p(36f, 44f), p(44f, 52f), p(36f, 60f)), gl, 642, width = line * 1.4f)
                    stroke(listOf(p(58f, 52f), p(80f, 52f)), gl, 643, width = line * 1.4f)
                    stroke(listOf(p(74f, 44f), p(82f, 52f), p(74f, 60f)), gl, 644, width = line * 1.4f)
                }
            }
        }
    }
}

/** The whole cluster: the step dial, the gap to the next card and the way-round dial. */
@Composable
internal fun InstrumentCluster(
    steps: Long, goal: Int, walkedKm: Double, nextKm: Double?, overall: Float, region: Float?,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        StepsDial(steps, goal, walkedKm, Modifier.weight(1.3f))
        GapDisplay(nextKm, Modifier.weight(0.9f))
        RouteDial(overall, region, Modifier.weight(1.3f))
    }
}
