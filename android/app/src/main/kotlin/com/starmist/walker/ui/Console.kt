package com.starmist.walker.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/*
 * The control console: what slides up over the dials when the player pulls it up from the bottom edge. It is made like the rest of the panel: the same stitched
 * leather strip with its brass rivets, and brass fittings: a name plate, three big keys with jewel lamps that switch what the map shows, the compass with the
 * heading written out, and the knurled knob that syncs the steps.
 */

private val LAMP_NAMES = Color(0xFFF2A93B)
private val LAMP_ROUTE = Color(0xFFE04B3F)
private val LAMP_REGIONS = Color(0xFF3B9BFF)

/** The north / north-east / ... name of a heading in degrees clockwise from north. */
internal fun compassName(deg: Float): String = listOf("北", "東北", "東", "東南", "南", "西南", "西", "西北")[(((deg % 360f + 360f) % 360f + 22.5f) / 45f).toInt() % 8]

@Composable
internal fun ControlPanel(layers: MapLayers, onLayers: (MapLayers) -> Unit, heading: Float, lastSync: Long?, onSync: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth()) {
        Canvas(Modifier.matchParentSize()) { drawStrip() }
        Column(
            Modifier.fillMaxSize().padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween,
        ) {
            NamePlate("操控台")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KeyCap(KeyKind.NAMES, "地名", layers.names, LAMP_NAMES, Modifier.weight(1f)) { onLayers(layers.copy(names = !layers.names)) }
                KeyCap(KeyKind.ROUTE, "路線", layers.route, LAMP_ROUTE, Modifier.weight(1f)) { onLayers(layers.copy(route = !layers.route)) }
                KeyCap(KeyKind.REGIONS, "區域", layers.regions, LAMP_REGIONS, Modifier.weight(1f)) { onLayers(layers.copy(regions = !layers.regions)) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Compass(heading, Modifier.size(58.dp))
                    Column {
                        Text("航向", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Vintage.brassDark)
                        Text("${compassName(heading)} ${heading.toInt()}°", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Vintage.ink)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("同步步數", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Vintage.brassDark)
                        Text(lastSync?.let { formatTime(it) } ?: "尚未同步", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Vintage.ink)
                    }
                    BrassKnob(onSync, Modifier.size(54.dp))
                }
            }
        }
    }
}

/** A brass name plate between two engraved rules. */
@Composable
private fun NamePlate(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.width(56.dp).height(1.5.dp).background(Vintage.brassDark.copy(alpha = 0.8f)))
        val shape = RoundedCornerShape(7.dp)
        Box(
            Modifier.clip(shape).background(Brush.verticalGradient(listOf(Vintage.brassLight, Vintage.brass, Vintage.brassDark))).border(1.1.dp, Vintage.ink, shape)
                .padding(horizontal = 16.dp, vertical = 2.dp),
        ) {
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Vintage.ink, letterSpacing = 4.sp)
        }
        Box(Modifier.width(56.dp).height(1.5.dp).background(Vintage.brassDark.copy(alpha = 0.8f)))
    }
}

private enum class KeyKind { NAMES, ROUTE, REGIONS }

/**
 * A big brass key with a bevelled edge and a jewel lamp above its drawn symbol and its name. Lit, the key stands out and the lamp glows;
 * off, it sits pressed in with a dark lamp.
 */
@Composable
private fun KeyCap(kind: KeyKind, text: String, on: Boolean, lamp: Color, modifier: Modifier, onClick: () -> Unit) {
    val glow by animateFloatAsState(if (on) 1f else 0f, spring(stiffness = Spring.StiffnessMediumLow), label = "lamp")
    val shape = RoundedCornerShape(15.dp)
    Box(modifier.height(74.dp).clip(shape).clickable(role = Role.Switch, onClick = onClick), contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.matchParentSize()) {
            val r = 15.dp.toPx()
            val w = size.width
            val h = size.height
            // the face, brighter when lit
            drawRoundRect(
                Brush.verticalGradient(listOf(lerp(Vintage.brass, Vintage.brassLight, glow), lerp(Vintage.brassDark, Vintage.brass, glow))),
                cornerRadius = CornerRadius(r),
            )
            // pressed in: a shadow along the top edge
            drawRoundRect(
                Brush.verticalGradient(listOf(Vintage.ink.copy(alpha = 0.38f * (1f - glow)), Color.Transparent), endY = 16.dp.toPx()),
                cornerRadius = CornerRadius(r),
            )
            // the bevel: a light edge on top, a dark one underneath
            drawLine(Color.White.copy(alpha = 0.18f + 0.38f * glow), Offset(r, 3.dp.toPx()), Offset(w - r, 3.dp.toPx()), 1.6.dp.toPx(), StrokeCap.Round)
            drawLine(Vintage.ink.copy(alpha = 0.30f), Offset(r, h - 3.dp.toPx()), Offset(w - r, h - 3.dp.toPx()), 1.6.dp.toPx(), StrokeCap.Round)
            drawRoundRect(Vintage.ink, cornerRadius = CornerRadius(r), style = Stroke(1.8.dp.toPx()))
            // the jewel lamp, with its glow
            val c = Offset(w / 2f, 13.dp.toPx())
            if (glow > 0.01f) drawCircle(Brush.radialGradient(listOf(lamp.copy(alpha = 0.6f * glow), Color.Transparent), center = c, radius = 17.dp.toPx()), 17.dp.toPx(), c)
            drawCircle(lerp(Color(0xFF3A2E22), lamp, glow), 5.dp.toPx(), c)
            drawCircle(Color.White.copy(alpha = 0.25f + 0.6f * glow), 1.7.dp.toPx(), Offset(c.x - 1.6.dp.toPx(), c.y - 1.6.dp.toPx()))
            drawCircle(Vintage.ink, 5.dp.toPx(), c, style = Stroke(1.1.dp.toPx()))
        }
        Column(Modifier.padding(top = 25.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val ink = Vintage.ink.copy(alpha = 0.55f + 0.45f * glow)
            Canvas(Modifier.size(22.dp)) { keySymbol(kind, ink) }
            Spacer(Modifier.height(1.dp))
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.Black, color = ink)
        }
    }
}

/** The small symbol on a key, drawn in a 24-unit square: a map pin, a winding path, an outlined region. */
private fun DrawScope.keySymbol(kind: KeyKind, color: Color) {
    val s = size.minDimension / 24f
    fun o(x: Float, y: Float) = Offset(x * s, y * s)
    val line = Stroke(2.1f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (kind) {
        KeyKind.NAMES -> {
            drawCircle(color, 5.6f * s, o(12f, 9f), style = line)
            drawLine(color, o(7.4f, 12.8f), o(12f, 21.5f), 2.1f * s, StrokeCap.Round)
            drawLine(color, o(16.6f, 12.8f), o(12f, 21.5f), 2.1f * s, StrokeCap.Round)
            drawCircle(color, 1.8f * s, o(12f, 9f))
        }
        KeyKind.ROUTE -> {
            val path = Path().apply {
                moveTo(4f * s, 19f * s)
                cubicTo(11f * s, 19f * s, 5f * s, 11f * s, 12f * s, 11f * s)
                cubicTo(19f * s, 11f * s, 13f * s, 5f * s, 20f * s, 5f * s)
            }
            drawPath(path, color, style = line)
            drawCircle(color, 2.3f * s, o(4f, 19f))
            drawCircle(color, 2.3f * s, o(20f, 5f))
        }
        KeyKind.REGIONS -> {
            val shape = Path().apply {
                moveTo(12f * s, 3f * s); lineTo(20f * s, 8f * s); lineTo(19f * s, 17f * s); lineTo(11f * s, 21.5f * s); lineTo(4f * s, 16f * s); lineTo(5f * s, 7f * s); close()
            }
            drawPath(shape, color, style = Stroke(2.0f * s, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.6f * s, 2.4f * s))))
            drawCircle(color, 1.9f * s, o(12f, 12.5f))
        }
    }
}

/** The knob that syncs the steps: brass with a knurled rim, like the winder of a watch. */
@Composable
private fun BrassKnob(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.clip(CircleShape).clickable(role = Role.Button, onClickLabel = "同步", onClick = onClick), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val c = center
            drawCircle(Brush.radialGradient(listOf(Vintage.brassDark, Vintage.ink), center = c, radius = r), r)
            // the knurling: short ridges all round the rim
            val teeth = 30
            for (i in 0 until teeth) {
                val a = (i * 2.0 * Math.PI / teeth).toFloat()
                val inner = Offset(c.x + cos(a) * r * 0.86f, c.y + sin(a) * r * 0.86f)
                val outer = Offset(c.x + cos(a) * r * 0.99f, c.y + sin(a) * r * 0.99f)
                drawLine(Vintage.brassLight.copy(alpha = 0.85f), inner, outer, 1.6.dp.toPx(), StrokeCap.Round)
            }
            drawCircle(Brush.radialGradient(listOf(Vintage.brassLight, Vintage.brass, Vintage.brassDark), center = Offset(c.x - r * 0.22f, c.y - r * 0.25f), radius = r * 0.95f), r * 0.78f, c)
            drawCircle(Vintage.ink.copy(alpha = 0.55f), r * 0.78f, c, style = Stroke(1.1.dp.toPx()))
            drawCircle(Vintage.ink.copy(alpha = 0.3f), r * 0.58f, c, style = Stroke(0.9.dp.toPx()))
            drawCircle(Vintage.ink, r, c, style = Stroke(1.8.dp.toPx()))
        }
        Icon(Icons.Filled.Refresh, contentDescription = "同步", tint = Vintage.ink, modifier = Modifier.size(24.dp))
    }
}

/** The brass tab on the bottom edge of the panel: pull it up, or tap it, for the console; down for the dials again. */
@Composable
internal fun GripTab(open: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(topStart = 13.dp, topEnd = 13.dp)
    Box(
        modifier.size(width = 72.dp, height = 20.dp).clip(shape).background(Brush.verticalGradient(listOf(Vintage.brassLight, Vintage.brass, Vintage.brassDark)))
            .border(1.3.dp, Vintage.ink, shape).clickable(role = Role.Button, onClickLabel = if (open) "收起操控台" else "打開操控台", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(width = 20.dp, height = 8.dp)) {
            val path = Path().apply {
                if (open) { moveTo(0f, 0f); lineTo(size.width / 2f, size.height); lineTo(size.width, 0f) }
                else { moveTo(0f, size.height); lineTo(size.width / 2f, 0f); lineTo(size.width, size.height) }
            }
            drawPath(path, Vintage.ink, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
