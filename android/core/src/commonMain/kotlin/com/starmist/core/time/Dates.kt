package com.starmist.core.time

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

/*
 * Small conveniences over kotlinx-datetime, named like the java.time calls they replace, so that the same
 * date code runs on Android and on iOS.
 */

fun LocalDate.Companion.now(zone: TimeZone = TimeZone.currentSystemDefault()): LocalDate = Clock.System.todayIn(zone)

fun LocalDate.Companion.of(year: Int, month: Int, day: Int): LocalDate = LocalDate(year, month, day)

fun LocalDate.plusDays(n: Long): LocalDate = plus(n, DateTimeUnit.DAY)
fun LocalDate.minusDays(n: Long): LocalDate = minus(n, DateTimeUnit.DAY)
fun LocalDate.plusWeeks(n: Long): LocalDate = plus(n, DateTimeUnit.WEEK)
fun LocalDate.plusMonths(n: Long): LocalDate = plus(n, DateTimeUnit.MONTH)
fun LocalDate.minusMonths(n: Long): LocalDate = minus(n, DateTimeUnit.MONTH)
fun LocalDate.plusYears(n: Long): LocalDate = plus(n, DateTimeUnit.YEAR)

fun LocalDate.toEpochDay(): Long = toEpochDays().toLong()
fun LocalDate.isBefore(other: LocalDate): Boolean = this < other
fun LocalDate.isAfter(other: LocalDate): Boolean = this > other

val LocalDate.monthValue: Int get() = monthNumber

fun LocalDate.withDayOfMonth(day: Int): LocalDate = LocalDate(year, monthNumber, day)
fun LocalDate.lengthOfMonth(): Int = YearMonth.from(this).lengthOfMonth()

/** Monday = 1 .. Sunday = 7. */
val DayOfWeek.value: Int get() = isoDayNumber

/** The Monday of the week this date is in (or the date itself, if it is a Monday). */
fun LocalDate.startOfWeek(): LocalDate = minusDays((dayOfWeek.isoDayNumber - 1).toLong())

/** A calendar month. */
data class YearMonth(val year: Int, val month: Int) {
    val monthValue: Int get() = month
    fun atDay(day: Int): LocalDate = LocalDate(year, month, day)
    fun lengthOfMonth(): Int = atEndOfMonth().dayOfMonth
    fun atEndOfMonth(): LocalDate = LocalDate(year, month, 1).plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

    companion object {
        fun from(date: LocalDate) = YearMonth(date.year, date.monthNumber)
        fun of(year: Int, month: Int) = YearMonth(year, month)
    }
}
