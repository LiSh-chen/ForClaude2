package com.starmist.walker.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starmist.core.route.PostcardFact
import com.starmist.core.route.PostcardTowns
import com.starmist.core.route.Traveler
import kotlin.math.PI
import kotlin.math.sin

private val CARD_WHITE = Color(0xFFFBF8EE)
private val BACK_PAPER = Color(0xFFF1E6C8)
private val POSTMARK = Color(0xFF2E3F5C)

/**
 * A postcard as a thing you hold: front is the picture on a printed white border, a little turned and worn at the corners,
 * with a line of greeting under it; the back (tap to turn it over) has the message on the left, a perforated stamp with the
 * sender on it, a round postmark and the address lines on the right.
 */
@Composable
internal fun PostcardCard(t: TownArea, sender: Traveler, fact: PostcardFact?, modifier: Modifier = Modifier) {
    var back by remember { mutableStateOf(false) }
    val turn by animateFloatAsState(if (back) 180f else 0f, tween(550), label = "turn")
    val density = LocalDensity.current.density
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(CARD_RATIO).clickable { back = !back }
                .graphicsLayer { rotationY = turn; cameraDistance = 14f * density },
        ) {
            if (turn <= 90f) PostcardFront(t, fact, Modifier.fillMaxSize())
            else PostcardBack(t, sender, fact, Modifier.fillMaxSize().graphicsLayer { rotationY = 180f })
        }
        Text(tr("點一下翻面"), fontFamily = QUILL, fontSize = 12.sp, color = Vintage.inkSoft)
    }
}

/** Width over height of a postcard as drawn here: the picture 3:2 plus the border and the greeting under it. */
internal const val CARD_RATIO = 1.22f

@Composable
internal fun PostcardFront(t: TownArea, fact: PostcardFact?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(3.dp)
    Box(modifier.padding(6.dp).rotate(-1.2f).softShadow(Lift.Lifted, 3.dp).background(CARD_WHITE, shape).paperFibers(t.name.hashCode(), 90, light = Color.White).drawBehind { corners() }.border(0.6.dp, Color(0xFFD9CFB3), shape)) {
        Column(Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PostcardPicture(t, fact?.art, Modifier.fillMaxWidth().aspectRatio(1.5f))
            Row(Modifier.fillMaxWidth().weight(1f, fill = false), verticalAlignment = Alignment.CenterVertically) {
                Text(trf("來自{0}的問候", t.name), fontFamily = QUILL, fontSize = 17.sp, fontWeight = FontWeight.Black, color = INK_RED, modifier = Modifier.weight(1f), maxLines = 1)
                Text(t.county, fontFamily = QUILL, fontSize = 12.sp, color = Vintage.inkSoft)
            }
        }
    }
}

/** Worn corners: a faint lighter triangle where the card has been handled. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.corners() {
    val s = 9.dp.toPx()
    val c = Color(0xFFE7DCC0).copy(alpha = 0.8f)
    fun tri(x: Float, y: Float, dx: Float, dy: Float) = drawPath(Path().apply { moveTo(x, y); lineTo(x + dx * s, y); lineTo(x, y + dy * s); close() }, c)
    tri(0f, 0f, 1f, 1f); tri(size.width, 0f, -1f, 1f); tri(0f, size.height, 1f, -1f); tri(size.width, size.height, -1f, -1f)
}

@Composable
internal fun PostcardBack(t: TownArea, sender: Traveler, fact: PostcardFact?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(3.dp)
    Box(modifier.padding(6.dp).softShadow(Lift.Lifted, 3.dp).background(Brush.verticalGradient(listOf(BACK_PAPER, Color(0xFFE9DCB8))), shape).paperFibers(t.name.hashCode() + 5, 220, light = Color(0xFFFFF8E2)).border(0.6.dp, Color(0xFFCDBE96), shape)) {
        Row(Modifier.fillMaxSize().padding(10.dp)) {
            // the message, written on ruled lines
            Column(Modifier.weight(1.25f).fillMaxHeight().padding(end = 8.dp)) {
                PostcardTowns.lines(sender, fact?.text ?: "").forEach { Text(it, fontFamily = QUILL, fontSize = 12.sp, lineHeight = 19.sp, color = SEPIA) }
            }
            // the line down the middle
            Box(Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF8A6B3A).copy(alpha = 0.45f)))
            Box(Modifier.weight(1f).fillMaxHeight().padding(start = 8.dp)) {
                Stamp(sender, Modifier.align(Alignment.TopEnd))
                Postmark(t, Modifier.align(Alignment.TopEnd).padding(end = 34.dp, top = 14.dp))
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(3) { Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF5A4630).copy(alpha = 0.4f))) }
                }
            }
        }
    }
}

/** A stamp with a perforated edge, the sender's portrait in it. */
@Composable
private fun Stamp(sender: Traveler, modifier: Modifier = Modifier) {
    Box(
        modifier.size(width = 46.dp, height = 56.dp).drawBehind {
            drawRect(Color(0xFFFBF3D9))
            val r = 2.2.dp.toPx()
            val gap = 7.dp.toPx()
            var x = gap / 2
            while (x < size.width) { drawCircle(BACK_PAPER, r, Offset(x, 0f)); drawCircle(BACK_PAPER, r, Offset(x, size.height)); x += gap }
            var y = gap / 2
            while (y < size.height) { drawCircle(BACK_PAPER, r, Offset(0f, y)); drawCircle(BACK_PAPER, r, Offset(size.width, y)); y += gap }
        }.padding(5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxSize().border(1.dp, Color(0xFFB3202A).copy(alpha = 0.7f)), contentAlignment = Alignment.Center) { TravelerAvatar(sender.id, 30.dp) }
    }
}

/** A round postmark with the place in it, and wavy cancelling lines running out of it across the stamp. */
@Composable
private fun Postmark(t: TownArea, modifier: Modifier = Modifier) {
    Box(modifier.size(width = 86.dp, height = 56.dp).rotate(-12f), contentAlignment = Alignment.CenterStart) {
        Canvas(Modifier.fillMaxSize()) {
            val ink = POSTMARK.copy(alpha = 0.62f)
            val c = Offset(size.height / 2, size.height / 2)
            drawCircle(ink, size.height * 0.46f, c, style = Stroke(1.6.dp.toPx()))
            drawCircle(ink, size.height * 0.36f, c, style = Stroke(0.8.dp.toPx()))
            for (k in 0 until 4) {
                val y = size.height * (0.22f + k * 0.18f)
                val p = Path().apply {
                    moveTo(size.height * 0.95f, y)
                    var x = size.height * 0.95f
                    while (x < size.width) { x += 3.dp.toPx(); lineTo(x, y + sin(x / 3.dp.toPx() * PI.toFloat() / 1.5f) * 2.dp.toPx()) }
                }
                drawPath(p, ink, style = Stroke(1.1.dp.toPx()))
            }
        }
        Column(Modifier.size(48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(t.county, fontSize = 7.sp, color = POSTMARK.copy(alpha = 0.75f), maxLines = 1, textAlign = TextAlign.Center)
            Text(t.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = POSTMARK.copy(alpha = 0.75f), maxLines = 1, textAlign = TextAlign.Center)
        }
    }
}
