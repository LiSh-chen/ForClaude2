package com.starmist.walker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
internal data class RegionInfo(val area: TownArea, val blurb: TownBlurb?, val walkedKm: Double, val exploredCount: Int)

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
) {
    val steps by vm.todaySteps.collectAsStateWithLifecycle()
    val routeState by vm.routeState.collectAsStateWithLifecycle()
    val granted by vm.permissionGranted.collectAsStateWithLifecycle()
    val hasCounter by vm.hasHardwareCounter.collectAsStateWithLifecycle()
    val lastSnapshot by vm.lastSnapshotAt.collectAsStateWithLifecycle()
    val areas = rememberTownAreas()
    val blurbs = rememberTownBlurbs()

    val profile = settings.profile
    val route = routeState?.let { Routes.byId(it.routeId) }
    val walkedKm = routeState?.walkedKm ?: 0.0
    val map: HomeMap = if (route == null) HomeMap.ChooseRoute else HomeMap.Real(route, walkedKm)
    val region = remember(areas, route, (walkedKm * 2).toInt()) {
        if (route == null) null else {
            val g = route.positionAt(walkedKm)
            val id = areas.find(g.lat, g.lng)
            if (id < 0) null else {
                val visited = areas.visited(route, walkedKm)
                val t = areas.towns[id]
                RegionInfo(t, blurbs["${t.county}|${t.name}"], visited[id] ?: 0.0, visited.size)
            }
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
        strideM = BodyMetrics.strideMeters(profile),
        region = region,
    )
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
    strideM: Double = 0.7,
    region: RegionInfo? = null,
) {
    Column(Modifier.padding(padding).padding(horizontal = 10.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!granted) PermissionCard(onRequestPermission, onOpenAppSettings)
        if (!hasCounter) NoticeCard("這台裝置沒有硬體計步器，目前版本無法計步。")
        if (map is HomeMap.Real) RegionCard(region)

        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(14.dp))) {
            when (map) {
                HomeMap.Preparing -> Text("準備中……", Modifier.align(Alignment.Center))
                HomeMap.ChooseRoute ->
                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        RoutePicker(onStart = onStartRoute)
                    }
                is HomeMap.Real -> RouteMapView(map.route, map.walkedKm, Modifier.fillMaxWidth(), initialZoom = map.zoom)
            }
        }

        Dashboard(steps, goal, km, kcal, lastSnapshot, onSync, (map as? HomeMap.Real), strideM)
    }
}


// ---- the region card -------------------------------------------------------------------------

/** The township being walked through: its name, one line that makes it memorable, three tags and one "did you know". */
@Composable
private fun RegionCard(region: RegionInfo?) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Vintage.parchmentLight), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (region == null) {
                Text("出發吧！", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("走進一個新的鄉鎮，這裡就會出現它的介紹。", style = MaterialTheme.typography.bodySmall)
                return@Column
            }
            val a = region.area
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(a.name, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color(0xFF0B4A78))
                Text(a.county, style = MaterialTheme.typography.labelMedium, color = Vintage.inkSoft, modifier = Modifier.padding(bottom = 3.dp))
                Spacer(Modifier.weight(1f))
                Text("第 ${region.exploredCount} 區", style = MaterialTheme.typography.labelMedium, color = Vintage.brassDark, modifier = Modifier.padding(bottom = 3.dp))
            }
            val b = region.blurb
            if (b != null) {
                Text(b.headline, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Vintage.ink, maxLines = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    b.tags.forEach { t ->
                        Text(t, fontSize = 11.sp, color = Color(0xFF0B4A78), modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF0E86D4).copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 2.dp), maxLines = 1)
                    }
                }
                Text("你知道嗎？${b.fact}", fontSize = 12.sp, color = Vintage.inkSoft, maxLines = 2)
            } else {
                Text("${terrainOf(a.medianElevM)}地帶的小角落，介紹整理中。", style = MaterialTheme.typography.bodySmall, color = Vintage.inkSoft)
            }
        }
    }
}

private fun terrainOf(medianElevM: Int) = when {
    medianElevM < 40 -> "平原"
    medianElevM < 300 -> "台地丘陵"
    medianElevM < 1000 -> "山地"
    else -> "高山"
}

// ---- the dashboard ---------------------------------------------------------------------------

/** Today's steps as a ring, then the way ahead as two bars: to the next stop, and around the island. */
@Composable
private fun Dashboard(steps: Long, goal: Int, km: Double, kcal: Double, lastSnapshot: Long?, onSync: () -> Unit, map: HomeMap.Real?, strideM: Double) {
    val progress = BodyMetrics.goalProgress(steps, goal).toFloat().coerceIn(0f, 1f)
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Vintage.parchmentLight), elevation = CardDefaults.cardElevation(3.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(96.dp)) {
                        val stroke = 10.dp.toPx()
                        val inset = stroke / 2
                        drawArc(Vintage.edge.copy(alpha = 0.3f), -90f, 360f, false, Offset(inset, inset), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
                        drawArc(Color(0xFF6B4A24), -90f, 360f * progress, false, Offset(inset, inset), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(steps.withCommas(), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 19.sp, maxLines = 1)
                        Text("/ ${goal.toLong().withCommas()}", fontSize = 10.sp, color = Vintage.inkSoft, maxLines = 1)
                    }
                }
                if (map != null) RouteBars(map.route, map.walkedKm, strideM, goal, Modifier.weight(1f))
                else Spacer(Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${(progress * 100).roundToInt()}%　${km.twoDecimals()} 公里　${kcal.roundToInt()} 大卡", fontSize = 12.sp, color = Vintage.inkSoft)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onSync) { Text(lastSnapshot?.let { formatTime(it) } ?: "同步", fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun RouteBars(route: RouteDef, walkedKm: Double, strideM: Double, goal: Int, modifier: Modifier) {
    val stride = strideM.coerceAtLeast(0.3)
    fun steps(km: Double) = (km * 1000.0 / stride).roundToLong().coerceAtLeast(0L)
    fun days(steps: Long) = if (goal <= 0) 0L else (steps + goal - 1) / goal
    val next = route.next(walkedKm)
    val collected = route.collected(walkedKm)
    val totalCards = route.stops.count { it.landmark != null }
    val legStart = collected.lastOrNull()?.let { c -> route.stops.indexOfFirst { it.landmark?.id == c.id }.takeIf { it >= 0 }?.let { route.cumulativeKm[it] } } ?: 0.0
    val legFraction = next?.let { n -> val end = walkedKm + n.second; if (end > legStart) ((walkedKm - legStart) / (end - legStart)).coerceIn(0.0, 1.0) else 0.0 } ?: 1.0
    val toNext = next?.let { steps(it.second) } ?: 0L
    val toEnd = steps(route.totalKm - walkedKm)
    val overall = (walkedKm / route.totalKm).coerceIn(0.0, 1.0)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("下一景點", fontSize = 11.sp, color = RED, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (next != null) Text("${"%.1f".format(next.second)} 公里・${days(toNext)} 天", fontSize = 11.sp, color = RED, fontWeight = FontWeight.Bold)
            }
            Text(if (next != null) "${toNext.withCommas()} 步" else "已完成", fontWeight = FontWeight.Bold, fontSize = 17.sp, fontFamily = FontFamily.Serif, maxLines = 1)
            Canvas(Modifier.fillMaxWidth().height(10.dp)) {
                val r = size.height / 2
                val x = (size.width * legFraction.toFloat()).coerceIn(r, size.width - r)
                drawRoundRect(Vintage.edge.copy(alpha = 0.28f), Offset.Zero, size, CornerRadius(r, r))
                drawRoundRect(RED.copy(alpha = 0.85f), Offset.Zero, Size((size.width * legFraction.toFloat()).coerceAtLeast(size.height), size.height), CornerRadius(r, r))
                drawCircle(Color.White, r * 0.85f, Offset(x, r)); drawCircle(BLUE, r * 0.55f, Offset(x, r))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("環島全程", fontSize = 11.sp, color = BLUE, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (next != null) Text("剩 ${"%.0f".format(route.totalKm - walkedKm)} 公里・${days(toEnd)} 天", fontSize = 11.sp, color = BLUE, fontWeight = FontWeight.Bold)
            }
            Text("${(overall * 100).roundToInt()}%　${collected.size}/$totalCards 景點", fontWeight = FontWeight.Bold, fontSize = 17.sp, fontFamily = FontFamily.Serif, maxLines = 1)
            Canvas(Modifier.fillMaxWidth().height(10.dp)) {
                val gap = 1.5.dp.toPx()
                val seg = (size.width - gap * (totalCards - 1)) / totalCards
                for (i in 0 until totalCards) {
                    val color = when { i < collected.size -> BLUE; i == collected.size && next != null -> RED.copy(alpha = 0.8f); else -> Vintage.edge.copy(alpha = 0.28f) }
                    drawRoundRect(color, Offset(i * (seg + gap), 0f), Size(seg, size.height), CornerRadius(2.dp.toPx()))
                }
            }
        }
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
