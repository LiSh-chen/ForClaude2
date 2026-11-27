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
            "xiaohe", "小禾", "農學系學生",
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

    /** Kilometres to walk after sending a postcard to a friend before the friend's answer comes. */
    const val REPLY_KM = 20.0

    /** The friendship points at which a friend tells one more of their own stories. */
    val STORY_AT = listOf(3, 6, 10)

    /** Small stories a friend tells about themselves, one for each of [STORY_AT]. */
    fun stories(id: String): List<String> = when (id) {
        "xipo" -> listOf(
            "年輕時我第一次出海，吐得比魚還多。老船長只說：看遠處，別看浪。到現在我走路累了，也還是這樣告訴自己。",
            "我的羅盤是老船長送的，玻璃裂了一道，我捨不得換。每次迷路，它還是指得比手機準。",
            "退休那天我把制服洗乾淨收進箱子，結果隔天又穿出門散步。你說，海這種東西，是不是戒不掉？",
        )
        "ahbo" -> listOf(
            "我拍過最好的一張照片，是等了三個早上的結果。前兩天都是霧，但我沒白等，因為霧裡的山有另一種樣子。",
            "有次為了拍雲海，我天沒亮就上山，結果忘了帶腳架。只好把相機架在背包上，反而拍出很穩的一張。",
            "我的第一台相機是二手的，快門會卡。後來我習慣多按一次，現在換了新相機，還是會多按一次。",
        )
        "xiaohe" -> listOf(
            "我騎單車看田，最怕下雨天的泥巴路。有一次連人帶車滑進水溝，農夫伯伯反而笑著把我拉起來。",
            "我在筆記本上畫過二十幾種稻穗，每一種都不太一樣。老師說我畫得比寫報告認真，我覺得這是稱讚。",
            "有個阿嬤塞給我一袋剛採的菜，說「學生仔，吃飽才有力氣騎車」。我到現在還記得那個味道。",
        )
        "atie" -> listOf(
            "我第一次追火車是小學，站在平交道旁等了兩個小時，就為了看那一班。看到的時候，我整個人都在發抖。",
            "我有個小本子，記每一班列車的車號。同伴笑我像在集郵，我說差不多啦，只是我的郵票會跑。",
            "有一次錯過末班車，我在小站的長椅睡了一晚。早上第一班車進站的聲音，比任何鬧鐘都好聽。",
        )
        "momo" -> listOf(
            "我的速寫本第一頁是一棵歪歪的樹。我一直沒撕掉它，因為每次翻到，就想起自己為什麼開始畫。",
            "畫畫最難的不是畫像，是畫「感覺」。有次我畫了一整個下午的海，最後只留下三條線，但那三條線最像。",
            "我把你走過的路，一段一段記在本子最後幾頁。等你走完，我想把它們拼成一張很長的畫送你。",
        )
        else -> emptyList()
    }

    /** Three things to write on a postcard to this friend, each in the way you would write to them. */
    fun cheers(id: String): List<String> = when (id) {
        "xipo" -> listOf("船長，我又走了一段，腳還硬朗！", "今天風不大，適合慢慢走。", "沿路的海味我都記著，等你回信講講你那邊的。")
        "ahbo" -> listOf("光很好，我邊走邊想起你的相機。", "這裡的雲有點意思，下次你來拍。", "我走到這裡了，你那邊的山好嗎？")
        "xiaohe" -> listOf("路邊的田我都有看，有幾塊長得特別好！", "騎車慢一點，我用走的也不輸你。", "我走到這裡了，你那邊的稻子黃了嗎？")
        "atie" -> listOf("剛剛有火車從旁邊開過去，我第一個想到你！", "我走到這裡了，這裡有沒有車站可以逛？", "等你回信，順便說說你最近追到哪一班車。")
        "momo" -> listOf("我停下來看了很久，這裡的樣子你一定想畫。", "你的速寫本又多了幾頁吧？寄來給我看。", "我走到這裡了，這張明信片就當作我的速寫。")
        else -> emptyList()
    }

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
