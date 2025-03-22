package com.example.composeime

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun KeyboardScreen(
    onKeyPressed: (String) -> Unit
) {
    var isUpperCase by remember { mutableStateOf(false) }
    var isSymbolsMode by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State for continuous deletion
    var isDeleting by remember { mutableStateOf(false) }

    // Shared state for long press popup
    var showLongPressPopup by remember { mutableStateOf(false) }
    var longPressAlternativesList by remember { mutableStateOf<List<String>>(emptyList()) }
    var onCharSelected by remember { mutableStateOf<(String) -> Unit>({}) }

    // Function for continuous deletion - use explicit () -> Unit type
    val startContinuousDelete: () -> Unit = {
        isDeleting = true
        coroutineScope.launch {
            // Initial delay before rapid deletion starts
            delay(500)
            while (isDeleting) {
                onKeyPressed("⌫")
                delay(50) // 20 deletions per second
            }
        }
    }

    val stopContinuousDelete: () -> Unit = {
        isDeleting = false
    }

    // Fixed height container to prevent jumping
    Box(
        modifier = Modifier
            .background(Color(0xFF212121))
            .fillMaxWidth()
            .height(280.dp) // Fixed height for keyboard
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        // Long press popup
        if (showLongPressPopup) {
            LongPressPopup(
                alternatives = longPressAlternativesList,
                onCharSelected = { selectedChar ->
                    onCharSelected(selectedChar)
                    onKeyPressed(selectedChar) // Notify parent about key press
                    showLongPressPopup = false
                },
                onDismissRequest = {
                    showLongPressPopup = false
                }
            )
        }

        // Use Crossfade for smoother transitions
        Crossfade(
            targetState = isSymbolsMode,
            animationSpec = tween(durationMillis = 200)
        ) { isSymbols ->
            if (!isSymbols) {
                LetterKeyboard(
                    isUpperCase = isUpperCase,
                    onUpperCaseChanged = { isUpperCase = it },
                    onSwitchToSymbols = { isSymbolsMode = true },
                    onKeyPressed = onKeyPressed,
                    showLongPressPopup = { alternatives, onSelected ->
                        longPressAlternativesList = alternatives
                        onCharSelected = onSelected
                        showLongPressPopup = true
                    },
                    onDeletePressed = { onKeyPressed("⌫") },
                    startContinuousDelete = startContinuousDelete,
                    stopContinuousDelete = stopContinuousDelete
                )
            } else {
                SymbolKeyboard(
                    onSwitchToLetters = { isSymbolsMode = false },
                    onKeyPressed = onKeyPressed,
                    showLongPressPopup = { alternatives, onSelected ->
                        longPressAlternativesList = alternatives
                        onCharSelected = onSelected
                        showLongPressPopup = true
                    },
                    onDeletePressed = { onKeyPressed("⌫") },
                    startContinuousDelete = startContinuousDelete,
                    stopContinuousDelete = stopContinuousDelete
                )
            }
        }
    }
}

// Map of long press options for each key
val longPressAlternatives = mapOf(
    "a" to listOf("à", "á", "â", "ä", "æ", "ã", "å", "ā"),
    "c" to listOf("ç", "ć", "č"),
    "e" to listOf("è", "é", "ê", "ë", "ē", "ė", "ę"),
    "i" to listOf("ì", "í", "î", "ï", "ī", "į"),
    "l" to listOf("ł"),
    "n" to listOf("ñ", "ń"),
    "o" to listOf("ò", "ó", "ô", "ö", "õ", "ø", "ō", "œ"),
    "s" to listOf("ś", "š", "ß"),
    "u" to listOf("ù", "ú", "û", "ü", "ū", "ů"),
    "y" to listOf("ÿ"),
    "z" to listOf("ž", "ź", "ż"),

    // Uppercase alternatives
    "A" to listOf("À", "Á", "Â", "Ä", "Æ", "Ã", "Å", "Ā"),
    "C" to listOf("Ç", "Ć", "Č"),
    "E" to listOf("È", "É", "Ê", "Ë", "Ē", "Ė", "Ę"),
    "I" to listOf("Ì", "Í", "Î", "Ï", "Ī", "Į"),
    "L" to listOf("Ł"),
    "N" to listOf("Ñ", "Ń"),
    "O" to listOf("Ò", "Ó", "Ô", "Ö", "Õ", "Ø", "Ō", "Œ"),
    "S" to listOf("Ś", "Š"),
    "U" to listOf("Ù", "Ú", "Û", "Ü", "Ū", "Ů"),
    "Y" to listOf("Ÿ"),
    "Z" to listOf("Ž", "Ź", "Ż"),

    // Numbers and symbols
    "0" to listOf("⁰", "°"),
    "1" to listOf("¹", "½", "⅓", "¼"),
    "2" to listOf("²", "⅔"),
    "3" to listOf("³", "¾", "⅜"),
    "4" to listOf("⁴"),
    "5" to listOf("⁵"),
    "7" to listOf("⁷"),
    "8" to listOf("⁸"),
    "9" to listOf("⁹"),
    "." to listOf("…", "•", "·"),
    "," to listOf("‚", "„"),
    "-" to listOf("—", "–", "•", "·"),
    "(" to listOf("[", "{", "<"),
    ")" to listOf("]", "}", ">"),
    "/" to listOf("\\", "|"),
    "?" to listOf("¿"),
    "!" to listOf("¡"),
    "$" to listOf("¢", "€", "£", "¥", "₹", "₽"),
    "'" to listOf(""", """, "„", "«", "»", "'", "'"),
    "\"" to listOf("«", "»", """, """),

    // Special for Space
    "Space" to listOf("Fast Delete")
)

@Composable
fun LetterKeyboard(
    isUpperCase: Boolean,
    onUpperCaseChanged: (Boolean) -> Unit,
    onSwitchToSymbols: () -> Unit,
    onKeyPressed: (String) -> Unit,
    showLongPressPopup: (List<String>, (String) -> Unit) -> Unit,
    onDeletePressed: () -> Unit,
    startContinuousDelete: () -> Unit,
    stopContinuousDelete: () -> Unit
) {
    val numberRow = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val topRow = if (isUpperCase)
        arrayOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
    else
        arrayOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")

    val middleRow = if (isUpperCase)
        arrayOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
    else
        arrayOf("a", "s", "d", "f", "g", "h", "j", "k", "l")

    val bottomRow = if (isUpperCase)
        arrayOf("Z", "X", "C", "V", "B", "N", "M")
    else
        arrayOf("z", "x", "c", "v", "b", "n", "m")

    Column {
        // Number row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            numberRow.forEach { key ->
                KeyboardKey(
                    keyboardKey = key,
                    modifier = Modifier.weight(1f),
                    keyColor = Color(0xFF333333),
                    onKeyClicked = { onKeyPressed(key) },
                    showLongPressPopup = showLongPressPopup
                )
            }
        }

        // Top row (QWERTYUIOP)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            topRow.forEach { key ->
                KeyboardKey(
                    keyboardKey = key,
                    modifier = Modifier.weight(1f),
                    keyColor = Color(0xFF424242),
                    onKeyClicked = { onKeyPressed(key) },
                    showLongPressPopup = showLongPressPopup
                )
            }
        }

        // Middle row (ASDFGHJKL)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Spacer(modifier = Modifier.weight(0.5f))
            middleRow.forEach { key ->
                KeyboardKey(
                    keyboardKey = key,
                    modifier = Modifier.weight(1f),
                    keyColor = Color(0xFF424242),
                    onKeyClicked = { onKeyPressed(key) },
                    showLongPressPopup = showLongPressPopup
                )
            }
            // Continuous delete backspace key
            ContinuousDeleteKey(
                modifier = Modifier.weight(1.5f),
                keyColor = Color(0xFF333333),
                onDeletePressed = onDeletePressed,
                startContinuousDelete = startContinuousDelete,
                stopContinuousDelete = stopContinuousDelete
            )
        }

        // Bottom row (ZXCVBNM)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Shift key
            SpecialKey(
                modifier = Modifier.weight(1.5f),
                keyColor = if (isUpperCase) Color(0xFF4CAF50) else Color(0xFF333333),
                onKeyClicked = { onUpperCaseChanged(!isUpperCase) }
            ) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Shift")
            }

            bottomRow.forEach { key ->
                KeyboardKey(
                    keyboardKey = key,
                    modifier = Modifier.weight(1f),
                    keyColor = Color(0xFF424242),
                    onKeyClicked = { onKeyPressed(key) },
                    showLongPressPopup = showLongPressPopup
                )
            }

            // Enter key
            SpecialKey(
                modifier = Modifier.weight(1.5f),
                keyColor = Color(0xFF333333),
                onKeyClicked = { onKeyPressed("\n") }
            ) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Enter")
            }
        }

        // Space bar row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            KeyboardKey(
                keyboardKey = "?123",
                modifier = Modifier.weight(2f),
                keyColor = Color(0xFF333333),
                onKeyClicked = onSwitchToSymbols,
                showLongPressPopup = showLongPressPopup
            )

            // Space bar
            KeyboardKey(
                keyboardKey = "Space",
                modifier = Modifier.weight(6f),
                keyColor = Color(0xFF424242),
                onKeyClicked = { onKeyPressed(" ") },
                showLongPressPopup = showLongPressPopup
            )

            KeyboardKey(
                keyboardKey = ".",
                modifier = Modifier.weight(1f),
                keyColor = Color(0xFF333333),
                onKeyClicked = { onKeyPressed(".") },
                showLongPressPopup = showLongPressPopup
            )

            KeyboardKey(
                keyboardKey = ",",
                modifier = Modifier.weight(1f),
                keyColor = Color(0xFF333333),
                onKeyClicked = { onKeyPressed(",") },
                showLongPressPopup = showLongPressPopup
            )
        }
    }
}

@Composable
fun SymbolKeyboard(
    onSwitchToLetters: () -> Unit,
    onKeyPressed: (String) -> Unit,
    showLongPressPopup: (List<String>, (String) -> Unit) -> Unit,
    onDeletePressed: () -> Unit,
    startContinuousDelete: () -> Unit,
    stopContinuousDelete: () -> Unit
) {
    // Define symbol layouts
    val symbols1Row = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val symbols2Row = arrayOf("@", "#", "$", "%", "&", "-", "+", "(", ")")
    val symbols3Row = arrayOf("*", "\"", "'", ":", ";", "!", "?", "/")
    val symbols4Row = arrayOf("~", "`", "^", "|", "=", "<", ">", "{", "}")

    Column {
        // First row of symbols
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            symbols1Row.forEach { key ->
                KeyboardKey(
                    keyboardKey = key,
                    modifier = Modifier.weight(1f),
                    keyColor = Color(0xFF333333),
                    onKeyClicked = { onKeyPressed(key) },
                    showLongPressPopup = showLongPressPopup
                )
            }
        }

        // Second row of symbols
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            symbols2Row.forEach { key ->
                KeyboardKey(
                    keyboardKey = key,
                    modifier = Modifier.weight(1f),
                    keyColor = Color(0xFF333333),
                    onKeyClicked = { onKeyPressed(key) },
                    showLongPressPopup = showLongPressPopup
                )
            }
        }

        // Third row of symbols
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            symbols3Row.forEach { key ->
                KeyboardKey(
                    keyboardKey = key,
                    modifier = Modifier.weight(1f),
                    keyColor = Color(0xFF333333),
                    onKeyClicked = { onKeyPressed(key) },
                    showLongPressPopup = showLongPressPopup
                )
            }

            // Continuous delete backspace key
            ContinuousDeleteKey(
                modifier = Modifier.weight(1f),
                keyColor = Color(0xFF333333),
                onDeletePressed = onDeletePressed,
                startContinuousDelete = startContinuousDelete,
                stopContinuousDelete = stopContinuousDelete
            )
        }

        // Fourth row with additional symbols
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            symbols4Row.forEach { key ->
                KeyboardKey(
                    keyboardKey = key,
                    modifier = Modifier.weight(1f),
                    keyColor = Color(0xFF333333),
                    onKeyClicked = { onKeyPressed(key) },
                    showLongPressPopup = showLongPressPopup
                )
            }

            // Enter key
            SpecialKey(
                modifier = Modifier.weight(1f),
                keyColor = Color(0xFF333333),
                onKeyClicked = { onKeyPressed("\n") }
            ) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Enter")
            }
        }

        // Space bar row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            KeyboardKey(
                keyboardKey = "ABC",
                modifier = Modifier.weight(2f),
                keyColor = Color(0xFF333333),
                onKeyClicked = onSwitchToLetters,
                showLongPressPopup = showLongPressPopup
            )

            // Space bar
            KeyboardKey(
                keyboardKey = "Space",
                modifier = Modifier.weight(6f),
                keyColor = Color(0xFF424242),
                onKeyClicked = { onKeyPressed(" ") },
                showLongPressPopup = showLongPressPopup
            )

            KeyboardKey(
                keyboardKey = "_",
                modifier = Modifier.weight(1f),
                keyColor = Color(0xFF333333),
                onKeyClicked = { onKeyPressed("_") },
                showLongPressPopup = showLongPressPopup
            )

            KeyboardKey(
                keyboardKey = "=",
                modifier = Modifier.weight(1f),
                keyColor = Color(0xFF333333),
                onKeyClicked = { onKeyPressed("=") },
                showLongPressPopup = showLongPressPopup
            )
        }
    }
}

@Composable
fun ContinuousDeleteKey(
    modifier: Modifier,
    keyColor: Color = Color(0xFF333333),
    onDeletePressed: () -> Unit,
    startContinuousDelete: () -> Unit,
    stopContinuousDelete: () -> Unit
) {
    val elevation by animateDpAsState(
        targetValue = 4.dp,
        label = "KeyElevation"
    )

    Box(
        modifier = modifier.padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .shadow(elevation, RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .background(keyColor)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            // Initial delete
                            onDeletePressed()

                            // Start continuous deletion
                            startContinuousDelete()

                            // Wait for release
                            try {
                                awaitRelease()
                            } finally {
                                // Stop continuous deletion when released
                                stopContinuousDelete()
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⌫",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun KeyboardKey(
    keyboardKey: String,
    modifier: Modifier,
    keyColor: Color = Color(0xFF424242),
    onKeyClicked: (() -> Unit)? = null,
    showLongPressPopup: (List<String>, (String) -> Unit) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()
    val coroutineScope = rememberCoroutineScope()

    // Check if this key has long press alternatives
    val hasLongPressOptions = longPressAlternatives.containsKey(keyboardKey)

    val elevation by animateDpAsState(
        targetValue = if (isPressed.value) 1.dp else 4.dp,
        label = "KeyElevation"
    )

    Box(
        modifier = modifier.padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .shadow(elevation, RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .background(keyColor)
                .pointerInput(keyboardKey) {
                    detectTapGestures(
                        onPress = { offset ->
                            if (hasLongPressOptions) {
                                val pressJob = coroutineScope.launch {
                                    delay(500) // 500ms delay for long press

                                    // Get the alternatives for this key
                                    val alternatives =
                                        longPressAlternatives[keyboardKey] ?: emptyList()

                                    // Show the popup with alternatives
                                    showLongPressPopup(alternatives) { selectedChar ->
                                        // Handle the selection (default behavior if not overridden)
                                        onKeyClicked?.invoke()
                                    }
                                }

                                tryAwaitRelease()

                                // Only cancel the job if a press is released quickly (before long press)
                                if (!pressJob.isCompleted && !pressJob.isCancelled) {
                                    pressJob.cancel()
                                    // Normal click behavior
                                    onKeyClicked?.invoke()
                                }
                            } else {
                                // No long press options, just handle as normal click
                                tryAwaitRelease()
                                onKeyClicked?.invoke()
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Display text according to key type
            when (keyboardKey) {
                "Space" -> Text(
                    text = "Space",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                else -> Text(
                    text = keyboardKey,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun LongPressPopup(
    alternatives: List<String>,
    onCharSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    Popup(
        alignment = Alignment.TopCenter,
        onDismissRequest = onDismissRequest
    ) {
        Card(
            elevation = 8.dp,
            backgroundColor = Color(0xFF303030),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .padding(8.dp)
                .clickable(enabled = false) { /* Prevent clicks from passing through */ }
        ) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                alternatives.forEach { char ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF424242))
                            .clickable { onCharSelected(char) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpecialKey(
    modifier: Modifier,
    keyColor: Color = Color(0xFF333333),
    onKeyClicked: () -> Unit,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()

    val elevation by animateDpAsState(
        targetValue = if (isPressed.value) 1.dp else 4.dp,
        label = "KeyElevation"
    )

    Box(
        modifier = modifier.padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .shadow(elevation, RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .background(keyColor)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onKeyClicked
                ),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
