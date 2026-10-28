package com.starmist.walker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.starmist.core.route.GeoPoint
import com.starmist.core.route.RouteDef
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/*
 * The four instruments of the home panel, each a different kind of gauge so that what it measures can be seen at a glance:
 * a fuel tank for today's steps, a compass for the way to the next card, a spirit level whose bubble drifts to the township
 * boundary, and a little map of the island with the walked part of the route inked in. All are drawn like pen sketches:
 * every line is nudged a little and gone over twice, the colour is a wash that does not quite fit the ink, shading is hatching.
 */

/** A pen working in a 100 x 100 square in the middle of the canvas. */
private class Pen(val d: DrawScope, size: Size) {
    val u = size.minDimension / 100f
    private val ox = (size.width - size.minDimension) / 2f
    private val oy = (size.height - size.minDimension) / 2f
    val ink = Vintage.ink
    val line = with(d) { 1.3.dp.toPx() }
    private val amp = with(d) { 0.5.dp.toPx() }
    private val step = with(d) { 6.dp.toPx() }

    fun p(x: Float, y: Float) = Offset(ox + x * u, oy + y * u)

    /** [pts] cut into short pieces, each nudged a little sideways, like a line drawn by hand. */
    private fun wobble(pts: List<Offset>, seed: Int, closed: Boolean): List<Offset> {
        val rnd = Random(seed)
        val out = ArrayList<Offset>()
        val n = pts.size
        val last = if (closed) n else n - 1
        for (i in 0 until last) {
            val a = pts[i]; val b = pts[(i + 1) % n]
            val len = (b - a).getDistance()
            val steps = maxOf(1, (len / step).toInt())
            val nx = if (len > 0f) -(b.y - a.y) / len else 0f
            val ny = if (len > 0f) (b.x - a.x) / len else 0f
            for (k in 0 until steps) {
                val t = k / steps.toFloat()
                val j = (rnd.nextFloat() - 0.5f) * 2f * amp
                out += Offset(a.x + (b.x - a.x) * t + nx * j, a.y + (b.y - a.y) * t + ny * j)
            }
        }
        if (!closed) out += pts.last() else out += out.first()
        return out
    }

    private fun pathOf(pts: List<Offset>, closed: Boolean): Path = Path().apply {
        moveTo(pts[0].x, pts[0].y)
        for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
        if (closed) close()
    }

    fun stroke(pts: List<Offset>, color: Color, seed: Int, width: Float = line, closed: Boolean = false, alpha: Float = 1f, dash: PathEffect? = null) {
        if (pts.size < 2) return
        val w = wobble(pts, seed, closed)
        d.drawPath(pathOf(w, closed), color.copy(alpha = color.alpha * alpha), style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = dash))
    }

    /** An outline, drawn again a little thinner and paler, the way a pen goes over it twice. */
    fun inked(pts: List<Offset>, seed: Int, closed: Boolean = false, width: Float = line, dash: PathEffect? = null) {
        stroke(pts, ink, seed, width, closed, 1f, dash)
        stroke(pts, ink, seed + 101, width * 0.55f, closed, 0.5f, dash)
    }

    /** A flat wash of colour, set a little off the outline. */
    fun wash(pts: List<Offset>, color: Color, alpha: Float, dx: Float = 1.2f, dy: Float = 1f) {
        val path = pathOf(pts.map { Offset(it.x + dx * u / 4f, it.y + dy * u / 4f) }, true)
        d.drawPath(path, color.copy(alpha = alpha))
    }

    /** Parallel pen strokes inside [area]: shading. [from] and [to] are the x range, [top] and [bottom] the y range, in units. */
    fun hatch(area: List<Offset>, color: Color, from: Float, to: Float, top: Float, bottom: Float, gap: Float = 3.2f, lean: Float = 6f, alpha: Float = 0.55f) {
        val clip = pathOf(area, true)
        d.clipPath(clip) {
            var x = from - lean
            var k = 0
            while (x < to) {
                val jit = ((k * 7919) % 5 - 2) * 0.12f
                d.drawLine(color.copy(alpha = alpha), p(x + jit, bottom), p(x + lean + jit, top), strokeWidth = with(d) { 0.8.dp.toPx() }, cap = StrokeCap.Round)
                x += gap; k++
            }
        }
    }

    fun clipTo(area: List<Offset>, block: () -> Unit) { d.clipPath(pathOf(area, true)) { block() } }

    fun circle(c: Offset, r: Float, n: Int = 30) = (0 until n).map { val a = 2.0 * PI * it / n; Offset(c.x + (cos(a) * r).toFloat(), c.y + (sin(a) * r).toFloat()) }

    fun roundRect(l: Float, t: Float, r: Float, b: Float, rad: Float): List<Offset> {
        val out = ArrayList<Offset>()
        fun corner(cx: Float, cy: Float, from: Double) { for (i in 0..5) { val a = from + PI / 2 * i / 5; out += p(cx + (cos(a) * rad).toFloat(), cy + (sin(a) * rad).toFloat()) } }
        corner(r - rad, t + rad, -PI / 2); corner(r - rad, b - rad, 0.0); corner(l + rad, b - rad, PI / 2); corner(l + rad, t + rad, PI)
        return out
    }
}

private fun dashed(pen: Pen, on: Float, off: Float) = PathEffect.dashPathEffect(floatArrayOf(on * pen.u, off * pen.u))

/** Today's steps as a fuel tank filling up to the goal flag. */
@Composable
internal fun TankGauge(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().aspectRatio(1f)) {
        val pen = Pen(this, size)
        with(pen) {
            val f = fraction.coerceIn(0f, 1f)
            val body = roundRect(30f, 16f, 70f, 90f, 9f)
            val bottom = 88f; val top = 19f
            val y = bottom - (bottom - top) * f
            val surface = (0..8).map { p(30f + it * 5f, y + (if (it % 2 == 0) 1.2f else -1.2f)) }
            val liquid = surface + listOf(p(70f, 92f), p(30f, 92f))
            if (f > 0.005f) clipTo(body) {
                wash(liquid, color, 0.5f)
                hatch(liquid, color, 28f, 72f, y, 92f, gap = 3.6f, lean = 5f, alpha = 0.7f)
            }
            inked(body, 11)
            inked(listOf(p(41f, 16f), p(41f, 9f), p(59f, 9f), p(59f, 16f)), 12)                     // the filler neck
            if (f > 0.005f) stroke(surface, ink, 13, width = line * 0.8f)
            for (k in 0..4) {                                                                       // scale on the right
                val yy = bottom - (bottom - top) * k / 4f
                stroke(listOf(p(74f, yy), p(if (k == 0 || k == 4) 83f else 79f, yy)), ink, 20 + k, width = line * 0.9f)
            }
            val flag = listOf(p(74f, top - 5f), p(90f, top - 1f), p(74f, top + 3f))                 // the goal flag
            wash(flag, Color(0xFFB3202A), 0.6f); inked(flag, 30, closed = true, width = line * 0.8f)
            if (f >= 0.999f) {                                                                      // goal reached: a sparkle over the tank
                for (a in listOf(0.0, PI / 2, PI / 4, 3 * PI / 4)) {
                    val r = if (a % (PI / 2) == 0.0) 7f else 4f
                    stroke(listOf(p(50f - (cos(a) * r).toFloat(), 4f - (sin(a) * r).toFloat()), p(50f + (cos(a) * r).toFloat(), 4f + (sin(a) * r).toFloat())), Color(0xFFB8892D), 40 + (a * 10).toInt(), width = line)
                }
            }
        }
    }
}

/** The way to the next card as a compass: the needle points at it ([bearing] in degrees from north, null when there is none). */
@Composable
internal fun CompassGauge(bearing: Float?, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().aspectRatio(1f)) {
        val pen = Pen(this, size)
        with(pen) {
            val c = p(50f, 52f)
            val ring = circle(c, 38f * u)
            wash(circle(c, 38f * u), Color(0xFFFFF6E0), 0.6f)
            inked(ring, 1); inked(circle(c, 33f * u, 26), 2, width = line * 0.7f)
            for (i in 0 until 16) {                                                                 // compass ticks
                val a = 2.0 * PI * i / 16
                val len = (if (i % 4 == 0) 8f else 4f) * u
                val dir = Offset(sin(a).toFloat(), -cos(a).toFloat())
                stroke(listOf(c + dir * (33f * u), c + dir * (33f * u - len)), ink, 50 + i, width = line * 0.8f)
            }
            val north = listOf(p(50f, 10f), p(46f, 19f), p(54f, 19f))                               // north mark
            wash(north, ink, 0.75f, 0f, 0f); inked(north, 3, closed = true, width = line * 0.7f)
            if (bearing != null) {
                val a = Math.toRadians(bearing.toDouble())
                val dir = Offset(sin(a).toFloat(), -cos(a).toFloat())
                val side = Offset(-dir.y, dir.x)
                val tip = c + dir * (29f * u); val tail = c - dir * (17f * u)
                val head = listOf(tip, c + side * (6f * u), c - side * (6f * u))
                val back = listOf(tail, c + side * (6f * u), c - side * (6f * u))
                wash(back, Vintage.inkSoft, 0.35f); hatch(back, ink, 0f, 100f, 0f, 100f, gap = 3f, alpha = 0.35f)
                inked(back, 4, closed = true, width = line * 0.8f)
                wash(head, color, 0.85f); inked(head, 5, closed = true)
            }
            val hub = circle(c, 3f * u, 12)
            wash(hub, Color.White, 1f, 0f, 0f); inked(hub, 6, closed = true, width = line * 0.8f)
        }
    }
}

/** The way to the township boundary as a spirit level: the bubble drifts along the tube to the fence at the right end. */
@Composable
internal fun LevelGauge(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().aspectRatio(1f)) {
        val pen = Pen(this, size)
        with(pen) {
            val f = fraction.coerceIn(0f, 1f)
            val tube = roundRect(5f, 38f, 82f, 64f, 12f)
            wash(tube, Color(0xFFDDEBD2), 0.7f)
            val cx = 22f + f * (63f - 22f)                                                          // bubble centre
            clipTo(tube) {
                val trail = listOf(p(5f, 38f), p(cx, 38f), p(cx, 64f), p(5f, 64f))
                wash(trail, color, 0.28f, 0f, 0f)
                hatch(trail, color, 0f, cx, 38f, 64f, gap = 3.4f, lean = 4f, alpha = 0.45f)
            }
            inked(tube, 21)
            for (k in 0..4) {                                                                       // marks on the glass
                val x = 22f + (63f - 22f) * k / 4f
                stroke(listOf(p(x, 38f), p(x, if (k % 2 == 0) 44f else 42f)), ink, 60 + k, width = line * 0.7f)
                stroke(listOf(p(x, 64f), p(x, if (k % 2 == 0) 58f else 60f)), ink, 70 + k, width = line * 0.7f)
            }
            val bubble = circle(p(cx, 51f), 9f * u, 22)
            wash(bubble, color, 0.55f, 0.5f, 0.5f); inked(bubble, 22, closed = true)
            stroke(listOf(p(cx - 4f, 47f), p(cx - 2f, 45f), p(cx + 1f, 44.5f)), Color.White, 23, width = line * 1.1f)   // the glint
            for (i in 0 until 3) {                                                                  // the picket fence at the boundary
                val x = 88f + i * 4.5f
                val picket = listOf(p(x - 1.6f, 70f), p(x - 1.6f, 34f), p(x, 30f), p(x + 1.6f, 34f), p(x + 1.6f, 70f))
                wash(picket, Color(0xFFB98F52), 0.5f, 0.6f, 0.4f); inked(picket, 24 + i, closed = true, width = line * 0.75f)
            }
            stroke(listOf(p(86f, 44f), p(100f, 44f)), ink, 28, width = line * 0.7f)
            stroke(listOf(p(86f, 58f), p(100f, 58f)), ink, 29, width = line * 0.7f)
            val flag = listOf(p(8f, 30f), p(8f, 18f), p(20f, 22f), p(8f, 26f))                      // the flag where the township began
            wash(flag, color, 0.7f); inked(flag, 25, closed = true, width = line * 0.75f)
            stroke(listOf(p(8f, 30f), p(8f, 38f)), ink, 26, width = line * 0.9f)
        }
    }
}

/** The whole route as a little map of the island in the 100 x 100 square: [xy] for every stop, [km] the distance at each, [map] for any point. */
internal class IslandShape(val xy: List<Pair<Float, Float>>, val km: List<Double>, private val minLng: Double, private val maxLat: Double, private val cosLat: Double, private val k: Double, private val ox: Double, private val oy: Double) {
    fun map(g: GeoPoint): Pair<Float, Float> = Pair((ox + (g.lng - minLng) * cosLat * k).toFloat(), (oy + (maxLat - g.lat) * k).toFloat())
}

internal fun islandShape(route: RouteDef): IslandShape {
    val pts = route.stops.map { it.at }
    val minLat = pts.minOf { it.lat }; val maxLat = pts.maxOf { it.lat }
    val minLng = pts.minOf { it.lng }; val maxLng = pts.maxOf { it.lng }
    // a degree of longitude is shorter than a degree of latitude: squeeze it so the island keeps its shape
    val cosLat = cos(Math.toRadians((minLat + maxLat) / 2))
    val w = (maxLng - minLng) * cosLat; val h = maxLat - minLat
    val k = minOf(70.0 / w.coerceAtLeast(1e-6), 84.0 / h.coerceAtLeast(1e-6))
    val ox = (100.0 - w * k) / 2; val oy = (100.0 - h * k) / 2
    return IslandShape(
        pts.map { ((ox + (it.lng - minLng) * cosLat * k).toFloat()) to ((oy + (maxLat - it.lat) * k).toFloat()) },
        route.cumulativeKm, minLng, maxLat, cosLat, k, ox, oy,
    )
}

@Composable
internal fun IslandGauge(shape: IslandShape, route: RouteDef, walkedKm: Double, color: Color, modifier: Modifier = Modifier) {
    val here = remember(route, (walkedKm * 5).toInt()) { route.positionAt(walkedKm) }
    Canvas(modifier.fillMaxWidth().aspectRatio(1f)) {
        val pen = Pen(this, size)
        with(pen) {
            val herePoint = shape.map(here).let { (px, py) -> p(px, py) }
            val all = shape.xy.map { p(it.first, it.second) }
            val split = shape.km.indexOfLast { it <= walkedKm + 1e-9 }.coerceAtLeast(0)
            val walked = all.take(split + 1) + herePoint
            val ahead = listOf(herePoint) + all.drop(split + 1)
            if (ahead.size > 1) inked(ahead, 31, width = line * 0.8f, dash = dashed(pen, 3f, 3f))
            if (walked.size > 1) {
                stroke(walked, color, 32, width = line * 4.2f, alpha = 0.28f)
                inked(walked, 33, width = line * 1.5f)
                stroke(walked, color, 34, width = line * 1.1f)
            }
            val start = all.first()
            stroke(listOf(start, Offset(start.x, start.y - 9f * u)), ink, 35, width = line * 0.9f)  // the start flag
            val flag = listOf(Offset(start.x, start.y - 9f * u), Offset(start.x + 6f * u, start.y - 7f * u), Offset(start.x, start.y - 5f * u))
            wash(flag, Color(0xFFB3202A), 0.7f, 0f, 0f); inked(flag, 36, closed = true, width = line * 0.7f)
            val dot = circle(herePoint, 3.6f * u, 16)
            wash(dot, Color.White, 1f, 0f, 0f); inked(dot, 37, closed = true, width = line)
            val core = circle(herePoint, 1.8f * u, 10)
            wash(core, color, 1f, 0f, 0f)
        }
    }
}
