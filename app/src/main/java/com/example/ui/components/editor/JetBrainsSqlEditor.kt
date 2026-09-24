package com.example.ui.components.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EditorColorTheme
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonokaiEditorTheme
import com.example.ui.theme.XcodeLightEditorTheme
import com.example.util.SqlFormatUtils

@Composable
fun JetBrainsSqlEditor(
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
    modifier: Modifier = Modifier,
    overrideDarkTheme: Boolean? = null
) {
    val clipboardManager = LocalClipboardManager.current
    val systemDark = isSystemInDarkTheme()
    val isDark = overrideDarkTheme ?: systemDark
    val editorTheme: EditorColorTheme = if (isDark) MonokaiEditorTheme else XcodeLightEditorTheme

    val visualTransformation = remember(editorTheme) {
        SqlSyntaxVisualTransformation(JetBrainsMonoFontFamily, editorTheme)
    }

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = sqlText, selection = TextRange(sqlText.length)))
    }

    // Undo / Redo history stacks
    val undoStack = remember { mutableStateListOf<String>() }
    val redoStack = remember { mutableStateListOf<String>() }

    // Search and Replace states
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var matchCase by remember { mutableStateOf(false) }
    var currentMatchIndex by remember { mutableIntStateOf(0) }

    // Synchronize external SQL changes
    LaunchedEffect(sqlText) {
        if (sqlText != textFieldValue.text) {
            val newSelection = textFieldValue.selection.let { sel ->
                val safeStart = sel.start.coerceIn(0, sqlText.length)
                val safeEnd = sel.end.coerceIn(0, sqlText.length)
                TextRange(safeStart, safeEnd)
            }
            textFieldValue = TextFieldValue(text = sqlText, selection = newSelection)
        }
    }

    // Calculate cursor Line & Column for IDE status bar
    val cursorPosition = textFieldValue.selection.start
    val textUpToCursor = textFieldValue.text.take(cursorPosition)
    val currentLine = textUpToCursor.count { it == '\n' } + 1
    val currentColumn = cursorPosition - textUpToCursor.lastIndexOf('\n').coerceAtLeast(-1)
    val lineCount = maxOf(1, textFieldValue.text.count { it == '\n' } + 1)

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    fun updateEditorText(newText: String, saveToUndo: Boolean = true) {
        if (saveToUndo && textFieldValue.text != newText) {
            if (undoStack.size > 50) undoStack.removeAt(0)
            undoStack.add(textFieldValue.text)
            redoStack.clear()
        }
        val safeSel = TextRange(newText.length.coerceAtMost(textFieldValue.selection.start))
        textFieldValue = TextFieldValue(text = newText, selection = safeSel)
        onSqlChange(newText)
    }

    fun handleUndo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(textFieldValue.text)
            textFieldValue = TextFieldValue(text = prev, selection = TextRange(prev.length))
            onSqlChange(prev)
        }
    }

    fun handleRedo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(textFieldValue.text)
            textFieldValue = TextFieldValue(text = next, selection = TextRange(next.length))
            onSqlChange(next)
        }
    }

    fun handleFormatSql() {
        val formatted = SqlFormatUtils.formatSql(textFieldValue.text)
        if (formatted != textFieldValue.text) {
            updateEditorText(formatted)
        }
    }

    // Search matches calculation
    val searchMatches by remember(textFieldValue.text, searchQuery, matchCase) {
        derivedStateOf {
            if (searchQuery.isEmpty()) {
                emptyList<Int>()
            } else {
                val matches = mutableListOf<Int>()
                val text = textFieldValue.text
                var index = 0
                while (index <= text.length - searchQuery.length) {
                    val found = text.indexOf(searchQuery, index, ignoreCase = !matchCase)
                    if (found == -1) break
                    matches.add(found)
                    index = found + searchQuery.length.coerceAtLeast(1)
                }
                matches
            }
        }
    }

    fun jumpToMatch(index: Int) {
        if (searchMatches.isNotEmpty()) {
            val clamped = index.coerceIn(0, searchMatches.size - 1)
            currentMatchIndex = clamped
            val start = searchMatches[clamped]
            val end = start + searchQuery.length
            textFieldValue = textFieldValue.copy(selection = TextRange(start, end))
        }
    }

    fun replaceCurrentMatch() {
        if (searchMatches.isNotEmpty() && currentMatchIndex in searchMatches.indices) {
            val matchStart = searchMatches[currentMatchIndex]
            val currentText = textFieldValue.text
            val newText = currentText.substring(0, matchStart) + replaceQuery + currentText.substring(matchStart + searchQuery.length)
            updateEditorText(newText)
        }
    }

    fun replaceAllMatches() {
        if (searchQuery.isNotEmpty()) {
            val currentText = textFieldValue.text
            val newText = if (matchCase) {
                currentText.replace(searchQuery, replaceQuery)
            } else {
                currentText.replace(Regex(Regex.escape(searchQuery), RegexOption.IGNORE_CASE), replaceQuery)
            }
            if (newText != currentText) {
                updateEditorText(newText)
            }
        }
    }

    fun insertTextAtCursor(snippet: String) {
        val currentText = textFieldValue.text
        val selStart = textFieldValue.selection.min
        val selEnd = textFieldValue.selection.max
        val before = currentText.substring(0, selStart)
        val after = currentText.substring(selEnd)

        val insertion = if (before.isNotEmpty() && !before.endsWith(" ") && !before.endsWith("\n") &&
            !snippet.startsWith(" ") && !snippet.startsWith(";") && !snippet.startsWith(",")) {
            " $snippet "
        } else {
            "$snippet "
        }

        val newText = before + insertion + after
        val newCursorPos = selStart + insertion.length
        updateEditorText(newText)
        textFieldValue = TextFieldValue(text = newText, selection = TextRange(newCursorPos))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(editorTheme.background)
            .border(1.dp, editorTheme.gutterBorder, RoundedCornerShape(10.dp))
    ) {
        // --- 1. JETBRAINS / XCODE IDE TAB & ACTION TOOLBAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(editorTheme.headerBackground)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Active Tab Indicator
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(editorTheme.tabActiveBackground)
                    .border(0.5.dp, editorTheme.headerBorder, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(editorTheme.tabActiveIndicator)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "query.sql",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = editorTheme.activeLineNumber
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Undo Action
            IconButton(
                onClick = { handleUndo() },
                enabled = undoStack.isNotEmpty(),
                modifier = Modifier.size(28.dp).testTag("editor_undo_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Undo,
                    contentDescription = "Undo",
                    tint = if (undoStack.isNotEmpty()) editorTheme.identifier else editorTheme.headerBorder,
                    modifier = Modifier.size(15.dp)
                )
            }

            // Redo Action
            IconButton(
                onClick = { handleRedo() },
                enabled = redoStack.isNotEmpty(),
                modifier = Modifier.size(28.dp).testTag("editor_redo_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Redo,
                    contentDescription = "Redo",
                    tint = if (redoStack.isNotEmpty()) editorTheme.identifier else editorTheme.headerBorder,
                    modifier = Modifier.size(15.dp)
                )
            }

            // Format / Beautify SQL Action
            IconButton(
                onClick = { handleFormatSql() },
                enabled = textFieldValue.text.isNotBlank(),
                modifier = Modifier.size(28.dp).testTag("editor_format_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = "Beautify SQL",
                    tint = if (textFieldValue.text.isNotBlank()) editorTheme.function else editorTheme.headerBorder,
                    modifier = Modifier.size(15.dp)
                )
            }

            // Find & Replace Toggle
            IconButton(
                onClick = { isSearchOpen = !isSearchOpen },
                modifier = Modifier.size(28.dp).testTag("editor_search_toggle_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Find & Replace",
                    tint = if (isSearchOpen) editorTheme.tabActiveIndicator else editorTheme.textMuted,
                    modifier = Modifier.size(15.dp)
                )
            }

            // Copy Action
            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(textFieldValue.text))
                },
                enabled = textFieldValue.text.isNotBlank(),
                modifier = Modifier.size(28.dp).testTag("copy_sql_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy SQL",
                    tint = if (textFieldValue.text.isNotBlank()) editorTheme.textMuted else editorTheme.headerBorder,
                    modifier = Modifier.size(15.dp)
                )
            }

            // Clear Action
            IconButton(
                onClick = {
                    updateEditorText("")
                    onClear()
                },
                enabled = textFieldValue.text.isNotBlank(),
                modifier = Modifier.size(28.dp).testTag("clear_sql_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear SQL",
                    tint = if (textFieldValue.text.isNotBlank()) editorTheme.textMuted else editorTheme.headerBorder,
                    modifier = Modifier.size(15.dp)
                )
            }

            // Fullscreen Toggle Action
            if (onToggleFullScreen != null) {
                IconButton(
                    onClick = onToggleFullScreen,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag(if (isFullScreen) "exit_fullscreen_editor_btn" else "fullscreen_editor_btn")
                ) {
                    Icon(
                        imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullScreen) "Exit Full Screen" else "Full Screen Editor",
                        tint = editorTheme.function,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        // --- SEARCH & REPLACE COLLAPSIBLE PANEL ---
        if (isSearchOpen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(editorTheme.gutterBackground)
                    .border(0.5.dp, editorTheme.gutterBorder)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Find Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            currentMatchIndex = 0
                            if (it.isNotEmpty()) jumpToMatch(0)
                        },
                        placeholder = { Text("Find in SQL...", fontSize = 11.sp, color = editorTheme.textMuted) },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, color = editorTheme.identifier),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = editorTheme.background,
                            unfocusedContainerColor = editorTheme.background,
                            focusedBorderColor = editorTheme.tabActiveIndicator,
                            unfocusedBorderColor = editorTheme.gutterBorder,
                            cursorColor = editorTheme.cursor
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("search_find_input")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = if (searchQuery.isEmpty()) "" else if (searchMatches.isEmpty()) "0/0" else "${currentMatchIndex + 1}/${searchMatches.size}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = editorTheme.textMuted,
                        modifier = Modifier.width(36.dp)
                    )

                    // Prev match
                    IconButton(
                        onClick = {
                            if (searchMatches.isNotEmpty()) {
                                val prev = if (currentMatchIndex <= 0) searchMatches.size - 1 else currentMatchIndex - 1
                                jumpToMatch(prev)
                            }
                        },
                        enabled = searchMatches.isNotEmpty(),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous Match", tint = editorTheme.identifier, modifier = Modifier.size(18.dp))
                    }

                    // Next match
                    IconButton(
                        onClick = {
                            if (searchMatches.isNotEmpty()) {
                                val next = (currentMatchIndex + 1) % searchMatches.size
                                jumpToMatch(next)
                            }
                        },
                        enabled = searchMatches.isNotEmpty(),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next Match", tint = editorTheme.identifier, modifier = Modifier.size(18.dp))
                    }

                    // Close search
                    IconButton(
                        onClick = { isSearchOpen = false },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Find", tint = editorTheme.textMuted, modifier = Modifier.size(16.dp))
                    }
                }

                // Replace Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replaceQuery,
                        onValueChange = { replaceQuery = it },
                        placeholder = { Text("Replace with...", fontSize = 11.sp, color = editorTheme.textMuted) },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, color = editorTheme.identifier),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = editorTheme.background,
                            unfocusedContainerColor = editorTheme.background,
                            focusedBorderColor = editorTheme.function,
                            unfocusedBorderColor = editorTheme.gutterBorder,
                            cursorColor = editorTheme.cursor
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("search_replace_input")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = { replaceCurrentMatch() },
                        enabled = searchMatches.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = editorTheme.headerBorder,
                            contentColor = editorTheme.identifier
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Replace", fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Button(
                        onClick = { replaceAllMatches() },
                        enabled = searchMatches.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = editorTheme.tabActiveIndicator,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("All", fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily)
                    }
                }
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(editorTheme.gutterBorder)
        )

        // --- 2. CODE EDITOR SURFACE WITH GUTTER & MONOKAI/XCODE SYNTAX ---
        val lineHeight = (fontSizeSp * 1.5f).sp
        val gutterWidth = (maxOf(2, lineCount.toString().length) * 11 + 18).dp

        val editorSurfaceModifier = if (isFullScreen) {
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(editorTheme.background)
                .verticalScroll(verticalScrollState)
        } else {
            Modifier
                .fillMaxWidth()
                .heightIn(min = 130.dp, max = 250.dp)
                .background(editorTheme.background)
                .verticalScroll(verticalScrollState)
        }

        Row(
            modifier = editorSurfaceModifier
        ) {
            // Left Gutter (Line Numbers)
            Column(
                modifier = Modifier
                    .width(gutterWidth)
                    .background(editorTheme.gutterBackground)
                    .padding(vertical = 10.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (line in 1..lineCount) {
                    val isActive = line == currentLine
                    Text(
                        text = "$line",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = (fontSizeSp - 2).coerceAtLeast(10).sp,
                        lineHeight = lineHeight,
                        textAlign = TextAlign.End,
                        color = if (isActive) editorTheme.activeLineNumber else editorTheme.lineNumber,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Gutter Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .heightIn(min = 130.dp)
                    .background(editorTheme.gutterBorder)
            )

            // Right Code Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScrollState)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                if (textFieldValue.text.isEmpty()) {
                    Text(
                        text = "-- Write SQL statement here...\n-- e.g. SELECT * FROM users;",
                        style = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = fontSizeSp.sp,
                            lineHeight = lineHeight,
                            color = editorTheme.comment
                        )
                    )
                }

                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        if (newValue.text != textFieldValue.text) {
                            if (undoStack.size > 50) undoStack.removeAt(0)
                            undoStack.add(textFieldValue.text)
                            redoStack.clear()
                            onSqlChange(newValue.text)
                        }
                        textFieldValue = newValue
                    },
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = fontSizeSp.sp,
                        lineHeight = lineHeight,
                        color = editorTheme.identifier,
                        letterSpacing = 0.sp
                    ),
                    cursorBrush = SolidColor(editorTheme.cursor),
                    visualTransformation = visualTransformation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sql_editor_input")
                )
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(editorTheme.gutterBorder)
        )

        // --- 3. IDE STATUS BAR (Cursor Position, Line Count, Encoding) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(editorTheme.headerBackground)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ln $currentLine, Col $currentColumn",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = editorTheme.textMuted
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "•",
                fontSize = 8.sp,
                color = editorTheme.gutterBorder
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "$lineCount lines",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = editorTheme.textMuted
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (isDark) "Monokai (Dark)" else "Xcode (Light)",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = editorTheme.textMuted
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "SQLite",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = editorTheme.keyword
            )
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(editorTheme.gutterBorder)
        )

        // --- EXECUTE BUTTON FOOTER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(editorTheme.headerBackground)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Font size adjuster
            if (onFontSizeChange != null) {
                IconButton(
                    onClick = { onFontSizeChange((fontSizeSp - 1).coerceAtLeast(12)) },
                    enabled = fontSizeSp > 12,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(Icons.Default.TextDecrease, contentDescription = "Decrease Font", tint = editorTheme.textMuted, modifier = Modifier.size(14.dp))
                }
                Text(
                    text = "${fontSizeSp}sp",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = editorTheme.textMuted
                )
                IconButton(
                    onClick = { onFontSizeChange((fontSizeSp + 1).coerceAtMost(24)) },
                    enabled = fontSizeSp < 24,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(Icons.Default.TextIncrease, contentDescription = "Increase Font", tint = editorTheme.textMuted, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onExecute,
                enabled = !isExecuting && textFieldValue.text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = editorTheme.keyword,
                    contentColor = Color.White,
                    disabledContainerColor = editorTheme.gutterBackground,
                    disabledContentColor = editorTheme.textMuted
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("run_query_button")
            ) {
                if (isExecuting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Executing...",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Run Query",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
