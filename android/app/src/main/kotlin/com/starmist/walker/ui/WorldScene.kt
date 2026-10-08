package com.starmist.walker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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

    Canvas(
        modifier
            .fillMaxWidth()
            .height(200.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
    ) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.80f
        val coast = regionId == "moonlit_coast"

        // Sky, sun or moon.
        drawRect(if (night) Vintage.night else Vintage.brassLight)
        if (night) {
            drawStars(w, h)
        } else {
            drawCircle(Vintage.brass, h * 0.10f, Offset(w * 0.84f, h * 0.22f))
        }

        // Three layers of paper hills (or sea swells on the coast), each drifting at its own pace.
        val far = if (coast) Vintage.sea else Vintage.hillFar
        val mid = if (coast) Color(0xFF4F878C) else Vintage.hillMid
        val near = if (coast) Color(0xFF3A6B70) else Vintage.hillNear
        drawHills(w, h, h * 0.52f, h * 0.10f, 260.dp.toPx(), offset * density * 0.15f, far)
        drawHills(w, h, h * 0.64f, h * 0.09f, 200.dp.toPx(), offset * density * 0.35f + 90f, mid)
        drawHills(w, h, h * 0.76f, h * 0.07f, 150.dp.toPx(), offset * density * 0.6f + 40f, near)

        // Foreground plants on the near layer.
        repeating(offset * density * 0.9f, 110.dp.toPx()) { x, i ->
            val k = (i % 3 + 3) % 3
            if (coast) drawGrassTuft(Offset(x + 30.dp.toPx(), groundY + 2.dp.toPx()), h * (0.16f + 0.03f * k), Vintage.mossLight)
            else drawLeafPlant(Offset(x + 30.dp.toPx(), groundY + 2.dp.toPx()), h * (0.22f + 0.04f * k), if (k == 1) Vintage.brass else Vintage.mossLight)
        }

        // The ground the traveller stands on.
        drawRect(Vintage.hillNear, Offset(0f, groundY), Size(w, h - groundY))
        drawRect(Color.Black.copy(alpha = 0.12f), Offset(0f, groundY), Size(w, 3.dp.toPx()))
        repeating(offset * density * 0.9f, 70.dp.toPx()) { x, i ->
            val y = groundY + 9.dp.toPx() + (i % 3 + 3) % 3 * 6.dp.toPx()
            drawOval(Vintage.hillMid.copy(alpha = 0.8f), Offset(x, y), Size(8.dp.toPx(), 3.dp.toPx()))
        }

        drawTraveller(character, w * 0.4f, groundY, h, walking, offset)
    }
}

private const val WALK_SPEED = 90f // units per second; scaled per layer below

private fun DrawScope.drawStars(w: Float, h: Float) {
    val spots = listOf(0.1f to 0.15f, 0.25f to 0.3f, 0.4f to 0.1f, 0.55f to 0.25f, 0.7f to 0.12f, 0.92f to 0.3f)
    spots.forEach { (x, y) -> drawCircle(Vintage.brassLight, radius = 2.2f, center = Offset(w * x, h * y)) }
    drawCircle(Vintage.parchmentLight, radius = h * 0.09f, center = Offset(w * 0.84f, h * 0.22f))
}

/** One layer of rolling hills with a faint paper shadow along its top edge. */
private fun DrawScope.drawHills(w: Float, h: Float, baseY: Float, amplitude: Float, wavelength: Float, shift: Float, color: Color) {
    val path = Path()
    var x = -4f
    path.moveTo(x, h)
    while (x <= w + 4f) {
        val phase = (x + shift) / wavelength * 2f * Math.PI.toFloat()
        path.lineTo(x, baseY - amplitude * (sin(phase) * 0.6f + sin(phase * 2.3f + 1f) * 0.4f))
        x += 6f
    }
    path.lineTo(w + 4f, h)
    path.close()
    drawPath(path, Color.Black.copy(alpha = 0.10f), style = Stroke(5.dp.toPx()))
    drawPath(path, color)
}

/** A cut-paper plant: three pointed leaves fanning from the base. */
private fun DrawScope.drawLeafPlant(base: Offset, height: Float, color: Color) {
    for ((k, lean) in listOf(-0.34f, 0f, 0.34f).withIndex()) {
        val len = height * (if (k == 1) 1f else 0.72f)
        val tip = Offset(base.x + lean * len * 1.3f, base.y - len)
        val half = len * 0.16f
        val leaf = Path().apply {
            moveTo(base.x, base.y)
            quadraticTo(base.x + lean * len * 0.4f - half, base.y - len * 0.55f, tip.x, tip.y)
            quadraticTo(base.x + lean * len * 0.9f + half, base.y - len * 0.45f, base.x, base.y)
            close()
        }
        drawPath(leaf, Color.Black.copy(alpha = 0.15f), style = Stroke(2.dp.toPx()))
        drawPath(leaf, color)
    }
}

private fun DrawScope.drawGrassTuft(base: Offset, height: Float, color: Color) {
    for (lean in listOf(-0.25f, 0f, 0.2f)) {
        val tip = Offset(base.x + lean * height, base.y - height * (1f - kotlin.math.abs(lean)))
        drawLine(color, base, tip, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
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
    val ink = Vintage.tealDeep
    val line = 2.6.dp.toPx()

    drawOval(Color.Black.copy(alpha = 0.14f), Offset(x - bodyW * 0.8f, groundY - unit), Size(bodyW * 1.6f, unit * 2.4f))

    // Legs.
    drawLine(ink, Offset(x - bodyW * 0.2f, bodyTop + bodyH), Offset(x - bodyW * 0.2f + phase * unit * 5f, groundY - unit), strokeWidth = line, cap = StrokeCap.Round)
    drawLine(ink, Offset(x + bodyW * 0.2f, bodyTop + bodyH), Offset(x + bodyW * 0.2f - phase * unit * 5f, groundY - unit), strokeWidth = line, cap = StrokeCap.Round)

    // Pack on the back.
    val pack = Path().apply {
        addRoundRect(androidx.compose.ui.geometry.RoundRect(x - bodyW * 0.95f, bodyTop + unit * 2f, x - bodyW * 0.25f, bodyTop + unit * 10f, unit * 1.5f, unit * 1.5f))
    }
    drawPath(pack, Color(0xFFC9923A))

    if (character.look == Look.CAPE) {
        val cape = Path().apply {
            moveTo(x - bodyW * 0.45f, bodyTop + unit * 2f)
            lineTo(x - bodyW * 1.3f - phase * unit * 2f, bodyTop + bodyH)
            lineTo(x - bodyW * 0.2f, bodyTop + bodyH)
            close()
        }
        drawPath(cape, character.accent)
    }

    // Coat: a flared shape, washed in the traveller's colour.
    val coat = Path().apply {
        moveTo(x - bodyW * 0.4f, bodyTop)
        lineTo(x + bodyW * 0.4f, bodyTop)
        lineTo(x + bodyW * 0.7f, bodyTop + bodyH)
        lineTo(x - bodyW * 0.7f, bodyTop + bodyH)
        close()
    }
    drawPath(coat, character.body)

    // Head.
    val head = Offset(x + unit * 0.5f, bodyTop - unit * 4.2f)
    drawCircle(character.accent, unit * 4.6f, head)
    drawCircle(ink, radius = unit * 0.7f, center = Offset(head.x + unit * 1.9f, head.y - unit * 0.3f))

    when (character.look) {
        Look.FOX, Look.CAT -> for (dx in listOf(-1f, 1f)) {
            val ear = Path().apply {
                moveTo(head.x + dx * unit * 1.2f, head.y - unit * 3.6f)
                lineTo(head.x + dx * unit * 4.4f, head.y - unit * 7.8f)
                lineTo(head.x + dx * unit * 4.8f, head.y - unit * 2.4f)
                close()
            }
            drawPath(ear, character.body)
        }
        Look.ELF -> {
            drawOval(character.body, Offset(head.x - unit, head.y - unit * 9.4f), Size(unit * 4f, unit * 6f))
        }
        Look.CAPE -> {
            // A wide-brimmed hat, as in the field notebook.
            drawOval(Color(0xFFD9C79A), Offset(head.x - unit * 7f, head.y - unit * 4.6f), Size(unit * 14f, unit * 3.2f))
            val crown = Path().apply {
                moveTo(head.x - unit * 3.5f, head.y - unit * 3.6f)
                quadraticTo(head.x, head.y - unit * 10f, head.x + unit * 3.5f, head.y - unit * 3.6f)
                close()
            }
            drawPath(crown, Color(0xFFD9C79A))
        }
    }
}
