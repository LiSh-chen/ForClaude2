package com.starmist.walker.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.starmist.walker.tracking.StepService

class AndroidPlatformInfo(private val context: Context) : PlatformInfo {
    override fun hasStepPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    override fun batteryRestricted(): Boolean {
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return !power.isIgnoringBatteryOptimizations(context.packageName)
    }

    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun elapsedSinceBootMillis(): Long = SystemClock.elapsedRealtime()

    override val stepServiceRunning: Boolean get() = StepService.running.value
}
