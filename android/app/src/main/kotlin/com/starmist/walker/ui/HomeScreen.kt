package com.starmist.walker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.delay
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.starmist.core.metrics.BodyMetrics
import com.starmist.core.route.RouteDef
import com.starmist.core.route.Routes
import com.starmist.walker.data.UserSettings
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** What the home map shows. */
internal sealed interface HomeMap {
    data object Preparing : HomeMap
    data object ChooseRoute : HomeMap
    data class Real(val route: RouteDef, val walkedKm: Double, val zoom: Float? = null) : HomeMap
}

/** The township being walked through, for the card above the map. */
internal data class RegionInfo(val area: TownArea, val tags: List<TownTag>?, val foundMask: Int, val walkedKm: Double, val order: Int, val explored: Boolean)

private val GREEN = Color(0xFF3C8D5A)
private val BLUE = Color(0xFF0E86D4)
private val RED = Color(0xFFB3202A)

/**
 * What opens first: the township being walked through (card), the island map zoomed in on the walker, and a
 * small dashboard with today's steps and the way ahead.
 */
@Composable
fun HomeScreen(
    vm: AppViewModel,
    settings: UserSettings,
    padding: PaddingValues,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    /** A collected card's marker on the route map was tapped: open that card in the collection. */
    onOpenCard: (String) -> Unit = {},
    /** The name of a roadside sight on the route map was tapped: show its introduction. */
    onOpenPoi: (String) -> Unit = {},
    /** A reached station's square on the route map was tapped: show its stamp. */
    onOpenStation: (String) -> Unit = {},
) {
    val steps by vm.todaySteps.collectAsStateWithLifecycle()
    val routeState by vm.routeState.collectAsStateWithLifecycle()
    val granted by vm.permissionGranted.collectAsStateWithLifecycle()
    val hasCounter by vm.hasHardwareCounter.collectAsStateWithLifecycle()
    val lastSnapshot by vm.lastSnapshotAt.collectAsStateWithLifecycle()
    val areas = rememberTownAreas()
    val blurbs = rememberTownBlurbs()
    val logs by vm.regionLogs.collectAsStateWithLifecycle()

    val profile = settings.profile
    val route = routeState?.let { Routes.byId(it.routeId) }
    val walkedKm = routeState?.walkedKm ?: 0.0
    val map: HomeMap = if (route == null) HomeMap.ChooseRoute else HomeMap.Real(route, walkedKm)
    val visited = remember(areas, route, (walkedKm * 2).toInt()) { if (route == null) emptyMap() else areas.visited(route, walkedKm) }
    val currentTown = remember(areas, route, (walkedKm * 2).toInt()) { route?.positionAt(walkedKm)?.let { areas.find(it.lat, it.lng) } ?: -1 }
    val infoOf: (Int) -> RegionInfo? = { id ->
        if (id !in areas.towns.indices) null else {
            val t = areas.towns[id]
            val key = "${t.county}|${t.name}"
            RegionInfo(t, blurbs[key], logs[key]?.mask ?: 0, visited[id] ?: 0.0, visited.keys.indexOf(id) + 1, id in visited)
        }
    }

    HomeContent(
        padding = padding,
        steps = steps,
        goal = settings.dailyGoal,
        km = BodyMetrics.distanceKm(steps, profile),
        kcal = BodyMetrics.kcal(steps, profile),
        lastSnapshot = lastSnapshot,
        granted = granted,
        hasCounter = hasCounter,
        map = map,
        onRequestPermission = onRequestPermission,
        onOpenAppSettings = onOpenAppSettings,
        onSync = { vm.refresh(announce = true) },
        onStartRoute = { vm.startRoute(it) },
        onOpenCard = onOpenCard,
        onOpenPoi = onOpenPoi,
        onOpenStation = onOpenStation,
        strideM = BodyMetrics.strideMeters(profile),
        currentTown = currentTown,
        exploredTowns = visited.keys,
        infoOf = infoOf,
    )
}

/**
 * True for a few seconds after the step count goes up a little, so the traveller walks only while the player does.
 * A big jump (the count loading when the app opens, or a batch of steps arriving at once) is not walking.
 */
@Composable
private fun rememberWalking(steps: Long): Boolean {
    var last by remember { mutableLongStateOf(steps) }
    var walking by remember { mutableStateOf(false) }
    LaunchedEffect(steps) {
        val added = steps - last
        last = steps
        if (added in 1..25) {
            walking = true
            delay(4_000)
            walking = false
        }
    }
    return walking
}

/** The home page as plain data in, so it can also be drawn without the app's state (screenshots). */
@Composable
internal fun HomeContent(
    padding: PaddingValues,
    steps: Long,
    goal: Int,
    km: Double,
    kcal: Double,
    lastSnapshot: Long?,
    granted: Boolean,
    hasCounter: Boolean,
    map: HomeMap,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onSync: () -> Unit,
    onStartRoute: (String) -> Unit,
    onOpenCard: (String) -> Unit = {},
    onOpenPoi: (String) -> Unit = {},
    onOpenStation: (String) -> Unit = {},
    strideM: Double = 0.7,
    currentTown: Int = -1,
    exploredTowns: Set<Int> = emptySet(),
    infoOf: (Int) -> RegionInfo? = { null },
    initialLayers: MapLayers = MapLayers(),
    initialControlsOpen: Boolean = false,
    initialPicked: Int = -1,
) {
    var showSpots by rememberSaveable { mutableStateOf(initialLayers.spots) }
    var showRoute by rememberSaveable { mutableStateOf(initialLayers.route) }
    var showRegions by rememberSaveable { mutableStateOf(initialLayers.regions) }
    var picked by rememberSaveable { mutableStateOf(initialPicked) }
    val layers = MapLayers(showSpots, showRoute, showRegions)
    val regionMode = showRegions && picked >= 0       // the panel under the map tells about the township picked, and only then
    var controlsOpen by rememberSaveable { mutableStateOf(initialControlsOpen) }   // the console, pulled up over the dials
    val walking = rememberWalking(steps)
    // The dashboard runs nearly edge to edge so that the dials can be big; the rest keeps a margin.
    Column(Modifier.padding(padding).padding(horizontal = 4.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!granted) Box(Modifier.padding(horizontal = 6.dp)) { PermissionCard(onRequestPermission, onOpenAppSettings) }
        if (!hasCounter) Box(Modifier.padding(horizontal = 6.dp)) { NoticeCard("這台裝置沒有硬體計步器，目前版本無法計步。") }

        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 6.dp).clip(RoundedCornerShape(14.dp))) {
            when (map) {
                HomeMap.Preparing -> Text("準備中……", Modifier.align(Alignment.Center))
                HomeMap.ChooseRoute ->
                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        RoutePicker(onStart = onStartRoute)
                    }
                is HomeMap.Real -> {
                    RouteMapView(
                        map.route, map.walkedKm, Modifier.fillMaxWidth(), initialZoom = map.zoom,
                        layers = layers, selectedTown = if (regionMode) picked else -1,
                        onTapTown = { id -> picked = if (id >= 0) id else -1 },
                        onTapCard = onOpenCard,
                        onTapPoi = onOpenPoi,
                        onTapStation = onOpenStation,
                        walking = walking,
                    )
                }
            }
        }

        if (map is HomeMap.Real) {
            val heading = remember(map.route, (map.walkedKm * 5).toInt()) { routeHeading(map.route, map.walkedKm) }
            val areas = rememberTownAreas()
            val border = remember(areas, map.route, (map.walkedKm * 10).toInt()) { areas.nextBorder(map.route, map.walkedKm) }
            BottomDeck(
                controlsOpen = controlsOpen, onControlsOpen = { controlsOpen = it }, regionMode = regionMode,
                instruments = { InstrumentPanel(map.route, map.walkedKm, goal, steps, border, Modifier.fillMaxWidth()) },
                regionInfo = { RegionPlate(infoOf(picked), picked == currentTown, it) },
                console = {
                    ConsoleDeck(
                        layers, { new ->
                            // the keys only say what is shown on the map; the panel stays as it is
                            showSpots = new.spots; showRoute = new.route; showRegions = new.regions
                            if (!new.regions) picked = -1
                        },
                        heading, lastSnapshot, onSync, steps, km, kcal, border?.kmDone, map.walkedKm,
                    )
                },
            )
        } else {
            Dashboard(steps, goal, km, kcal, lastSnapshot, onSync)
        }
    }
}


// ---- the region card -------------------------------------------------------------------------

/** The township picked on the map (the one being walked through at first): its name and three features, each found on a different visit. */
@Composable
private fun RegionPanel(region: RegionInfo?, isCurrent: Boolean, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (region == null) {
            Text("點一下走過的區域", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("藍色的區域是你走過的，點它看這裡的特色。", fontSize = 12.sp, color = Vintage.inkSoft)
            return@Column
        }
        val a = region.area
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // The name is told only when the township has been walked through.
            Text(if (region.explored) a.name else "？？？", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF0058C8), maxLines = 1)
            if (region.explored) Text(a.county, fontSize = 11.sp, color = Vintage.inkSoft, modifier = Modifier.padding(bottom = 2.dp), maxLines = 1)
            Spacer(Modifier.weight(1f))
            Text(if (isCurrent) "目前所在" else if (region.explored) "第 ${region.order} 區" else "", fontSize = 11.sp, color = Vintage.brassDark, modifier = Modifier.padding(bottom = 2.dp))
        }
        val tags = region.tags
        if (!region.explored || tags == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { repeat(3) { LockedTag(tags?.getOrNull(it)?.icon.orEmpty()) } }
            Text(if (!region.explored) "還沒走到這裡" else "特色整理中", fontSize = 12.sp, color = Vintage.inkSoft)
            return@Column
        }
        val newest = tags.indices.lastOrNull { region.foundMask and (1 shl it) != 0 }
        var picked by remember(a.name, region.foundMask) { mutableStateOf(newest) }
        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            tags.forEachIndexed { i, t ->
                val found = region.foundMask and (1 shl i) != 0
                if (!found) LockedTag(t.icon) else {
                    val on = picked == i
                    Text(
                        if (t.icon.isEmpty()) t.tag else "${t.icon} ${t.tag}", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                        color = if (on) Color.White else Color(0xFF0058C8),
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (on) Color(0xFF1A8CFF) else Color(0xFF1A8CFF).copy(alpha = 0.14f))
                            .clickable { picked = i }.padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
            }
        }
        val p = picked
        // No line limit: the fact can be seventy characters and the panel grows to fit it instead of cutting it off.
        Text(if (p != null) "你知道嗎？${tags[p].fact}" else "再走一段，就會發現這裡的特色。", fontSize = 11.sp, color = Vintage.inkSoft)
    }
}

/** A feature not found yet: its little picture and question marks, so there is something to guess (a padlock when it has no picture). */
@Composable
private fun LockedTag(icon: String = "") {
    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Vintage.edge.copy(alpha = 0.22f)).padding(horizontal = 10.dp, vertical = 3.dp)) {
        if (icon.isNotEmpty()) Text("$icon ？？？", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Vintage.inkSoft, maxLines = 1)
        else Canvas(Modifier.size(width = 12.dp, height = 14.dp)) {
            val c = Vintage.inkSoft.copy(alpha = 0.7f)
            val w = size.width; val h = size.height
            drawArc(c, 180f, 180f, false, Offset(w * 0.2f, h * 0.02f), Size(w * 0.6f, h * 0.6f), style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round))
            drawRoundRect(c, Offset(0f, h * 0.42f), Size(w, h * 0.58f), CornerRadius(2.dp.toPx()))
        }
    }
}

// ---- the dashboard ---------------------------------------------------------------------------

/** Before a route is chosen: today's steps as a ring. */
@Composable
private fun Dashboard(steps: Long, goal: Int, km: Double, kcal: Double, lastSnapshot: Long?, onSync: () -> Unit) {
    val progress = BodyMetrics.goalProgress(steps, goal).toFloat().coerceIn(0f, 1f)
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.Transparent), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                StepRing(steps, goal, progress, 96.dp)
                Spacer(Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("今日走了 ${km.twoDecimals()} 公里，消耗約 ${kcal.roundToInt()} 大卡", fontSize = 12.sp, color = Vintage.inkSoft)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onSync) { Text("同步", fontSize = 12.sp) }
            }
        }
    }
}

/**
 * The panel under the map: one strip with the dials on it (or, while a township is picked on the map, what it holds) and, below them on the same
 * strip, the control console, as tall as they are. The panel shows the top part; pulled up from the bottom edge (or by the tab there) it slides up with the
 * finger by its whole height, so that at rest only one of the two is in view. The window is the same height whichever is shown, so that the map does not jump.
 */
@Composable
private fun BottomDeck(
    controlsOpen: Boolean, onControlsOpen: (Boolean) -> Unit, regionMode: Boolean,
    instruments: @Composable () -> Unit, regionInfo: @Composable (Modifier) -> Unit, console: @Composable () -> Unit,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    var faceHeight by remember { mutableStateOf(204.dp) }       // the height the dials take; the township card is made as tall
    val slidePx = with(density) { faceHeight.toPx() }        // the whole height: at the end of the slide only the console is in view
    val slide = remember { androidx.compose.animation.core.Animatable(if (controlsOpen) 1f else 0f) }     // 0: the dials, 1: the console
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val settle = androidx.compose.animation.core.spring<Float>(dampingRatio = 0.85f, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)
    androidx.compose.runtime.LaunchedEffect(controlsOpen) { slide.animateTo(if (controlsOpen) 1f else 0f, settle) }
    Box(
        Modifier.fillMaxWidth().height(faceHeight + 12.dp).clip(RoundedCornerShape(20.dp)).draggable(
            orientation = androidx.compose.foundation.gestures.Orientation.Vertical,
            state = androidx.compose.foundation.gestures.rememberDraggableState { delta -> scope.launch { slide.snapTo((slide.value - delta / slidePx).coerceIn(0f, 1f)) } },
            onDragStopped = { velocity ->
                val open = if (kotlin.math.abs(velocity) > 500f) velocity < 0f else slide.value > 0.5f
                onControlsOpen(open)
                scope.launch { slide.animateTo(if (open) 1f else 0f, settle) }
            },
        ),
    ) {
        Canvas(Modifier.matchParentSize()) { drawStrip() }
        // the part inside the stitching: the dials above, the console below, the whole of it slid up by the finger
        Box(Modifier.fillMaxSize().padding(6.dp).clip(RoundedCornerShape(15.dp))) {
            Column(
                Modifier.fillMaxWidth().wrapContentHeight(align = Alignment.Top, unbounded = true)
                    .offset { androidx.compose.ui.unit.IntOffset(0, -(slide.value * slidePx).roundToInt()) },
            ) {
                if (regionMode) regionInfo(Modifier.fillMaxWidth().height(faceHeight))
                else Box(Modifier.fillMaxWidth().heightIn(min = 186.dp).onSizeChanged { faceHeight = with(density) { it.height.toDp() }.coerceAtLeast(186.dp) }, contentAlignment = Alignment.Center) { instruments() }
                Box(Modifier.fillMaxWidth().height(faceHeight)) { console() }
            }
        }
        GripTab(controlsOpen, { onControlsOpen(!controlsOpen) }, Modifier.align(Alignment.BottomCenter))
    }
}

/** What the township picked holds, on a paper plate set into the strip where the dials would be. */
@Composable
private fun RegionPlate(region: RegionInfo?, isCurrent: Boolean, modifier: Modifier) {
    Box(modifier.padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 12.dp).clip(RoundedCornerShape(12.dp)).background(Vintage.parchmentLight.copy(alpha = 0.92f)).padding(12.dp)) {
        RegionPanel(region, isCurrent, Modifier.fillMaxWidth())
    }
}

/** The way the traveller is walking: from a little behind to a little ahead on the route, degrees clockwise from north. */
private fun routeHeading(route: RouteDef, walkedKm: Double): Float {
    val a = route.positionAt((walkedKm - 0.4).coerceAtLeast(0.0))
    val b = route.positionAt((walkedKm + 0.4).coerceAtMost(route.totalKm))
    val east = (b.lng - a.lng) * kotlin.math.cos(Math.toRadians(a.lat))
    val north = b.lat - a.lat
    return if (east == 0.0 && north == 0.0) 0f else ((Math.toDegrees(kotlin.math.atan2(east, north)) + 360.0) % 360.0).toFloat()
}

/** Today's steps against the goal: a ring with the count inside. */
@Composable
private fun StepRing(steps: Long, goal: Int, progress: Float, size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = this.size.width / 9.6f
            val inset = stroke / 2
            drawArc(Vintage.edge.copy(alpha = 0.3f), -90f, 360f, false, Offset(inset, inset), Size(this.size.width - stroke, this.size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(Color(0xFF6B4A24), -90f, 360f * progress, false, Offset(inset, inset), Size(this.size.width - stroke, this.size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(steps.withCommas(), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = if (size >= 90.dp) 19.sp else 16.sp, maxLines = 1)
            Text("/ ${goal.toLong().withCommas()}", fontSize = 10.sp, color = Vintage.inkSoft, maxLines = 1)
        }
    }
}

/**
 * The way ahead as a car's instrument cluster: the needles and lamps say how things stand, the numbers are one tap away
 * (a dialog with the exact readings).
 */
@Composable
private fun InstrumentPanel(route: RouteDef, walkedKm: Double, goal: Int, steps: Long, border: TownAreas.RegionBorder?, modifier: Modifier) {
    val overall = (walkedKm / route.totalKm).coerceIn(0.0, 1.0)
    InstrumentCluster(steps, goal, overall.toFloat(), border?.fraction?.toFloat(), modifier)
}

@Composable
private fun PermissionCard(onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("需要「動作與健身」權限", fontWeight = FontWeight.Bold)
            Text(
                "只用來讀取手機內建的計步器，不會取得位置、聯絡人或其他資料。",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 6.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRequest) { Text("授予權限") }
                OutlinedButton(onClick = onOpenSettings) { Text("開啟系統設定") }
            }
        }
    }
}

@Composable
private fun NoticeCard(text: String) {
    Card(Modifier.fillMaxWidth()) { Text(text, Modifier.padding(12.dp)) }
}
