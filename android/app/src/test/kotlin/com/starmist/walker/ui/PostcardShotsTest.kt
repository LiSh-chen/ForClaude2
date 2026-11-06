package com.starmist.walker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import com.starmist.walker.data.Ledger
import org.junit.Test

/** The quest and postcard screens with made-up data. */
class PostcardShotsTest : ShotsBase(DeviceConfig.PIXEL_5) {
    private fun TownAreas.pick(vararg names: String): List<TownArea> =
        names.mapNotNull { n -> towns.firstOrNull { it.name == n } }.ifEmpty { towns.take(names.size) }

    @Composable
    private fun Frame(content: @Composable () -> Unit) = Page(tab = 3, bar = false) { pad ->
        Box(Modifier.fillMaxSize().padding(pad).padding(16.dp)) { content() }
    }

    @Test fun postcards_pick() = paparazzi.snapshot {
        val areas = rememberTownAreas()
        Frame { PostcardsSheet(2, areas.pick("阿里山鄉", "仁愛鄉", "尖石鄉"), areas.pick("桃源區", "信義鄉"), {}, {}, {}) }
    }

    @Test fun postcards_empty() = paparazzi.snapshot {
        val areas = rememberTownAreas()
        Frame { PostcardsSheet(0, emptyList(), areas.pick("桃源區", "信義鄉", "阿里山鄉"), {}, {}, {}) }
    }

    @Test fun postcard_opened() = paparazzi.snapshot {
        val areas = rememberTownAreas()
        Frame { PostcardOpenedSheet(areas.pick("阿里山鄉").first()) {} }
    }

    @Test fun quests_list() = paparazzi.snapshot {
        val areas = rememberTownAreas()
        val blurbs = rememberTownBlurbs()
        val quests = QuestCatalog.build(areas, blurbs)
        val ledger = Ledger(
            cards = setOf("taipei101", "keelung", "jiufen", "fulong", "toucheng", "sandiaojiao", "ruisui", "shuishang"),
            towns = blurbs.keys.take(14).toSet(), earned = 3,
        )
        Frame { QuestsSheet(quests, ledger) {} }
    }

    @Test fun quest_celebration() = paparazzi.snapshot {
        Frame { QuestCelebrationSheet("北台灣經典", "集齊：台北101、基隆廟口、九份、福隆", "明信片 ×2", onPostcards = {}, onDismiss = {}) }
    }
}
