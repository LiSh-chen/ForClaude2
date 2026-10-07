package com.starmist.walker.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DailyStepsEntity::class, SnapshotStateEntity::class, RebootLogEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stepDao(): StepDao
}
