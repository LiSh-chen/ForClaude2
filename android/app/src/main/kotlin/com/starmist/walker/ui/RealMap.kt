package com.starmist.walker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
internal class RealMapData(val island: List<Offset>, val waves: List<Offset>, val peaks: List<Pair<Offset, Float>>)

@Composable
internal fun rememberRealMapData(route: RouteDef): RealMapData = remember(route.id) {
    val island = TaiwanOutline.lonLat.map { it.unit() }
    val waves = ArrayList<Offset>()
    var gy = 0.02f
    var row = 0
    while (gy < 1f) {
        var gx = 0.03f + (row % 2) * 0.03f
        while (gx < 1f) {
            val jx = ((row * 31 + (gx * 100).toInt() * 17) % 7 - 3) * 0.004f
            val jy = ((row * 13 + (gx * 100).toInt() * 29) % 5 - 2) * 0.004f
            val p = Offset(gx + jx, gy + jy)
            if (!inside(p, island)) waves += p
            gx += 0.06f
        }
        gy += 0.028f
        row++
    }
    // A rough line along the central mountain range, only to place decorative peaks; nothing is named.
    val ridge = listOf(
        22.3 to 120.82, 22.65 to 120.78, 23.0 to 120.90, 23.47 to 120.96, 23.8 to 121.08,
        24.15 to 121.28, 24.4 to 121.27, 24.75 to 121.45, 25.0 to 121.60,
    )
    val peaks = ArrayList<Pair<Offset, Float>>()
    var n = 0
    for (i in 1 until ridge.size) {
        val (la, lo) = ridge[i - 1]
        val (lb, lp) = ridge[i]
        val steps = 7
        for (s in 0 until steps) {
            val t = s / steps.toDouble()
            val lat = la + (lb - la) * t
            val lon = lo + (lp - lo) * t
            for (k in 0..1) {
                val side = if ((n + k) % 2 == 0) -1 else 1
                val spread = 0.04 + ((n * 7 + k * 5) % 6) * 0.025
                val p = GeoPoint(lat + ((n * 3 + k) % 5 - 2) * 0.02, lon + side * spread).unit()
                if (inside(p, island)) peaks += p to (0.75f + ((n * 5 + k * 3) % 5) * 0.12f)
                n++
            }
        }
    }
    RealMapData(island, waves, peaks)
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
    val label = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Vintage.ink)
    val here = route.positionAt(walkedKm)

    // Sea: faint wave marks.
    val waveStroke = Stroke(1.dp.toPx(), cap = StrokeCap.Round)
    data.waves.forEach { u ->
        val o = u.scaled()
        val path = Path().apply {
            moveTo(o.x - 6.dp.toPx(), o.y)
            quadraticTo(o.x - 3.dp.toPx(), o.y - 3.dp.toPx(), o.x, o.y)
            quadraticTo(o.x + 3.dp.toPx(), o.y + 3.dp.toPx(), o.x + 6.dp.toPx(), o.y)
        }
        drawPath(path, Color(0xFF7FA3A6).copy(alpha = 0.55f), style = waveStroke)
    }

    // Land.
    val island = Path().apply {
        data.island.forEachIndexed { i, u ->
            val o = u.scaled()
            if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
        }
        close()
    }
    drawPath(island, Color(0xFF9DB7B7).copy(alpha = 0.55f), style = Stroke(7.dp.toPx()))
    drawPath(island, LAND_COLOR)
    drawPath(island, Vintage.inkSoft, style = Stroke(1.3.dp.toPx()))

    // Mountains along the middle of the island.
    data.peaks.forEach { (u, size) ->
        val o = u.scaled()
        val s = 7.dp.toPx() * size
        val range = Path().apply {
            moveTo(o.x - s * 1.3f, o.y)
            lineTo(o.x - s * 0.5f, o.y - s * 1.3f)
            lineTo(o.x + s * 0.1f, o.y - s * 0.3f)
            lineTo(o.x + s * 0.6f, o.y - s * 0.95f)
            lineTo(o.x + s * 1.4f, o.y)
            close()
        }
        drawPath(range, Vintage.mossLight.copy(alpha = 0.75f))
        drawPath(range, Vintage.inkSoft.copy(alpha = 0.6f), style = Stroke(0.9.dp.toPx()))
    }

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
        drawPoiGlyph(poi.kind, o, 9.dp.toPx(), alpha)
        val text = measurer.measure(poi.name, label.copy(fontSize = 10.sp, color = Vintage.ink.copy(alpha = alpha)))
        drawText(text, topLeft = Offset(o.x - text.size.width / 2f, o.y + 6.dp.toPx()))
    }

    // Cards and stamps.
    route.stops.forEachIndexed { i, stop ->
        val o = stop.at.px()
        val reached = route.cumulativeKm[i] <= walkedKm + 1e-9
        if (stop.landmark != null) {
            drawCircle(if (reached) Vintage.moss else Vintage.parchmentLight, 5.dp.toPx(), o)
            drawCircle(if (reached) Color.White else Vintage.moss, 5.dp.toPx(), o, style = Stroke(1.5.dp.toPx()))
        } else if (stop.supply != null) {
            val side = 8.dp.toPx()
            drawRect(if (reached) SUPPLY_COLOR else Vintage.parchmentLight, Offset(o.x - side / 2, o.y - side / 2), Size(side, side))
            drawRect(if (reached) Color.White else SUPPLY_COLOR, Offset(o.x - side / 2, o.y - side / 2), Size(side, side), style = Stroke(1.4.dp.toPx()))
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
        val height = 46.dp.toPx()
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

@Composable
internal fun rememberMapMeasurer(): TextMeasurer = rememberTextMeasurer()

