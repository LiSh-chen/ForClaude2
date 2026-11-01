package com.starmist.walker.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.datetime.LocalDate
import com.starmist.core.time.now
import com.starmist.walker.R
import com.starmist.walker.data.SettingsStore
import com.starmist.walker.ui.MainActivity
import com.starmist.walker.ui.withCommas
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Tells the user once a day when the day's step goal is reached: a notification (when the system lets us post one) and, for the screen the app is showing,
 * an event on [reached]. Called with the day's steps from the step service and from the screen.
 */
class GoalWatcher(private val context: Context, private val settings: SettingsStore) {
    private val _reached = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val reached: SharedFlow<Int> = _reached.asSharedFlow()
    private val lock = Mutex()

    suspend fun check(steps: Long) = lock.withLock {
        val s = settings.current()
        val day = LocalDate.now().toString()
        if (!s.goalNotify || steps < s.dailyGoal || s.goalShownOn == day) return@withLock
        settings.update { it.copy(goalShownOn = day) }
        _reached.tryEmit(s.dailyGoal)
        post(steps, s.dailyGoal)
    }

    private fun post(steps: Long, goal: Int) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "每日目標", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "走到每日步數目標時通知一次" })
        val open = PendingIntent.getActivity(context, 1, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_walk)
            .setContentTitle("今日目標達成！")
            .setContentText("今天已走 ${steps.withCommas()} 步，達成每日目標 ${goal.toLong().withCommas()} 步。")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL_ID = "daily_goal"
        const val NOTIFICATION_ID = 2
    }
}
