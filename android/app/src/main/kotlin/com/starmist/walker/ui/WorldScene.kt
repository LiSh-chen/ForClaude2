package com.starmist.walker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.sin

/** The travellers a player can pick. Look only; nothing else changes. */
enum class Look { FOX, ELF, CAT, CAPE }

data class Character(val name: String, val look: Look, val body: Color, val accent: Color)

object Characters {
    val all = listOf(
        Character("小狐", Look.FOX, Color(0xFFE0A064), Color(0xFFFFF1DC)),
        Character("森林精靈", Look.ELF, Color(0xFF8DB584), Color(0xFFE3F0C8)),
        Character("貓耳", Look.CAT, Color(0xFFA9A5B8), Color(0xFFF2D6DE)),
        Character("披風旅人", Look.CAPE, Color(0xFF8DA9CF), Color(0xFFD98A7A)),
    )
}

/**
 * A pen-and-wash view of the traveller on the road, drawn straight onto the paper. The layers move
 * at different speeds while [walking] is true and hold still otherwise, so it reads as depth
 * without any background work.
 */
@Composable
fun WorldScene(
    regionId: String?,
    characterId: Int,
    walking: Boolean,
    modifier: Modifier = Modifier,
) {
    val hour = remember { LocalTime.now().hour }
    val night = hour < 6 || hour >= 19
    val character = Characters.all[characterId.coerceIn(0, Characters.all.lastIndex)]

    // Distance travelled on screen; only advances while walking, and keeps its value when stopped.
    var offset by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(walking) {
        if (walking) {
            var last = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                offset += (now - last) / 1_000_000_000f * WALK_SPEED
                last = now
            }
        }
    }

    Canvas(modifier.fillMaxWidth().height(190.dp)) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.80f

        if (night) {
            drawRect(Vintage.night.copy(alpha = 0.22f))
            drawStars(w, h)
        } else {
            drawCircle(Vintage.brassLight.copy(alpha = 0.55f), radius = h * 0.09f, center = Offset(w * 0.84f, h * 0.2f))
            drawCircle(Vintage.ink.copy(alpha = 0.4f), radius = h * 0.09f, center = Offset(w * 0.84f, h * 0.2f), style = Stroke(1.dp.toPx()))
        }

        if (regionId == "moonlit_coast") drawCoast(w, h, groundY, offset) else drawForest(w, h, groundY, offset)

        // The road: a dashed pen line with a few pebbles.
        drawLine(
            Vintage.ink.copy(alpha = 0.7f), Offset(0f, groundY), Offset(w, groundY),
            strokeWidth = 1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
        )
        repeating(offset * density * 0.9f, 70.dp.toPx()) { x, i ->
            val y = groundY + 10.dp.toPx() + (i % 3 + 3) % 3 * 7.dp.toPx()
            drawOval(Vintage.ink.copy(alpha = 0.25f), Offset(x, y), Size(7.dp.toPx(), 3.dp.toPx()))
        }

        drawTraveller(character, w * 0.4f, groundY, h, walking, offset)
    }
}

private const val WALK_SPEED = 90f // units per second; scaled per layer below

private fun DrawScope.drawStars(w: Float, h: Float) {
    val spots = listOf(0.1f to 0.15f, 0.25f to 0.3f, 0.4f to 0.1f, 0.55f to 0.25f, 0.7f to 0.12f, 0.92f to 0.3f)
    spots.forEach { (x, y) -> drawCircle(Vintage.brassLight, radius = 2.2f, center = Offset(w * x, h * y)) }
    drawCircle(Vintage.parchmentLight, radius = h * 0.08f, center = Offset(w * 0.84f, h * 0.2f))
    drawCircle(Vintage.ink.copy(alpha = 0.5f), radius = h * 0.08f, center = Offset(w * 0.84f, h * 0.2f), style = Stroke(1.dp.toPx()))
}

private fun DrawScope.drawForest(w: Float, h: Float, groundY: Float, offset: Float) {
    // Far trees, faint.
    repeating(offset * density * 0.2f, 120.dp.toPx()) { x, i ->
        drawTreeSketch(Offset(x + 30.dp.toPx(), groundY - 4.dp.toPx()), h * (0.42f + 0.06f * ((i % 3 + 3) % 3)), Vintage.ink.copy(alpha = 0.3f), Vintage.mossLight.copy(alpha = 0.5f))
    }
    // Ferns on the left of each patch, a flower on the right.
    repeating(offset * density * 0.55f, w * 0.95f) { x, i ->
        val lift = if ((i % 2 + 2) % 2 == 0) 0f else 6.dp.toPx()
        drawFern(Offset(x + w * 0.06f, groundY + lift), h * 0.62f, 0.22f)
        drawFern(Offset(x + w * 0.12f, groundY + lift), h * 0.46f, 0.32f)
        drawFern(Offset(x + w * 0.02f, groundY + lift), h * 0.5f, -0.18f)
        drawLily(Offset(x + w * 0.72f, groundY + lift), h * 0.58f, -0.06f)
        drawLily(Offset(x + w * 0.8f, groundY + lift), h * 0.4f, 0.1f, petal = Vintage.mossLight)
    }
}

private fun DrawScope.drawCoast(w: Float, h: Float, groundY: Float, offset: Float) {
    val sea = Vintage.sea.copy(alpha = 0.28f)
    drawRect(sea, Offset(0f, h * 0.5f), Size(w, groundY - h * 0.5f))
    for (row in 0..3) {
        val y = h * 0.54f + row * h * 0.075f
        drawWaveLine(y, 0f, w, 4.dp.toPx(), 36.dp.toPx(), offset * density * (0.2f + row * 0.08f), Vintage.ink.copy(alpha = 0.35f))
    }
    repeating(offset * density * 0.55f, w * 0.9f) { x, i ->
        drawReed(Offset(x + w * 0.08f, groundY), h * 0.55f, 0.1f)
        drawReed(Offset(x + w * 0.12f, groundY), h * 0.4f, -0.12f)
        drawShell(Offset(x + w * 0.62f, groundY + 12.dp.toPx()), 7.dp.toPx())
        drawReed(Offset(x + w * 0.8f, groundY), h * 0.5f, 0.08f)
        if ((i % 2 + 2) % 2 == 0) drawShell(Offset(x + w * 0.35f, groundY + 16.dp.toPx()), 5.dp.toPx())
    }
}

private fun DrawScope.drawTraveller(
    character: Character,
    x: Float,
    groundY: Float,
    h: Float,
    walking: Boolean,
    offset: Float,
) {
    val unit = h * 0.012f
    val phase = if (walking) sin(offset * 0.14f) else 0f
    val bob = if (walking) abs(phase) * unit * 1.2f else 0f
    val bodyH = unit * 17f
    val bodyW = unit * 9f
    val bodyTop = groundY - unit * 8f - bodyH - bob
    val ink = Vintage.ink
    val line = 1.3.dp.toPx()

    drawOval(Color.Black.copy(alpha = 0.14f), Offset(x - bodyW * 0.8f, groundY - unit), Size(bodyW * 1.6f, unit * 2.4f))

    // Legs.
    drawLine(ink, Offset(x - bodyW * 0.2f, bodyTop + bodyH), Offset(x - bodyW * 0.2f + phase * unit * 5f, groundY - unit), strokeWidth = line, cap = StrokeCap.Round)
    drawLine(ink, Offset(x + bodyW * 0.2f, bodyTop + bodyH), Offset(x + bodyW * 0.2f - phase * unit * 5f, groundY - unit), strokeWidth = line, cap = StrokeCap.Round)

    // Pack on the back.
    val pack = Path().apply {
        addRoundRect(androidx.compose.ui.geometry.RoundRect(x - bodyW * 0.95f, bodyTop + unit * 2f, x - bodyW * 0.25f, bodyTop + unit * 10f, unit * 1.5f, unit * 1.5f))
    }
    drawPath(pack, Color(0xFFC9A263).copy(alpha = 0.8f))
    drawPath(pack, ink, style = Stroke(line))

    if (character.look == Look.CAPE) {
        val cape = Path().apply {
            moveTo(x - bodyW * 0.45f, bodyTop + unit * 2f)
            lineTo(x - bodyW * 1.3f - phase * unit * 2f, bodyTop + bodyH)
            lineTo(x - bodyW * 0.2f, bodyTop + bodyH)
            close()
        }
        drawPath(cape, character.accent.copy(alpha = 0.8f))
        drawPath(cape, ink, style = Stroke(line))
    }

    // Coat: a flared shape, washed in the traveller's colour.
    val coat = Path().apply {
        moveTo(x - bodyW * 0.4f, bodyTop)
        lineTo(x + bodyW * 0.4f, bodyTop)
        lineTo(x + bodyW * 0.7f, bodyTop + bodyH)
        lineTo(x - bodyW * 0.7f, bodyTop + bodyH)
        close()
    }
    drawPath(coat, character.body.copy(alpha = 0.85f))
    drawPath(coat, ink, style = Stroke(line))

    // Head.
    val head = Offset(x + unit * 0.5f, bodyTop - unit * 4.2f)
    drawCircle(character.accent, unit * 4.6f, head)
    drawCircle(ink, unit * 4.6f, head, style = Stroke(line))
    drawCircle(ink, radius = unit * 0.7f, center = Offset(head.x + unit * 1.9f, head.y - unit * 0.3f))

    when (character.look) {
        Look.FOX, Look.CAT -> for (dx in listOf(-1f, 1f)) {
            val ear = Path().apply {
                moveTo(head.x + dx * unit * 1.2f, head.y - unit * 3.6f)
                lineTo(head.x + dx * unit * 4.4f, head.y - unit * 7.8f)
                lineTo(head.x + dx * unit * 4.8f, head.y - unit * 2.4f)
                close()
            }
            drawPath(ear, character.body.copy(alpha = 0.9f))
            drawPath(ear, ink, style = Stroke(line))
        }
        Look.ELF -> {
            drawOval(character.body.copy(alpha = 0.9f), Offset(head.x - unit, head.y - unit * 9.4f), Size(unit * 4f, unit * 6f))
            drawOval(ink, Offset(head.x - unit, head.y - unit * 9.4f), Size(unit * 4f, unit * 6f), style = Stroke(line))
        }
        Look.CAPE -> {
            // A wide-brimmed hat, as in the field notebook.
            drawOval(Color(0xFFD9C79A), Offset(head.x - unit * 7f, head.y - unit * 4.6f), Size(unit * 14f, unit * 3.2f))
            drawOval(ink, Offset(head.x - unit * 7f, head.y - unit * 4.6f), Size(unit * 14f, unit * 3.2f), style = Stroke(line))
            val crown = Path().apply {
                moveTo(head.x - unit * 3.5f, head.y - unit * 3.6f)
                quadraticTo(head.x, head.y - unit * 10f, head.x + unit * 3.5f, head.y - unit * 3.6f)
                close()
            }
            drawPath(crown, Color(0xFFD9C79A))
            drawPath(crown, ink, style = Stroke(line))
        }
    }
}
