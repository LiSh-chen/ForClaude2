package com.starmist.core.route

/** The figures known about a township the route does not go through. */
data class TownProfile(val key: String, val county: String, val name: String, val highM: Int, val medianM: Int, val areaKm2: Double, val villages: Int)

/**
 * Which place a traveler writes from, and what they say. Only the township's own figures are used as facts; the rest is the
 * traveler's voice (feelings and habits), so nothing unchecked is claimed about a place.
 */
object PostcardTowns {
    /** How much each traveler likes a place (lower is more): their taste, from the figures we have. */
    private fun score(travelerId: String, p: TownProfile): Double = when (travelerId) {
        "ahbo" -> -p.highM.toDouble()
        "xiaohe" -> p.medianM.toDouble()
        "xipo" -> p.medianM + p.areaKm2 / 10.0
        "atie" -> kotlin.math.abs(p.medianM - 150).toDouble()
        "momo" -> -p.villages.toDouble()
        else -> 0.0
    }

    /** One of the places [travelerId] likes best among [candidates] (townships not yet written from); [seed] varies the pick. */
    fun choose(travelerId: String, candidates: List<TownProfile>, seed: Int): String? {
        if (candidates.isEmpty()) return null
        val best = candidates.sortedWith(compareBy({ score(travelerId, it) }, { it.key })).take(6)
        return best[((seed % best.size) + best.size) % best.size].key
    }

    fun lines(t: Traveler, p: TownProfile): List<String> {
        val where = "${p.county}${p.name}"
        val area = "%.0f".format(p.areaKm2)
        val body = when (t.id) {
            "ahbo" -> listOf(
                "今天來到$where。最高的地方海拔約 ${p.highM} 公尺，我停下來拍了好多張。",
                when {
                    p.highM >= 2500 -> "這個高度，連呼吸都變得很認真。"
                    p.highM >= 1000 -> "山路一直往上，背包越來越重了。"
                    else -> "沒有想像中高，但值得拍。"
                },
            )
            "xiaohe" -> listOf(
                "我騎到$where 啦！這裡有 ${p.villages} 個村里，面積約 $area 平方公里。",
                if (p.medianM <= 30) "地勢平緩，單車騎起來很輕鬆。" else "有些坡度，單車得用力踩。",
            )
            "xipo" -> listOf(
                "我到了$where。面積約 $area 平方公里，共 ${p.villages} 個村里。",
                "年輕人，除了看地圖，也要記得看看人。",
            )
            "atie" -> listOf(
                "今天走到$where，海拔中位數約 ${p.medianM} 公尺。",
                "不知道這附近有沒有火車，我先豎起耳朵聽聽。",
            )
            "momo" -> listOf(
                "我在$where 翻開速寫本。這裡有 ${p.villages} 個村里，面積約 $area 平方公里。",
                "我想把每一個村里都畫下來。",
            )
            else -> listOf("我到了$where。")
        }
        return body + "—— ${t.name}"
    }
}
