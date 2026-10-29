package com.starmist.walker.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
        stroke(listOf(at(cx, cy, r * (if (major) 0.80f else 0.88f), deg), at(cx, cy, r * 0.95f, deg)), ink, seed + 10 + i, width = line * (if (major) 1.1f else 0.7f))
    }
}

private fun Pen.needle(cx: Float, cy: Float, len: Float, deg: Float, color: Color, seed: Int) {
    val tip = at(cx, cy, len, deg)
    val l = at(cx, cy, len * 0.09f, deg + 90f); val r = at(cx, cy, len * 0.09f, deg - 90f)
    val tail = at(cx, cy, len * 0.18f, deg + 180f)
    val body = listOf(l, tip, r, tail)
    wash(body, color, 0.95f, 0.5f, 0.5f)
    inked(body, seed, closed = true, width = line * 0.9f)
    val hub = circle(p(cx, cy), 4.2f * u, 18)
    wash(hub, Color.White, 1f, 0f, 0f); inked(hub, seed + 3, closed = true, width = line)
    wash(circle(p(cx, cy), 1.6f * u, 10), ink, 1f, 0f, 0f)
}

private val needleSpring = spring<Float>(dampingRatio = 0.42f, stiffness = Spring.StiffnessVeryLow)

/** Big dial: the way to the next card. The needle climbs from the last card to the next; the green stretch at the end is "nearly there"; the arrow on the face points the way. */
@Composable
internal fun NextCardDial(fraction: Float, bearing: Float?, modifier: Modifier = Modifier) {
    val v by animateFloatAsState(fraction.coerceIn(0f, 1f), needleSpring, label = "next")
    val b by animateFloatAsState(bearing ?: 0f, needleSpring, label = "bearing")
    Canvas(modifier.aspectRatio(1f)) {
        val pen = Pen(this, size)
        with(pen) {
            val cx = 50f; val cy = 52f; val r = 46f; val start = 135f; val sweep = 270f
            face(cx, cy, r, start, sweep, 5, 200, whole = true)
            band(cx, cy, r * 0.62f, r * 0.74f, start, sweep, 0.80f, 1f, OK_GREEN, 230)
            band(cx, cy, r * 0.62f, r * 0.74f, start, sweep, 0f, v.coerceAtMost(0.8f), INK_RED, 231, alpha = 0.3f)
            // the finishing flag at the end of the scale
            val fp = at(cx, cy, r * 0.62f, start + sweep)
            val pole = listOf(fp, Offset(fp.x, fp.y - 11f * u))
            stroke(pole, ink, 232, width = line)
            val flag = listOf(pole[1], Offset(pole[1].x + 8f * u, pole[1].y + 2.5f * u), Offset(pole[1].x, pole[1].y + 5f * u))
            wash(flag, INK_RED, 0.9f, 0f, 0f); inked(flag, 233, closed = true, width = line * 0.7f)
            // the arrow to the next card, turning with the bearing
            if (bearing != null) {
                val c = Offset(cx, cy - 20f)
                fun rot(dx: Float, dy: Float): Offset {
                    val t = rad(b)
                    return p(c.x + dx * cos(t).toFloat() - dy * sin(t).toFloat(), c.y + dx * sin(t).toFloat() + dy * cos(t).toFloat())
                }
                val arrow = listOf(rot(0f, -11f), rot(7f, 8f), rot(0f, 4f), rot(-7f, 8f))
                wash(arrow, INK_RED, 0.8f, 0.5f, 0.5f); inked(arrow, 234, closed = true, width = line * 0.9f)
            }
            needle(cx, cy, r * 0.74f, start + sweep * v, INK_RED, 240)
        }
    }
}

/** Big dial: the whole island. The needle is how far round you are, the little island shows the route, a blue tick marks every card on the scale, and a counter reads the kilometres. */
@Composable
internal fun IslandDial(shape: IslandShape, cardFractions: List<Float>, fraction: Float, walkedKm: Double, modifier: Modifier = Modifier) {
    val v by animateFloatAsState(fraction.coerceIn(0f, 1f), needleSpring, label = "island")
    val tm = rememberTextMeasurer()
    Canvas(modifier.aspectRatio(1f)) {
        val pen = Pen(this, size)
        with(pen) {
            val cx = 50f; val cy = 52f; val r = 46f; val start = 135f; val sweep = 270f
            face(cx, cy, r, start, sweep, 4, 300, whole = true)
            band(cx, cy, r * 0.84f, r * 0.93f, start, sweep, 0f, v, COLD_BLUE, 330, alpha = 0.45f)
            // a small dot on the scale for every card; the ones already collected are filled
            for (f in cardFractions) {
                val c = at(cx, cy, r * 0.70f, start + sweep * f)
                val dot = circle(c, 1.6f * u, 8)
                if (f <= v + 0.001f) wash(dot, COLD_BLUE, 1f, 0f, 0f) else wash(dot, ink, 0.25f, 0f, 0f)
            }
            // the island, with the walked part inked in blue
            fun map(g: Pair<Float, Float>) = p(31f + g.first * 0.38f, 24f + g.second * 0.30f)
            val all = shape.xy.map(::map)
            val split = shape.km.indexOfLast { it <= walkedKm + 1e-9 }.coerceAtLeast(0)
            if (all.size - split > 1) inked(all.drop(split), 301, width = line * 0.7f, dash = dashed(this, 2.5f, 2.5f))
            val walked = all.take(split + 1)
            if (walked.size > 1) { stroke(walked, COLD_BLUE, 302, width = line * 1.8f); inked(walked, 303, width = line * 0.7f) }
            // the odometer window
            val label = "%.1f".format(walkedKm)
            val style = TextStyle(fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White)
            val laid = tm.measure(label, style)
            val box = roundRect(30f, 78f, 70f, 90f, 2.5f)
            wash(box, ink, 0.92f, 0f, 0f); inked(box, 304, closed = true, width = line * 0.7f)
            drawText(laid, topLeft = Offset(p(50f, 84f).x - laid.size.width / 2f, p(50f, 84f).y - laid.size.height / 2f))
            needle(cx, cy, r * 0.80f, start + sweep * v, INK_RED, 310)
        }
    }
}

/** What a half dial measures, so that one drawing routine serves the fuel and the temperature gauge. */
internal enum class HalfKind { FUEL, BOUNDARY }

/** Half dial: the fuel gauge (today's steps, empty to full) or the temperature gauge (entered the township to about to leave it). */
@Composable
internal fun HalfDial(kind: HalfKind, fraction: Float, active: Boolean, modifier: Modifier = Modifier) {
    val v by animateFloatAsState(fraction.coerceIn(0f, 1f), needleSpring, label = "half")
    val tm = rememberTextMeasurer()
    Canvas(modifier.aspectRatio(1.7f)) {
        val pen = Pen(this, size)
        with(pen) {
            val cx = 50f; val cy = 88f; val r = 72f; val start = 180f; val sweep = 180f
            val seed = if (kind == HalfKind.FUEL) 400 else 500
            face(cx, cy, r, start, sweep, 4, seed, whole = false)
            if (kind == HalfKind.FUEL) {
                band(cx, cy, r * 0.62f, r * 0.74f, start, sweep, 0f, 0.15f, INK_RED, seed + 60)
                band(cx, cy, r * 0.62f, r * 0.74f, start, sweep, 0.15f, 0.6f, AMBER, seed + 61, alpha = 0.4f)
                band(cx, cy, r * 0.62f, r * 0.74f, start, sweep, 0.6f, 1f, OK_GREEN, seed + 62)
                // the pump
                val body = roundRect(42f, 48f, 58f, 76f, 2f)
                wash(body, Color(0xFF6B4A24), 0.35f, 0f, 0f); inked(body, seed + 70, closed = true, width = line * 0.9f)
                val win = roundRect(45f, 51f, 55f, 59f, 1f)
                wash(win, Color.White, 1f, 0f, 0f); inked(win, seed + 71, closed = true, width = line * 0.6f)
                inked(listOf(p(58f, 56f), p(64f, 56f), p(64f, 70f), p(68f, 70f)), seed + 72, width = line * 0.8f)
            } else {
                band(cx, cy, r * 0.62f, r * 0.74f, start, sweep, 0f, 0.3f, COLD_BLUE, seed + 60, alpha = 0.4f)
                band(cx, cy, r * 0.62f, r * 0.74f, start, sweep, 0.3f, 0.75f, OK_GREEN, seed + 61, alpha = 0.35f)
                band(cx, cy, r * 0.62f, r * 0.74f, start, sweep, 0.75f, 1f, INK_RED, seed + 62)
                // the township fence
                for ((i, x) in listOf(40f, 50f, 60f).withIndex()) {
                    val post = listOf(p(x - 2.5f, 76f), p(x - 2.5f, 56f), p(x, 52f), p(x + 2.5f, 56f), p(x + 2.5f, 76f))
                    wash(post, Color(0xFF6B4A24), 0.4f, 0f, 0f); inked(post, seed + 70 + i, closed = true, width = line * 0.7f)
                }
                for ((i, y) in listOf(61f, 69f).withIndex()) inked(listOf(p(36f, y), p(64f, y)), seed + 80 + i, width = line * 0.8f)
            }
            val style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Vintage.inkSoft)
            val (l, rr) = if (kind == HalfKind.FUEL) "E" to "F" else "入" to "出"
            for ((txt, x) in listOf(l to 8f, rr to 92f)) {
                val laid = tm.measure(txt, style)
                drawText(laid, topLeft = Offset(p(x, 94f).x - laid.size.width / 2f, p(x, 94f).y - laid.size.height / 2f))
            }
            needle(cx, cy, r * 0.76f, start + sweep * (if (active) v else 0f), INK_RED, seed + 90)
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

/** The whole cluster: two big dials with the two half dials stacked between them. */
@Composable
internal fun InstrumentCluster(
    nextFraction: Float, bearing: Float?, island: IslandShape, cardFractions: List<Float>, overall: Float, walkedKm: Double,
    fuel: Float, border: Float?, modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        NextCardDial(nextFraction, bearing, Modifier.weight(1.25f))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            HalfDial(HalfKind.FUEL, fuel, true, Modifier.fillMaxWidth())
            HalfDial(HalfKind.BOUNDARY, border ?: 0f, border != null, Modifier.fillMaxWidth())
        }
        IslandDial(island, cardFractions, overall, walkedKm, Modifier.weight(1.25f))
    }
}
