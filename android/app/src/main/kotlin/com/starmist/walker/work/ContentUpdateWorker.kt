package com.starmist.walker.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.starmist.walker.container
import java.util.concurrent.TimeUnit

/** Checks for a newer content pack about twice a day, only when the phone is online. */
object ContentUpdateScheduler {
    private const val NAME = "content_update"

    fun ensureScheduled(context: Context) {
        val request = PeriodicWorkRequestBuilder<ContentUpdateWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}

class ContentUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        applicationContext.container.content.refresh()
        return Result.success() // a failed check is simply tried again at the next period
    }
}
