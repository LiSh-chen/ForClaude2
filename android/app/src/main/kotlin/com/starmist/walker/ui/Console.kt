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
        Notes(steps, todayKm, kcal, townKm, walkedKm, Modifier.weight(1.3f))
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

private val QUILL = androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(com.starmist.walker.R.font.lxgw_wenkai_tc_subset))
private val NOTE_ROW = 22.dp
private val SEPIA = Color(0xFF3A2A1B)
private val TODAY_GREEN = Color(0xFF2E7D4F)

private enum class NoteKind { TODAY, REGION, ISLAND, STEPS, ROAD, FLAME }
private const val PAGE_LINES = 6

/**
 * One page of parchment with everything written on it in a scribe's kai hand (LXGW WenKai TC, a subset of it), six lines: today (steps; distance and energy), then
 * what has been walked in this township and round the island. Each part has a title in the colour of its needle on the dials and a little drawing in the margin.
 */
@Composable
private fun Notes(steps: Long, todayKm: Double, kcal: Double, townKm: Double?, walkedKm: Double, modifier: Modifier) {
    val title = androidx.compose.ui.text.TextStyle(fontFamily = QUILL, fontSize = 16.sp, lineHeight = 20.sp)
    val value = androidx.compose.ui.text.TextStyle(fontFamily = QUILL, fontSize = 14.sp, lineHeight = 20.sp, color = SEPIA)
    Box(modifier.fillMaxWidth().height(NOTE_ROW * PAGE_LINES + 10.dp)) {
        Canvas(Modifier.matchParentSize()) { Pen(this, size).parchmentPage(NOTE_ROW.toPx(), 5.dp.toPx(), PAGE_LINES) }
        Column(Modifier.padding(start = 6.dp, end = 6.dp, top = 5.dp)) {
            // today: the title and the steps on one line, the distance and the energy on the next
            Row(Modifier.height(NOTE_ROW), verticalAlignment = Alignment.CenterVertically) {
                Margin(NoteKind.TODAY, TODAY_GREEN)
                Text("今日", Modifier.padding(start = 6.dp), style = title.copy(color = TODAY_GREEN), softWrap = false)
                Text("${steps.withCommas()} 步", Modifier.padding(start = 8.dp), style = value, softWrap = false)
            }
            Row(Modifier.height(NOTE_ROW), verticalAlignment = Alignment.CenterVertically) {
                Margin(NoteKind.ROAD, SEPIA)
                Text("${todayKm.fixed(1)} 公里", Modifier.padding(start = 6.dp), style = value, softWrap = false)
                Canvas(Modifier.padding(start = 7.dp).size(12.dp)) { Pen(this, size).noteIcon(NoteKind.FLAME, SEPIA, 505) }
                Text("${kcal.roundToInt()} 大卡", Modifier.padding(start = 2.dp), style = value, softWrap = false)
            }
            PageLine(NoteKind.REGION, "本區域", title.copy(color = INK_RED), INK_RED)
            PageLine(null, "已走 ${townKm?.fixed(1) ?: "-"} 公里", value, SEPIA)
            PageLine(NoteKind.ISLAND, "環島全程", title.copy(color = COLD_BLUE), COLD_BLUE)
            PageLine(null, "已走 ${walkedKm.fixed(1)} 公里", value, SEPIA)
        }
    }
}

/** The margin of the page: a drawing (or none) in a column of its own. */
@Composable
private fun Margin(kind: NoteKind?, color: Color) {
    Box(Modifier.width(20.dp), contentAlignment = Alignment.Center) {
        if (kind != null) Canvas(Modifier.size(18.dp)) { Pen(this, size).noteIcon(kind, color, 500 + kind.ordinal) }
    }
}

/** One ruled line of the page: a drawing in the margin (or none) and the writing. */
@Composable
private fun PageLine(kind: NoteKind?, text: String, style: androidx.compose.ui.text.TextStyle, color: Color) {
    Row(Modifier.height(NOTE_ROW), verticalAlignment = Alignment.CenterVertically) {
        Margin(kind, color)
        Text(text, Modifier.padding(start = 6.dp), style = style, softWrap = false)
    }
}

/** The page itself: uneven parchment with stains, a dark edge, faint brown rules, a faded red margin and two holes for the cord. */
private fun Pen.parchmentPage(row: Float, top: Float, lines: Int) {
    // the page is tall and narrow, so it is drawn in pixels and not in the pen's square units
    val dp = with(d) { 1.dp.toPx() }
    val w = d.size.width; val h = d.size.height
    fun rounded(l: Float, t: Float, r: Float, b: Float, rad: Float): List<Offset> {
        val out = ArrayList<Offset>()
        fun arc(cx: Float, cy: Float, from: Int) { for (i in 0..4) { val a = Math.toRadians((from + i * 22.5)); out += Offset(cx + (rad * cos(a)).toFloat(), cy + (rad * sin(a)).toFloat()) } }
        arc(r - rad, t + rad, 270); arc(r - rad, b - rad, 0); arc(l + rad, b - rad, 90); arc(l + rad, t + rad, 180)
        return out
    }
    val page = rounded(1.5f * dp, 1.5f * dp, w - 1.5f * dp, h - 1.5f * dp, 5f * dp)
    wash(page, Color(0xFFEBD7A4), 1f, 0f, 0f)
    // stains: soft brown blotches near the corners and edges, clipped to the page
    clipTo(page) {
        val blots = listOf(Triple(0.08f, 0.1f, 26f), Triple(0.93f, 0.2f, 20f), Triple(0.85f, 0.92f, 30f), Triple(0.12f, 0.88f, 22f), Triple(0.55f, 0.5f, 14f))
        for ((k, bl) in blots.withIndex()) {
            val c = Offset(w * bl.first, h * bl.second)
            d.drawCircle(Color(0xFFB07F3A).copy(alpha = 0.10f), bl.third * dp, c)
            d.drawCircle(Color(0xFFB07F3A).copy(alpha = 0.07f), bl.third * dp * 0.6f, Offset(c.x + 3f * k * dp, c.y - 2f * k * dp))
        }
    }
    stroke(page, Vintage.edge, 710, line * 1.8f, closed = true, alpha = 0.9f, calm = 2.2f)
    stroke(page, Vintage.brassDark, 711, line * 0.6f, closed = true, alpha = 0.6f, calm = 1.4f)
    for (k in 1..lines) {
        val y = top + row * k
        stroke(listOf(Offset(6f * dp, y), Offset(w - 6f * dp, y)), Color(0xFF8A6B3F), 720 + k, 0.7f * line, alpha = 0.35f, calm = 0.8f)
    }
    val mx = 25f * dp
    stroke(listOf(Offset(mx, 4f * dp), Offset(mx, h - 4f * dp)), Color(0xFFB3463C), 730, 0.9f * line, alpha = 0.45f, calm = 0.4f)
}

/** The little drawings in the margin, in the pen's 100 x 100 square. */
private fun Pen.noteIcon(kind: NoteKind, color: Color, seed: Int) {
    val w = line * 1.5f
    when (kind) {
        NoteKind.TODAY -> {
            // a sun: a disc with rays
            val c = p(50f, 50f)
            wash(circle(c, 18f * u, 16), Color(0xFFE8A93A), 0.55f, 0f, 0f)
            stroke(circle(c, 18f * u, 16), ink, seed, w, closed = true, calm = 0.4f)
            for (i in 0 until 8) {
                val a = PI / 4 * i
                stroke(listOf(Offset(c.x + (26f * u * cos(a)).toFloat(), c.y + (26f * u * sin(a)).toFloat()), Offset(c.x + (42f * u * cos(a)).toFloat(), c.y + (42f * u * sin(a)).toFloat())), color, seed + i + 1, w, calm = 0.3f)
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
        NoteKind.STEPS -> for ((i, c) in listOf(p(36f, 64f), p(64f, 38f)).withIndex()) {
            val sole = circle(c, 14f * u, 16)
            wash(sole, SEPIA, 0.35f, 0f, 0f)
            stroke(sole, ink, seed + i, w, closed = true, calm = 0.4f)
            for (t in 0..2) wash(circle(Offset(c.x + (t - 1) * 8f * u, c.y - 20f * u), 3.2f * u, 8), SEPIA, 0.8f, 0f, 0f)
        }
        NoteKind.ROAD -> {
            val road = listOf(p(14f, 82f), p(34f, 82f), p(48f, 72f), p(52f, 56f), p(44f, 44f), p(52f, 30f), p(68f, 22f), p(86f, 20f))
            stroke(road, ink, seed, w, calm = 0.3f)
            wash(circle(p(14f, 82f), 5f * u, 10), SEPIA, 0.9f, 0f, 0f)
            wash(circle(p(86f, 20f), 5f * u, 10), INK_RED, 0.9f, 0f, 0f)
        }
        NoteKind.FLAME -> {
            val flame = listOf(p(50f, 8f), p(66f, 34f), p(76f, 56f), p(70f, 80f), p(50f, 92f), p(30f, 80f), p(24f, 58f), p(36f, 40f), p(44f, 26f))
            wash(flame, Color(0xFFE2572B), 0.7f, 0f, 0f)
            stroke(flame, ink, seed, w, closed = true, calm = 0.4f)
            wash(listOf(p(50f, 46f), p(60f, 64f), p(54f, 82f), p(42f, 82f), p(40f, 64f)), Color(0xFFF3C14B), 0.9f, 0f, 0f)
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
