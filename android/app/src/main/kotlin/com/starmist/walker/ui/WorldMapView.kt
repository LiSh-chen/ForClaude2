package com.starmist.walker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.starmist.core.world.Pt
import com.starmist.core.world.WorldMap
import kotlin.math.abs
import kotlin.math.sin

private val landOutline = listOf(
    Pt(0.04, 0.03), Pt(0.50, 0.01), Pt(0.93, 0.08), Pt(0.97, 0.36), Pt(0.93, 0.58),
    Pt(0.88, 0.82), Pt(0.64, 0.97), Pt(0.36, 0.99), Pt(0.09, 0.89), Pt(0.05, 0.60), Pt(0.02, 0.30),
)

internal class Puff(val at: Pt, val alpha: Float)

private const val FOG_COLUMNS = 15
private const val FOG_ROWS = 20

/** What the world map needs besides its size: where the walker is, what has been walked, the fog and the art. */
internal class WorldMapData(
    val walked: List<Pt>,
    val traveller: Pt,
    val puffs: List<Puff>,
    val fogArt: androidx.compose.ui.graphics.ImageBitmap?,
    val mapArt: androidx.compose.ui.graphics.ImageBitmap?,
    val pin: androidx.compose.ui.graphics.ImageBitmap?,
)

@Composable
internal fun rememberWorldMapData(regionIndex: Int, fraction: Double, finished: Boolean): WorldMapData {
    val key = Triple(regionIndex, (fraction * 40).toInt(), finished)
    val walked = remember(key) { WorldMap.walkedPoints(regionIndex, fraction, finished) }
    val traveller = remember(key) { WorldMap.pointAt(regionIndex, if (finished) 1.0 else fraction) }
    val puffs = remember(key) { fogPuffs(walked) }
    return WorldMapData(walked, traveller, puffs, rememberArt("fog_puff"), rememberArt("world_map_base"), rememberArt("traveller_fox"))
}

/** Width over height of the world map. */
@Composable
internal fun worldMapAspect(): Float = rememberArt("world_map_base")?.let { it.width.toFloat() / it.height } ?: 0.78f

/**
 * The whole world on one sheet of old paper. Places the traveller has not reached are hidden under
 * drifting fog; the fog lifts around every spot that has been walked. Static, for the journey page.
 */
@Composable
fun WorldMapView(regionIndex: Int, fraction: Double, finished: Boolean, modifier: Modifier = Modifier) {
    val data = rememberWorldMapData(regionIndex, fraction, finished)
    val measurer = rememberTextMeasurer()
    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(worldMapAspect())
            .clip(TornShape(5, 3.dp))
            .background(Vintage.parchmentLight),
    ) { drawWorldMap(size.width, size.height, data, measurer, zoom = 1f) }
}

/** The same world, zoomed in on the traveller; pinch and drag to look around, never out past the whole map. */
@Composable
fun ZoomWorldMap(regionIndex: Int, fraction: Double, finished: Boolean, modifier: Modifier = Modifier) {
    val data = rememberWorldMapData(regionIndex, fraction, finished)
    val measurer = rememberTextMeasurer()
    ZoomPanMap(
        aspect = worldMapAspect(),
        focus = Offset(data.traveller.x.toFloat(), data.traveller.y.toFloat()),
        followKey = (regionIndex * 1000 + (fraction * 200).toInt()),
        modifier = modifier,
        initialZoom = 3.2f,
        background = androidx.compose.ui.graphics.Color(0xFFEAD9AE),
    ) { w, h, zoom -> drawWorldMap(w, h, data, measurer, zoom) }
}

internal fun DrawScope.drawWorldMap(w: Float, h: Float, data: WorldMapData, measurer: androidx.compose.ui.text.TextMeasurer, zoom: Float) {
    val walked = data.walked
    val traveller = data.traveller
    val puffs = data.puffs
    val fogArt = data.fogArt
    val mapArt = data.mapArt
    val label = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Vintage.ink)
    val sublabel = TextStyle(fontFamily = FontFamily.Serif, fontSize = 10.sp, color = Vintage.inkSoft)
    fun Pt.px() = Offset((x * w).toFloat(), (y * h).toFloat())

    if (mapArt != null) {
        // The painted world map; fog, route and the traveller are drawn over it.
        drawImage(mapArt, dstSize = androidx.compose.ui.unit.IntSize(w.toInt(), h.toInt()))
    }

    // Land, with the sea hatched in around it (the drawn fallback when there is no painted map).
    val land = smoothClosed(landOutline.map { it.px() })
    if (mapArt == null) drawPath(land, Vintage.mossLight.copy(alpha = 0.22f))
    if (mapArt == null) clipPath(land, clipOp = ClipOp.Difference) {
        var y = -h * 0.05f
        while (y < h) {
            drawLine(Vintage.sea.copy(alpha = 0.55f), Offset(0f, y), Offset(w, y + h * 0.05f), strokeWidth = 1.dp.toPx())
            y += 9.dp.toPx()
        }
    }
    if (mapArt == null) {
        drawPath(land, Vintage.ink.copy(alpha = 0.25f), style = Stroke(4.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round))
        drawPath(land, Vintage.ink.copy(alpha = 0.85f), style = Stroke(1.4.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }

    // The whole route as a dotted line; the part already walked is drawn solid.
    val route = ArrayList<Offset>()
    for (k in WorldMap.nodes.indices) for (i in 0..16) route += WorldMap.pointAt(k, i / 16.0).px()
    val dotted = Path().apply { route.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
    drawPath(dotted, Vintage.inkSoft.copy(alpha = 0.7f), style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 11f))))
    val done = Path().apply { walked.forEachIndexed { i, p -> val o = p.px(); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
    drawPath(done, Vintage.brassDark, style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round))

    // Landmarks and names.
    WorldMap.nodes.forEach { node ->
        val at = node.at.px()
        if (mapArt == null) drawLandmark(node.id, at, h)
        val name = measurer.measure(node.name, label)
        val labelTop = at.y + if (mapArt == null) h * 0.055f else -name.size.height / 2f
        if (mapArt != null) {
            // A small paper tag so the name reads over the painting.
            drawRoundRect(
                Vintage.parchmentLight.copy(alpha = 0.82f),
                Offset(at.x - name.size.width / 2f - 6.dp.toPx(), labelTop - 2.dp.toPx()),
                Size(name.size.width + 12.dp.toPx(), name.size.height + 4.dp.toPx()),
                androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
            )
        }
        drawText(name, topLeft = Offset(at.x - name.size.width / 2f, labelTop))
        if (!node.available) {
            val note = measurer.measure("尚未開放", sublabel)
            drawText(note, topLeft = Offset(at.x - note.size.width / 2f, labelTop + name.size.height + 3.dp.toPx()))
        }
    }

    // Small sights near the traveller, drawn only while close; they fade out as the traveller moves away.
    drawNearbySights(w, h, traveller)

    // The traveller: the fox if the art is there, a brass pin otherwise.
    val pin = traveller.px()
    val art = data.pin
    if (art != null) {
        val height = 46.dp.toPx()
        val width = height * art.width / art.height
        drawOval(Color.Black.copy(alpha = 0.18f), Offset(pin.x - width * 0.3f, pin.y - 3.dp.toPx()), Size(width * 0.6f, 6.dp.toPx()))
        drawImage(
            art,
            dstOffset = androidx.compose.ui.unit.IntOffset((pin.x - width / 2f).toInt(), (pin.y - height + 2.dp.toPx()).toInt()),
            dstSize = androidx.compose.ui.unit.IntSize(width.toInt(), height.toInt()),
        )
    } else {
        drawCircle(Vintage.brassLight, 9.dp.toPx(), pin)
        drawCircle(Vintage.ink, 9.dp.toPx(), pin, style = Stroke(1.6.dp.toPx()))
        drawCircle(Vintage.moss, 3.5.dp.toPx(), pin)
    }

    // Fog over everything that is still unknown.
    val radius = w / FOG_COLUMNS * 1.15f
    puffs.forEach { puff ->
        val center = puff.at.px()
        if (fogArt != null) {
            // The painted puff is soft-edged, so draw it a little larger than the grid spacing.
            val side = (radius * 2.6f).toInt()
            drawImage(
                fogArt,
                dstOffset = androidx.compose.ui.unit.IntOffset((center.x - side / 2f).toInt(), (center.y - side / 2f).toInt()),
                dstSize = androidx.compose.ui.unit.IntSize(side, side),
                alpha = puff.alpha,
            )
            return@forEach
        }
        drawCircle(
            brush = Brush.radialGradient(
                0f to Vintage.fog.copy(alpha = puff.alpha),
                0.65f to Vintage.fog.copy(alpha = puff.alpha * 0.8f),
                1f to Color.Transparent,
                center = center, radius = radius,
            ),
            radius = radius, center = center,
        )
    }
}

/** Places for small things to see along each region; each shows only while the traveller is close. */
private class Sight(val at: Pt, val kind: Int)

private val sights: List<Sight> by lazy {
    val list = ArrayList<Sight>()
    for (k in WorldMap.nodes.indices) {
        val count = 12
        for (i in 0 until count) {
            val t = (i + 0.5) / count
            val a = WorldMap.pointAt(k, (t - 0.02).coerceAtLeast(0.0))
            val b = WorldMap.pointAt(k, (t + 0.02).coerceAtMost(1.0))
            val base = WorldMap.pointAt(k, t)
            val dx = b.x - a.x
            val dy = b.y - a.y
            val len = kotlin.math.hypot(dx, dy).coerceAtLeast(1e-6)
            val side = if ((i + k) % 2 == 0) 1.0 else -1.0
            val away = 0.022 + ((i * 7 + k * 3) % 5) * 0.008
            list += Sight(Pt(base.x - dy / len * away * side, base.y + dx / len * away * side), (i * 3 + k * 2) % 6)
        }
    }
    list
}

private fun DrawScope.drawNearbySights(w: Float, h: Float, traveller: Pt) {
    val range = 0.13
    sights.forEach { sight ->
        val d = traveller.distanceTo(sight.at)
        if (d > range) return@forEach
        val alpha = (1.0 - (d - range * 0.55) / (range * 0.45)).coerceIn(0.0, 1.0).toFloat()
        if (alpha <= 0f) return@forEach
        val o = Offset((sight.at.x * w).toFloat(), (sight.at.y * h).toFloat())
        val ink = Vintage.ink.copy(alpha = alpha)
        val unit = 14.dp.toPx()
        when (sight.kind) {
            0 -> drawTreeSketch(o, unit * 1.8f, ink, Vintage.mossLight.copy(alpha = alpha))
            1 -> drawFern(o, unit * 1.6f, 0.2f, ink, Vintage.mossLight.copy(alpha = alpha))
            2 -> drawReed(o, unit * 1.5f, 0.1f, ink)
            3 -> drawShell(o, unit * 0.45f, ink)
            4 -> { // a ring of glowing mushrooms
                for (m in -1..1) {
                    val c = Offset(o.x + m * unit * 0.5f, o.y - kotlin.math.abs(m) * unit * 0.1f)
                    drawLine(ink, c, Offset(c.x, c.y - unit * 0.35f), 1.1.dp.toPx())
                    drawArc(Vintage.rose.copy(alpha = alpha), 180f, 180f, true, Offset(c.x - unit * 0.28f, c.y - unit * 0.6f), Size(unit * 0.56f, unit * 0.5f))
                    drawCircle(Vintage.brassLight.copy(alpha = alpha * 0.55f), unit * 0.55f, Offset(c.x, c.y - unit * 0.4f))
                }
            }
            else -> { // a mossy stone
                drawOval(Vintage.parchmentDark.copy(alpha = alpha), Offset(o.x - unit * 0.55f, o.y - unit * 0.4f), Size(unit * 1.1f, unit * 0.7f))
                drawOval(ink, Offset(o.x - unit * 0.55f, o.y - unit * 0.4f), Size(unit * 1.1f, unit * 0.7f), style = Stroke(1.dp.toPx()))
                drawOval(Vintage.mossLight.copy(alpha = alpha), Offset(o.x - unit * 0.35f, o.y - unit * 0.45f), Size(unit * 0.7f, unit * 0.25f))
            }
        }
    }
}

/** Fog puffs on a loose grid; none where the traveller has been, thinner at the edge of what is known. */
private fun fogPuffs(walked: List<Pt>): List<Puff> {
    val puffs = ArrayList<Puff>()
    for (row in 0..FOG_ROWS) for (column in 0..FOG_COLUMNS) {
        val jitterX = ((row * 7 + column * 13) % 5 - 2) * 0.006
        val jitterY = ((row * 11 + column * 5) % 5 - 2) * 0.006
        val at = Pt(column.toDouble() / FOG_COLUMNS + jitterX, row.toDouble() / FOG_ROWS + jitterY)
        val distance = walked.minOf { it.distanceTo(at) }
        if (distance <= WorldMap.REVEAL_RADIUS) continue
        val alpha = ((distance - WorldMap.REVEAL_RADIUS) / 0.1).coerceIn(0.0, 1.0).toFloat() * 0.95f
        puffs += Puff(at, alpha.coerceAtLeast(0.15f))
    }
    return puffs
}

private fun smoothClosed(points: List<Offset>): Path {
    val path = Path()
    val n = points.size
    fun mid(a: Offset, b: Offset) = Offset((a.x + b.x) / 2, (a.y + b.y) / 2)
    val first = mid(points[n - 1], points[0])
    path.moveTo(first.x, first.y)
    for (i in 0 until n) {
        val m = mid(points[i], points[(i + 1) % n])
        path.quadraticTo(points[i].x, points[i].y, m.x, m.y)
    }
    path.close()
    return path
}

/** A tiny pen sketch for each place. Sizes follow the sheet height so the map scales with the screen. */
private fun DrawScope.drawLandmark(id: String, at: Offset, h: Float) {
    val u = h * 0.012f
    val ink = Vintage.ink
    val line = 1.2.dp.toPx()
    when (id) {
        "whispering_forest" -> {
            drawTreeSketch(Offset(at.x - u * 4f, at.y + u * 2f), u * 9f)
            drawTreeSketch(Offset(at.x + u * 4f, at.y + u * 2.5f), u * 8f)
            drawTreeSketch(Offset(at.x, at.y + u * 4f), u * 10f)
        }
        "moonlit_coast" -> {
            for (row in 0..2) drawWaveLine(at.y + row * u * 2.2f, at.x - u * 7f, at.x + u * 7f, u * 0.8f, u * 4f, 0f, ink.copy(alpha = 0.7f))
            drawShell(Offset(at.x + u * 5.5f, at.y - u * 3f), u * 1.8f)
        }
        "coral_isles" -> {
            drawOval(Vintage.mossLight.copy(alpha = 0.6f), Offset(at.x - u * 5f, at.y), Size(u * 10f, u * 3.4f))
            drawOval(ink, Offset(at.x - u * 5f, at.y), Size(u * 10f, u * 3.4f), style = Stroke(line))
            for (k in -1..1) {
                val base = Offset(at.x + k * u * 2.4f, at.y + u * 0.8f)
                drawLine(Vintage.rose, base, Offset(base.x + k * u, base.y - u * 4f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                drawLine(ink, base, Offset(base.x + k * u, base.y - u * 4f), strokeWidth = 0.9.dp.toPx(), cap = StrokeCap.Round)
            }
        }
        "misty_swamp" -> {
            drawReed(Offset(at.x - u * 3f, at.y + u * 3f), u * 8f, 0.1f)
            drawReed(Offset(at.x + u * 2f, at.y + u * 3f), u * 6f, -0.12f)
            drawOval(Vintage.sea.copy(alpha = 0.5f), Offset(at.x - u * 6f, at.y + u * 2.5f), Size(u * 12f, u * 3f))
        }
        "star_dunes" -> {
            for (k in 0..2) {
                val y = at.y + k * u * 2.4f
                drawArc(ink.copy(alpha = 0.8f), 190f, 160f, false, Offset(at.x - u * 7f + k * u, y - u * 2.5f), Size(u * 10f, u * 5f), style = Stroke(line))
            }
            drawCircle(Vintage.brassLight, u * 0.9f, Offset(at.x + u * 5f, at.y - u * 4f))
        }
        "sky_ruins" -> {
            drawOval(Vintage.parchmentDark, Offset(at.x - u * 5f, at.y - u * 2f), Size(u * 10f, u * 3f))
            drawOval(ink, Offset(at.x - u * 5f, at.y - u * 2f), Size(u * 10f, u * 3f), style = Stroke(line))
            val arch = Path().apply {
                moveTo(at.x - u * 2.5f, at.y - u * 1.5f)
                lineTo(at.x - u * 2.5f, at.y - u * 5f)
                quadraticTo(at.x, at.y - u * 8f, at.x + u * 2.5f, at.y - u * 5f)
                lineTo(at.x + u * 2.5f, at.y - u * 1.5f)
            }
            drawPath(arch, ink, style = Stroke(line, cap = StrokeCap.Round))
            drawCircle(Vintage.parchmentLight, u * 1.6f, Offset(at.x + u * 6f, at.y - u * 5f))
            drawCircle(ink.copy(alpha = 0.5f), u * 1.6f, Offset(at.x + u * 6f, at.y - u * 5f), style = Stroke(0.9.dp.toPx()))
        }
        "world_tree" -> {
            drawLine(ink, Offset(at.x, at.y + u * 4f), Offset(at.x, at.y - u * 2f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            val crown = Offset(at.x, at.y - u * 6f)
            drawCircle(Vintage.mossLight.copy(alpha = 0.7f), u * 6f, crown)
            drawCircle(ink, u * 6f, crown, style = Stroke(line))
            for (k in 0..4) {
                val a = k * 1.25f
                drawLine(ink.copy(alpha = 0.6f), crown, Offset(crown.x + sin(a) * u * 5f, crown.y - abs(sin(a * 2f)) * u * 5f), strokeWidth = 0.9.dp.toPx())
            }
        }
    }
}
