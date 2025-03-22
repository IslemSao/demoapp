package com.example.composeime

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import splitties.systemservices.inputMethodManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MultilingualKeyboardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkColors.background
                ) {
                    KeyboardSetupScreen()
                }
            }
        }
    }
}

// Simple dark color palette
object DarkColors {
    val background = Color(0xFF121212)
    val surface = Color(0xFF1E1E1E)
    val primary = Color(0xFF4F81FF)
    val accent = Color(0xFFFFB74D)
    val onBackground = Color(0xFFE0E0E0)
    val onSurface = Color(0xFFCCCCCC)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultilingualKeyboardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = DarkColors.primary,
            onPrimary = Color.White,
            background = DarkColors.background,
            onBackground = DarkColors.onBackground,
            surface = DarkColors.surface,
            onSurface = DarkColors.onSurface,
            tertiary = DarkColors.accent
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardSetupScreen() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Simple header
        Text(
            text = "Multilingual Keyboard",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = DarkColors.onBackground
        )

        Text(
            text = "Support for Asian and African languages",
            fontSize = 14.sp,
            color = DarkColors.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Main content
        MultilingualKeyboardContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultilingualKeyboardContent() {
    val ctx = LocalContext.current
    val (text, setValue) = remember { mutableStateOf(TextFieldValue("Try typing in your language...")) }
    var selectedLanguageGroup by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Setup card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = DarkColors.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Keyboard Setup",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkColors.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        ctx.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkColors.primary
                    )
                ) {
                    Text(
                        text = "Enable Keyboard",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        inputMethodManager.showInputMethodPicker()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DarkColors.primary
                    )
                ) {
                    Text(
                        text = "Select as Default",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Test input card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = DarkColors.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Test Your Keyboard",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkColors.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = setValue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    placeholder = { Text("Start typing to test...") },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkColors.primary,
                        unfocusedBorderColor = DarkColors.onSurface.copy(alpha = 0.3f),
                        focusedTextColor = DarkColors.onBackground,
                        unfocusedTextColor = DarkColors.onBackground.copy(alpha = 0.8f),
                        cursorColor = DarkColors.primary,
                        focusedContainerColor = DarkColors.background,
                        unfocusedContainerColor = DarkColors.background
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sample script examples
                val sampleText = when (selectedLanguageGroup) {
                    0 -> "你好 こんにちは 안녕하세요"
                    1 -> "ሰላም አመሰግናለሁ مرحبا"
                    else -> "Hello World"
                }

                Text(
                    text = "Example: $sampleText",
                    fontSize = 14.sp,
                    color = DarkColors.onSurface.copy(alpha = 0.7f)
                )
            }
        }

    }
}

@Composable
fun LanguageGroupTab(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                color = if (isSelected) DarkColors.primary.copy(alpha = 0.2f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            color = if (isSelected) DarkColors.primary else DarkColors.onSurface.copy(alpha = 0.7f),
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun AsianLanguageOptions() {
    val asianLanguages = listOf(
        "Chinese (Simplified)" to "中文",
        "Chinese (Traditional)" to "繁體中文",
        "Japanese" to "日本語",
        "Korean" to "한국어",
        "Hindi" to "हिन्दी",
        "Thai" to "ไทย",
        "Vietnamese" to "Tiếng Việt"
    )

    LanguageGrid(languages = asianLanguages)
}

@Composable
fun AfricanLanguageOptions() {
    val africanLanguages = listOf(
        "Amharic" to "አማርኛ",
        "Arabic" to "العربية",
        "Hausa" to "Hausa",
        "Swahili" to "Kiswahili",
        "Yoruba" to "Yorùbá",
        "Zulu" to "isiZulu",
        "Afrikaans" to "Afrikaans"
    )

    LanguageGrid(languages = africanLanguages)
}

@Composable
fun OtherLanguageOptions() {
    val otherLanguages = listOf(
        "English" to "English",
        "Spanish" to "Español",
        "French" to "Français",
        "German" to "Deutsch",
        "Russian" to "Русский",
        "Portuguese" to "Português",
        "Italian" to "Italiano"
    )

    LanguageGrid(languages = otherLanguages)
}

@Composable
fun LanguageGrid(languages: List<Pair<String, String>>) {
    Column {
        for (languagePair in languages) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = languagePair.first,
                    fontSize = 14.sp,
                    color = DarkColors.onSurface
                )

                Text(
                    text = languagePair.second,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkColors.primary
                )
            }

            if (languagePair != languages.last()) {
                Divider(
                    color = DarkColors.onSurface.copy(alpha = 0.1f),
                    thickness = 1.dp
                )
            }
        }
    }
}
