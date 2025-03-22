package com.example.composeime

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.AbstractComposeView

class ComposeKeyboardView(context: Context) : AbstractComposeView(context) {
    private val imeService = context as IMEService

    @Composable
    override fun Content() {
        // Get the ViewModel safely within the composition
        val viewModel = imeService.getKeyboardViewModel()

        // Collect state from ViewModel
        val suggestions by viewModel.suggestions.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()

        Column {
            // Add suggestion bar at the top
            SuggestionBar(
                suggestions = suggestions,
                onSuggestionClick = { suggestion ->
                    imeService.selectSuggestion(suggestion)
                },
                isLoading = isLoading
            )

            // Main keyboard layout
            KeyboardScreen(
                onKeyPressed = { key ->
                    when (key) {
                        "⌫" -> imeService.deleteText()
                        else -> imeService.handleKeyPress(key)
                    }
                }
            )
        }
    }
}
