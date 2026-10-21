package com.starmist.core.route

/** What the content pack says about one card: its name and the exactly-ten-character line. */
data class CardText(val name: String, val blurb: String)

/**
 * The three text files of the content pack and the rules each must obey. The same checks run in
 * the unit tests (so a bad file never reaches a release) and in the app when a downloaded pack
 * arrives (so a bad download is thrown away and the bundled text stays).
 */
object ContentPack {
    /** Bumped only when a file's layout changes in a way an older app could not read. */
    const val SCHEMA = 1

    const val CARDS = "card_texts.txt"
    const val FACTS = "landmark_facts.txt"
    const val TOWNS = "town_blurbs.txt"
    val FILES = listOf(CARDS, FACTS, TOWNS)

    private val superlatives = Regex("最|唯一|第一|全台|全國|僅存")

    private fun data(lines: List<String>) = lines.filter { it.isNotBlank() && !it.startsWith("#") }

    /** `id|name|blurb`; lines that do not fit are left out. */
    fun parseCards(lines: List<String>): Map<String, CardText> =
        data(lines).mapNotNull { l ->
            val f = l.split('|')
            if (f.size == 3 && f.all { it.isNotBlank() }) f[0].trim() to CardText(f[1].trim(), f[2].trim()) else null
        }.toMap()

    fun validateCards(lines: List<String>, knownIds: Set<String>): List<String> {
        val errors = ArrayList<String>()
        val seen = HashSet<String>()
        data(lines).forEach { l ->
            val f = l.split('|')
            if (f.size != 3 || f.any { it.isBlank() }) { errors += "bad line: $l"; return@forEach }
            if (f[0] !in knownIds) errors += "unknown card id ${f[0]}"
            if (!seen.add(f[0])) errors += "duplicate card id ${f[0]}"
            if (f[2].trim().length != 10) errors += "${f[0]}: the line must be exactly 10 characters: ${f[2]}"
        }
        (knownIds - seen).forEach { errors += "missing card $it" }
        return errors
    }

    /** `id|fact|fact` (one or two facts of 14 to 60 characters). */
    fun validateFacts(lines: List<String>, knownIds: Set<String>): List<String> {
        val errors = ArrayList<String>()
        data(lines).forEach { l ->
            val f = l.split('|')
            if (f.size !in 2..3) { errors += "bad line: $l"; return@forEach }
            if (f[0] !in knownIds) errors += "unknown card id ${f[0]}"
            f.drop(1).forEach { t ->
                if (t.trim().length !in 14..60) errors += "${f[0]}: a fact must be 14 to 60 characters: $t"
                if (superlatives.containsMatchIn(t)) errors += "${f[0]}: no superlatives: $t"
            }
        }
        return errors
    }

    /** `county|name|tag:sentence` with one to three tags. */
    fun validateTowns(lines: List<String>): List<String> {
        val errors = ArrayList<String>()
        data(lines).forEach { l ->
            val f = l.split('|')
            if (f.size !in 3..5) { errors += "bad line: $l"; return@forEach }
            f.drop(2).forEach { x ->
                val t = x.split(':', limit = 2)
                if (t.size != 2 || t[0].isBlank() || t[1].isBlank()) errors += "${f[1]}: bad tag: $x"
                else {
                    if (t[1].trim().length !in 14..70) errors += "${f[1]}/${t[0]}: sentence must be 14 to 70 characters"
                    if (superlatives.containsMatchIn(t[1])) errors += "${f[1]}/${t[0]}: no superlatives"
                }
            }
        }
        return errors
    }

    /** Checks one file by name; an unknown name is an error. */
    fun validate(name: String, lines: List<String>, knownIds: Set<String>): List<String> = when (name) {
        CARDS -> validateCards(lines, knownIds)
        FACTS -> validateFacts(lines, knownIds)
        TOWNS -> validateTowns(lines)
        else -> listOf("unknown file $name")
    }
}

/** The route with card names and lines replaced by [texts]; a card without an entry is left as it is. */
fun RouteDef.withCardTexts(texts: Map<String, CardText>): RouteDef =
    copy(stops = stops.map { s ->
        val lm = s.landmark ?: return@map s
        val t = texts[lm.id] ?: return@map s
        s.copy(name = t.name, landmark = lm.copy(name = t.name, blurb = t.blurb))
    })
