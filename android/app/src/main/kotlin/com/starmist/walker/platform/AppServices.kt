package com.starmist.walker.platform

import com.starmist.walker.data.RegionStore
import com.starmist.walker.data.RouteStore
import com.starmist.walker.data.SettingsStore
import com.starmist.walker.data.StepRepository
import com.starmist.walker.ui.TownAreas
import com.starmist.walker.ui.TownTag

/** Everything the view model needs, so that it does not have to know which phone system it is on. */
interface AppServices {
    val settings: SettingsStore
    val routes: RouteStore
    val regions: RegionStore
    val repository: StepRepository
    val platform: PlatformInfo

    /** The township outlines and their features, read once from the app's files. */
    val townAreas: TownAreas
    val townBlurbs: Map<String, List<TownTag>>
}
