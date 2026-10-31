package com.starmist.walker.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/*
 * A car's instrument cluster in pen sketch: two big round dials with a needle (the way to the next card, the whole island),
 * between them two half dials (fuel for today's steps, temperature for the way through the township), . What is going on is read from where the needles point and which lamps are lit, not from words.
 */

private val INK_RED = Color(0xFFB3202A)
private val AMBER = Color(0xFFE0A21B)
private val OK_GREEN = Color(0xFF3C8D5A)
private val COLD_BLUE = Color(0xFF0E86D4)
private val PAPER = Color(0xFFFFFBEA)
private val STEP_GREEN = Color(0xFF2E7D4F)
private val BRONZE = Color(0xFF6B4A24)
private val ISLAND_BLUE = Color(0xFF0B5E96)

private fun rad(deg: Float) = (deg * PI / 180.0)

/** The point at angle [deg] (degrees, clockwise from east) and radius [r] around ([cx], [cy]), in the pen's units. */
private fun Pen.at(cx: Float, cy: Float, r: Float, deg: Float) = p(cx + (cos(rad(deg)) * r).toFloat(), cy + (sin(rad(deg)) * r).toFloat())

private fun Pen.arcPts(cx: Float, cy: Float, r: Float, d0: Float, d1: Float, n: Int = 36) = (0..n).map { at(cx, cy, r, d0 + (d1 - d0) * it / n) }

/** A coloured band along the dial between two radii, from fraction [f0] to [f1] of the sweep. */
private fun Pen.band(cx: Float, cy: Float, r0: Float, r1: Float, start: Float, sweep: Float, f0: Float, f1: Float, color: Color, seed: Int, alpha: Float = 0.55f) {
    if (f1 - f0 < 0.004f) return
    val a = start + sweep * f0; val b = start + sweep * f1
    val shape = arcPts(cx, cy, r1, a, b, 24) + arcPts(cx, cy, r0, b, a, 24)
    wash(shape, color, alpha, 0f, 0f)
    hatch(shape, color, 0f, 100f, 0f, 100f, gap = 2.6f, lean = 4f, alpha = 0.6f)
    stroke(shape, ink, seed, width = line * 0.7f, closed = true, alpha = 0.8f)
}

/** The rim, ticks and paper of a dial. */
private fun Pen.face(cx: Float, cy: Float, r: Float, start: Float, sweep: Float, majors: Int, seed: Int, whole: Boolean) {
    val rim = if (whole) arcPts(cx, cy, r, 0f, 360f, 40) else arcPts(cx, cy, r, start, start + sweep, 40) + listOf(at(cx, cy, r, start))
    if (whole) {
        // a brass bezel round the dial
        val bezel = arcPts(cx, cy, r + 1.9f, 0f, 360f, 48)
        wash(bezel, Vintage.brass, 1f, 0f, 0f)
        hatch(bezel, Vintage.brassDark, 0f, 100f, 0f, 100f, gap = 2.4f, lean = 4f, alpha = 0.35f)
        inked(bezel, seed + 2, closed = true, width = line * 1.2f, calm = 0.25f)
    }
    wash(rim, PAPER, 1f, 0f, 0f)
    inked(rim, seed, closed = true, width = line * 1.4f, calm = 0.25f)
    if (whole) inked(arcPts(cx, cy, r * 0.93f, 0f, 360f, 40), seed + 1, closed = true, width = line * 0.6f, calm = 0.25f)
    for (i in 0..majors * 2) {
        val major = i % 2 == 0
        val deg = start + sweep * i / (majors * 2f)
        stroke(listOf(at(cx, cy, r * (if (major) 0.85f else 0.90f), deg), at(cx, cy, r * 0.95f, deg)), ink, seed + 10 + i, width = line * (if (major) 1.1f else 0.7f))
    }
}

private fun Pen.needle(cx: Float, cy: Float, len: Float, deg: Float, color: Color, seed: Int, wide: Float = 0.09f, hub: Boolean = true) {
    val tip = at(cx, cy, len, deg)
    val l = at(cx, cy, len * wide, deg + 90f); val r = at(cx, cy, len * wide, deg - 90f)
    val tail = at(cx, cy, len * 0.18f, deg + 180f)
    val body = listOf(l, tip, r, tail)
    wash(body, color, 0.95f, 0.5f, 0.5f)
    inked(body, seed, closed = true, width = line * 0.9f)
    if (hub) {
        val h = circle(p(cx, cy), 4.2f * u, 18)
        wash(h, Color.White, 1f, 0f, 0f); inked(h, seed + 3, closed = true, width = line)
        wash(circle(p(cx, cy), 1.6f * u, 10), ink, 1f, 0f, 0f)
    }
}

private fun Pen.label(tm: TextMeasurer, s: String, style: TextStyle, c: Offset, grows: Boolean = true) {
    // the writing grows with the dial (up to 40%), so that a bigger dial is easier to read and not only emptier
    val grow = if (grows) (u / with(d) { 1.6.dp.toPx() }).coerceIn(1f, 1.4f) else 1f
    val l = tm.measure(s, if (grow == 1f) style else style.copy(fontSize = style.fontSize * grow))
    d.drawText(l, topLeft = Offset(c.x - l.size.width / 2f, c.y - l.size.height / 2f))
}

/** What each counter showed last, kept while the app lives, so that coming back to the map it goes on from there instead of rolling up from zero again. */
private val lastShown = HashMap<String, Float>()

/**
 * A reading in tenths that rolls to its value: from zero when the app first shows it, and from the old reading when it changes or the screen is
 * shown again. [key] says which counter it is.
 */
@Composable
private fun rememberRolling(tenths: Float, key: String): Float {
    // The screenshot tests take their picture at once, so there the reading does not roll.
    val inspect = LocalInspectionMode.current || decodeThumbsInline
    val a = remember { Animatable(if (inspect) tenths else lastShown[key] ?: 0f) }
    LaunchedEffect(tenths) { if (inspect) a.snapTo(tenths) else a.animateTo(tenths, tween(1600, easing = FastOutSlowInEasing)) }
    DisposableEffect(key) { onDispose { lastShown[key] = a.value } }
    return a.value
}

/**
 * A mechanical odometer of [cells] drums, the last one (tenths, with its decimal point, unless [decimal] is off) on a pale drum, as on a car's dial. [tenths] is the
 * reading times ten, or null for a dashed, blank one. A drum rolls to the next digit only when the drums below it pass from 9 to 0.
 * [x] is the left edge, [y] the top, in the pen's units.
 */
private fun Pen.odometer(tm: TextMeasurer, x: Float, y: Float, tenths: Float?, cells: Int = 5, cw: Float = 4.8f, ch: Float = 7f, decimal: Boolean = true, drum: Color = Color(0xFF2B2118)) {
    val lastW = if (decimal) cw * 1.55f else cw
    val total = cw * (cells - 1) + lastW
    val frame = roundRect(x - 0.8f, y - 0.8f, x + total + 0.8f, y + ch + 0.8f, 1.2f)
    wash(frame, ink, 0.85f, 0f, 0f); inked(frame, 900, closed = true, width = line * 0.8f, calm = 0f)
    val cream = Color(0xFFF3E9C9)
    // sized from the drum itself, so that ".9" fits the tenths drum whatever the size of the dial
    val fontSize = with(d) { minOf(ch * u * 0.75f, lastW * u / 1.35f).toSp() }
    val t = tenths?.coerceIn(0f, Math.pow(10.0, cells.toDouble()).toFloat() - 1f)
    val whole = t?.let { floor(it).toLong() } ?: 0L
    val carry = if (t != null) t - whole else 0f
    for (i in 0 until cells) {
        val k = cells - 1 - i
        val last = k == 0 && decimal
        val x0 = x + i * cw
        val w = if (last) lastW else cw
        val cell = listOf(p(x0 + 0.25f, y), p(x0 + w - 0.25f, y), p(x0 + w - 0.25f, y + ch), p(x0 + 0.25f, y + ch))
        wash(cell, if (last) cream else drum, 1f, 0f, 0f)
        val style = TextStyle(fontSize = fontSize, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = if (last) Vintage.ink else cream)
        val cx = x0 + w / 2f
        if (t == null) { label(tm, "-", style, p(cx, y + ch / 2f), grows = false); continue }
        var m = 1L; repeat(k) { m *= 10 }
        val roll = if (whole % m == m - 1) carry else 0f
        val digit = ((whole / m) % 10).toInt()
        fun text(d: Int) = if (last) ".${d % 10}" else "${d % 10}"
        clipTo(cell) {
            label(tm, text(digit), style, p(cx, y + ch / 2f - roll * ch), grows = false)
            label(tm, text(digit + 1), style, p(cx, y + ch / 2f + (1f - roll) * ch), grows = false)
        }
    }
}

private val needleSpring = spring<Float>(dampingRatio = 0.42f, stiffness = Spring.StiffnessVeryLow)

/** The step scale: a round step between ticks, and the labels in thousands when that reads better. */
private fun stepScale(max: Int): Int = listOf(500, 1000, 2000, 2500, 5000, 10000, 20000).first { max / it <= 6 }

/** Big dial: today's steps. The scale is labelled and stretches to 120% of the goal; the red stretch is what lies beyond the goal. */
@Composable
internal fun StepsDial(steps: Long, goal: Int, todayKm: Double, kcal: Double, modifier: Modifier = Modifier) {
    val max = (goal * 1.2f).coerceAtLeast(1f)
    val v by animateFloatAsState((steps / max).coerceIn(0f, 1f), needleSpring, label = "steps")
    val tm = rememberTextMeasurer()
    val stepsRoll = rememberRolling(steps.toFloat(), "steps")
    Canvas(modifier.aspectRatio(1f)) {
        val pen = Pen(this, size)
        with(pen) {
            val cx = 50f; val cy = 52f; val r = 46f; val start = 135f; val sweep = 270f
            face(cx, cy, r, start, sweep, 0, 200, whole = true)
            val step = stepScale(max.toInt())
            val goalDeg = start + sweep / 1.2f
            val thousands = step >= 1000
            val small = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Vintage.ink)
            var value = 0
            while (value <= max + 1) {
                val deg = start + sweep * value / max
                if (value != goal) stroke(listOf(at(cx, cy, r * 0.84f, deg), at(cx, cy, r * 0.95f, deg)), ink, 210 + value / 100, width = line * 1.2f)
                val half = value + step / 2
                if (half < max && half != goal) { val d2 = start + sweep * half / max; stroke(listOf(at(cx, cy, r * 0.89f, d2), at(cx, cy, r * 0.95f, d2)), ink, 260 + value / 100, width = line * 0.7f) }
                val txt = if (thousands) (value / 1000f).let { if (it % 1f == 0f) "%d".format(it.toInt()) else "%.1f".format(it) } else value.toString()
                // a number sitting at the goal would only collide with its name
                if (kotlin.math.abs(deg - goalDeg) > 11f) label(tm, txt, small, at(cx, cy, r * 0.66f, deg))
                value += step
            }
            // the goal: a small red triangle in the rim, where its tick would be, pointing in at the scale, and its name inside the numbers
            val tri = listOf(at(cx, cy, r * 0.86f, goalDeg), at(cx, cy, r * 0.95f, goalDeg - 3.4f), at(cx, cy, r * 0.95f, goalDeg + 3.4f))
            wash(tri, INK_RED, 1f, 0f, 0f); inked(tri, 220, closed = true, width = line * 0.7f, calm = 0f)
            label(tm, "GOAL", TextStyle(fontSize = 6.sp, fontWeight = FontWeight.Black, color = INK_RED), at(cx, cy, r * 0.62f, goalDeg))
            // beyond the goal
            band(cx, cy, r * 0.76f, r * 0.82f, start, sweep, 1f / 1.2f, 1f, INK_RED, 230, alpha = 0.6f)
            label(tm, if (thousands) "x1000" else "步", TextStyle(fontSize = 7.sp, color = Vintage.inkSoft), p(cx, 31f))
            // today's count in full, on green drums like the needle, with the distance and energy under it
            label(tm, "今日步數", TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Black, color = STEP_GREEN), p(cx, 63.5f))
            odometer(tm, 34f, 68f, stepsRoll, cells = 5, cw = 6.4f, ch = 10.5f, decimal = false)
            label(tm, "%.2f 公里・%d 大卡".format(todayKm, kcal.roundToInt()), TextStyle(fontSize = 6.sp, fontWeight = FontWeight.Bold, color = Vintage.inkSoft), p(cx, 86.5f))
            needle(cx, cy, r * 0.72f, start + sweep * v, STEP_GREEN, 240)
        }
    }
}

/** Big dial: the way round, scaled in percent. The short blue needle is how far round the island you are, the long red one how far through the township; their distances are in the middle panel, in the same colours. */
@Composable
internal fun RouteDial(overall: Float, region: Float?, townKm: Double?, totalKm: Double, modifier: Modifier = Modifier) {
    val v by animateFloatAsState(overall.coerceIn(0f, 1f), needleSpring, label = "island")
    val w by animateFloatAsState((region ?: 0f).coerceIn(0f, 1f), needleSpring, label = "region")
    val town = rememberRolling(((townKm ?: 0.0) * 10).toFloat(), "town")
    val total = rememberRolling((totalKm * 10).toFloat(), "total")
    val tm = rememberTextMeasurer()
    Canvas(modifier.aspectRatio(1f)) {
        val pen = Pen(this, size)
        with(pen) {
            val cx = 50f; val cy = 52f; val r = 46f; val start = 135f; val sweep = 270f
            face(cx, cy, r, start, sweep, 10, 300, whole = true)
            band(cx, cy, r * 0.76f, r * 0.82f, start, sweep, 0f, v, COLD_BLUE, 330, alpha = 0.5f)
            val num = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Vintage.ink)
            for (i in 0..5) label(tm, "${i * 20}", num, at(cx, cy, r * 0.66f, start + sweep * i / 5f))
            label(tm, "進度 %", TextStyle(fontSize = 6.sp, color = Vintage.inkSoft), p(cx, 30f))
            // the two distances walked, as two odometers, the unit printed on the face
            // the kilometres walked in the township (the colour of the long needle) and in all, as two drum counters
            val tag = TextStyle(fontSize = 6.sp, fontWeight = FontWeight.Bold)
            label(tm, "區域 km", tag.copy(color = INK_RED), p(50f, 62.5f))
            odometer(tm, 33.9f, 65.8f, if (townKm != null) town else null, cw = 5.8f, ch = 8.6f)
            label(tm, "總里程 km", tag.copy(color = BRONZE), p(50f, 78.6f))
            odometer(tm, 33.9f, 81.8f, total, cw = 5.8f, ch = 8.6f)
            // long needle: through the township; short one: round the island
            needle(cx, cy, r * 0.80f, start + sweep * w, INK_RED, 310, wide = 0.05f, hub = false)
            needle(cx, cy, r * 0.50f, start + sweep * v, COLD_BLUE, 320, wide = 0.13f)
        }
    }
}

/**
 * The whole cluster, mounted on a stitched leather strip with brass rivets: the step dial and the way-round dial side by side, and under them,
 * still on the strip, the compass (it turns to the walker's heading), the three brass buttons that switch what the map shows, and the knob that syncs.
 */
@Composable
internal fun InstrumentCluster(
    steps: Long, goal: Int, overall: Float, region: Float?, walkedKm: Double, townKm: Double?, todayKm: Double, kcal: Double, heading: Float, onSync: () -> Unit,
    layers: MapLayers, onLayers: (MapLayers) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth()) {
        Canvas(Modifier.matchParentSize()) { drawStrip() }
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                StepsDial(steps, goal, todayKm, kcal, Modifier.weight(1f))
                RouteDial(overall, region, townKm, walkedKm, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Compass(heading, Modifier.size(46.dp))
                LampSwitch("地名", layers.names, Color(0xFFF2A93B)) { onLayers(layers.copy(names = !layers.names)) }
                LampSwitch("路線", layers.route, Color(0xFFE04B3F)) { onLayers(layers.copy(route = !layers.route)) }
                LampSwitch("區域", layers.regions, Color(0xFF3B9BFF)) { onLayers(layers.copy(regions = !layers.regions)) }
                SyncKnob(onSync)
            }
        }
    }
}

/** A brass push button with a lamp above its name, like the others on the panel: the lamp is lit while what the button switches is shown on the map. */
@Composable
private fun LampSwitch(text: String, on: Boolean, lit: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier.size(width = 54.dp, height = 40.dp).clip(shape)
            .background(Brush.verticalGradient(if (on) listOf(Vintage.brassLight, Vintage.brass) else listOf(Vintage.brass, Vintage.brassDark)))
            .border(1.6.dp, Vintage.ink, shape)
            .clickable(role = Role.Switch, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(9.dp).clip(CircleShape)
                .background(if (on) Brush.radialGradient(listOf(Color.White, lit, lit.copy(alpha = 0.75f))) else Brush.radialGradient(listOf(Vintage.ink.copy(alpha = 0.55f), Vintage.ink.copy(alpha = 0.8f))))
                .border(0.9.dp, Vintage.ink, CircleShape),
        )
        Spacer(Modifier.height(2.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Black, color = if (on) Vintage.ink else Vintage.ink.copy(alpha = 0.55f))
    }
}

/** The strip the dials are mounted on: darker paper with a stitched edge and a brass rivet in each corner, and a sprig of leaves. */
private fun DrawScope.drawStrip() {
    val corner = 20.dp.toPx()
    drawRoundRect(Vintage.parchmentDark.copy(alpha = 0.85f), cornerRadius = CornerRadius(corner))
    drawRoundRect(Vintage.edge, topLeft = Offset(1.dp.toPx(), 1.dp.toPx()), size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()), cornerRadius = CornerRadius(corner), style = Stroke(1.6.dp.toPx()))
    val inset = 6.dp.toPx()
    drawRoundRect(
        Vintage.brassDark, topLeft = Offset(inset, inset), size = Size(size.width - 2 * inset, size.height - 2 * inset), cornerRadius = CornerRadius(corner - 4.dp.toPx()),
        style = Stroke(1.1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx()))),
    )
    val rivet = 3.2.dp.toPx()
    val off = 13.dp.toPx()
    for (c in listOf(Offset(off, off), Offset(size.width - off, off), Offset(off, size.height - off), Offset(size.width - off, size.height - off))) {
        drawCircle(Vintage.brass, rivet, c)
        drawCircle(Vintage.ink, rivet, c, style = Stroke(0.9.dp.toPx()))
        drawLine(Vintage.ink, Offset(c.x - rivet * 0.6f, c.y - rivet * 0.6f), Offset(c.x + rivet * 0.6f, c.y + rivet * 0.6f), 0.9.dp.toPx())
    }
    // a sprig of three leaves at each end of the strip's top edge, peeping out from behind the dials
    fun sprig(x: Float, dir: Float) {
        val y = 4.dp.toPx()
        val stem = Path().apply { moveTo(x, y); quadraticTo(x + dir * 16.dp.toPx(), y - 1.dp.toPx(), x + dir * 30.dp.toPx(), y + 3.dp.toPx()) }
        drawPath(stem, Vintage.inkSoft, style = Stroke(1.dp.toPx(), cap = StrokeCap.Round))
        for (i in 0..2) {
            val lx = x + dir * (8 + i * 9).dp.toPx()
            val ly = y + (if (i % 2 == 0) -3 else 3).dp.toPx()
            drawOval(Color(0xFF6E8B4A).copy(alpha = 0.85f), Offset(lx - 4.dp.toPx(), ly - 1.6.dp.toPx()), Size(8.dp.toPx(), 3.2.dp.toPx()))
        }
    }
    sprig(26.dp.toPx(), 1f)
    sprig(size.width - 26.dp.toPx(), -1f)
}

/** A little compass rose; the red pointer turns to the way the traveller is walking (degrees clockwise from north). */
@Composable
private fun Compass(heading: Float, modifier: Modifier = Modifier) {
    val tm = rememberTextMeasurer()
    // The pointer always turns the short way round, never the long way past north.
    var angle by remember { mutableFloatStateOf(heading) }
    LaunchedEffect(heading) {
        var d = (heading - angle) % 360f
        if (d > 180f) d -= 360f
        if (d < -180f) d += 360f
        angle += d
    }
    val turn by animateFloatAsState(angle, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessVeryLow), label = "heading")
    Canvas(modifier) {
        val pen = Pen(this, size)
        with(pen) {
            val ring = circle(p(50f, 52f), 40f * u, 40)
            wash(ring, PAPER, 1f, 0f, 0f)
            inked(ring, 960, closed = true, width = line * 1.2f, calm = 0.25f)
            inked(circle(p(50f, 52f), 34f * u, 40), 961, closed = true, width = line * 0.6f, calm = 0.25f)
            // the four points of the rose, fixed, north up
            val south = listOf(p(50f, 88f), p(56f, 52f), p(44f, 52f))
            val north = listOf(p(50f, 16f), p(56f, 52f), p(44f, 52f))
            val east = listOf(p(86f, 52f), p(50f, 46f), p(50f, 58f))
            val west = listOf(p(14f, 52f), p(50f, 46f), p(50f, 58f))
            for ((i, pts) in listOf(north, south, east, west).withIndex()) { wash(pts, Vintage.brass, 0.55f, 0f, 0f); inked(pts, 962 + i, closed = true, width = line * 0.6f, calm = 0f) }
            // the heading pointer
            val c = Offset(50f, 52f)
            fun rot(dx: Float, dy: Float): Offset {
                val t = rad(turn)
                return p(c.x + dx * cos(t).toFloat() - dy * sin(t).toFloat(), c.y + dx * sin(t).toFloat() + dy * cos(t).toFloat())
            }
            val pointer = listOf(rot(0f, -36f), rot(7f, 4f), rot(0f, 0f), rot(-7f, 4f))
            wash(pointer, INK_RED, 0.95f, 0f, 0f); inked(pointer, 970, closed = true, width = line * 0.8f, calm = 0f)
            wash(circle(p(50f, 52f), 2.2f * u, 8), Vintage.ink, 1f, 0f, 0f)
            label(tm, "N", TextStyle(fontSize = 6.sp, fontWeight = FontWeight.Black, color = Vintage.ink), p(50f, 6f))
        }
    }
}

/** The brass knob that syncs the step count, with its name under it. */
@Composable
private fun SyncKnob(onSync: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Vintage.brassLight, Vintage.brass, Vintage.brassDark)))
                .border(1.6.dp, Vintage.ink, CircleShape).clickable(onClick = onSync),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = "同步", tint = Vintage.ink, modifier = Modifier.size(24.dp))
        }
        Text("同步", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Vintage.brassDark)
    }
}
