package com.starmist.core.route

/** A place name as said in another language of Taiwan: [lang] 台語 or 客語, the [han] characters, the official [roman] spelling, a [soundAlike] written with Mandarin characters, and where the reading came from ([source]: 辭典 or 逐字). */
data class LocalName(val lang: String, val han: String, val roman: String, val soundAlike: String, val source: String)

/** What the content pack says about one card: its name. Its features live in the facts file. */
data class CardText(val name: String)

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
    const val POIS = "poi_intros.txt"
    const val LOCAL = "local_names.txt"
    val FILES = listOf(CARDS, FACTS, TOWNS, POIS, LOCAL)

    private val superlatives = Regex("最|唯一|第一|全台|全國|僅存")

    private fun data(lines: List<String>) = lines.filter { it.isNotBlank() && !it.startsWith("#") }

    /** `key|language|characters|romanization|sound-alike|source`, as key to its entry. */
    fun parseLocalNames(lines: List<String>): Map<String, LocalName> =
        data(lines).mapNotNull { l ->
            val f = l.split('|').map { it.trim() }
            if (f.size == 6 && f.all { it.isNotEmpty() }) f[0] to LocalName(f[1], f[2], f[3], f[4], f[5]) else null
        }.toMap()

    private val han = Regex("^\\p{IsHan}{2,6}$")
    private val roman = Regex("^[\\p{L}\\p{M}\\- ]{2,40}$")

    /** `key|language|characters|romanization|sound-alike|source`: a card id or `county/township` key; a Hokkien or Hakka reading of a place name. */
    fun validateLocalNames(lines: List<String>, knownIds: Set<String>): List<String> {
        val errors = ArrayList<String>()
        val seen = HashSet<String>()
        data(lines).forEach { l ->
            val f = l.split('|').map { it.trim() }
            if (f.size != 6 || f.any { it.isEmpty() }) { errors += "bad line: $l"; return@forEach }
            if (!seen.add(f[0])) errors += "duplicate key ${f[0]}"
            if ('/' in f[0]) { if (!Regex("^.+[縣市]/.+[鄉鎮市區]$").matches(f[0])) errors += "bad township key ${f[0]}" }
            else if (f[0] !in knownIds) errors += "unknown card id ${f[0]}"
            if (f[1] !in setOf("台語", "客語")) errors += "${f[0]}: language must be 台語 or 客語"
            if (!han.matches(f[2])) errors += "${f[0]}: characters: ${f[2]}"
            if (!roman.matches(f[3])) errors += "${f[0]}: romanization: ${f[3]}"
            if (!han.matches(f[4])) errors += "${f[0]}: sound-alike: ${f[4]}"
            if (f[5] !in setOf("辭典", "逐字")) errors += "${f[0]}: source must be 辭典 or 逐字"
            if (superlatives.containsMatchIn(l)) errors += "${f[0]}: no superlatives"
        }
        return errors
    }

    /** `id|sentence|sentence` as id to its sentences. */
    fun parsePois(lines: List<String>): Map<String, List<String>> =
        data(lines).mapNotNull { l -> l.split('|').let { f -> if (f.size >= 2) f[0].trim() to f.drop(1).map { it.trim() }.filter { it.isNotEmpty() } else null } }.toMap()

    /** `id|name`; lines that do not fit are left out. */
    fun parseCards(lines: List<String>): Map<String, CardText> =
        data(lines).mapNotNull { l ->
            val f = l.split('|')
            if (f.size == 2 && f.all { it.isNotBlank() }) f[0].trim() to CardText(f[1].trim()) else null
        }.toMap()

    fun validateCards(lines: List<String>, knownIds: Set<String>): List<String> {
        val errors = ArrayList<String>()
        val seen = HashSet<String>()
        data(lines).forEach { l ->
            val f = l.split('|')
            if (f.size != 2 || f.any { it.isBlank() }) { errors += "bad line: $l"; return@forEach }
            if (f[0] !in knownIds) errors += "unknown card id ${f[0]}"
            if (!seen.add(f[0])) errors += "duplicate card id ${f[0]}"
        }
        (knownIds - seen).forEach { errors += "missing card $it" }
        return errors
    }

    /** `id|tag:fact|tag:fact|tag:fact`: one to three features per card, a short tag and a sentence of 14 to 70 characters. */
    fun validateFacts(lines: List<String>, knownIds: Set<String>): List<String> {
        val errors = ArrayList<String>()
        val seen = HashSet<String>()
        data(lines).forEach { l ->
            val f = l.split('|')
            if (f.size !in 2..4) { errors += "bad line: $l"; return@forEach }
            if (f[0] !in knownIds) errors += "unknown card id ${f[0]}"
            if (!seen.add(f[0])) errors += "duplicate card id ${f[0]}"
            f.drop(1).forEach { x ->
                val t = x.split(':', limit = 2)
                if (t.size != 2 || t[0].isBlank() || t[1].isBlank()) errors += "${f[0]}: bad feature: $x"
                else {
                    if (t[1].trim().length !in 14..70) errors += "${f[0]}/${t[0]}: sentence must be 14 to 70 characters"
                    if (superlatives.containsMatchIn(t[1])) errors += "${f[0]}/${t[0]}: no superlatives"
                }
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

    /** `id|sentence|sentence`: one to three sentences of 14 to 70 characters about a roadside sight, one line for each sight in [knownIds]. */
    fun validatePois(lines: List<String>, knownIds: Set<String>): List<String> {
        val errors = ArrayList<String>()
        val seen = HashSet<String>()
        data(lines).forEach { l ->
            val f = l.split('|')
            if (f.size !in 2..4) { errors += "bad line: $l"; return@forEach }
            if (f[0] !in knownIds) errors += "unknown sight id ${f[0]}"
            if (!seen.add(f[0])) errors += "duplicate sight id ${f[0]}"
            f.drop(1).forEach { x ->
                if (x.trim().length !in 14..70) errors += "${f[0]}: sentence must be 14 to 70 characters: $x"
                if (superlatives.containsMatchIn(x)) errors += "${f[0]}: no superlatives: $x"
            }
        }
        (knownIds - seen).forEach { errors += "missing sight $it" }
        return errors
    }

    /** Checks one file by name; an unknown name is an error. */
    fun validate(name: String, lines: List<String>, knownIds: Set<String>): List<String> = when (name) {
        CARDS -> validateCards(lines, knownIds)
        FACTS -> validateFacts(lines, knownIds)
        TOWNS -> validateTowns(lines)
        POIS -> validatePois(lines, knownIds)
        LOCAL -> validateLocalNames(lines, knownIds)
        else -> listOf("unknown file $name")
    }
}

/** The route with card names replaced by [texts]; a card without an entry is left as it is. */
fun RouteDef.withCardTexts(texts: Map<String, CardText>): RouteDef =
    copy(stops = stops.map { s ->
        val lm = s.landmark ?: return@map s
        val t = texts[lm.id] ?: return@map s
        s.copy(name = t.name, landmark = lm.copy(name = t.name))
    })
