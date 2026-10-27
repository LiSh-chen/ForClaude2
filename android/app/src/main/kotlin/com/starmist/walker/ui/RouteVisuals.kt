package com.starmist.walker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starmist.core.route.GeoPoint
import com.starmist.core.route.Landmark
import com.starmist.core.route.RouteDef
import com.starmist.core.route.RouteEvent
import com.starmist.core.route.TaiwanOutline
import kotlin.math.cos
import kotlin.math.roundToInt

private val STAMP_INK = Color(0xFFB04A3A)

/** Which way the traveller faces on the screen: +1 right (east), -1 left (west), 0 when the route runs straight up or down here. */
internal fun walkerHeading(route: RouteDef, walkedKm: Double): Float {
    val a = route.positionAt(walkedKm).unit().x
    val ahead = route.positionAt((walkedKm + 1.0).coerceAtMost(route.totalKm)).unit().x
    val dx = if (walkedKm + 1.0 > route.totalKm) a - route.positionAt((walkedKm - 1.0).coerceAtLeast(0.0)).unit().x else ahead - a
    return if (dx > 0.0004f) 1f else if (dx < -0.0004f) -1f else 0f
}

/** The island walk as a map the player can pinch and drag; it opens on the walker and cannot zoom out past the whole island. */
@Composable
fun RouteMapView(
    route: RouteDef, walkedKm: Double, modifier: Modifier = Modifier, initialZoom: Float? = null,
    regionMode: Boolean = false, selectedTown: Int = -1, onTapTown: ((Int) -> Unit)? = null,
    /** Route mode: a collected card's marker was tapped (its id). */
    onTapCard: ((String) -> Unit)? = null,
    /** True while steps are being counted: the traveller walks. */
    walking: Boolean = false,
) {
    val measurer = rememberMapMeasurer()
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val data = rememberRealMapData(route)
    val walker = rememberWalkerArt()
    // The step cycle: one turn of [phase] is a pair of steps, and 0 and 0.5 are the standing (passing) poses. A walk starts
    // from one with a short run-up, and when it stops the step in progress is finished, slowing down, on the next one,
    // so the traveller never jumps between walking and standing.
    val phase = remember { mutableFloatStateOf(0f) }
    val walk = remember { mutableFloatStateOf(0f) }        // 0 standing .. 1 walking at full pace (eased)
    LaunchedEffect(walking) {
        var last = withFrameNanos { it }
        if (walking) {
            var t = 0f
            while (true) {
                val now = withFrameNanos { it }; val dt = (now - last) / 1e9f; last = now
                t += dt
                val up = minOf(1f, t / 0.35f)
                walk.floatValue = up
                phase.floatValue = (phase.floatValue + dt * (0.4f + 0.6f * up)) % 1f      // one second per pair of steps at full pace
            }
        } else {
            val start = phase.floatValue
            if (start == 0f || start == 0.5f) { walk.floatValue = 0f; return@LaunchedEffect }
            val target = if (start < 0.5f) 0.5f else 1f
            var p = start
            while (p < target) {
                val now = withFrameNanos { it }; val dt = (now - last) / 1e9f; last = now
                val left = target - p
                val down = minOf(1f, left / 0.2f)
                walk.floatValue = down
                p += dt * (0.3f + 0.7f * down)
                phase.floatValue = if (p >= target) target % 1f else p
            }
            phase.floatValue = target % 1f
            walk.floatValue = 0f
        }
    }
    // While it stands still the traveller does a little something every few seconds (see Idle); walking, or still finishing a step, stops it.
    val idleAct = remember { mutableIntStateOf(Idle.NONE) }
    val idleT = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(walking) {
        idleAct.intValue = Idle.NONE
        if (walking) return@LaunchedEffect
        while (walk.floatValue > 0f) delay(100)
        val dice = kotlin.random.Random(System.nanoTime())
        var previous = Idle.NONE
        while (true) {
            delay(3_500L + dice.nextInt(4_500))
            var act: Int
            do { act = 1 + dice.nextInt(Idle.COUNT) } while (act == previous)
            previous = act
            idleAct.intValue = act
            val begin = withFrameNanos { it }
            while (true) {
                val t = (withFrameNanos { it } - begin) / 1e6f / Idle.MS[act]
                if (t >= 1f) break
                idleT.floatValue = t
            }
            idleAct.intValue = Idle.NONE
            idleT.floatValue = 0f
        }
    }
    // Which way the traveller faces: the way the route runs on the screen a little ahead (kept when it runs straight up or down).
    val heading = remember(route, walkedKm.toInt()) { walkerHeading(route, walkedKm) }
    var faceTarget by remember { mutableFloatStateOf(if (heading == 0f) 1f else heading) }
    LaunchedEffect(heading) { if (heading != 0f) faceTarget = heading }
    val face = animateFloatAsState(faceTarget, tween(260, easing = FastOutSlowInEasing), label = "face")
    val base = rememberArt("real_map_base")
    val tiles = rememberMapTiles()
    val areas = rememberTownAreas()
    val explored = remember(areas, route, (walkedKm * 2).toInt()) { areas.visited(route, walkedKm).keys }
    val currentTown = remember(areas, route, (walkedKm * 2).toInt()) { route.positionAt(walkedKm).let { areas.find(it.lat, it.lng) } }
    val here = route.positionAt(walkedKm)
    ZoomPanMap(
        aspect = REAL_MAP_ASPECT,
        focus = here.unit(),
        followKey = (walkedKm * 20).toInt(),
        modifier = modifier,
        initialZoom = initialZoom ?: if (base != null) 5.5f else 12f,
        maxZoom = 12f,
        background = SEA_COLOR,
        onTap = if (regionMode && onTapTown != null) { u, _ ->
            onTapTown(areas.find(MAP_LAT_MAX - u.y.toDouble() * (MAP_LAT_MAX - MAP_LAT_MIN), MAP_LON_MIN + u.x.toDouble() * (MAP_LON_MAX - MAP_LON_MIN)))
        } else if (!regionMode && onTapCard != null) { u, perPx ->
            // The nearest collected card's marker within about 30 dp of the tap.
            val owned = route.collected(walkedKm).map { it.id }.toSet()
            var best: String? = null
            var bestPx = 30f * density
            route.stops.forEach { stop ->
                val id = stop.landmark?.id ?: return@forEach
                if (id !in owned) return@forEach
                val q = stop.at.unit()
                val d = kotlin.math.hypot((q.x - u.x) / perPx.x, (q.y - u.y) / perPx.y)
                if (d < bestPx) { bestPx = d; best = id }
            }
            best?.let(onTapCard)
        } else null,
        top = { w, h, _, _ -> drawWalker(Offset(here.unit().x * w, here.unit().y * h), w, walker, phase.floatValue, walk.floatValue, face.value, idleAct.intValue, idleT.floatValue, measurer) },
    ) { w, h, _, view -> drawRealMap(w, h, route, data, walkedKm, measurer, walker?.body, base, tiles, view, areas, explored, currentTown, regionMode, selectedTown) }
}

/** Small square picture for a card: the painted art inside the album frame once collected, an empty album slot before. */
@Composable
fun CardEmblem(card: Landmark, owned: Boolean, modifier: Modifier = Modifier) {
    val art = rememberArt("card_${card.id}")
    val frame = rememberArt("collection_slot_frame")
    val slot = rememberArt("collection_slot_locked")
    if (owned && art != null) {
        Box(modifier) {
            Image(art, contentDescription = card.name, modifier = Modifier.matchParentSize().padding(4.dp).clip(RoundedCornerShape(2.dp)), contentScale = ContentScale.Crop)
            if (frame != null) Image(frame, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
        }
    } else if (!owned && slot != null) {
        Image(slot, contentDescription = null, modifier = modifier, contentScale = ContentScale.Fit)
    } else {
        Box(
            modifier.clip(CircleShape).background(if (owned) Vintage.mossLight else Vintage.edge.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (owned) card.name.take(1) else "？", fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = Vintage.ink)
        }
    }
}

/** A collected card, large: picture, the ten-character line, and where on the route it was found. */
@Composable
fun CardFace(card: Landmark, atKm: Double) {
    // Scrolls: inside a dialog on a small phone the picture and three features can be taller than the room, and the text was cut off.
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val art = rememberArt("card_${card.id}")
        if (art != null) {
            Image(art, contentDescription = card.name, modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
        } else {
            CardEmblem(card, true, Modifier.size(96.dp))
        }
        rememberLandmarkFacts()[card.id].orEmpty().forEach { f ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(f.tag, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = Vintage.ink)
                Text(f.fact, fontSize = 14.sp, color = Vintage.inkSoft, modifier = Modifier.fillMaxWidth())
            }
        }
        Text("路線第 ${atKm.roundToInt()} 公里", style = MaterialTheme.typography.labelMedium)
    }
}

/** A round station stamp: station name and the year the station opened. */
@Composable
fun StationStamp(name: String, opened: String, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val big = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = STAMP_INK)
    val small = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = STAMP_INK)
    Canvas(modifier.aspectRatio(1f)) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension / 2 - 2.dp.toPx()
        rotate(-8f, c) {
            drawCircle(STAMP_INK.copy(alpha = 0.9f), r, c, style = Stroke(3.dp.toPx()))
            drawCircle(STAMP_INK.copy(alpha = 0.7f), r - 6.dp.toPx(), c, style = Stroke(1.dp.toPx()))
            val top = measurer.measure("台鐵車站", small)
            drawText(top, topLeft = Offset(c.x - top.size.width / 2f, c.y - r * 0.62f))
            val title = measurer.measure(name, big)
            drawText(title, topLeft = Offset(c.x - title.size.width / 2f, c.y - title.size.height / 2f))
            val year = measurer.measure("$opened 年開站", small)
            drawText(year, topLeft = Offset(c.x - year.size.width / 2f, c.y + r * 0.38f))
        }
    }
}

/** One thing that happened on the route, shown when it is reached. */
@Composable
fun RouteEventDialog(event: RouteEvent, onDismiss: () -> Unit) {
    when (event) {
        is RouteEvent.CardCollected -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("獲得景點卡：${event.landmark.name}") },
            text = { CardFace(event.landmark, event.atKm) },
            confirmButton = { TextButton(onClick = onDismiss) { Text("收下") } },
        )
        is RouteEvent.StampCollected -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("蓋上紀念章") },
            text = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StationStamp(event.supply.name, event.supply.opened, Modifier.size(170.dp))
                    Text("${event.supply.name}・${event.supply.opened} 年開站", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("收下") } },
        )
        is RouteEvent.Transit -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("${event.by}前往${event.into}") },
            text = { Text("這一段不能步行，直接${event.by}抵達，不計步數。") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("上車") } },
        )
        RouteEvent.Finished -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("環島完成！") },
            text = { Text("你用自己的腳步繞完了整座島。景點卡和紀念章都留在「路線」頁。") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("太棒了") } },
        )
    }
}
