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
internal class Pen(val d: DrawScope, size: Size) {
    val u = size.minDimension / 100f
    private val ox = (size.width - size.minDimension) / 2f
    private val oy = (size.height - size.minDimension) / 2f
    val ink = Vintage.ink
    val line = with(d) { 1.3.dp.toPx() }
    private val amp = with(d) { 0.5.dp.toPx() }
    private val step = with(d) { 6.dp.toPx() }

    fun p(x: Float, y: Float) = Offset(ox + x * u, oy + y * u)

    /** [pts] cut into short pieces, each nudged a little sideways, like a line drawn by hand. */
    private fun wobble(pts: List<Offset>, seed: Int, closed: Boolean, calm: Float): List<Offset> {
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
                val j = (rnd.nextFloat() - 0.5f) * 2f * amp * calm
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

    fun stroke(pts: List<Offset>, color: Color, seed: Int, width: Float = line, closed: Boolean = false, alpha: Float = 1f, dash: PathEffect? = null, calm: Float = 1f) {
        if (pts.size < 2) return
        val w = wobble(pts, seed, closed, calm)
        d.drawPath(pathOf(w, closed), color.copy(alpha = color.alpha * alpha), style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = dash))
    }

    /** An outline, drawn again a little thinner and paler, the way a pen goes over it twice. */
    fun inked(pts: List<Offset>, seed: Int, closed: Boolean = false, width: Float = line, dash: PathEffect? = null, calm: Float = 1f) {
        stroke(pts, ink, seed, width, closed, 1f, dash, calm)
        stroke(pts, ink, seed + 101, width * 0.55f, closed, 0.5f, dash, calm)
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

internal fun dashed(pen: Pen, on: Float, off: Float) = PathEffect.dashPathEffect(floatArrayOf(on * pen.u, off * pen.u))

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
