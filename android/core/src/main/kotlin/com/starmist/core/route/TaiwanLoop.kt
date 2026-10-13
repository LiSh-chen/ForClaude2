package com.starmist.core.route

/** The round-island trip: Taipei, clockwise down the east coast, up the west coast and home. */
object TaiwanLoop {
    private fun stop(name: String, lat: Double, lng: Double) = Stop(name, GeoPoint(lat, lng))

    private fun card(id: String, name: String, lat: Double, lng: Double, blurb: String) =
        Stop(name, GeoPoint(lat, lng), Landmark(id, name, blurb))

    val route = RouteDef(
        id = "taiwan_loop",
        region = "台灣",
        name = "環島一圈",
        description = "從台北出發，順時針沿東岸南下、繞過南端，再從西岸北上回到台北。",
        stops = listOf(
            card("taipei101", "台北101", 25.0339, 121.5645, "竹節高塔守望整座盆地"),
            stop("基隆", 25.1276, 121.7392),
            card("jiufen", "九份", 25.1092, 121.8442, "山城石階燈籠映照海景"),
            card("fulong", "福隆", 25.0194, 121.9458, "綿長沙灘與鐵路便當香"),
            card("sandiaojiao", "三貂角", 25.0003, 122.0053, "本島最東端的白色燈塔"),
            stop("頭城", 24.8590, 121.8236),
            card("jiaoxi", "礁溪", 24.8277, 121.7739, "溫泉暖身蘭陽平原入口"),
            stop("宜蘭", 24.7570, 121.7530),
            stop("羅東", 24.6770, 121.7690),
            card("suao", "蘇澳", 24.5960, 121.8590, "冷泉冒著氣泡的海港城"),
            stop("南澳", 24.4630, 121.8000),
            stop("和平", 24.3040, 121.7550),
            card("qingshui", "清水斷崖", 24.2150, 121.6900, "斷崖垂直插入蔚藍海洋"),
            card("taroko", "太魯閣", 24.1580, 121.6200, "大理石峽谷鑿穿整座山"),
            card("qixingtan", "七星潭", 24.0010, 121.6310, "弧形礫石海灘海天相連"),
            stop("花蓮", 23.9870, 121.6010),
            stop("鳳林", 23.7450, 121.4510),
            stop("瑞穗", 23.4960, 121.3700),
            stop("玉里", 23.3350, 121.3130),
            card("chishang", "池上", 23.1250, 121.2190, "稻浪無邊米香飄滿山谷"),
            card("luye", "鹿野", 22.9120, 121.1290, "高台草原夏天放熱氣球"),
            card("taitung", "台東", 22.7583, 121.1444, "鐵花村裡夜夜歌聲不斷"),
            stop("知本", 22.7000, 121.0300),
            card("taimali", "太麻里", 22.6150, 121.0000, "金針花海襯著藍色海洋"),
            stop("大武", 22.3560, 120.8960),
            card("nanhui", "南迴公路", 22.2900, 120.8500, "南迴公路穿越山海之間"),
            stop("楓港", 22.2000, 120.7100),
            card("hengchun", "恆春", 22.0020, 120.7440, "完整古城牆圍繞著小鎮"),
            card("eluanbi", "鵝鑾鼻", 21.9020, 120.8530, "海角盡頭那座白色燈塔"),
            stop("車城", 22.0770, 120.7090),
            stop("枋寮", 22.3660, 120.5940),
            stop("潮州", 22.5500, 120.5400),
            card("kaohsiung", "高雄駁二", 22.6200, 120.2810, "舊倉庫裡長出藝術花朵"),
            card("anping", "安平古堡", 22.9996, 120.1606, "荷蘭人所建的古老城堡"),
            stop("嘉義", 23.4800, 120.4500),
            card("lukang", "鹿港", 24.0570, 120.4350, "九曲巷弄裡藏著老手藝"),
            card("gaomei", "高美濕地", 24.3100, 120.5500, "風車與夕陽倒映在濕地"),
            stop("苗栗", 24.5600, 120.8200),
            card("hsinchu", "新竹", 24.8050, 120.9680, "九降風吹乾了香甜米粉"),
            stop("桃園", 24.9900, 121.3100),
            card("finish", "回到台北", 25.0478, 121.5170, "繞完一圈回到熟悉的家"),
        ),
    )
}
