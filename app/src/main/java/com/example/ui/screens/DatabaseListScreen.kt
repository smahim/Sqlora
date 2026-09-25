package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DatabaseItem
import com.example.data.model.DatabaseMetadata
import com.example.data.model.TableMetadata
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.DatabaseListViewModel

@Composable
fun DatabaseListScreen(
    viewModel: DatabaseListViewModel,
    onDatabaseClick: (Long) -> Unit,
    onOpenConsole: (Long?) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var databaseToDelete by remember { mutableStateOf<DatabaseItem?>(null) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> if (uri != null) viewModel.openSafDatabase(uri) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/x-sqlite3")
    ) { uri: Uri? -> if (uri != null) viewModel.createSafDatabase(uri) }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    val filtered = remember(uiState.databases, searchQuery) {
        if (searchQuery.isBlank()) uiState.databases else uiState.databases.filter {
            val q = searchQuery.trim()
            it.name.contains(q, true) || it.filePath.contains(q, true)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "SQLora",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp
                            )
                            Text(
                                "Database Explorer",
                                color = TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { onOpenConsole(null) },
                            modifier = Modifier.testTag("global_sql_console_button")
                        ) {
                            Icon(Icons.Default.Terminal, "SQL Console", tint = TextSecondary)
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("global_settings_button")
                        ) {
                            Icon(Icons.Default.Settings, "Settings", tint = TextSecondary)
                        }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, "More", tint = TextSecondary)
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                ExplorerMenuItem(Icons.Default.FolderOpen, "Open Database") {
                                    menuExpanded = false
                                    openDocumentLauncher.launch(arrayOf("*/*", "application/x-sqlite3", "application/vnd.sqlite3", "application/octet-stream"))
                                }
                                ExplorerMenuItem(Icons.Default.Add, "Create Database") {
                                    menuExpanded = false
                                    viewModel.openCreateDialog()
                                }
                                ExplorerMenuItem(Icons.Default.CreateNewFolder, "Create Database File") {
                                    menuExpanded = false
                                    createDocumentLauncher.launch("new_database.db")
                                }
                                ExplorerMenuItem(Icons.Default.Terminal, "New SQL Console") {
                                    menuExpanded = false
                                    onOpenConsole(null)
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
                Box(
                    Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            ExplorerHeader(
                databaseCount = filtered.size,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onOpenDatabase = {
                    openDocumentLauncher.launch(arrayOf("*/*", "application/x-sqlite3", "application/vnd.sqlite3", "application/octet-stream"))
                },
                onCreateDatabase = { viewModel.openCreateDialog() }
            )

            if (filtered.isEmpty()) {
                ExplorerEmptyState(
                    hasSearch = searchQuery.isNotBlank(),
                    onOpen = {
                        openDocumentLauncher.launch(arrayOf("*/*", "application/x-sqlite3", "application/vnd.sqlite3", "application/octet-stream"))
                    },
                    onCreate = { viewModel.openCreateDialog() }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    item {
                        Text(
                            "DATABASES  ·  ${filtered.size}",
                            modifier = Modifier.padding(start = 10.dp, top = 10.dp, bottom = 6.dp),
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        )
                    }
                    items(filtered, key = { it.id }) { db ->
                        DatabaseTreeNode(
                            database = db,
                            expanded = db.id in uiState.expandedDatabaseIds,
                            metadata = uiState.metadataByDatabaseId[db.id],
                            metadataLoading = db.id in uiState.loadingMetadataIds,
                            onToggle = { viewModel.toggleDatabaseExpanded(db.id) },
                            onOpen = { onDatabaseClick(db.id) },
                            onDelete = { databaseToDelete = db }
                        )
                    }
                }
            }
        }
    }

    if (uiState.showCreateDialog) {
        CreateDatabaseDialog(
            onDismiss = { viewModel.closeCreateDialog() },
            onConfirm = { name, description, initialSql ->
                viewModel.createDatabase(name, description, initialSql)
            }
        )
    }

    databaseToDelete?.let { db ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { databaseToDelete = null },
            title = { Text("Delete database?", color = TextPrimary) },
            text = { Text("Delete ${db.name}? This removes the SQLora entry and may remove the local file.", color = TextSecondary) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.deleteDatabase(db.id, db.name)
                    databaseToDelete = null
                }) { Text("Delete", color = RoseAccent) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { databaseToDelete = null }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun ExplorerMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = TextSecondary) },
        onClick = onClick
    )
}

@Composable
private fun ExplorerHeader(
    databaseCount: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenDatabase: () -> Unit,
    onCreateDatabase: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Workspace", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(
                    if (databaseCount == 1) "1 database" else "$databaseCount databases",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ExplorerActionButton(Icons.Default.FolderOpen, "Open", onOpenDatabase)
                ExplorerActionButton(Icons.Default.Add, "New", onCreateDatabase)
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("search_databases_input"),
            singleLine = true,
            placeholder = { Text("Filter databases...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                { androidx.compose.material3.TextButton(onClick = { onSearchQueryChange("") }) { Text("Clear", fontSize = 11.sp) } }
            } else null,
            shape = RoundedCornerShape(11.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = CyanAccent.copy(alpha = 0.75f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = CyanAccent
            )
        )
    }
}

@Composable
private fun ExplorerActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DatabaseTreeNode(
    database: DatabaseItem,
    expanded: Boolean,
    metadata: DatabaseMetadata?,
    metadataLoading: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    var contextMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(9.dp))
                .background(if (expanded) MaterialTheme.colorScheme.surface else Color.Transparent)
                .border(if (expanded) 1.dp else 0.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), RoundedCornerShape(9.dp))
                .clickable(onClick = onToggle)
                .padding(start = 5.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = TextMuted,
                modifier = Modifier.size(19.dp)
            )
            Icon(Icons.Default.Folder, null, tint = CyanAccent, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(database.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(database.filePath, color = TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("${database.tableCount} tbl", color = TextMuted, fontSize = 10.sp)
            IconButton(onClick = onOpen, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Terminal, "Open SQL console", tint = TextSecondary, modifier = Modifier.size(17.dp))
            }
            Box {
                IconButton(onClick = { contextMenu = true }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.MoreVert, "Database actions", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = contextMenu, onDismissRequest = { contextMenu = false }) {
                    DropdownMenuItem(text = { Text("Open database") }, leadingIcon = { Icon(Icons.Default.Storage, null) }, onClick = { contextMenu = false; onOpen() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { contextMenu = false; onDelete() })
                }
            }
        }

        if (expanded) {
            if (metadataLoading) {
                TreeChildRow("Loading schema…", Icons.Default.Bolt, TextMuted, 1)
            } else if (metadata != null) {
                TreeCategory("Tables", Icons.Default.TableChart, metadata.tables.size, EmeraldAccent)
                metadata.tables.forEach { table ->
                    TreeTableRow(table)
                }
                TreeCategory("Views", Icons.Default.ViewList, metadata.views.size, CyanAccent)
                metadata.views.forEach { view ->
                    TreeChildRow(view.name, Icons.Default.ViewList, TextSecondary, 1)
                }
                val indexes = metadata.tables.flatMap { it.indexes }.distinctBy { it.name }
                TreeCategory("Indexes", Icons.Default.Storage, indexes.size, CyanAccent)
                indexes.forEach { index ->
                    TreeChildRow(index.name, Icons.Default.Storage, TextSecondary, 1)
                }
                TreeCategory("Triggers", Icons.Default.Bolt, metadata.triggers.size, RoseAccent)
                metadata.triggers.forEach { trigger ->
                    TreeChildRow(trigger.name, Icons.Default.Bolt, TextSecondary, 1)
                }
            } else {
                TreeChildRow("Schema unavailable — open database", Icons.Default.FolderOpen, TextMuted, 1)
            }
        }
    }
}

@Composable
private fun TreeCategory(title: String, icon: ImageVector, count: Int, tint: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 36.dp, top = 8.dp, bottom = 2.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(7.dp))
        Text(title, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(count.toString(), color = TextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun TreeTableRow(table: TableMetadata) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 58.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.TableChart, null, tint = EmeraldAccent.copy(alpha = 0.85f), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(7.dp))
        Text(table.name, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${table.rowCount} rows", color = TextMuted, fontSize = 9.sp)
    }
}

@Composable
private fun TreeChildRow(title: String, icon: ImageVector, tint: Color, level: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = (58 + level * 20).dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint.copy(alpha = 0.85f), modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(7.dp))
        Text(title, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ExplorerEmptyState(hasSearch: Boolean, onOpen: () -> Unit, onCreate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Storage, null, tint = CyanAccent.copy(alpha = 0.8f), modifier = Modifier.size(46.dp))
        Spacer(Modifier.height(14.dp))
        Text(if (hasSearch) "No matching databases" else "No databases yet", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            if (hasSearch) "Try a different database name or path." else "Open an existing .db file or create a new SQLite database.",
            color = TextMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
        if (!hasSearch) {
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExplorerActionButton(Icons.Default.FolderOpen, "Open database", onOpen)
                ExplorerActionButton(Icons.Default.Add, "New database", onCreate)
            }
        }
    }
}
