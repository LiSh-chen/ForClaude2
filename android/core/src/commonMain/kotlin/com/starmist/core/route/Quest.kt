package com.starmist.core.route

/**
 * A task to complete. It asks for景點卡 ([cards]) and/or townships ([towns], keys "county|name").
 * An empty set means "any": the quest then only counts how many ([needCards] / [needTowns]) have been collected overall.
 * [reward] is a hook for later: null means no reward is defined yet.
 */
data class Quest(
    val id: String,
    val group: String,
    val title: String,
    val desc: String,
    val cards: Set<String> = emptySet(),
    val towns: Set<String> = emptySet(),
    val needCards: Int = cards.size,
    val needTowns: Int = towns.size,
    val reward: String? = null,
)

data class QuestProgress(val have: Int, val need: Int) {
    val done: Boolean get() = need > 0 && have >= need
}

object QuestEngine {
    /** How far [quest] is, given everything collected so far (across routes and laps). */
    fun progress(quest: Quest, cards: Set<String>, towns: Set<String>): QuestProgress {
        val c = if (quest.cards.isEmpty()) cards.size else quest.cards.count { it in cards }
        val t = if (quest.towns.isEmpty()) towns.size else quest.towns.count { it in towns }
        return QuestProgress(minOf(c, quest.needCards) + minOf(t, quest.needTowns), quest.needCards + quest.needTowns)
    }

    /** The quests that are complete now but are not in [alreadyDone], smallest first. */
    fun newlyDone(quests: List<Quest>, alreadyDone: Set<String>, cards: Set<String>, towns: Set<String>): List<Quest> =
        quests.filter { it.id !in alreadyDone && progress(it, cards, towns).done }
            .sortedBy { progress(it, cards, towns).need }
}
