package com.starmist.walker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starmist.core.time.value
import kotlin.math.cos
import kotlin.math.sin

private val CAL_RED = Color(0xFFC0392B)
private val CAL_PAPER = Color(0xFFFFFBEF)
private val CAL_INK = Color(0xFF2F2A26)
private val monthNames = listOf("一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月")

/**
 * A month as a desk calendar: spiral binding and a red header on top, a stack of pages under it, printed numbers (red at the
 * weekend). The more steps a day had the deeper the watercolour under its number; a day that reached the goal has a red
 * rubber stamp, and today is circled in marker.
 */
@Composable
internal fun DeskCalendar(v: StatVisual.Heat, modifier: Modifier = Modifier) {
    val offset = v.month.atDay(1).dayOfWeek.value - 1
    val rows = (offset + v.steps.size + 6) / 7
    val top = (v.steps.take(v.todayIndex + 1).maxOrNull() ?: 0L).coerceAtLeast(1L).coerceAtLeast(if (v.goal > 0) v.goal.toLong() else 1L)
    Box(
        modifier.fillMaxWidth().drawBehind {
            // the pages underneath: two sheets, a little turned, peeking out below
            val x = 4.dp.toPx()
            val top = 8.dp.toPx()
            val w = size.width - 2 * x
            val h = size.height - top - 10.dp.toPx()
            for ((dy, deg, col) in listOf(Triple(9.dp, 0.8f, Color(0xFFE9DEBF)), Triple(5.dp, -0.6f, Color(0xFFF3EBD0)))) {
                rotate(deg, Offset(size.width / 2, top + h / 2)) { drawRoundRect(col, Offset(x, top + dy.toPx()), Size(w, h), CornerRadius(6.dp.toPx())) }
            }
        }.padding(top = 8.dp, bottom = 10.dp, start = 4.dp, end = 4.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().softShadow(Lift.Lifted, 6.dp).background(CAL_PAPER, androidx.compose.foundation.shape.RoundedCornerShape(6.dp)).drawBehind { curl() },
        ) {
            // red header with the month
            Box(
                Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(CAL_RED, Color(0xFFA32A1F))), androidx.compose.foundation.shape.RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).padding(top = 14.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${v.month.year}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f), letterSpacing = 3.sp)
                    Text(monthNames[v.month.monthValue - 1], fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 30.sp, color = Color.White)
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEachIndexed { i, d ->
                    Text(d, Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = if (i >= 5) CAL_RED else CAL_INK.copy(alpha = 0.6f))
                }
            }
            Column(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 10.dp)) {
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth()) {
                        for (c in 0 until 7) {
                            val d = r * 7 + c - offset
                            Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                                if (d in v.steps.indices) DayCell(d, v.steps[d], d > v.todayIndex, d == v.todayIndex, v.goal > 0 && d <= v.todayIndex && v.steps[d] >= v.goal, c >= 5, top)
                            }
                        }
                    }
                }
            }
        }
        // spiral binding over the top edge
        Canvas(Modifier.fillMaxWidth().height(22.dp).align(Alignment.TopCenter).padding(horizontal = 14.dp)) {
            val n = 9
            val gap = size.width / n
            for (i in 0 until n) {
                val x = gap * (i + 0.5f)
                drawCircle(Color(0xFF3A2F2A), 4.dp.toPx(), Offset(x, 14.dp.toPx()))
                drawLine(Brush.horizontalGradient(listOf(Color(0xFFB9BCC2), Color(0xFFF2F3F5), Color(0xFF8E9298)), x - 3.dp.toPx(), x + 3.dp.toPx()), Offset(x, 1.dp.toPx()), Offset(x, 16.dp.toPx()), 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun DayCell(d: Int, n: Long, future: Boolean, today: Boolean, reached: Boolean, weekend: Boolean, top: Long) {
    Box(
        Modifier.aspectRatio(1f).drawBehind {
            val c = Offset(size.width / 2, size.height / 2)
            val rad = size.width * 0.44f
            // watercolour: a soft blot, deeper with more steps
            if (!future && n > 0) {
                val a = (0.18f + 0.55f * (n.toFloat() / top)).coerceIn(0.18f, 0.75f)
                drawCircle(Brush.radialGradient(listOf(Vintage.brass.copy(alpha = a), Vintage.brass.copy(alpha = a * 0.55f), Color.Transparent), c, rad * 1.05f), rad * 1.05f, c)
            }
            if (reached) rubberStamp(c, rad * 0.95f, d)
            if (today) marker(c, rad * 1.02f)
        },
        contentAlignment = Alignment.Center,
    ) {
        Text("${d + 1}", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (future) CAL_INK.copy(alpha = 0.35f) else if (weekend) CAL_RED else CAL_INK)
    }
}

/** A round red stamp, a little off-centre and turned differently every day, with a thinner ring inside it. */
private fun DrawScope.rubberStamp(c: Offset, r: Float, d: Int) {
    val ink = Color(0xFFC0392B).copy(alpha = 0.78f)
    val j = Offset(((d * 37) % 7 - 3) * 0.35f, ((d * 53) % 7 - 3) * 0.35f)
    val cc = c + j
    drawCircle(ink, r, cc, style = Stroke(1.8.dp.toPx()))
    drawCircle(ink.copy(alpha = 0.6f), r * 0.82f, cc, style = Stroke(0.8.dp.toPx()))
    // a few specks where the ink did not take
    for (k in 0 until 5) {
        val a = (d * 1.7f + k * 1.26f)
        drawCircle(Color(0xFFFFFBEF).copy(alpha = 0.9f), 0.9.dp.toPx(), cc + Offset(cos(a) * r, sin(a) * r))
    }
}

/** Today: circled twice with a marker pen, the second loop a little wider and not closing. */
private fun DrawScope.marker(c: Offset, r: Float) {
    val pen = Color(0xFFD3392B)
    val p = Path()
    val steps = 48
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val ang = (t * 2.15f * Math.PI).toFloat() - 1.2f
        val rr = r * (0.9f + 0.16f * t) * (1f + 0.05f * sin(t * 9f))
        val x = c.x + cos(ang) * rr * 1.06f
        val y = c.y + sin(ang) * rr * 0.94f
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    drawPath(p, pen.copy(alpha = 0.85f), style = Stroke(2.2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
}

/** The lower right corner of the top sheet, turning up a little. */
private fun DrawScope.curl() {
    val s = 22.dp.toPx()
    val p = Path().apply { moveTo(size.width, size.height - s); lineTo(size.width - s, size.height); lineTo(size.width, size.height); close() }
    drawPath(p, Brush.linearGradient(listOf(Color(0xFFE2D6B4), Color(0xFFFFFDF4)), Offset(size.width - s, size.height - s), Offset(size.width, size.height)))
    drawLine(Color(0xFF4A3216).copy(alpha = 0.2f), Offset(size.width, size.height - s), Offset(size.width - s, size.height), 0.8.dp.toPx())
}
