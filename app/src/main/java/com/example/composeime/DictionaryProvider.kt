package com.example.composeime

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.*
import java.util.zip.GZIPInputStream

class DictionaryProvider(private val context: Context) {
    // Main dictionary using a more efficient data structure: Trie
    private val wordTrie = WordTrie()

    // N-gram model for next word prediction
    private val bigramModel = mutableMapOf<String, MutableMap<String, Int>>()

    // Top frequent words cache for quick access
    private val topFrequentWords = mutableListOf<String>()

    // User-specific words learned from typing
    private val userDictionary = mutableMapOf<String, Int>()

    // User-specific n-grams
    private val userBigrams = mutableMapOf<String, MutableMap<String, Int>>()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Dictionary size monitoring
    private val _dictionarySize = MutableStateFlow(0)
    val dictionarySize: StateFlow<Int> = _dictionarySize

    // Shared preferences for user dictionary persistence
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("keyboard_dictionary", Context.MODE_PRIVATE)
    }

    // Constants
    companion object {
        private const val TOP_WORDS_COUNT = 1000
        private const val DEFAULT_SUGGESTION_LIMIT = 3
        private const val USER_WORD_BOOST_FACTOR = 2.5
        private const val MAIN_DICTIONARY_FILENAME = "word_freq_dict.gz"
        private const val BIGRAM_DICTIONARY_FILENAME = "bigram_dict.gz"
    }

    init {
        // Load user dictionary from preferences
        loadUserDictionary()
    }

    /**
     * Load the main dictionary from compressed resource file or cached file
     */
    suspend fun loadMainDictionary() = withContext(Dispatchers.IO) {
        _isLoading.value = true

        try {
            val startTime = System.currentTimeMillis()
            val cacheFile = File(context.cacheDir, "dictionary_cache")

            // Try to load from cache first (faster on subsequent launches)
            if (cacheFile.exists() && cacheFile.length() > 0) {
                loadFromCache(cacheFile)
            } else {
                // Load from raw resources and create cache
                loadFromResources()
                createCache(cacheFile)
            }

            // Precompute top frequent words for quick suggestion access
            precomputeTopWords()

            val endTime = System.currentTimeMillis()
            println("Dictionary loaded in ${endTime - startTime}ms with ${_dictionarySize.value} words")

        } catch (e: Exception) {
            e.printStackTrace()
            // If compressed file is missing or error occurs, add basic words
            addBasicWords()
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Load dictionary from cached binary format
     */
    private fun loadFromCache(cacheFile: File) {
        try {
            FileInputStream(cacheFile).use { fis ->
                // Implementation would deserialize the trie structure
                // For simplicity, just load as text for now
                BufferedReader(InputStreamReader(fis)).use { reader ->
                    var line: String?
                    var count = 0

                    while (reader.readLine().also { line = it } != null) {
                        line?.let { entry ->
                            val parts = entry.split(",")
                            if (parts.size >= 2) {
                                val word = parts[0].trim().lowercase()
                                val frequency = parts[1].trim().toIntOrNull() ?: 1
                                wordTrie.insert(word, frequency)
                                count++
                            }
                        }
                    }
                    _dictionarySize.value = count
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fall back to resource loading if cache fails
            loadFromResources()
        }
    }

    /**
     * Load dictionary from raw resources
     */
    private fun loadFromResources() {
        try {
            // You'll need to add this word frequency dictionary to your raw resources
            context.resources.openRawResource(R.raw.word_freq_dict).use { inputStream ->
                GZIPInputStream(inputStream).use { gzipStream ->
                    BufferedReader(InputStreamReader(gzipStream)).use { reader ->
                        var line: String?
                        var count = 0

                        while (reader.readLine().also { line = it } != null) {
                            line?.let { entry ->
                                val parts = entry.split(",")
                                if (parts.size >= 2) {
                                    val word = parts[0].trim().lowercase()
                                    val frequency = parts[1].trim().toIntOrNull() ?: 1
                                    wordTrie.insert(word, frequency)
                                    count++
                                }
                            }
                        }
                        _dictionarySize.value = count
                    }
                }
            }

            // Load bigram data for next-word prediction
            try {
                context.resources.openRawResource(R.raw.bigram_dict).use { inputStream ->
                    GZIPInputStream(inputStream).use { gzipStream ->
                        BufferedReader(InputStreamReader(gzipStream)).use { reader ->
                            var line: String?

                            while (reader.readLine().also { line = it } != null) {
                                line?.let { entry ->
                                    val parts = entry.split(",")
                                    if (parts.size >= 3) {
                                        val word1 = parts[0].trim().lowercase()
                                        val word2 = parts[1].trim().lowercase()
                                        val frequency = parts[2].trim().toIntOrNull() ?: 1

                                        bigramModel.getOrPut(word1) { mutableMapOf() }[word2] = frequency
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Bigram data is optional, so just log the error
                e.printStackTrace()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            addBasicWords()
        }
    }

    /**
     * Create a cache file for faster loading in the future
     */
    private fun createCache(cacheFile: File) {
        try {
            FileOutputStream(cacheFile).use { fos ->
                // Implementation would serialize the trie structure
                // For simplicity, just storing as text for now
                wordTrie.getAllWords().forEach { (word, freq) ->
                    fos.write("$word,$freq\n".toByteArray())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Cache creation is optional, so just log the error
        }
    }

    /**
     * Precompute the most frequent words for quick access
     */
    private fun precomputeTopWords() {
        topFrequentWords.clear()
        topFrequentWords.addAll(
            wordTrie.getAllWords()
                .sortedByDescending { it.second }
                .take(TOP_WORDS_COUNT)
                .map { it.first }
        )
    }

    /**
     * Load user dictionary from SharedPreferences
     */
    private fun loadUserDictionary() {
        userDictionary.clear()
        userBigrams.clear()

        val userDictStr = prefs.getString("user_dictionary", null)
        userDictStr?.split("|")?.forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val word = parts[0]
                val freq = parts[1].toIntOrNull() ?: 1
                userDictionary[word] = freq
            }
        }

        val userBigramStr = prefs.getString("user_bigrams", null)
        userBigramStr?.split("|")?.forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 3) {
                val word1 = parts[0]
                val word2 = parts[1]
                val freq = parts[2].toIntOrNull() ?: 1
                userBigrams.getOrPut(word1) { mutableMapOf() }[word2] = freq
            }
        }
    }

    /**
     * Save user dictionary to SharedPreferences
     */
    private fun saveUserDictionary() {
        val dictStr = userDictionary.entries
            .joinToString("|") { "${it.key}:${it.value}" }

        val bigramStr = userBigrams.entries
            .flatMap { (word1, map) ->
                map.entries.map { (word2, freq) -> "$word1:$word2:$freq" }
            }
            .joinToString("|")

        prefs.edit {
            putString("user_dictionary", dictStr)
            putString("user_bigrams", bigramStr)
        }
    }

    /**
     * Add a word the user has typed to the user dictionary
     */
    fun learnWord(word: String) {
        if (word.isBlank() || word.length < 2) return

        val lowerWord = word.lowercase(Locale.getDefault())
        val currentFreq = userDictionary[lowerWord] ?: 0
        userDictionary[lowerWord] = currentFreq + 1

        // Also add to the main trie with high frequency
        wordTrie.insert(lowerWord, (currentFreq + 1) * USER_WORD_BOOST_FACTOR.toInt())

        // Save periodically
        if (currentFreq % 5 == 0) {
            saveUserDictionary()
        }
    }

    /**
     * Learn word pairs (bigrams) when user types words in sequence
     */
    fun learnWordPair(firstWord: String, secondWord: String) {
        if (firstWord.isBlank() || secondWord.isBlank() ||
            firstWord.length < 2 || secondWord.length < 2) return

        val lowerFirst = firstWord.lowercase(Locale.getDefault())
        val lowerSecond = secondWord.lowercase(Locale.getDefault())

        val bigramMap = userBigrams.getOrPut(lowerFirst) { mutableMapOf() }
        val currentFreq = bigramMap[lowerSecond] ?: 0
        bigramMap[lowerSecond] = currentFreq + 1

        // Save periodically
        if (currentFreq % 3 == 0) {
            saveUserDictionary()
        }
    }

    /**
     * Add a fallback set of basic words
     */
    fun addBasicWords() {
        val commonWords = listOf(
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "I",
            "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
            "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
            "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
            "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
            "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
            "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
            "than", "then", "now", "look", "only", "come", "its", "over", "think", "also"
        )

        // Add with high frequency
        commonWords.forEachIndexed { index, word ->
            // Give earlier words higher frequency
            val frequency = 10000 - index * 10
            wordTrie.insert(word, frequency)
        }

        // Add some common bigrams
        val commonBigrams = mapOf(
            "thank" to listOf("you", "god", "goodness"),
            "how" to listOf("are", "is", "do", "did", "about"),
            "i" to listOf("am", "will", "have", "think", "was", "can"),
            "let" to listOf("me", "us", "it", "them"),
            "would" to listOf("you", "like", "be", "have"),
            "could" to listOf("you", "be", "have", "get"),
            "please" to listOf("let", "help", "send", "check")
        )

        commonBigrams.forEach { (first, seconds) ->
            seconds.forEachIndexed { index, second ->
                bigramModel.getOrPut(first) { mutableMapOf() }[second] = 1000 - index * 10
            }
        }

        _dictionarySize.value = commonWords.size
    }

    /**
     * Get suggestions based on the current word prefix
     */
    fun getSuggestions(currentWord: String, limit: Int = DEFAULT_SUGGESTION_LIMIT): List<String> {
        if (currentWord.isBlank()) return emptyList()

        val lowerPrefix = currentWord.lowercase(Locale.getDefault())

        // Get prefix matches from the trie
        val matches = wordTrie.findAllWithPrefix(lowerPrefix, limit * 3)

        // Combine with any user dictionary matches
        val userMatches = userDictionary.entries
            .filter { it.key.startsWith(lowerPrefix) }
            .map { it.key to (it.value * USER_WORD_BOOST_FACTOR).toInt() }

        // Get exact match if it exists (to potentially boost it)
        val exactMatch = wordTrie.find(lowerPrefix)

        // Combine all matches
        return (matches + userMatches)
            .groupBy { it.first }
            .map { entry ->
                val frequency = entry.value.sumOf { it.second }
                // Boost exact matches slightly
                val adjustedFreq = if (entry.key == lowerPrefix) frequency * 1.2 else frequency
                entry.key to adjustedFreq
            }
            .sortedByDescending { it.second.toInt()  }
            .take(limit)
            .map { it.first }
    }

    /**
     * Get next word suggestions based on the previous word
     */
    fun getNextWordSuggestions(previousWord: String, limit: Int = DEFAULT_SUGGESTION_LIMIT): List<String> {
        if (previousWord.isBlank()) {
            // At the start of input, return common starter words
            return topFrequentWords.take(limit)
        }

        val lowerPrevious = previousWord.lowercase(Locale.getDefault())

        // Try user-specific bigrams first
        val userNextWords = userBigrams[lowerPrevious]?.entries
            ?.map { it.key to (it.value * USER_WORD_BOOST_FACTOR).toInt() }
            ?.sortedByDescending { it.second }
            ?.map { it.first }
            ?.take(limit)

        if (!userNextWords.isNullOrEmpty()) {
            return userNextWords
        }

        // Try general bigram model
        val generalNextWords = bigramModel[lowerPrevious]?.entries
            ?.map { it.key to it.value }
            ?.sortedByDescending { it.second }
            ?.map { it.first }
            ?.take(limit)

        if (!generalNextWords.isNullOrEmpty()) {
            return generalNextWords
        }

        // Fall back to most common words if no bigrams available
        return topFrequentWords.take(limit)
    }

    /**
     * Get contextual suggestions based on previous words and current prefix
     */
    fun getContextualSuggestions(
        previousWords: List<String>,
        currentPrefix: String,
        limit: Int = DEFAULT_SUGGESTION_LIMIT
    ): List<String> {
        // If we have a prefix, prioritize prefix matches
        if (currentPrefix.isNotEmpty()) {
            return getSuggestions(currentPrefix, limit)
        }

        // Otherwise use the last word for next-word prediction
        val lastWord = previousWords.lastOrNull() ?: return topFrequentWords.take(limit)
        return getNextWordSuggestions(lastWord, limit)
    }
}

/**
 * Trie data structure for efficient prefix lookups
 */
class WordTrie {
    private val root = TrieNode()

    /**
     * Insert a word with its frequency
     */
    fun insert(word: String, frequency: Int) {
        var current = root

        for (char in word) {
            current = current.children.getOrPut(char) { TrieNode() }
        }

        current.isEndOfWord = true
        current.frequency = frequency
    }

    /**
     * Find exact word match and return its frequency
     */
    fun find(word: String): Pair<String, Int>? {
        var current = root

        for (char in word) {
            current = current.children[char] ?: return null
        }

        return if (current.isEndOfWord) word to current.frequency else null
    }

    /**
     * Find all words with the given prefix, sorted by frequency
     */
    fun findAllWithPrefix(prefix: String, limit: Int): List<Pair<String, Int>> {
        var current = root

        // Navigate to the prefix node
        for (char in prefix) {
            current = current.children[char] ?: return emptyList()
        }

        // Get all words with this prefix
        val results = mutableListOf<Pair<String, Int>>()
        findAllWords(current, StringBuilder(prefix), results)

        return results.sortedByDescending { it.second }.take(limit)
    }

    /**
     * Recursively find all words from a given node
     */
    private fun findAllWords(
        node: TrieNode,
        prefix: StringBuilder,
        results: MutableList<Pair<String, Int>>
    ) {
        if (node.isEndOfWord) {
            results.add(prefix.toString() to node.frequency)
        }

        for ((char, childNode) in node.children) {
            prefix.append(char)
            findAllWords(childNode, prefix, results)
            prefix.deleteCharAt(prefix.length - 1)
        }
    }

    /**
     * Get all words in the trie (for caching)
     */
    fun getAllWords(): List<Pair<String, Int>> {
        val results = mutableListOf<Pair<String, Int>>()
        findAllWords(root, StringBuilder(), results)
        return results
    }
}

/**
 * Node in the Trie
 */
class TrieNode {
    val children = mutableMapOf<Char, TrieNode>()
    var isEndOfWord = false
    var frequency = 0
}
