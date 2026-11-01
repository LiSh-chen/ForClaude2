package com.starmist.walker.data

import com.starmist.core.route.RouteStateCodec
import com.starmist.walker.ui.I18n
import com.starmist.walker.platform.AppServices
import kotlinx.datetime.LocalDate
import org.json.JSONArray
import org.json.JSONObject

/**
 * The backup file: one JSON text holding the steps of every day, the settings, how far the route has got and the township collection. It is written and read by
 * the user's own choice of place; the app sends it nowhere.
 */
object Backup {
    private const val APP = "starmist-walker"
    private const val VERSION = 1

    /** What a restore did: how many days of steps came back. */
    class Restored(val days: Int)

    suspend fun build(c: AppServices, nowMillis: Long): String {
        val s = c.settings.current()
        val root = JSONObject()
        root.put("app", APP).put("version", VERSION).put("exportedAt", nowMillis)
        root.put(
            "settings",
            JSONObject()
                .put("heightCm", s.heightCm).put("weightKg", s.weightKg).put("sex", s.sex.name).put("dailyGoal", s.dailyGoal)
                .put("multiplier", s.multiplier).put("strideOverrideM", s.strideOverrideM ?: JSONObject.NULL)
                .put("stableMode", s.stableMode).put("fontSize", s.fontSize).put("panelStyle", s.panelStyle).put("goalNotify", s.goalNotify),
        )
        val days = JSONArray()
        c.repository.allDays().forEach { days.put(JSONArray().put(it.date).put(it.rawSteps).put(it.scaledSteps).put(it.manualAdjust)) }
        root.put("days", days)
        root.put("route", c.routes.exportRaw() ?: JSONObject.NULL)
        val (log, lap) = c.regions.exportRaw()
        root.put("regionLog", log).put("regionLap", lap)
        return root.toString(1)
    }

    /** Checks [text] and, only if all of it is sound, puts it in place of what is on the phone. Throws [IllegalArgumentException] with a reason otherwise. */
    suspend fun restore(c: AppServices, text: String): Restored {
        val root = try { JSONObject(text) } catch (e: Exception) { throw IllegalArgumentException("這不是備份檔") }
        require(root.optString("app") == APP) { "這不是來台灣走走的備份檔" }
        require(root.optInt("version", 0) in 1..VERSION) { "備份檔的版本太新，請先更新 App" }
        val daysJson = root.optJSONArray("days") ?: throw IllegalArgumentException("備份檔裡沒有步數紀錄")
        val days = ArrayList<DailyStepsEntity>(daysJson.length())
        for (i in 0 until daysJson.length()) {
            val row = daysJson.optJSONArray(i) ?: throw IllegalArgumentException("步數紀錄的格式不對")
            val date = row.optString(0)
            try { LocalDate.parse(date) } catch (e: Exception) { throw IllegalArgumentException(I18n.trf("步數紀錄裡有不正確的日期：{0}", date)) }
            val raw = row.optLong(1); val scaled = row.optLong(2)
            require(raw >= 0 && scaled >= 0) { "步數紀錄裡有負數" }
            days += DailyStepsEntity(date, raw, scaled, row.optLong(3))
        }
        val route = if (root.isNull("route")) null else root.optString("route")
        require(route == null || RouteStateCodec.decode(route) != null) { "路線進度的格式不對" }
        val st = root.optJSONObject("settings") ?: throw IllegalArgumentException("備份檔裡沒有設定")
        // everything is sound: now put it in place
        c.repository.replaceAllDays(days)
        c.routes.importRaw(route)
        c.regions.importRaw(root.optString("regionLog"), root.optInt("regionLap", 0))
        c.settings.update {
            it.copy(
                heightCm = st.optDouble("heightCm", it.heightCm), weightKg = st.optDouble("weightKg", it.weightKg),
                sex = runCatching { com.starmist.core.metrics.Sex.valueOf(st.optString("sex")) }.getOrDefault(it.sex),
                dailyGoal = st.optInt("dailyGoal", it.dailyGoal), multiplier = st.optDouble("multiplier", it.multiplier),
                strideOverrideM = if (st.isNull("strideOverrideM")) null else st.optDouble("strideOverrideM"),
                stableMode = st.optBoolean("stableMode", it.stableMode), fontSize = st.optInt("fontSize", it.fontSize),
                panelStyle = st.optInt("panelStyle", it.panelStyle), goalNotify = st.optBoolean("goalNotify", it.goalNotify), onboarded = true,
            )
        }
        return Restored(days.size)
    }
}
