package com.starmist.walker.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Today's progress as a brass pocket compass wrapped in vines. The green arc around the dial fills
 * as the day's goal is approached.
 */
@Composable
fun CompassDial(steps: Long, goal: Int, progress: Double, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(progress.coerceIn(0.0, 1.0).toFloat(), label = "compass")
    val measurer = rememberTextMeasurer()
    val letterStyle = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Vintage.ink.copy(alpha = 0.75f))

    val caseArt = rememberArt("compass_case")
    val faceArt = rememberArt("compass_face")
    if (caseArt != null && faceArt != null) {
        // Painted brass case over a painted dial; the progress arc runs in the ring between them.
        Box(modifier.width(300.dp).height(300.dp * 1120f / 1024f), contentAlignment = Alignment.TopCenter) {
            Canvas(Modifier.fillMaxSize()) {
                val k = size.width / 1024f
                val center = Offset(512f * k, 638.4f * k)
                val faceSide = (590f * k).toInt()
                drawImage(
                    faceArt,
                    dstOffset = IntOffset((center.x - faceSide / 2f).toInt(), (center.y - faceSide / 2f).toInt()),
                    dstSize = IntSize(faceSide, faceSide),
                )
                val track = 270f * k
                val stroke = 20f * k
                val topLeft = Offset(center.x - track, center.y - track)
                val arcSize = Size(track * 2, track * 2)
                drawArc(Vintage.brassDark.copy(alpha = 0.2f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                if (animated > 0f) drawArc(Vintage.moss, -90f, 360f * animated, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                drawImage(caseArt, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
            }
            Column(Modifier.padding(top = 300.dp * 0.57f * 1120f / 1024f - 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(steps.withCommas(), fontSize = 38.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = Vintage.ink)
                Text("/ ${goal.toLong().withCommas()} 步", style = MaterialTheme.typography.bodyMedium)
                Text("${(progress * 100).roundToInt()}%", style = MaterialTheme.typography.titleMedium)
            }
        }
        return
    }

    Box(modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(300.dp)) {
            val center = Offset(size.width / 2, size.height * 0.54f)
            val outer = size.width * 0.34f
            val ringWidth = outer * 0.13f
            val face = outer - ringWidth

            // Brass case and the loop it hangs from.
            drawCircle(
                brush = Brush.sweepGradient(listOf(Vintage.brassLight, Vintage.brass, Vintage.brassDark, Vintage.brass, Vintage.brassLight), center),
                radius = outer - ringWidth / 2, center = center, style = Stroke(ringWidth),
            )
            drawCircle(Vintage.ink.copy(alpha = 0.7f), outer, center, style = Stroke(1.2.dp.toPx()))
            drawCircle(Vintage.ink.copy(alpha = 0.5f), face, center, style = Stroke(1.dp.toPx()))
            val loopCenter = Offset(center.x, center.y - outer - outer * 0.1f)
            drawCircle(Vintage.brassDark, outer * 0.12f, loopCenter, style = Stroke(outer * 0.05f))
            drawLine(Vintage.brassDark, Offset(center.x, loopCenter.y + outer * 0.1f), Offset(center.x, center.y - outer), strokeWidth = outer * 0.07f)

            // Dial face.
            drawCircle(
                brush = Brush.radialGradient(listOf(Vintage.parchmentLight, Vintage.brassLight.copy(alpha = 0.75f)), center, face),
                radius = face, center = center,
            )

            // Progress: a moss-green arc on a faint track.
            val track = face * 0.84f
            val trackWidth = face * 0.09f
            val arcTopLeft = Offset(center.x - track, center.y - track)
            val arcSize = Size(track * 2, track * 2)
            drawArc(Vintage.brassDark.copy(alpha = 0.22f), 0f, 360f, false, arcTopLeft, arcSize, style = Stroke(trackWidth))
            if (animated > 0f) {
                drawArc(Vintage.moss, -90f, 360f * animated, false, arcTopLeft, arcSize, style = Stroke(trackWidth, cap = StrokeCap.Round))
            }

            // Compass rose, faint so the number stays readable.
            val reach = track * 0.8f
            val waist = reach * 0.16f
            val star = Path().apply {
                moveTo(center.x, center.y - reach)
                lineTo(center.x + waist, center.y - waist)
                lineTo(center.x + reach, center.y)
                lineTo(center.x + waist, center.y + waist)
                lineTo(center.x, center.y + reach)
                lineTo(center.x - waist, center.y + waist)
                lineTo(center.x - reach, center.y)
                lineTo(center.x - waist, center.y - waist)
                close()
            }
            drawPath(star, Vintage.brass.copy(alpha = 0.16f))
            drawPath(star, Vintage.ink.copy(alpha = 0.28f), style = Stroke(1.dp.toPx()))
            val diagonal = reach * 0.62f * 0.7071f
            for (sx in listOf(-1f, 1f)) for (sy in listOf(-1f, 1f)) {
                drawLine(Vintage.ink.copy(alpha = 0.2f), center, Offset(center.x + sx * diagonal, center.y + sy * diagonal), strokeWidth = 1.dp.toPx())
            }

            // N, E, S, W.
            val letterRadius = track * 0.66f
            listOf("N" to Offset(0f, -1f), "E" to Offset(1f, 0f), "S" to Offset(0f, 1f), "W" to Offset(-1f, 0f)).forEach { (letter, dir) ->
                val layout = measurer.measure(letter, letterStyle)
                drawText(
                    layout,
                    topLeft = Offset(
                        center.x + dir.x * letterRadius - layout.size.width / 2f,
                        center.y + dir.y * letterRadius - layout.size.height / 2f,
                    ),
                )
            }

            // Vines climbing the case.
            drawVine(center, outer, startDeg = 100f, sweepDeg = 170f, phase = 0f)
            drawVine(center, outer, startDeg = -75f, sweepDeg = 120f, phase = 1.7f)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(steps.withCommas(), fontSize = 44.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = Vintage.ink)
            Text("/ ${goal.toLong().withCommas()} 步", style = MaterialTheme.typography.bodyMedium)
            Text("${(progress * 100).roundToInt()}%", style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun DrawScope.drawVine(center: Offset, radius: Float, startDeg: Float, sweepDeg: Float, phase: Float) {
    val path = Path()
    val leaves = ArrayList<Path>()
    val segments = 40
    for (i in 0..segments) {
        val rad = ((startDeg + sweepDeg * i / segments) * PI / 180.0)
        val outward = Offset(cos(rad).toFloat(), sin(rad).toFloat())
        val r = radius * (1.08f + 0.05f * sin(i * 0.9f + phase))
        val p = Offset(center.x + outward.x * r, center.y + outward.y * r)
        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        if (i % 4 == 2) {
            val side = if ((i / 4) % 2 == 0) 1f else -1f
            val tangent = Offset(-outward.y, outward.x)
            val out = radius * 0.12f
            val along = radius * 0.09f * side
            val tip = Offset(p.x + outward.x * out + tangent.x * along, p.y + outward.y * out + tangent.y * along)
            leaves += Path().apply {
                moveTo(p.x, p.y)
                quadraticTo(p.x + outward.x * out * 0.2f + tangent.x * along * 1.1f, p.y + outward.y * out * 0.2f + tangent.y * along * 1.1f, tip.x, tip.y)
                quadraticTo(p.x + outward.x * out * 1.1f + tangent.x * along * 0.1f, p.y + outward.y * out * 1.1f + tangent.y * along * 0.1f, p.x, p.y)
                close()
            }
        }
    }
    drawPath(path, Vintage.moss, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round))
    leaves.forEach {
        drawPath(it, Vintage.mossLight.copy(alpha = 0.85f))
        drawPath(it, Vintage.moss, style = Stroke(1.dp.toPx()))
    }
}
