package com.starmist.walker

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.starmist.walker.content.ContentStore
import com.starmist.walker.data.AppDatabase
import com.starmist.walker.data.regionDataStore
import com.starmist.walker.data.routeDataStore
import com.starmist.walker.data.settingsDataStore
import com.starmist.walker.platform.AndroidPlatformInfo
import com.starmist.walker.platform.AppServices
import com.starmist.walker.platform.PlatformInfo
import com.starmist.walker.ui.TownAreas
import com.starmist.walker.ui.TownTag
import com.starmist.walker.ui.loadTownBlurbs
import com.starmist.walker.data.RegionStore
import com.starmist.walker.data.RouteStore
import com.starmist.walker.data.SettingsStore
import com.starmist.walker.data.StepRepository
import com.starmist.walker.sensor.StepCounterReader
import com.starmist.walker.work.SnapshotScheduler

/** Hand-wired dependencies; small enough that a DI framework would only add weight. */
class AppContainer(context: Context) : AppServices {
    private val appContext = context.applicationContext
    private val database = Room.databaseBuilder(appContext, AppDatabase::class.java, "starmist.db").build()
    val content = ContentStore(appContext)
    override val settings = SettingsStore(appContext.settingsDataStore)
    override val routes = RouteStore(appContext.routeDataStore)
    override val regions = RegionStore(appContext.regionDataStore)
    override val platform: PlatformInfo = AndroidPlatformInfo(appContext)
    val reader = StepCounterReader(appContext)
    override val repository = StepRepository(platform, database, settings, reader)
    override val townAreas: TownAreas by lazy { TownAreas.load(appContext) }
    override val townBlurbs: Map<String, List<TownTag>> by lazy { loadTownBlurbs(appContext) }
}

class StarMistApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.content.applyToRoutes()
        SnapshotScheduler.ensureScheduled(this)
    }
}

val Context.container: AppContainer
    get() = (applicationContext as StarMistApp).container
