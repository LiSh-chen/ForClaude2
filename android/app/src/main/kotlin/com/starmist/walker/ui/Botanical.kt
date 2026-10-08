package com.starmist.walker.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/** Small pen-and-wash drawings in the style of an old field guide. Everything is a few strokes. */

private fun quad(a: Offset, c: Offset, b: Offset, t: Float): Offset {
    val u = 1 - t
    return Offset(u * u * a.x + 2 * u * t * c.x + t * t * b.x, u * u * a.y + 2 * u * t * c.y + t * t * b.y)
}

private fun quadTangent(a: Offset, c: Offset, b: Offset, t: Float): Offset {
    val u = 1 - t
    val dx = 2 * u * (c.x - a.x) + 2 * t * (b.x - c.x)
    val dy = 2 * u * (c.y - a.y) + 2 * t * (b.y - c.y)
    val length = hypot(dx, dy).coerceAtLeast(0.0001f)
    return Offset(dx / length, dy / length)
}

/** A fern frond: a curved stem with paired leaflets that shrink towards the tip. */
fun DrawScope.drawFern(base: Offset, height: Float, lean: Float, ink: Color = Vintage.ink, wash: Color = Vintage.mossLight) {
    val tip = Offset(base.x + lean * height, base.y - height)
    val control = Offset(base.x - lean * height * 0.15f, base.y - height * 0.65f)
    val stem = Path().apply {
        moveTo(base.x, base.y)
        quadraticTo(control.x, control.y, tip.x, tip.y)
    }
    drawPath(stem, ink, style = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round))
    val leaflets = 16
    for (i in 1..leaflets) {
        val t = i / (leaflets + 1f)
        val p = quad(base, control, tip, t)
        val tangent = quadTangent(base, control, tip, t)
        val normal = Offset(-tangent.y, tangent.x)
        val length = height * 0.3f * (1 - t).pow(0.75f) + height * 0.02f
        for (side in listOf(-1f, 1f)) {
            val end = Offset(
                p.x + (tangent.x * 0.6f + normal.x * side) * length,
                p.y + (tangent.y * 0.6f + normal.y * side) * length,
            )
            drawLine(wash.copy(alpha = 0.55f), p, end, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
            drawLine(ink.copy(alpha = 0.85f), p, end, strokeWidth = 1.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

/** A lily-like flower on a tall stem, with a wash of colour inside the pen lines. */
fun DrawScope.drawLily(base: Offset, height: Float, lean: Float, petal: Color = Vintage.rose, ink: Color = Vintage.ink) {
    val tip = Offset(base.x + lean * height, base.y - height)
    val control = Offset(base.x + lean * height * 0.1f, base.y - height * 0.55f)
    val stem = Path().apply {
        moveTo(base.x, base.y)
        quadraticTo(control.x, control.y, tip.x, tip.y)
    }
    drawPath(stem, ink, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
    // Two long basal leaves.
    for (side in listOf(-1f, 1f)) {
        val leaf = Path().apply {
            moveTo(base.x, base.y)
            quadraticTo(base.x + side * height * 0.35f, base.y - height * 0.15f, base.x + side * height * 0.45f, base.y - height * 0.02f)
            quadraticTo(base.x + side * height * 0.2f, base.y - height * 0.04f, base.x, base.y)
            close()
        }
        drawPath(leaf, Vintage.mossLight.copy(alpha = 0.7f))
        drawPath(leaf, ink, style = Stroke(1.dp.toPx()))
    }
    // Petals fan out from the tip.
    val petalLength = height * 0.28f
    for (k in -2..2) {
        val angle = (-90f + k * 34f) * PI.toFloat() / 180f
        val dir = Offset(cos(angle), sin(angle))
        val perp = Offset(-dir.y, dir.x)
        val shape = Path().apply {
            moveTo(tip.x, tip.y)
            quadraticTo(
                tip.x + dir.x * petalLength * 0.5f + perp.x * petalLength * 0.28f,
                tip.y + dir.y * petalLength * 0.5f + perp.y * petalLength * 0.28f,
                tip.x + dir.x * petalLength, tip.y + dir.y * petalLength,
            )
            quadraticTo(
                tip.x + dir.x * petalLength * 0.5f - perp.x * petalLength * 0.28f,
                tip.y + dir.y * petalLength * 0.5f - perp.y * petalLength * 0.28f,
                tip.x, tip.y,
            )
            close()
        }
        drawPath(shape, petal.copy(alpha = 0.6f))
        drawPath(shape, ink, style = Stroke(1.dp.toPx()))
    }
    for (k in -1..1) {
        val end = Offset(tip.x + k * petalLength * 0.18f, tip.y - petalLength * 0.5f)
        drawLine(ink, tip, end, strokeWidth = 0.8.dp.toPx())
        drawCircle(Vintage.brass, radius = 1.8.dp.toPx(), center = end)
    }
}

/** A reed with a cattail head. */
fun DrawScope.drawReed(base: Offset, height: Float, lean: Float, ink: Color = Vintage.ink) {
    val tip = Offset(base.x + lean * height, base.y - height)
    drawLine(ink, base, tip, strokeWidth = 1.3.dp.toPx(), cap = StrokeCap.Round)
    val leafEnd = Offset(base.x - lean * height * 0.6f - height * 0.12f, base.y - height * 0.55f)
    drawLine(Vintage.mossLight.copy(alpha = 0.7f), base, leafEnd, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
    drawLine(ink.copy(alpha = 0.8f), base, leafEnd, strokeWidth = 0.9.dp.toPx(), cap = StrokeCap.Round)
    drawLine(Color(0xFF7A5230), tip, Offset(tip.x + lean * height * 0.08f, tip.y - height * 0.16f), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
}

/** A spiral shell. */
fun DrawScope.drawShell(center: Offset, radius: Float, ink: Color = Vintage.ink) {
    drawCircle(Vintage.rose.copy(alpha = 0.45f), radius, center)
    drawCircle(ink, radius, center, style = Stroke(1.2.dp.toPx()))
    var r = radius * 0.7f
    var start = 20f
    repeat(3) {
        drawArc(
            ink, start, 250f, false,
            Offset(center.x - r, center.y - r), Size(r * 2, r * 2), style = Stroke(0.9.dp.toPx(), cap = StrokeCap.Round),
        )
        r *= 0.55f
        start += 70f
    }
}

/** One wavy line of water; [phase] slides the pattern sideways so waves can drift. */
fun DrawScope.drawWaveLine(y: Float, from: Float, to: Float, amplitude: Float, wavelength: Float, phase: Float, color: Color) {
    val path = Path()
    var x = from - (phase % wavelength) - wavelength
    path.moveTo(x, y)
    var flip = 1f
    while (x < to + wavelength) {
        path.quadraticTo(x + wavelength / 4, y - amplitude * flip, x + wavelength / 2, y)
        flip = -flip
        x += wavelength / 2
    }
    drawPath(path, color, style = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round))
}

/** A round-crowned tree outline, used far away and on the map. */
fun DrawScope.drawTreeSketch(base: Offset, height: Float, ink: Color = Vintage.ink, wash: Color = Vintage.mossLight) {
    drawLine(ink, base, Offset(base.x, base.y - height * 0.45f), strokeWidth = 1.4.dp.toPx(), cap = StrokeCap.Round)
    val crown = Offset(base.x, base.y - height * 0.7f)
    val radius = height * 0.3f
    drawCircle(wash.copy(alpha = 0.6f), radius, crown)
    drawCircle(ink, radius, crown, style = Stroke(1.1.dp.toPx()))
    drawArc(ink.copy(alpha = 0.6f), 200f, 100f, false, Offset(crown.x - radius * 0.6f, crown.y - radius * 0.6f), Size(radius * 1.2f, radius * 1.2f), style = Stroke(0.8.dp.toPx()))
}

/** Repeats [draw] across the width, shifted by [shift], so scenery can scroll without end. */
inline fun DrawScope.repeating(shift: Float, period: Float, draw: DrawScope.(x: Float, index: Int) -> Unit) {
    val offset = shift % period
    var index = -1
    var x = -offset - period
    while (x < size.width + period) {
        draw(x, index)
        x += period
        index++
    }
}
