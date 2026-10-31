package com.starmist.walker.ui

import app.cash.paparazzi.DeviceConfig
import org.junit.Test

/** The bottom panel on a narrow phone (360 dp wide), so that what fits on one phone is looked at on a smaller one too. */
class ScreenshotNarrowTest : ShotsBase(DeviceConfig.NEXUS_5) {
    @Test fun narrow_dials() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 85.0)) }
    @Test fun narrow_console() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 85.0), controlsOpen = true) }
}

/** The same with the system's font size turned right up: the panel keeps its own sizes. */
class ScreenshotBigFontTest : ShotsBase(DeviceConfig.PIXEL_5.copy(fontScale = 1.6f)) {
    @Test fun bigfont_dials() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 85.0)) }
    @Test fun bigfont_console() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 85.0), controlsOpen = true) }
}
