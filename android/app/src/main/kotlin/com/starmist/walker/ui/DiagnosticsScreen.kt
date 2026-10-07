package com.starmist.walker.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.starmist.core.diagnostics.Issue
import com.starmist.walker.data.DiagnosticsInfo
import kotlinx.coroutines.launch

private fun Issue.describe(): String = when (this) {
    Issue.PERMISSION_MISSING ->
        "尚未授予「動作與健身」權限，無法讀取計步器。"
    Issue.NO_HARDWARE_COUNTER ->
        "這台裝置沒有硬體計步器，目前版本無法計步。"
    Issue.NEVER_READ ->
        "還沒有成功讀取過計步器。按下「立即讀取」試試。"
    Issue.STALE_SNAPSHOT ->
        "超過 6 小時沒有讀取，系統可能限制了背景工作。步數不會遺失（計步器會持續累計），開啟 App 後會自動補上；" +
            "如果經常發生，請檢查電池設定。"
    Issue.BATTERY_RESTRICTED ->
        "系統套用了電池最佳化，可能延後背景讀取。步數仍會在下次讀取時補齊；若發現常常晚才更新，可把本 App 設為「不限制」。"
}

@Composable
fun DiagnosticsScreen(vm: AppViewModel, padding: PaddingValues, onOpenAppSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf<DiagnosticsInfo?>(null) }
    var version by remember { mutableIntStateOf(0) }
    val multiplier = vm.settings.collectAsStateWithLifecycle().value?.multiplier

    LaunchedEffect(version) { info = vm.loadDiagnostics() }

    Column(
        Modifier
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("診斷", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        val d = info
        if (d == null) {
            Text("檢查中…")
            return@Column
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("硬體計步器：${if (d.hasHardwareCounter) "有" else "沒有"}")
                Text("動作與健身權限：${if (d.permissionGranted) "已授予" else "未授予"}")
                Text("最近一次讀取：${d.lastSnapshotAtMillis?.let(::formatTime) ?: "從未"}")
                multiplier?.let { Text("修正係數：×${String.format(java.util.Locale.getDefault(), "%.2f", it)}") }
                Text(
                    "最近偵測到的重新開機：" +
                        if (d.recentReboots.isEmpty()) "無" else d.recentReboots.joinToString("、") { formatTime(it.atMillis) },
                )
            }
        }

        if (d.issues.isEmpty()) {
            Card(Modifier.fillMaxWidth()) { Text("一切正常。", Modifier.padding(16.dp)) }
        } else {
            d.issues.forEach { issue ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) { Text(issue.describe(), Modifier.padding(16.dp)) }
            }
        }

        Button(onClick = {
            scope.launch {
                vm.refresh(announce = true).join()
                version++
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("立即讀取") }

        OutlinedButton(onClick = onOpenAppSettings, modifier = Modifier.fillMaxWidth()) { Text("開啟本 App 的系統設定") }
        OutlinedButton(
            onClick = { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("開啟電池最佳化設定") }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("常見的背景限制", fontWeight = FontWeight.SemiBold)
                Text(
                    "部分廠牌（小米、OPPO、vivo、三星等）會在背景強力清理 App。本 App 不靠常駐運作：" +
                        "計步器由硬體持續累計，即使 App 被清掉，下次開啟或背景讀取時也會補上。" +
                        "若仍想更即時，可在系統設定中允許本 App「自啟動」並將電池設為「不限制」。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
