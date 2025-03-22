package com.example.composeime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KeyboardViewModel(private val dictionaryProvider: DictionaryProvider) : ViewModel() {

    // Current word being typed
    private val _currentWord = MutableStateFlow("")
    val currentWord = _currentWord.asStateFlow()

    // Previous words (for context-aware suggestions)
    private val _previousWords = MutableStateFlow<List<String>>(emptyList())

    // Sentence tracking
    private val _isStartOfSentence = MutableStateFlow(true)

    // Current suggestions based on typed word
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    // Dictionary stats
    val isLoading = dictionaryProvider.isLoading
    val dictionarySize = dictionaryProvider.dictionarySize

    // Recently used words cache
    private val recentWords = mutableListOf<String>()
    private val maxRecentWords = 50

    // Maximum number of previous words to track for context
    private val maxPreviousWordsContext = 3

    init {
        // Load dictionary in background
        viewModelScope.launch {
            dictionaryProvider.loadMainDictionary()
        }
    }

    /**
     * Process text input and update state
     */
    fun processTextInput(text: String) {
        when {
            // Space or punctuation: word is complete
            text == " " || text.matches(Regex("[.,!?;:]")) -> {
                // Learn the completed word if it's valid
                val completedWord = _currentWord.value
                if (completedWord.length > 1) {
                    dictionaryProvider.learnWord(completedWord)
                    addToRecentWords(completedWord)

                    // Learn word pairs for better next-word prediction
                    val previousWord = _previousWords.value.lastOrNull()
                    if (previousWord != null) {
                        dictionaryProvider.learnWordPair(previousWord, completedWord)
                    }

                    // Track previous words for context
                    val updatedPrevWords = _previousWords.value.toMutableList()
                    updatedPrevWords.add(completedWord)
                    if (updatedPrevWords.size > maxPreviousWordsContext) {
                        updatedPrevWords.removeAt(0)
                    }
                    _previousWords.value = updatedPrevWords
                }

                _currentWord.value = ""

                // Check if this is end of sentence
                if (text.matches(Regex("[.!?]"))) {
                    _isStartOfSentence.value = true
                    // Clear context when sentence ends
                    _previousWords.value = emptyList()
                }

                // Update suggestions - if space, show next-word suggestions
                if (text == " ") {
                    _suggestions.value = dictionaryProvider.getContextualSuggestions(
                        _previousWords.value,
                        "",
                        3
                    )
                } else {
                    _suggestions.value = emptyList()
                }
            }

            // Backspace handling
            text == "⌫" -> {
                if (_currentWord.value.isNotEmpty()) {
                    _currentWord.value = _currentWord.value.dropLast(1)
                    updateSuggestions()
                } else {
                    // We're potentially erasing a previous word
                    if (_previousWords.value.isNotEmpty()) {
                        val prevWordsList = _previousWords.value.toMutableList()
                        prevWordsList.removeAt(prevWordsList.lastIndex)
                        _previousWords.value = prevWordsList
                    }
                    _suggestions.value = dictionaryProvider.getContextualSuggestions(
                        _previousWords.value,
                        "",
                        3
                    )
                }
            }

            // Regular character input
            else -> {
                // If first character, apply capitalization for sentence start
                val charToAdd = if (_isStartOfSentence.value && _currentWord.value.isEmpty()) {
                    _isStartOfSentence.value = false
                    text.uppercase()
                } else {
                    text
                }

                _currentWord.value += charToAdd
                updateSuggestions()
            }
        }
    }

    /**
     * Update suggestions based on current word and context
     */
    private fun updateSuggestions() {
        if (_currentWord.value.isNotEmpty()) {
            _suggestions.value = dictionaryProvider.getContextualSuggestions(
                _previousWords.value,
                _currentWord.value,
                3
            )
        } else {
            _suggestions.value = emptyList()
        }
    }

    /**
     * Handle suggestion selection
     */
    fun selectSuggestion(suggestion: String) {
        // Learn the selected word
        dictionaryProvider.learnWord(suggestion)
        addToRecentWords(suggestion)

        // Learn word pair with previous word
        val previousWord = _previousWords.value.lastOrNull()
        if (previousWord != null) {
            dictionaryProvider.learnWordPair(previousWord, suggestion)
        }

        // Update context
        val updatedPrevWords = _previousWords.value.toMutableList()
        updatedPrevWords.add(suggestion)
        if (updatedPrevWords.size > maxPreviousWordsContext) {
            updatedPrevWords.removeAt(0)
        }
        _previousWords.value = updatedPrevWords

        // Reset current word
        _currentWord.value = ""

        // Update suggestions for next words
        _suggestions.value = dictionaryProvider.getContextualSuggestions(
            updatedPrevWords,
            "",
            3
        )
    }

    /**
     * Reset current word tracking
     */
    fun resetCurrentWord() {
        _currentWord.value = ""
        _suggestions.value = emptyList()
    }

    /**
     * Add word to recent words cache
     */
    private fun addToRecentWords(word: String) {
        if (word.isBlank()) return

        // Remove if already in the list (to move it to front)
        recentWords.remove(word)

        // Add to front of list
        recentWords.add(0, word)

        // Trim the list if needed
        if (recentWords.size > maxRecentWords) {
            recentWords.removeAt(recentWords.lastIndex)
        }
    }

    /**
     * Get recently used words for suggestions
     */
    fun getRecentWords(limit: Int = 3): List<String> {
        return recentWords.take(limit)
    }
}
class KeyboardViewModelFactory(private val dictionaryProvider: DictionaryProvider) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KeyboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return KeyboardViewModel(dictionaryProvider) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
