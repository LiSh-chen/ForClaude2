package com.starmist.walker.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun Long.withCommas(): String = String.format(Locale.getDefault(), "%,d", this)

fun Double.oneDecimal(): String = String.format(Locale.getDefault(), "%.1f", this)

fun Double.twoDecimals(): String = String.format(Locale.getDefault(), "%.2f", this)

private val timeFormat = DateTimeFormatter.ofPattern("MM/dd HH:mm")

fun formatTime(epochMillis: Long): String =
    timeFormat.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
