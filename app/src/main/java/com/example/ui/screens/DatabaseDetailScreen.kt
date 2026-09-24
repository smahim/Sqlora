package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.data.model.DatabaseSummary
import com.example.data.model.TableInfo
import com.example.data.model.TableMetadata
import com.example.data.model.TriggerMetadata
import com.example.data.model.ViewMetadata
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
import com.example.ui.components.StudioTopBar
import com.example.ui.components.TableCard
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CodeBackground
import com.example.ui.theme.CodeTextStyle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.SqlFormatUtils
import com.example.viewmodel.DatabaseDetailViewModel

@Composable
fun DatabaseDetailScreen(
    viewModel: DatabaseDetailViewModel,
    onBackClick: () -> Unit,
    onTableClick: (tableName: String) -> Unit,
    onOpenConsole: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val db = uiState.database
    val meta = uiState.metadata
    val snackbarHostState = remember { SnackbarHostState() }

    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-sqlite3")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportToSaf(uri)
        }
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            StudioTopBar(
                title = db?.name ?: "Database",
                subtitle = "${meta?.tables?.size ?: 0} tables • ${SqlFormatUtils.formatBytes(db?.sizeBytes ?: 0L)}",
                onBackClick = onBackClick,
                actions = {
                    IconButton(
                        onClick = { viewModel.loadMetadata() },
                        modifier = Modifier.testTag("refresh_meta_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = TextSecondary
                        )
                    }
                    if (db != null) {
                        IconButton(
                            onClick = { onOpenConsole(db.id) },
                            modifier = Modifier.testTag("open_db_console_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "SQL Console",
                                tint = CyanAccent
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (uiState.isLoading && meta == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CyanAccent)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Database Header Info Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = db?.name ?: "Database",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (db?.isSafBacked == true) {
                                    StatusBadge(text = "SAF BACKED", color = AmberAccent, showDot = false)
                                } else if (db?.isSample == true) {
                                    StatusBadge(text = "SAMPLE", color = EmeraldAccent, showDot = false)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = db?.filePath ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action buttons (Console, Export, Sync)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { db?.let { onOpenConsole(it.id) } },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("detail_launch_console_btn")
                        ) {
                            Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SQL Console", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                exportDocumentLauncher.launch("${db?.name ?: "export"}.db")
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("detail_export_saf_btn")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export SAF", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                        }

                        if (db?.isSafBacked == true) {
                            IconButton(
                                onClick = { viewModel.syncSaf() },
                                modifier = Modifier.testTag("sync_saf_button")
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = "Sync SAF", tint = EmeraldAccent)
                            }
                        }
                    }
                }

                // Tab Selector (Tables, Views, Triggers, DB Info)
                TabRow(
                    selectedTabIndex = uiState.activeTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.activeTab]),
                            color = MaterialTheme.colorScheme.primary,
                            height = 2.dp
                        )
                    }
                ) {
                    Tab(
                        selected = uiState.activeTab == 0,
                        onClick = { viewModel.setActiveTab(0) },
                        text = {
                            Text(
                                text = "Tables (${meta?.tables?.size ?: 0})",
                                fontWeight = if (uiState.activeTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.activeTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    Tab(
                        selected = uiState.activeTab == 1,
                        onClick = { viewModel.setActiveTab(1) },
                        text = {
                            Text(
                                text = "Views (${meta?.views?.size ?: 0})",
                                fontWeight = if (uiState.activeTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.activeTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    Tab(
                        selected = uiState.activeTab == 2,
                        onClick = { viewModel.setActiveTab(2) },
                        text = {
                            Text(
                                text = "Triggers (${meta?.triggers?.size ?: 0})",
                                fontWeight = if (uiState.activeTab == 2) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.activeTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    Tab(
                        selected = uiState.activeTab == 3,
                        onClick = { viewModel.setActiveTab(3) },
                        text = {
                            Text(
                                text = "DB Info",
                                fontWeight = if (uiState.activeTab == 3) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.activeTab == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }

                // Tab Content
                Box(modifier = Modifier.weight(1f)) {
                    when (uiState.activeTab) {
                        0 -> TablesTabContent(
                            tables = uiState.filteredTables,
                            searchQuery = uiState.searchQuery,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onTableClick = onTableClick,
                            onOpenConsole = { db?.let { onOpenConsole(it.id) } }
                        )
                        1 -> ViewsTabContent(
                            views = uiState.filteredViews,
                            onOpenConsoleWithSql = { sql -> db?.let { onOpenConsole(it.id) } }
                        )
                        2 -> TriggersTabContent(
                            triggers = meta?.triggers ?: emptyList()
                        )
                        3 -> meta?.summary?.let {
                            DbInfoTabContent(summary = it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TablesTabContent(
    tables: List<TableMetadata>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onTableClick: (String) -> Unit,
    onOpenConsole: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_tables_input"),
                placeholder = { Text("Filter tables...", color = TextMuted) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted)
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
                shape = RoundedCornerShape(8.dp)
            )
        }

        if (tables.isEmpty()) {
            item {
                EmptyStateView(
                    title = "No Tables Found",
                    subtitle = if (searchQuery.isNotBlank()) "No tables matching '$searchQuery'." else "This database has no tables yet. Execute a CREATE TABLE statement in the SQL Console.",
                    actionButtonText = "Open SQL Console",
                    onActionClick = onOpenConsole,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            }
        } else {
            items(tables, key = { it.name }) { table ->
                TableCard(
                    table = TableInfo(
                        name = table.name,
                        type = "TABLE",
                        columnCount = table.columns.size,
                        rowCount = table.rowCount
                    ),
                    onClick = { onTableClick(table.name) }
                )
            }
        }
    }
}

@Composable
private fun ViewsTabContent(
    views: List<ViewMetadata>,
    onOpenConsoleWithSql: (String) -> Unit
) {
    if (views.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No views defined in this database",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(views, key = { it.name }) { view ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = PurpleAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = view.name,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        StatusBadge(
                            text = "${view.columns.size} cols",
                            color = PurpleAccent,
                            showDot = false
                        )
                    }

                    if (view.sql.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = view.sql,
                            style = CodeTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TriggersTabContent(triggers: List<TriggerMetadata>) {
    if (triggers.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No triggers defined in this database",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(triggers, key = { it.name }) { trigger ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = trigger.name,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        StatusBadge(
                            text = "${trigger.timing} ${trigger.event}",
                            color = AmberAccent,
                            showDot = false
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Target Table: ${trigger.targetTable}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (trigger.sql.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = trigger.sql,
                            style = CodeTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DbInfoTabContent(summary: DatabaseSummary) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "SQLITE ENGINE METRICS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
        }

        item {
            InfoMetricRow(label = "SQLite Version", value = summary.sqliteVersion)
            InfoMetricRow(label = "Encoding", value = summary.encoding)
            InfoMetricRow(label = "Journal Mode", value = summary.journalMode.uppercase())
            InfoMetricRow(label = "Page Size", value = "${summary.pageSizeBytes} bytes")
            InfoMetricRow(label = "Page Count", value = "${summary.pageCount} pages")
            InfoMetricRow(label = "Total File Size", value = SqlFormatUtils.formatBytes(summary.fileSizeBytes))
            InfoMetricRow(label = "User Version", value = "${summary.userVersion}")
            InfoMetricRow(label = "Auto Vacuum", value = summary.autoVacuum)
            InfoMetricRow(label = "Discovered Tables", value = "${summary.tableCount}")
            InfoMetricRow(label = "Discovered Views", value = "${summary.viewCount}")
            InfoMetricRow(label = "Discovered Indexes", value = "${summary.indexCount}")
            InfoMetricRow(label = "Discovered Triggers", value = "${summary.triggerCount}")
        }
    }
}

@Composable
private fun InfoMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
