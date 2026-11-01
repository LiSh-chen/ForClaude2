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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.unit.em
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * The control console under the dials: one piece with them, on the same strip, shown when the panel is pulled up. It is drawn with the same pen as the dials
 * (inked outlines, a wash of colour, hatching for shade) and laid out like the row of keys on a car's centre console: a lamp over each key, its picture in
 * the middle and its name under it, the compass and the sync knob at the two ends.
 */

private val LAMP_SPOTS = Color(0xFFF2A93B)
private val LAMP_ROUTE = Color(0xFFE04B3F)
private val LAMP_REGIONS = Color(0xFF3B9BFF)

internal val QUILL = androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(com.starmist.walker.R.font.lxgw_wenkai_tc_subset))
private val NOTE_ROW = 22.dp
internal val SEPIA = Color(0xFF3A2A1B)
private val TODAY_GREEN = Color(0xFF2E7D4F)
private val KEY_MAX = 60.dp

private enum class NoteKind { TODAY, REGION, ISLAND, STEPS, ROAD, FLAME }

/** The row of buttons along the foot of the panel: the compass, the three things the map can show, and the sync knob with its lamp. */
@Composable
internal fun KeyRow(layers: MapLayers, onLayers: (MapLayers) -> Unit, heading: Float, lastSync: Long?, onSync: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = KEY_MAX), horizontalAlignment = Alignment.CenterHorizontally) {
                Compass(heading, Modifier.fillMaxWidth().aspectRatio(1f))
                Text("景", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Transparent)   // the height of the names under the keys
            }
        }
        KeyCell(KeyKind.SPOTS, "景點", layers.spots, LAMP_SPOTS, 100, Modifier.weight(1f)) { onLayers(layers.copy(spots = !layers.spots)) }
        KeyCell(KeyKind.ROUTE, "路線", layers.route, LAMP_ROUTE, 200, Modifier.weight(1f)) { onLayers(layers.copy(route = !layers.route)) }
        KeyCell(KeyKind.REGIONS, "區域", layers.regions, LAMP_REGIONS, 300, Modifier.weight(1f)) { onLayers(layers.copy(regions = !layers.regions)) }
        // the sync knob: its lamp is green once the steps have been synced and red until then
        Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = KEY_MAX).clickable(role = Role.Button, onClickLabel = tr("同步"), onClick = onSync), horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) { Pen(this, size).knob(400, if (lastSync != null) Color(0xFF4FBF6A) else Color(0xFFE04B3F)) }
                Text("同步", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Vintage.ink)
            }
        }
    }
}

/** What the notebook page shows. */
internal class NotebookData(
    val steps: Long, val goal: Int, val todayKm: Double, val kcal: Double,
    val townKm: Double?, val townTotalKm: Double?, val walkedKm: Double, val totalKm: Double,
)

private val NOTEBOOK_WIDTH = 372.dp
private const val NOTEBOOK_LINES = 7
private val NOTEBOOK_HEIGHT = NOTE_ROW * NOTEBOOK_LINES + 12.dp

/**
 * The notebook page, the other way (besides the dials) to see how the walk is going: a whole page of parchment, written in a scribe's kai hand (LXGW WenKai TC,
 * a subset of it), with the steps, the distance and energy, the township and the way round the island, each measured on a line of its own and (but the distance and energy) with a crayon-hatched bar under it of how far it has got. It is laid out on one
 * design size and scaled to the room (see [FitDesign]), so it is the same on every phone.
 */
@Composable
internal fun NotebookPanel(data: NotebookData, modifier: Modifier = Modifier) {
    FitDesign(NOTEBOOK_WIDTH, NOTEBOOK_HEIGHT, modifier.fillMaxWidth().aspectRatio(NOTEBOOK_WIDTH.value / NOTEBOOK_HEIGHT.value)) {
        val title = androidx.compose.ui.text.TextStyle(fontFamily = QUILL, fontSize = 14.sp, lineHeight = 1.15.em, fontWeight = FontWeight.Bold)
        val value = androidx.compose.ui.text.TextStyle(fontFamily = QUILL, fontSize = 13.sp, lineHeight = 1.15.em, color = SEPIA)
        fun km(x: Double) = x.fixed(1)
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.matchParentSize()) { Pen(this, size).parchmentPage(NOTE_ROW.toPx(), 6.dp.toPx(), NOTEBOOK_LINES) }
            Column(Modifier.padding(start = 6.dp, end = 10.dp, top = 6.dp)) {
                Entry(NoteKind.STEPS, "今日步數", TODAY_GREEN, "${data.steps.withCommas()} / ${data.goal.toLong().withCommas()} 步", data.steps.toFloat() / data.goal.coerceAtLeast(1), title, value)
                // the distance and the energy of today: only numbers, no bar
                Row(Modifier.height(NOTE_ROW).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Margin(NoteKind.ROAD, SEPIA)
                    Text("今日距離", Modifier.padding(start = 6.dp), style = title.copy(color = SEPIA), softWrap = false)
                    Text("${km(data.todayKm)} km", Modifier.padding(start = 8.dp), style = value, softWrap = false)
                    Spacer(Modifier.weight(1f))
                    Canvas(Modifier.size(15.dp)) { Pen(this, size).noteIcon(NoteKind.FLAME, SEPIA, 505) }
                    Text("熱量", Modifier.padding(start = 4.dp), style = title.copy(color = SEPIA), softWrap = false)
                    Text("${data.kcal.roundToInt()} 大卡", Modifier.padding(start = 8.dp), style = value, softWrap = false)
                }
                Entry(
                    NoteKind.REGION, "本區域", INK_RED,
                    if (data.townKm == null) "—" else if (data.townTotalKm == null) "達成 ${km(data.townKm)} km" else "達成 ${km(data.townKm)} / ${km(data.townTotalKm)} km",
                    if (data.townKm != null && data.townTotalKm != null && data.townTotalKm > 0) (data.townKm / data.townTotalKm).toFloat() else null, title, value,
                )
                Entry(NoteKind.ISLAND, "環島全程", COLD_BLUE, "達成 ${km(data.walkedKm)} / ${data.totalKm.toLong().withCommas()} km", (data.walkedKm / data.totalKm.coerceAtLeast(1.0)).toFloat(), title, value)
            }
        }
    }
}

/** A drawing in a column of its own at the start of a line. */
@Composable
private fun Margin(kind: NoteKind?, color: Color) {
    Box(Modifier.width(22.dp), contentAlignment = Alignment.Center) {
        if (kind != null) Canvas(Modifier.size(17.dp)) { Pen(this, size).noteIcon(kind, color, 500 + kind.ordinal) }
    }
}

/** What is measured: its drawing, its name and the figure on one line, and under it the bar of how far it has got (when there is one). */
@Composable
private fun Entry(kind: NoteKind, name: String, color: Color, text: String, fraction: Float?, title: androidx.compose.ui.text.TextStyle, value: androidx.compose.ui.text.TextStyle) {
    Row(Modifier.height(NOTE_ROW).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Margin(kind, color)
        Text(name, Modifier.padding(start = 6.dp), style = title.copy(color = color), softWrap = false)
        Spacer(Modifier.weight(1f))
        Text(text, style = value, softWrap = false)
    }
    Row(Modifier.height(NOTE_ROW).fillMaxWidth()) {
        Spacer(Modifier.width(28.dp))
        if (fraction != null) Canvas(Modifier.fillMaxSize()) { Pen(this, size).hatchedBar(fraction, color, 800 + kind.ordinal) }
    }
}

/** A rounded rectangle in pixels, as a list of points (the pen's own rectangles are in its square units, which does not suit a long thin shape). */
internal fun pxRound(l: Float, t: Float, r: Float, b: Float, rad: Float): List<Offset> {
    val out = ArrayList<Offset>()
    fun arc(cx: Float, cy: Float, from: Int) { for (i in 0..4) { val a = Math.toRadians(from + i * 22.5); out += Offset(cx + (rad * cos(a)).toFloat(), cy + (rad * sin(a)).toFloat()) } }
    arc(r - rad, t + rad, 270); arc(r - rad, b - rad, 0); arc(l + rad, b - rad, 90); arc(l + rad, t + rad, 180)
    return out
}

/** A bar drawn with a crayon: a pale track with a pen outline, and the part that is done filled with slanting strokes of the colour. */
private fun Pen.hatchedBar(fraction: Float, color: Color, seed: Int) {
    val dp = with(d) { 1.dp.toPx() }
    val w = d.size.width; val h = d.size.height
    val top = 4f * dp; val bottom = h - 4f * dp
    val track = pxRound(0f, top, w, bottom, 3f * dp)
    wash(track, color, 0.10f, 0f, 0f)
    stroke(track, ink, seed, line * 0.8f, closed = true, alpha = 0.5f, calm = 1f)
    val f = fraction.coerceIn(0f, 1f)
    if (f > 0f) {
        val right = maxOf(f * w, 6f * dp)
        val fill = pxRound(0f, top, right, bottom, 3f * dp)
        clipTo(fill) {
            var x = -(bottom - top); var k = 0
            while (x < right + (bottom - top)) {
                val jitter = ((k * 7919) % 5 - 2) * 0.25f * dp
                d.drawLine(color.copy(alpha = 0.9f), Offset(x + jitter, bottom), Offset(x + jitter + (bottom - top), top), strokeWidth = 1.2f * dp, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                x += 3.4f * dp; k++
            }
        }
        stroke(fill, ink, seed + 7, line * 1.4f, closed = true, calm = 1.2f)
    }
}

/** The page itself: uneven parchment with stains, a dark edge, faint brown rules, a faded red margin and two holes for the cord. */
internal fun Pen.parchmentPage(row: Float, top: Float, lines: Int) {
    // the page is drawn in pixels and not in the pen's square units
    val dp = with(d) { 1.dp.toPx() }
    val w = d.size.width; val h = d.size.height
    val page = pxRound(1.5f * dp, 1.5f * dp, w - 1.5f * dp, h - 1.5f * dp, 5f * dp)
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
    val count = if (lines < 0) ((h - top) / row).toInt() else lines   // a negative number: as many rules as the page has room for
    for (k in 1..count) {
        val y = top + row * k
        stroke(listOf(Offset(6f * dp, y), Offset(w - 6f * dp, y)), Color(0xFF8A6B3F), 720 + k, 0.7f * line, alpha = 0.35f, calm = 0.8f)
    }
    val mx = 26f * dp
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
        NoteKind.STEPS -> {
            // a bare foot: the ball and the heel, and five toes
            fun oval(cx: Float, cy: Float, rx: Float, ry: Float) = (0 until 20).map { val a = 2.0 * PI * it / 20; p(cx + (rx * cos(a)).toFloat(), cy + (ry * sin(a)).toFloat()) }
            val ball = oval(46f, 56f, 22f, 26f); val heel = oval(50f, 84f, 13f, 12f)
            wash(ball, SEPIA, 0.4f, 0f, 0f); wash(heel, SEPIA, 0.4f, 0f, 0f)
            stroke(ball, ink, seed, w, closed = true, calm = 0.3f); stroke(heel, ink, seed + 1, w, closed = true, calm = 0.3f)
            for ((i, t) in listOf(Triple(30f, 22f, 7f), Triple(44f, 15f, 5.4f), Triple(56f, 15f, 4.8f), Triple(66f, 20f, 4.2f), Triple(72f, 29f, 3.6f)).withIndex())
                wash(circle(p(t.first, t.second), t.third * u, 10), SEPIA, 0.85f, 0f, 0f)
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

private enum class KeyKind { SPOTS, ROUTE, REGIONS }

/** One key: its lamp lights (and the key stands out) while what it switches is shown on the map. */
@Composable
private fun KeyCell(kind: KeyKind, label: String, on: Boolean, lamp: Color, seed: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val glow by animateFloatAsState(if (on) 1f else 0f, spring(stiffness = Spring.StiffnessMediumLow), label = "lamp")
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = KEY_MAX).clickable(role = Role.Switch, onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) { Pen(this, size).key(kind, glow, lamp, seed) }
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Vintage.ink.copy(alpha = 0.6f + 0.4f * glow))
        }
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
        KeyKind.SPOTS -> {
            // a big sight: a pin; a small one: a ring with a dot
            val c = p(41f, 50f)
            stroke(circle(c, 14f * u, 20), ink, seed, w, closed = true, alpha = a, calm = 0.3f)
            stroke(listOf(p(31.5f, 59f), p(41f, 80f)), ink, seed + 1, w, alpha = a, calm = 0.2f)
            stroke(listOf(p(50.5f, 59f), p(41f, 80f)), ink, seed + 2, w, alpha = a, calm = 0.2f)
            wash(circle(c, 4f * u, 10), ink, a, 0f, 0f)
            val s = p(71f, 70f)
            stroke(circle(s, 8f * u, 14), ink, seed + 3, w * 0.9f, closed = true, alpha = a, calm = 0.2f)
            wash(circle(s, 2.6f * u, 8), ink, a, 0f, 0f)
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
private fun Pen.knob(seed: Int, lamp: Color) {
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
    // the lamp, at the top centre of the face, level with the lamps over the keys
    val lc = p(50f, 27f)
    wash(circle(lc, 11f * u, 18), lamp, 0.30f, 0f, 0f)
    val jewel = circle(lc, 6f * u, 14)
    wash(jewel, lamp, 1f, 0f, 0f)
    stroke(jewel, ink, seed + 60, line, closed = true, calm = 0.2f)
    wash(circle(Offset(lc.x - 1.8f * u, lc.y - 1.8f * u), 1.8f * u, 8), Color.White, 0.8f, 0f, 0f)
}

/** The button that closes a letter: a brass seal with a cross on it. */
internal fun Pen.closeSeal(seed: Int) {
    val face = circle(p(50f, 50f), 40f * u, 28)
    wash(face, Vintage.brass, 1f, 0f, 0f)
    wash(circle(p(42f, 42f), 22f * u, 18), Vintage.brassLight, 0.6f, 0f, 0f)
    inked(face, seed, closed = true, width = line * 1.3f, calm = 0.3f)
    stroke(listOf(p(34f, 34f), p(66f, 66f)), ink, seed + 1, line * 1.8f, calm = 0.2f)
    stroke(listOf(p(66f, 34f), p(34f, 66f)), ink, seed + 2, line * 1.8f, calm = 0.2f)
}
