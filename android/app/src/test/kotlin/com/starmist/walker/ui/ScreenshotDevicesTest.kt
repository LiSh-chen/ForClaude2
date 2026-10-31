package com.starmist.walker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import org.junit.Test

/*
 * The bottom panel on the phones and with the three writing sizes the user can pick in the settings (small, medium, large), so that each of them is looked at:
 * a narrow phone (360 dp wide) and the usual one (393 dp), each with the dials, the console, and the card of a township picked on the map; and the system's
 * font size turned right up, which the app ignores.
 */
abstract class PanelShots(device: DeviceConfig, step: Int) : ShotsBase(device, step) {
    @Test fun dials() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 85.0)) }
    @Test fun console() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 85.0), controlsOpen = true) }
    @Test fun township() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 830.0), regionMode = true) }
}

class ScreenshotNarrowSmallTest : PanelShots(DeviceConfig.NEXUS_5, 0)
class ScreenshotNarrowMediumTest : PanelShots(DeviceConfig.NEXUS_5, 1)
class ScreenshotNarrowLargeTest : PanelShots(DeviceConfig.NEXUS_5, 2)
class ScreenshotUsualSmallTest : PanelShots(DeviceConfig.PIXEL_5, 0)
class ScreenshotUsualLargeTest : PanelShots(DeviceConfig.PIXEL_5, 2)
class ScreenshotSystemFontTest : PanelShots(DeviceConfig.PIXEL_5.copy(fontScale = 1.6f), 1)

/** The picker in the settings, in each of its three states. */
class ScreenshotFontPickerTest : ShotsBase(DeviceConfig.PIXEL_5) {
    @Test fun font_picker() = paparazzi.snapshot {
        Page(3) { pad ->
            Column(Modifier.padding(pad).padding(16.dp)) {
                FontSizePicker(0) {}
                FontSizePicker(1) {}
                FontSizePicker(2) {}
            }
        }
    }
}
