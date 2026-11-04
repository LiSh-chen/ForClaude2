package com.starmist.walker.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Everything ever collected (kept across routes and laps) and the quests already celebrated. */
data class Ledger(val cards: Set<String> = emptySet(), val towns: Set<String> = emptySet(), val done: Set<String> = emptySet(), val seeded: Boolean = false)

/** The lifetime ledger the quests are measured against; a route only knows its own progress, this one never forgets. */
class CollectionStore(private val store: DataStore<Preferences>) {
    private val cardsKey = stringPreferencesKey("cards")
    private val townsKey = stringPreferencesKey("towns")
    private val doneKey = stringPreferencesKey("done")
    private val seededKey = booleanPreferencesKey("seeded")

    val flow: Flow<Ledger> = store.data.map { read(it) }

    suspend fun current(): Ledger = flow.first()

    /** Adds what was collected; returns the ledger as it is afterwards. Safe to call again with the same things. */
    suspend fun add(cards: Collection<String>, towns: Collection<String>): Ledger {
        var out = Ledger()
        store.edit {
            val cur = read(it)
            out = cur.copy(cards = cur.cards + cards, towns = cur.towns + towns)
            write(it, out)
        }
        return out
    }

    /** Records quests as celebrated; [seeded] marks that the first backfill has been done. */
    suspend fun markDone(ids: Collection<String>, seeded: Boolean = true) {
        store.edit { write(it, read(it).copy(done = read(it).done + ids, seeded = seeded)) }
    }

    /** Four lines of text (cards, townships, done quests, seeded flag) for the backup. */
    suspend fun exportRaw(): String = current().let { listOf(it.cards.joinToString(","), it.towns.joinToString(";"), it.done.joinToString(","), it.seeded.toString()).joinToString("\n") }

    suspend fun importRaw(raw: String) {
        val l = raw.split('\n')
        store.edit {
            write(it, Ledger(split(l.getOrNull(0), ','), split(l.getOrNull(1), ';'), split(l.getOrNull(2), ','), l.getOrNull(3) == "true"))
        }
    }

    private fun split(s: String?, sep: Char) = s?.split(sep)?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    private fun read(p: Preferences) = Ledger(split(p[cardsKey], ','), split(p[townsKey], ';'), split(p[doneKey], ','), p[seededKey] ?: false)
    private fun write(p: androidx.datastore.preferences.core.MutablePreferences, c: Ledger) {
        p[cardsKey] = c.cards.joinToString(","); p[townsKey] = c.towns.joinToString(";"); p[doneKey] = c.done.joinToString(","); p[seededKey] = c.seeded
    }
}
