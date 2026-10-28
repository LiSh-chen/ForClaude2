package com.starmist.walker.ui

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * The statistics the daily draw can pick from. Each is a small card made from what the app already knows:
 * the step history, the route, and the townships walked through. [buildStat] makes the card (or says there is
 * not enough to show yet), [drawStat] picks today's.
 */
enum class StatKind(val emoji: String, val title: String) {
    RULER("📏", "拿熟悉的東西量一量"),
    HEATMAP("🗓️", "本月熱度圖"),
    WEEKDAY("📆", "你的星期幾"),
    TOWN_LONGEST("🐢", "逗留最久的鄉鎮"),
    TOWN_SHORTEST("🐇", "一溜煙穿過的鄉鎮"),
    TOWN_BIGGEST("🗺️", "走過的鄉鎮之大"),
    TOWN_HIGHEST("⛰️", "走過的鄉鎮之高"),
    FOOTPRINT("👣", "鄉鎮足跡"),
    WEEK_VS_LAST("⚖️", "這週對上週"),
    MONTH_VS_LAST("🌙", "這個月對上個月"),
    STREAK("🔥", "連續達標"),
    BEST_DAY("🏅", "紀錄日"),
    BEST_MONTH("🏆", "月份排行"),
    TODAY_VS_AVG("🎯", "今天對上平常的你"),
    ROUTE_PROGRESS("🧭", "環島進度"),
    CARDS("🃏", "景點卡收集"),
    FEATURES("✨", "找到的特色"),
    MILESTONE("🚩", "下一個里程碑"),
    FORTUNE("🎋", "旅人籤詩"),
}

/** A township the route has gone through: [passed] is false for the one the traveller is in now. */
data class TownVisit(val name: String, val county: String, val km: Double, val areaKm2: Double, val highElevM: Int, val passed: Boolean)

/** Everything the cards are made from. */
data class StatInputs(
    val today: LocalDate,
    val history: Map<LocalDate, Long>,
    val goal: Int,
    val strideM: Double,
    val routeTotalKm: Double = 0.0,
    val walkedKm: Double = 0.0,
    val nextCard: Pair<String, Double>? = null,
    val towns: List<TownVisit> = emptyList(),
    val townsTotal: Int = 0,
    val tagsFound: Int = 0,
    val cardsOwned: Int = 0,
    val cardsTotal: Int = 0,
)

/** A picture for a card: a month as a calendar, or a few labelled bars. */
sealed interface StatVisual {
    /** [steps] for each day of [month] (index 0 is the 1st; days after [todayIndex] are still to come). */
    class Heat(val month: YearMonth, val steps: List<Long>, val goal: Int, val todayIndex: Int) : StatVisual

    /** [rows] are label to value; [highlight] is the row to colour (or -1); [unit] follows each value. */
    class Bars(val rows: List<Pair<String, Double>>, val unit: String, val highlight: Int = -1) : StatVisual
}

class StatCard(val kind: StatKind, val headline: String, val line: String, val note: String? = null, val visual: StatVisual? = null)

private val weekdayNames = arrayOf("週一", "週二", "週三", "週四", "週五", "週六", "週日")

private class Ruler(val doing: String, val unit: String, val km: Double, val note: String)

private fun steps(n: Long) = n.withCommas()
private fun nice(x: Double): String = if (x >= 10) x.roundToLong().withCommas() else String.format("%.1f", x)

/** Days from [from] to [to], both included. */
private fun span(from: LocalDate, to: LocalDate): Sequence<LocalDate> = generateSequence(from) { if (it < to) it.plusDays(1) else null }

private fun StatInputs.sum(from: LocalDate, to: LocalDate): Long = span(from, to).sumOf { history[it] ?: 0L }

/** Consecutive days reaching the goal, ending today (or yesterday while today is not done yet), and the longest run in the last year. */
internal fun streaks(history: Map<LocalDate, Long>, goal: Int, today: LocalDate): Pair<Int, Int> {
    if (goal <= 0) return 0 to 0
    fun hit(d: LocalDate) = (history[d] ?: 0L) >= goal
    var cur = 0
    var d = if (hit(today)) today else today.minusDays(1)
    while (hit(d)) { cur++; d = d.minusDays(1) }
    var longest = 0
    var run = 0
    span(today.minusDays(364), today).forEach { day -> if (hit(day)) { run++; if (run > longest) longest = run } else run = 0 }
    return cur to maxOf(longest, cur)
}

/** Builds [kind]'s card, or null when there is not enough yet. [seed] picks among a card's variants (the same all day). */
fun buildStat(kind: StatKind, i: StatInputs, seed: Long): StatCard? {
    val rnd = Random(seed * 31 + kind.ordinal)
    val total = i.history.values.sum()
    val daysWithSteps = i.history.count { it.value > 0 }
    return when (kind) {
        StatKind.RULER -> {
            if (total < 100 || i.strideM <= 0) return null
            val km = total * i.strideM / 1000.0
            val rulers = buildList {
                add(Ruler("繞標準操場", "圈", 0.4, "標準操場一圈是 400 公尺。"))
                add(Ruler("跑全程馬拉松", "場", 42.195, "全程馬拉松是 42.195 公里。"))
                add(Ruler("走完台灣本島的南北長度", "趟", 394.0, "台灣本島南北約長 394 公里。"))
                add(Ruler("走完高鐵的營運路線", "趟", 350.0, "高鐵營運路線約 350 公里。"))
                if (i.routeTotalKm > 0) add(Ruler("走完這條環島路線", "圈", i.routeTotalKm, "這條路線全長約 ${i.routeTotalKm.roundToInt()} 公里。"))
            }
            val r = rnd.nextInt(rulers.size).let { rulers[it] }
            StatCard(kind, "${nice(km / r.km)} ${r.unit}", "累計 ${steps(total)} 步，約 ${nice(km)} 公里，等於${r.doing}。", r.note)
        }
        StatKind.HEATMAP -> {
            val ym = YearMonth.from(i.today)
            val days = (1..ym.lengthOfMonth()).map { i.history[ym.atDay(it)] ?: 0L }
            val upto = i.today.dayOfMonth
            val walked = days.take(upto).count { it > 0 }
            if (walked == 0) return null
            val reached = if (i.goal > 0) days.take(upto).count { it >= i.goal } else 0
            val best = days.take(upto).withIndex().maxByOrNull { it.value }!!
            StatCard(kind, "$walked 天有出門走走", "達標 $reached 天，單日步數最多是 ${ym.monthValue}/${best.index + 1} 的 ${steps(best.value)} 步。", "顏色越深，那天走得越多；有圈的是今天。", StatVisual.Heat(ym, days, i.goal, upto - 1))
        }
        StatKind.WEEKDAY -> {
            if (daysWithSteps < 14) return null
            val window = span(i.today.minusDays(83), i.today).toList()
            val avg = DayOfWeek.values().map { dow -> window.filter { it.dayOfWeek == dow }.let { ds -> if (ds.isEmpty()) 0.0 else ds.sumOf { i.history[it] ?: 0L }.toDouble() / ds.size } }
            val best = avg.indices.maxByOrNull { avg[it] }!!
            if (avg[best] <= 0) return null
            StatCard(kind, "${weekdayNames[best]}的你走得最勤", "近 12 週的${weekdayNames[best]}，平均每天 ${steps(avg[best].roundToLong())} 步。", null, StatVisual.Bars(avg.indices.map { weekdayNames[it] to avg[it] }, "步", best))
        }
        StatKind.TOWN_LONGEST -> {
            val passed = i.towns.filter { it.passed }
            val t = passed.maxByOrNull { it.km } ?: return null
            StatCard(kind, t.name, "${t.county}・你在這裡走了 ${nice(t.km)} 公里。", "路過的鄉鎮裡，這裡讓你走得最久。", StatVisual.Bars(passed.sortedByDescending { it.km }.take(5).map { it.name to it.km }, "公里", 0))
        }
        StatKind.TOWN_SHORTEST -> {
            val passed = i.towns.filter { it.passed && it.km > 0.01 }
            if (passed.size < 2) return null
            val t = passed.minByOrNull { it.km }!!
            StatCard(kind, t.name, "${t.county}・只走了 ${nice(t.km)} 公里就穿過去了。", "一不留神就走出去，要不要回頭看看？", StatVisual.Bars(passed.sortedBy { it.km }.take(5).map { it.name to it.km }, "公里", 0))
        }
        StatKind.TOWN_BIGGEST -> {
            if (i.towns.size < 2) return null
            val t = i.towns.maxByOrNull { it.areaKm2 }!!
            StatCard(kind, t.name, "${t.county}・面積約 ${nice(t.areaKm2)} 平方公里。", "你走過的鄉鎮裡，這一個的面積最大。", StatVisual.Bars(i.towns.sortedByDescending { it.areaKm2 }.take(5).map { it.name to it.areaKm2 }, "平方公里", 0))
        }
        StatKind.TOWN_HIGHEST -> {
            val high = i.towns.filter { it.highElevM > 0 }
            if (high.size < 2) return null
            val t = high.maxByOrNull { it.highElevM }!!
            StatCard(kind, t.name, "${t.county}・境內最高處約 ${steps(t.highElevM.toLong())} 公尺。", "你走過的鄉鎮裡，這一個的海拔最高。", StatVisual.Bars(high.sortedByDescending { it.highElevM }.take(5).map { it.name to it.highElevM.toDouble() }, "公尺", 0))
        }
        StatKind.FOOTPRINT -> {
            if (i.towns.isEmpty() || i.townsTotal <= 0) return null
            val counties = i.towns.groupingBy { it.county }.eachCount()
            StatCard(kind, "${i.towns.size} / ${i.townsTotal}", "走過 ${counties.size} 個縣市，${i.towns.size} 個鄉鎮市區。", null, StatVisual.Bars(counties.entries.sortedByDescending { it.value }.take(5).map { it.key to it.value.toDouble() }, "個", 0))
        }
        StatKind.WEEK_VS_LAST -> {
            val monday = i.today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val now = i.sum(monday, i.today)
            val before = i.sum(monday.minusDays(7), i.today.minusDays(7))
            if (before <= 0 || now <= 0) return null
            val diff = now - before
            StatCard(
                kind, if (diff >= 0) "多走 ${steps(diff)} 步" else "少走 ${steps(-diff)} 步",
                "這週到今天 ${steps(now)} 步，上週同一段 ${steps(before)} 步。",
                if (diff >= 0) "腳步輕快，繼續保持。" else "天氣或行程使然，下一段補回來就好。",
                StatVisual.Bars(listOf("本週到今天" to now.toDouble(), "上週同期" to before.toDouble()), "步", 0),
            )
        }
        StatKind.MONTH_VS_LAST -> {
            val first = i.today.withDayOfMonth(1)
            val lastFirst = first.minusMonths(1)
            val n = i.today.dayOfMonth.coerceAtMost(lastFirst.lengthOfMonth())
            val now = i.sum(first, i.today)
            val before = i.sum(lastFirst, lastFirst.withDayOfMonth(n))
            if (before <= 0 || now <= 0) return null
            val pct = ((now - before) * 100.0 / before).roundToInt()
            StatCard(
                kind, if (pct >= 0) "多了 $pct%" else "少了 ${-pct}%",
                "本月到今天 ${steps(now)} 步，上個月同一段 ${steps(before)} 步。", null,
                StatVisual.Bars(listOf("本月到今天" to now.toDouble(), "上月同期" to before.toDouble()), "步", 0),
            )
        }
        StatKind.STREAK -> {
            if (i.goal <= 0) return null
            val (cur, longest) = streaks(i.history, i.goal, i.today)
            if (longest == 0) return null
            StatCard(kind, "$cur 天", if (cur > 0) "連續達成每日目標 ${steps(i.goal.toLong())} 步。" else "今天達標，連續天數就重新開始。", "近一年最長的連續達標是 $longest 天。")
        }
        StatKind.BEST_DAY -> {
            val best = i.history.maxByOrNull { it.value } ?: return null
            if (best.value <= 0) return null
            val d = best.key
            StatCard(kind, "${steps(best.value)} 步", "${d.year} 年 ${d.monthValue} 月 ${d.dayOfMonth} 日（${weekdayNames[d.dayOfWeek.value - 1]}）", "那天約走了 ${nice(best.value * i.strideM / 1000.0)} 公里。")
        }
        StatKind.BEST_MONTH -> {
            val months = i.history.entries.groupBy { YearMonth.from(it.key) }.mapValues { e -> e.value.sumOf { it.value } }.filterValues { it > 0 }
            if (months.size < 2) return null
            val best = months.maxByOrNull { it.value }!!
            StatCard(
                kind, "${best.key.year} 年 ${best.key.monthValue} 月", "這個月走了 ${steps(best.value)} 步，是你走得最勤的一個月。", null,
                StatVisual.Bars(months.entries.sortedByDescending { it.value }.take(5).map { "${it.key.year % 100}/${it.key.monthValue}" to it.value.toDouble() }, "步", 0),
            )
        }
        StatKind.TODAY_VS_AVG -> {
            val now = i.history[i.today] ?: 0L
            val avg = i.sum(i.today.minusDays(30), i.today.minusDays(1)) / 30.0
            if (avg <= 0) return null
            val pct = (now * 100.0 / avg).roundToInt()
            StatCard(kind, "$pct%", "今天 ${steps(now)} 步，近 30 天的日均是 ${steps(avg.roundToLong())} 步。", if (pct >= 100) "今天比平常更有精神。" else "今天還有時間，慢慢走。")
        }
        StatKind.ROUTE_PROGRESS -> {
            if (i.routeTotalKm <= 0) return null
            val pct = (i.walkedKm * 100.0 / i.routeTotalKm).coerceIn(0.0, 100.0)
            val perDay = i.sum(i.today.minusDays(13), i.today) * i.strideM / 1000.0 / 14.0
            val left = (i.routeTotalKm - i.walkedKm).coerceAtLeast(0.0)
            StatCard(
                kind, "${String.format("%.1f", pct)}%", "已走 ${nice(i.walkedKm)} / ${nice(i.routeTotalKm)} 公里，還剩 ${nice(left)} 公里。",
                if (perDay > 0.05 && left > 0) "照近兩週的速度，大約再走 ${nice(left / perDay)} 天。" else null,
            )
        }
        StatKind.CARDS -> {
            if (i.cardsTotal <= 0) return null
            val next = i.nextCard
            StatCard(kind, "${i.cardsOwned} / ${i.cardsTotal}", if (next != null) "下一張：${next.first}，還有約 ${nice(next.second)} 公里。" else "景點卡都收齊了。")
        }
        StatKind.FEATURES -> {
            if (i.towns.isEmpty()) return null
            StatCard(kind, "${i.tagsFound} 項", "在 ${i.towns.size} 個鄉鎮裡找到的特色。", "每個鄉鎮有 1 到 3 項特色，走得夠久才會一一發現。")
        }
        StatKind.MILESTONE -> {
            if (total <= 0) return null
            val marks = listOf(10_000L, 50_000L, 100_000L, 500_000L, 1_000_000L, 5_000_000L, 10_000_000L, 50_000_000L)
            val next = marks.firstOrNull { it > total } ?: return null
            val avg = i.sum(i.today.minusDays(29), i.today) / 30.0
            StatCard(kind, "還差 ${steps(next - total)} 步", "下一個里程碑是累計 ${steps(next)} 步，目前 ${steps(total)} 步。", if (avg > 0) "照近 30 天的日均，大約再走 ${nice((next - total) / avg)} 天。" else null)
        }
        StatKind.FORTUNE -> {
            val title = listOf("大吉", "中吉", "小吉", "吉", "末吉").let { it[rnd.nextInt(it.size)] }
            val poems = listOf(
                "路不遠，走著走著就到了。", "今天的風向，剛好是你要去的方向。", "慢一點也沒關係，景色不會跑掉。",
                "口袋裡的零錢，換得到一杯路邊的好茶。", "轉角的小店，藏著今天的小驚喜。", "鞋帶綁緊，故事就從腳下開始。",
                "別看遠方，先看腳邊的小花。", "雲影走得慢，你也可以。", "再走一站，天色就好看了。", "今天適合繞一點遠路。",
            )
            val good = listOf("繞遠路回家", "跟自己的影子賽跑", "走樓梯", "多喝一口水", "提早一站下車", "把手機放進口袋")
            val bad = listOf("搭電梯", "邊走邊滑手機", "原地發呆太久", "跳過早餐", "抄捷徑錯過風景", "賴在沙發上")
            val lucky = (rnd.nextInt(8) + 1) * 1000 + rnd.nextInt(10) * 100
            StatCard(kind, title, poems[rnd.nextInt(poems.size)], "宜：${good[rnd.nextInt(good.size)]}　忌：${bad[rnd.nextInt(bad.size)]}　幸運步數：約 ${steps(lucky.toLong())}")
        }
    }
}

/** The kinds that have something to show with [i]. */
fun availableStats(i: StatInputs, seed: Long): List<StatKind> = StatKind.values().filter { buildStat(it, i, seed) != null }

/**
 * Today's draw: the same all day for the same [available] cards (so reopening the app shows the same one), different
 * from one day to the next, and never the same kind two days in a row when there is a choice.
 */
fun drawStat(date: LocalDate, available: List<StatKind>): StatKind? {
    if (available.isEmpty()) return null
    fun pick(d: LocalDate) = available[Random(d.toEpochDay() * 7919L + 13).nextInt(available.size)]
    val today = pick(date)
    return if (available.size > 1 && today == pick(date.minusDays(1))) available[(available.indexOf(today) + 1) % available.size] else today
}

/** The seed for a day's variants (which ruler, which fortune). */
fun dayStatSeed(date: LocalDate): Long = date.toEpochDay()

