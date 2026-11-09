package com.starmist.core.route

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
    const val POSTCARDS = "postcard_facts.txt"
    val FILES = listOf(CARDS, FACTS, TOWNS, POIS, POSTCARDS)

    private val superlatives = Regex("最|唯一|第一|全台|全國|僅存")

    /** An editor's note that slipped into a sentence, such as "（拿掉冠軍）". */
    private val editorNote = Regex("（(拿掉|維持|建議|二擇一|待|請|刪)")

    private fun data(lines: List<String>) = lines.filter { it.isNotBlank() && !it.startsWith("#") }

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

    /** `county|name|tag:sentence` with one to three tags; a sentence is 14 to [TOWN_MAX] characters. */
    fun validateTowns(lines: List<String>): List<String> {
        val errors = ArrayList<String>()
        data(lines).forEach { l ->
            val f = l.split('|')
            if (f.size !in 3..5) { errors += "bad line: $l"; return@forEach }
            f.drop(2).forEach { x ->
                val t = x.split(':', limit = 2)
                if (t.size != 2 || t[0].isBlank() || t[1].isBlank()) errors += "${f[1]}: bad tag: $x"
                else {
                    val sentence = t[1].trim()
                    val checked = sentence.endsWith(CHECKED)
                    if (sentence.trimEnd(CHECKED).length !in 14..TOWN_MAX) errors += "${f[1]}/${t[0]}: sentence must be 14 to $TOWN_MAX characters"
                    if (!checked && superlatives.containsMatchIn(sentence)) errors += "${f[1]}/${t[0]}: no superlatives"
                    if (editorNote.containsMatchIn(sentence)) errors += "${f[1]}/${t[0]}: editor's note left in the sentence"
                }
            }
        }
        return errors
    }

    /**
     * A township sentence may say "first" or "only" when it ends with this mark: the claim was checked
     * against at least two sources (written in docs/research). The app does not show the mark.
     */
    const val CHECKED = '※'

    /** A township sentence says what the place is and what one can do there, so it may run longer than a card fact. */
    const val TOWN_MAX = 90

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

    private val datingWords = Regex("目前|現在|最近|如今|至今|今年|近年|日前")

    /** `county|name|travelerId|sentence`: what a traveler writes about a township the route does not pass (20 to 120 characters). */
    fun validatePostcards(lines: List<String>): List<String> {
        val errors = ArrayList<String>()
        val seen = HashSet<String>()
        val travelers = Travelers.all.map { it.id }.toSet()
        data(lines).forEach { l ->
            val f = l.split('|')
            if (f.size != 4) { errors += "bad line: $l"; return@forEach }
            val key = f[0] + "|" + f[1] + "|" + f[2]
            if (!seen.add(key)) errors += "duplicate $key"
            if (f[2] !in travelers) errors += "${f[1]}: unknown traveler ${f[2]}"
            val s = f[3].trim()
            if (s.trimEnd(CHECKED).length !in 20..120) errors += "${f[1]}/${f[2]}: sentence must be 20 to 120 characters"
            if (!s.endsWith(CHECKED) && superlatives.containsMatchIn(s)) errors += "${f[1]}/${f[2]}: no superlatives"
            if (datingWords.containsMatchIn(s)) errors += "${f[1]}/${f[2]}: no words that date"
            if (editorNote.containsMatchIn(s)) errors += "${f[1]}/${f[2]}: editor's note left in the sentence"
        }
        return errors
    }

    /** Township key ("county|name") to what each traveler writes about it. */
    fun parsePostcards(lines: List<String>): Map<String, Map<String, String>> =
        data(lines).mapNotNull { l -> l.split('|').takeIf { it.size == 4 }?.let { Triple(it[0] + "|" + it[1], it[2], it[3].trim().trimEnd(CHECKED)) } }
            .groupBy({ it.first }, { it.second to it.third }).mapValues { it.value.toMap() }

    /** Checks one file by name; an unknown name is an error. */
    fun validate(name: String, lines: List<String>, knownIds: Set<String>): List<String> = when (name) {
        CARDS -> validateCards(lines, knownIds)
        FACTS -> validateFacts(lines, knownIds)
        TOWNS -> validateTowns(lines)
        POIS -> validatePois(lines, knownIds)
        POSTCARDS -> validatePostcards(lines)
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
