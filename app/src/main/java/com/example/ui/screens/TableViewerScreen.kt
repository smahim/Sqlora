package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.ColumnInfo
import com.example.data.model.ForeignKeyInfo
import com.example.data.model.IndexInfo
import com.example.ui.components.DataTableGrid
import com.example.ui.components.StatusBadge
import com.example.ui.components.StudioTopBar
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.TableHeaderTextStyle
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.TableViewerViewModel

@Composable
fun TableViewerScreen(
    viewModel: TableViewerViewModel,
    onBackClick: () -> Unit,
    onOpenConsoleWithSql: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Data Rows, 1: Schema
    var isFullScreenTable by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (!isFullScreenTable) {
                StudioTopBar(
                    title = uiState.tableName,
                    subtitle = "${uiState.columns.size} columns • offset ${uiState.pageOffset}",
                    onBackClick = onBackClick,
                    actions = {
                        IconButton(
                            onClick = { viewModel.loadData() },
                            modifier = Modifier.testTag("refresh_table_data_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                onOpenConsoleWithSql("SELECT * FROM \"${uiState.tableName}\" LIMIT 50;")
                            },
                            modifier = Modifier.testTag("query_table_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Query Table",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isFullScreenTable) {
                // Tab Selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary,
                            height = 2.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "Data Rows",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "Schema & Constraints",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            if (selectedTab == 0) {
                // DATA ROWS VIEW
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = if (isFullScreenTable) 8.dp else 12.dp, vertical = 6.dp)
                ) {
                    if (!isFullScreenTable) {
                        // Filter bar + Pagination controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = uiState.filterText,
                                onValueChange = { viewModel.setFilterText(it) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("table_rows_filter_input"),
                                placeholder = { Text("Filter rows...", fontSize = 12.sp, color = TextMuted) },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = { viewModel.prevPage() },
                                enabled = uiState.pageOffset > 0,
                                modifier = Modifier.testTag("prev_page_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous Page",
                                    tint = if (uiState.pageOffset > 0) CyanAccent else TextMuted
                                )
                            }

                            IconButton(
                                onClick = { viewModel.nextPage() },
                                modifier = Modifier.testTag("next_page_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next Page",
                                    tint = CyanAccent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CyanAccent)
                        }
                    } else {
                        val result = uiState.queryResult
                        val pkColumns = remember(uiState.columns) {
                            uiState.columns.filter { it.isPrimaryKey }.map { it.name }.toSet()
                        }
                        DataTableGrid(
                            columns = result?.columns ?: uiState.columns.map { it.name },
                            rows = uiState.displayedRows,
                            primaryKeyColumns = pkColumns,
                            isFullScreen = isFullScreenTable,
                            onToggleFullScreen = { isFullScreenTable = !isFullScreenTable },
                            emptyMessage = if (uiState.filterText.isNotBlank()) "No rows matching filter" else "Table is empty",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                // SCHEMA, FOREIGN KEYS & INDEXES VIEW
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Columns Section
                    item {
                        Text(
                            text = "COLUMNS & TYPES",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                    }

                    items(uiState.columns, key = { it.cid }) { col ->
                        ColumnSchemaRow(column = col)
                    }

                    // Foreign Keys Section
                    if (uiState.foreignKeys.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "FOREIGN KEY CONSTRAINTS",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                        }

                        items(uiState.foreignKeys, key = { "${it.id}_${it.seq}" }) { fk ->
                            ForeignKeyRow(fk = fk)
                        }
                    }

                    // Indexes Section
                    if (uiState.indexes.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "INDEXES",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                        }

                        items(uiState.indexes, key = { it.name }) { idx ->
                            IndexRow(idx = idx)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnSchemaRow(column: ColumnInfo) {
    val borderColor = if (column.isPrimaryKey) AmberAccent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
    val bgColor = if (column.isPrimaryKey) AmberAccent.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.5f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (column.isPrimaryKey) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Primary Key",
                        tint = AmberAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                }
                Text(
                    text = column.name,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (column.isPrimaryKey) AmberAccent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (column.defaultValue != null) {
                Text(
                    text = "default: ${column.defaultValue}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        Text(
            text = column.type,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )

        Row(
            modifier = Modifier.weight(1.2f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (column.isPrimaryKey) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(AmberAccent.copy(alpha = 0.15f))
                        .border(0.5.dp, AmberAccent.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = AmberAccent,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("PK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AmberAccent)
                }
            }
            if (column.isNotNull) {
                StatusBadge(text = "NN", color = RoseAccent, showDot = false)
            }
        }
    }
}

@Composable
private fun ForeignKeyRow(fk: ForeignKeyInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CompareArrows,
            contentDescription = null,
            tint = PurpleAccent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${fk.fromColumn} → ${fk.targetTable}(${fk.toColumn})",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "ON UPDATE ${fk.onUpdate} • ON DELETE ${fk.onDelete}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun IndexRow(idx: IndexInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.FilterList,
            contentDescription = null,
            tint = EmeraldAccent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = idx.name,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (idx.isUnique) {
                    Spacer(modifier = Modifier.width(6.dp))
                    StatusBadge(text = "UNIQUE", color = EmeraldAccent, showDot = false)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Columns: (${idx.columns.joinToString(", ")})",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
