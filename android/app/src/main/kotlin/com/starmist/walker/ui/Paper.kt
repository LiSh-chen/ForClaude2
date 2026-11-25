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
import androidx.compose.ui.graphics.Path
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

/** The notebook page on its desk: [content] is drawn on a page with a shadow, with a strip of desk showing at the sides. */
@Composable
internal fun PaperPage(padding: PaddingValues, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(Modifier.fillMaxSize().background(DESK).padding(padding).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Box(Modifier.fillMaxSize().softShadow(Lift.Raised, 14.dp).background(Vintage.parchment, shape).clip(shape)) { content() }
    }
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
    val brush = if (small) Brush.horizontalGradient(listOf(Vintage.mossLight, Color(0xFFD5E2C8))) else Brush.horizontalGradient(listOf(Vintage.brassLight, Color(0xFFF0DCA0)))
    val fold = if (small) Vintage.moss else Vintage.brassDark
    Row(
        modifier.fillMaxWidth().padding(start = 9.dp)
            .softShadow(Lift.Raised, 8.dp)
            .drawBehind {
                val f = 9.dp.toPx()
                drawPath(Path().apply { moveTo(-f, size.height); lineTo(0f, size.height); lineTo(0f, size.height - f); close() }, fold)
            }
            .background(brush, shape)
            .border(0.8.dp, if (small) Vintage.moss.copy(alpha = 0.6f) else Vintage.brassDark.copy(alpha = 0.7f), shape)
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
    Box(
        modifier.softShadow(Lift.Raised, 20.dp)
            .background(Brush.verticalGradient(listOf(Vintage.brassLight, Vintage.brass)), shape)
            .border(1.dp, Vintage.brassDark, shape)
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
                Box(
                    Modifier.size(42.dp).background(Brush.verticalGradient(listOf(Vintage.brassLight, Vintage.brass)), CircleShape).border(1.dp, Vintage.brassDark, CircleShape),
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
    Box(
        modifier.softShadow(Lift.Raised, 20.dp).background(Brush.verticalGradient(listOf(Vintage.brassLight, Vintage.brass)), shape).border(1.dp, Vintage.brassDark, shape)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Vintage.ink) }
}

/** Two or three choices in a pill: the chosen one is brass and raised, the others are written on the paper. */
@Composable
internal fun <T> PillTabs(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier.fillMaxWidth().background(Vintage.edge.copy(alpha = 0.18f), shape).border(0.8.dp, Vintage.edge.copy(alpha = 0.5f), shape).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            val m = Modifier.weight(1f).clip(shape)
                .let { if (on) it.background(Brush.verticalGradient(listOf(Vintage.brassLight, Vintage.brass)), shape) else it }
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
