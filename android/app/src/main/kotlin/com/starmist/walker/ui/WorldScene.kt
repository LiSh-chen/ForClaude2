package com.starmist.walker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import kotlin.math.sin

/** The travellers a player can pick. Look only; nothing else changes. */
enum class Look { FOX, ELF, CAT, CAPE }

data class Character(val name: String, val look: Look, val body: Color, val accent: Color)

object Characters {
    val all = listOf(
        Character("小狐", Look.FOX, Color(0xFFE8893C), Color(0xFFFFF1DC)),
        Character("森林精靈", Look.ELF, Color(0xFF5FA55A), Color(0xFFBFE8A8)),
        Character("貓耳", Look.CAT, Color(0xFF8C8FA3), Color(0xFFF2C6D4)),
        Character("披風旅人", Look.CAPE, Color(0xFF4F7FC4), Color(0xFFD94F4F)),
    )
}

private class Palette(val skyTop: Color, val skyBottom: Color, val far: Color, val mid: Color, val ground: Color)

private fun paletteFor(regionId: String?, night: Boolean): Palette {
    val base = when (regionId) {
        "moonlit_coast" -> Palette(
            skyTop = Color(0xFF9CC9F0), skyBottom = Color(0xFFF7E4CF),
            far = Color(0xFF8FB8D8), mid = Color(0xFF4F9BBF), ground = Color(0xFFE9D8A6),
        )
        else -> Palette(
            skyTop = Color(0xFFA9D9F2), skyBottom = Color(0xFFE4F4DA),
            far = Color(0xFF7FB38A), mid = Color(0xFF3F8F5A), ground = Color(0xFF6BAA5B),
        )
    }
    if (!night) return base
    val dark = Color(0xFF0B1230)
    return Palette(
        skyTop = Color(0xFF0F1B3D), skyBottom = Color(0xFF2B3A67),
        far = lerp(base.far, dark, 0.6f), mid = lerp(base.mid, dark, 0.6f), ground = lerp(base.ground, dark, 0.55f),
    )
}

/**
 * A side-on view of the traveller on the road. The layers move at different speeds while
 * [walking] is true and hold still otherwise, so it reads as depth without any background work.
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
    val palette = remember(regionId, night) { paletteFor(regionId, night) }
    val character = Characters.all[characterId.coerceIn(0, Characters.all.lastIndex)]

    // Distance travelled on screen; only advances while walking, and keeps its value when stopped.
    var offset by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(walking) {
        if (walking) {
            var last = androidx.compose.runtime.withFrameNanos { it }
            while (true) {
                val now = androidx.compose.runtime.withFrameNanos { it }
                offset += (now - last) / 1_000_000_000f * WALK_SPEED
                last = now
            }
        }
    }

    Canvas(modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(16.dp))) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.78f

        drawRect(Brush.verticalGradient(listOf(palette.skyTop, palette.skyBottom)))
        if (night) drawStars(w, h) else drawSun(w, h)

        drawHills(palette.far, offset * 0.15f, h * 0.55f, h * 0.18f, w * 0.9f)
        if (regionId == "moonlit_coast") {
            drawSea(palette.mid, offset * 0.35f, groundY - h * 0.12f, w)
        } else {
            drawTrees(palette.mid, offset * 0.4f, groundY, h * 0.36f, w * 0.34f)
        }

        drawRect(palette.ground, Offset(0f, groundY), Size(w, h - groundY))
        drawPebbles(offset, groundY, w, h)
        drawTraveller(character, w * 0.38f, groundY, h, walking, offset)
    }
}

private const val WALK_SPEED = 90f // dp-like units per second; scaled by layer speeds above

private fun DrawScope.drawSun(w: Float, h: Float) {
    drawCircle(Color(0xFFFFF2B0), radius = h * 0.1f, center = Offset(w * 0.8f, h * 0.2f))
}

private fun DrawScope.drawStars(w: Float, h: Float) {
    val spots = listOf(0.1f to 0.15f, 0.25f to 0.3f, 0.4f to 0.1f, 0.55f to 0.25f, 0.7f to 0.12f, 0.9f to 0.3f)
    spots.forEach { (x, y) -> drawCircle(Color(0xFFFFF8D8), radius = 2.2f, center = Offset(w * x, h * y)) }
    drawCircle(Color(0xFFF4F1D0), radius = h * 0.08f, center = Offset(w * 0.8f, h * 0.2f))
}

private fun DrawScope.drawHills(color: Color, shift: Float, baseY: Float, height: Float, period: Float) {
    val path = Path()
    val start = -((shift * density) % period) - period
    path.moveTo(start, size.height)
    var x = start
    while (x < size.width + period) {
        path.quadraticTo(x + period / 2, baseY - height, x + period, baseY)
        x += period
    }
    path.lineTo(x, size.height)
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawTrees(color: Color, shift: Float, groundY: Float, treeH: Float, spacing: Float) {
    val first = -((shift * density) % spacing) - spacing
    var x = first
    var i = 0
    while (x < size.width + spacing) {
        val scale = 0.8f + 0.25f * ((i * 7) % 4) / 3f
        val th = treeH * scale
        drawRect(Color(0xFF6B4A2B), Offset(x - 3f, groundY - th * 0.3f), Size(6f, th * 0.3f))
        val crown = Path().apply {
            moveTo(x, groundY - th)
            lineTo(x + th * 0.3f, groundY - th * 0.25f)
            lineTo(x - th * 0.3f, groundY - th * 0.25f)
            close()
        }
        drawPath(crown, color)
        x += spacing
        i++
    }
}

private fun DrawScope.drawSea(color: Color, shift: Float, topY: Float, w: Float) {
    drawRect(color, Offset(0f, topY), Size(w, size.height - topY))
    val crest = Color.White.copy(alpha = 0.55f)
    var x = -((shift * density) % 60f) - 60f
    while (x < w + 60f) {
        drawRoundRect(crest, Offset(x, topY + 8f), Size(34f, 4f), CornerRadius(2f))
        x += 60f
    }
}

private fun DrawScope.drawPebbles(offset: Float, groundY: Float, w: Float, h: Float) {
    var x = -((offset * density) % 90f) - 90f
    var i = 0
    while (x < w + 90f) {
        val y = groundY + (h - groundY) * (0.3f + 0.2f * (i % 3))
        drawCircle(Color.White.copy(alpha = 0.25f), radius = 3f + (i % 2) * 2f, center = Offset(x, y))
        x += 90f
        i++
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
    val bob = if (walking) kotlin.math.abs(phase) * unit * 1.2f else 0f
    val bodyH = unit * 16f
    val bodyW = unit * 10f
    val bodyTop = groundY - unit * 8f - bodyH - bob

    // shadow
    drawOval(Color.Black.copy(alpha = 0.18f), Offset(x - bodyW * 0.7f, groundY - unit), Size(bodyW * 1.4f, unit * 2.4f))
    // legs
    drawLine(character.body, Offset(x - bodyW * 0.2f, bodyTop + bodyH), Offset(x - bodyW * 0.2f + phase * unit * 5f, groundY - unit), strokeWidth = unit * 2.4f)
    drawLine(character.body, Offset(x + bodyW * 0.2f, bodyTop + bodyH), Offset(x + bodyW * 0.2f - phase * unit * 5f, groundY - unit), strokeWidth = unit * 2.4f)
    // cape for the last look
    if (character.look == Look.CAPE) {
        val cape = Path().apply {
            moveTo(x - bodyW * 0.5f, bodyTop + unit * 2f)
            lineTo(x - bodyW * 1.3f - phase * unit * 2f, bodyTop + bodyH)
            lineTo(x - bodyW * 0.3f, bodyTop + bodyH)
            close()
        }
        drawPath(cape, character.accent)
    }
    // body and head
    drawRoundRect(character.body, Offset(x - bodyW / 2, bodyTop), Size(bodyW, bodyH), CornerRadius(unit * 3f))
    val headC = Offset(x + unit * 0.5f, bodyTop - unit * 4f)
    drawCircle(character.accent, radius = unit * 5.2f, center = headC)
    // ears / leaf
    when (character.look) {
        Look.FOX, Look.CAT -> {
            for (dx in listOf(-1f, 1f)) {
                val ear = Path().apply {
                    moveTo(headC.x + dx * unit * 1.5f, headC.y - unit * 4f)
                    lineTo(headC.x + dx * unit * 4.8f, headC.y - unit * 8f)
                    lineTo(headC.x + dx * unit * 5.2f, headC.y - unit * 2.5f)
                    close()
                }
                drawPath(ear, character.body)
            }
        }
        Look.ELF -> drawOval(character.body, Offset(headC.x - unit, headC.y - unit * 9f), Size(unit * 4f, unit * 6f))
        Look.CAPE -> Unit
    }
    // eye
    drawCircle(Color(0xFF2B2B2B), radius = unit * 0.8f, center = Offset(headC.x + unit * 2.2f, headC.y - unit * 0.4f))
}
