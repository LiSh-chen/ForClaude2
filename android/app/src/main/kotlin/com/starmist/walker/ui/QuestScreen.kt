package com.starmist.walker.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.starmist.core.route.Quest
import com.starmist.core.route.QuestEngine
import com.starmist.walker.data.Ledger

@Composable
private fun Sheet(content: @Composable () -> Unit) {
    Box(Modifier.padding(14.dp)) {
        PaperCard(Modifier.fillMaxWidth(), Lift.Lifted, 14.dp, tint = Vintage.parchment) { Box(Modifier.padding(16.dp)) { content() } }
    }
}

/** Every task with how far it is; finished ones are marked with a tick. */
@Composable
internal fun QuestsSheet(quests: List<Quest>, ledger: Ledger, startOnAlbum: Boolean = false, onClose: () -> Unit) {
    val rows = quests.map { it to QuestEngine.progress(it, ledger.cards, ledger.towns) }
    var album by remember { mutableStateOf(startOnAlbum) }
    Sheet {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (album) tr("郵票冊") else trf("任務 {0} / {1}", rows.count { it.second.done }, rows.size), fontFamily = QUILL, fontSize = 26.sp, fontWeight = FontWeight.Black, color = INK_RED)
            PillTabs(listOf(false to tr("任務"), true to tr("郵票冊")), album, { album = it })
            if (album) StampAlbum(rows.map { it.first to it.second.done }, titlesOf(ledger.done)) else LazyColumn(Modifier.heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.groupBy { it.first.group }.forEach { (group, list) ->
                    item(key = "g_$group") { RibbonHeader(tr(group)) }
                    items(list, key = { it.first.id }) { (q, p) ->
                        PaperCard(Modifier.fillMaxWidth(), Lift.Raised, 10.dp) {
                        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Text((if (p.done) "✓ " else "") + tr(q.title), fontFamily = QUILL, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (p.done) Vintage.moss else Vintage.ink, modifier = Modifier.weight(1f))
                                Text("${p.have} / ${p.need}", fontFamily = QUILL, fontSize = 13.sp, color = Vintage.inkSoft)
                            }
                            Text(tr(q.desc), fontFamily = QUILL, fontSize = 13.sp, color = SEPIA)
                            LinearProgressIndicator(
                                progress = { if (p.need == 0) 0f else p.have.toFloat() / p.need }, modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                color = if (p.done) Vintage.moss else Vintage.brass, trackColor = Vintage.parchmentDark,
                            )
                            Text(trf("獎勵：郵票 ×{0}・紀念郵票", q.postcards), fontFamily = QUILL, fontSize = 12.sp, color = Vintage.inkSoft)
                        }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { BrassPill(tr("關閉"), onClose) }
        }
    }
}

/** Titles earned by the milestones: the highest of the cards and the highest of the townships reached. */
internal fun titlesOf(done: Set<String>): List<String> {
    val byCards = listOf("ms_cards_all" to "環島收藏家", "ms_cards_30" to "環島老手", "ms_cards_20" to "資深行腳", "ms_cards_10" to "背包旅人", "ms_cards_5" to "初出茅廬")
    val byTowns = listOf("ms_towns_60" to "足跡遍地", "ms_towns_30" to "鄉鎮探險家", "ms_towns_10" to "小鎮散步客")
    return listOfNotNull(byCards.firstOrNull { it.first in done }?.second, byTowns.firstOrNull { it.first in done }?.second)
}

private fun groupColor(group: String): Color = when (group) {
    "里程碑" -> Color(0xFFB8892D); "縣市・景點" -> Color(0xFF3F6B34); "縣市・足跡" -> Color(0xFF2C3F85); "區域" -> Color(0xFF9A3B2B); else -> Color(0xFF6A3F86)
}

private fun groupMark(group: String) = when (group) { "里程碑" -> "★"; "縣市・景點" -> "景"; "縣市・足跡" -> "足"; "區域" -> "區"; else -> "題" }

/** Every task is a stamp: the ones done are printed in colour, the others are an empty frame waiting for it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StampAlbum(stamps: List<Pair<Quest, Boolean>>, titles: List<String>) {
    val have = stamps.count { it.second }
    val titleNames = titles.map { tr(it) }
    LazyColumn(Modifier.heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(trf("已收集 {0} / {1} 枚", have, stamps.size), fontFamily = QUILL, fontSize = 15.sp, color = SEPIA)
                Text(if (titles.isEmpty()) tr("稱號：還沒有（完成里程碑任務可以得到）") else trf("稱號：{0}", titleNames.joinToString("・")), fontFamily = QUILL, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = INK_RED)
            }
        }
        stamps.groupBy { it.first.group }.forEach { (group, list) ->
            item(key = "sg_$group") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RibbonHeader(tr(group))
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 3) {
                        list.forEach { (q, done) -> Box(Modifier.weight(1f)) { StampTile(tr(q.title), group, done) } }
                        repeat((3 - list.size % 3) % 3) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StampTile(title: String, group: String, done: Boolean) {
    val color = if (done) groupColor(group) else Vintage.edge
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.82f), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(0.82f)) {
                val r = 2.6.dp.toPx()
                val gap = 8.dp.toPx()
                drawRect(if (done) Color(0xFFFFF8E2) else Color.Transparent)
                if (done) drawRect(color.copy(alpha = 0.09f), Offset(7.dp.toPx(), 7.dp.toPx()), androidx.compose.ui.geometry.Size(size.width - 14.dp.toPx(), size.height - 14.dp.toPx()))
                var x = gap / 2
                while (x < size.width) { drawCircle(Vintage.parchment, r, Offset(x, 0f)); drawCircle(Vintage.parchment, r, Offset(x, size.height)); x += gap }
                var y = gap / 2
                while (y < size.height) { drawCircle(Vintage.parchment, r, Offset(0f, y)); drawCircle(Vintage.parchment, r, Offset(size.width, y)); y += gap }
                drawRect(color.copy(alpha = if (done) 0.8f else 0.5f), Offset(7.dp.toPx(), 7.dp.toPx()), androidx.compose.ui.geometry.Size(size.width - 14.dp.toPx(), size.height - 14.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.2.dp.toPx(), pathEffect = if (done) null else androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))))
            }
            Text(if (done) groupMark(group) else "？", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 26.sp, color = if (done) color else Vintage.edge.copy(alpha = 0.7f))
        }
        Text(if (done) title else "？？？", fontFamily = QUILL, fontSize = 11.sp, textAlign = TextAlign.Center, color = if (done) Vintage.ink else Vintage.inkSoft, maxLines = 2)
    }
}

@Composable
fun QuestsDialog(quests: List<Quest>, ledger: Ledger, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) { QuestsSheet(quests, ledger, onClose = onDismiss) }
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
                BrassLabel("✉ " + trf("獎勵：{0}", reward))
            } else {
                Text(tr("獎勵即將開放，敬請期待！"), fontFamily = QUILL, fontSize = 13.sp, color = Vintage.inkSoft)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                BrassPill(tr("太棒了"), onDismiss)
                if (onPostcards != null) BrassPill(tr("去寄明信片"), onPostcards)
            }
        }
    }
}

@Composable
fun QuestCompletedDialog(title: String, desc: String, reward: String?, onPostcards: (() -> Unit)? = null, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) { QuestCelebrationSheet(title, desc, reward, onPostcards, onDismiss) }
}
