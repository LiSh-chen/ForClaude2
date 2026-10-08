package com.starmist.walker.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.starmist.walker.container
import com.starmist.walker.tracking.StepService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * After a restart the step counter starts again from zero. Re-arm the schedule, take a reading, and
 * bring stable mode back up so counting resumes without the user opening the app.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        SnapshotScheduler.ensureScheduled(context)
        SnapshotScheduler.snapshotSoon(context)

        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (context.container.settings.current().stableMode) StepService.start(context)
            } finally {
                pending.finish()
            }
        }
    }
}
