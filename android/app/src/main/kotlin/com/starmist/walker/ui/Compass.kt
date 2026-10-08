package com.starmist.walker.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Today's progress as stacked paper discs: a deep-teal base, a cream face, and an orange arc that
 * fills as the day's goal is approached. Each layer casts a soft cut-paper shadow.
 */
@Composable
fun CompassDial(steps: Long, goal: Int, progress: Double, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(progress.coerceIn(0.0, 1.0).toFloat(), label = "dial")

    Box(modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(300.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val base = size.width * 0.47f

            paperDisc(center, base, Vintage.teal)
            paperDisc(center, base * 0.84f, Vintage.parchmentLight)

            val track = base * 0.915f
            val width = base * 0.115f
            val topLeft = Offset(center.x - track, center.y - track)
            val arcSize = Size(track * 2, track * 2)
            drawArc(Vintage.teal.copy(alpha = 0.18f), 0f, 360f, false, topLeft, arcSize, style = Stroke(width))
            if (animated > 0f) {
                drawArc(Vintage.orange, -90f, 360f * animated, false, topLeft + Offset(0f, 2.dp.toPx()), arcSize, style = Stroke(width, cap = StrokeCap.Round), alpha = 0.25f)
                drawArc(Vintage.orange, -90f, 360f * animated, false, topLeft, arcSize, style = Stroke(width, cap = StrokeCap.Round))
            }
            paperDisc(center, base * 0.66f, Vintage.stain)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(steps.withCommas(), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Vintage.ink)
            Text("/ ${goal.toLong().withCommas()} 步", style = MaterialTheme.typography.bodyMedium, color = Vintage.inkSoft)
            Text("${(progress * 100).roundToInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Vintage.orange)
        }
    }
}

/** A flat disc with a soft drop shadow, as if cut from card. */
private fun DrawScope.paperDisc(center: Offset, radius: Float, color: Color) {
    val lift = 3.dp.toPx()
    drawCircle(Color.Black.copy(alpha = 0.10f), radius, center + Offset(0f, lift * 1.4f))
    drawCircle(Color.Black.copy(alpha = 0.10f), radius, center + Offset(0f, lift * 0.6f))
    drawCircle(color, radius, center)
}
