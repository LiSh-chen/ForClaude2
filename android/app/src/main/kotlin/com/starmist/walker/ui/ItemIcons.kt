package com.starmist.walker.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.starmist.core.world.ItemType

/** Small pen-and-wash icons for the things found on the road. [r] is roughly the half-size in px. */
fun DrawScope.drawItemIcon(item: ItemType, c: Offset, r: Float) {
    val ink = Vintage.ink
    val line = 1.4.dp.toPx()
    when (item) {
        ItemType.LIFE_SEED -> {
            val seed = Path().apply {
                moveTo(c.x, c.y - r * 0.5f)
                cubicTo(c.x + r * 0.9f, c.y - r * 0.1f, c.x + r * 0.5f, c.y + r * 0.9f, c.x, c.y + r * 0.9f)
                cubicTo(c.x - r * 0.5f, c.y + r * 0.9f, c.x - r * 0.9f, c.y - r * 0.1f, c.x, c.y - r * 0.5f)
                close()
            }
            drawPath(seed, Color(0xFFC9A263))
            drawPath(seed, ink, style = Stroke(line))
            for (side in listOf(-1f, 1f)) {
                val leaf = Path().apply {
                    moveTo(c.x, c.y - r * 0.5f)
                    quadraticTo(c.x + side * r * 0.9f, c.y - r * 1.2f, c.x + side * r * 0.8f, c.y - r * 0.6f)
                    quadraticTo(c.x + side * r * 0.3f, c.y - r * 0.4f, c.x, c.y - r * 0.5f)
                    close()
                }
                drawPath(leaf, Vintage.moss)
                drawPath(leaf, ink, style = Stroke(line * 0.7f))
            }
        }
        ItemType.MOON_DEW -> {
            val drop = Path().apply {
                moveTo(c.x, c.y - r)
                cubicTo(c.x + r * 0.9f, c.y - r * 0.1f, c.x + r * 0.8f, c.y + r * 0.9f, c.x, c.y + r * 0.9f)
                cubicTo(c.x - r * 0.8f, c.y + r * 0.9f, c.x - r * 0.9f, c.y - r * 0.1f, c.x, c.y - r)
                close()
            }
            drawPath(drop, Color(0xFFCFE3F0))
            drawPath(drop, ink, style = Stroke(line))
            drawArc(Color.White, 110f, 70f, false, Offset(c.x - r * 0.55f, c.y - r * 0.1f), Size(r * 1.1f, r * 1.1f), style = Stroke(line * 1.4f, cap = StrokeCap.Round))
        }
        ItemType.TIDE_PEARL -> {
            drawCircle(Color(0xFFF1EBDD), r * 0.8f, c)
            drawCircle(Color(0xFFB9C7D4).copy(alpha = 0.6f), r * 0.8f, Offset(c.x + r * 0.15f, c.y + r * 0.15f), style = Stroke(r * 0.25f))
            drawCircle(ink, r * 0.8f, c, style = Stroke(line))
            drawCircle(Color.White, r * 0.2f, Offset(c.x - r * 0.3f, c.y - r * 0.3f))
        }
        ItemType.RAINBOW_CORAL -> {
            val colours = listOf(Color(0xFFD9786A), Color(0xFFE0A858), Color(0xFF8DB584), Color(0xFF8DB4C4))
            listOf(-0.6f to 0, 0f to 1, 0.6f to 2, 0.25f to 3).forEach { (dx, k) ->
                val top = Offset(c.x + dx * r, c.y - r * (0.5f + 0.15f * k))
                drawLine(colours[k], Offset(c.x, c.y + r * 0.8f), top, strokeWidth = r * 0.28f, cap = StrokeCap.Round)
                drawLine(ink, Offset(c.x, c.y + r * 0.8f), top, strokeWidth = line * 0.6f, cap = StrokeCap.Round)
            }
        }
        ItemType.WIND_FEATHER -> {
            val feather = Path().apply {
                moveTo(c.x - r * 0.7f, c.y + r * 0.8f)
                cubicTo(c.x - r * 0.9f, c.y - r * 0.4f, c.x + r * 0.2f, c.y - r * 1.1f, c.x + r * 0.8f, c.y - r * 0.9f)
                cubicTo(c.x + r * 0.9f, c.y - r * 0.1f, c.x + r * 0.2f, c.y + r * 0.5f, c.x - r * 0.7f, c.y + r * 0.8f)
                close()
            }
            drawPath(feather, Color(0xFFE6EEF2))
            drawPath(feather, ink, style = Stroke(line))
            drawLine(ink, Offset(c.x - r * 0.7f, c.y + r * 0.8f), Offset(c.x + r * 0.6f, c.y - r * 0.7f), strokeWidth = line * 0.8f, cap = StrokeCap.Round)
        }
        ItemType.RUNE_SHARD -> {
            val shard = Path().apply {
                moveTo(c.x, c.y - r)
                lineTo(c.x + r * 0.7f, c.y - r * 0.1f)
                lineTo(c.x + r * 0.35f, c.y + r * 0.9f)
                lineTo(c.x - r * 0.5f, c.y + r * 0.7f)
                lineTo(c.x - r * 0.7f, c.y - r * 0.2f)
                close()
            }
            drawPath(shard, Color(0xFF9FB3A6))
            drawPath(shard, ink, style = Stroke(line))
            drawLine(Vintage.brassDark, Offset(c.x - r * 0.15f, c.y - r * 0.4f), Offset(c.x - r * 0.15f, c.y + r * 0.5f), strokeWidth = line, cap = StrokeCap.Round)
            drawLine(Vintage.brassDark, Offset(c.x - r * 0.15f, c.y), Offset(c.x + r * 0.3f, c.y - r * 0.3f), strokeWidth = line, cap = StrokeCap.Round)
        }
    }
}
