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
    @Test fun notebook() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 85.0), notebook = true) }
    @Test fun township() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 830.0), regionMode = true) }
    /** A township with a long note, to see that all of it is shown. */
    /** A township with three features, whose chips wrap onto a second line. */
    @Test fun township_three() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 830.0), pick = "淡水區") }
    @Test fun township_long() = paparazzi.snapshot { Home(true, HomeMap.Real(forward, 830.0), pick = "湖口鄉") }
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

/** The first-run pages and the privacy policy, at the three writing sizes. */
abstract class FirstRunShots(step: Int) : ShotsBase(DeviceConfig.PIXEL_5, step) {
    private fun page(n: Int) = paparazzi.snapshot {
        Page { _ -> OnboardingScreen(com.starmist.walker.data.UserSettings(), {}, permissionGranted = n == 3, onRequestPermission = {}, onFinish = {}, startPage = n) }
    }
    @Test fun first_run_0() = page(0)
    @Test fun first_run_1() = page(1)
    @Test fun first_run_2() = page(2)
    @Test fun first_run_3() = page(3)
    @Test fun first_run_4() = page(4)
    @Test fun privacy() = paparazzi.snapshot { Page { _ -> PrivacySheet {} } }
}

class ScreenshotFirstRunSmallTest : FirstRunShots(0)
class ScreenshotFirstRunMediumTest : FirstRunShots(1)
class ScreenshotFirstRunLargeTest : FirstRunShots(2)
