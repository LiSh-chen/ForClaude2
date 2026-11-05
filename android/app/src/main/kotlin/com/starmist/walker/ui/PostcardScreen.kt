package com.starmist.walker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.starmist.walker.data.Ledger

/** A line about a township that does not give its name away: where it is and how big and how high. */
@androidx.compose.runtime.Composable
internal fun postcardHint(t: TownArea): String {
    val kind = when {
        t.highElevM >= 2500 -> tr("高山")
        t.highElevM >= 1000 -> tr("山地")
        t.medianElevM <= 30 -> tr("近海平原")
        else -> tr("丘陵台地")
    }
    return trf("{0}・{1}・約 {2} 平方公里", t.county, kind, "%.0f".format(t.areaKm2))
}

/** What a postcard says, from the township's own figures (more words can be added per township later). */
@androidx.compose.runtime.Composable
internal fun postcardBody(t: TownArea): List<String> = listOf(
    trf("面積約 {0} 平方公里，共 {1} 個村里。", "%.1f".format(t.areaKm2), t.villages),
    trf("境內最高處海拔約 {0} 公尺，中位數約 {1} 公尺。", t.highElevM, t.medianElevM),
    tr("這個地方不在你的環島路線上，是靠任務換來的一張明信片。"),
)

/** Pick a township from three mysteries when a postcard is left; below, the postcards already collected. */
@Composable
fun PostcardsDialog(vm: AppViewModel, ledger: Ledger, onDismiss: () -> Unit) {
    var opened by remember { mutableStateOf<TownArea?>(null) }
    val offer = remember(ledger.unlocked, ledger.earned, ledger.towns) { vm.postcardOffer(ledger).mapNotNull { vm.townByKey(it) } }
    val mine = remember(ledger.unlocked) { ledger.unlocked.sorted().mapNotNull { vm.townByKey(it) } }
    val show = opened
    if (show != null) {
        AlertDialog(
            onDismissRequest = { opened = null },
            title = { Text(trf("來自{0}的明信片", show.name)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(show.county, style = MaterialTheme.typography.bodySmall)
                    postcardBody(show).forEach { Text(it) }
                }
            },
            confirmButton = { TextButton(onClick = { opened = null }) { Text("收下") } },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(trf("明信片（可選 {0} 張）", ledger.postcardsLeft)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (offer.isNotEmpty()) {
                    item { Text("完成任務換到的明信片，來自路線以外的地方。三選一，選了就會揭曉是哪裡：", style = MaterialTheme.typography.bodyMedium) }
                    items(offer, key = { "offer_${it.county}|${it.name}" }) { t ->
                        Card(
                            Modifier.fillMaxWidth().clickable { vm.unlockPostcard("${t.county}|${t.name}"); opened = t },
                            colors = CardDefaults.cardColors(containerColor = Vintage.parchmentLight),
                        ) { Text("？ " + postcardHint(t), Modifier.padding(14.dp), fontWeight = FontWeight.SemiBold) }
                    }
                } else {
                    item { Text("目前沒有可用的明信片。完成「任務與里程碑」就能獲得。", style = MaterialTheme.typography.bodyMedium) }
                }
                item { Text(trf("已收集 {0} 張", mine.size), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                items(mine, key = { "mine_${it.county}|${it.name}" }) { t ->
                    Text("${t.county} ${t.name}", Modifier.fillMaxWidth().clickable { opened = t }.padding(vertical = 6.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("關閉") } },
    )
}
