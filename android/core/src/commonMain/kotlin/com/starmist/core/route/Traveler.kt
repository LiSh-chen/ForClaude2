package com.starmist.core.route

/** Where on a route a traveler is met: the card whose place it is, and which meeting it is (1 = getting to know, 2 = becoming friends). */
data class Meeting(val cardId: String, val stage: Int)

/** A fellow traveler met on the road who, once a friend, sends postcards from places the route does not go. */
data class Traveler(
    val id: String,
    val name: String,
    /** What they do, in a few words. */
    val job: String,
    /** What they say at the first meeting and at the second. */
    val hello: String,
    val friend: String,
    /** The meetings on each route, by route id; both are in the first half of the way. */
    val meetings: Map<String, List<Meeting>>,
)

sealed interface TravelerEvent {
    val travelerId: String
    val atKm: Double
    data class Met(override val travelerId: String, override val atKm: Double) : TravelerEvent
    data class BecameFriend(override val travelerId: String, override val atKm: Double) : TravelerEvent
    /** A postcard is due; [key] identifies it (traveler, route and number) so that it is only sent once. */
    data class MailDue(override val travelerId: String, override val atKm: Double, val key: String) : TravelerEvent
}

object Travelers {
    const val FORWARD = "taiwan_loop"
    const val REVERSE = "taiwan_loop_ccw"

    /** Kilometres of walking between one postcard of a friend and the next. */
    const val MAIL_EVERY_KM = 150.0

    val all: List<Traveler> = listOf(
        Traveler(
            "ahbo", "阿柏", "攝影師",
            "我在等光。你也是在趕路嗎？我叫阿柏，專門拍山和雲。",
            "又遇到你了！路上能碰到第二次，就是朋友。以後我去哪裡拍照，就寄明信片給你。",
            mapOf(
                FORWARD to listOf(Meeting("sandiaojiao", 1), Meeting("luye", 2)),
                REVERSE to listOf(Meeting("taoyuan", 1), Meeting("dajia", 2)),
            ),
        ),
        Traveler(
            "xiaohe", "小禾", "騎單車的農學系學生",
            "嗨！我騎單車一路看田。你走路的話，應該會看到更多細節吧？",
            "我們又碰面了！這次換我說：我要去一些你不會經過的地方，到了就寫信給你。",
            mapOf(
                FORWARD to listOf(Meeting("ruisui", 1), Meeting("guanshan", 2)),
                REVERSE to listOf(Meeting("xiluo", 1), Meeting("peitian", 2)),
            ),
        ),
        Traveler(
            "xipo", "汐婆", "退休的船長",
            "年輕人，走這麼遠啊？我跑過不少港口，現在換成用腳看世界。",
            "第二次遇見，就是自己人啦。我到了新地方，會寄張明信片告訴你。",
            mapOf(
                FORWARD to listOf(Meeting("keelung", 1), Meeting("qixingtan", 2)),
                REVERSE to listOf(Meeting("tongxiao", 1), Meeting("gaomei", 2)),
            ),
        ),
        Traveler(
            "atie", "阿鐵", "鐵道迷",
            "你聽，遠處有火車！我專門追著鐵道跑，你呢？",
            "又在鐵道旁遇到你，這就是緣分！我去到你的路線外，會寄明信片給你。",
            mapOf(
                FORWARD to listOf(Meeting("lintienshan", 1), Meeting("chishang", 2)),
                REVERSE to listOf(Meeting("roundhouse", 1), Meeting("hinoki", 2)),
            ),
        ),
        Traveler(
            "momo", "墨墨", "拿速寫本的畫家",
            "別動，這個角度很好看……啊抱歉，我在畫畫。我叫墨墨。",
            "第二次遇到，我已經把你畫進本子裡了。之後去哪裡畫畫，就寄給你看。",
            mapOf(
                FORWARD to listOf(Meeting("toucheng", 1), Meeting("pinegarden", 2)),
                REVERSE to listOf(Meeting("dihua", 1), Meeting("hsinchu", 2)),
            ),
        ),
    )

    fun byId(id: String): Traveler? = all.firstOrNull { it.id == id }

    /** Meetings on [route] with the kilometre each happens at, in order of walking. */
    fun meetingsOn(route: RouteDef): List<Triple<Traveler, Meeting, Double>> {
        val kmOf = route.cards.associate { it.first.id to it.second }
        return all.flatMap { t -> t.meetings[route.id].orEmpty().mapNotNull { m -> kmOf[m.cardId]?.let { Triple(t, m, it) } } }
            .sortedBy { it.third }
    }

    /**
     * What has happened on [route] up to [walkedKm] that is not yet in [met] / [friends] / [mailed]: meetings in order, then the
     * postcards due, one every [MAIL_EVERY_KM] after the traveler became a friend on this route.
     */
    fun due(route: RouteDef, walkedKm: Double, met: Set<String>, friends: Set<String>, mailed: Set<String>): List<TravelerEvent> {
        val out = ArrayList<TravelerEvent>()
        val meetNow = HashSet(met)
        val friendNow = HashSet(friends)
        val friendKm = HashMap<String, Double>()
        for ((t, m, km) in meetingsOn(route)) {
            if (m.stage == 2) friendKm[t.id] = km
            if (km > walkedKm + 1e-9) continue
            if (m.stage == 1 && t.id !in meetNow) { meetNow += t.id; out += TravelerEvent.Met(t.id, km) }
            if (m.stage == 2 && t.id in meetNow && t.id !in friendNow) { friendNow += t.id; out += TravelerEvent.BecameFriend(t.id, km) }
        }
        val reach = minOf(walkedKm, route.totalKm)
        for ((id, fk) in friendKm) {
            if (id !in friendNow) continue
            var k = 1
            while (fk + k * MAIL_EVERY_KM <= reach + 1e-9) {
                val key = "$id|${route.id}|$k"
                if (key !in mailed) out += TravelerEvent.MailDue(id, fk + k * MAIL_EVERY_KM, key)
                k++
            }
        }
        return out.sortedBy { it.atKm }
    }
}
