package com.starmist.walker.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** The folder the app keeps its files in on an iPhone. */
@OptIn(ExperimentalForeignApi::class)
fun iosDocumentsDirectory(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(NSDocumentDirectory, NSUserDomainMask, null, false, null)
    return requireNotNull(url?.path) { "no documents folder" }
}

/** The small settings files (same names as on Android), one per store. */
fun createIosDataStore(name: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { "${iosDocumentsDirectory()}/$name.preferences_pb".toPath() })

/** The step database, in the app's documents folder. */
fun createIosDatabase(): AppDatabase =
    Room.databaseBuilder<AppDatabase>(name = "${iosDocumentsDirectory()}/starmist.db")
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

/** Changes made in [block] are committed together (Android uses Room's own helper for this). */
class IosDbTransaction(private val database: AppDatabase) : DbTransaction {
    override suspend fun <R> run(block: suspend () -> R): R =
        database.useWriterConnection { it.immediateTransaction { block() } }
}
