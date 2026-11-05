package com.starmist.walker.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [DailyStepsEntity::class, SnapshotStateEntity::class, RebootLogEntity::class],
    version = 1,
    exportSchema = false,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stepDao(): StepDao
}

/** Room fills this in for each platform. */
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

/** Runs [block] so that all the database changes in it happen together or not at all (each platform supplies its own). */
interface DbTransaction {
    suspend fun <R> run(block: suspend () -> R): R
}
