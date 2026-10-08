package com.starmist.core.world

/** A stretch of the route. Items are rolled from [lootWeights]; [keyItem] is the one the region's shrine needs. */
data class Region(
    val id: String,
    val name: String,
    val lengthSteps: Long,
    val keyItem: ItemType,
    val lootWeights: Map<ItemType, Int>,
    val foundLines: List<String>,
    val clearedText: String,
)

data class ChoiceOption(val label: String, val result: String, val bonus: Reward)

/** A small story card with two harmless choices; there is no wrong answer. */
data class EncounterDef(
    val id: String,
    val regionId: String,
    val title: String,
    val text: String,
    val a: ChoiceOption,
    val b: ChoiceOption,
)

/** Content of the prologue that exists so far (the first two regions). */
object Prologue {
    val forest = Region(
        id = "whispering_forest",
        name = "低語森林",
        lengthSteps = 90_000,
        keyItem = ItemType.LIFE_SEED,
        lootWeights = mapOf(
            ItemType.LIFE_SEED to 45,
            ItemType.MOON_DEW to 35,
            ItemType.RUNE_SHARD to 12,
        ),
        foundLines = listOf(
            "一陣風帶來樹葉的低語，你在路邊撿到了東西。",
            "苔蘚底下有什麼在發光，你伸手拾起。",
            "螢光蘑菇圍成一圈，圈中放著一份小小的禮物。",
            "樹根之間躺著一件被遺忘的小東西。",
        ),
        clearedText = "森林的低語漸漸變成了歌聲。星霧在這裡退去了。",
    )

    val coast = Region(
        id = "moonlit_coast",
        name = "月光海岸",
        lengthSteps = 100_000,
        keyItem = ItemType.TIDE_PEARL,
        lootWeights = mapOf(
            ItemType.TIDE_PEARL to 45,
            ItemType.MOON_DEW to 35,
            ItemType.RUNE_SHARD to 12,
            ItemType.LIFE_SEED to 8,
        ),
        foundLines = listOf(
            "浪花退去，沙灘上留下一點閃光。",
            "月光落在水面，一件東西隨著波浪漂到你腳邊。",
            "貝殼的縫隙裡藏著什麼，你輕輕取出。",
            "海風吹開一叢海草，下面有個小小的驚喜。",
        ),
        clearedText = "潮汐重新有了節奏。海岸的星霧散去，月光照亮遠方。",
    )

    val regions: List<Region> = listOf(forest, coast)

    val encounters: List<EncounterDef> = listOf(
        EncounterDef(
            "forest_fawn", forest.id, "受傷的小鹿",
            "溪邊有隻鹿角精靈的小鹿，被藤蔓纏住了腳。",
            ChoiceOption("幫牠解開藤蔓", "小鹿用鹿角輕輕碰了碰你，留下一點光。", Reward(ItemType.MOON_DEW)),
            ChoiceOption("留下水和乾糧", "小鹿安心地喝了水，朝森林深處走去。", Reward(ItemType.LIFE_SEED)),
        ),
        EncounterDef(
            "forest_riddle", forest.id, "苔靈的謎語",
            "一團苔靈攔住了路：「什麼東西越走越長，卻不會累？」",
            ChoiceOption("路", "苔靈開心地轉圈，送你一顆種子。", Reward(ItemType.LIFE_SEED)),
            ChoiceOption("影子", "苔靈笑說這個答案也不錯，從背後掏出一片發光的碎片。", Reward(ItemType.RUNE_SHARD)),
        ),
        EncounterDef(
            "forest_mushrooms", forest.id, "發光蘑菇圈",
            "蘑菇圍成了一個圓圈，微微亮著。",
            ChoiceOption("繞過去", "蘑菇們輕輕搖晃，像是在道謝。", Reward(ItemType.MOON_DEW)),
            ChoiceOption("走進去看看", "你聽見遠處古老的低語，腳下的苔蘚亮了一下。", Reward(ItemType.RUNE_SHARD)),
        ),
        EncounterDef(
            "forest_firefly", forest.id, "迷路的螢火蟲",
            "一隻螢火蟲在你面前繞圈，好像找不到回家的路。",
            ChoiceOption("讓牠停在肩上", "牠陪你走了一小段，留下一滴光。", Reward(ItemType.MOON_DEW)),
            ChoiceOption("為牠指路", "螢火蟲飛向樹梢，落下一顆發亮的種子。", Reward(ItemType.LIFE_SEED)),
        ),
        EncounterDef(
            "forest_oldtree", forest.id, "倒下的古樹",
            "一棵很老的樹倒在路中央，樹皮上刻著模糊的紋路。",
            ChoiceOption("在樹下休息一會", "樹洞裡滴下清涼的露水。", Reward(ItemType.MOON_DEW)),
            ChoiceOption("撿起樹皮上的符文", "紋路離開樹皮，變成一片溫熱的碎片。", Reward(ItemType.RUNE_SHARD)),
        ),
        EncounterDef(
            "coast_whale", coast.id, "鯨靈的歌聲",
            "遠處的海面上傳來低沉的歌聲，是潮汐鯨靈。",
            ChoiceOption("安靜聆聽", "歌聲結束時，一顆珍珠被浪送到你手上。", Reward(ItemType.TIDE_PEARL)),
            ChoiceOption("輕輕和聲", "鯨靈高興地噴出一道水霧，凝成月光露。", Reward(ItemType.MOON_DEW)),
        ),
        EncounterDef(
            "coast_crab", coast.id, "寄居蟹商人",
            "一隻背著大貝殼的寄居蟹攤開了小攤子。",
            ChoiceOption("用一首歌交換", "寄居蟹聽得入迷，送你一顆潮汐珠。", Reward(ItemType.TIDE_PEARL)),
            ChoiceOption("幫牠搬貝殼", "寄居蟹感激地塞給你一片刻著符文的碎片。", Reward(ItemType.RUNE_SHARD)),
        ),
        EncounterDef(
            "coast_jellyfish", coast.id, "月光水母",
            "淺灘上漂著幾隻透明的水母，像一盞盞小燈。",
            ChoiceOption("伸手碰碰", "指尖沾上了一點溫柔的光。", Reward(ItemType.MOON_DEW)),
            ChoiceOption("靜靜看牠們漂走", "水母散去後，水面留下一顆珍珠。", Reward(ItemType.TIDE_PEARL)),
        ),
        EncounterDef(
            "coast_bottle", coast.id, "漂流瓶",
            "一個舊玻璃瓶隨著浪漂到岸邊，瓶口封著蠟。",
            ChoiceOption("打開看看", "瓶裡沒有信，只有一片會發光的碎片。", Reward(ItemType.RUNE_SHARD)),
            ChoiceOption("放回海裡", "瓶子漂遠了，浪花留下一顆珍珠當作回禮。", Reward(ItemType.TIDE_PEARL)),
        ),
        EncounterDef(
            "coast_lowtide", coast.id, "退潮的沙灘",
            "潮水退得很遠，露出一整片閃著濕光的沙灘。",
            ChoiceOption("撿貝殼", "你拾到一顆圓潤的珍珠。", Reward(ItemType.TIDE_PEARL)),
            ChoiceOption("在沙上寫下名字", "浪花捲走了名字，留下一點月光。", Reward(ItemType.MOON_DEW)),
        ),
    )
}
