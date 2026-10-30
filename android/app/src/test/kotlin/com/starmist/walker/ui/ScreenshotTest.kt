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
    init { decodeThumbsInline = true }

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

    /** Free exploration, a third of the way from the township at [km] on the loop to the next one east/south of it. */
    private fun Explore(regionMode: Boolean, picked: Boolean = true) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val areas = remember { TownAreas.load(context) }
        val blurbs = remember { loadTownBlurbs(context) }
        val nb = remember { areas.neighborLists() }
        val loop = remember { areas.visited(forward, 85.0) }
        val at = forward.positionAt(85.0)
        val st0 = ExploreEngine.start(areas, at, 0, 90f)!!
        val target = nb[st0.current].filter { it !in loop }.firstOrNull() ?: nb[st0.current].first()
        val st = if (picked) ExploreEngine.go(st0, areas, target).let { it.copy(progressKm = ExploreEngine.legKm(it, areas) / 3) } else st0
        val view = ExploreEngine.view(st, areas, nb[st.current])
        val explored = loop.keys + st.visited
        val map = HomeMap.Real(forward, 85.0, explore = view, exploredCount = explored.size)
        Page { pad ->
            HomeContent(
                padding = pad, steps = 4321, goal = 9000, km = 3.0, kcal = 170.0,
                lastSnapshot = null, granted = true, hasCounter = true, map = map,
                onRequestPermission = {}, onOpenAppSettings = {}, onSync = {}, onStartRoute = {}, onToggleExplore = {},
                currentTown = st.current, exploredTowns = explored,
                infoOf = { id -> if (id !in areas.towns.indices) null else areas.towns[id].let { t -> RegionInfo(t, blurbs["${t.county}|${t.name}"], 1, 0.0, 1, id in explored) } },
                initialRegionMode = regionMode,
            )
        }
    }

    @Test fun explore_route_mode() = paparazzi.snapshot { Explore(false) }
    @Test fun explore_region_mode() = paparazzi.snapshot { Explore(true, picked = false) }

    /** Where on the loop the walker is closest to a small sight, so the screen shows how such sights look. */
    private fun kmNear(poiId: String): Double {
        val p = forward.pois.first { it.id == poiId }
        var best = 0.0; var bestD = Double.MAX_VALUE; var k = 0.0
        while (k <= forward.totalKm) { val d = forward.positionAt(k).distanceKm(p.at); if (d < bestD) { bestD = d; best = k }; k += 0.5 }
        return best
    }

    @Test fun home_poi_nanfangao() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, kmNear("nanfangao")), 6200) }
    @Test fun home_poi_kaohsiung() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, kmNear("cijin")), 9800) }
    @Test fun home_poi_ahead() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, kmNear("luodong") - 14.0), 5400) }
    @Test fun home_poi_tainan() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, kmNear("chihkan")), 3100) }

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
            val places = rememberTownAreas().cardPlaces
            val features = rememberLandmarkFacts()
            androidx.compose.foundation.lazy.LazyColumn(Modifier.padding(pad), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                item { androidx.compose.material3.Text("收藏・真實世界") }
                routeProgressItems(forward, com.starmist.core.route.RouteState(forward.id, 0L, 260.0), places, features, emptyList(), onToggle = {}, onOpen = {}, onRestart = {}, onSwitch = {})
            }
        }
    }

    @Test fun collection_album() = paparazzi.snapshot {
        Page(tab = 3) { pad ->
            val places = rememberTownAreas().cardPlaces
            val features = rememberLandmarkFacts()
            androidx.compose.foundation.lazy.LazyColumn(Modifier.padding(pad), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                item { androidx.compose.material3.Text("收藏・真實世界") }
                routeProgressItems(forward, com.starmist.core.route.RouteState(forward.id, 0L, 260.0), places, features, emptyList(), onToggle = {}, onOpen = {}, onRestart = {}, onSwitch = {}, album = true)
            }
        }
    }

    /** A few of the daily statistics cards, one under the other. */
    @Test fun stats_cards() = paparazzi.snapshot {
        val today = java.time.LocalDate.of(2026, 10, 28)
        val history = (0L..100L).associate { back ->
            val d = today.minusDays(back)
            d to when { d.dayOfMonth == 20 -> 0L; d.dayOfWeek.value == 6 -> 11000L; d.dayOfWeek.value == 7 -> 9000L; else -> 4000L + (d.dayOfMonth * 137L) % 5000 }
        }
        val towns = listOf(TownVisit("鹿港鎮", "彰化縣", 9.5, 39.0, 20, true), TownVisit("秀水鄉", "彰化縣", 3.2, 36.0, 15, true), TownVisit("伸港鄉", "彰化縣", 1.4, 23.0, 10, true), TownVisit("和美鎮", "彰化縣", 4.8, 38.0, 12, false))
        val inputs = StatInputs(today, history, 8000, 0.7, 1100.0, 275.0, "某某卡" to 12.3, towns, 109, 5, 9, 45)
        StarMistTheme {
            androidx.compose.foundation.layout.Column(Modifier.fillMaxSize().background(Vintage.parchment).padding(12.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp)) {
                listOf(StatKind.HEATMAP, StatKind.WEEKDAY, StatKind.TOWN_LONGEST, StatKind.FORTUNE).forEach { k ->
                    buildStat(k, inputs, dayStatSeed(today))?.let { StatCardView(it) }
                }
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
                    drawWalker(p, 4500f, art, if (i == 0) 0f else ((i - 1 + 4) % 6) / 6f + 0.001f, walk = if (i > 0) 1f else 0f)   // phase 0 is drawn frame 2, so frame f is at (f + 4) % 6 sixths
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

    /** The standing-still acts, one per row, at five moments each. */
    @Test fun walker_idle() = paparazzi.snapshot {
        StarMistTheme {
            val art = rememberWalkerArt()
            val measurer = androidx.compose.ui.text.rememberTextMeasurer()
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize().background(Vintage.parchment)) {
                val colW = size.width / 5f
                val rowH = size.height / 6f
                for (act in 1..Idle.COUNT) {
                    val ts = listOf(0.1f, 0.3f, 0.5f, 0.7f, 0.9f)
                    ts.forEachIndexed { i, t ->
                        drawWalker(androidx.compose.ui.geometry.Offset(colW * (i + 0.5f), rowH * (act + 0.2f)), 4500f, art, 0f, 0f, 1f, act, t, measurer)
                    }
                }
            }
        }
    }
}
