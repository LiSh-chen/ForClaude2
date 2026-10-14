package com.starmist.walker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.starmist.core.route.Landmark
import com.starmist.core.route.RouteDef
import com.starmist.core.route.RouteState
import com.starmist.core.route.Routes
import com.starmist.walker.data.UserSettings
import kotlin.math.roundToInt

/** Walk a real-world route: pick one, watch the distance grow, and collect a card and a station stamp along the way. */
@Composable
fun RouteScreen(vm: AppViewModel, settings: UserSettings, padding: PaddingValues) {
    val state by vm.routeState.collectAsStateWithLifecycle()
    val current = state
    val route = current?.let { Routes.byId(it.routeId) }

    Column(
        Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("收藏・真實世界", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (current == null || route == null) {
            RoutePicker(onStart = { vm.startRoute(it) })
        } else {
            RouteProgressContent(route, current, onRestart = { vm.restartRoute() }, onSwitch = { vm.startRoute(it) })
        }
    }
}

/** Choose a route and which way round to walk it. */
@Composable
internal fun RoutePicker(onStart: (String) -> Unit) {
    Text("選一條真實世界的路線，從現在開始用你的腳步走完它。路上會收集景點卡，經過車站還會蓋紀念章。出發後就不能改方向，除非重新出發。", style = MaterialTheme.typography.bodyMedium)
    Routes.families.forEach { family ->
        val first = family.first()
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Vintage.parchmentLight)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${first.region}・環島一圈", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(first.description, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "全長約 ${first.totalKm.roundToInt()} 公里・${first.cards.size} 張景點卡・${first.supplies.size} 個紀念章",
                    style = MaterialTheme.typography.labelMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    family.forEach { route ->
                        Button(onClick = { onStart(route.id) }, modifier = Modifier.weight(1f)) { Text("${route.direction}出發") }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ColumnScope.RouteProgressContent(route: RouteDef, state: RouteState, onRestart: () -> Unit, onSwitch: (String) -> Unit) {
    val km = state.walkedKm
    val done = km >= route.totalKm - 1e-9
    val next = route.next(km)
    val here = route.lastPassed(km)
    var confirmRestart by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Landmark?>(null) }

    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Vintage.parchmentLight)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${route.region}・${route.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(progress = { (km / route.totalKm).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(8.dp))
            Text("已走約 ${"%.1f".format(km)} / ${route.totalKm.roundToInt()} 公里", fontWeight = FontWeight.SemiBold)
            if (done) Text("環島完成！", color = Vintage.brassDark, fontWeight = FontWeight.Bold)
            else {
                Text("目前在 ${here.name} 附近", style = MaterialTheme.typography.bodyMedium)
                next?.let { (card, away) -> Text("下一張卡：${card.name}，還有約 ${"%.1f".format(away)} 公里", style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }

    if (km < 0.05) {
        val others = Routes.all.filter { it.family == route.family && it.id != route.id }
        others.forEach { other ->
            OutlinedButton(onClick = { onSwitch(other.id) }, modifier = Modifier.fillMaxWidth()) { Text("改成${other.direction}走（還沒出發才能改）") }
        }
    }

    val collected = route.collected(km).map { it.id }.toSet()
    Text("景點卡 ${collected.size} / ${route.cards.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    route.cards.forEach { (card, at) ->
        val owned = card.id in collected
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = if (owned) Vintage.parchmentLight else Vintage.parchmentDark.copy(alpha = 0.6f)),
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CardEmblem(card, owned, Modifier.size(52.dp))
                Column(Modifier.weight(1f)) {
                    if (owned) {
                        Text(card.name, fontWeight = FontWeight.Bold)
                        Text(card.blurb, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("？？？", fontWeight = FontWeight.Bold)
                        Text("第 ${at.roundToInt()} 公里解鎖", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (owned) TextButton(onClick = { detail = card }) { Text("看") }
            }
        }
    }

    val stamps = route.suppliesReached(km).map { it.id }.toSet()
    Spacer(Modifier.height(4.dp))
    Text("車站紀念章 ${stamps.size} / ${route.supplies.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    StampGrid(route, stamps)

    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = { confirmRestart = true }, modifier = Modifier.fillMaxWidth()) { Text("重新選路線／重新出發") }

    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            title = { Text("重新出發？") },
            text = { Text("目前這條路線的進度、景點卡和紀念章會清除，不能復原。") },
            confirmButton = { TextButton(onClick = { confirmRestart = false; onRestart() }) { Text("清除並重新選") } },
            dismissButton = { TextButton(onClick = { confirmRestart = false }) { Text("取消") } },
        )
    }
    detail?.let { card ->
        AlertDialog(
            onDismissRequest = { detail = null },
            title = { Text(card.name) },
            text = { CardFace(card, route.cards.first { it.first.id == card.id }.second) },
            confirmButton = { TextButton(onClick = { detail = null }) { Text("關閉") } },
        )
    }
}

@Composable
private fun StampGrid(route: RouteDef, owned: Set<String>) {
    val rows = route.supplies.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (supply, at) ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (supply.id in owned) StationStamp(supply.name, supply.opened, Modifier.size(104.dp))
                        else Text("${supply.name}\n第 ${at.roundToInt()} 公里", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 36.dp))
                    }
                }
                repeat(3 - row.size) { Box(Modifier.weight(1f)) {} }
            }
        }
    }
}
