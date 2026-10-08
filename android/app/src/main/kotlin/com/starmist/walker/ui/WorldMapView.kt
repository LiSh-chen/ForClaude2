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

private class Puff(val at: Pt, val alpha: Float)

private const val FOG_COLUMNS = 15
private const val FOG_ROWS = 20

/**
 * The whole world on one sheet of old paper. Places the traveller has not reached are hidden under
 * drifting fog; the fog lifts around every spot that has been walked.
 */
@Composable
fun WorldMapView(regionIndex: Int, fraction: Double, finished: Boolean, modifier: Modifier = Modifier) {
    val key = Triple(regionIndex, (fraction * 40).toInt(), finished)
    val walked = remember(key) { WorldMap.walkedPoints(regionIndex, fraction, finished) }
    val traveller = remember(key) { WorldMap.pointAt(regionIndex, if (finished) 1.0 else fraction) }
    val puffs = remember(key) { fogPuffs(walked) }
    val measurer = rememberTextMeasurer()
    val label = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Vintage.ink)
    val sublabel = TextStyle(fontFamily = FontFamily.Serif, fontSize = 10.sp, color = Vintage.inkSoft)

    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(0.78f)
            .clip(TornShape(5, 3.dp))
            .background(Vintage.parchmentLight),
    ) {
        val w = size.width
        val h = size.height
        fun Pt.px() = Offset((x * w).toFloat(), (y * h).toFloat())

        // Land, with the sea hatched in around it.
        val land = smoothClosed(landOutline.map { it.px() })
        drawPath(land, Vintage.mossLight.copy(alpha = 0.22f))
        clipPath(land, clipOp = ClipOp.Difference) {
            var y = -h * 0.05f
            while (y < h) {
                drawLine(Vintage.sea.copy(alpha = 0.55f), Offset(0f, y), Offset(w, y + h * 0.05f), strokeWidth = 1.dp.toPx())
                y += 9.dp.toPx()
            }
        }
        drawPath(land, Vintage.ink.copy(alpha = 0.25f), style = Stroke(4.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round))
        drawPath(land, Vintage.ink.copy(alpha = 0.85f), style = Stroke(1.4.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round))

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
            drawLandmark(node.id, at, h)
            val name = measurer.measure(node.name, label)
            val labelTop = at.y + h * 0.055f
            drawText(name, topLeft = Offset(at.x - name.size.width / 2f, labelTop))
            if (!node.available) {
                val note = measurer.measure("尚未開放", sublabel)
                drawText(note, topLeft = Offset(at.x - note.size.width / 2f, labelTop + name.size.height))
            }
        }

        // The traveller: a brass pin.
        val pin = traveller.px()
        drawCircle(Vintage.brassLight, 9.dp.toPx(), pin)
        drawCircle(Vintage.ink, 9.dp.toPx(), pin, style = Stroke(1.6.dp.toPx()))
        drawCircle(Vintage.moss, 3.5.dp.toPx(), pin)

        // Fog over everything that is still unknown.
        val radius = w / FOG_COLUMNS * 1.15f
        puffs.forEach { puff ->
            val center = puff.at.px()
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
        drawRect(Vintage.ink.copy(alpha = 0.8f), style = Stroke(2.dp.toPx()))
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
