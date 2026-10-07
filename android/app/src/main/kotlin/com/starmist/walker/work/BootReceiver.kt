package com.starmist.walker.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** After a restart the step counter starts again from zero; take a reading early and re-arm the schedule. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        SnapshotScheduler.ensureScheduled(context)
        SnapshotScheduler.snapshotSoon(context)
    }
}
