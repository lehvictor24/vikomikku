package eu.kanade.tachiyomi.data.dictionary

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tachiyomi.core.common.preference.PreferenceStore

/**
 * In-process Japanese dictionary service.
 *
 * Ships with a 20-word built-in mini-dictionary so the popup works out of the box.
 * Call [loadJmdict] with a user-supplied JMdict JSON file to expand to 100k+ entries.
 *
 * The JMdict JSON format expected here is the one produced by
 * https://github.com/scriptin/jmdict-simplified (fields: words[].kanji, words[].kana,
 * words[].sense).
 */
class JmdictService(
    private val context: Context,
    private val preferenceStore: PreferenceStore,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /** Path to the user-supplied JMdict JSON file (empty = use mini dict only). */
    private val jmdictPath = preferenceStore.getString(PREF_JMDICT_PATH, "")

    @Volatile
    private var index: Map<String, DictionaryEntry> = MINI_DICT
    private var sortedKeys: List<String> = MINI_DICT.keys.sortedByDescending { it.length }

    /** Total number of entries in the current index. */
    val size: Int get() = index.size

    /** Whether the full JMdict is loaded (rather than just the mini-dict). */
    val isFullDictLoaded: Boolean get() = index !== MINI_DICT && index.size > MINI_DICT.size

    // ── Lookup ────────────────────────────────────────────────────────────────

    fun lookup(word: String): DictionaryEntry? = index[word]

    fun tokenize(text: String): List<Tokenizer.Token> = Tokenizer.tokenize(text, sortedKeys)

    // ── Loading ───────────────────────────────────────────────────────────────

    /**
     * Tries to load the JMdict from [jmdictPath] preference (if set).
     * Call once at startup; safe to call again after the user selects a new file.
     */
    suspend fun tryLoadSaved() {
        val path = jmdictPath.get().takeIf { it.isNotBlank() } ?: return
        try {
            loadJmdict(path)
        } catch (_: Exception) {
            // Saved path is stale or unreadable — silently fall back to mini-dict
        }
    }

    /** Parses the JMdict JSON at [uriString] and rebuilds the index. */
    suspend fun loadJmdict(uriString: String) = withContext(Dispatchers.IO) {
        val uri = android.net.Uri.parse(uriString)
        val rawJson = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            ?: error("Cannot open JMdict file")

        val jmdict = json.decodeFromString<JmdictJson>(rawJson)
        val newIndex = mutableMapOf<String, DictionaryEntry>()

        for (word in (jmdict.words ?: jmdict.entries ?: emptyList())) {
            val kanjiList = word.kanji.map { it.text }
            val kanaList = word.kana.map { it.text }
            val reading = kanaList.firstOrNull() ?: ""
            val headword = kanjiList.firstOrNull() ?: reading
            val isCommon = word.kanji.firstOrNull()?.tags
                ?.any { it in listOf("ichi1", "news1", "spec1", "gai1") } == true
            val meanings = word.sense.map { sense ->
                val pos = sense.partOfSpeech.take(2).joinToString(", ").ifBlank { "—" }
                val glosses = sense.gloss
                    .filter { it.lang == null || it.lang == "eng" }
                    .map { it.text }
                    .filter { it.isNotBlank() }
                DictionaryEntry.Meaning(pos, glosses)
            }.filter { it.glosses.isNotEmpty() }

            val entry = DictionaryEntry(
                word = headword,
                reading = reading,
                meanings = meanings,
                common = isCommon,
                frequencyRank = if (isCommon) 3500 else 500,
            )
            for (key in (kanjiList + kanaList)) {
                if (key.isNotBlank() && !newIndex.containsKey(key)) newIndex[key] = entry
            }
        }

        index = newIndex
        sortedKeys = newIndex.keys.sortedByDescending { it.length }
        jmdictPath.set(uriString)
    }

    // ── JMdict JSON models ────────────────────────────────────────────────────

    @Serializable
    private data class JmdictJson(
        val words: List<JWord>? = null,
        val entries: List<JWord>? = null,
    )

    @Serializable
    private data class JWord(
        val kanji: List<JKanji> = emptyList(),
        val kana: List<JKana> = emptyList(),
        val sense: List<JSense> = emptyList(),
    )

    @Serializable
    private data class JKanji(val text: String = "", val tags: List<String> = emptyList())

    @Serializable
    private data class JKana(val text: String = "", val tags: List<String> = emptyList())

    @Serializable
    private data class JSense(
        @SerialName("partOfSpeech") val partOfSpeech: List<String> = emptyList(),
        val gloss: List<JGloss> = emptyList(),
    )

    @Serializable
    private data class JGloss(
        val text: String = "",
        val lang: String? = null,
    )

    // ── Bundled mini-dictionary (20 words from mokuro-reader.html) ────────────

    companion object {
        const val PREF_JMDICT_PATH = "jmdict_path"

        private val MINI_DICT: Map<String, DictionaryEntry> = listOf(
            DictionaryEntry("魔法", "まほう", listOf(DictionaryEntry.Meaning("Noun", listOf("magic", "sorcery"))), true, 1240),
            DictionaryEntry("少女", "しょうじょ", listOf(DictionaryEntry.Meaning("Noun", listOf("girl", "young lady"))), true, 2100),
            DictionaryEntry("戦う", "たたかう", listOf(DictionaryEntry.Meaning("Verb (u)", listOf("to fight"))), false, 890),
            DictionaryEntry("今日", "きょう", listOf(DictionaryEntry.Meaning("Noun", listOf("today"))), true, 4500),
            DictionaryEntry("夢", "ゆめ", listOf(DictionaryEntry.Meaning("Noun", listOf("dream"))), true, 2900),
            DictionaryEntry("世界", "せかい", listOf(DictionaryEntry.Meaning("Noun", listOf("world"))), true, 5100),
            DictionaryEntry("希望", "きぼう", listOf(DictionaryEntry.Meaning("Noun", listOf("hope"))), true, 1900),
            DictionaryEntry("力", "ちから", listOf(DictionaryEntry.Meaning("Noun", listOf("strength", "power"))), true, 4100),
            DictionaryEntry("笑顔", "えがお", listOf(DictionaryEntry.Meaning("Noun", listOf("smile"))), false, 780),
            DictionaryEntry("信じる", "しんじる", listOf(DictionaryEntry.Meaning("Verb (ru)", listOf("to believe"))), true, 1800),
            DictionaryEntry("輝く", "かがやく", listOf(DictionaryEntry.Meaning("Verb (u)", listOf("to shine"))), false, 540),
            DictionaryEntry("星", "ほし", listOf(DictionaryEntry.Meaning("Noun", listOf("star"))), true, 2200),
            DictionaryEntry("諦める", "あきらめる", listOf(DictionaryEntry.Meaning("Verb (ru)", listOf("to give up"))), false, 620),
            DictionaryEntry("待つ", "まつ", listOf(DictionaryEntry.Meaning("Verb (u)", listOf("to wait"))), true, 3200),
            DictionaryEntry("私", "わたし", listOf(DictionaryEntry.Meaning("Pronoun", listOf("I", "me"))), true, 5000),
            DictionaryEntry("君", "きみ", listOf(DictionaryEntry.Meaning("Pronoun", listOf("you"))), true, 3800),
            DictionaryEntry("好き", "すき", listOf(DictionaryEntry.Meaning("Adj (na)", listOf("liked"))), true, 4200),
            DictionaryEntry("嘘", "うそ", listOf(DictionaryEntry.Meaning("Noun", listOf("lie"))), true, 2600),
            DictionaryEntry("代償", "だいしょう", listOf(DictionaryEntry.Meaning("Noun", listOf("price", "cost"))), false, 420),
            DictionaryEntry("笑う", "わらう", listOf(DictionaryEntry.Meaning("Verb (u)", listOf("to laugh"))), true, 2800),
        ).flatMap { entry ->
            listOf(entry.word to entry, entry.reading to entry)
        }.toMap()
    }
}
