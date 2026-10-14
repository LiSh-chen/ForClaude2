package com.starmist.walker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starmist.core.route.GeoPoint
import com.starmist.core.route.POI_RANGE_KM
import com.starmist.core.route.PoiKind
import com.starmist.core.route.RouteDef
import com.starmist.core.route.TaiwanOutline
import kotlin.math.cos

internal const val MAP_LON_MIN = 119.95
internal const val MAP_LON_MAX = 122.1
internal const val MAP_LAT_MIN = 21.8
internal const val MAP_LAT_MAX = 25.4

/** Width over height of the island map. */
internal val REAL_MAP_ASPECT: Float =
    ((MAP_LON_MAX - MAP_LON_MIN) * cos(Math.toRadians(23.5)) / (MAP_LAT_MAX - MAP_LAT_MIN)).toFloat()

internal val SEA_COLOR = Color(0xFFCFE0E0)
private val LAND_COLOR = Color(0xFFE9DFBF)
private val STAMP_RED = Color(0xFFB04A3A)
private val SUPPLY_COLOR = Color(0xFFD9744B)

/** A point on the earth as a position on the unit square of the map. */
internal fun GeoPoint.unit() = Offset(
    ((lng - MAP_LON_MIN) / (MAP_LON_MAX - MAP_LON_MIN)).toFloat(),
    ((MAP_LAT_MAX - lat) / (MAP_LAT_MAX - MAP_LAT_MIN)).toFloat(),
)

/** What can be worked out once per route: the island shape and the little decorations. */
internal class RealMapData(
    val island: List<Offset>,
    /** Centres of soft tinted patches that suggest the mountain belt. */
    val relief: List<Pair<Offset, Float>>,
    /** Small hill sketches, with a size factor. */
    val hills: List<Pair<Offset, Float>>,
    /** Tiny trees and grass ticks scattered over the land. */
    val trees: List<Offset>,
    val ticks: List<Offset>,
    /** Fields on the western plain. */
    val fields: List<Offset>,
    val boats: List<Offset>,
)

@Composable
internal fun rememberRealMapData(route: RouteDef): RealMapData = remember(route.id) {
    val island = TaiwanOutline.lonLat.map { it.unit() }
    // A rough line along the central mountain range, only to place decoration; nothing is named.
    val ridge = listOf(
        22.3 to 120.82, 22.65 to 120.78, 23.0 to 120.90, 23.47 to 120.96, 23.8 to 121.08,
        24.15 to 121.28, 24.4 to 121.27, 24.75 to 121.45, 25.0 to 121.60,
    )
    val relief = ArrayList<Pair<Offset, Float>>()
    val hills = ArrayList<Pair<Offset, Float>>()
    var n = 0
    for (i in 1 until ridge.size) {
        val (la, lo) = ridge[i - 1]
        val (lb, lp) = ridge[i]
        val steps = 10
        for (s in 0 until steps) {
            val t = s / steps.toDouble()
            val lat = la + (lb - la) * t
            val lon = lo + (lp - lo) * t
            relief += GeoPoint(lat, lon).unit() to (0.9f + (n % 3) * 0.15f)
            for (k in 0..2) {
                val spread = ((n * 7 + k * 11) % 9 - 4) * 0.035
                val p = GeoPoint(lat + ((n * 3 + k * 7) % 7 - 3) * 0.018, lon + spread).unit()
                if (inside(p, island)) hills += p to (0.7f + ((n * 5 + k * 3) % 5) * 0.1f)
                n++
            }
        }
    }
    // Scatter small trees and grass ticks on a jittered grid, thinner on the high ground.
    val trees = ArrayList<Offset>()
    val ticks = ArrayList<Offset>()
    val fields = ArrayList<Offset>()
    var gy = 0.2f
    var row = 0
    while (gy < 1f) {
        var gx = 0.05f + (row % 2) * 0.012f
        while (gx < 0.95f) {
            val jx = ((row * 31 + (gx * 997).toInt() * 17) % 9 - 4) * 0.0025f
            val jy = ((row * 13 + (gx * 991).toInt() * 29) % 7 - 3) * 0.0025f
            val p = Offset(gx + jx, gy + jy)
            if (inside(p, island)) {
                val h = (row * 7 + (gx * 313).toInt() * 5) % 10
                val west = p.x < ridgeX(p.y)
                when {
                    h < 2 -> trees += p
                    h < 6 -> ticks += p
                    west && h < 8 -> fields += p
                }
            }
            gx += 0.013f
        }
        gy += 0.0085f
        row++
    }
    val boats = listOf(Offset(0.12f, 0.35f), Offset(0.9f, 0.62f), Offset(0.2f, 0.78f), Offset(0.88f, 0.12f), Offset(0.93f, 0.9f))
    RealMapData(island, relief, hills, trees, ticks, fields, boats)
}

/** The x position of the central range at height [y] on the unit square (decoration only). */
private fun ridgeX(y: Float): Float {
    val lat = MAP_LAT_MAX - y * (MAP_LAT_MAX - MAP_LAT_MIN)
    val lon = when {
        lat > 25.0 -> 121.6
        lat > 24.4 -> 121.27 + (lat - 24.4) * 0.55
        lat > 23.47 -> 120.96 + (lat - 23.47) * 0.33
        else -> 120.8 + (lat - 22.3) * 0.14
    }
    return ((lon - MAP_LON_MIN) / (MAP_LON_MAX - MAP_LON_MIN)).toFloat()
}

private fun inside(p: Offset, poly: List<Offset>): Boolean {
    var result = false
    var j = poly.lastIndex
    for (i in poly.indices) {
        val a = poly[i]
        val b = poly[j]
        if ((a.y > p.y) != (b.y > p.y) && p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x) result = !result
        j = i
    }
    return result
}

/**
 * Draws the island walk in a [w] by [h] box: coastline, a few decorations, the route (walked part
 * solid), cards and stamps, small sights while the walker is near them, and the walker.
 */
internal fun DrawScope.drawRealMap(
    w: Float,
    h: Float,
    route: RouteDef,
    data: RealMapData,
    walkedKm: Double,
    measurer: TextMeasurer,
    pin: ImageBitmap?,
) {
    fun GeoPoint.px() = unit().let { Offset(it.x * w, it.y * h) }
    fun Offset.scaled() = Offset(x * w, y * h)
    val label = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Vintage.ink)
    val here = route.positionAt(walkedKm)

    // Sea: ink hatching like an old atlas, thicker near the coast.
    val island = Path().apply {
        data.island.forEachIndexed { i, u ->
            val o = u.scaled()
            if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
        }
        close()
    }
    clipPath(island, clipOp = ClipOp.Difference) {
        val gap = 7.dp.toPx()
        var y = 0f
        while (y < h) {
            drawLine(Color(0xFF6E9396).copy(alpha = 0.22f), Offset(0f, y), Offset(w, y), 0.8.dp.toPx())
            y += gap
        }
    }
    for ((width, alpha) in listOf(16f to 0.10f, 10f to 0.14f, 5f to 0.2f)) {
        drawPath(island, Color(0xFF6E9396).copy(alpha = alpha), style = Stroke(width.dp.toPx(), join = StrokeJoin.Round))
    }
    data.boats.forEach { u -> drawBoat(u.scaled(), 7.dp.toPx()) }
    drawCompassRose(Offset(w * 0.16f, h * 0.9f), 26.dp.toPx())

    // Land.
    drawPath(island, LAND_COLOR)
    clipPath(island) {
        // Soft tint over the mountain belt, and flat field colour on the western plain.
        data.relief.forEach { (u, s) ->
            val c = u.scaled()
            val r = w * 0.085f * s
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    listOf(Color(0xFF9DB08A).copy(alpha = 0.42f), Color.Transparent), center = c, radius = r,
                ),
                radius = r, center = c,
            )
        }
        data.fields.forEach { u ->
            val o = u.scaled()
            drawRect(Color(0xFFD9CF9C).copy(alpha = 0.55f), Offset(o.x - 3.dp.toPx(), o.y - 1.5.dp.toPx()), Size(6.dp.toPx(), 3.dp.toPx()))
        }
        data.ticks.forEach { u ->
            val o = u.scaled()
            drawLine(Vintage.inkSoft.copy(alpha = 0.35f), o, Offset(o.x - 1.5.dp.toPx(), o.y - 3.dp.toPx()), 0.8.dp.toPx())
            drawLine(Vintage.inkSoft.copy(alpha = 0.35f), o, Offset(o.x + 1.5.dp.toPx(), o.y - 3.dp.toPx()), 0.8.dp.toPx())
        }
        data.trees.forEach { u ->
            val o = u.scaled()
            drawCircle(Vintage.mossLight.copy(alpha = 0.8f), 2.6.dp.toPx(), Offset(o.x, o.y - 3.dp.toPx()))
            drawCircle(Vintage.inkSoft.copy(alpha = 0.55f), 2.6.dp.toPx(), Offset(o.x, o.y - 3.dp.toPx()), style = Stroke(0.7.dp.toPx()))
            drawLine(Vintage.inkSoft.copy(alpha = 0.55f), o, Offset(o.x, o.y - 0.5.dp.toPx()), 0.8.dp.toPx())
        }
        // Hills: a light wash, a pen curve and a few hatch strokes, not solid shapes.
        data.hills.forEach { (u, size) ->
            val o = u.scaled()
            val s = 6.dp.toPx() * size
            drawOval(Vintage.mossLight.copy(alpha = 0.4f), Offset(o.x - s * 1.2f, o.y - s * 0.7f), Size(s * 2.4f, s * 1.1f))
            val crest = Path().apply {
                moveTo(o.x - s * 1.1f, o.y + s * 0.1f)
                quadraticTo(o.x - s * 0.5f, o.y - s * 1.1f, o.x, o.y - s * 0.9f)
                quadraticTo(o.x + s * 0.6f, o.y - s * 0.8f, o.x + s * 1.1f, o.y + s * 0.1f)
            }
            drawPath(crest, Vintage.inkSoft.copy(alpha = 0.6f), style = Stroke(0.9.dp.toPx(), cap = StrokeCap.Round))
            for (k in 1..3) {
                val x = o.x + s * (0.1f + 0.22f * k)
                drawLine(Vintage.inkSoft.copy(alpha = 0.4f), Offset(x, o.y - s * (0.8f - 0.12f * k)), Offset(x + s * 0.12f, o.y - s * 0.05f), 0.7.dp.toPx())
            }
        }
    }
    drawPath(island, Vintage.inkSoft, style = Stroke(1.4.dp.toPx(), join = StrokeJoin.Round))

    // The route: dotted ahead, solid behind, dashed red where a train or ferry carries the walker.
    val dotted = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 5.dp.toPx()))
    val dashed = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx()))
    for (i in 1 until route.stops.size) {
        val a = route.stops[i - 1].at.px()
        val b = route.stops[i].at.px()
        val startKm = route.cumulativeKm[i - 1]
        val endKm = route.cumulativeKm[i]
        when {
            route.stops[i].transit != null ->
                drawLine(if (walkedKm >= endKm) STAMP_RED else STAMP_RED.copy(alpha = 0.35f), a, b, 2.4.dp.toPx(), pathEffect = dashed)
            walkedKm >= endKm -> drawLine(Vintage.moss, a, b, 3.dp.toPx(), cap = StrokeCap.Round)
            walkedKm <= startKm -> drawLine(Vintage.inkSoft.copy(alpha = 0.6f), a, b, 1.6.dp.toPx(), pathEffect = dotted)
            else -> {
                val mid = here.px()
                drawLine(Vintage.moss, a, mid, 3.dp.toPx(), cap = StrokeCap.Round)
                drawLine(Vintage.inkSoft.copy(alpha = 0.6f), mid, b, 1.6.dp.toPx(), pathEffect = dotted)
            }
        }
    }

    // Small sights: only while the walker is near, fading in and out.
    route.poisNear(here).forEach { (poi, km) ->
        val alpha = (1f - ((km - POI_RANGE_KM * 0.6) / (POI_RANGE_KM * 0.4)).toFloat().coerceIn(0f, 1f))
        val o = poi.at.px()
        drawPoiGlyph(poi.kind, o, 11.dp.toPx(), alpha)
        val text = measurer.measure(poi.name, label.copy(fontSize = 12.sp, color = Vintage.ink.copy(alpha = alpha)))
        drawText(text, topLeft = Offset(o.x - text.size.width / 2f, o.y + 10.dp.toPx()))
    }

    // Cards and stamps.
    route.stops.forEachIndexed { i, stop ->
        val o = stop.at.px()
        val reached = route.cumulativeKm[i] <= walkedKm + 1e-9
        if (stop.landmark != null) {
            drawCircle(if (reached) Vintage.moss else Vintage.parchmentLight, 7.dp.toPx(), o)
            drawCircle(if (reached) Color.White else Vintage.moss, 7.dp.toPx(), o, style = Stroke(2.dp.toPx()))
        } else if (stop.supply != null) {
            val side = 11.dp.toPx()
            drawRect(if (reached) SUPPLY_COLOR else Vintage.parchmentLight, Offset(o.x - side / 2, o.y - side / 2), Size(side, side))
            drawRect(if (reached) Color.White else SUPPLY_COLOR, Offset(o.x - side / 2, o.y - side / 2), Size(side, side), style = Stroke(1.8.dp.toPx()))
        }
    }
    val next = route.next(walkedKm)?.first
    val lastCard = route.collected(walkedKm).lastOrNull()
    listOfNotNull(next?.let { it to "下一張" }, lastCard?.let { it to "" }).forEach { (card, tag) ->
        val o = route.stops.first { it.landmark?.id == card.id }.at.px()
        val text = measurer.measure(card.name + if (tag.isNotEmpty()) "（$tag）" else "", label)
        val x = if (o.x + text.size.width + 12.dp.toPx() > w) o.x - text.size.width - 9.dp.toPx() else o.x + 9.dp.toPx()
        drawRoundRect(
            Vintage.parchmentLight.copy(alpha = 0.88f),
            Offset(x - 3.dp.toPx(), o.y - text.size.height / 2f - 1.dp.toPx()),
            Size(text.size.width + 6.dp.toPx(), text.size.height + 2.dp.toPx()),
            CornerRadius(3.dp.toPx()),
        )
        drawText(text, topLeft = Offset(x, o.y - text.size.height / 2f))
    }

    // The walker.
    val p = here.px()
    if (pin != null) {
        val height = 60.dp.toPx()
        val width = height * pin.width / pin.height
        drawOval(Color.Black.copy(alpha = 0.18f), Offset(p.x - width * 0.3f, p.y - 3.dp.toPx()), Size(width * 0.6f, 6.dp.toPx()))
        drawImage(
            pin,
            dstOffset = IntOffset((p.x - width / 2f).toInt(), (p.y - height + 2.dp.toPx()).toInt()),
            dstSize = IntSize(width.toInt(), height.toInt()),
        )
    } else {
        drawCircle(Vintage.brassLight, 8.dp.toPx(), p)
        drawCircle(Vintage.ink, 8.dp.toPx(), p, style = Stroke(1.6.dp.toPx()))
        drawCircle(Vintage.moss, 3.dp.toPx(), p)
    }
}

/** A tiny pen sketch for each kind of roadside sight. [u] is about half its size. */
private fun DrawScope.drawPoiGlyph(kind: PoiKind, at: Offset, u: Float, alpha: Float) {
    val ink = Vintage.ink.copy(alpha = alpha)
    val wash = Vintage.parchmentLight.copy(alpha = alpha)
    val green = Vintage.mossLight.copy(alpha = alpha)
    val line = 1.1.dp.toPx()
    drawCircle(Vintage.parchmentLight.copy(alpha = 0.75f * alpha), u * 1.35f, at)
    when (kind) {
        PoiKind.HARBOR -> {
            val hull = Path().apply {
                moveTo(at.x - u, at.y); lineTo(at.x + u, at.y); lineTo(at.x + u * 0.6f, at.y + u * 0.6f); lineTo(at.x - u * 0.6f, at.y + u * 0.6f); close()
            }
            drawPath(hull, wash); drawPath(hull, ink, style = Stroke(line))
            drawLine(ink, Offset(at.x, at.y), Offset(at.x, at.y - u * 1.1f), line)
            drawLine(ink, Offset(at.x, at.y - u * 1.1f), Offset(at.x + u * 0.7f, at.y - u * 0.2f), line)
        }
        PoiKind.TEMPLE -> {
            val roof = Path().apply { moveTo(at.x - u, at.y - u * 0.2f); quadraticTo(at.x, at.y - u * 1.3f, at.x + u, at.y - u * 0.2f) }
            drawPath(roof, ink, style = Stroke(line, cap = StrokeCap.Round))
            drawLine(ink, Offset(at.x - u * 0.6f, at.y - u * 0.2f), Offset(at.x - u * 0.6f, at.y + u * 0.8f), line)
            drawLine(ink, Offset(at.x + u * 0.6f, at.y - u * 0.2f), Offset(at.x + u * 0.6f, at.y + u * 0.8f), line)
        }
        PoiKind.MUSEUM -> {
            val top = Path().apply { moveTo(at.x - u, at.y - u * 0.2f); lineTo(at.x, at.y - u); lineTo(at.x + u, at.y - u * 0.2f); close() }
            drawPath(top, wash); drawPath(top, ink, style = Stroke(line))
            for (k in -1..1) drawLine(ink, Offset(at.x + k * u * 0.6f, at.y - u * 0.1f), Offset(at.x + k * u * 0.6f, at.y + u * 0.8f), line)
            drawLine(ink, Offset(at.x - u, at.y + u * 0.8f), Offset(at.x + u, at.y + u * 0.8f), line)
        }
        PoiKind.SPRING -> {
            drawOval(Vintage.sea.copy(alpha = 0.6f * alpha), Offset(at.x - u, at.y + u * 0.1f), Size(u * 2f, u * 0.9f))
            drawOval(ink, Offset(at.x - u, at.y + u * 0.1f), Size(u * 2f, u * 0.9f), style = Stroke(line))
            for (k in -1..1) {
                val steam = Path().apply {
                    moveTo(at.x + k * u * 0.5f, at.y); quadraticTo(at.x + k * u * 0.5f + u * 0.3f, at.y - u * 0.5f, at.x + k * u * 0.5f, at.y - u)
                }
                drawPath(steam, ink, style = Stroke(line * 0.8f, cap = StrokeCap.Round))
            }
        }
        PoiKind.WATERFALL -> {
            drawRect(green, Offset(at.x - u, at.y - u), Size(u * 2f, u * 0.5f))
            drawRect(ink, Offset(at.x - u, at.y - u), Size(u * 2f, u * 0.5f), style = Stroke(line))
            for (k in -1..1) drawLine(Vintage.sea.copy(alpha = alpha), Offset(at.x + k * u * 0.4f, at.y - u * 0.5f), Offset(at.x + k * u * 0.4f, at.y + u * 0.9f), line * 1.4f)
        }
        PoiKind.PARK -> {
            drawLine(ink, Offset(at.x, at.y + u * 0.8f), Offset(at.x, at.y), line * 1.3f)
            drawCircle(green, u * 0.9f, Offset(at.x, at.y - u * 0.4f))
            drawCircle(ink, u * 0.9f, Offset(at.x, at.y - u * 0.4f), style = Stroke(line))
        }
        PoiKind.THEATER -> {
            val arch = Path().apply { moveTo(at.x - u, at.y + u * 0.8f); lineTo(at.x - u, at.y - u * 0.1f); quadraticTo(at.x, at.y - u * 1.4f, at.x + u, at.y - u * 0.1f); lineTo(at.x + u, at.y + u * 0.8f) }
            drawPath(arch, ink, style = Stroke(line, cap = StrokeCap.Round))
            drawLine(ink, Offset(at.x, at.y - u * 0.5f), Offset(at.x, at.y + u * 0.8f), line)
        }
        PoiKind.FORT -> {
            drawRect(wash, Offset(at.x - u, at.y - u * 0.3f), Size(u * 2f, u * 1.1f))
            drawRect(ink, Offset(at.x - u, at.y - u * 0.3f), Size(u * 2f, u * 1.1f), style = Stroke(line))
            for (k in -1..1) drawRect(ink, Offset(at.x + k * u * 0.65f - u * 0.18f, at.y - u * 0.7f), Size(u * 0.36f, u * 0.4f))
        }
        PoiKind.LIGHTHOUSE -> {
            val tower = Path().apply { moveTo(at.x - u * 0.5f, at.y + u * 0.9f); lineTo(at.x - u * 0.3f, at.y - u * 0.6f); lineTo(at.x + u * 0.3f, at.y - u * 0.6f); lineTo(at.x + u * 0.5f, at.y + u * 0.9f); close() }
            drawPath(tower, wash); drawPath(tower, ink, style = Stroke(line))
            drawCircle(Vintage.brass.copy(alpha = alpha), u * 0.35f, Offset(at.x, at.y - u * 0.95f))
        }
    }
}

/** A small sailing boat, drawn in pen. */
private fun DrawScope.drawBoat(at: Offset, u: Float) {
    val ink = Vintage.inkSoft.copy(alpha = 0.8f)
    val hull = Path().apply { moveTo(at.x - u, at.y); lineTo(at.x + u, at.y); lineTo(at.x + u * 0.6f, at.y + u * 0.5f); lineTo(at.x - u * 0.6f, at.y + u * 0.5f); close() }
    drawPath(hull, Vintage.parchmentLight)
    drawPath(hull, ink, style = Stroke(1.dp.toPx()))
    drawLine(ink, Offset(at.x, at.y), Offset(at.x, at.y - u * 1.6f), 1.dp.toPx())
    val sail = Path().apply { moveTo(at.x + u * 0.1f, at.y - u * 1.5f); lineTo(at.x + u * 0.9f, at.y - u * 0.2f); lineTo(at.x + u * 0.1f, at.y - u * 0.2f); close() }
    drawPath(sail, Vintage.parchmentLight)
    drawPath(sail, ink, style = Stroke(0.9.dp.toPx()))
}

/** A small compass rose with a north mark. */
private fun DrawScope.drawCompassRose(c: Offset, r: Float) {
    val ink = Vintage.inkSoft.copy(alpha = 0.85f)
    drawCircle(ink, r, c, style = Stroke(1.dp.toPx()))
    drawCircle(ink.copy(alpha = 0.5f), r * 0.78f, c, style = Stroke(0.7.dp.toPx()))
    val star = Path().apply {
        moveTo(c.x, c.y - r * 1.25f); lineTo(c.x + r * 0.18f, c.y - r * 0.18f); lineTo(c.x + r * 1.25f, c.y)
        lineTo(c.x + r * 0.18f, c.y + r * 0.18f); lineTo(c.x, c.y + r * 1.25f); lineTo(c.x - r * 0.18f, c.y + r * 0.18f)
        lineTo(c.x - r * 1.25f, c.y); lineTo(c.x - r * 0.18f, c.y - r * 0.18f); close()
    }
    drawPath(star, Vintage.brassLight.copy(alpha = 0.85f))
    drawPath(star, ink, style = Stroke(0.9.dp.toPx()))
    drawLine(ink, Offset(c.x, c.y - r * 1.25f), Offset(c.x, c.y - r * 1.6f), 1.2.dp.toPx())
}

@Composable
internal fun rememberMapMeasurer(): TextMeasurer = rememberTextMeasurer()

