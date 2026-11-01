package com.starmist.walker.ui

import com.starmist.core.time.value
import com.starmist.core.time.minusDays
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.starmist.core.route.Routes
import org.junit.Rule
import org.junit.Test

/** The set-up the screenshot tests share: the real screens with made-up data, on the device the subclass names. */
abstract class ShotsBase(device: DeviceConfig, private val fontStep: Int = 1, private val lang: Lang = Lang.ZH) {
    init { decodeThumbsInline = true }

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = device)

    init {
        com.starmist.core.route.Routes.applyTexts(
            com.starmist.core.route.ContentPack.parseCards(java.io.File("src/main/res/raw/card_texts.txt").readLines()),
        )
    }

    internal val forward = Routes.byId("taiwan_loop")!!

    @Composable
    internal fun Page(tab: Int = 0, content: @Composable (PaddingValues) -> Unit) {
        AppLanguage(lang) {
            AppFontScale(fontStep) {
                StarMistTheme {
                    Scaffold(containerColor = Vintage.parchment, bottomBar = { AppBottomBar(tab) {} }) { content(it) }
                }
            }
        }
    }

    @Composable
    internal fun Home(real: Boolean, map: HomeMap, steps: Long = 4321, regionMode: Boolean = false, layers: MapLayers? = null, notebook: Boolean = false, pick: String? = null) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val areas = remember { TownAreas.load(context) }
        val blurbs = remember { loadTownBlurbs(context) }
        val pickId = remember(pick) { if (pick == null) -1 else areas.towns.indexOfFirst { it.name == pick } }
        // a township picked by name: the walker stands in it, so that it has been walked through and tells its features
        val shown = remember(map, pickId) {
            if (pickId < 0 || map !is HomeMap.Real) map else {
                var k = 0.0
                while (k < map.route.totalKm && map.route.positionAt(k).let { areas.find(it.lat, it.lng) } != pickId) k += 0.5
                HomeMap.Real(map.route, k)
            }
        }
        val m = shown as? HomeMap.Real
        val visited = remember(m) { if (m == null) emptyMap() else areas.visited(m.route, m.walkedKm) }
        val cur = remember(m) { m?.route?.positionAt(m.walkedKm)?.let { areas.find(it.lat, it.lng) } ?: -1 }
        Page { pad ->
            HomeContent(
                padding = pad, steps = steps, goal = 9000, km = steps * 0.0007, kcal = steps * 0.04,
                lastSnapshot = null, granted = true, hasCounter = true, map = shown,
                onRequestPermission = {}, onOpenAppSettings = {}, onSync = {}, onStartRoute = {},
                currentTown = cur, exploredTowns = visited.keys,
                infoOf = { id -> if (id !in areas.towns.indices) null else areas.towns[id].let { t -> RegionInfo(t, blurbs["${t.county}|${t.name}"], 1, visited[id] ?: 0.0, visited.keys.indexOf(id) + 1, id in visited) } },
                initialLayers = layers ?: if (regionMode || pickId >= 0) MapLayers(spots = true, route = false, regions = true) else MapLayers(),
                panelStyle = if (notebook) 1 else 0,
                initialPicked = if (pickId >= 0) pickId else if (regionMode) cur else -1,
            )
        }
    }

}
