package com.starmist.walker.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.starmist.walker.container
import java.util.concurrent.TimeUnit

/**
 * Background reading schedule. Everything here is deferrable, inexact work that WorkManager
 * batches with other apps' jobs and that respects Doze, so it adds almost no battery cost. A
 * missed run loses nothing: the hardware counter keeps counting and the next reading catches up.
 * Steps between two readings that straddle midnight are split by time (see DaySplitter), so no
 * exact-time alarm is needed for the day boundary.
 */
object SnapshotScheduler {
    private const val PERIODIC = "snapshot_periodic"
    private const val ON_BOOT = "snapshot_boot"

    /** Safe to call repeatedly; existing schedules are kept. */
    fun ensureScheduled(context: Context) {
        val work = WorkManager.getInstance(context)
        work.enqueueUniquePeriodicWork(
            PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SnapshotWorker>(30, TimeUnit.MINUTES).build(),
        )
    }

    fun snapshotSoon(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            ON_BOOT,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<SnapshotWorker>().build(),
        )
    }
}

class SnapshotWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        applicationContext.container.repository.takeSnapshot()
        return Result.success()
    }
}
