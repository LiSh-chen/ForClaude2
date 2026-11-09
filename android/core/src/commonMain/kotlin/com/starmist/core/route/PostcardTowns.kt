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
    fun choose(travelerId: String, candidates: List<TownProfile>, seed: Int, preferred: Set<String> = emptySet()): String? {
        if (candidates.isEmpty()) return null
        // a place the traveler has something researched to say about comes first
        val pool = candidates.filter { it.key in preferred }.ifEmpty { candidates }
        val best = pool.sortedWith(compareBy({ score(travelerId, it) }, { it.key })).take(6)
        return best[((seed % best.size) + best.size) % best.size].key
    }

    /** A closing line with the township's figures, in each traveler's own way of talking. */
    private fun figures(t: Traveler, p: TownProfile, area: String): String = when (t.id) {
        "ahbo" -> "約 $area 平方公里，${p.villages} 個村里。我慢慢走。"
        "xiaohe" -> "這裡有 ${p.villages} 個村里，約 $area 平方公里，我要一個一個騎過去！"
        "xipo" -> "約 $area 平方公里，${p.villages} 個村里，你自己看地圖吧。"
        "atie" -> "約 $area 平方公里，${p.villages} 個村里，我的筆記本又多了一頁！"
        "momo" -> "約 $area 平方公里，${p.villages} 個村里，我想把它們都畫下來。"
        else -> "約 $area 平方公里，${p.villages} 個村里。"
    }

    /**
     * The postcard text: [fact] (researched, told in the traveler's own words) with the township's figures, or the traveler's
     * general voice when there is none. Each traveler talks differently: ahbo calm and short, xiaohe bright, xipo plain and teasing,
     * atie talkative and excited, momo soft and observing.
     */
    fun lines(t: Traveler, p: TownProfile, fact: String? = null): List<String> {
        val where = "${p.county}${p.name}"
        val area = kotlin.math.round(p.areaKm2).toLong().toString()
        if (fact != null) return listOf(fact, figures(t, p, area), "—— ${t.name}")
        val body = when (t.id) {
            "ahbo" -> listOf(
                "今天走到$where，最高的地方海拔約 ${p.highM} 公尺。我停下來，等了一下光。",
                when {
                    p.highM >= 2500 -> "這個高度，連呼吸都變得很認真。"
                    p.highM >= 1000 -> "山路一直往上，背包越來越重了。"
                    else -> "沒有想像中高，但值得拍。"
                },
            )
            "xiaohe" -> listOf(
                "欸！我騎到$where 了！這裡有 ${p.villages} 個村里，面積約 $area 平方公里。",
                if (p.medianM <= 30) "地勢平緩，騎起來超輕鬆～" else "有點坡度，我整個人都在冒汗啦。",
            )
            "xipo" -> listOf(
                "少年仔，我到$where 了。面積約 $area 平方公里，共 ${p.villages} 個村里。",
                "出門在外，有空就看看地圖，也看看人。",
            )
            "atie" -> listOf(
                "你聽我說！我走到$where 了，海拔中位數約 ${p.medianM} 公尺。",
                "不知道附近有沒有火車，我先把耳朵豎起來聽聽！",
            )
            "momo" -> listOf(
                "我在$where 翻開了新的一頁。這裡有 ${p.villages} 個村里，面積約 $area 平方公里。",
                "我想把每一個村里都畫下來。",
            )
            else -> listOf("我到了$where。")
        }
        return body + "—— ${t.name}"
    }
}
