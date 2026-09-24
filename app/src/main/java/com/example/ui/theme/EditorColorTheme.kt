package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Common interface for SQL Editor color schemes.
 * Supports JetBrains Monokai (Dark) and Apple Xcode (Light).
 */
interface EditorColorTheme {
    val background: Color
    val gutterBackground: Color
    val gutterBorder: Color
    val lineNumber: Color
    val activeLineNumber: Color
    val activeLineBackground: Color
    val selection: Color
    val cursor: Color

    // Syntax tokens
    val keyword: Color
    val function: Color
    val stringLiteral: Color
    val number: Color
    val comment: Color
    val operator: Color
    val punctuation: Color
    val identifier: Color
    val type: Color
    val entityName: Color

    // Header / tabs
    val headerBackground: Color
    val headerBorder: Color
    val tabActiveBackground: Color
    val tabActiveIndicator: Color
    val textMuted: Color
    val isDark: Boolean
}

/**
 * JetBrains Monokai Editor Theme (Dark Mode)
 */
object MonokaiEditorTheme : EditorColorTheme {
    override val background = Color(0xFF272822)
    override val gutterBackground = Color(0xFF1E1F1C)
    override val gutterBorder = Color(0xFF383830)
    override val lineNumber = Color(0xFF75715E)
    override val activeLineNumber = Color(0xFFF8F8F2)
    override val activeLineBackground = Color(0xFF32332B)
    override val selection = Color(0xFF49483E)
    override val cursor = Color(0xFFF8F8F0)

    override val keyword = Color(0xFFF92672)
    override val function = Color(0xFF66D9EF)
    override val stringLiteral = Color(0xFFE6DB74)
    override val number = Color(0xFFAE81FF)
    override val comment = Color(0xFF75715E)
    override val operator = Color(0xFFF92672)
    override val punctuation = Color(0xFFF8F8F2)
    override val identifier = Color(0xFFF8F8F2)
    override val type = Color(0xFF66D9EF)
    override val entityName = Color(0xFFA6E22E)

    override val headerBackground = Color(0xFF1E1F1C)
    override val headerBorder = Color(0xFF383830)
    override val tabActiveBackground = Color(0xFF272822)
    override val tabActiveIndicator = Color(0xFFF92672)
    override val textMuted = Color(0xFF8F908A)
    override val isDark = true
}

/**
 * Apple Xcode-Inspired Clean Light Code Editor Theme
 * Clean white background, high contrast, professional typography & syntax tokens.
 */
object XcodeLightEditorTheme : EditorColorTheme {
    override val background = Color(0xFFFFFFFF)
    override val gutterBackground = Color(0xFFF8F9FA)
    override val gutterBorder = Color(0xFFE5E7EB)
    override val lineNumber = Color(0xFF9CA3AF)
    override val activeLineNumber = Color(0xFF1F2937)
    override val activeLineBackground = Color(0xFFEDF5FF)
    override val selection = Color(0xFFB4D8FD)
    override val cursor = Color(0xFF0066CC)

    override val keyword = Color(0xFF9B2393)      // Xcode signature magenta
    override val function = Color(0xFF3900A0)     // Xcode deep indigo
    override val stringLiteral = Color(0xFFC41A16)// Xcode crimson red
    override val number = Color(0xFF1C00CF)       // Xcode cobalt blue
    override val comment = Color(0xFF5D6C79)      // Xcode slate gray
    override val operator = Color(0xFF0F172A)     // Dark slate
    override val punctuation = Color(0xFF334155)  // Slate
    override val identifier = Color(0xFF1E293B)   // High contrast slate
    override val type = Color(0xFF4B2185)         // Rich purple
    override val entityName = Color(0xFF23575C)   // Deep teal

    override val headerBackground = Color(0xFFF1F5F9)
    override val headerBorder = Color(0xFFE2E8F0)
    override val tabActiveBackground = Color(0xFFFFFFFF)
    override val tabActiveIndicator = Color(0xFF007AFF)
    override val textMuted = Color(0xFF64748B)
    override val isDark = false
}

fun getEditorColorTheme(isDark: Boolean): EditorColorTheme {
    return if (isDark) MonokaiEditorTheme else XcodeLightEditorTheme
}
