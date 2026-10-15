package com.starmist.walker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.starmist.core.metrics.BodyMetrics
import com.starmist.core.metrics.Sex
import com.starmist.core.steps.StepScaler
import com.starmist.walker.data.UserSettings
import java.util.Locale

@Composable
fun SettingsScreen(
    vm: AppViewModel,
    settings: UserSettings,
    padding: PaddingValues,
    onOpenDiagnostics: () -> Unit,
) {
    val context = LocalContext.current
    var showCalibration by remember { mutableStateOf(false) }
    var showAdjust by remember { mutableStateOf(false) }
    var showStride by remember { mutableStateOf(false) }

    Column(
        Modifier
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Section("個人資料與目標") {
            NumberField("身高（公分）", settings.heightCm.toInt().toString(), KeyboardType.Number) { text ->
                text.toDoubleOrNull()?.takeIf { it in 100.0..230.0 }?.let { h -> vm.updateSettings { it.copy(heightCm = h) } }
            }
            NumberField("體重（公斤）", settings.weightKg.toInt().toString(), KeyboardType.Number) { text ->
                text.toDoubleOrNull()?.takeIf { it in 25.0..250.0 }?.let { w -> vm.updateSettings { it.copy(weightKg = w) } }
            }
            NumberField("每日目標（步）", settings.dailyGoal.toString(), KeyboardType.Number) { text ->
                text.toIntOrNull()?.takeIf { it in 500..100_000 }?.let { g -> vm.updateSettings { it.copy(dailyGoal = g) } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Sex.MALE to "男", Sex.FEMALE to "女", Sex.OTHER to "不指定").forEach { (value, label) ->
                    FilterChip(
                        selected = settings.sex == value,
                        onClick = { vm.updateSettings { it.copy(sex = value) } },
                        label = { Text(label) },
                    )
                }
            }
            Text("性別只用來估算步長。", style = MaterialTheme.typography.labelSmall)
        }

        Section("穩定計步模式") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("螢幕關閉時也持續計步", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Switch(checked = settings.stableMode, onCheckedChange = { vm.setStableMode(it) })
            }
            Text(
                "許多手機的計步器只在有程式監聽時才累計。開啟後，本 App 會用一個低調的常駐通知保持監聽；" +
                    "感測器會把步數累積一小段時間再一起交給 App，CPU 大部分時間不用醒來，不持有喚醒鎖，耗電很低。" +
                    "關閉後，螢幕關閉期間的步數在多數手機上將無法記錄。",
                style = MaterialTheme.typography.labelSmall,
            )
            Text(OemHints.forThisDevice(), style = MaterialTheme.typography.labelSmall)
            OutlinedButton(
                onClick = { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("開啟電池最佳化設定") }
        }

        Section("計步微調") {
            var local by remember(settings.multiplier) { mutableFloatStateOf(settings.multiplier.toFloat()) }
            Text("修正係數　×${String.format(Locale.getDefault(), "%.2f", local)}", fontWeight = FontWeight.SemiBold)
            Slider(
                value = local,
                onValueChange = { local = it },
                onValueChangeFinished = { vm.setMultiplier(local.toDouble()) },
                valueRange = StepScaler.MIN_MULTIPLIER.toFloat()..StepScaler.MAX_MULTIPLIER.toFloat(),
            )
            Text(
                "覺得步數偏少就調高，偏多就調低。只影響之後新增的步數，不會改動過去的紀錄。",
                style = MaterialTheme.typography.labelSmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showCalibration = true }) { Text("校準精靈") }
                TextButton(onClick = { vm.setMultiplier(1.0) }) { Text("重設為 1.00") }
            }
            OutlinedButton(onClick = { showAdjust = true }, modifier = Modifier.fillMaxWidth()) {
                Text("手動補登／修正某日步數")
            }
            val stride = BodyMetrics.strideMeters(settings.profile)
            OutlinedButton(onClick = { showStride = true }, modifier = Modifier.fillMaxWidth()) {
                Text("步長校準（目前 ${String.format(Locale.getDefault(), "%.2f", stride)} 公尺）")
            }
        }

        Section("診斷") {
            Text("檢查感測器、權限與背景讀取狀態；懷疑漏記時先看這裡。", style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onOpenDiagnostics, modifier = Modifier.fillMaxWidth()) { Text("開啟診斷") }
        }
    }

    if (showCalibration) CalibrationDialog(vm, settings.multiplier) { showCalibration = false }
    if (showAdjust) AdjustDialog(vm) { showAdjust = false }
    if (showStride) StrideDialog(settings, onApply = { m -> vm.updateSettings { it.copy(strideOverrideM = m) } }) {
        showStride = false
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

/** Text field that edits its own copy and reports every change; the caller ignores invalid input. */
@Composable
fun NumberField(label: String, initial: String, keyboard: KeyboardType, onChange: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onChange(it)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
    )
}
