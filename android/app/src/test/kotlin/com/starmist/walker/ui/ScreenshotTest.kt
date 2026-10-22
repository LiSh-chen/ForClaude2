package com.starmist.walker.ui

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

/** Renders the real screens with made-up data, so a UI change can be looked at before any APK. */
class ScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    init {
        com.starmist.core.route.Routes.applyTexts(
            com.starmist.core.route.ContentPack.parseCards(java.io.File("src/main/res/raw/card_texts.txt").readLines()),
        )
    }

    private val forward = Routes.byId("taiwan_loop")!!

    @Composable
    private fun Page(tab: Int = 0, content: @Composable (PaddingValues) -> Unit) {
        StarMistTheme {
            Scaffold(containerColor = Vintage.parchment, bottomBar = { AppBottomBar(tab) {} }) { content(it) }
        }
    }

    @Composable
    private fun Home(real: Boolean, map: HomeMap, steps: Long = 4321, regionMode: Boolean = false) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val areas = remember { TownAreas.load(context) }
        val blurbs = remember { loadTownBlurbs(context) }
        val m = map as? HomeMap.Real
        val visited = remember(m) { if (m == null) emptyMap() else areas.visited(m.route, m.walkedKm) }
        val cur = remember(m) { m?.route?.positionAt(m.walkedKm)?.let { areas.find(it.lat, it.lng) } ?: -1 }
        Page { pad ->
            HomeContent(
                padding = pad, steps = steps, goal = 9000, km = steps * 0.0007, kcal = steps * 0.04,
                lastSnapshot = null, granted = true, hasCounter = true, map = map,
                onRequestPermission = {}, onOpenAppSettings = {}, onSync = {}, onStartRoute = {},
                currentTown = cur, exploredTowns = visited.keys,
                infoOf = { id -> if (id !in areas.towns.indices) null else areas.towns[id].let { t -> RegionInfo(t, blurbs["${t.county}|${t.name}"], 1, visited[id] ?: 0.0, visited.keys.indexOf(id) + 1, id in visited) } },
                initialRegionMode = regionMode,
            )
        }
    }

    @Test fun home_real_start() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 0.0), 120) }
    @Test fun home_real_mid() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 85.0)) }
    @Test fun home_real_east() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 260.0)) }
    @Test fun home_real_coast_ne() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 78.0)) }
    @Test fun home_real_south_tip() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 478.0)) }
    @Test fun home_west_tainan() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 660.0)) }
    @Test fun home_west_xiluo() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 770.0)) }
    @Test fun home_west_changhua() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 830.0)) }
    @Test fun home_west_dajia() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 880.0)) }
    @Test fun home_west_hsinchu() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 960.0)) }
    @Test fun home_west_taoyuan() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 1005.0)) }
    @Test fun home_zoom_max() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 830.0, 12f)) }
    @Test fun home_zoom_min() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 830.0, 1f)) }
    @Test fun home_zoom_max_taipei() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 2.0, 12f)) }
    @Test fun home_region_mode() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 830.0), regionMode = true) }
    @Test fun home_region_mode_zoom_min() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 830.0, 1f), regionMode = true) }
    @Test fun home_choose_route() = paparazzi.snapshot { Home(true, HomeMap.ChooseRoute, 0) }

    @Test fun collection_real() = paparazzi.snapshot {
        Page(tab = 3) { pad ->
            androidx.compose.foundation.layout.Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.Text("收藏・真實世界")
                RouteProgressContent(forward, com.starmist.core.route.RouteState(forward.id, 0L, 260.0), onRestart = {}, onSwitch = {})
            }
        }
    }

    @Test fun card_face() = paparazzi.snapshot {
        StarMistTheme { Box(Modifier.fillMaxSize().background(Vintage.parchment).padding(16.dp)) { CardFace(forward.cards[3].first, forward.cards[3].second) } }
    }

    @Test fun station_stamp() = paparazzi.snapshot {
        StarMistTheme { Box(Modifier.fillMaxSize().background(Vintage.parchment).padding(16.dp)) { StationStamp("鳳林", "1913", Modifier.padding(8.dp)) } }
    }

    /** The traveller standing, then the six frames of the drawn walk cycle, side by side. */
    @Test fun walker_frames() = paparazzi.snapshot {
        StarMistTheme {
            val art = rememberWalkerArt()
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize().background(Vintage.parchment)) {
                val colW = size.width / 7f
                for (i in 0..6) {
                    val p = androidx.compose.ui.geometry.Offset(colW * (i + 0.5f), size.height * 0.5f)
                    drawWalker(p, 4500f, art, if (i == 0) 0f else (i - 1) / 6f + 0.01f, walk = if (i > 0) 1f else 0f)
                }
            }
        }
    }

    /** Facing left, standing, and halfway through the turn and through the start of a walk. */
    @Test fun walker_turns() = paparazzi.snapshot {
        StarMistTheme {
            val art = rememberWalkerArt()
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize().background(Vintage.parchment)) {
                val colW = size.width / 6f
                val states = listOf(Triple(1f, 0f, 0f), Triple(-1f, 0f, 0f), Triple(0.5f, 0f, 0f), Triple(0f, 0f, 0f), Triple(-1f, 0.5f, 0.2f), Triple(-1f, 1f, 0.4f))
                states.forEachIndexed { i, (face, walk, phase) ->
                    drawWalker(androidx.compose.ui.geometry.Offset(colW * (i + 0.5f), size.height * 0.5f), 4500f, art, phase, walk, face)
                }
            }
        }
    }
}
