package com.starmist.walker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Layers of paper. A page of the app is a notebook page lying on a desk; cards are paper pasted onto the page; buttons and
 * badges are brass and ink on top of that. Each layer throws a soft warm-brown shadow, so that the eye can tell which is in front.
 */

/** The desk the notebook page lies on, a little darker than the paper. */
internal val DESK = Color(0xFFCDB88B)

private val SHADOW = Color(0xFF4A3216)

/** The four parts of the app, each a different kind of paper in the same notebook, with its own colour of brass, ribbon and button. */
internal enum class TabLook(val light: Color, val mid: Color, val dark: Color, val mark: String) {
    Today(Color(0xFFE6C874), Color(0xFFB8892D), Color(0xFF7C5A1B), "今"),
    Collection(Color(0xFFB9D6A6), Color(0xFF86B473), Color(0xFF3F6B34), "藏"),
    Stats(Color(0xFFC3D0EE), Color(0xFF8CA2DA), Color(0xFF2C3F85), "計"),
    Settings(Color(0xFFEDBBA3), Color(0xFFD08968), Color(0xFF7A3520), "設"),
}

/** Which part of the app the composables below are in (the colours of buttons and ribbons follow it). */
internal val LocalTab = androidx.compose.runtime.staticCompositionLocalOf { TabLook.Today }

/** Room kept free at the right end of the big ribbons, where the bookmark hangs (0 outside a page). */
internal val LocalBookmarkInset = androidx.compose.runtime.staticCompositionLocalOf { 0.dp }

/** How high a piece of paper is held above what is under it. */
internal enum class Lift { Rest, Raised, Lifted }

/** A soft warm shadow made of a few widening translucent layers (no platform shadow, so it looks the same everywhere). */
internal fun Modifier.softShadow(lift: Lift, corner: Dp = 10.dp): Modifier = if (lift == Lift.Rest) this else drawBehind {
    val (dy, spread, layers, alpha) = if (lift == Lift.Raised) listOf(2f, 6f, 6f, 0.035f) else listOf(6f, 14f, 9f, 0.03f)
    val n = layers.toInt()
    for (i in n downTo 1) {
        val s = (spread * i / n).dp.toPx()
        drawRoundRect(SHADOW.copy(alpha = alpha), Offset(-s, -s + dy.dp.toPx()), Size(size.width + 2 * s, size.height + 2 * s), CornerRadius(corner.toPx() + s))
    }
}

/** The notebook page on its desk (the desk is the same everywhere): [look] says what paper the page is made of. */
@Composable
internal fun PaperPage(padding: PaddingValues, look: TabLook = TabLook.Today, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    androidx.compose.runtime.CompositionLocalProvider(LocalTab provides look, LocalBookmarkInset provides 34.dp) {
        Box(Modifier.fillMaxSize().background(DESK).padding(padding).padding(horizontal = 12.dp, vertical = 10.dp)) {
            Box(Modifier.fillMaxSize().softShadow(Lift.Raised, 14.dp).background(pageColor(look), shape).clip(shape).drawBehind { drawPaper(look) }) { content() }
            Bookmark(look, Modifier.align(Alignment.TopEnd).padding(end = 22.dp))
        }
    }
}

private fun pageColor(look: TabLook) = when (look) {
    TabLook.Today -> Vintage.parchment
    TabLook.Collection -> Color(0xFFF2EBCF)
    TabLook.Stats -> Color(0xFFF6F3E6)
    TabLook.Settings -> Color(0xFFDDC28F)
}

/** What is printed or fibred into the page: contour lines (today), dots (scrapbook), squared paper with a red margin (stats), kraft fibres and stitches (settings). */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPaper(look: TabLook) {
    val w = size.width
    val h = size.height
    when (look) {
        TabLook.Today -> {
            val line = Vintage.edge.copy(alpha = 0.16f)
            for (k in 0 until 7) {
                val y0 = h * (0.08f + k * 0.15f)
                val p = Path().apply {
                    moveTo(0f, y0)
                    var x = 0f
                    while (x < w) { cubicTo(x + w * 0.1f, y0 - 14.dp.toPx() * (1 + k % 3) * 0.4f, x + w * 0.2f, y0 + 16.dp.toPx() * 0.5f, x + w * 0.3f, y0 + (k % 2 * 6 - 3).dp.toPx()); x += w * 0.3f }
                }
                drawPath(p, line, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
            }
        }
        TabLook.Collection -> {
            val gap = 16.dp.toPx()
            val dot = Color(0xFF6E8F5C).copy(alpha = 0.22f)
            var y = gap
            while (y < h) { var x = gap; while (x < w) { drawCircle(dot, 1.1.dp.toPx(), Offset(x, y)); x += gap }; y += gap }
        }
        TabLook.Stats -> {
            val gap = 18.dp.toPx()
            val blue = Color(0xFF5A78C8)
            var i = 0
            var x = 0f
            while (x < w) { drawLine(blue.copy(alpha = if (i % 5 == 0) 0.2f else 0.1f), Offset(x, 0f), Offset(x, h), 0.8.dp.toPx()); x += gap; i++ }
            i = 0
            var y = 0f
            while (y < h) { drawLine(blue.copy(alpha = if (i % 5 == 0) 0.2f else 0.1f), Offset(0f, y), Offset(w, y), 0.8.dp.toPx()); y += gap; i++ }
            drawLine(Color(0xFFD23B3B).copy(alpha = 0.45f), Offset(9.dp.toPx(), 0f), Offset(9.dp.toPx(), h), 1.2.dp.toPx())
        }
        TabLook.Settings -> {
            val rnd = kotlin.random.Random(7)
            repeat(420) {
                val x = rnd.nextFloat() * w
                val y = rnd.nextFloat() * h
                val len = (4 + rnd.nextInt(9)).dp.toPx()
                val a = rnd.nextFloat() * 3.14f
                val dark = rnd.nextBoolean()
                drawLine((if (dark) Color(0xFF7A5528) else Color(0xFFF4E3BA)).copy(alpha = if (dark) 0.14f else 0.3f), Offset(x, y), Offset(x + kotlin.math.cos(a) * len, y + kotlin.math.sin(a) * len), 0.8.dp.toPx())
            }
            val inset = 6.dp.toPx()
            drawRoundRect(Color(0xFF6B4A22).copy(alpha = 0.5f), Offset(inset, inset), Size(w - 2 * inset, h - 2 * inset), CornerRadius(10.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(1.2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx()))))
        }
    }
}

/** A ribbon bookmark hanging from the top of the page, in the colour of this part of the app. */
@Composable
private fun Bookmark(look: TabLook, modifier: Modifier = Modifier) {
    Box(
        modifier.size(width = 26.dp, height = 40.dp).drawBehind {
            val p = Path().apply { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width, size.height); lineTo(size.width / 2, size.height - 9.dp.toPx()); lineTo(0f, size.height); close() }
            drawPath(p, SHADOW.copy(alpha = 0.18f), style = androidx.compose.ui.graphics.drawscope.Fill)
            translate(0f, 0f) { drawPath(p, Brush.verticalGradient(listOf(look.mid, look.dark))) }
        },
        contentAlignment = Alignment.TopCenter,
    ) { Text(look.mark, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, modifier = Modifier.padding(top = 8.dp)) }
}

/** A piece of paper pasted onto the page; with [onClick] the whole card can be pressed. */
@Composable
internal fun PaperCard(
    modifier: Modifier = Modifier, lift: Lift = Lift.Raised, corner: Dp = 10.dp, tint: Color = Vintage.parchmentLight, onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    Column(
        modifier.softShadow(lift, corner)
            .background(Brush.verticalGradient(listOf(tint, Color(0xFFF0E2BC))), shape)
            .border(0.8.dp, Vintage.edge.copy(alpha = 0.7f), shape)
            .let { if (onClick != null) it.clip(shape).clickable(onClick = onClick) else it },
        content = content,
    )
}

/** A heading on a ribbon, folded round the edge of the page; [small] is the quieter green one for the groups under it. */
@Composable
internal fun RibbonHeader(title: String, modifier: Modifier = Modifier, small: Boolean = false, onClick: (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null) {
    val shape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 14.dp, bottomEnd = 14.dp)
    val look = LocalTab.current
    val brush = if (small) Brush.horizontalGradient(listOf(Vintage.mossLight, Color(0xFFD5E2C8))) else Brush.horizontalGradient(listOf(look.light, look.light.copy(alpha = 0.7f).compositeOver(Color.White)))
    val fold = if (small) Vintage.moss else look.dark
    Row(
        modifier.fillMaxWidth().padding(start = 9.dp, end = if (small) 0.dp else LocalBookmarkInset.current)
            .softShadow(Lift.Raised, 8.dp)
            .drawBehind {
                val f = 9.dp.toPx()
                drawPath(Path().apply { moveTo(-f, size.height); lineTo(0f, size.height); lineTo(0f, size.height - f); close() }, fold)
            }
            .background(brush, shape)
            .border(0.8.dp, if (small) Vintage.moss.copy(alpha = 0.6f) else look.dark.copy(alpha = 0.7f), shape)
            .let { if (onClick != null) it.clip(shape).clickable(onClick = onClick) else it }
            .padding(horizontal = 12.dp, vertical = if (small) 6.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = if (small) 15.sp else 17.sp, color = Vintage.ink, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** A raised brass button in the shape of a pill. */
@Composable
internal fun BrassPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    val look = LocalTab.current
    Box(
        modifier.softShadow(Lift.Raised, 20.dp)
            .background(Brush.verticalGradient(listOf(look.light, look.mid)), shape)
            .border(1.dp, look.dark, shape)
            .clip(shape).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Vintage.ink) }
}

/** A small red dot on the corner of something that has news. */
@Composable
internal fun RedDot(modifier: Modifier = Modifier) {
    Box(modifier.size(13.dp).background(Color(0xFFD23B3B), CircleShape).border(2.dp, Vintage.parchmentLight, CircleShape))
}

/** A card that opens a part of the app: a round brass emblem, a title, a line under it, and a red dot when there is news. */
@Composable
internal fun EntryButton(icon: String, title: String, subtitle: String, badge: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier) {
        PaperCard(Modifier.fillMaxWidth(), Lift.Raised, 12.dp, onClick = onClick) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val look = LocalTab.current
                Box(
                    Modifier.size(42.dp).background(Brush.verticalGradient(listOf(look.light, look.mid)), CircleShape).border(1.dp, look.dark, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text(icon, fontSize = 20.sp) }
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Vintage.ink, maxLines = 1)
                    Text(subtitle, fontSize = 11.sp, color = Vintage.inkSoft, maxLines = 1)
                }
            }
        }
        if (badge) RedDot(Modifier.align(Alignment.TopEnd).offset(x = (-4).dp, y = 4.dp))
    }
}

/** A raised brass label (like [BrassPill] but not pressable), for what a quest gives. */
@Composable
internal fun BrassLabel(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    val look = LocalTab.current
    Box(
        modifier.softShadow(Lift.Raised, 20.dp).background(Brush.verticalGradient(listOf(look.light, look.mid)), shape).border(1.dp, look.dark, shape)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Vintage.ink) }
}

/** Two or three choices in a pill: the chosen one is brass and raised, the others are written on the paper. */
@Composable
internal fun <T> PillTabs(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    val look = LocalTab.current
    Row(
        modifier.fillMaxWidth().background(Vintage.edge.copy(alpha = 0.18f), shape).border(0.8.dp, Vintage.edge.copy(alpha = 0.5f), shape).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            val m = Modifier.weight(1f).clip(shape)
                .let { if (on) it.background(Brush.verticalGradient(listOf(look.light, look.mid)), shape) else it }
                .clickable { onSelect(value) }.padding(vertical = 7.dp)
            Box(m, contentAlignment = Alignment.Center) { Text(label, fontWeight = FontWeight.Bold, color = if (on) Vintage.ink else Vintage.inkSoft) }
        }
    }
}

/** The soft shadow a bar throws upward onto the page above it. */
internal fun Modifier.shadowAbove(): Modifier = drawBehind {
    val h = 10.dp.toPx()
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, SHADOW.copy(alpha = 0.16f)), startY = -h, endY = 0f), Offset(0f, -h), Size(size.width, h))
    drawRect(Vintage.edge.copy(alpha = 0.8f), Offset.Zero, Size(size.width, 1.dp.toPx()))
}

/** Short pale and dark fibres lying in the paper, the same every time (so a card does not shimmer when it is redrawn). */
internal fun Modifier.paperFibers(seed: Int, count: Int = 160, dark: Color = Color(0xFF8A6B3A), light: Color = Color.White): Modifier = drawBehind {
    val rnd = kotlin.random.Random(seed)
    repeat(count) {
        val x = rnd.nextFloat() * size.width
        val y = rnd.nextFloat() * size.height
        val len = (3 + rnd.nextInt(8)).dp.toPx()
        val a = rnd.nextFloat() * 3.14f
        val isDark = rnd.nextInt(3) == 0
        drawLine((if (isDark) dark.copy(alpha = 0.16f) else light.copy(alpha = 0.5f)), Offset(x, y), Offset(x + kotlin.math.cos(a) * len, y + kotlin.math.sin(a) * len), 0.7.dp.toPx())
    }
}
