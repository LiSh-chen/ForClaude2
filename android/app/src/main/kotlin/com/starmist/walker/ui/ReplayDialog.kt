package com.starmist.walker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.starmist.walker.data.UserSettings

/** "Welcome back": what happened on the road while the app was closed. Shown once, can be skipped. */
@Composable
fun ReplayDialog(vm: AppViewModel, settings: UserSettings, summary: ReplaySummary, regionId: String?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("旅途回顧") },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WorldScene(regionId, settings.characterId, walking = true)
                Text("這趟走了 ${summary.stepsWalked.withCommas()} 步。", fontWeight = FontWeight.SemiBold)
                if (summary.itemsFound.isNotEmpty()) {
                    Text(
                        "撿到：" + summary.itemsFound.entries.joinToString("、") { "${it.key.displayName}×${it.value}" },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                summary.events.take(MAX_LINES).forEach {
                    Text("• ${describe(it, vm)}", style = MaterialTheme.typography.bodySmall)
                }
                if (summary.events.size > MAX_LINES) {
                    Text("……還有 ${summary.events.size - MAX_LINES} 件事。", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("繼續旅程") } },
    )
}

private const val MAX_LINES = 8
