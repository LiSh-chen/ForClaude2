package com.starmist.walker.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.starmist.core.metrics.BodyMetrics
import com.starmist.core.route.Routes
import com.starmist.walker.data.UserSettings
import kotlin.math.roundToLong
import kotlinx.coroutines.launch

/** Remembers on which day the draw was made, so a day's card stays open when the app is reopened. */
private fun drawPrefs(context: Context) = context.getSharedPreferences("stat_draw", Context.MODE_PRIVATE)

/** What the statistics are made from, taken from the view model's flows. */
@Composable
private fun rememberStatInputs(vm: AppViewModel, settings: UserSettings): StatInputs {
    val today by vm.today.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val routeState by vm.routeState.collectAsStateWithLifecycle()
    val logs by vm.regionLogs.collectAsStateWithLifecycle()
    val areas = rememberTownAreas()
    val steps = history[today] ?: 0L
    return remember(today, history.size, steps, routeState, logs, areas, settings.dailyGoal) {
        val route = routeState?.let { Routes.byId(it.routeId) }
        val walked = routeState?.walkedKm ?: 0.0
        val visited = if (route != null) areas.visited(route, walked) else emptyMap()
        val here = route?.positionAt(walked)?.let { areas.find(it.lat, it.lng) } ?: -1
        val towns = visited.entries.filter { it.key in areas.towns.indices }.map { (id, km) ->
            val t = areas.towns[id]
            TownVisit(t.name, t.county, km, t.areaKm2, t.highElevM, passed = id != here)
        }
        StatInputs(
            today = today, history = history, goal = settings.dailyGoal, strideM = BodyMetrics.strideMeters(settings.profile),
            routeTotalKm = route?.totalKm ?: 0.0, walkedKm = walked,
            nextCard = route?.next(walked)?.let { it.first.name to it.second },
            towns = towns, townsTotal = areas.towns.size,
            tagsFound = logs.values.sumOf { Integer.bitCount(it.mask) },
            cardsOwned = route?.collected(walked)?.size ?: 0, cardsTotal = route?.cards?.size ?: 0,
        )
    }
}

/** The daily draw: shake the stick tube once a day and the day's statistic opens; the other cards can be browsed any time. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatDrawSection(vm: AppViewModel, settings: UserSettings) {
    val context = LocalContext.current
    val inputs = rememberStatInputs(vm, settings)
    val seed = dayStatSeed(inputs.today)
    val available = remember(inputs, seed) { availableStats(inputs, seed) }
    val todayKey = inputs.today.toString()
    // What was drawn today is kept (the day and the kind), so reopening the app shows the same card even if more cards have become available since.
    val stored = remember(todayKey) {
        val p = drawPrefs(context)
        if (p.getString("day", null) == todayKey) runCatching { StatKind.valueOf(p.getString("kind", "") ?: "") }.getOrNull() else null
    }
    var drawnToday by remember(todayKey) { mutableStateOf(stored) }
    var shown by remember(todayKey) { mutableStateOf(stored) }
    val candidate = remember(available, inputs.today) { drawStat(inputs.today, available) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var shaking by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Vintage.parchmentLight)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val open = drawnToday != null && shown != null
            if (!open) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎋", fontSize = 56.sp, modifier = Modifier.rotate(shake.value))
                    Text("今日籤", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("每天抽一支籤，看看今天要翻開哪一張統計。", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = Vintage.inkSoft)
                    Button(
                        enabled = candidate != null && !shaking,
                        onClick = {
                            shaking = true
                            scope.launch {
                                repeat(4) { shake.animateTo(14f, tween(70)); shake.animateTo(-14f, tween(70)) }
                                shake.animateTo(0f, tween(70))
                                drawPrefs(context).edit().putString("day", todayKey).putString("kind", candidate?.name).apply()
                                drawnToday = candidate
                                shown = candidate
                                shaking = false
                            }
                        },
                    ) { Text(if (candidate == null) "再走幾步就能抽" else "抽一支") }
                }
            }
            AnimatedVisibility(open, enter = fadeIn(tween(500)) + expandVertically(tween(500))) {
                shown?.let { kind ->
                    val card = remember(kind, inputs, seed) { buildStat(kind, inputs, seed) }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (kind == drawnToday) "今日籤" else "看看別張", style = MaterialTheme.typography.labelMedium, color = Vintage.brassDark)
                        if (card != null) StatCardView(card)
                        Text("也可以翻翻別的：", style = MaterialTheme.typography.labelMedium, color = Vintage.inkSoft)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            available.forEach { k ->
                                val on = k == kind
                                Text(
                                    "${k.emoji} ${k.title}", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = if (on) Color.White else Vintage.brassDark,
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                        .background(if (on) Vintage.brass else Vintage.brass.copy(alpha = 0.16f))
                                        .clickable { shown = k }
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One statistic: its name, the big line, what it means, a note, and its picture. */
@Composable
private fun StatCardView(card: StatCard) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("${card.kind.emoji} ${card.kind.title}", fontWeight = FontWeight.Bold, color = Vintage.brassDark)
        Text(card.headline, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = Vintage.ink)
        Text(card.line, style = MaterialTheme.typography.bodyMedium)
        card.visual?.let { v ->
            when (v) {
                is StatVisual.Heat -> HeatMapView(v)
                is StatVisual.Bars -> BarsView(v)
            }
        }
        card.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Vintage.inkSoft) }
    }
}

@Composable
private fun BarsView(v: StatVisual.Bars) {
    val maxValue = (v.rows.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1e-9)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        v.rows.forEachIndexed { i, (label, value) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(label, fontSize = 12.sp, maxLines = 1, modifier = Modifier.width(76.dp))
                Box(Modifier.weight(1f).height(10.dp)) {
                    Canvas(Modifier.fillMaxWidth().height(10.dp)) {
                        drawRoundRect(Vintage.edge.copy(alpha = 0.22f), Offset.Zero, Size(size.width, size.height), CornerRadius(5.dp.toPx()))
                        drawRoundRect(if (i == v.highlight) Vintage.brass else Vintage.stain, Offset.Zero, Size((size.width * (value / maxValue)).toFloat().coerceAtLeast(size.height), size.height), CornerRadius(5.dp.toPx()))
                    }
                }
                Text(if (value >= 100) "${value.roundToLong().withCommas()} ${v.unit}" else "${"%.1f".format(value)} ${v.unit}", fontSize = 12.sp, maxLines = 1, textAlign = TextAlign.End, modifier = Modifier.width(88.dp))
            }
        }
    }
}

/** A month as a calendar of squares, Monday first; the deeper the colour the more steps, a ring marks a day that reached the goal's colour, and the outline is today. */
@Composable
private fun HeatMapView(v: StatVisual.Heat) {
    val offset = v.month.atDay(1).dayOfWeek.value - 1
    val rows = (offset + v.steps.size + 6) / 7
    val top = (v.steps.take(v.todayIndex + 1).maxOrNull() ?: 0L).coerceAtLeast(1L).coerceAtLeast(if (v.goal > 0) v.goal.toLong() else 1L)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { Text(it, Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.Center, color = Vintage.inkSoft) }
        }
        Canvas(Modifier.fillMaxWidth().aspectRatio(7f / rows)) {
            val cell = size.width / 7f
            val pad = cell * 0.08f
            v.steps.forEachIndexed { d, n ->
                val x = ((offset + d) % 7) * cell
                val y = ((offset + d) / 7) * cell
                val future = d > v.todayIndex
                val color = when {
                    future -> Vintage.edge.copy(alpha = 0.08f)
                    n <= 0 -> Vintage.edge.copy(alpha = 0.18f)
                    else -> Vintage.brass.copy(alpha = (0.25f + 0.75f * (n.toFloat() / top)).coerceIn(0.25f, 1f))
                }
                drawRoundRect(color, Offset(x + pad, y + pad), Size(cell - 2 * pad, cell - 2 * pad), CornerRadius(cell * 0.18f))
                if (v.goal > 0 && !future && n >= v.goal) drawCircle(Color.White.copy(alpha = 0.9f), cell * 0.1f, Offset(x + cell / 2, y + cell / 2))
                if (d == v.todayIndex) drawRoundRect(Vintage.ink, Offset(x + pad, y + pad), Size(cell - 2 * pad, cell - 2 * pad), CornerRadius(cell * 0.18f), style = Stroke(2.dp.toPx()))
            }
        }
        Text("白點：達成每日目標。", fontSize = 11.sp, color = Vintage.inkSoft)
    }
}
