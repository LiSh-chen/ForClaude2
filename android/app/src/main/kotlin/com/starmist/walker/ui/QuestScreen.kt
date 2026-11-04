package com.starmist.walker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.starmist.core.route.Quest
import com.starmist.core.route.QuestEngine
import com.starmist.walker.data.Ledger

/** Every task with how far it is; finished ones first marked with a tick. */
@Composable
fun QuestsDialog(quests: List<Quest>, ledger: Ledger, onDismiss: () -> Unit) {
    val rows = quests.map { it to QuestEngine.progress(it, ledger.cards, ledger.towns) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(trf("任務 {0} / {1}", rows.count { it.second.done }, rows.size)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.groupBy { it.first.group }.forEach { (group, list) ->
                    item(key = "g_$group") { Text(tr(group), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                    items(list, key = { it.first.id }) { (q, p) ->
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Text((if (p.done) "✓ " else "") + tr(q.title), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text("${p.have} / ${p.need}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(tr(q.desc), style = MaterialTheme.typography.bodySmall)
                            LinearProgressIndicator(progress = { if (p.need == 0) 0f else p.have.toFloat() / p.need }, modifier = Modifier.fillMaxWidth().padding(top = 2.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("關閉") } },
    )
}

/** The celebration shown the moment a task is completed. */
@Composable
fun QuestCompletedDialog(title: String, desc: String, reward: String?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎉 任務完成！") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(tr(desc), style = MaterialTheme.typography.bodyMedium)
                Text(if (reward != null) trf("獎勵：{0}", reward) else tr("獎勵即將開放，敬請期待！"), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("太棒了") } },
    )
}
