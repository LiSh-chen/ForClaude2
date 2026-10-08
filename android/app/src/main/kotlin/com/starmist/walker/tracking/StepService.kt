package com.starmist.walker.tracking

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.starmist.walker.R
import com.starmist.walker.container
import com.starmist.walker.sensor.StepCounterReader
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Keeps the step sensor registered so the phone keeps counting with the screen off.
 *
 * Batching follows the screen: with the screen off the sensor hub holds events for a couple of
 * minutes, so the CPU is only woken occasionally while walking and not at all while standing
 * still. With the screen on (someone may be looking at the count) events are delivered at once and
 * the notification refreshes about once a second. No wake lock is held.
 */
class StepService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var session: StepCounterReader.LiveSession? = null
    private val values = Channel<Long>(Channel.CONFLATED)

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> applyBatching(screenOn = true)
                Intent.ACTION_SCREEN_OFF -> applyBatching(screenOn = false)
            }
        }
    }

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

        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        session = repository.reader.openSession { values.trySend(it) }
        val power = getSystemService(PowerManager::class.java)
        applyBatching(screenOn = power.isInteractive)

        // The channel keeps only the newest value, so the delay below simply spaces out the work.
        for (counter in values) {
            repository.ingestLive(counter)
            if (screenIsOn) updateNotification()
            delay(if (screenIsOn) SCREEN_ON_INTERVAL_MS else SCREEN_OFF_INTERVAL_MS)
        }
    }

    @Volatile
    private var screenIsOn = true

    /** Immediate delivery while the screen is on, long batching while it is off. */
    private fun applyBatching(screenOn: Boolean) {
        screenIsOn = screenOn
        val session = session ?: return
        session.start(if (screenOn) 0 else BATCH_LATENCY_US)
        // On screen-on, pull the latest value out of the sensor right away so the count is current.
        if (screenOn) session.flush()
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
        session?.stop()
        session = null
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: IllegalArgumentException) {
            // never registered (service stopped before it got that far)
        }
        values.close()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "step_tracking"
        private const val NOTIFICATION_ID = 1
        private const val BATCH_LATENCY_US = 120_000_000 // screen off: sensor may hold events up to 2 min
        private const val SCREEN_ON_INTERVAL_MS = 1_000L
        private const val SCREEN_OFF_INTERVAL_MS = 5_000L

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
