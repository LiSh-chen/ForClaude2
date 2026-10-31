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
    steps: Long, todayKm: Double, kcal: Double, townKm: Double?, townLeft: Double?, walkedKm: Double, totalKm: Double, modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Notes(steps, todayKm, kcal, townKm, townLeft, walkedKm, totalKm, Modifier.weight(1.12f))
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

private val NOTE_INK = Color(0xFF23407A)   // fountain-pen blue-black
private val NOTE_SIZE = 12.sp
private val NOTE_FONT = androidx.compose.ui.text.font.FontFamily.Cursive

/**
 * The three notes, written in one hand at one size: what was walked today, how far through this township, how far round the island - each with a small
 * drawing and a title in the colour of its needle on the dials, and under it what is walked ("已走") and what is left ("未走").
 */
@Composable
private fun Notes(steps: Long, todayKm: Double, kcal: Double, townKm: Double?, townLeft: Double?, walkedKm: Double, totalKm: Double, modifier: Modifier) {
    Column(modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
        Note(NoteKind.TODAY, "今日", Color(0xFF2E7D4F), "已走 ${steps.withCommas()} 步", "${todayKm.twoDecimals()} 公里・${kcal.roundToInt()} 大卡")
        Note(
            NoteKind.REGION, "本區域", INK_RED,
            if (townKm != null) "已走 ${townKm.fixed(1)} 公里" else "已走 -", if (townLeft != null) "未走 ${townLeft.fixed(1)} 公里" else "未走 -",
        )
        Note(NoteKind.ISLAND, "環島全程", COLD_BLUE, "已走 ${walkedKm.fixed(1)} 公里", "未走 ${(totalKm - walkedKm).coerceAtLeast(0.0).fixed(1)} 公里")
    }
}

private enum class NoteKind { TODAY, REGION, ISLAND }

/** One note: a drawing, the title underlined in its colour, then two lines of writing. */
@Composable
private fun Note(kind: NoteKind, title: String, color: Color, line1: String, line2: String) {
    val style = androidx.compose.ui.text.TextStyle(fontFamily = NOTE_FONT, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = NOTE_SIZE, lineHeight = 15.sp)
    Row(verticalAlignment = Alignment.Top) {
        Canvas(Modifier.size(26.dp)) { Pen(this, size).noteIcon(kind, color, 500 + kind.ordinal) }
        Column(Modifier.padding(start = 3.dp)) {
            Text(title, style = style, fontWeight = FontWeight.Black, color = color)
            Canvas(Modifier.fillMaxWidth().height(3.dp)) {
                Pen(this, size).stroke(listOf(Offset(0f, size.height / 2), Offset(size.width * 0.85f, size.height / 2)), color, 600 + kind.ordinal, 1.dp.toPx(), alpha = 0.7f, calm = 1.5f)
            }
            Text(line1, style = style, fontWeight = FontWeight.Bold, color = NOTE_INK)
            Text(line2, style = style, color = NOTE_INK.copy(alpha = 0.65f))
        }
    }
}

/** The note's little drawing, in the pen's 100 x 100 square. */
private fun Pen.noteIcon(kind: NoteKind, color: Color, seed: Int) {
    val w = line * 1.5f
    when (kind) {
        NoteKind.TODAY -> {
            // two footprints
            for ((i, c) in listOf(p(36f, 62f), p(64f, 40f)).withIndex()) {
                val sole = circle(c, 15f * u, 18)
                wash(sole, color, 0.35f, 0f, 0f)
                stroke(sole, ink, seed + i, w, closed = true, calm = 0.4f)
                for (t in 0..2) wash(circle(Offset(c.x + (t - 1) * 8f * u, c.y - 21f * u), 3.4f * u, 8), color, 0.8f, 0f, 0f)
            }
        }
        NoteKind.REGION -> {
            val hex = (0 until 6).map { val ang = PI / 3 * it - PI / 2; p(50f + (38f * cos(ang)).toFloat(), 52f + (38f * sin(ang)).toFloat()) }
            wash(hex, color, 0.25f, 0f, 0f)
            stroke(hex, ink, seed, w, closed = true, dash = dashed(this, 5f, 3.5f), calm = 0.3f)
            wash(circle(p(50f, 52f), 7f * u, 10), color, 0.9f, 0f, 0f)
        }
        NoteKind.ISLAND -> {
            val isle = listOf(p(52f, 8f), p(64f, 22f), p(68f, 45f), p(60f, 70f), p(50f, 94f), p(40f, 76f), p(33f, 52f), p(38f, 26f))
            wash(isle, color, 0.3f, 0f, 0f)
            stroke(isle, ink, seed, w, closed = true, calm = 0.4f)
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
