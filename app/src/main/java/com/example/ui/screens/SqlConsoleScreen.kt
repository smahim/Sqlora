package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.QueryHistoryItem
import com.example.data.model.QueryResult
import com.example.data.model.SavedQueryItem
import com.example.data.preferences.EditorPreferences
import com.example.ui.components.DataTableGrid
import com.example.ui.components.SqlEditorCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.StudioTopBar
import com.example.ui.components.chart.ChartDataExtractor
import com.example.ui.components.chart.SqlChartView
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CodeBackground
import com.example.ui.theme.CodeTextStyle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XcodeBlue
import com.example.util.SqlFormatUtils

enum class OutputViewMode {
    TABLE,
    CHART
}

enum class ConsoleLayoutMode {
    SPLIT,
    FULLSCREEN_EDITOR,
    FULLSCREEN_OUTPUT
}

@Composable
fun SqlConsoleScreen(
    viewModel: com.example.viewmodel.SqlConsoleViewModel,
    editorPreferences: EditorPreferences,
    onBackClick: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val savedQueries by viewModel.savedQueries.collectAsStateWithLifecycle()
    val fontSizeSp by editorPreferences.fontSizeSp.collectAsStateWithLifecycle()
    val themeMode by editorPreferences.themeMode.collectAsStateWithLifecycle()
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        EditorPreferences.THEME_LIGHT -> false
        EditorPreferences.THEME_SYSTEM -> isSystemDark
        else -> true
    }

    var isEditorFullScreen by remember { mutableStateOf(false) }
    var isOutputFullScreen by remember { mutableStateOf(false) }
    var outputViewMode by remember { mutableStateOf(OutputViewMode.TABLE) }

    var dbDropdownExpanded by remember { mutableStateOf(false) }
    var showBookmarkDialog by remember { mutableStateOf(false) }
    var bookmarkTitle by remember { mutableStateOf("") }

    var tabToRename by remember { mutableStateOf<com.example.viewmodel.SqlTab?>(null) }
    var renameTabTitle by remember { mutableStateOf("") }

    val activeDb = uiState.databases.find { it.id == uiState.selectedDatabaseId }

    val layoutMode = when {
        isEditorFullScreen -> ConsoleLayoutMode.FULLSCREEN_EDITOR
        isOutputFullScreen -> ConsoleLayoutMode.FULLSCREEN_OUTPUT
        else -> ConsoleLayoutMode.SPLIT
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (layoutMode == ConsoleLayoutMode.SPLIT) {
                StudioTopBar(
                    title = "SQL Console",
                    subtitle = if (activeDb != null) "${activeDb.name} • SQLite Engine" else "Select database",
                    onBackClick = onBackClick,
                    actions = {
                        // Quick Toggle between Dark (Monokai) & Light (Apple Xcode)
                        IconButton(
                            onClick = {
                                val nextMode = if (isDark) EditorPreferences.THEME_LIGHT else EditorPreferences.THEME_DARK
                                editorPreferences.setThemeMode(nextMode)
                            },
                            modifier = Modifier.testTag("toggle_editor_theme_button")
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDark) "Switch to Apple Xcode (Light)" else "Switch to Monokai (Dark)",
                                tint = if (isDark) AmberAccent else XcodeBlue
                            )
                        }

                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("console_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Editor Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { showBookmarkDialog = true },
                            enabled = uiState.sqlText.isNotBlank(),
                            modifier = Modifier.testTag("bookmark_query_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = "Save Query",
                                tint = if (uiState.sqlText.isNotBlank()) AmberAccent else TextMuted
                            )
                        }

                        // Database Selector Dropdown
                        Box {
                            OutlinedButton(
                                onClick = { dbDropdownExpanded = true },
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("db_selector_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activeDb?.name ?: "Select DB",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            DropdownMenu(
                                expanded = dbDropdownExpanded,
                                onDismissRequest = { dbDropdownExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                uiState.databases.forEach { db ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = db.name,
                                                color = if (db.id == uiState.selectedDatabaseId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (db.id == uiState.selectedDatabaseId) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            viewModel.selectDatabase(db.id)
                                            dbDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = layoutMode,
            transitionSpec = {
                fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(120))
            },
            label = "ConsoleLayoutTransition",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { mode ->
            when (mode) {
                // --- 1. FULL SCREEN CODE EDITOR MODE ---
                ConsoleLayoutMode.FULLSCREEN_EDITOR -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        // Multi-Tab Workspace Header in Fullscreen
                        SqlWorkspaceTabBar(
                            tabs = uiState.tabs,
                            activeTabId = uiState.activeTabId,
                            onSelectTab = { viewModel.selectTab(it) },
                            onCloseTab = { viewModel.closeTab(it) },
                            onStartRename = {
                                tabToRename = it
                                renameTabTitle = it.title
                            },
                            onNewTab = { viewModel.addNewTab() }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        SqlEditorCard(
                            sqlText = uiState.sqlText,
                            onSqlChange = { viewModel.setSqlText(it) },
                            onExecute = { viewModel.executeSql() },
                            onClear = { viewModel.clearSql() },
                            onInsertKeyword = { viewModel.insertKeyword(it) },
                            isExecuting = uiState.isExecuting,
                            fontSizeSp = fontSizeSp,
                            onFontSizeChange = { editorPreferences.setFontSize(it) },
                            isFullScreen = true,
                            onToggleFullScreen = { isEditorFullScreen = false },
                            overrideDarkTheme = isDark,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // --- 2. FULL SCREEN QUERY OUTPUT MODE ---
                ConsoleLayoutMode.FULLSCREEN_OUTPUT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        QueryResultView(
                            queryResult = uiState.queryResult,
                            viewMode = outputViewMode,
                            onViewModeChange = { outputViewMode = it },
                            isFullScreen = true,
                            onToggleFullScreen = { isOutputFullScreen = false }
                        )
                    }
                }

                // --- 3. SPLIT WORKSPACE MODE ---
                ConsoleLayoutMode.SPLIT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp)
                    ) {
                        Spacer(modifier = Modifier.height(6.dp))

                        // Multi-Tab Workspace Header
                        SqlWorkspaceTabBar(
                            tabs = uiState.tabs,
                            activeTabId = uiState.activeTabId,
                            onSelectTab = { viewModel.selectTab(it) },
                            onCloseTab = { viewModel.closeTab(it) },
                            onStartRename = {
                                tabToRename = it
                                renameTabTitle = it.title
                            },
                            onNewTab = { viewModel.addNewTab() }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // SQL Editor Box with Fullscreen Toggle
                        SqlEditorCard(
                            sqlText = uiState.sqlText,
                            onSqlChange = { viewModel.setSqlText(it) },
                            onExecute = { viewModel.executeSql() },
                            onClear = { viewModel.clearSql() },
                            onInsertKeyword = { viewModel.insertKeyword(it) },
                            isExecuting = uiState.isExecuting,
                            fontSizeSp = fontSizeSp,
                            onFontSizeChange = { editorPreferences.setFontSize(it) },
                            isFullScreen = false,
                            onToggleFullScreen = { isEditorFullScreen = true },
                            overrideDarkTheme = isDark
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tabs for Results / History / Saved
                        TabRow(
                            selectedTabIndex = uiState.activeBottomTab,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.activeBottomTab]),
                                    color = MaterialTheme.colorScheme.primary,
                                    height = 2.dp
                                )
                            }
                        ) {
                            Tab(
                                selected = uiState.activeBottomTab == 0,
                                onClick = { viewModel.setActiveTab(0) },
                                text = {
                                    Text(
                                        text = "Results",
                                        fontWeight = if (uiState.activeBottomTab == 0) FontWeight.Bold else FontWeight.Normal,
                                        color = if (uiState.activeBottomTab == 0) CyanAccent else TextSecondary
                                    )
                                }
                            )
                            Tab(
                                selected = uiState.activeBottomTab == 1,
                                onClick = { viewModel.setActiveTab(1) },
                                text = {
                                    Text(
                                        text = "History (${history.size})",
                                        fontWeight = if (uiState.activeBottomTab == 1) FontWeight.Bold else FontWeight.Normal,
                                        color = if (uiState.activeBottomTab == 1) CyanAccent else TextSecondary
                                    )
                                }
                            )
                            Tab(
                                selected = uiState.activeBottomTab == 2,
                                onClick = { viewModel.setActiveTab(2) },
                                text = {
                                    Text(
                                        text = "Bookmarks (${savedQueries.size})",
                                        fontWeight = if (uiState.activeBottomTab == 2) FontWeight.Bold else FontWeight.Normal,
                                        color = if (uiState.activeBottomTab == 2) CyanAccent else TextSecondary
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Tab Content
                        Box(modifier = Modifier.weight(1f)) {
                            when (uiState.activeBottomTab) {
                                0 -> QueryResultView(
                                    queryResult = uiState.queryResult,
                                    viewMode = outputViewMode,
                                    onViewModeChange = { outputViewMode = it },
                                    isFullScreen = false,
                                    onToggleFullScreen = { isOutputFullScreen = true }
                                )
                                1 -> QueryHistoryView(
                                    history = history,
                                    onSelectQuery = {
                                        viewModel.setSqlText(it)
                                        viewModel.setActiveTab(0)
                                    },
                                    onOpenInNewTab = { title, sql ->
                                        viewModel.loadQueryIntoNewTab(title, sql)
                                        viewModel.setActiveTab(0)
                                    },
                                    onDeleteHistoryItem = { viewModel.deleteHistoryItem(it) },
                                    onClearHistory = { viewModel.clearHistory() }
                                )
                                2 -> SavedQueriesView(
                                    savedQueries = savedQueries,
                                    onSelectQuery = {
                                        viewModel.setSqlText(it)
                                        viewModel.setActiveTab(0)
                                    },
                                    onOpenInNewTab = { title, sql ->
                                        viewModel.loadQueryIntoNewTab(title, sql)
                                        viewModel.setActiveTab(0)
                                    },
                                    onDeleteSavedQuery = { viewModel.deleteSavedQuery(it) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showBookmarkDialog) {
        AlertDialog(
            onDismissRequest = { showBookmarkDialog = false },
            title = {
                Text(
                    text = "Save Query Bookmark",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Query Title",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = bookmarkTitle,
                        onValueChange = { bookmarkTitle = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bookmark_title_input"),
                        placeholder = { Text("e.g., Get top customers by spend", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bookmarkTitle.isNotBlank()) {
                            viewModel.bookmarkCurrentQuery(bookmarkTitle.trim())
                            bookmarkTitle = ""
                            showBookmarkDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberAccent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Bookmark", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showBookmarkDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (tabToRename != null) {
        AlertDialog(
            onDismissRequest = { tabToRename = null },
            title = {
                Text(
                    text = "Rename Tab",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "Tab Name",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = renameTabTitle,
                        onValueChange = { renameTabTitle = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rename_tab_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        tabToRename?.let { tab ->
                            if (renameTabTitle.isNotBlank()) {
                                viewModel.renameTab(tab.id, renameTabTitle.trim())
                            }
                        }
                        tabToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { tabToRename = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun QueryResultView(
    queryResult: QueryResult?,
    viewMode: OutputViewMode,
    onViewModeChange: (OutputViewMode) -> Unit,
    isFullScreen: Boolean = false,
    onToggleFullScreen: (() -> Unit)? = null
) {
    if (queryResult == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Run a SQL statement above to view results",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Status & Output Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (queryResult.isSuccess) {
                StatusBadge(
                    text = "${queryResult.executionTimeMs} ms",
                    color = EmeraldAccent
                )
                Spacer(modifier = Modifier.width(6.dp))
                if (queryResult.isSelect) {
                    StatusBadge(
                        text = "${queryResult.rows.size} rows",
                        color = CyanAccent
                    )
                } else {
                    StatusBadge(
                        text = "${queryResult.affectedRows} rows affected",
                        color = AmberAccent
                    )
                }
            } else {
                StatusBadge(
                    text = "SQLITE ERROR",
                    color = RoseAccent
                )
                Spacer(modifier = Modifier.width(6.dp))
                StatusBadge(
                    text = "${queryResult.executionTimeMs} ms",
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Switch between Table and Visualization if SELECT
            if (queryResult.isSuccess && queryResult.isSelect && queryResult.rows.isNotEmpty()) {
                val hasChartSuitability = remember(queryResult) {
                    val analysis = ChartDataExtractor.analyzeColumns(queryResult)
                    ChartDataExtractor.getSuitableChartTypes(analysis, queryResult.rows.size).isNotEmpty()
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = viewMode == OutputViewMode.TABLE,
                        onClick = { onViewModeChange(OutputViewMode.TABLE) },
                        label = { Text("Grid", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = viewMode == OutputViewMode.TABLE,
                            borderColor = if (viewMode == OutputViewMode.TABLE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp).testTag("output_view_table_tab")
                    )

                    FilterChip(
                        selected = viewMode == OutputViewMode.CHART,
                        onClick = { onViewModeChange(OutputViewMode.CHART) },
                        label = { Text("Chart", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedLabelColor = if (hasChartSuitability) AmberAccent else MaterialTheme.colorScheme.primary,
                            selectedLeadingIconColor = if (hasChartSuitability) AmberAccent else MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = viewMode == OutputViewMode.CHART,
                            borderColor = if (viewMode == OutputViewMode.CHART) (if (hasChartSuitability) AmberAccent else MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp).testTag("output_view_chart_tab")
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))
            }

            // Dedicated Full Screen Button for query result
            if (onToggleFullScreen != null) {
                IconButton(
                    onClick = onToggleFullScreen,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag(if (isFullScreen) "exit_fullscreen_output_btn" else "fullscreen_output_btn")
                ) {
                    Icon(
                        imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullScreen) "Exit Full Screen" else "Full Screen Output",
                        tint = CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (!queryResult.isSuccess) {
            // Detailed SQLite Error Card with Explanation & Technical Details
            val error = queryResult.errorDetails
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, RoseAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = RoseAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error?.title ?: "SQLite Execution Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = RoseAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = error?.userExplanation ?: (queryResult.errorMessage ?: "Unknown error"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (!error?.suggestion.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = error.suggestion,
                                style = MaterialTheme.typography.bodySmall,
                                color = AmberAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Technical Message Box
                    Text(
                        text = "TECHNICAL DETAILS:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = error?.technicalMessage ?: queryResult.errorMessage ?: "",
                            style = CodeTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else if (!queryResult.isSelect) {
            // Write / DDL Execution Success
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Executed Successfully",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${queryResult.affectedRows} rows changed in ${queryResult.executionTimeMs} ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            // SELECT Query Result: Render Table or Visualization
            if (viewMode == OutputViewMode.CHART) {
                SqlChartView(
                    queryResult = queryResult,
                    isFullScreen = isFullScreen,
                    onToggleFullScreen = onToggleFullScreen,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                DataTableGrid(
                    columns = queryResult.columns,
                    rows = queryResult.rows,
                    isFullScreen = isFullScreen,
                    onToggleFullScreen = onToggleFullScreen,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun QueryHistoryView(
    history: List<QueryHistoryItem>,
    onSelectQuery: (String) -> Unit,
    onOpenInNewTab: (String, String) -> Unit,
    onDeleteHistoryItem: (Long) -> Unit,
    onClearHistory: () -> Unit
) {
    if (history.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No query history recorded for this database",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Executions (${history.size})",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onClearHistory,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("clear_history_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear History",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(history, key = { it.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .clickable { onSelectQuery(item.sqlQuery) }
                        .testTag("history_item_${item.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusBadge(
                                text = if (item.isSuccess) "${item.executionTimeMs}ms" else "FAILED",
                                color = if (item.isSuccess) EmeraldAccent else RoseAccent,
                                showDot = true
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = SqlFormatUtils.formatTime(item.executedAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))

                            // Open in new tab action
                            IconButton(
                                onClick = { onOpenInNewTab("History #${item.id}", item.sqlQuery) },
                                modifier = Modifier.size(26.dp).testTag("history_open_tab_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Open in New Tab",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Delete history item
                            IconButton(
                                onClick = { onDeleteHistoryItem(item.id) },
                                modifier = Modifier.size(26.dp).testTag("history_delete_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete item",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.sqlQuery,
                            style = CodeTextStyle,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedQueriesView(
    savedQueries: List<SavedQueryItem>,
    onSelectQuery: (String) -> Unit,
    onOpenInNewTab: (String, String) -> Unit,
    onDeleteSavedQuery: (Long) -> Unit
) {
    if (savedQueries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No bookmarked queries. Click the bookmark icon to save current SQL.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(savedQueries, key = { it.id }) { query ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .clickable { onSelectQuery(query.sqlQuery) }
                    .testTag("saved_query_${query.id}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = query.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Open in new tab
                        IconButton(
                            onClick = { onOpenInNewTab(query.title, query.sqlQuery) },
                            modifier = Modifier.size(26.dp).testTag("saved_query_tab_${query.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Open in New Tab",
                                tint = CyanAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Delete query
                        IconButton(
                            onClick = { onDeleteSavedQuery(query.id) },
                            modifier = Modifier.size(26.dp).testTag("saved_query_delete_${query.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Saved Query",
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = query.sqlQuery,
                        style = CodeTextStyle,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SqlWorkspaceTabBar(
    tabs: List<com.example.viewmodel.SqlTab>,
    activeTabId: String,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onStartRename: (com.example.viewmodel.SqlTab) -> Unit,
    onNewTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val isActive = tab.id == activeTabId
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                    .border(
                        width = if (isActive) 1.5.dp else 0.5.dp,
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelectTab(tab.id) }
                    .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                    .testTag("sql_tab_${tab.id}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active indicator dot
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Text(
                    text = tab.title,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                // Rename Icon Button
                IconButton(
                    onClick = { onStartRename(tab) },
                    modifier = Modifier.size(24.dp).testTag("rename_tab_${tab.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename tab",
                        tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(11.dp)
                    )
                }

                // Close Tab Button
                IconButton(
                    onClick = { onCloseTab(tab.id) },
                    modifier = Modifier.size(24.dp).testTag("close_tab_${tab.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close tab",
                        tint = if (isActive) RoseAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // New Tab (+) Button
        IconButton(
            onClick = onNewTab,
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .testTag("new_sql_tab_button")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Query Tab",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
