package com.starmist.walker.ui

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Locale

fun Long.withCommas(): String = String.format(Locale.getDefault(), "%,d", this)

fun Double.twoDecimals(): String = String.format(Locale.getDefault(), "%.2f", this)

/** "MM/dd HH:mm" in the phone's time zone. */
fun formatTime(epochMillis: Long): String {
    val t = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    fun two(n: Int) = n.toString().padStart(2, '0')
    return "${two(t.monthNumber)}/${two(t.dayOfMonth)} ${two(t.hour)}:${two(t.minute)}"
}
