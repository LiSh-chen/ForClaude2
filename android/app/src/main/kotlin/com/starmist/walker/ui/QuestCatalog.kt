package com.starmist.walker.ui

import com.starmist.core.route.Quest
import com.starmist.core.route.Routes

/** The list of tasks, built from the cards on the routes and the townships that have features. Rewards are not set yet (null). */
object QuestCatalog {
    private val REGIONS = listOf(
        "北台灣" to listOf("基隆市", "臺北市", "新北市", "桃園市", "新竹市", "新竹縣"),
        "中台灣" to listOf("苗栗縣", "臺中市", "彰化縣", "南投縣", "雲林縣"),
        "南台灣" to listOf("嘉義市", "嘉義縣", "臺南市", "高雄市", "屏東縣"),
        "東台灣" to listOf("宜蘭縣", "花蓮縣", "臺東縣"),
    )

    /** Hand-picked sets of cards that go together (ids from the card list). */
    private val COMBOS = listOf(
        Triple("north_classics", "北台灣經典", listOf("taipei101", "keelung", "jiufen", "fulong")),
        Triple("tropic", "跨越北回歸線", listOf("ruisui", "shuishang")),
        Triple("salt_sugar", "鹽與糖的故事", listOf("jingzijiao", "tongxiao", "qiaotou")),
        Triple("southern_tip", "國境之南", listOf("eluanbi", "maobitou", "hengchun", "checheng")),
        Triple("rift_valley", "花東縱谷", listOf("ruisui", "yuli", "chishang", "guanshan", "luye")),
        Triple("temples", "廟宇巡禮", listOf("dajia", "peitian", "checheng", "hsinchu")),
        Triple("harbors", "港口小鎮", listOf("keelung", "donggang", "budai", "fangliao")),
        Triple("old_streets", "老街漫步", listOf("dihua", "lukang", "toucheng", "jiufen")),
    )

    fun build(areas: TownAreas, blurbs: Map<String, List<TownTag>>): List<Quest> {
        val allCards = Routes.all.flatMap { r -> r.cards.map { it.first.id } }.toSet()
        val cardsBy = areas.cardPlaces.entries.filter { it.key in allCards }.groupBy({ it.value.county }, { it.key }).mapValues { it.value.toSet() }
        val townsBy = blurbs.keys.groupBy({ it.substringBefore('|') }).mapValues { e -> e.value.toSet() }
        val out = ArrayList<Quest>()

        // milestones: how many in all
        val totalCards = allCards.size
        val totalTowns = townsBy.values.sumOf { it.size }
        listOf(5, 10, 20, 30).filter { it < totalCards }.forEach {
            out += Quest("ms_cards_$it", "里程碑", "收集 $it 張景點卡", "累積收集 $it 張景點卡（跨路線、跨圈數都算）", needCards = it)
        }
        out += Quest("ms_cards_all", "里程碑", "景點卡全收集", "集滿全部 $totalCards 張景點卡", cards = allCards, postcards = 3)
        listOf(10, 30, 60).filter { it < totalTowns }.forEach {
            out += Quest("ms_towns_$it", "里程碑", "走過 $it 個鄉鎮區", "累積走過 $it 個鄉鎮區", needTowns = it)
        }

        // one county
        cardsBy.entries.filter { it.value.size >= 2 }.sortedBy { it.key }.forEach { (county, ids) ->
            out += Quest("cards:$county", "縣市・景點", "$county 景點收集", "集滿 $county 的 ${ids.size} 張景點卡", cards = ids)
        }
        townsBy.entries.filter { it.value.size >= 2 }.sortedBy { it.key }.forEach { (county, keys) ->
            out += Quest("towns:$county", "縣市・足跡", "走遍$county", "走過 $county 的 ${keys.size} 個鄉鎮區", towns = keys)
        }

        // a part of the island
        REGIONS.forEach { (name, counties) ->
            val cards = counties.flatMap { cardsBy[it].orEmpty() }.toSet()
            val towns = counties.flatMap { townsBy[it].orEmpty() }.toSet()
            val names = counties.joinToString("、")
            if (cards.size >= 2) out += Quest("rcards:$name", "區域", "$name 景點大滿貫", "集滿$name（$names）所有景點卡", cards = cards, postcards = 2)
            if (towns.size >= 2) out += Quest("rtowns:$name", "區域", "$name 足跡遍布", "走遍$name（$names）所有有特色的鄉鎮區", towns = towns, postcards = 2)
        }

        // themed sets of cards (a set is dropped if one of its cards is not on any route)
        COMBOS.filter { (_, _, ids) -> allCards.containsAll(ids) }.forEach { (id, title, ids) ->
            val names = ids.joinToString("、") { cardName(it) }
            out += Quest("combo:$id", "主題", title, "集齊：$names", cards = ids.toSet(), postcards = 2)
        }
        return out
    }

    private fun cardName(id: String): String = Routes.all.firstNotNullOfOrNull { r -> r.cards.firstOrNull { it.first.id == id }?.first?.name } ?: id
}
