package com.starmist.walker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.starmist.core.route.Quest
import com.starmist.core.route.QuestEngine
import com.starmist.walker.data.Ledger

@Composable
private fun Sheet(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(Modifier.fillMaxWidth().background(Vintage.parchment, shape).border(1.5.dp, Vintage.edge, shape).padding(16.dp)) { content() }
}

/** Every task with how far it is; finished ones are marked with a tick. */
@Composable
internal fun QuestsSheet(quests: List<Quest>, ledger: Ledger, onClose: () -> Unit) {
    val rows = quests.map { it to QuestEngine.progress(it, ledger.cards, ledger.towns) }
    Sheet {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(trf("任務 {0} / {1}", rows.count { it.second.done }, rows.size), fontFamily = QUILL, fontSize = 26.sp, fontWeight = FontWeight.Black, color = INK_RED)
            LazyColumn(Modifier.heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.groupBy { it.first.group }.forEach { (group, list) ->
                    item(key = "g_$group") { Text(tr(group), fontFamily = QUILL, fontSize = 17.sp, fontWeight = FontWeight.Black, color = Vintage.brassDark) }
                    items(list, key = { it.first.id }) { (q, p) ->
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Text((if (p.done) "✓ " else "") + tr(q.title), fontFamily = QUILL, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (p.done) Vintage.moss else Vintage.ink, modifier = Modifier.weight(1f))
                                Text("${p.have} / ${p.need}", fontFamily = QUILL, fontSize = 13.sp, color = Vintage.inkSoft)
                            }
                            Text(tr(q.desc), fontFamily = QUILL, fontSize = 13.sp, color = SEPIA)
                            LinearProgressIndicator(
                                progress = { if (p.need == 0) 0f else p.have.toFloat() / p.need }, modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                color = if (p.done) Vintage.moss else Vintage.brass, trackColor = Vintage.parchmentDark,
                            )
                            Text(trf("獎勵：明信片 ×{0}", q.postcards), fontFamily = QUILL, fontSize = 12.sp, color = Vintage.inkSoft)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onClose) { Text(tr("關閉"), fontFamily = QUILL, fontSize = 18.sp, fontWeight = FontWeight.Black) } }
        }
    }
}

@Composable
fun QuestsDialog(quests: List<Quest>, ledger: Ledger, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) { QuestsSheet(quests, ledger, onDismiss) }
}

/** The celebration shown the moment a task is completed. */
@Composable
internal fun QuestCelebrationSheet(title: String, desc: String, reward: String?, onPostcards: (() -> Unit)?, onDismiss: () -> Unit) {
    Sheet {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🎉", fontSize = 44.sp)
            Text(tr("任務完成！"), fontFamily = QUILL, fontSize = 28.sp, fontWeight = FontWeight.Black, color = INK_RED)
            Text(tr(title), fontFamily = QUILL, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Vintage.ink)
            Text(tr(desc), fontFamily = QUILL, fontSize = 14.sp, color = SEPIA)
            if (reward != null) {
                Box(Modifier.border(1.5.dp, Vintage.brassDark, RoundedCornerShape(4.dp)).background(Vintage.parchmentLight, RoundedCornerShape(4.dp)).padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Text("✉ " + trf("獎勵：{0}", reward), fontFamily = QUILL, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Vintage.brassDark)
                }
            } else {
                Text(tr("獎勵即將開放，敬請期待！"), fontFamily = QUILL, fontSize = 13.sp, color = Vintage.inkSoft)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text(tr("太棒了"), fontFamily = QUILL, fontSize = 18.sp, fontWeight = FontWeight.Black) }
                if (onPostcards != null) TextButton(onClick = onPostcards) { Text(tr("請朋友寄信"), fontFamily = QUILL, fontSize = 18.sp, fontWeight = FontWeight.Black, color = INK_RED) }
            }
        }
    }
}

@Composable
fun QuestCompletedDialog(title: String, desc: String, reward: String?, onPostcards: (() -> Unit)? = null, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) { QuestCelebrationSheet(title, desc, reward, onPostcards, onDismiss) }
}
