package eu.kanade.tachiyomi.data.dictionary

/**
 * A single dictionary entry for a Japanese word.
 */
data class DictionaryEntry(
    val word: String,
    val reading: String,
    val meanings: List<Meaning>,
    val common: Boolean = false,
    val frequencyRank: Int = 0,
) {
    data class Meaning(
        val pos: String,
        val glosses: List<String>,
    )
}
