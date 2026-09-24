package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EditorPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _fontSizeSp = MutableStateFlow(
        prefs.getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE)
    )
    val fontSizeSp: StateFlow<Int> = _fontSizeSp.asStateFlow()

    private val _showLineNumbers = MutableStateFlow(
        prefs.getBoolean(KEY_SHOW_LINE_NUMBERS, true)
    )
    val showLineNumbers: StateFlow<Boolean> = _showLineNumbers.asStateFlow()

    private val _themeMode = MutableStateFlow(
        prefs.getString(KEY_THEME_MODE, THEME_DARK) ?: THEME_DARK
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setFontSize(sizeSp: Int) {
        val validSize = sizeSp.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
        prefs.edit().putInt(KEY_FONT_SIZE, validSize).apply()
        _fontSizeSp.value = validSize
    }

    fun setShowLineNumbers(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_LINE_NUMBERS, show).apply()
        _showLineNumbers.value = show
    }

    fun setThemeMode(mode: String) {
        val validMode = if (mode in listOf(THEME_DARK, THEME_LIGHT, THEME_SYSTEM)) mode else THEME_DARK
        prefs.edit().putString(KEY_THEME_MODE, validMode).apply()
        _themeMode.value = validMode
    }

    companion object {
        private const val PREFS_NAME = "sql_editor_preferences"
        private const val KEY_FONT_SIZE = "editor_font_size_sp"
        private const val KEY_SHOW_LINE_NUMBERS = "editor_show_line_numbers"
        private const val KEY_THEME_MODE = "app_theme_mode"

        const val THEME_DARK = "DARK"
        const val THEME_LIGHT = "LIGHT"
        const val THEME_SYSTEM = "SYSTEM"

        const val DEFAULT_FONT_SIZE = 15
        const val MIN_FONT_SIZE = 12
        const val MAX_FONT_SIZE = 24

        val AVAILABLE_FONT_SIZES = listOf(12, 13, 14, 15, 16, 18, 20, 22, 24)

        @Volatile
        private var instance: EditorPreferences? = null

        fun getInstance(context: Context): EditorPreferences {
            return instance ?: synchronized(this) {
                instance ?: EditorPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
