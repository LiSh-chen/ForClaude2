package com.starmist.walker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
    strideM: Double = 0.7,
    currentTown: Int = -1,
    exploredTowns: Set<Int> = emptySet(),
    infoOf: (Int) -> RegionInfo? = { null },
    initialRegionMode: Boolean = false,
) {
    var regionMode by rememberSaveable { mutableStateOf(initialRegionMode) }
    var picked by rememberSaveable { mutableStateOf(-1) }
    val walking = rememberWalking(steps)
    val shown = if (picked >= 0) picked else currentTown
    Column(Modifier.padding(padding).padding(horizontal = 10.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!granted) PermissionCard(onRequestPermission, onOpenAppSettings)
        if (!hasCounter) NoticeCard("這台裝置沒有硬體計步器，目前版本無法計步。")

        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(14.dp))) {
            when (map) {
                HomeMap.Preparing -> Text("準備中……", Modifier.align(Alignment.Center))
                HomeMap.ChooseRoute ->
                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        RoutePicker(onStart = onStartRoute)
                    }
                is HomeMap.Real -> {
                    RouteMapView(
                        map.route, map.walkedKm, Modifier.fillMaxWidth(), initialZoom = map.zoom,
                        regionMode = regionMode, selectedTown = if (regionMode) shown else -1,
                        onTapTown = { id -> picked = if (id >= 0) id else -1 },
                        onTapCard = onOpenCard,
                        onTapPoi = onOpenPoi,
                        walking = walking,
                    )
                    ModeSwitch(regionMode, Modifier.align(Alignment.TopEnd).padding(8.dp)) { regionMode = it; if (!it) picked = -1 }
                }
            }
        }

        Dashboard(steps, goal, km, kcal, lastSnapshot, onSync, (map as? HomeMap.Real), strideM, regionMode, infoOf(shown), shown == currentTown, walking)
    }
}


/** Two small buttons on the map: the route with its stops, or the townships walked through. */
@Composable
private fun ModeSwitch(regionMode: Boolean, modifier: Modifier, onChange: (Boolean) -> Unit) {
    Row(modifier.clip(RoundedCornerShape(16.dp)).background(Vintage.parchmentLight.copy(alpha = 0.95f)).padding(2.dp)) {
        listOf(false to "路線", true to "區域").forEach { (mode, text) ->
            val on = regionMode == mode
            Text(
                text, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                color = if (on) Color.White else Vintage.inkSoft,
                modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(if (on) (if (mode) Color(0xFF1A8CFF) else Color(0xFFB3202A)) else Color.Transparent)
                    .clickable { onChange(mode) }.padding(horizontal = 12.dp, vertical = 5.dp),
            )
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

/** Today's steps as a ring; beside it the region panel (region mode) or, on the route, the day's walk with the way ahead as a table below. */
@Composable
private fun Dashboard(steps: Long, goal: Int, km: Double, kcal: Double, lastSnapshot: Long?, onSync: () -> Unit, map: HomeMap.Real?, strideM: Double, regionMode: Boolean, region: RegionInfo?, isCurrent: Boolean, walking: Boolean) {
    val progress = BodyMetrics.goalProgress(steps, goal).toFloat().coerceIn(0f, 1f)
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Vintage.parchmentLight), elevation = CardDefaults.cardElevation(3.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (map != null && !regionMode) {
                InstrumentPanel(map.route, map.walkedKm, strideM, goal, steps, progress, km, kcal, walking, onSync, Modifier.fillMaxWidth())
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    StepRing(steps, goal, progress, 96.dp)
                    if (map != null) RegionPanel(region, isCurrent, Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("今日走了 ${km.twoDecimals()} 公里，消耗約 ${kcal.roundToInt()} 大卡", fontSize = 12.sp, color = Vintage.inkSoft)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onSync) { Text("同步", fontSize = 12.sp) }
                }
            }
        }
    }
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
private fun InstrumentPanel(route: RouteDef, walkedKm: Double, strideM: Double, goal: Int, steps: Long, progress: Float, todayKm: Double, kcal: Double, walking: Boolean, onSync: () -> Unit, modifier: Modifier) {
    val stride = strideM.coerceAtLeast(0.3)
    fun stepsFor(km: Double) = (km * 1000.0 / stride).roundToLong().coerceAtLeast(0L)
    fun days(steps: Long) = if (goal <= 0) 0L else (steps + goal - 1) / goal
    val areas = rememberTownAreas()
    val border = remember(areas, route, (walkedKm * 10).toInt()) { areas.nextBorder(route, walkedKm) }
    val next = route.next(walkedKm)
    val collected = route.collected(walkedKm)
    val totalCards = route.stops.count { it.landmark != null }
    val overall = (walkedKm / route.totalKm).coerceIn(0.0, 1.0)
    var detail by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        InstrumentCluster(
            steps, goal, walkedKm, next?.second, overall.toFloat(), border?.fraction?.toFloat(),
            Modifier.clickable { detail = true },
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
            Telltale(0, walking, GREEN, Modifier.width(36.dp))
            Telltale(1, steps >= goal, Color(0xFFE0A21B), Modifier.width(36.dp))
            Telltale(2, next != null && next.second <= 2.0, RED, Modifier.width(36.dp))
            Telltale(3, border != null && border.kmLeft <= 1.5, BLUE, Modifier.width(36.dp))
            Icon(
                Icons.Filled.Refresh, contentDescription = "同步", tint = Vintage.inkSoft,
                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onSync).padding(3.dp),
            )
        }
    }
    if (detail) {
        AlertDialog(
            onDismissRequest = { detail = false },
            confirmButton = { TextButton(onClick = { detail = false }) { Text("好") } },
            title = { Text("儀表讀數", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("今日步數　${steps.withCommas()} / ${goal.toLong().withCommas()} 步（${todayKm.twoDecimals()} 公里，約 ${kcal.roundToInt()} 大卡）", fontSize = 13.sp)
                    if (next != null) {
                        val s = stepsFor(next.second)
                        Text("下一景點　${next.first.name}，還有 ${"%.1f".format(next.second)} 公里（${s.withCommas()} 步，約 ${days(s)} 天）", fontSize = 13.sp)
                    } else Text("景點全部收集完成", fontSize = 13.sp)
                    if (border != null) {
                        val s = stepsFor(border.kmLeft)
                        Text("離開此區域　還有 ${"%.1f".format(border.kmLeft)} 公里（${s.withCommas()} 步，約 ${days(s)} 天）", fontSize = 13.sp)
                    }
                    val left = route.totalKm - walkedKm
                    Text("環島全程　${(overall * 100).roundToInt()}%，已走 ${"%.1f".format(walkedKm)} 公里，剩 ${"%.0f".format(left)} 公里（約 ${days(stepsFor(left))} 天）", fontSize = 13.sp)
                    Text("景點　${collected.size} / $totalCards", fontSize = 13.sp)
                    Text("燈號：走路中、達成目標、景點在 2 公里內、區域邊界在 1.5 公里內。", fontSize = 11.sp, color = Vintage.inkSoft)
                }
            },
        )
    }
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
