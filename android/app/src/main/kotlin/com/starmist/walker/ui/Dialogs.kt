package com.starmist.walker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.starmist.core.metrics.BodyMetrics
import com.starmist.core.steps.Calibration
import com.starmist.core.steps.CalibrationTrial
import com.starmist.walker.data.DailyStepsEntity
import com.starmist.walker.data.UserSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

/**
 * Walk a stretch, count your own steps, and let the app work out the correction factor.
 * The sensor is read directly (before any correction) so repeated calibrations don't compound.
 */
@Composable
fun CalibrationDialog(vm: AppViewModel, currentMultiplier: Double, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var stage by remember { mutableIntStateOf(0) } // 0 intro, 1 walking, 2 enter count, 3 result
    var busy by remember { mutableStateOf(false) }
    var startCounter by remember { mutableLongStateOf(0) }
    var rawSteps by remember { mutableIntStateOf(0) }
    var actualText by remember { mutableStateOf("") }
    var proposed by remember { mutableStateOf<Double?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("校準精靈") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (stage) {
                    0 -> Text("按「開始」後，用平常的方式走約 50 步並自己默數，走完再按「結束」。手機請放在平常放的位置。")
                    1 -> Text("走路中…請自己數步數，走完後按「結束」。")
                    2 -> {
                        Text("感測器計到 $rawSteps 步。你實際數到幾步？")
                        NumberField("實際步數", actualText, KeyboardType.Number) { actualText = it }
                    }
                    else -> Text(
                        "建議修正係數 ×${String.format(Locale.getDefault(), "%.2f", proposed ?: 1.0)}" +
                            "（目前 ×${String.format(Locale.getDefault(), "%.2f", currentMultiplier)}）",
                    )
                }
                if (busy) Text("讀取中…", style = MaterialTheme.typography.labelMedium)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            when (stage) {
                0 -> TextButton(enabled = !busy, onClick = {
                    scope.launch {
                        busy = true
                        error = null
                        val c = vm.readRawCounter()
                        busy = false
                        if (c == null) error = "讀不到計步器，請確認已授予權限。" else {
                            startCounter = c
                            stage = 1
                        }
                    }
                }) { Text("開始") }
                1 -> TextButton(enabled = !busy, onClick = {
                    scope.launch {
                        busy = true
                        error = null
                        delay(2_500) // the hardware counter reports in small batches
                        val c = vm.readRawCounter()
                        busy = false
                        when {
                            c == null -> error = "讀不到計步器，請再試一次。"
                            c < startCounter -> {
                                error = "測量期間手機重新啟動了，請重來。"
                                stage = 0
                            }
                            else -> {
                                rawSteps = (c - startCounter).toInt()
                                stage = 2
                            }
                        }
                    }
                }) { Text("結束") }
                2 -> TextButton(onClick = {
                    val actual = actualText.toIntOrNull()
                    val m = actual?.let { Calibration.multiplierFrom(listOf(CalibrationTrial(rawSteps, it))) }
                    if (m == null) error = "步數太少或輸入不正確，請走久一點（至少 10 步）再試。" else {
                        proposed = m
                        error = null
                        stage = 3
                    }
                }) { Text("計算") }
                else -> TextButton(onClick = {
                    proposed?.let { vm.setMultiplier(it) }
                    onDismiss()
                }) { Text("套用") }
            }
        },
        dismissButton = {
            Row {
                if (stage == 3) TextButton(onClick = { stage = 0; actualText = "" }) { Text("重新測量") }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}

/** Set any of the last two weeks to the number you know is right. The sensor value stays recoverable. */
@Composable
fun AdjustDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val today = remember { LocalDate.now() }
    val days = remember { (0L..13L).map { today.minusDays(it) } }
    var selected by remember { mutableStateOf(today) }
    var version by remember { mutableIntStateOf(0) }
    var row by remember { mutableStateOf<DailyStepsEntity?>(null) }
    var text by remember { mutableStateOf("") }
    var loadTick by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selected, version) {
        row = vm.dayRow(selected)
        text = (row?.total ?: 0L).toString()
        loadTick++
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("手動補登／修正") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(days) { day ->
                        FilterChip(
                            selected = day == selected,
                            onClick = { selected = day },
                            label = { Text(if (day == today) "今天" else "${day.monthValue}/${day.dayOfMonth}") },
                        )
                    }
                }
                val r = row
                Text(
                    if (r == null) "這天沒有紀錄" else
                        "感測器 ${r.scaledSteps.withCommas()} 步，手動調整 ${if (r.manualAdjust >= 0) "+" else ""}${r.manualAdjust}",
                    style = MaterialTheme.typography.labelMedium,
                )
                // The field keeps its own text, so it is rebuilt whenever another day has been loaded.
                key(loadTick) {
                    NumberField("這天的正確步數", text, KeyboardType.Number) { text = it }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                text.toLongOrNull()?.takeIf { it in 0..200_000 }?.let { total ->
                    scope.launch {
                        vm.setDayTotal(selected, total)
                        version++
                    }
                }
            }) { Text("儲存") }
        },
        dismissButton = {
            Row {
                TextButton(
                    enabled = (row?.manualAdjust ?: 0L) != 0L,
                    onClick = {
                        scope.launch {
                            vm.clearAdjustment(selected)
                            version++
                        }
                    },
                ) { Text("還原") }
                TextButton(onClick = onDismiss) { Text("關閉") }
            }
        },
    )
}

/** Stride from a measured walk, so distance doesn't depend on a height formula. */
@Composable
fun StrideDialog(settings: UserSettings, onApply: (Double?) -> Unit, onDismiss: () -> Unit) {
    var distanceText by remember { mutableStateOf("") }
    var stepsText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("步長校準") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("走一段已知長度的路（例如操場一圈 400 公尺），輸入距離和你數到的步數。")
                NumberField("距離（公尺）", distanceText, KeyboardType.Number) { distanceText = it }
                NumberField("步數", stepsText, KeyboardType.Number) { stepsText = it }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (settings.strideOverrideM != null) {
                    Text(
                        "目前使用自訂步長 ${String.format(Locale.getDefault(), "%.2f", settings.strideOverrideM)} 公尺",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val stride = BodyMetrics.strideFromKnownDistance(
                    distanceText.toDoubleOrNull() ?: 0.0,
                    stepsText.toIntOrNull() ?: 0,
                )
                if (stride == null) error = "數值不合理，請再確認。" else {
                    onApply(stride)
                    onDismiss()
                }
            }) { Text("套用") }
        },
        dismissButton = {
            Row {
                if (settings.strideOverrideM != null) {
                    TextButton(onClick = { onApply(null); onDismiss() }) { Text("改回身高推算") }
                }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}
