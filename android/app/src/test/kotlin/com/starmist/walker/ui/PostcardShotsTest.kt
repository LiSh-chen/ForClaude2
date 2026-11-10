package com.starmist.walker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import com.starmist.core.route.Travelers
import com.starmist.walker.data.Ledger
import org.junit.Test

/** The quest, traveler and postcard screens with made-up data. */
class PostcardShotsTest : ShotsBase(DeviceConfig.PIXEL_5) {
    private fun TownAreas.pick(vararg names: String): List<TownArea> =
        names.mapNotNull { n -> towns.firstOrNull { it.name == n } }.ifEmpty { towns.take(names.size) }

    @Composable
    private fun Frame(content: @Composable () -> Unit) = Page(tab = 3, bar = false) { pad ->
        Box(Modifier.fillMaxSize().padding(pad).padding(16.dp)) { content() }
    }

    private val rows get() = Travelers.all.mapIndexed { i, t -> TravelerRow(t, if (i < 2) 2 else if (i == 2) 1 else 0) }

    @Test fun travelers_page() = paparazzi.snapshot {
        val areas = rememberTownAreas()
        val a = areas.pick("阿里山鄉", "桃源區")
        Frame { PostcardsSheet(1, rows, listOf(MailRow(a[0], Travelers.byId("ahbo"), null), MailRow(a.last(), Travelers.byId("xiaohe"), null)), {}, {}, {}) }
    }

    @Test fun travelers_all() = paparazzi.snapshot {
        Frame {
            androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) { Travelers.all.forEach { TravelerAvatar(it.id, 64.dp) } }
                androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) { Travelers.all.forEach { TravelerAvatar(it.id, 52.dp, dim = true) } }
                androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) { Travelers.all.forEach { TravelerAvatar(it.id, 30.dp) } }
            }
        }
    }

    @Test fun traveler_meet() = paparazzi.snapshot { Frame { TravelerMeetSheet(Travelers.byId("xipo")!!, 1) {} } }

    @Test fun traveler_friend() = paparazzi.snapshot { Frame { TravelerMeetSheet(Travelers.byId("ahbo")!!, 2) {} } }

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

    private fun factShot(townName: String, travelerId: String) = paparazzi.snapshot {
        val areas = rememberTownAreas()
        val facts = com.starmist.core.route.ContentPack.parsePostcards(java.io.File("src/main/res/raw/postcard_facts.txt").readLines())
        val t = areas.towns.first { it.name == townName }
        Frame { PostcardOpenedSheet(t, Travelers.byId(travelerId)!!, facts["${t.county}|${t.name}"]?.get(travelerId)) {} }
    }

    @Test fun voice_ahbo() = factShot("仁愛鄉", "ahbo")
    @Test fun voice_xiaohe() = factShot("口湖鄉", "xiaohe")
    @Test fun voice_xipo() = factShot("萬里區", "xipo")
    @Test fun voice_atie() = factShot("平溪區", "atie")
    @Test fun voice_momo() = factShot("三峽區", "momo")
}
