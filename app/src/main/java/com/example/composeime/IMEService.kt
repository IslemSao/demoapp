package com.example.composeime

import android.inputmethodservice.InputMethodService
import android.view.View
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class IMEService : LifecycleInputMethodService(),
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private lateinit var keyboardViewModel: KeyboardViewModel
    private val dictionaryProvider by lazy { DictionaryProvider(this) }

    // Word tracking
    private var currentWordStart = 0
    private var lastCommittedWord = ""

    override fun onCreateInputView(): View {
        // Initialize ViewModel first
        keyboardViewModel = ViewModelProvider(
            this,
            KeyboardViewModelFactory(dictionaryProvider)
        )[KeyboardViewModel::class.java]

        // Then create the view
        val view = ComposeKeyboardView(this)

        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }

        return view
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
    }

    /**
     * Process key presses and update the current word
     */
    fun handleKeyPress(text: String) {
        // Update ViewModel
        keyboardViewModel.processTextInput(text)

        // Handle space or punctuation for word tracking
        if (text == " " || text.matches(Regex("[.,!?;:]"))) {
            val currentText = currentInputConnection?.getTextBeforeCursor(1000, 0) ?: ""

            // Update last committed word if possible
            if (currentText.isNotEmpty()) {
                val words = currentText.trim().split(Regex("\\s+"))
                if (words.isNotEmpty()) {
                    lastCommittedWord = words.last().replace(Regex("[.,!?;:]"), "")
                }
            }

            // Reset word tracking position
            currentWordStart = currentText.length + 1
        }

        // Commit the text to input field
        currentInputConnection?.commitText(text, 1)
    }

    /**
     * Handle suggestion selection
     */
    fun selectSuggestion(suggestion: String) {
        val currentIC = currentInputConnection ?: return

        // Calculate how much of the current word to delete
        val currentText = currentIC.getTextBeforeCursor(1000, 0) ?: ""
        val currentWordLength = if (currentText.length >= currentWordStart) {
            currentText.length - currentWordStart
        } else {
            0
        }

        if (currentWordLength > 0) {
            // Delete the partial word
            currentIC.deleteSurroundingText(currentWordLength, 0)
        }

        // Insert the full suggestion
        currentIC.commitText(suggestion, 1)

        // Add a space after the suggestion
        currentIC.commitText(" ", 1)

        // Update tracking
        lastCommittedWord = suggestion
        keyboardViewModel.selectSuggestion(suggestion)
        currentWordStart = (currentIC.getTextBeforeCursor(1000, 0)?.length ?: 0) + 1
    }

    /**
     * Delete text (for backspace)
     */
    fun deleteText() {
        val currentIC = currentInputConnection ?: return

        currentIC.deleteSurroundingText(1, 0)

        // Update current word tracking if needed
        val currentText = currentIC.getTextBeforeCursor(1000, 0) ?: ""
        if (currentText.length < currentWordStart) {
            // We've deleted back into a previous word
            currentWordStart = Math.max(0, currentText.length)
            val words = currentText.trim().split(Regex("\\s+"))
            lastCommittedWord = if (words.isNotEmpty()) words.last() else ""
        }

        // Update ViewModel state
        keyboardViewModel.processTextInput("⌫")
    }

    override val viewModelStore: ViewModelStore
        get() = store
    override val lifecycle: Lifecycle
        get() = dispatcher.lifecycle

    // ViewModelStore Methods
    private val store = ViewModelStore()

    // SaveStateRegistry Methods
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    // Provide access to the ViewModel for composition
    fun getKeyboardViewModel(): KeyboardViewModel = keyboardViewModel
}
