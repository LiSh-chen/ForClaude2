package com.starmist.walker.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import kotlin.math.roundToInt
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * The control console under the dials: one piece with them, on the same strip, shown when the panel is pulled up. It is drawn with the same pen as the dials
 * (inked outlines, a wash of colour, hatching for shade) and laid out like the row of keys on a car's centre console: a lamp over each key, its picture in
 * the middle and its name under it, the compass and the sync knob at the two ends.
 */

private val LAMP_NAMES = Color(0xFFF2A93B)
private val LAMP_ROUTE = Color(0xFFE04B3F)
private val LAMP_REGIONS = Color(0xFF3B9BFF)
private val CELL = 50.dp

/** The north / north-east / ... name of a heading in degrees clockwise from north. */
internal fun compassName(deg: Float): String = listOf("北", "東北", "東", "東南", "南", "西南", "西", "西北")[(((deg % 360f + 360f) % 360f + 22.5f) / 45f).toInt() % 8]

/**
 * The console, as tall as the dials: on the left the three counters (steps today, kilometres in this township, kilometres in all), in the middle the compass,
 * on the right the buttons.
 */
@Composable
internal fun ConsoleDeck(
    layers: MapLayers, onLayers: (MapLayers) -> Unit, heading: Float, lastSync: Long?, onSync: () -> Unit,
    steps: Long, todayKm: Double, kcal: Double, townKm: Double?, walkedKm: Double, modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Counters(steps, todayKm, kcal, townKm, walkedKm, Modifier.weight(1.12f))
        Column(Modifier.width(78.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Compass(heading, Modifier.size(72.dp))
            Text("${compassName(heading)} ${heading.toInt()}°", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Vintage.ink)
            Text("航向", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Vintage.brassDark)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                KeyCell(KeyKind.NAMES, "地名", layers.names, LAMP_NAMES, 100) { onLayers(layers.copy(names = !layers.names)) }
                KeyCell(KeyKind.ROUTE, "路線", layers.route, LAMP_ROUTE, 200) { onLayers(layers.copy(route = !layers.route)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                KeyCell(KeyKind.REGIONS, "區域", layers.regions, LAMP_REGIONS, 300) { onLayers(layers.copy(regions = !layers.regions)) }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(role = Role.Button, onClickLabel = "同步", onClick = onSync)) {
                    Canvas(Modifier.size(CELL)) { Pen(this, size).knob(400) }
                    Text("同步", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Vintage.ink)
                }
            }
            Text(lastSync?.let { "上次同步 ${formatTime(it)}" } ?: "尚未同步", fontSize = 9.sp, color = Vintage.inkSoft)
        }
    }
}

/** The three counters, with the day's distance and energy under the steps. */
@Composable
private fun Counters(steps: Long, todayKm: Double, kcal: Double, townKm: Double?, walkedKm: Double, modifier: Modifier) {
    val tm = rememberTextMeasurer()
    val stepsRoll = rememberRolling(steps.toFloat(), "steps")
    val town = rememberRolling(((townKm ?: 0.0) * 10).toFloat(), "town")
    val total = rememberRolling((walkedKm * 10).toFloat(), "total")
    Column(modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
        CounterRow("今日步數", Color(0xFF2E7D4F), tm, stepsRoll, cells = 5, decimal = false)
        Text("${todayKm.twoDecimals()} 公里・${kcal.roundToInt()} 大卡", fontSize = 9.sp, lineHeight = 11.sp, fontWeight = FontWeight.Bold, color = Vintage.inkSoft)
        CounterRow("區域 km", Color(0xFFB3202A), tm, if (townKm != null) town else null, cells = 5, decimal = true)
        CounterRow("總里程 km", Color(0xFF6B4A24), tm, total, cells = 5, decimal = true)
    }
}

/** A counter with its name over it, the drums the same as on the dials. */
@Composable
private fun CounterRow(label: String, color: Color, tm: TextMeasurer, reading: Float?, cells: Int, decimal: Boolean) {
    Column {
        Text(label, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Black, color = color)
        Canvas(Modifier.fillMaxWidth().height(27.dp)) {
            val pen = Pen(this, size)
            // the pen's unit is the height of this strip, so the width is a few hundred units: its left edge is this far out
            val left = -(size.width - size.height) / 2f / (size.height / 100f)
            with(pen) { odometer(tm, left + 1f, 5f, reading, cells = cells, cw = 74f, ch = 90f, decimal = decimal) }
        }
    }
}

private enum class KeyKind { NAMES, ROUTE, REGIONS }

/** One key: its lamp lights (and the key stands out) while what it switches is shown on the map. */
@Composable
private fun KeyCell(kind: KeyKind, label: String, on: Boolean, lamp: Color, seed: Int, onClick: () -> Unit) {
    val glow by animateFloatAsState(if (on) 1f else 0f, spring(stiffness = Spring.StiffnessMediumLow), label = "lamp")
    Column(Modifier.clickable(role = Role.Switch, onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(CELL)) { Pen(this, size).key(kind, glow, lamp, seed) }
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Vintage.ink.copy(alpha = 0.6f + 0.4f * glow))
    }
}

/** The key itself, in the pen's 100 x 100 square. */
private fun Pen.key(kind: KeyKind, glow: Float, lamp: Color, seed: Int) {
    val body = roundRect(6f, 6f, 94f, 94f, 15f)
    // lit: pale and standing out; off: a darker paper, hatched as if pressed in
    wash(body, lerp(Color(0xFFB59A62), Color(0xFFFBEFC8), glow), 1f, 0f, 0f)
    if (glow < 0.98f) hatch(body, ink, 6f, 94f, 6f, 94f, gap = 4.2f, lean = 9f, alpha = 0.24f * (1f - glow))
    inked(body, seed, closed = true, width = line * 1.5f, calm = 0.3f)
    stroke(roundRect(13f, 13f, 87f, 87f, 10f), ink, seed + 7, line * 0.55f, closed = true, alpha = 0.4f, calm = 0.2f)
    // the lamp over the picture
    val c = p(50f, 22f)
    if (glow > 0.02f) wash(circle(c, 11f * u, 20), lamp, 0.30f * glow, 0f, 0f)
    wash(circle(c, 5.4f * u, 16), lerp(Color(0xFF4A3E32), lamp, glow), 1f, 0f, 0f)
    inked(circle(c, 5.4f * u, 16), seed + 9, closed = true, width = line * 0.9f, calm = 0.1f)
    d.drawCircle(Color.White.copy(alpha = 0.3f + 0.6f * glow), 1.5f * u, Offset(c.x - 1.7f * u, c.y - 1.7f * u))
    picture(kind, seed + 20, 0.55f + 0.45f * glow)
}

/** The key's picture: a map pin, a winding road, an outlined region. */
private fun Pen.picture(kind: KeyKind, seed: Int, a: Float) {
    val w = line * 1.7f
    when (kind) {
        KeyKind.NAMES -> {
            val c = p(50f, 55f)
            stroke(circle(c, 12f * u, 20), ink, seed, w, closed = true, alpha = a, calm = 0.3f)
            stroke(listOf(p(40.5f, 63f), p(50f, 82f)), ink, seed + 1, w, alpha = a, calm = 0.2f)
            stroke(listOf(p(59.5f, 63f), p(50f, 82f)), ink, seed + 2, w, alpha = a, calm = 0.2f)
            wash(circle(c, 3.6f * u, 10), ink, a, 0f, 0f)
        }
        KeyKind.ROUTE -> {
            val road = listOf(p(25f, 80f), p(39f, 80f), p(50f, 74f), p(54f, 64f), p(48f, 56f), p(52f, 48f), p(62f, 42f), p(76f, 40f))
            stroke(road, ink, seed, w, alpha = a, calm = 0.25f)
            wash(circle(p(25f, 80f), 4f * u, 10), ink, a, 0f, 0f)
            wash(circle(p(76f, 40f), 4f * u, 10), ink, a, 0f, 0f)
        }
        KeyKind.REGIONS -> {
            val hex = (0 until 6).map { val ang = PI / 3 * it - PI / 2; p(50f + (18f * cos(ang)).toFloat(), 59f + (18f * sin(ang)).toFloat()) }
            stroke(hex, ink, seed, w, closed = true, alpha = a, dash = dashed(this, 5f, 3.5f), calm = 0.2f)
            wash(circle(p(50f, 59f), 3.2f * u, 10), ink, a, 0f, 0f)
        }
    }
}

/** The sync knob: brass with a knurled rim, and the arrow that goes round. */
private fun Pen.knob(seed: Int) {
    for (i in 0 until 28) {
        val ang = 2.0 * PI * i / 28
        val cx = cos(ang).toFloat(); val sy = sin(ang).toFloat()
        stroke(listOf(p(50f + 42f * cx, 54f + 42f * sy), p(50f + 48f * cx, 54f + 48f * sy)), ink, seed + i, line, alpha = 0.85f, calm = 0f)
    }
    val face = circle(p(50f, 54f), 38f * u, 28)
    wash(face, Vintage.brass, 1f, 0f, 0f)
    wash(circle(p(44f, 48f), 22f * u, 20), Vintage.brassLight, 0.6f, 0f, 0f)
    inked(face, seed + 50, closed = true, width = line * 1.5f, calm = 0.3f)
    stroke(circle(p(50f, 54f), 46f * u, 28), ink, seed + 51, line * 0.8f, closed = true, alpha = 0.6f, calm = 0.2f)
    // the arrow going round
    val arc = (0..22).map { val ang = Math.toRadians(40.0 + it * 13.0); p(50f + (17f * cos(ang)).toFloat(), 54f + (17f * sin(ang)).toFloat()) }
    stroke(arc, ink, seed + 52, line * 1.8f, calm = 0.2f)
    val last = arc.last(); val dir = (last - arc[arc.size - 2]).let { it / it.getDistance() }
    val nor = Offset(-dir.y, dir.x)
    val head = listOf(last + dir * (6.5f * u), last - dir * (2f * u) + nor * (5f * u), last - dir * (2f * u) - nor * (5f * u))
    wash(head, ink, 1f, 0f, 0f)
}

/** The tab on the bottom edge of the panel, drawn like everything else on it: pull the panel up by it, or tap it; down for the dials again. */
@Composable
internal fun GripTab(open: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = 76.dp, height = 22.dp).clickable(role = Role.Button, onClickLabel = if (open) "收起操控台" else "打開操控台", onClick = onClick)) {
        val pen = Pen(this, size)
        with(pen) {
            // the unit is the tab's height, so the width is about 345 units: from -123 to 222
            val tab = roundRect(-123f, 0f, 222f, 150f, 48f)
            wash(tab, Vintage.brass, 1f, 0f, 0f)
            wash(roundRect(-100f, 6f, 200f, 60f, 30f), Vintage.brassLight, 0.55f, 0f, 0f)
            inked(tab, 31, closed = true, width = line * 1.4f, calm = 0.25f)
            val chevron = if (open) listOf(p(40f, 36f), p(50f, 58f), p(60f, 36f)) else listOf(p(40f, 58f), p(50f, 36f), p(60f, 58f))
            stroke(chevron, ink, 32, line * 1.9f, calm = 0.1f)
        }
    }
}
