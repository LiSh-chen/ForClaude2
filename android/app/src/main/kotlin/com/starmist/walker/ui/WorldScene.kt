package com.starmist.walker.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
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
    pickups: kotlinx.coroutines.flow.SharedFlow<Pickup>? = null,
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

    val art = rememberArt(
        when (character.look) {
            Look.FOX -> "traveller_fox"
            Look.ELF -> "traveller_elf"
            Look.CAT -> "traveller_cat"
            Look.CAPE -> "traveller_cape"
        },
    )

    // A slow breathing clock so the traveller is never frozen, even when standing still.
    val idle by rememberInfiniteTransition(label = "idle").animateFloat(
        0f, (2 * Math.PI).toFloat(),
        infiniteRepeatable(tween(2800, easing = LinearEasing)), label = "idle",
    )

    // Finds: each one plays a short pop-up, hop and fly-to-bag animation, one after another.
    var found by remember { mutableStateOf<Pickup?>(null) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(pickups) {
        pickups?.collect { pickup ->
            found = pickup
            progress.snapTo(0f)
            progress.animateTo(1f, tween(PICKUP_MS, easing = LinearEasing))
            found = null
        }
    }

    Box(modifier.fillMaxWidth().height(190.dp)) {
    Canvas(Modifier.fillMaxSize()) {
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

        val t = if (found != null) progress.value else -1f
        // The traveller hops with delight while the find hovers overhead.
        val lift = if (t in 0.2f..0.55f) sin((t - 0.2f) / 0.35f * Math.PI.toFloat()) * h * 0.09f else 0f
        if (art != null) drawTravellerArt(art, w * 0.4f, groundY, h, walking, offset, idle, lift)
        else drawTraveller(character, w * 0.4f, groundY - lift, h, walking, offset)

        found?.let { drawPickup(it.item, t, Offset(w * 0.4f, groundY), h, Offset(w - 22.dp.toPx(), 22.dp.toPx())) }
    }
    found?.let { pickup ->
        val t = progress.value
        val alpha = when {
            t < 0.15f -> 0f
            t < 0.3f -> (t - 0.15f) / 0.15f
            t > 0.85f -> (1f - t) / 0.15f
            else -> 1f
        }
        Text(
            pickup.line ?: "遇見了什麼……",
            Modifier.align(Alignment.TopCenter).padding(top = 8.dp, start = 12.dp, end = 12.dp).graphicsLayer { this.alpha = alpha },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Vintage.ink,
        )
        Text(
            "獲得 ${pickup.item.displayName}",
            Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).graphicsLayer { this.alpha = alpha },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Vintage.brassDark,
        )
    }
    }
}

private const val PICKUP_MS = 2200

/**
 * The find pops out of the ground ahead of the traveller, hovers with a sparkle while the traveller
 * hops, then arcs up into the bag in the corner. [t] runs 0..1 over the whole animation.
 */
private fun DrawScope.drawPickup(item: com.starmist.core.world.ItemType, t: Float, feet: Offset, h: Float, bag: Offset) {
    val start = Offset(feet.x + h * 0.32f, feet.y - h * 0.04f)
    val hover = Offset(feet.x + h * 0.10f, feet.y - h * 0.62f)
    val r = h * 0.085f
    val pos: Offset
    val scale: Float
    var alpha = 1f
    when {
        t < 0.2f -> { // pop out of the ground
            val k = t / 0.2f
            pos = Offset(start.x, start.y - k * h * 0.14f)
            scale = k * 1.25f
        }
        t < 0.6f -> { // rise to hover and bob
            val k = ((t - 0.2f) / 0.4f).coerceIn(0f, 1f)
            val ease = 1f - (1f - k) * (1f - k)
            pos = Offset(start.x + (hover.x - start.x) * ease, start.y - h * 0.14f + (hover.y - (start.y - h * 0.14f)) * ease + sin(k * 12f) * h * 0.012f)
            scale = 1.25f - 0.25f * ease
        }
        else -> { // fly to the bag
            val k = ((t - 0.6f) / 0.4f).coerceIn(0f, 1f)
            val ease = k * k
            pos = Offset(hover.x + (bag.x - hover.x) * ease, hover.y + (bag.y - hover.y) * ease - sin(k * Math.PI.toFloat()) * h * 0.15f)
            scale = 1f - 0.65f * ease
            alpha = 1f - 0.4f * k
        }
    }
    // Glow and sparkles around it while it hovers.
    if (t in 0.1f..0.65f) {
        val glow = sin(((t - 0.1f) / 0.55f) * Math.PI.toFloat())
        drawCircle(Vintage.brassLight.copy(alpha = 0.55f * glow), r * 2.1f * scale, pos)
        for (i in 0 until 6) {
            val a = i * Math.PI.toFloat() / 3f + t * 9f
            val d = r * (1.5f + 0.5f * sin(t * 20f + i))
            val p = Offset(pos.x + kotlin.math.cos(a) * d, pos.y + sin(a) * d)
            drawCircle(Vintage.brass.copy(alpha = glow), r * 0.12f, p)
        }
    }
    drawItemIcon(item, pos, r * scale.coerceAtLeast(0.01f))
    if (alpha < 1f) drawCircle(Vintage.parchment.copy(alpha = 1f - alpha), r * scale, pos) // fades into the page
}

/** The painted traveller, bobbing and leaning as it walks and breathing when it stands. */
private fun DrawScope.drawTravellerArt(
    art: androidx.compose.ui.graphics.ImageBitmap,
    x: Float,
    groundY: Float,
    h: Float,
    walking: Boolean,
    offset: Float,
    idle: Float,
    lift: Float,
) {
    val height = h * 0.68f
    val width = height * art.width / art.height
    val phase = sin(offset * 0.14f)
    val bob = if (walking) abs(phase) * h * 0.035f else 0f
    val tilt = if (walking) phase * 3.5f else sin(idle) * 0.8f
    val squash = if (walking) 1f - abs(phase) * 0.03f else 1f + sin(idle) * 0.012f
    val feet = Offset(x, groundY)

    val shadowW = width * (0.55f - (bob + lift) / h * 1.2f)
    drawOval(Color.Black.copy(alpha = 0.16f), Offset(x - shadowW / 2f, groundY - 3.dp.toPx()), Size(shadowW, 7.dp.toPx()))
    translate(0f, -(bob + lift)) {
        rotate(tilt, feet) {
            scale(1f, squash, feet) {
                drawImage(
                    art,
                    dstOffset = androidx.compose.ui.unit.IntOffset((x - width / 2f).toInt(), (groundY - height + 4.dp.toPx()).toInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(width.toInt(), height.toInt()),
                )
            }
        }
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
