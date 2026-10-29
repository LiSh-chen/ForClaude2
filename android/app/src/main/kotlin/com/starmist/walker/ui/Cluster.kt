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
            // the digital kilometre counter
            val box = roundRect(25f, 76f, 75f, 90f, 2.5f)
            wash(box, ink, 0.92f, 0f, 0f); inked(box, 304, closed = true, width = line * 0.7f)
            label(tm, "%.1f km".format(walkedKm), TextStyle(fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color(0xFFB8E986)), p(50f, 83f))
            needle(cx, cy, r * 0.72f, start + sweep * v, INK_RED, 240)
        }
    }
}

/** Big dial: the way round. The short blue needle is how far round the island you are, the long red one how far through the township; START and END are written on the scale. */
@Composable
internal fun RouteDial(shape: IslandShape, cardFractions: List<Float>, overall: Float, region: Float?, walkedKm: Double, modifier: Modifier = Modifier) {
    val v by animateFloatAsState(overall.coerceIn(0f, 1f), needleSpring, label = "island")
    val w by animateFloatAsState((region ?: 0f).coerceIn(0f, 1f), needleSpring, label = "region")
    val tm = rememberTextMeasurer()
    Canvas(modifier.aspectRatio(1f)) {
        val pen = Pen(this, size)
        with(pen) {
            val cx = 50f; val cy = 52f; val r = 46f; val start = 135f; val sweep = 270f
            face(cx, cy, r, start, sweep, 10, 300, whole = true)
            band(cx, cy, r * 0.76f, r * 0.82f, start, sweep, 0f, v, COLD_BLUE, 330, alpha = 0.5f)
            for (f in cardFractions) {
                val dot = circle(at(cx, cy, r * 0.70f, start + sweep * f), 1.3f * u, 8)
                if (f <= v + 0.001f) wash(dot, COLD_BLUE, 1f, 0f, 0f) else wash(dot, ink, 0.25f, 0f, 0f)
            }
            // the island, with the walked part inked in blue
            fun map(g: Pair<Float, Float>) = p(33f + g.first * 0.34f, 22f + g.second * 0.28f)
            val all = shape.xy.map(::map)
            val split = shape.km.indexOfLast { it <= walkedKm + 1e-9 }.coerceAtLeast(0)
            if (all.size - split > 1) inked(all.drop(split), 301, width = line * 0.6f, dash = dashed(this, 2.5f, 2.5f))
            val walked = all.take(split + 1)
            if (walked.size > 1) { stroke(walked, COLD_BLUE, 302, width = line * 1.6f); inked(walked, 303, width = line * 0.6f) }
            val txt = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Black, color = Vintage.ink)
            label(tm, "START", txt, at(cx, cy, r * 0.62f, start + 14f).let { Offset(it.x + 6f * u, it.y + 3f * u) })
            label(tm, "END", txt, at(cx, cy, r * 0.62f, start + sweep - 14f).let { Offset(it.x - 4f * u, it.y + 3f * u) })
            // long needle: through the township; short one: round the island
            needle(cx, cy, r * 0.80f, start + sweep * w, INK_RED, 310, wide = 0.05f, hub = false)
            needle(cx, cy, r * 0.50f, start + sweep * v, COLD_BLUE, 320, wide = 0.13f)
        }
    }
}

/** Between the dials: the distance to the next card as a car's following-distance display: a digital reading, bars that fill as it gets close, an arrow to the way. */
@Composable
internal fun GapDisplay(km: Double?, bearing: Float?, modifier: Modifier = Modifier) {
    val tm = rememberTextMeasurer()
    val lit = when { km == null -> 0; km <= 2.0 -> 5; km <= 5.0 -> 4; km <= 10.0 -> 3; km <= 20.0 -> 2; else -> 1 }
    Canvas(modifier.aspectRatio(1f)) {
        val pen = Pen(this, size)
        with(pen) {
            // the card ahead
            val pin = listOf(p(50f, 20f), p(40f, 8f), p(43f, -2f), p(50f, -6f), p(57f, -2f), p(60f, 8f))
            wash(pin, INK_RED, 0.9f, 0f, 0f); inked(pin, 700, closed = true, width = line * 0.8f)
            wash(circle(p(50f, 4f), 2.6f * u, 10), Color.White, 1f, 0f, 0f)
            // the window
            val box = roundRect(4f, 26f, 96f, 70f, 5f)
            wash(box, ink, 0.93f, 0f, 0f); inked(box, 701, closed = true, width = line)
            val lcd = Color(0xFFB8E986)
            label(tm, if (km != null) "%.1f".format(km) else "--.-", TextStyle(fontSize = 18.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = lcd), p(54f, 44f))
            label(tm, "km", TextStyle(fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = lcd.copy(alpha = 0.8f)), p(54f, 62f))
            if (bearing != null) {
                val t = rad(bearing)
                fun rot(dx: Float, dy: Float) = p(17f + dx * cos(t).toFloat() - dy * sin(t).toFloat(), 46f + dx * sin(t).toFloat() + dy * cos(t).toFloat())
                val arrow = listOf(rot(0f, -9f), rot(6f, 7f), rot(0f, 3f), rot(-6f, 7f))
                wash(arrow, lcd, 1f, 0f, 0f)
            }
            // five bars, more of them lit the nearer the card
            for (i in 0 until 5) {
                val x = 14f + i * 17f; val top = 92f - i * 4.5f
                val bar = roundRect(x, top, x + 11f, 98f, 1.5f)
                val on = i < lit
                wash(bar, if (on) (if (i >= 3) OK_GREEN else AMBER) else ink, if (on) 0.9f else 0.15f, 0f, 0f)
                inked(bar, 710 + i, closed = true, width = line * 0.6f)
            }
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

/** The whole cluster: the step dial, the following-distance display and the way-round dial. */
@Composable
internal fun InstrumentCluster(
    steps: Long, goal: Int, walkedKm: Double, nextKm: Double?, bearing: Float?, island: IslandShape, cardFractions: List<Float>, overall: Float, region: Float?,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        StepsDial(steps, goal, walkedKm, Modifier.weight(1.3f))
        GapDisplay(nextKm, bearing, Modifier.weight(0.9f))
        RouteDial(island, cardFractions, overall, region, walkedKm, Modifier.weight(1.3f))
    }
}
