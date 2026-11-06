package com.starmist.walker.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Everything ever collected (kept across routes and laps), the quests already celebrated, the travelers met and the postcards they sent. */
data class Ledger(
    val cards: Set<String> = emptySet(),
    val towns: Set<String> = emptySet(),
    val done: Set<String> = emptySet(),
    val seeded: Boolean = false,
    /** Off-route townships a postcard has come from. */
    val unlocked: Set<String> = emptySet(),
    /** Postcards earned by quests in all: each can be used to ask a friend to write. */
    val earned: Int = 0,
    val met: Set<String> = emptySet(),
    val friends: Set<String> = emptySet(),
    /** Keys of the scheduled postcards already sent (see Travelers.due). */
    val mailed: Set<String> = emptySet(),
    /** Who sent the postcard of each township (township key to traveler id). */
    val senders: Map<String, String> = emptyMap(),
    /** How many quest postcards have been used. */
    val used: Int = 0,
) {
    /** Quest postcards not yet used. */
    val postcardsLeft: Int get() = (earned - used).coerceAtLeast(0)
}

/** The lifetime ledger the quests and travelers are measured against; a route only knows its own progress, this one never forgets. */
class CollectionStore(private val store: DataStore<Preferences>) {
    private val cardsKey = stringPreferencesKey("cards")
    private val townsKey = stringPreferencesKey("towns")
    private val doneKey = stringPreferencesKey("done")
    private val seededKey = booleanPreferencesKey("seeded")
    private val unlockedKey = stringPreferencesKey("unlocked")
    private val earnedKey = intPreferencesKey("earned")
    private val metKey = stringPreferencesKey("met")
    private val friendsKey = stringPreferencesKey("friends")
    private val mailedKey = stringPreferencesKey("mailed")
    private val sendersKey = stringPreferencesKey("senders")
    private val usedKey = intPreferencesKey("used")

    val flow: Flow<Ledger> = store.data.map { read(it) }

    suspend fun current(): Ledger = flow.first()

    private suspend fun change(f: (Ledger) -> Ledger): Ledger {
        var out = Ledger()
        store.edit { out = f(read(it)); write(it, out) }
        return out
    }

    /** Adds what was collected; returns the ledger as it is afterwards. Safe to call again with the same things. */
    suspend fun add(cards: Collection<String>, towns: Collection<String>): Ledger = change { it.copy(cards = it.cards + cards, towns = it.towns + towns) }

    /** Records quests as celebrated; [seeded] marks that the first backfill has been done. */
    suspend fun markDone(ids: Collection<String>, seeded: Boolean = true, postcards: Int = 0) {
        change { it.copy(done = it.done + ids, seeded = seeded, earned = it.earned + postcards) }
    }

    suspend fun meet(travelerId: String) { change { it.copy(met = it.met + travelerId) } }

    suspend fun befriend(travelerId: String) { change { it.copy(met = it.met + travelerId, friends = it.friends + travelerId) } }

    /**
     * Records a postcard from [travelerId] about [townKey]. [mailKey] is the scheduled postcard it answers (null for one asked for
     * with a quest postcard, which is then used up). Returns false and changes nothing when that scheduled one was already sent.
     */
    suspend fun receive(travelerId: String, townKey: String, mailKey: String?): Boolean {
        var ok = true
        change {
            if (mailKey != null && mailKey in it.mailed) { ok = false; it }
            else it.copy(
                unlocked = it.unlocked + townKey, senders = it.senders + (townKey to travelerId),
                mailed = if (mailKey != null) it.mailed + mailKey else it.mailed,
                used = if (mailKey == null) it.used + 1 else it.used,
            )
        }
        return ok
    }

    /** Forgets which scheduled postcards were sent on [routeId], so that a new lap on it can send them again. */
    suspend fun newLapOn(routeId: String) { change { it.copy(mailed = it.mailed.filterNot { k -> k.split('|').getOrNull(1) == routeId }.toSet()) } }

    /** One line per part, for the backup. */
    suspend fun exportRaw(): String = current().let {
        listOf(
            it.cards.joinToString(","), it.towns.joinToString(";"), it.done.joinToString(","), it.seeded.toString(), it.unlocked.joinToString(";"), it.earned.toString(),
            it.met.joinToString(","), it.friends.joinToString(","), it.mailed.joinToString(";"), it.senders.entries.joinToString(";") { e -> "${e.key}>${e.value}" }, it.used.toString(),
        ).joinToString("\n")
    }

    suspend fun importRaw(raw: String) {
        val l = raw.split('\n')
        store.edit {
            write(
                it,
                Ledger(
                    split(l.getOrNull(0), ','), split(l.getOrNull(1), ';'), split(l.getOrNull(2), ','), l.getOrNull(3) == "true", split(l.getOrNull(4), ';'), l.getOrNull(5)?.toIntOrNull() ?: 0,
                    split(l.getOrNull(6), ','), split(l.getOrNull(7), ','), split(l.getOrNull(8), ';'), senders(l.getOrNull(9)), l.getOrNull(10)?.toIntOrNull() ?: 0,
                ),
            )
        }
    }

    private fun split(s: String?, sep: Char) = s?.split(sep)?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    private fun senders(s: String?): Map<String, String> = split(s, ';').mapNotNull { e -> e.split('>').takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()

    private fun read(p: Preferences) = Ledger(
        split(p[cardsKey], ','), split(p[townsKey], ';'), split(p[doneKey], ','), p[seededKey] ?: false, split(p[unlockedKey], ';'), p[earnedKey] ?: 0,
        split(p[metKey], ','), split(p[friendsKey], ','), split(p[mailedKey], ';'), senders(p[sendersKey]), p[usedKey] ?: 0,
    )

    private fun write(p: MutablePreferences, c: Ledger) {
        p[cardsKey] = c.cards.joinToString(","); p[townsKey] = c.towns.joinToString(";"); p[doneKey] = c.done.joinToString(","); p[seededKey] = c.seeded
        p[unlockedKey] = c.unlocked.joinToString(";"); p[earnedKey] = c.earned
        p[metKey] = c.met.joinToString(","); p[friendsKey] = c.friends.joinToString(","); p[mailedKey] = c.mailed.joinToString(";")
        p[sendersKey] = c.senders.entries.joinToString(";") { "${it.key}>${it.value}" }; p[usedKey] = c.used
    }
}
