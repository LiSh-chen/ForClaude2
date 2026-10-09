package com.starmist.walker.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.starmist.core.metrics.BodyMetrics
import com.starmist.walker.data.UserSettings
import kotlin.math.roundToInt

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
    val walking by vm.walking.collectAsStateWithLifecycle()
    val granted by vm.permissionGranted.collectAsStateWithLifecycle()
    val hasCounter by vm.hasHardwareCounter.collectAsStateWithLifecycle()
    val lastSnapshot by vm.lastSnapshotAt.collectAsStateWithLifecycle()

    val profile = settings.profile
    val km = BodyMetrics.distanceKm(steps, profile)
    val kcal = BodyMetrics.kcal(steps, profile)
    val progress = BodyMetrics.goalProgress(steps, settings.dailyGoal)
    val remaining = (settings.dailyGoal - steps).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!granted) PermissionCard(onRequestPermission, onOpenAppSettings)
        if (!hasCounter) NoticeCard("這台裝置沒有硬體計步器，目前版本無法計步。")

        Text(
            "星霧大陸",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Vintage.ink,
        )
        WorldScene(
            regionId = journey?.let { vm.engine.regionAt(it.position)?.id },
            characterId = settings.characterId,
            walking = walking,
        )
        CompassDial(steps = steps, goal = settings.dailyGoal, progress = progress)
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("距離", "${km.twoDecimals()} 公里", Modifier.weight(1f))
            StatCard("消耗", "${kcal.roundToInt()} 大卡", Modifier.weight(1f))
            StatCard(
                "離目標",
                if (remaining == 0L) "已達標" else "${remaining.withCommas()} 步",
                Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text("距離與熱量為依身高體重估算", style = MaterialTheme.typography.labelSmall)

        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { vm.refresh(announce = true) }) { Text("立即同步") }
        Text(
            text = lastSnapshot?.let { "上次讀取 ${formatTime(it)}" } ?: "尚未讀取",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PermissionCard(onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("需要「動作與健身」權限", fontWeight = FontWeight.Bold)
            Text(
                "只用來讀取手機內建的計步器，不會取得位置、聯絡人或其他資料。",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp),
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
    Card(Modifier.fillMaxWidth()) { Text(text, Modifier.padding(16.dp)) }
}
