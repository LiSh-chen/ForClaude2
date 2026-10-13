package com.starmist.core.route

/**
 * The round-island trip: Taipei, clockwise down the east coast (by train across the Su-Hua stretch),
 * through the Huadong valley, over the old South-link road, round the Hengchun peninsula and up the
 * west coast (by ferry across the Tamsui River) back to Taipei.
 *
 * Every card point comes from a source-stated coordinate and its blurb from a checked fact; see
 * android/docs/TAIWAN_LOOP_RESEARCH.md. Plain stops only shape the line.
 */
object TaiwanLoop {
    private fun stop(name: String, lat: Double, lng: Double, transit: String? = null) =
        Stop(name, GeoPoint(lat, lng), transit = transit)

    /** A railway station used as a supply stop; the opening year is from the station's article. */
    private fun supply(id: String, name: String, lat: Double, lng: Double, opened: String) =
        Stop(name + "站", GeoPoint(lat, lng), supply = Supply(id, name + "站", opened))

    private fun card(id: String, name: String, lat: Double, lng: Double, blurb: String) =
        Stop(name, GeoPoint(lat, lng), Landmark(id, name, blurb))

    val route = RouteDef(
        id = "taiwan_loop",
        region = "台灣",
        name = "環島一圈",
        description = "從台北出發，順時針沿東岸南下，穿過花東縱谷、繞過恆春半島，再從西岸北上回到台北。蘇澳到花蓮搭火車、淡水到八里搭渡輪，這兩段不算步數。",
        stops = listOf(
            card("taipei101", "台北101", 25.03361, 121.56444, "高五百零八公尺的高塔"),
            card("keelung", "基隆廟口", 25.12861, 121.74319, "奠濟宮四周擠滿小吃攤"),
            card("jiufen", "九份", 25.1070, 121.8430, "淘金小山城因電影重生"),
            card("fulong", "福隆", 25.02083, 121.94528, "雙溪在這裡流入太平洋"),
            card("sandiaojiao", "三貂角", 25.00694, 122.00111, "位於本島最東端的燈塔"),
            card("toucheng", "頭城老街", 24.85808, 121.82444, "開蘭第一街約六百公尺"),
            card("ncta", "傳統藝術中心", 24.7031, 121.8204, "傳藝中心坐落冬山河畔"),
            card("suao", "蘇澳冷泉", 24.59673, 121.85098, "約二十一度的碳酸冷泉"),
            stop("花蓮", 23.9769, 121.604, transit = "搭火車"),
            card("qixingtan", "七星潭", 24.03289, 121.62794, "新月形海灣鋪滿礫石灘"),
            card("pinegarden", "松園別館", 23.98261, 121.61542, "日治時期軍方的辦公處"),
            supply("fenglin", "鳳林", 23.74639, 121.44689, "1912"),
            card("lintienshan", "林田山", 23.71774, 121.39842, "至今仍保存的林業聚落"),
            supply("guangfu", "光復", 23.66600, 121.42100, "1913"),
            card("ruisui", "瑞穗北回歸線標", 23.4655, 121.3578, "回歸線標一九三三年建"),
            card("yuli", "玉里", 23.33158, 121.31175, "當地招牌小吃是玉里麵"),
            card("chishang", "池上", 23.12417, 121.22083, "池上便當起源於火車站"),
            card("guanshan", "關山", 23.04567, 121.16442, "環鎮單車道長十二公里"),
            card("luye", "鹿野高台", 22.91339, 121.11961, "夏季舉辦熱氣球嘉年華"),
            card("prehistory", "史前博物館", 22.76028, 121.09167, "專門展示臺灣史前文化"),
            card("taimali", "太麻里", 22.61914, 121.00492, "號稱台灣最早的曙光處"),
            supply("jinlun", "金崙", 22.53136, 120.96739, "1992"),
            supply("dawu", "大武", 22.36531, 120.90056, "1992"),
            card("shouka", "壽卡", 22.24528, 120.83556, "南迴公路上最高的山口"),
            card("hengchun", "恆春古城", 22.0027, 120.7447, "四座城門全都完整保留"),
            card("eluanbi", "鵝鑾鼻", 21.9022, 120.8525, "台灣本島最南端的燈塔"),
            card("maobitou", "貓鼻頭", 21.9197, 120.7394, "狀似蹲貓的波浪狀礁岩"),
            card("nmmba", "海生館", 22.0464, 120.6978, "海生館二〇〇〇年成立"),
            supply("fangshan", "枋山", 22.26706, 120.65945, "1992"),
            card("fangliao", "枋寮", 22.3683, 120.5950, "環島鐵路由此全線貫通"),
            card("donggang", "東港", 22.46303, 120.44881, "東港迎王平安祭典主廟"),
            card("pier2", "高雄駁二", 22.61989, 120.28172, "港邊舊倉庫變身藝術區"),
            card("lotuspond", "蓮池潭", 22.6850, 120.2975, "龍虎塔一九七六年落成"),
            card("qiaotou", "橋頭糖廠", 22.75778, 120.31417, "一九〇一年興建的糖廠"),
            card("anping", "安平古堡", 23.0017, 120.1608, "荷蘭人一六二四年起建"),
            card("jingzijiao", "井仔腳鹽田", 23.2589, 120.1076, "現存最古老的瓦盤鹽田"),
            card("budai", "布袋", 23.36, 120.17, "一八九五年日軍登陸處"),
            card("peitian", "朴子配天宮", 23.4679, 120.2452, "朴子供奉媽祖的配天宮"),
            card("shuishang", "水上北回歸線標", 23.4539, 120.4167, "一九〇八年起建的標誌"),
            card("hinoki", "檜意森活村", 23.48619, 120.45411, "林務局宿舍改造的園區"),
            supply("dalin", "大林", 23.60100, 120.45600, "1903"),
            supply("dounan", "斗南", 23.67300, 120.48100, "1903"),
            card("xiluo", "西螺大橋", 23.81286, 120.46092, "長近兩公里的鋼桁大橋"),
            card("lukang", "鹿港老街", 24.0505, 120.4356, "寬約七十公分的摸乳巷"),
            card("roundhouse", "彰化扇形車庫", 24.0856, 120.5400, "一九二二年建成仍運作"),
            card("gaomei", "高美濕地", 24.3121, 120.5497, "風車剪影襯著濕地夕陽"),
            card("dajia", "大甲鎮瀾宮", 24.34522, 120.62358, "九天遶境三百四十公里"),
            card("tongxiao", "通霄精鹽廠", 24.5564, 120.7042, "台鹽通霄的觀光精鹽廠"),
            supply("houlong", "後龍", 24.61600, 120.78700, "1922"),
            supply("zhunan", "竹南", 24.68700, 120.88100, "1902"),
            card("hsinchu", "新竹城隍廟", 24.80437, 120.96606, "一七四八年創建城隍廟"),
            supply("hukou", "湖口", 24.90300, 121.04400, "1893"),
            supply("zhongli", "中壢", 24.95400, 121.22600, "1893"),
            card("taoyuan", "桃園神社", 25.0063, 121.3253, "一九三八年建成的神社"),
            card("shihsanhang", "十三行博物館", 25.1568, 121.4052, "展示十三行遺址的文物"),
            stop("淡水", 25.1696, 121.4422, transit = "搭渡輪"),
            card("dihua", "迪化街", 25.0667, 121.5100, "號稱台北最古老的街道"),
            card("finish", "回到台北", 25.0475, 121.5170, "繞完一圈回到熟悉的家"),
        ),
    )
}
