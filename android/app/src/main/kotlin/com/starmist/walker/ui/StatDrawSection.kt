package com.starmist.walker.ui

import com.starmist.core.time.value
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

/**
 * The daily draw: shake the stick tube and it gives one card, always one of [DRAW_POOL]; two draws a day. When the measure
 * card is drawn, the charts below count in its familiar unit (a running track, the Taipei 101, a marathon ...) for the day.
 */
@Composable
fun StatDrawSection(vm: AppViewModel, settings: UserSettings, onRuler: (Ruler?) -> Unit) {
    val context = LocalContext.current
    val inputs = rememberStatInputs(vm, settings)
    val seed = dayStatSeed(inputs.today)
    val rulers = remember(inputs.routeTotalKm) { rulersFor(inputs.routeTotalKm) }
    val todayKey = inputs.today.toString()
    val candidate = remember(rulers, inputs.today) { drawRuler(inputs.today, rulers) }
    // What can be drawn now: the cards of the pool that have something to show.
    val pool = remember(inputs, seed, candidate) { DRAW_POOL.filter { buildStat(it, inputs, seed, candidate) != null } }
    // The kinds drawn today are kept, so reopening the app shows the same cards.
    var drawn by remember(todayKey) {
        val p = drawPrefs(context)
        val kept: List<StatKind> =
            if (p.getString("day", null) == todayKey) p.getString("kinds", "").orEmpty().split(",").mapNotNull { n -> DRAW_POOL.firstOrNull { it.name == n } } else emptyList()
        mutableStateOf(kept)
    }
    val ruler = if (StatKind.RULER in drawn) candidate else null
    LaunchedEffect(ruler) { onRuler(ruler) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var shaking by remember { mutableStateOf(false) }
    val canDraw = drawn.size < DRAWS_PER_DAY && pool.isNotEmpty() && !shaking

    PaperCard(Modifier.fillMaxWidth(), Lift.Lifted, 14.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🎋", fontSize = if (drawn.isEmpty()) 56.sp else 36.sp, modifier = Modifier.rotate(shake.value))
                Text("今日籤", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                if (drawn.isEmpty()) Text("每天最多抽兩支籤，每支籤是一張統計卡；抽到量尺，下面的圖表就換成那個單位。", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = Vintage.inkSoft)
                if (drawn.size < DRAWS_PER_DAY) {
                    Button(
                        enabled = canDraw,
                        onClick = {
                            shaking = true
                            scope.launch {
                                repeat(4) { shake.animateTo(14f, tween(70)); shake.animateTo(-14f, tween(70)) }
                                shake.animateTo(0f, tween(70))
                                // never the same card twice in a day, while there is another to give
                                val next = (pool - drawn.toSet()).ifEmpty { pool }.random()
                                val now = drawn + next
                                drawPrefs(context).edit().putString("day", todayKey).putString("kinds", now.joinToString(",") { it.name }).apply()
                                drawn = now
                                shaking = false
                            }
                        },
                    ) { Text(if (drawn.isEmpty()) "抽一支" else "再抽一支") }
                } else Text("今天的兩支籤都抽完了，明天再來。", style = MaterialTheme.typography.bodySmall, color = Vintage.inkSoft)
            }
            AnimatedVisibility(drawn.isNotEmpty(), enter = fadeIn(tween(500)) + expandVertically(tween(500))) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    drawn.reversed().forEachIndexed { i, kind ->
                        val card = remember(kind, inputs, seed, candidate) { buildStat(kind, inputs, seed, candidate) }
                        if (card != null) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("第 ${drawn.size - i} 支籤", style = MaterialTheme.typography.labelMedium, color = Vintage.brassDark)
                            StatCardView(card)
                        }
                    }
                }
            }
        }
    }
}

/** One statistic: its name, the big line, what it means, a note, and its picture. */
@Composable
internal fun StatCardView(card: StatCard) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("${card.kind.emoji} ${card.kind.title}", fontWeight = FontWeight.Bold, color = Vintage.brassDark)
        Text(card.headline, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = Vintage.ink)
        Text(card.line, style = MaterialTheme.typography.bodyMedium)
        card.visual?.let { v ->
            when (v) {
                is StatVisual.Heat -> Column { DeskCalendar(v); Text("紅章：達成每日目標；紅圈是今天。", fontSize = 11.sp, color = Vintage.inkSoft) }
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
