package com.starmist.walker.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
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
import kotlin.math.floor
import kotlin.math.sin

/*
 * A car's instrument cluster in pen sketch: two big round dials with a needle (the way to the next card, the whole island),
 * between them two half dials (fuel for today's steps, temperature for the way through the township), . What is going on is read from where the needles point and which lamps are lit, not from words.
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
    inked(rim, seed, closed = true, width = line * 1.4f, calm = 0.25f)
    if (whole) inked(arcPts(cx, cy, r * 0.93f, 0f, 360f, 40), seed + 1, closed = true, width = line * 0.6f, calm = 0.25f)
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

/** A reading in tenths that rolls to its value: from zero when the screen first shows, and from the old reading when it changes. */
@Composable
private fun rememberRolling(tenths: Float): Float {
    // The screenshot tests take their picture at once, so there the reading does not roll.
    val inspect = LocalInspectionMode.current || decodeThumbsInline
    val a = remember { Animatable(if (inspect) tenths else 0f) }
    LaunchedEffect(tenths) { if (inspect) a.snapTo(tenths) else a.animateTo(tenths, tween(1600, easing = FastOutSlowInEasing)) }
    return a.value
}

/**
 * A mechanical odometer of [cells] drums, the last one (tenths, with its decimal point) on a pale drum, as on a car's dial. [tenths] is the
 * reading times ten, or null for a dashed, blank one. A drum rolls to the next digit only when the drums below it pass from 9 to 0.
 * [x] is the left edge, [y] the top, in the pen's units.
 */
private fun Pen.odometer(tm: TextMeasurer, x: Float, y: Float, tenths: Float?, cells: Int = 5, cw: Float = 4.8f, ch: Float = 7f) {
    val lastW = cw * 1.55f
    val total = cw * (cells - 1) + lastW
    val frame = roundRect(x - 0.8f, y - 0.8f, x + total + 0.8f, y + ch + 0.8f, 1.2f)
    wash(frame, ink, 0.85f, 0f, 0f); inked(frame, 900, closed = true, width = line * 0.8f)
    val cream = Color(0xFFF3E9C9)
    // sized from the drum itself, so that ".9" fits the tenths drum whatever the size of the dial
    val fontSize = with(d) { minOf(ch * u * 0.75f, lastW * u / 1.35f).toSp() }
    val t = tenths?.coerceIn(0f, Math.pow(10.0, cells.toDouble()).toFloat() - 1f)
    val whole = t?.let { floor(it).toLong() } ?: 0L
    val carry = if (t != null) t - whole else 0f
    for (i in 0 until cells) {
        val k = cells - 1 - i
        val last = k == 0
        val x0 = x + i * cw
        val w = if (last) lastW else cw
        val cell = listOf(p(x0 + 0.25f, y), p(x0 + w - 0.25f, y), p(x0 + w - 0.25f, y + ch), p(x0 + 0.25f, y + ch))
        wash(cell, if (last) cream else Color(0xFF2B2118), 1f, 0f, 0f)
        val style = TextStyle(fontSize = fontSize, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = if (last) Vintage.ink else cream)
        val cx = x0 + w / 2f
        if (t == null) { label(tm, "-", style, p(cx, y + ch / 2f)); continue }
        var m = 1L; repeat(k) { m *= 10 }
        val roll = if (whole % m == m - 1) carry else 0f
        val digit = ((whole / m) % 10).toInt()
        fun text(d: Int) = if (last) ".${d % 10}" else "${d % 10}"
        clipTo(cell) {
            label(tm, text(digit), style, p(cx, y + ch / 2f - roll * ch))
            label(tm, text(digit + 1), style, p(cx, y + ch / 2f + (1f - roll) * ch))
        }
    }
}

/** The segments lit for each digit, a to g as bits 0 to 6; 10 is a dash. */
private val SEGMENTS = intArrayOf(0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F, 0x40)

/** One seven-segment digit with [x], [y] its top left in the pen's units; [digit] null leaves it blank, only the ghost of the unlit segments. */
private fun Pen.digit7(x: Float, y: Float, w: Float, h: Float, digit: Int?) {
    val t = w * 0.2f
    val mid = y + h / 2f
    val mask = if (digit == null) 0 else SEGMENTS[digit]
    fun hz(yc: Float): List<Offset> { val x0 = x + t * 0.6f; val x1 = x + w - t * 0.6f; return listOf(p(x0, yc), p(x0 + t / 2, yc - t / 2), p(x1 - t / 2, yc - t / 2), p(x1, yc), p(x1 - t / 2, yc + t / 2), p(x0 + t / 2, yc + t / 2)) }
    fun vt(xc: Float, y0: Float, y1: Float) = listOf(p(xc, y0), p(xc + t / 2, y0 + t / 2), p(xc + t / 2, y1 - t / 2), p(xc, y1), p(xc - t / 2, y1 - t / 2), p(xc - t / 2, y0 + t / 2))
    val shapes = listOf(
        hz(y + t / 2), vt(x + w - t / 2, y + t * 0.6f, mid - t * 0.4f), vt(x + w - t / 2, mid + t * 0.4f, y + h - t * 0.6f), hz(y + h - t / 2),
        vt(x + t / 2, mid + t * 0.4f, y + h - t * 0.6f), vt(x + t / 2, y + t * 0.6f, mid - t * 0.4f), hz(mid),
    )
    for ((i, shape) in shapes.withIndex()) wash(shape, ink, if (mask and (1 shl i) != 0) 0.92f else 0.07f, 0f, 0f)
}

private val needleSpring = spring<Float>(dampingRatio = 0.42f, stiffness = Spring.StiffnessVeryLow)

/** The step scale: a round step between ticks, and the labels in thousands when that reads better. */
private fun stepScale(max: Int): Int = listOf(500, 1000, 2000, 2500, 5000, 10000, 20000).first { max / it <= 6 }

/** Big dial: today's steps. The scale is labelled and stretches to 120% of the goal; the red stretch is what lies beyond the goal. A digital window gives the kilometres walked in all. */
@Composable
internal fun StepsDial(steps: Long, goal: Int, walkedKm: Double, modifier: Modifier = Modifier) {
    val max = (goal * 1.2f).coerceAtLeast(1f)
    val v by animateFloatAsState((steps / max).coerceIn(0f, 1f), needleSpring, label = "steps")
    val km10 = rememberRolling((walkedKm * 10).toFloat())
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
            label(tm, if (thousands) "x1000 步" else "步", TextStyle(fontSize = 7.sp, color = Vintage.inkSoft), p(cx, 30f))
            // the kilometre counter, a recessed window in the face of the dial
            label(tm, "總里程 km", TextStyle(fontSize = 6.sp, fontWeight = FontWeight.Bold, color = Vintage.inkSoft), p(50f, 73.5f))
            odometer(tm, 36.7f, 78f, km10)
            needle(cx, cy, r * 0.72f, start + sweep * v, INK_RED, 240)
        }
    }
}

/** Big dial: the way round, scaled in percent. The short blue needle is how far round the island you are, the long red one how far through the township; the two values are written in the window below. */
@Composable
internal fun RouteDial(overall: Float, region: Float?, islandKm: Double, townKm: Double?, modifier: Modifier = Modifier) {
    val v by animateFloatAsState(overall.coerceIn(0f, 1f), needleSpring, label = "island")
    val w by animateFloatAsState((region ?: 0f).coerceIn(0f, 1f), needleSpring, label = "region")
    val islandT = rememberRolling((islandKm * 10).toFloat())
    val townT = rememberRolling(((townKm ?: 0.0) * 10).toFloat())
    val tm = rememberTextMeasurer()
    Canvas(modifier.aspectRatio(1f)) {
        val pen = Pen(this, size)
        with(pen) {
            val cx = 50f; val cy = 52f; val r = 46f; val start = 135f; val sweep = 270f
            face(cx, cy, r, start, sweep, 10, 300, whole = true)
            band(cx, cy, r * 0.76f, r * 0.82f, start, sweep, 0f, v, COLD_BLUE, 330, alpha = 0.5f)
            val num = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Vintage.ink)
            for (i in 0..5) label(tm, "${i * 20}", num, at(cx, cy, r * 0.66f, start + sweep * i / 5f))
            label(tm, "進度 %", TextStyle(fontSize = 6.sp, color = Vintage.inkSoft), p(cx, 30f))
            // the two distances walked, as two odometers, the unit printed on the face
            label(tm, "km", TextStyle(fontSize = 5.sp, color = Vintage.inkSoft), p(50f, 61.5f))
            // a dot in the colour of each needle says whose distance it is
            for ((y, color) in listOf(67.3f to Color(0xFF0E86D4), 77f to INK_RED)) {
                val dot = circle(p(39.5f, y), 1.9f * u, 12)
                wash(dot, color, 1f, 0f, 0f); inked(dot, 330 + y.toInt(), closed = true, width = line * 0.6f, calm = 0.3f)
            }
            odometer(tm, 44.5f, 64.5f, islandT, cw = 3.6f, ch = 5.5f)
            odometer(tm, 44.5f, 74.2f, if (townKm != null) townT else null, cw = 3.6f, ch = 5.5f)
            // long needle: through the township; short one: round the island
            needle(cx, cy, r * 0.80f, start + sweep * w, INK_RED, 310, wide = 0.05f, hub = false)
            needle(cx, cy, r * 0.50f, start + sweep * v, COLD_BLUE, 320, wide = 0.13f)
        }
    }
}

/** Between the dials: how far the next card is, as a driving-assist display on a face like the dials'. The card is up the road, the traveller at the bottom, and stripes across the road are the gap: the farther away, the more of them. The distance is a digital readout below. */
@Composable
internal fun GapDisplay(km: Double?, modifier: Modifier = Modifier) {
    val tm = rememberTextMeasurer()
    val walker = rememberArt("traveller_back") ?: rememberArt("traveller_walkx_2")
    val lit = when { km == null -> 0; km <= 2.0 -> 1; km <= 5.0 -> 2; km <= 10.0 -> 3; km <= 20.0 -> 4; else -> 5 }
    Canvas(modifier.aspectRatio(0.7f)) {
        val pen = Pen(this, size)
        with(pen) {
            // the face, like a dial's
            val face = roundRect(3f, -19f, 97f, 119f, 12f)
            wash(face, PAPER, 1f, 0f, 0f)
            inked(face, 790, closed = true, width = line * 1.4f, calm = 0.25f)
            inked(roundRect(6f, -16f, 94f, 116f, 10f), 791, closed = true, width = line * 0.6f, calm = 0.25f)
            // the road: two edges running up to the card, shaded, with a dashed middle
            val top = 11f; val bottom = 83f
            fun half(y: Float) = 8f + (y - top) / (bottom - top) * 38f
            val left = listOf(p(50f - half(top), top), p(50f - half(bottom), bottom))
            val right = listOf(p(50f + half(top), top), p(50f + half(bottom), bottom))
            val road = listOf(left[0], right[0], right[1], left[1])
            hatch(road, ink, 0f, 100f, 0f, 100f, gap = 3.4f, lean = 5f, alpha = 0.16f)
            inked(left, 800, width = line * 1.2f); inked(right, 801, width = line * 1.2f)
            inked(listOf(p(50f, top), p(50f, bottom)), 802, width = line * 0.7f, dash = dashed(this, 5f, 6f))
            // the stripes across the road, washed and hatched like the dials' bands: the nearest to the traveller first
            for (i in 0 until 5) {
                val y1 = 49f - i * 7.2f; val y0 = y1 - 4.4f
                val st = listOf(p(50f - half(y0) + 2f, y0), p(50f + half(y0) - 2f, y0), p(50f + half(y1) - 2f, y1), p(50f - half(y1) + 2f, y1))
                if (i < lit) {
                    wash(st, COLD_BLUE, 0.45f, 0f, 0f)
                    hatch(st, COLD_BLUE, 0f, 100f, 0f, 100f, gap = 2.2f, lean = 3f, alpha = 0.7f)
                } else wash(st, ink, 0.05f, 0f, 0f)
                inked(st, 810 + i, closed = true, width = line * 0.7f)
            }
            // the card at the far end
            val pin = listOf(p(50f, 9f), p(42f, -2f), p(44f, -9f), p(50f, -12f), p(56f, -9f), p(58f, -2f))
            wash(pin, INK_RED, 0.55f, 0f, 0f)
            hatch(pin, INK_RED, 0f, 100f, 0f, 100f, gap = 2.2f, lean = 3f, alpha = 0.7f)
            inked(pin, 820, closed = true, width = line * 0.9f)
            wash(circle(p(50f, -5.5f), 2.4f * u, 10), PAPER, 1f, 0f, 0f)
            // the traveller of the map
            if (walker != null) {
                val h = 32f * u; val w = h * walker.width / walker.height
                val foot = p(50f, 51f)
                d.drawImage(walker, srcOffset = IntOffset.Zero, srcSize = IntSize(walker.width, walker.height), dstOffset = IntOffset((foot.x - w / 2f).roundToInt(), foot.y.roundToInt()), dstSize = IntSize(w.roundToInt(), h.roundToInt()), filterQuality = FilterQuality.High)
            }
            // the distance to the next card: printed on the face, read on a digital window
            label(tm, "距下一景點", TextStyle(fontSize = 6.sp, fontWeight = FontWeight.Bold, color = Vintage.inkSoft), p(50f, 89f))
            val window = roundRect(18f, 94f, 82f, 109f, 3f)
            wash(window, ink, 0.07f, 0f, 0f); inked(window, 304, closed = true, width = line * 0.8f)
            val tenths = km?.let { (it * 10).roundToInt().coerceIn(0, 9999) }
            val cells = if (tenths == null) listOf(null, null, 10, 10) else listOf(
                if (tenths >= 1000) tenths / 1000 else null,
                if (tenths >= 100) tenths / 100 % 10 else null,
                tenths / 10 % 10,
                tenths % 10,
            )
            for ((i, digit) in cells.withIndex()) digit7(23f + i * 8.6f + (if (i == 3) 2.4f else 0f), 96.8f, 7.2f, 10f, digit)
            wash(circle(p(23f + 2 * 8.6f + 7.2f + 1.7f, 105.8f), 0.9f * u, 8), ink, 0.92f, 0f, 0f)
            label(tm, "km", TextStyle(fontSize = 6.sp, fontWeight = FontWeight.Bold, color = Vintage.inkSoft), p(72f, 103f))
        }
    }
}

/** The whole cluster: the step dial, the gap to the next card and the way-round dial. */
@Composable
internal fun InstrumentCluster(
    steps: Long, goal: Int, walkedKm: Double, nextKm: Double?, overall: Float, region: Float?, islandKm: Double, townKm: Double?,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        StepsDial(steps, goal, walkedKm, Modifier.weight(1.3f))
        GapDisplay(nextKm, Modifier.weight(0.9f))
        RouteDial(overall, region, islandKm, townKm, Modifier.weight(1.3f))
    }
}
