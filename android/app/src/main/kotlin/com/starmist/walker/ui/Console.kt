package com.starmist.walker.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
        Notes(steps, todayKm, kcal, townKm, walkedKm, Modifier.weight(1.12f))
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

private val NOTE_SIZE = 10.sp

/**
 * Today in a frame of its own with drums (steps, distance, energy); under it a notebook page with what has been walked in this township and round the island.
 */
@Composable
private fun Notes(steps: Long, todayKm: Double, kcal: Double, townKm: Double?, walkedKm: Double, modifier: Modifier) {
    val tm = rememberTextMeasurer()
    val stepsRoll = rememberRolling(steps.toFloat(), "steps")
    Column(modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
        Counter(NoteKind.TODAY, "今日已走 步", Color(0xFF2E7D4F), tm, stepsRoll, decimal = false, framed = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiniDrums(tm, rememberRolling((todayKm * 10).roundToInt().toFloat(), "todayKm"), cells = 3, decimal = true, Modifier.size(38.dp, 18.dp))
                Text("公里", Modifier.padding(horizontal = 2.dp), softWrap = false, fontSize = NOTE_SIZE, lineHeight = 12.sp, fontWeight = FontWeight.Black, color = Vintage.inkSoft)
                MiniDrums(tm, rememberRolling(kcal.roundToInt().toFloat(), "kcal"), cells = 4, decimal = false, Modifier.size(42.dp, 18.dp))
                Text("大卡", Modifier.padding(start = 2.dp), softWrap = false, fontSize = NOTE_SIZE, lineHeight = 12.sp, fontWeight = FontWeight.Black, color = Vintage.inkSoft)
            }
        }
        NotebookPage(townKm, walkedKm)
    }
}

private enum class NoteKind { TODAY, REGION, ISLAND }

/** One counter: drawing and title, the drums, and the line under them. */
@Composable
private fun Counter(kind: NoteKind, title: String, color: Color, tm: TextMeasurer, reading: Float?, decimal: Boolean, framed: Boolean = false, under: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
    if (framed) Canvas(Modifier.matchParentSize()) {
        val pen = Pen(this, size)
        with(pen) {
            val wu = size.width / u; val l = (100f - wu) / 2f
            val box = roundRect(l + 1f, 2f, l + wu - 1f, 98f, 9f)
            wash(box, color, 0.10f, 0f, 0f)
            inked(box, 700 + kind.ordinal, closed = true, width = line * 1.2f, calm = 0.4f)
        }
    }
    Column(if (framed) Modifier.padding(horizontal = 5.dp, vertical = 3.dp) else Modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(14.dp)) { Pen(this, size).noteIcon(kind, color, 500 + kind.ordinal) }
            Text(title, Modifier.padding(start = 3.dp), fontSize = NOTE_SIZE, lineHeight = 12.sp, fontWeight = FontWeight.Black, color = color)
        }
        Canvas(Modifier.fillMaxWidth().height(24.dp)) {
            val pen = Pen(this, size)
            // the pen's unit is the height of this strip, so the width is a few hundred units: its left edge is this far out
            val left = -(size.width - size.height) / 2f / (size.height / 100f)
            with(pen) { odometer(tm, left + 1f, 5f, reading, cells = 5, cw = 74f, ch = 90f, decimal = decimal) }
        }
        under()
    }
    }
}

/** A small set of drums (a smaller reading under the big one). */
@Composable
private fun MiniDrums(tm: TextMeasurer, reading: Float?, cells: Int, decimal: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        val left = -(size.width - size.height) / 2f / (size.height / 100f)
        with(Pen(this, size)) { odometer(tm, left + 1f, 5f, reading, cells = cells, cw = 60f, ch = 90f, decimal = decimal) }
    }
}

private val NOTE_INK = Color(0xFF23407A)   // fountain-pen blue-black
private val NOTE_ROW = 22.dp

/**
 * A page of a notebook: ruled lines, a red margin and binder holes, and in blue-black ink what has been walked in this township and round the island,
 * each written under a title in the colour of its needle on the dials.
 */
@Composable
private fun NotebookPage(townKm: Double?, walkedKm: Double) {
    val style = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 13.sp, lineHeight = 16.sp,
    )
    Box(Modifier.fillMaxWidth().height(NOTE_ROW * 4 + 8.dp)) {
        Canvas(Modifier.matchParentSize()) {
            val pen = Pen(this, size)
            with(pen) {
                val wu = size.width / u; val l = (100f - wu) / 2f
                val page = roundRect(l + 1f, 1f, l + wu - 1f, 99f, 5f)
                wash(page, Color(0xFFFFFBEA), 1f, 0f, 0f)
                inked(page, 710, closed = true, width = line, calm = 0.4f)
                val top = 4.dp.toPx()
                for (k in 1..4) {
                    val y = top + NOTE_ROW.toPx() * k
                    stroke(listOf(Offset(10.dp.toPx(), y), Offset(size.width - 6.dp.toPx(), y)), Color(0xFF6C8EBF), 720 + k, 0.8.dp.toPx(), alpha = 0.55f, calm = 0.5f)
                }
                val mx = 22.dp.toPx()
                stroke(listOf(Offset(mx, 3.dp.toPx()), Offset(mx, size.height - 3.dp.toPx())), Color(0xFFD24A4A), 730, 0.9.dp.toPx(), alpha = 0.6f, calm = 0.3f)
                for (k in 0..2) wash(circle(Offset(9.dp.toPx(), size.height * (0.2f + 0.3f * k)), 2.4.dp.toPx(), 10), Vintage.parchmentDark, 1f, 0f, 0f)
            }
        }
        Column(Modifier.padding(start = 26.dp, end = 6.dp, top = 4.dp)) {
            NoteLine(NoteKind.REGION, "本區域", INK_RED, style)
            NoteValue("已走 ${townKm?.fixed(1) ?: "-"} 公里", style)
            NoteLine(NoteKind.ISLAND, "環島全程", COLD_BLUE, style)
            NoteValue("已走 ${walkedKm.fixed(1)} 公里", style)
        }
    }
}

@Composable
private fun NoteLine(kind: NoteKind, title: String, color: Color, style: androidx.compose.ui.text.TextStyle) {
    Row(Modifier.height(NOTE_ROW), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(14.dp)) { Pen(this, size).noteIcon(kind, color, 500 + kind.ordinal) }
        Text(title, Modifier.padding(start = 3.dp), style = style, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
private fun NoteValue(text: String, style: androidx.compose.ui.text.TextStyle) {
    Box(Modifier.height(NOTE_ROW).padding(start = 17.dp), contentAlignment = Alignment.CenterStart) {
        Text(text, style = style, fontWeight = FontWeight.Bold, color = NOTE_INK, softWrap = false)
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
