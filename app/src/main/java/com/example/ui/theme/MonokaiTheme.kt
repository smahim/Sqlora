package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Dedicated dark Monokai editor theme matching JetBrains IDEs (IntelliJ / DataGrip).
 * NOTE: This is strictly applied inside the SQL Code Editor and does not bleed
 * into the rest of the application's Material 3 dark theme.
 */
object MonokaiTheme {
    // Editor Surface & Gutter
    val Background = Color(0xFF272822)         // Classic Monokai Dark
    val GutterBackground = Color(0xFF1E1F1C)   // Left gutter for line numbers
    val GutterBorder = Color(0xFF383830)       // Divider between gutter and code
    val LineNumber = Color(0xFF75715E)         // Line number color
    val ActiveLineNumber = Color(0xFFF8F8F2)   // Highlighted line number
    val ActiveLineBackground = Color(0xFF32332B) // Active line subtle highlight
    val Selection = Color(0xFF49483E)          // Text selection background
    val Cursor = Color(0xFFF8F8F0)             // Cursor / Caret color

    // Syntax Highlighting Tokens
    val Keyword = Color(0xFFF92672)            // SELECT, FROM, WHERE, etc. (Vivid Pink)
    val Function = Color(0xFF66D9EF)           // COUNT, SUM, AVG, ROUND, etc. (Cyan)
    val StringLiteral = Color(0xFFE6DB74)      // 'text', "text" (Ochre Yellow)
    val Number = Color(0xFFAE81FF)             // 123, 45.67 (Purple / Lavender)
    val Comment = Color(0xFF75715E)            // -- comment, /* comment */ (Olive Gray)
    val Operator = Color(0xFFF92672)           // =, !=, <, >, +, -, *, / (Pink)
    val Punctuation = Color(0xFFF8F8F2)        // (, ), ,, ; (Soft White)
    val Identifier = Color(0xFFF8F8F2)         // Column / General identifiers (Off-white)
    val Type = Color(0xFF66D9EF)               // INTEGER, TEXT, REAL, BLOB (Cyan)
    val EntityName = Color(0xFFA6E22E)         // Table / View names (Spring Green)

    // Editor Toolbar / Header
    val HeaderBackground = Color(0xFF1E1F1C)
    val HeaderBorder = Color(0xFF383830)
    val TabActiveBackground = Color(0xFF272822)
    val TabActiveIndicator = Color(0xFFF92672)
    val TextMuted = Color(0xFF8F908A)
}
