package com.starmist.walker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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

    private val forward = Routes.byId("taiwan_loop")!!

    @Composable
    private fun Page(tab: Int = 0, content: @Composable (PaddingValues) -> Unit) {
        StarMistTheme {
            Scaffold(containerColor = Vintage.parchment, bottomBar = { AppBottomBar(tab) {} }) { content(it) }
        }
    }

    @Composable
    private fun Home(real: Boolean, map: HomeMap, steps: Long = 4321) {
        Page { pad ->
            HomeContent(
                padding = pad, steps = steps, goal = 9000, km = steps * 0.0007, kcal = steps * 0.04,
                lastSnapshot = null, real = real, granted = true, hasCounter = true, map = map,
                onChallenge = {}, onRequestPermission = {}, onOpenAppSettings = {}, onSync = {}, onStartRoute = {},
            )
        }
    }

    @Test fun home_virtual_start() = paparazzi.snapshot { Home(false, HomeMap.Virtual("星霧森林", 0, 0.02, false), 120) }
    @Test fun home_virtual_mid() = paparazzi.snapshot { Home(false, HomeMap.Virtual("星霧森林", 0, 0.55, false)) }
    @Test fun home_virtual_coast() = paparazzi.snapshot { Home(false, HomeMap.Virtual("海岸", 1, 0.4, false)) }
    @Test fun home_real_start() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 0.0), 120) }
    @Test fun home_real_mid() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 85.0)) }
    @Test fun home_real_east() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 260.0)) }
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
        StarMistTheme { Box(Modifier.padding(16.dp)) { CardFace(forward.cards[3].first, forward.cards[3].second) } }
    }

    @Test fun station_stamp() = paparazzi.snapshot {
        StarMistTheme { Box(Modifier.padding(16.dp)) { StationStamp("鳳林", "1913", Modifier.padding(8.dp)) } }
    }
}
