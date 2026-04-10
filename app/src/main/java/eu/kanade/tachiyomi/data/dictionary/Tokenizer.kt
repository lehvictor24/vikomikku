package eu.kanade.tachiyomi.data.dictionary

/**
 * Greedy longest-match tokenizer over a dictionary index.
 *
 * Scans [text] left-to-right and tries to match the longest dictionary key starting at each
 * position. If no match is found the current character is emitted as a single-character token
 * with [Token.hasEntry] = false.
 *
 * Ported from the greedy tokenizer in mokuro-reader.html.
 */
object Tokenizer {

    data class Token(val text: String, val hasEntry: Boolean)

    /**
     * Tokenises [text] using [dictKeys] (all keys already sorted by descending length for a
     * single pass). Returns an ordered list of tokens.
     */
    fun tokenize(text: String, dictKeys: List<String>): List<Token> {
        // Sort keys by descending length once; callers may cache this sorted list.
        val sortedKeys = dictKeys.sortedByDescending { it.length }
        val result = mutableListOf<Token>()
        var i = 0
        while (i < text.length) {
            var matched = false
            for (key in sortedKeys) {
                if (text.startsWith(key, i)) {
                    result += Token(key, hasEntry = true)
                    i += key.length
                    matched = true
                    break
                }
            }
            if (!matched) {
                result += Token(text[i].toString(), hasEntry = false)
                i++
            }
        }
        return result
    }
}
