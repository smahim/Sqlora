package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.components.editor.JetBrainsSqlEditor

/**
 * Modern JetBrains-style Monokai SQL Editor Card.
 * Adheres to dark Monokai IDE appearance with JetBrains Mono font, line numbers,
 * syntax highlighting, and persistent font-size settings.
 */
@Composable
fun SqlEditorCard(
    sqlText: String,
    onSqlChange: (String) -> Unit,
    onExecute: () -> Unit,
    onClear: () -> Unit,
    onInsertKeyword: (String) -> Unit,
    isExecuting: Boolean,
    fontSizeSp: Int = 15,
    onFontSizeChange: ((Int) -> Unit)? = null,
    isFullScreen: Boolean = false,
    onToggleFullScreen: (() -> Unit)? = null,
    overrideDarkTheme: Boolean? = null,
    modifier: Modifier = Modifier
) {
    JetBrainsSqlEditor(
        sqlText = sqlText,
        onSqlChange = onSqlChange,
        onExecute = onExecute,
        onClear = onClear,
        onInsertKeyword = onInsertKeyword,
        isExecuting = isExecuting,
        fontSizeSp = fontSizeSp,
        onFontSizeChange = onFontSizeChange,
        isFullScreen = isFullScreen,
        onToggleFullScreen = onToggleFullScreen,
        overrideDarkTheme = overrideDarkTheme,
        modifier = modifier
    )
}
