package com.starmist.walker.ui

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.floor

/** 1234567 -> "1,234,567". */
fun Long.withCommas(): String {
    val digits = abs(this).toString()
    val grouped = digits.reversed().chunked(3).joinToString(",").reversed()
    return if (this < 0) "-$grouped" else grouped
}

/** The number with exactly [digits] decimals, rounded half up ("%.2f" on every phone system alike). */
fun Double.fixed(digits: Int): String {
    var scale = 1L
    repeat(digits) { scale *= 10 }
    val scaled = floor(abs(this) * scale + 0.5).toLong()
    val whole = scaled / scale
    val text = if (digits == 0) "$whole" else "$whole.${(scaled % scale).toString().padStart(digits, '0')}"
    return if (this < 0 && scaled != 0L) "-$text" else text
}

fun Double.twoDecimals(): String = fixed(2)

/** "MM/dd HH:mm" in the phone's time zone. */
fun formatTime(epochMillis: Long): String {
    val t = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    fun two(n: Int) = n.toString().padStart(2, '0')
    return "${two(t.monthNumber)}/${two(t.dayOfMonth)} ${two(t.hour)}:${two(t.minute)}"
}
