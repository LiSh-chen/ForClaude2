package com.starmist.walker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {
    @Query("SELECT * FROM daily_steps WHERE date BETWEEN :from AND :to")
    fun observeRange(from: String, to: String): Flow<List<DailyStepsEntity>>

    @Query("SELECT * FROM daily_steps WHERE date = :date")
    suspend fun getDay(date: String): DailyStepsEntity?

    /** Creates an all-zero row for the day if there is none (SQLite on Android 10 has no UPSERT). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: DailyStepsEntity)

    @Query(
        "UPDATE daily_steps SET rawSteps = rawSteps + :raw, scaledSteps = scaledSteps + :scaled " +
            "WHERE date = :date",
    )
    suspend fun addSteps(date: String, raw: Long, scaled: Long)

    @Query("UPDATE daily_steps SET manualAdjust = :adjust WHERE date = :date")
    suspend fun setManualAdjust(date: String, adjust: Long)

    @Query("SELECT * FROM snapshot_state WHERE id = 1")
    suspend fun getState(): SnapshotStateEntity?

    @Query("SELECT * FROM snapshot_state WHERE id = 1")
    fun observeState(): Flow<SnapshotStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putState(state: SnapshotStateEntity)

    @Insert
    suspend fun insertReboot(entry: RebootLogEntity)

    @Query("SELECT * FROM reboot_log ORDER BY atMillis DESC LIMIT :limit")
    fun observeRecentReboots(limit: Int): Flow<List<RebootLogEntity>>
}
