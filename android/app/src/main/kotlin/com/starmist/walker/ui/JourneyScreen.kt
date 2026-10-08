package com.starmist.walker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.starmist.core.world.ItemType
import com.starmist.core.world.JourneyEvent
import com.starmist.walker.data.UserSettings

/** Where the traveller is on the route, what they carry, and the story cards waiting for an answer. */
@Composable
fun JourneyScreen(vm: AppViewModel, settings: UserSettings, padding: PaddingValues) {
    val journey by vm.journey.collectAsStateWithLifecycle()
    val walking by vm.walking.collectAsStateWithLifecycle()
    val engine = vm.engine

    // Entering the page brings the journey up to date.
    LaunchedEffect(Unit) { vm.settleJourney() }

    val state = journey
    val progress = state?.let { engine.progress(it.position) }

    Column(
        Modifier
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WorldScene(progress?.region?.id, settings.characterId, walking)

        if (state == null || progress == null) {
            Text("旅程會在你走路後開始。", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (progress.finished) "目前開放的路線已走完" else "正在前往：${progress.region?.name ?: ""}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                LinearProgressIndicator(
                    progress = { progress.regionFraction.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "${progress.stepsIntoRegion.withCommas()} / ${(progress.region?.lengthSteps ?: 0L).withCommas()} 步",
                    style = MaterialTheme.typography.labelMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    engine.regions.forEachIndexed { index, region ->
                        val done = region.id in state.clearedRegions
                        Text(
                            "${index + 1}. ${region.name}${if (done) " ✓" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                if (progress.finished) {
                    Text("新的區域與篇章會在之後的更新加入，多走的步數會保留。", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("背包", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val shown = ItemType.entries.filter { state.count(it) > 0 }
                if (shown.isEmpty()) {
                    Text("還沒有撿到東西。每走約 2,000 步，路上會有新的發現。", style = MaterialTheme.typography.bodyMedium)
                } else {
                    shown.forEach { Text("${it.displayName} × ${state.count(it)}") }
                    Text(
                        "這些素材之後會在聖地祭壇用來讓土地復甦（尚未開放）。",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        Text("路上的故事", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (state.pending.isEmpty()) {
            Text("目前沒有等待回應的故事。", style = MaterialTheme.typography.bodyMedium)
        }
        state.pending.forEach { card ->
            val def = engine.definition(card.defId) ?: return@forEach
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(def.title, fontWeight = FontWeight.Bold)
                    Text(def.text, style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { vm.resolveCard(card.encounterIndex, true) }, modifier = Modifier.fillMaxWidth()) {
                        Text(def.a.label)
                    }
                    OutlinedButton(onClick = { vm.resolveCard(card.encounterIndex, false) }, modifier = Modifier.fillMaxWidth()) {
                        Text(def.b.label)
                    }
                }
            }
        }
    }
}

/** One line per thing that happened, in plain words. */
fun describe(event: JourneyEvent, vm: AppViewModel): String = when (event) {
    is JourneyEvent.Found -> "${event.line}（獲得 ${event.item.displayName}）"
    is JourneyEvent.ChoiceAppeared -> {
        val title = vm.engine.definition(event.defId)?.title ?: "一個故事"
        "遇到「$title」，也撿到了${event.item.displayName}。到「旅程」頁回應它。"
    }
    is JourneyEvent.RegionCleared -> {
        val region = vm.engine.region(event.regionId)
        "${region?.name ?: ""}：${region?.clearedText ?: "星霧散去了。"}"
    }
    JourneyEvent.RouteEnd -> "目前開放的路線已走到盡頭，新的篇章敬請期待。"
}
