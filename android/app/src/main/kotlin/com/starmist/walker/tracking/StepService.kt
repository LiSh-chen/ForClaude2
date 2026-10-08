package com.starmist.walker.tracking

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.starmist.walker.R
import com.starmist.walker.container
import com.starmist.walker.ui.MainActivity
import com.starmist.walker.ui.withCommas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Keeps the step sensor registered so the phone keeps counting with the screen off.
 *
 * The listener asks the sensor hub to batch events for a couple of minutes, so the CPU is only
 * woken occasionally while walking and not at all while standing still. No wake lock is held.
 */
class StepService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val type = if (Build.VERSION.SDK_INT >= 34) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(null), type)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasPermission(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (job?.isActive != true) {
            _running.value = true
            job = scope.launch { track() }
        }
        return START_STICKY
    }

    private suspend fun track() {
        val repository = container.repository
        // Catch up on anything counted since the last reading before listening live.
        repository.takeSnapshot()
        updateNotification()
        repository.reader.liveCounter(BATCH_LATENCY_US).conflate().collect { counter ->
            repository.ingestLive(counter)
            updateNotification()
            delay(MIN_INTERVAL_MS) // conflate keeps only the newest value meanwhile
        }
    }

    private suspend fun updateNotification() {
        val steps = container.repository.getDay(LocalDate.now())?.total ?: 0L
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(steps))
    }

    private fun buildNotification(steps: Long?): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_walk)
            .setContentTitle("星霧大陸 正在計步")
            .setContentText(steps?.let { "今日 ${it.withCommas()} 步" } ?: "準備中…")
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "計步", NotificationManager.IMPORTANCE_LOW).apply {
            description = "讓手機在螢幕關閉時也能持續計步"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        _running.value = false
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "step_tracking"
        private const val NOTIFICATION_ID = 1
        private const val BATCH_LATENCY_US = 120_000_000 // sensor may hold events up to 2 min
        private const val MIN_INTERVAL_MS = 15_000L

        private val _running = MutableStateFlow(false)

        /** True while the service is alive in this process. */
        val running: StateFlow<Boolean> = _running

        private fun hasPermission(context: Context) =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
                PackageManager.PERMISSION_GRANTED

        /** Starts the service if the permission is there; quietly does nothing if Android refuses right now. */
        fun start(context: Context) {
            if (!hasPermission(context)) return
            try {
                ContextCompat.startForegroundService(context, Intent(context, StepService::class.java))
            } catch (e: IllegalStateException) {
                // Background start not allowed at this moment; the next app launch or boot retries.
            } catch (e: SecurityException) {
                // A required permission is missing.
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, StepService::class.java))
        }
    }
}
