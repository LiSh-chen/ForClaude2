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
import com.starmist.core.time.now
import kotlinx.coroutines.launch

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
    var showPrivacy by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<android.net.Uri?>(null) }

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

        Section("字體大小") {
            FontSizePicker(settings.fontSize) { vm.setFontSize(it) }
            Text("只改本 App 的字，不跟手機系統的字體設定走。", style = MaterialTheme.typography.labelSmall)
        }

        Section("底部資訊面板") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("儀表", "筆記頁").forEachIndexed { i, label ->
                    FilterChip(selected = settings.panelStyle == i, onClick = { vm.setPanelStyle(i) }, label = { Text(label) })
                }
            }
            Text("地圖下方顯示儀表（兩個錶盤），或一整頁手寫筆記加進度長條圖，二選一。", style = MaterialTheme.typography.labelSmall)
        }

        Section("提醒") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("走到每日目標時通知", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Switch(checked = settings.goalNotify, onCheckedChange = { on -> vm.updateSettings { it.copy(goalNotify = on) } })
            }
            Text("每天只通知一次。系統通知被關閉時，只會在 App 裡提示。", style = MaterialTheme.typography.labelSmall)
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

        Section("資料備份") {
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            val export = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")) { uri ->
                if (uri != null) scope.launch {
                    val ok = runCatching {
                        val text = vm.buildBackup()
                        context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) } ?: error("no stream")
                    }.isSuccess
                    vm.say(if (ok) "已匯出備份檔" else "匯出失敗")
                }
            }
            val import = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) pendingImport = uri
            }
            Text("把步數、設定、路線進度和鄉鎮收集存成一個檔案，換手機或重新安裝後可以匯入。檔案存在哪裡由你決定，App 不會傳到任何地方。", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { export.launch("來台灣走走備份-${kotlinx.datetime.LocalDate.now()}.json") }) { Text("匯出備份") }
                OutlinedButton(onClick = { import.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Text("匯入備份") }
            }
            pendingImport?.let { uri ->
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { pendingImport = null },
                    title = { Text("匯入備份？") },
                    text = { Text("這會取代這支手機上現有的步數、設定、路線進度和鄉鎮收集。確定要匯入嗎？") },
                    confirmButton = {
                        TextButton(onClick = {
                            pendingImport = null
                            scope.launch {
                                val text = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } }.getOrNull()
                                if (text == null) vm.say("匯入失敗：讀不到檔案") else vm.restoreBackup(text)
                            }
                        }) { Text("匯入") }
                    },
                    dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("取消") } },
                )
            }
        }

        Section("關於") {
            OutlinedButton(onClick = { showPrivacy = true }, modifier = Modifier.fillMaxWidth()) { Text("隱私權政策") }
        }

        Section("診斷") {
            Text("檢查感測器、權限與背景讀取狀態；懷疑漏記時先看這裡。", style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onOpenDiagnostics, modifier = Modifier.fillMaxWidth()) { Text("開啟診斷") }
        }
    }

    if (showCalibration) CalibrationDialog(vm, settings.multiplier) { showCalibration = false }
    if (showAdjust) AdjustDialog(vm) { showAdjust = false }
    if (showPrivacy) PrivacyDialog { showPrivacy = false }
    if (showStride) StrideDialog(settings, onApply = { m -> vm.updateSettings { it.copy(strideOverrideM = m) } }) {
        showStride = false
    }
}

/** Small, medium or large writing for the whole app. */
@Composable
internal fun FontSizePicker(step: Int, onPick: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("小", "中", "大").forEachIndexed { i, label ->
            FilterChip(selected = step == i, onClick = { onPick(i) }, label = { Text(label) })
        }
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
