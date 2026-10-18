package com.starmist.walker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.starmist.core.metrics.BodyMetrics
import com.starmist.core.route.RouteDef
import com.starmist.core.route.Routes
import com.starmist.walker.data.UserSettings
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** What the home map shows. */
internal sealed interface HomeMap {
    data object Preparing : HomeMap
    data class Virtual(val regionName: String, val regionIndex: Int, val fraction: Double, val finished: Boolean) : HomeMap
    data object ChooseRoute : HomeMap
    data class Real(val route: RouteDef, val walkedKm: Double, val zoom: Float? = null) : HomeMap
}

/**
 * What opens first: the map of the challenge being walked (the fantasy world or a real-world
 * route), zoomed in on where the walker is, with today's steps and goal underneath. Cards,
 * stamps, items and history live on their own pages.
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
    val journey by vm.journey.collectAsStateWithLifecycle()
    val routeState by vm.routeState.collectAsStateWithLifecycle()
    val granted by vm.permissionGranted.collectAsStateWithLifecycle()
    val hasCounter by vm.hasHardwareCounter.collectAsStateWithLifecycle()
    val lastSnapshot by vm.lastSnapshotAt.collectAsStateWithLifecycle()

    val profile = settings.profile
    val real = settings.challenge == 1
    val map: HomeMap = if (!real) {
        val state = journey
        if (state == null) HomeMap.Preparing else {
            val p = vm.engine.progress(state.position)
            HomeMap.Virtual(p.region?.name ?: "星霧大陸", p.regionIndex, p.regionFraction, p.finished)
        }
    } else {
        val route = routeState?.let { Routes.byId(it.routeId) }
        if (route == null) HomeMap.ChooseRoute else HomeMap.Real(route, routeState?.walkedKm ?: 0.0)
    }

    HomeContent(
        padding = padding,
        steps = steps,
        goal = settings.dailyGoal,
        km = BodyMetrics.distanceKm(steps, profile),
        kcal = BodyMetrics.kcal(steps, profile),
        lastSnapshot = lastSnapshot,
        real = real,
        granted = granted,
        hasCounter = hasCounter,
        map = map,
        onChallenge = { value -> vm.switchChallenge(value) },
        onRequestPermission = onRequestPermission,
        onOpenAppSettings = onOpenAppSettings,
        onSync = { vm.refresh(announce = true) },
        onStartRoute = { vm.startRoute(it) },
        overlay = { if (map is HomeMap.Virtual) PickupBanner(vm) },
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
    real: Boolean,
    granted: Boolean,
    hasCounter: Boolean,
    map: HomeMap,
    onChallenge: (Int) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onSync: () -> Unit,
    onStartRoute: (String) -> Unit,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val progress = BodyMetrics.goalProgress(steps, goal)
    val remaining = (goal - steps).coerceAtLeast(0)

    Column(Modifier.padding(padding).padding(horizontal = 10.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!granted) PermissionCard(onRequestPermission, onOpenAppSettings)
        if (!hasCounter) NoticeCard("這台裝置沒有硬體計步器，目前版本無法計步。")

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChallengeButton("虛擬世界", !real, Modifier.weight(1f)) { onChallenge(0) }
            ChallengeButton("真實世界", real, Modifier.weight(1f)) { onChallenge(1) }
        }

        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(14.dp))) {
            when (map) {
                HomeMap.Preparing -> Text("世界準備中……", Modifier.align(Alignment.Center))
                is HomeMap.Virtual -> {
                    Column(Modifier.fillMaxSize()) {
                        ChallengeHeader(
                            title = map.regionName,
                            detail = if (map.finished) "目前開放的路線已走完" else "這個區域已走 ${(map.fraction * 100).roundToInt()}%",
                        )
                        ZoomWorldMap(map.regionIndex, map.fraction, map.finished, Modifier.weight(1f).fillMaxWidth())
                    }
                    overlay()
                }
                HomeMap.ChooseRoute ->
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        RoutePicker(onStart = onStartRoute)
                    }
                is HomeMap.Real -> {
                    val next = map.route.next(map.walkedKm)
                    Column(Modifier.fillMaxSize()) {
                        ChallengeHeader(
                            title = "${map.route.region}・${map.route.name}",
                            detail = "已走 ${"%.1f".format(map.walkedKm)} / ${map.route.totalKm.roundToInt()} 公里" +
                                (next?.let { "　下一張：${it.first.name}（${"%.1f".format(it.second)} 公里）" } ?: "　環島完成"),
                        )
                        RouteMapView(map.route, map.walkedKm, Modifier.weight(1f).fillMaxWidth(), initialZoom = map.zoom)
                    }
                }
            }
        }

        StepsPanel(
            steps = steps,
            goal = goal,
            progress = progress,
            remaining = remaining,
            km = km,
            kcal = kcal,
            lastSnapshot = lastSnapshot,
            onSync = onSync,
        )
    }
}

@Composable
private fun ChallengeButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val m = modifier.height(38.dp)
    val pad = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    if (selected) Button(onClick = onClick, modifier = m, contentPadding = pad) { Text(text, fontSize = 15.sp) }
    else OutlinedButton(onClick = onClick, modifier = m, contentPadding = pad) { Text(text, fontSize = 15.sp) }
}

@Composable
private fun ChallengeHeader(title: String, detail: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, fontSize = 15.sp, maxLines = 1)
        Text(detail, fontSize = 12.sp, maxLines = 1)
    }
}

/** Today's steps against the goal, with the distance and energy they come to. */
@Composable
private fun StepsPanel(
    steps: Long,
    goal: Int,
    progress: Double,
    remaining: Long,
    km: Double,
    kcal: Double,
    lastSnapshot: Long?,
    onSync: () -> Unit,
) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Vintage.parchmentLight), elevation = CardDefaults.cardElevation(3.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(steps.withCommas(), fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = Vintage.ink)
                Text("/ ${goal.toLong().withCommas()} 步", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 6.dp))
                Spacer(Modifier.weight(1f))
                Text("${(progress * 100).roundToInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Vintage.brassDark, modifier = Modifier.padding(bottom = 4.dp))
            }
            LinearProgressIndicator(progress = { progress.toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${km.twoDecimals()} 公里　${kcal.roundToInt()} 大卡　" + if (remaining == 0L) "已達標" else "還差 ${remaining.withCommas()} 步",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = onSync) { Text(lastSnapshot?.let { "同步 ${formatTime(it)}" } ?: "立即同步", fontSize = 12.sp) }
            }
        }
    }
}

/** A small banner when the traveller picks something up in the fantasy world. */
@Composable
private fun BoxScope.PickupBanner(vm: AppViewModel) {
    var current by remember { mutableStateOf<Pickup?>(null) }
    LaunchedEffect(Unit) {
        vm.pickups.collect { pickup ->
            current = pickup
            delay(2800)
            current = null
        }
    }
    current?.let { pickup ->
        Card(
            Modifier.align(Alignment.TopCenter).padding(top = 56.dp, start = 12.dp, end = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Vintage.parchmentLight.copy(alpha = 0.95f)),
            elevation = CardDefaults.cardElevation(4.dp),
        ) {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Canvas(Modifier.size(34.dp)) { drawItemIcon(pickup.item, Offset(size.width / 2, size.height / 2), size.width * 0.38f) }
                Column {
                    Text("獲得 ${pickup.item.displayName}", fontWeight = FontWeight.Bold)
                    pickup.line?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
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
