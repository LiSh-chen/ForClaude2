package com.starmist.walker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.starmist.core.stats.Bucket
import com.starmist.core.stats.Period
import com.starmist.core.stats.PeriodSummary
import com.starmist.walker.data.UserSettings
import java.time.format.DateTimeFormatter

private val periods = listOf(Period.WEEK to "週", Period.MONTH to "月", Period.YEAR to "年")
private val weekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

@Composable
fun StatsScreen(vm: AppViewModel, settings: UserSettings, padding: PaddingValues) {
    val period by vm.period.collectAsStateWithLifecycle()
    val summary by vm.summary.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()

    Column(
        Modifier
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        TabRow(selectedTabIndex = periods.indexOfFirst { it.first == period }) {
            periods.forEach { (p, label) ->
                Tab(selected = p == period, onClick = { vm.selectPeriod(p) }, text = { Text(label) })
            }
        }

        val s = summary
        if (s == null) {
            Text("載入中…", Modifier.padding(16.dp))
            return@Column
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = { vm.shiftPeriod(-1) }) { Text("‹ 上一期") }
            Text(rangeLabel(period, s), fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { vm.shiftPeriod(1) }, enabled = s.rangeEnd.isBefore(today)) { Text("下一期 ›") }
        }

        BarChart(
            buckets = s.buckets,
            period = period,
            goal = settings.dailyGoal,
        )

        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryCard("總計", "${s.totalSteps.withCommas()} 步", Modifier.weight(1f))
            SummaryCard("日均", "${s.averagePerDay.withCommas()} 步", Modifier.weight(1f))
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryCard(
                "最佳日",
                s.bestDay?.let { "${it.first.monthValue}/${it.first.dayOfMonth}　${it.second.withCommas()} 步" } ?: "—",
                Modifier.weight(1f),
            )
            SummaryCard("達標天數", "${s.goalDays} 天", Modifier.weight(1f))
        }
    }
}

private fun rangeLabel(period: Period, s: PeriodSummary): String = when (period) {
    Period.WEEK -> {
        val f = DateTimeFormatter.ofPattern("M/d")
        "${f.format(s.rangeStart)} – ${f.format(s.rangeEnd)}"
    }
    Period.MONTH -> "${s.rangeStart.year} 年 ${s.rangeStart.monthValue} 月"
    Period.YEAR -> "${s.rangeStart.year} 年"
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BarChart(buckets: List<Bucket>, period: Period, goal: Int) {
    var selected by remember(buckets) { mutableIntStateOf(-1) }
    val barColor = MaterialTheme.colorScheme.primary
    val reachedColor = MaterialTheme.colorScheme.tertiary
    val selectedColor = MaterialTheme.colorScheme.secondary
    val goalColor = MaterialTheme.colorScheme.outline
    val showGoal = goal > 0 && period != Period.YEAR
    val maxValue = maxOf(
        buckets.maxOfOrNull { it.steps } ?: 0L,
        if (showGoal) goal.toLong() else 0L,
        1L,
    ).toFloat() * 1.1f

    Column {
        Text(
            text = buckets.getOrNull(selected)?.let { "${it.start.monthValue}/${it.start.dayOfMonth}　${it.steps.withCommas()} 步" }
                ?: "點選長條可看數值",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .pointerInput(buckets.size) {
                    detectTapGestures { offset ->
                        val index = (offset.x / size.width * buckets.size).toInt().coerceIn(0, buckets.size - 1)
                        selected = if (selected == index) -1 else index
                    }
                },
        ) {
            val slot = size.width / buckets.size
            val barWidth = slot * 0.6f
            buckets.forEachIndexed { i, b ->
                val h = size.height * (b.steps / maxValue)
                val color = when {
                    i == selected -> selectedColor
                    showGoal && b.steps >= goal -> reachedColor
                    else -> barColor
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(i * slot + (slot - barWidth) / 2, size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
            if (showGoal) {
                val y = size.height * (1 - goal / maxValue)
                drawLine(
                    color = goalColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            buckets.forEachIndexed { i, _ ->
                Text(
                    text = axisLabel(period, i),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    color = Color.Unspecified,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun axisLabel(period: Period, index: Int): String = when (period) {
    Period.WEEK -> weekdayLabels[index]
    Period.MONTH -> (index + 1).let { if (it == 1 || it % 5 == 0) it.toString() else "" }
    Period.YEAR -> (index + 1).toString()
}
