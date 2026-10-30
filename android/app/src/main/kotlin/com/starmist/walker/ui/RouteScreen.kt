package com.starmist.walker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
    var confirmRestart by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Landmark?>(null) }
    // Counties whose open/closed state the player flipped (a county opens by itself once it holds a card).
    var flipped by rememberSaveable { mutableStateOf(listOf<String>()) }
    var album by rememberSaveable { mutableStateOf(false) }
    val features = rememberLandmarkFacts()
    val areasForStamps = rememberTownAreas()
    val places = areasForStamps.cardPlaces
    val sightStamps = remember(route, areasForStamps) { route?.let { PoiStamps.of(it, areasForStamps) } ?: emptyList() }

    // Lazy: only the cards on screen are built, instead of all forty-five (and their pictures) when the tab opens.
    LazyColumn(
        Modifier.padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("收藏・真實世界", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        if (current == null || route == null) {
            item { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { RoutePicker(onStart = { vm.startRoute(it) }) } }
        } else {
            routeProgressItems(
                route, current, places, features, flipped,
                onToggle = { county -> flipped = if (county in flipped) flipped - county else flipped + county },
                onOpen = { detail = it }, onRestart = { confirmRestart = true }, onSwitch = { vm.startRoute(it) },
                album = album, onAlbum = { album = it }, sightStamps = sightStamps,
            )
        }
    }

    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            title = { Text("重新出發？") },
            text = { Text("目前這條路線的進度、景點卡和紀念章會清除，不能復原。") },
            confirmButton = { TextButton(onClick = { confirmRestart = false; vm.restartRoute() }) { Text("清除並重新選") } },
            dismissButton = { TextButton(onClick = { confirmRestart = false }) { Text("取消") } },
        )
    }
    val card = detail
    if (card != null && route != null) CardDialog(route, card) { detail = null }
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

/** The route's progress as list items: summary, cards grouped by county (each group opens and closes), station stamps, restart. */
internal fun LazyListScope.routeProgressItems(
    route: RouteDef, state: RouteState, places: Map<String, TownArea>, features: Map<String, List<TownTag>>, flipped: List<String>,
    onToggle: (String) -> Unit, onOpen: (Landmark) -> Unit, onRestart: () -> Unit, onSwitch: (String) -> Unit,
    /** True shows every card as a square picture in an album grid instead of the list by county. */
    album: Boolean = false, onAlbum: (Boolean) -> Unit = {},
    /** Each small sight with the kilometre its stamp is earned at (null: never on this route). */
    sightStamps: List<Pair<com.starmist.core.route.Poi, Double?>> = route.poiStamps.map { it.first to (it.second as Double?) },
) {
    val km = state.walkedKm
    val done = km >= route.totalKm - 1e-9
    val next = route.next(km)
    val here = route.lastPassed(km)
    val collected = route.collected(km).map { it.id }.toSet()

    item {
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
    }
    if (km < 0.05) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Routes.all.filter { it.family == route.family && it.id != route.id }.forEach { other ->
                    OutlinedButton(onClick = { onSwitch(other.id) }, modifier = Modifier.fillMaxWidth()) { Text("改成${other.direction}走（還沒出發才能改）") }
                }
            }
        }
    }

    item {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("景點卡 ${collected.size} / ${route.cards.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            ViewSwitch(album, onAlbum)
        }
    }
    if (album) {
        // The album: every card as a picture, three to a row, in the order the route meets them.
        items(route.cards.chunked(3), key = { row -> "album_" + row.first().first.id }) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (card, _) ->
                    val owned = card.id in collected
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).then(if (owned) Modifier.clickable { onOpen(card) } else Modifier),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CardEmblem(card, owned, Modifier.fillMaxWidth().aspectRatio(1f))
                        Text(if (owned) card.name else "？", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, color = if (owned) Vintage.ink else Vintage.inkSoft)
                    }
                }
                repeat(3 - row.size) { Box(Modifier.weight(1f)) {} }
            }
        }
    } else {
        // Counties in the order the route reaches them.
        route.cards.groupBy { places[it.first.id]?.county ?: "其他" }.forEach { (county, cards) ->
            val have = cards.count { it.first.id in collected }
            val open = (have > 0) != (county in flipped)
            item(key = "county_$county") { CountyHeader(county, have, cards.size, open) { onToggle(county) } }
            if (open) items(cards, key = { it.first.id }) { (card, at) ->
                val owned = card.id in collected
                CardRow(card, at, owned, places[card.id]?.name, features[card.id].orEmpty()) { onOpen(card) }
            }
        }
    }

    val stamps = route.suppliesReached(km).map { it.id }.toSet()
    item { Text("車站紀念章 ${stamps.size} / ${route.supplies.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
    item { StampGrid(route, stamps) }
    val sights = PoiStamps.earned(sightStamps, km).map { it.id }.toSet()
    item { Text("小景點章 ${sights.size} / ${route.pois.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
    item { PoiStampGrid(sightStamps, sights) }
    item { OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("重新選路線／重新出發") } }
}

/** The two ways to look at the cards: a list by county, or an album of pictures. */
@Composable
private fun ViewSwitch(album: Boolean, onAlbum: (Boolean) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(14.dp)).background(Vintage.edge.copy(alpha = 0.18f))) {
        listOf(false to "清單", true to "相簿").forEach { (isAlbum, label) ->
            val on = album == isAlbum
            Text(
                label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (on) Color.White else Vintage.brassDark,
                modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(if (on) Vintage.brass else Color.Transparent)
                    .clickable { onAlbum(isAlbum) }.padding(horizontal = 14.dp, vertical = 5.dp),
            )
        }
    }
}

/** A county's name with how many of its cards are collected; tap to open or close its list. */
@Composable
private fun CountyHeader(county: String, have: Int, total: Int, open: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(if (open) "▾" else "▸", color = Vintage.brassDark, fontWeight = FontWeight.Bold)
        Text(county, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Text("$have / $total", style = MaterialTheme.typography.labelLarge, color = if (have == total) Vintage.brassDark else Vintage.inkSoft)
    }
}

@Composable
private fun CardRow(card: Landmark, at: Double, owned: Boolean, township: String?, features: List<TownTag>, onOpen: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().then(if (owned) Modifier.clickable(onClick = onOpen) else Modifier),
        colors = CardDefaults.cardColors(containerColor = if (owned) Vintage.parchmentLight else Vintage.parchmentDark.copy(alpha = 0.6f)),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                if (owned) {
                    Text(card.name, fontWeight = FontWeight.Bold)
                    if (township != null) Text(township, style = MaterialTheme.typography.labelMedium, color = Vintage.brassDark)
                    Text(features.joinToString(" · ") { it.tag }, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("？？？", fontWeight = FontWeight.Bold)
                    Text("第 ${at.roundToInt()} 公里解鎖", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (owned) FilledTonalButton(onClick = onOpen, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) { Text("查看") }
        }
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

@Composable
private fun PoiStampGrid(stamps: List<Pair<com.starmist.core.route.Poi, Double?>>, owned: Set<String>) {
    val rows = stamps.sortedBy { it.second ?: Double.MAX_VALUE }.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (poi, at) ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (poi.id in owned) PoiStamp(poi, Modifier.size(104.dp))
                        else Text(if (at != null) "？？？\n第 ${at.roundToInt()} 公里" else "？？？", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 36.dp))
                    }
                }
                repeat(3 - row.size) { Box(Modifier.weight(1f)) {} }
            }
        }
    }
}
