package com.starmist.walker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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
internal fun RouteMapView(
    route: RouteDef, walkedKm: Double, modifier: Modifier = Modifier, initialZoom: Float? = null,
    regionMode: Boolean = false, selectedTown: Int = -1, onTapTown: ((Int) -> Unit)? = null,
    /** Route mode: a collected card's marker was tapped (its id). */
    onTapCard: ((String) -> Unit)? = null,
    /** Route mode: the name of a roadside sight was tapped (its id). */
    onTapPoi: ((String) -> Unit)? = null,
    /** Route mode: a reached station's square was tapped (its id). */
    onTapStation: ((String) -> Unit)? = null,
    /** True while steps are being counted: the traveller walks. */
    walking: Boolean = false,
    /** Free exploration: the walker is off the route, going from township to township. */
    explore: ExploreView? = null,
) {
    val measurer = rememberMapMeasurer()
    var selectedPoi by remember { mutableStateOf<String?>(null) }   // a small sight's dot tapped on the map: its name is shown
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
            walker?.let { art -> withContext(Dispatchers.Default) { art.idleFrames(act) } }   // decode off the main thread so it does not stutter
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
    val heading = remember(route, walkedKm.toInt(), explore?.heading) {
        explore?.let { if (it.here.lng - (it.target?.lng ?: it.here.lng) < -1e-6) 1f else if (it.here.lng - (it.target?.lng ?: it.here.lng) > 1e-6) -1f else 0f } ?: walkerHeading(route, walkedKm)
    }
    var faceTarget by remember { mutableFloatStateOf(if (heading == 0f) 1f else heading) }
    LaunchedEffect(heading) { if (heading != 0f) faceTarget = heading }
    val face = animateFloatAsState(faceTarget, tween(260, easing = FastOutSlowInEasing), label = "face")
    val base = rememberArt("real_map_base")
    val tiles = rememberMapTiles()
    val areas = rememberTownAreas()
    val explored = remember(areas, route, (walkedKm * 2).toInt(), explore?.visited) { areas.visited(route, walkedKm).keys + explore?.visited.orEmpty() }
    // A small sight in a township not walked through yet lies in the fog: neither shown nor tapped.
    val visiblePois = remember(areas, route, explored) {
        if (explored.isEmpty()) null else route.pois.filter { p ->
            val id = areas.find(p.at.lat, p.at.lng).let { if (it >= 0) it else areas.townAt(p.at.lat, p.at.lng)?.let { t -> areas.towns.indexOf(t) } ?: -1 }
            id in explored
        }.map { it.id }.toSet()
    }
    val poiStamps = remember(areas, route) { PoiStamps.of(route, areas) }
    val earnedPois = remember(poiStamps, areas, route, (walkedKm * 4).toInt(), explore?.visited) {
        val byWalk = PoiStamps.earned(poiStamps, walkedKm).map { it.id }.toSet()
        val byExplore = explore?.visited.orEmpty().let { seen -> route.pois.filter { p -> areas.find(p.at.lat, p.at.lng) in seen }.map { it.id } }
        byWalk + byExplore
    }
    val currentTown = remember(areas, route, (walkedKm * 2).toInt(), explore?.current) { explore?.current ?: route.positionAt(walkedKm).let { areas.find(it.lat, it.lng) } }
    val here = explore?.here ?: route.positionAt(walkedKm)
    ZoomPanMap(
        aspect = REAL_MAP_ASPECT,
        focus = here.unit(),
        followKey = explore?.followKey ?: (walkedKm * 20).toInt(),
        modifier = modifier,
        initialZoom = initialZoom ?: if (base != null) 5.5f else 12f,
        maxZoom = 12f,
        background = SEA_COLOR,
        onTap = if (regionMode && onTapTown != null) { u, _ ->
            onTapTown(areas.find(MAP_LAT_MAX - u.y.toDouble() * (MAP_LAT_MAX - MAP_LAT_MIN), MAP_LON_MIN + u.x.toDouble() * (MAP_LON_MAX - MAP_LON_MIN)))
        } else if (!regionMode && (onTapCard != null || onTapPoi != null || onTapStation != null)) { u, perPx ->
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
            val tappedCard = best
            // A reached station's square, about 30 dp round it.
            var station: String? = null
            var stationPx = 30f * density
            route.stops.forEachIndexed { i, stop ->
                val sup = stop.supply ?: return@forEachIndexed
                if (route.cumulativeKm[i] > walkedKm + 1e-9) return@forEachIndexed
                val q = stop.at.unit()
                val d = kotlin.math.hypot((q.x - u.x) / perPx.x, (q.y - u.y) / perPx.y)
                if (d < stationPx) { stationPx = d; station = sup.id }
            }
            val tappedStation = station
            if (tappedCard != null) { selectedPoi = null; onTapCard?.invoke(tappedCard) }
            else if (tappedStation != null) { selectedPoi = null; onTapStation?.invoke(tappedStation) }
            else {
                // Otherwise the nearest small sight, about 38 dp round it: its name is shown, and its stamp too if it is earned.
                var poi: com.starmist.core.route.Poi? = null
                var poiPx = 38f * density
                route.pois.filter { visiblePois == null || it.id in visiblePois }.forEach { p ->
                    val q = p.at.unit()
                    val d = kotlin.math.hypot((q.x - u.x) / perPx.x, (q.y - u.y) / perPx.y)
                    if (d < poiPx) { poiPx = d; poi = p }
                }
                val hit = poi
                selectedPoi = hit?.id
                if (hit != null && hit.id in earnedPois) onTapPoi?.invoke(hit.id)
            }
        } else null,
        top = { w, h, _, _ -> drawWalker(Offset(here.unit().x * w, here.unit().y * h), w, walker, phase.floatValue, walk.floatValue, face.value, idleAct.intValue, idleT.floatValue, measurer) },
    ) { w, h, _, view -> drawRealMap(w, h, route, data, walkedKm, measurer, walker?.body, base, tiles, view, areas, explored, currentTown, regionMode, selectedTown, selectedPoi, visiblePois, earnedPois, explore) }
}

/** Small square picture for a card: the painted art inside the album frame once collected, an empty album slot before. */
@Composable
fun CardEmblem(card: Landmark, owned: Boolean, modifier: Modifier = Modifier) {
    val art = if (owned) rememberCardThumb(card.id) else null
    val frame = rememberArt("collection_slot_frame")
    val slot = rememberArt("collection_slot_locked")
    if (owned && frame != null) {
        Box(modifier) {
            if (art != null) Image(art, contentDescription = card.name, modifier = Modifier.matchParentSize().padding(4.dp).clip(RoundedCornerShape(2.dp)), contentScale = ContentScale.Crop)
            Image(frame, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
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

/**
 * A collected card, large: picture, where it is (county and township), its features, and where on the route it was found.
 * It is laid out to show everything without scrolling: the text takes the room it needs and the picture takes what is left.
 */
@Composable
fun CardFace(card: Landmark, atKm: Double) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
        val full = maxHeight
        val wide = maxWidth
        Column(Modifier.fillMaxWidth().heightIn(max = full), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val art = rememberArt("card_${card.id}")
            if (art != null) {
                Image(
                    art, contentDescription = card.name,
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(min = 72.dp, max = wide * 3 / 4)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                CardEmblem(card, true, Modifier.size(64.dp))
            }
            rememberTownAreas().cardPlaces[card.id]?.let { Text("📍 ${it.county} ${it.name}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Vintage.brassDark, modifier = Modifier.fillMaxWidth()) }
            rememberLandmarkFacts()[card.id].orEmpty().forEach { f ->
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(if (f.icon.isEmpty()) f.tag else "${f.icon} ${f.tag}", fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = Vintage.ink)
                    Text(f.fact, fontSize = 12.sp, lineHeight = 16.sp, color = Vintage.inkSoft, modifier = Modifier.fillMaxWidth())
                }
            }
            Text("路線第 ${atKm.roundToInt()} 公里", style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** A roadside sight's few lines, opened by tapping its name on the map. */
@Composable
fun PoiDialog(poi: com.starmist.core.route.Poi, onDismiss: () -> Unit) {
    val lines = rememberPoiIntros()[poi.id].orEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(poi.name) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PoiStamp(poi, Modifier.size(150.dp))
                lines.forEach { Text(it, fontSize = 14.sp, color = Vintage.ink, modifier = Modifier.fillMaxWidth()) }
                Text("走過這裡就蓋上的小章，不算景點卡。", fontSize = 12.sp, color = Vintage.inkSoft, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("關閉") } },
    )
}

private val POI_INK = Color(0xFF2F7F6E)

/** What a small sight is, for the foot of its stamp. */
private fun com.starmist.core.route.Poi.kindName() = when (kind) {
    com.starmist.core.route.PoiKind.HARBOR -> "漁港"
    com.starmist.core.route.PoiKind.TEMPLE -> "古剎"
    com.starmist.core.route.PoiKind.MUSEUM -> "博物館"
    com.starmist.core.route.PoiKind.SPRING -> "溫泉"
    com.starmist.core.route.PoiKind.WATERFALL -> "瀑布"
    com.starmist.core.route.PoiKind.PARK -> "公園"
    com.starmist.core.route.PoiKind.THEATER -> "表演場館"
    com.starmist.core.route.PoiKind.FORT -> "古蹟"
    com.starmist.core.route.PoiKind.LIGHTHOUSE -> "燈塔"
}

/** A round teal stamp for a small sight: its name, and what it is. */
@Composable
fun PoiStamp(poi: com.starmist.core.route.Poi, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val name = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = POI_INK)
    val small = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = POI_INK)
    Canvas(modifier.aspectRatio(1f)) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension / 2 - 2.dp.toPx()
        val k = size.minDimension / 150.dp.toPx()
        rotate(6f, c) {
            drawCircle(POI_INK.copy(alpha = 0.9f), r, c, style = Stroke(3.dp.toPx() * k))
            drawCircle(POI_INK.copy(alpha = 0.7f), r - 6.dp.toPx() * k, c, style = Stroke(1.dp.toPx() * k))
            val top = measurer.measure("小景點", small.copy(fontSize = small.fontSize * k))
            drawText(top, topLeft = Offset(c.x - top.size.width / 2f, c.y - r * 0.66f))
            val lines = if (poi.name.length > 5) poi.name.chunked((poi.name.length + 1) / 2) else listOf(poi.name)
            val laid = lines.map { measurer.measure(it, name.copy(fontSize = name.fontSize * k)) }
            val total = laid.sumOf { it.size.height }
            var y = c.y - total / 2f
            laid.forEach { drawText(it, topLeft = Offset(c.x - it.size.width / 2f, y)); y += it.size.height }
            val kind = measurer.measure(poi.kindName(), small.copy(fontSize = small.fontSize * k))
            drawText(kind, topLeft = Offset(c.x - kind.size.width / 2f, c.y + r * 0.46f))
        }
    }
}

/** A station's stamp tapped on the map: its name and when it opened. */
@Composable
fun StationDialog(supply: com.starmist.core.route.Supply, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(supply.name) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StationStamp(supply.name, supply.opened, Modifier.size(170.dp))
                Text("${supply.opened} 年開站。走到這裡就蓋上這枚紀念章，橘色的小方塊就是它。", fontSize = 13.sp, color = Vintage.inkSoft)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("關閉") } },
    )
}

/** A collected card opened over whatever is on screen (a card tapped on the map). */
@Composable
fun CardDialog(route: RouteDef, card: Landmark, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(card.name) },
        text = { CardFace(card, route.cards.first { it.first.id == card.id }.second) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("關閉") } },
    )
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
        is RouteEvent.PoiCollected -> {
            val lines = rememberPoiIntros()[event.poi.id].orEmpty()
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("蓋上小章：${event.poi.name}") },
                text = {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PoiStamp(event.poi, Modifier.size(150.dp))
                        lines.forEach { Text(it, fontSize = 14.sp, color = Vintage.ink, modifier = Modifier.fillMaxWidth()) }
                    }
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("收下") } },
            )
        }
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
