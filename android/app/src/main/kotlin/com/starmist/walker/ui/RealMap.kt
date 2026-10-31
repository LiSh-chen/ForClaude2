package com.starmist.walker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
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
import com.starmist.core.route.RouteDef
import com.starmist.core.route.TaiwanOutline
import kotlin.math.cos

internal const val MAP_LON_MIN = 119.93
internal const val MAP_LON_MAX = 122.12
internal const val MAP_LAT_MIN = 21.8
internal const val MAP_LAT_MAX = 25.4

/** Width over height of the island map. */
internal val REAL_MAP_ASPECT: Float =
    ((MAP_LON_MAX - MAP_LON_MIN) * cos(Math.toRadians(23.5)) / (MAP_LAT_MAX - MAP_LAT_MIN)).toFloat()

internal val SEA_COLOR = Color(0xFFE4DAC6)
private val LAND_COLOR = Color(0xFFE9DFBF)
private val SUPPLY_COLOR = Color(0xFFD9744B)

/** A point on the earth as a position on the unit square of the map. */
internal fun GeoPoint.unit() = Offset(
    ((lng - MAP_LON_MIN) / (MAP_LON_MAX - MAP_LON_MIN)).toFloat(),
    ((MAP_LAT_MAX - lat) / (MAP_LAT_MAX - MAP_LAT_MIN)).toFloat(),
)

/** Rivers as (lat, lon) courses from the mountains to the sea; drawn small and unnamed, decoration only. */
private val RIVERS: List<List<Pair<Double, Double>>> = listOf(
    listOf(23.95 to 121.15, 23.87 to 120.9, 23.84 to 120.6, 23.82 to 120.28),   // 濁水溪
    listOf(24.25 to 121.3, 24.25 to 121.0, 24.3 to 120.75, 24.35 to 120.6),     // 大甲溪
    listOf(24.7 to 121.35, 24.9 to 121.4, 25.05 to 121.45, 25.17 to 121.42),    // 淡水河
    listOf(23.3 to 120.8, 23.2 to 120.5, 23.05 to 120.07),                      // 曾文溪
    listOf(23.25 to 120.9, 22.95 to 120.65, 22.5 to 120.4),                     // 高屏溪
    listOf(23.45 to 121.38, 23.7 to 121.43, 23.92 to 121.62),                   // 花蓮溪
    listOf(23.1 to 121.2, 23.3 to 121.3, 23.47 to 121.52),                      // 秀姑巒溪
    listOf(24.5 to 121.5, 24.65 to 121.7, 24.7 to 121.83),                      // 蘭陽溪
    listOf(23.05 to 121.1, 22.85 to 121.15, 22.78 to 121.2),                    // 卑南溪
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
 * What the map shows: the names of places (of the cards and stations while the route is shown, of the townships while the regions are), the route (its line,
 * cards, stations and small sights), and the outlines of the townships walked through.
 */
data class MapLayers(val spots: Boolean = true, val route: Boolean = true, val regions: Boolean = false)

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
    base: ImageBitmap? = null,
    tiles: MapTiles? = null,
    view: Rect = Rect(0f, 0f, w, h),
    areas: TownAreas? = null,
    explored: Set<Int> = emptySet(),
    currentTown: Int = -1,
    layers: MapLayers = MapLayers(),
    selectedTown: Int = -1,
    /** The small sight whose name was asked for by tapping its dot. */
    selectedPoi: String? = null,
    /** The small sights not hidden in the fog (null: all). */
    visiblePois: Set<String>? = null,
    /** The small sights whose stamp is earned. */
    earnedPois: Set<String> = emptySet(),
) {
    val regionMode = layers.regions
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
    if (base != null) {
        drawImage(base, dstSize = IntSize(w.toInt(), h.toInt()))
        // Detail: the sharp tiles over the part on screen (only once the low-resolution picture would look soft).
        if (tiles != null && w > 2200f) {
            val sx = w / MAP_FULL_W; val sy = h / MAP_FULL_H
            for (id in tiles.visible(view, w, h)) {
                val bmp = tiles.get(id) ?: continue
                val x0 = (id % MAP_TILE_COLS) * MAP_TILE; val y0 = (id / MAP_TILE_COLS) * MAP_TILE
                val left = kotlin.math.floor(x0 * sx).toInt(); val top = kotlin.math.floor(y0 * sy).toInt()
                val right = kotlin.math.ceil((x0 + bmp.width) * sx).toInt(); val bottom = kotlin.math.ceil((y0 + bmp.height) * sy).toInt()
                drawImage(bmp, dstOffset = IntOffset(left, top), dstSize = IntSize(right - left, bottom - top))
            }
        }
    } else {
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
        // Rivers: thin blue lines that wander from the hills to the sea.
        clipPath(island) {
            RIVERS.forEach { course ->
                val pts = course.map { GeoPoint(it.first, it.second).px() }
                val river = Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    for (i in 1 until pts.size) {
                        val a = pts[i - 1]; val b = pts[i]
                        val mx = (a.x + b.x) / 2f; val my = (a.y + b.y) / 2f
                        val bend = (if (i % 2 == 0) 1 else -1) * (b - a).getDistance() * 0.12f
                        quadraticTo(mx - (b.y - a.y) / (b - a).getDistance().coerceAtLeast(1f) * bend, my + (b.x - a.x) / (b - a).getDistance().coerceAtLeast(1f) * bend, b.x, b.y)
                    }
                }
                drawPath(river, Color(0xFF6E9396).copy(alpha = 0.9f), style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(river, Color(0xFFBBD6D6), style = Stroke(1.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        drawPath(island, Vintage.inkSoft, style = Stroke(1.4.dp.toPx(), join = StrokeJoin.Round))


    }

    // Mist over everything not walked yet: only the townships the route has passed through are clear.
    // A flat veil, soft drifting banks of mist on it, and mist creeping a little way into the cleared area along its edge.
    if (areas != null && explored.isNotEmpty()) {
        val fog = areas.fog(explored, w, h)
        drawPath(fog, MIST_VEIL.copy(alpha = 0.84f))
        clipPath(fog) {
            val pad = w * 0.09f
            val seen = Rect(view.left - pad, view.top - pad, view.right + pad, view.bottom + pad)
            for (b in (if (w > 3000f) MIST_BANKS + MIST_WISPS else MIST_BANKS)) {
                val c = Offset(b.u * w, b.v * h)
                if (c.x !in seen.left..seen.right || c.y !in seen.top..seen.bottom) continue
                val r = b.r * w
                withTransform({ scale(b.stretch, 1f, c) }) {
                    drawCircle(Brush.radialGradient(listOf(b.color.copy(alpha = b.alpha), Color.Transparent), center = c, radius = r), radius = r, center = c)
                }
            }
        }
        val edge = areas.fogEdge(explored, w, h)
        val k = (w / 3000f).coerceIn(0.35f, 2.5f)
        for ((width, alpha) in listOf(44f to 0.07f, 28f to 0.10f, 16f to 0.14f, 7f to 0.20f)) {
            drawPath(edge, MIST_VEIL.copy(alpha = alpha), style = Stroke(width.dp.toPx() * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }

    // Region mode: the mist already shows what is walked, so the townships get no colour: only a thin dark-grey outline and their names.
    // The one the walker is in has a slightly firmer line, the one picked a firm one.
    if (regionMode && areas != null && explored.isNotEmpty()) {
        val here = if (currentTown >= 0) currentTown else selectedTown
        val grey = Color(0xFF3A3F44)
        if (w >= 1700f) {
            explored.forEach { id ->
                if (id != selectedTown) drawPath(areas.path(id, w, h), grey.copy(alpha = if (id == here) 0.55f else 0.30f), style = Stroke((if (id == here) 1.6f else 1.0f).dp.toPx(), join = StrokeJoin.Round))
            }
        }
        if (selectedTown in explored) { drawPath(areas.path(selectedTown, w, h), Color.White.copy(alpha = 0.9f), style = Stroke(5.dp.toPx(), join = StrokeJoin.Round)); drawPath(areas.path(selectedTown, w, h), grey, style = Stroke(2.2.dp.toPx(), join = StrokeJoin.Round)) }
        if (w > 3000f) {
            val placedNames = ArrayList<Rect>()
            val pxPerKm = w / 222.5f
            val ids = explored.toList()   // a township not walked through keeps its name hidden
            ids.forEach { id ->
                val t = areas.towns[id]
                val q = GeoPoint(t.lat, t.lon).px()
                if (q.x < view.left - 80f || q.x > view.right + 80f || q.y < view.top - 40f || q.y > view.bottom + 40f) return@forEach
                val sel = id == selectedTown
                val maxW = (t.widthKm * pxPerKm * 0.92).toFloat().coerceAtLeast(1f)
                var sp = if (sel) 15f else 13f
                var tx = measurer.measure(t.name, TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = sp.sp, color = Color(0xFF3A3F44)))
                while (sp > 6f && tx.size.width > maxW) { sp -= 0.5f; tx = measurer.measure(t.name, tx.layoutInput.style.copy(fontSize = sp.sp)) }
                if (tx.size.width > maxW && !sel) return@forEach
                val hw = tx.size.width / 2f; val hh = tx.size.height / 2f
                val tl0 = Offset(q.x - hw, q.y - hh)
                val box = Rect(tl0.x, tl0.y, tl0.x + tx.size.width, tl0.y + tx.size.height)
                if (sel || placedNames.none { it.overlaps(box) }) {
                    val halo = measurer.measure(t.name, tx.layoutInput.style.copy(color = Color.White.copy(alpha = 0.92f)))
                    for (dx in floatArrayOf(-1.6f, 1.6f)) for (dy in floatArrayOf(-1.6f, 1.6f)) drawText(halo, topLeft = Offset(tl0.x + dx, tl0.y + dy))
                    drawText(tx, topLeft = tl0); placedNames += box
                }
            }
        }
    }

    // Region mode, a township not walked through yet that has been picked: no name, but its border in a dashed line so the player can see how far it reaches.
    if (regionMode && areas != null && w >= 1700f && selectedTown in areas.towns.indices && selectedTown !in explored) {
        val edge = areas.path(selectedTown, w, h)
        val dashes = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 6.dp.toPx()))
        drawPath(edge, Color.White.copy(alpha = 0.85f), style = Stroke(4.5.dp.toPx(), join = StrokeJoin.Round, pathEffect = dashes))
        drawPath(edge, Color(0xFF3A3F44), style = Stroke(2.dp.toPx(), join = StrokeJoin.Round, pathEffect = dashes))
    }

    // The route: a bold solid line behind the walker, a dashed dark line ahead, a dotted red one where a train or ferry carries the walker.
    // Each is one path so the dashes run on unbroken; a pale casing under both keeps them readable on the painting.
    val walkedPath = Path(); val aheadPath = Path(); val ridePath = Path(); val rideAhead = Path()
    var walkedEnd: Offset? = null; var aheadEnd: Offset? = null; var rideEnd: Offset? = null; var rideAheadEnd: Offset? = null
    fun extend(path: Path, last: Offset?, a: Offset, b: Offset): Offset {
        if (last == null || (last - a).getDistance() > 0.5f) path.moveTo(a.x, a.y)
        path.lineTo(b.x, b.y); return b
    }
    // The way ahead is only shown as far as the next card; everything beyond stays unknown.
    val nextStopIdx = route.stops.indices.firstOrNull { route.stops[it].landmark != null && route.cumulativeKm[it] > walkedKm + 1e-9 } ?: route.stops.lastIndex
    for (i in 1..nextStopIdx) {
        val a = route.stops[i - 1].at.px(); val b = route.stops[i].at.px()
        val startKm = route.cumulativeKm[i - 1]; val endKm = route.cumulativeKm[i]
        when {
            route.stops[i].transit != null -> if (walkedKm >= endKm) rideEnd = extend(ridePath, rideEnd, a, b) else rideAheadEnd = extend(rideAhead, rideAheadEnd, a, b)
            walkedKm >= endKm -> walkedEnd = extend(walkedPath, walkedEnd, a, b)
            walkedKm <= startKm -> aheadEnd = extend(aheadPath, aheadEnd, a, b)
            else -> {
                val mid = here.px()
                walkedEnd = extend(walkedPath, walkedEnd, a, mid)
                aheadEnd = extend(aheadPath, aheadEnd, mid, b)
            }
        }
    }
    val round = StrokeCap.Round
    val k = (w / 3000f).coerceIn(0.28f, 1f) * (if (regionMode && !layers.route) 0.6f else 1f)
    val ms = (w / 2400f).coerceIn(0.4f, 1f)      // markers shrink with the map
    val casing = Color.White.copy(alpha = 0.92f)
    val rideDots = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx() * k.coerceAtLeast(0.5f), 3.dp.toPx() * k.coerceAtLeast(0.5f)))
    val aheadDash = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 6.dp.toPx()))
    if (layers.route) drawPath(aheadPath, casing, style = Stroke(6.5.dp.toPx() * k, cap = StrokeCap.Butt, join = StrokeJoin.Round))
    if (layers.route) drawPath(walkedPath, casing, style = Stroke(9.dp.toPx() * k, cap = round, join = StrokeJoin.Round))
    if (layers.route) drawPath(ridePath, casing, style = Stroke(7.dp.toPx() * k.coerceAtLeast(0.5f), cap = round, join = StrokeJoin.Round))
    if (layers.route) drawPath(rideAhead, Color(0xFF7B3FB5).copy(alpha = 0.6f), style = Stroke(3.dp.toPx() * k.coerceAtLeast(0.5f), cap = round, pathEffect = rideDots))
    if (layers.route) drawPath(ridePath, Color(0xFF7B3FB5), style = Stroke(3.5.dp.toPx() * k.coerceAtLeast(0.5f), cap = round, pathEffect = rideDots))
    if (layers.route) drawPath(aheadPath, Color(0xFFB3202A), style = Stroke(2.8.dp.toPx() * k, cap = StrokeCap.Butt, join = StrokeJoin.Round, pathEffect = aheadDash))
    if (layers.route) drawPath(walkedPath, Color(0xFF0E86D4), style = Stroke(5.dp.toPx() * k, cap = round, join = StrokeJoin.Round))
    if (layers.route) drawPath(walkedPath, Color(0xFF9AD8FF).copy(alpha = 0.8f), style = Stroke(1.6.dp.toPx() * k, cap = round, join = StrokeJoin.Round))

    // Everything that takes room is recorded, so a name is only put where it covers neither a marker nor another name.
    val occupied = ArrayList<Rect>()
    fun reserve(center: Offset, half: Float) { occupied += Rect(center.x - half, center.y - half, center.x + half, center.y + half) }
    val p = here.px()
    // The traveller shrinks with the map and turns into a glowing dot when the island is small on screen.
    val dotMode = w < 1700f
    val pinHeight = 60.dp.toPx() * (w / 4500f).coerceIn(0.4f, 1f)
    val pinWidth = if (pin != null) pinHeight * pin.width / pin.height else 16.dp.toPx()
    occupied += if (pin != null && !dotMode) Rect(p.x - pinWidth / 2, p.y - pinHeight, p.x + pinWidth / 2, p.y + 4.dp.toPx())
    else Rect(p.x - 9.dp.toPx(), p.y - 9.dp.toPx(), p.x + 9.dp.toPx(), p.y + 9.dp.toPx())
    route.stops.forEach { stop -> if (stop.landmark != null || stop.supply != null) reserve(stop.at.px(), 9.dp.toPx()) }
    val shownPois = route.pois.filter { visiblePois == null || it.id in visiblePois }
    // A small sight lying on a card or a station is set a little aside, so that its dot is not hidden behind or over theirs.
    val markerPoints = route.stops.filter { it.landmark != null || it.supply != null }.map { it.at.px() }
    fun poiSpot(poi: com.starmist.core.route.Poi): Offset {
        val o = poi.at.px()
        val near = markerPoints.firstOrNull { (it - o).getDistance() < 14.dp.toPx() } ?: return o
        val v = o - near
        val len = v.getDistance()
        val dir = if (len < 1f) Offset(0.7f, -0.7f) else v / len
        return near + dir * 16.dp.toPx()
    }
    shownPois.forEach { reserve(poiSpot(it), 5.dp.toPx()) }

    /** Puts [text] beside [anchor] on a parchment tag, on the first side that is free; returns false when none is. */
    fun placeName(text: String, anchor: Offset, size: androidx.compose.ui.unit.TextUnit, color: Color, gap: Float, force: Boolean = false, plain: Boolean = false, edge: Color? = null, weight: FontWeight = FontWeight.Bold, underline: Boolean = false): Boolean {
        val under = if (underline) androidx.compose.ui.text.style.TextDecoration.Underline else null
        val t = measurer.measure(text, label.copy(fontSize = size, color = color, fontWeight = weight, textDecoration = under))
        val padX = 4.dp.toPx(); val padY = 1.5.dp.toPx()
        val tw = t.size.width.toFloat(); val th = t.size.height.toFloat()
        val tops = listOf(
            Offset(anchor.x + gap, anchor.y - th / 2),
            Offset(anchor.x - gap - tw, anchor.y - th / 2),
            Offset(anchor.x - tw / 2, anchor.y - gap - th),
            Offset(anchor.x - tw / 2, anchor.y + gap),
        )
        val chosen = tops.firstOrNull { o ->
            val r = Rect(o.x - padX, o.y - padY, o.x + tw + padX, o.y + th + padY)
            r.left >= 0f && r.top >= 0f && r.right <= w && r.bottom <= h && occupied.none { it.overlaps(r) }
        } ?: if (force) tops[0] else return false
        val box = Rect(chosen.x - padX, chosen.y - padY, chosen.x + tw + padX, chosen.y + th + padY)
        if (plain) {
            drawText(measurer.measure(text, label.copy(fontSize = size, fontWeight = weight, color = Color(0xFFFFF6E0).copy(alpha = 0.9f), drawStyle = Stroke(width = 4f, join = StrokeJoin.Round))), topLeft = chosen)
        } else {
            drawRoundRect(Vintage.parchmentLight.copy(alpha = 0.96f), box.topLeft, box.size, CornerRadius(6.dp.toPx()))
            drawRoundRect(edge ?: Vintage.edge.copy(alpha = 0.7f), box.topLeft, box.size, CornerRadius(6.dp.toPx()), style = Stroke((if (edge != null) 2.2f else 0.8f).dp.toPx()))
        }
        drawText(t, topLeft = chosen)
        occupied += box
        return true
    }

    // Cards and stamps: collected ones are solid blue, the next card is a big red target; nothing further on is shown.
    val nextCardId = route.next(walkedKm)?.first?.id
    if (layers.spots) route.stops.forEachIndexed { i, stop ->
        val o = stop.at.px()
        val reached = route.cumulativeKm[i] <= walkedKm + 1e-9
        if (stop.landmark != null) {
            when {
                stop.landmark?.id == nextCardId -> {
                    drawCircle(Color(0xFFB3202A).copy(alpha = 0.25f), 19.dp.toPx() * ms, o)
                    drawCircle(Color.White, 10.dp.toPx() * ms, o)
                    drawCircle(Color(0xFFB3202A), 10.dp.toPx() * ms, o, style = Stroke(3.5.dp.toPx() * ms))
                    drawCircle(Color(0xFFB3202A), 4.dp.toPx() * ms, o)
                }
                reached -> { drawCircle(Color(0xFF0E86D4), 6.5.dp.toPx() * ms, o); drawCircle(Color.White, 6.5.dp.toPx() * ms, o, style = Stroke(2.dp.toPx() * ms)) }
            }
        } else if (stop.supply != null && reached) {
            val side = 11.dp.toPx() * ms
            drawRect(SUPPLY_COLOR, Offset(o.x - side / 2, o.y - side / 2), Size(side, side))
            drawRect(Color.White, Offset(o.x - side / 2, o.y - side / 2), Size(side, side), style = Stroke(1.8.dp.toPx()))
        }
    }

    // Small sights: a plain ring until the stamp is earned, then the teal seal.
    if (layers.spots) {
        val earned = earnedPois
        shownPois.forEach { poi ->
            val o = poiSpot(poi)
            if (poi.id in earned) {
                drawCircle(Color(0xFF2F7F6E), 6.dp.toPx() * ms, o)
                drawCircle(Color(0xFFFFF6E0), 6.dp.toPx() * ms, o, style = Stroke(1.4.dp.toPx() * ms))
            } else {
                drawCircle(Color(0xFF2F7F6E), 4.dp.toPx() * ms, o, style = Stroke(1.6.dp.toPx() * ms))
            }
        }
    }

    // Names are queued with a priority and placed from the most important down; a name that would cover a more important
    // one (or a marker) is simply not drawn, and it comes back by itself once there is room.
    class Label(val priority: Int, val place: () -> Unit)
    val queue = ArrayList<Label>()
    val next = route.next(walkedKm)?.first
    val lastCard = route.collected(walkedKm).lastOrNull()
    // How far to the next card (its name stays hidden until it is reached).
    if (w >= 1700f && layers.spots) next?.let { n ->
        val toNext = route.next(walkedKm)!!.second
        queue += Label(100) {
            val o = route.stops.first { it.landmark?.id == n.id }.at.px()
            placeName("還有 ${"%.1f".format(toNext)} 公里", o, 13.sp, Color(0xFFB3202A), 16.dp.toPx(), force = true, edge = Color(0xFFB3202A))
        }
    }
    // Names of the cards already collected.
    if (w >= 1700f && layers.spots) route.collected(walkedKm).forEach { card ->
        val o = route.stops.first { it.landmark?.id == card.id }.at.px()
        if (card.id != lastCard?.id && (w <= 3000f || o.x < view.left - 60f || o.x > view.right + 60f || o.y < view.top - 40f || o.y > view.bottom + 40f)) return@forEach
        queue += Label(if (card.id == lastCard?.id) 90 else 60) { placeName(card.name, o, 12.sp, Vintage.ink, 11.dp.toPx(), force = card.id == lastCard?.id) }
    }
    // Stations whose stamp has been collected: the square on the map gets its name.
    if (w >= 1700f && layers.spots) route.stops.forEachIndexed { i, stop ->
        val supply = stop.supply ?: return@forEachIndexed
        if (route.cumulativeKm[i] > walkedKm + 1e-9) return@forEachIndexed
        val o = stop.at.px()
        if (w <= 3000f || o.x < view.left - 60f || o.x > view.right + 60f || o.y < view.top - 40f || o.y > view.bottom + 40f) return@forEachIndexed
        queue += Label(45) { placeName(supply.name, o, 11.sp, Vintage.ink, 9.dp.toPx(), plain = true, weight = FontWeight.Medium) }
    }
    // A small sight's name appears only when its dot has been tapped.
    if (layers.spots) shownPois.firstOrNull { it.id == selectedPoi }?.let { poi ->
        queue += Label(120) { placeName(poi.name, poiSpot(poi), 12.sp, Color(0xFF16584A), 9.dp.toPx(), force = true, edge = Color(0xFF2F7F6E), weight = FontWeight.Bold) }
    }
    queue.sortedByDescending { it.priority }.forEach { it.place() }

    // The walker is drawn by [drawWalker] in a layer above this one (see ZoomPanMap.top).
}

/** The traveller in three layers so it can walk: body (with hat, cape and pack), staff with its lantern, and the hand that holds the staff. */
/** [frames] is a drawn walk cycle (art_traveller_walk_0..5); when it is missing the layers below are moved by the code instead. */
internal class WalkerArt(
    val body: ImageBitmap, val staff: ImageBitmap, val hand: ImageBitmap, val frames: List<ImageBitmap> = emptyList(),
    /** Loads the pictures of an idle act, or null when they are missing (see [Idle]). Only the act in use is kept in memory. */
    private val loadIdle: (Int) -> List<ImageBitmap>? = { null },
) {
    @Volatile private var idleCache: Pair<Int, List<ImageBitmap>?>? = null

    fun idleFrames(act: Int): List<ImageBitmap>? {
        idleCache?.let { if (it.first == act) return it.second }
        return loadIdle(act).also { idleCache = act to it }
    }
}

@Composable
internal fun rememberWalkerArt(): WalkerArt? {
    val body = rememberArt("traveller_body")
    val staff = rememberArt("traveller_staff")
    val hand = rememberArt("traveller_hand")
    // The smooth cycle (24 pictures: the six drawn frames with in-betweens, half size) when it is there, else the six drawn frames.
    val smooth = (0 until WALKX_FRAMES).map { rememberArt("traveller_walkx_$it") }
    val frames = if (smooth.all { it != null }) smooth else (0 until WALK_FRAMES).map { rememberArt("traveller_walk_$it") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val loadIdle: (Int) -> List<ImageBitmap>? = remember(context) {
        { act ->
            val ids = (0 until Idle.FRAMES).map { context.resources.getIdentifier("art_idle_${Idle.NAMES[act]}_$it", "drawable", context.packageName) }
            if (ids.any { it == 0 }) null else runCatching { ids.map { ImageBitmap.imageResource(context.resources, it) } }.getOrNull()
        }
    }
    return if (body != null && staff != null && hand != null) WalkerArt(body, staff, hand, frames.filterNotNull().takeIf { it.size == WALK_FRAMES || it.size == WALKX_FRAMES } ?: emptyList(), loadIdle) else null
}

private const val WALK_FRAMES = 6
private const val WALKX_FRAMES = 24     // art_traveller_walkx_0..23: four pictures from one drawn frame to the next
private const val REST_FRAME = 2      // the drawn frame at phase 0: a passing pose, the one it stands in

// Where things are in the traveller pictures, as fractions of their size.
private const val HAND_X = 0.20f       // unused while the staff layer is empty (the traveller is one picture per pose)
private const val HAND_Y = 0.62f
private const val FEET_X = 0.40f       // the middle of the body (not the staff): it stands on the map point, and the picture is mirrored about it when the walker turns
private const val FEET_Y = 0.975f
private const val LANTERN_X = 0.885f   // the lantern hangs on the staff in front of the walker
private const val LANTERN_Y = 0.49f

/**
 * Draws the walker standing at [p]. With [walking] it takes steps: the body hops and rocks from foot to
 * foot, squashing a little when it lands, the staff swings a beat behind, and the lantern glows
 * brighter. [phase] runs 0 to 1 once per pair of steps. Standing still it is a plain picture, so
 * nothing needs redrawing.
 */
internal fun DrawScope.drawWalker(
    p: Offset, w: Float, art: WalkerArt?, phase: Float, walk: Float, face: Float = 1f,
    /** A little thing it does while standing (see [Idle]) and how far through it is, 0 to 1; [measurer] draws its marks. */
    idle: Int = Idle.NONE, idleT: Float = 0f, measurer: androidx.compose.ui.text.TextMeasurer? = null,
) {
    if (art == null || w < 1700f) {
        drawCircle(Color(0xFFFF8A00).copy(alpha = 0.25f), 13.dp.toPx(), p)
        drawCircle(Color(0xFFFF8A00).copy(alpha = 0.45f), 8.dp.toPx(), p)
        drawCircle(Color.White, 5.dp.toPx(), p)
        drawCircle(Color(0xFFFF8A00), 3.2.dp.toPx(), p)
        return
    }
    val k = walk.coerceIn(0f, 1f)                          // 0 standing, 1 walking; eases in and out
    val framesMode = art.frames.isNotEmpty()
    val m = if (framesMode) 0f else k                      // the code-driven motion is only used without drawn frames
    val height = 60.dp.toPx() * (w / 4500f).coerceIn(0.4f, 1f)
    val width = height * art.body.width / art.body.height
    val turn = (phase * 2.0 * Math.PI).toFloat()
    val rock = kotlin.math.sin(turn) * m                   // -1..1, foot to foot
    val hop = kotlin.math.abs(kotlin.math.sin(turn)) * m   // 0 on each landing, 1 mid-step
    val lift = height * 0.05f * hop
    val squash = m * (1f - kotlin.math.abs(kotlin.math.sin(turn)))   // how recently it landed

    val idleArt = if (idle != Idle.NONE) art.idleFrames(idle) else null
    val pose = if (idle != Idle.NONE && idleArt == null) idlePose(idle, idleT) else IdlePose()   // the code-made poses only stand in for missing drawings
    val air = (hop + pose.lift * 8f).coerceIn(0f, 1f)       // how far off the ground, for the shadow
    // Turning round: the whole figure is mirrored about its feet, passing through edge-on, so it turns instead of jumping.
    withTransform({ scale(face * pose.face, 1f, p) }) {
        drawOval(Color.Black.copy(alpha = 0.18f - 0.06f * air), Offset(p.x - width * 0.3f * (1f - 0.12f * air), p.y - 3.dp.toPx()), Size(width * 0.6f * (1f - 0.12f * air), 6.dp.toPx()))

        val feet = Offset(width * FEET_X, height * FEET_Y)
        val hand = Offset(width * HAND_X, height * HAND_Y)
        // Drawn walk cycle: the picture is always one of the frames. Phase 0 and 0.5 are the passing poses, which is how it
        // stands, so a walk starts from and ends on a standing pose and nothing has to be cross-faded.
        if (framesMode || idleArt != null) {
            val n = art.frames.size
            val per = (n / WALK_FRAMES).coerceAtLeast(1)                // pictures per drawn frame
            val pos = phase * n
            val first = if (framesMode) (pos.toInt() + REST_FRAME * per) % n else 0
            val frame = if (framesMode) art.frames[first] else null
            val next = if (framesMode) art.frames[(first + 1) % n] else null
            val between = pos - pos.toInt()                             // how far on from [frame] to [next]
            translate(p.x - width * FEET_X, p.y - height + 2.dp.toPx() - pose.lift * height) {
                withTransform({
                    rotate(pose.rot, feet)
                    scale(pose.sx, pose.sy, feet)
                }) {
                    val dst = IntSize(width.toInt(), height.toInt())
                    if (idleArt != null) {
                        // The standing picture of the walk is what it rests in, so the act melts in from it and back out to it.
                        val edge = minOf(1f, idleT / 0.08f, (1f - idleT) / 0.08f).coerceIn(0f, 1f)
                        if (edge < 1f && frame != null) drawImage(frame, dstSize = dst)
                        val shown = idleFrame(idle, idleT)
                        // The act's pictures have extra room left and right (a wide pose needs it): same height, centred on the standing picture.
                        val wide = (height * idleArt[0].width / idleArt[0].height).toInt()
                        val at = IntOffset(-(wide - dst.width) / 2, 0)
                        drawImage(idleArt[shown.a], dstOffset = at, dstSize = IntSize(wide, dst.height), alpha = edge)
                        if (shown.mix > 0.01f) drawImage(idleArt[shown.b], dstOffset = at, dstSize = IntSize(wide, dst.height), alpha = shown.mix * edge)
                    } else if (frame != null) {
                        drawImage(frame, dstSize = dst)
                        if (next != null && between > 0.02f) drawImage(next, dstSize = dst, alpha = between)   // melt into the next picture, so the steps do not click
                    }
                    val lantern = Offset(width * LANTERN_X, height * LANTERN_Y)
                    val glow = 0.30f + 0.12f * kotlin.math.sin(turn * 2f) * k
                    drawCircle(
                        Brush.radialGradient(listOf(Color(0xFFFFE9A8).copy(alpha = glow), Color.Transparent), lantern, height * 0.16f),
                        height * 0.16f, lantern,
                    )
                }
            }
        }
        val standing = if (framesMode || idleArt != null) 0f else 1f
        if (standing > 0.01f) {
            translate(p.x - width * FEET_X, p.y - height + 2.dp.toPx() - lift) {
                withTransform({
                    rotate(3.5f * rock, feet)
                    scale(1f + 0.035f * squash, 1f - 0.045f * squash, feet)
                }) {
                    drawImage(art.body, dstSize = IntSize(width.toInt(), height.toInt()), alpha = standing)
                    val swing = 8f * kotlin.math.sin(turn - 0.9f) * m
                    withTransform({ rotate(swing, hand) }) {
                        drawImage(art.staff, dstSize = IntSize(width.toInt(), height.toInt()), alpha = standing)
                        val glow = (0.22f + (0.08f + 0.15f * kotlin.math.sin(turn * 2f)) * m) * standing
                        val lantern = Offset(width * LANTERN_X, height * LANTERN_Y)
                        drawCircle(
                            Brush.radialGradient(listOf(Color(0xFFFFE9A8).copy(alpha = glow), Color.Transparent), lantern, height * 0.16f),
                            height * 0.16f, lantern,
                        )
                    }
                    drawImage(art.hand, dstSize = IntSize(width.toInt(), height.toInt()), alpha = standing)
                }
            }
        }
    }
    if (idle != Idle.NONE && measurer != null) {
        val sign = if (face >= 0f) 1f else -1f
        drawIdleEmote(measurer, idle, idleT, Offset(p.x, p.y - height * 1.02f), height, sign)
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


private val MIST_VEIL = Color(0xFFE9E3D1)

/** One soft bank of mist: centre in map units (0 to 1), radius as a share of the map width, how much wider than tall. */
private class MistBank(val u: Float, val v: Float, val r: Float, val stretch: Float, val color: Color, val alpha: Float)

private fun mistBanks(seed: Int, count: Int, rMin: Float, rMax: Float): List<MistBank> {
    val rnd = kotlin.random.Random(seed)
    return List(count) {
        val light = rnd.nextFloat() < 0.7f
        MistBank(
            u = rnd.nextFloat(), v = rnd.nextFloat(),
            r = rMin + (rMax - rMin) * rnd.nextFloat(),
            stretch = 1.5f + rnd.nextFloat(),
            color = if (light) Color(0xFFFAF6EA) else Color(0xFFB9B29C),
            alpha = if (light) 0.30f + 0.20f * rnd.nextFloat() else 0.14f + 0.12f * rnd.nextFloat(),
        )
    }
}

/** The same banks every time, so the mist always lies in the same places; big ones for the overview, small wisps when zoomed in. */
private val MIST_BANKS = mistBanks(7, 170, 0.025f, 0.075f)
private val MIST_WISPS = mistBanks(11, 520, 0.010f, 0.030f)
