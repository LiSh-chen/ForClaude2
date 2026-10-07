package com.starmist.walker

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.starmist.walker.data.AppDatabase
import com.starmist.walker.data.SettingsStore
import com.starmist.walker.data.StepRepository
import com.starmist.walker.sensor.StepCounterReader
import com.starmist.walker.work.SnapshotScheduler

/** Hand-wired dependencies; small enough that a DI framework would only add weight. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = Room.databaseBuilder(appContext, AppDatabase::class.java, "starmist.db").build()
    val settings = SettingsStore(appContext)
    val reader = StepCounterReader(appContext)
    val repository = StepRepository(appContext, database, settings, reader)
}

class StarMistApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SnapshotScheduler.ensureScheduled(this)
    }
}

val Context.container: AppContainer
    get() = (applicationContext as StarMistApp).container
