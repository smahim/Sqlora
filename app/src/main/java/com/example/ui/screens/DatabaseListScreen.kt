package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.example.ui.components.DatabaseCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StudioTopBar
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.SqlFormatUtils
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

    var databaseToDelete by remember { mutableStateOf<DatabaseItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Filter databases based on search query
    val filteredDatabases = remember(uiState.databases, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.databases
        } else {
            val q = searchQuery.trim().lowercase()
            uiState.databases.filter {
                it.name.lowercase().contains(q) ||
                    it.description.lowercase().contains(q) ||
                    it.filePath.lowercase().contains(q)
            }
        }
    }

    // SAF Open Document Launcher
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.openSafDatabase(uri)
        }
    }

    // SAF Create Document Launcher
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-sqlite3")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.createSafDatabase(uri)
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
                title = "SQLora",
                subtitle = "Native SQLite Manager • Storage Access Framework",
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("global_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { onOpenConsole(null) },
                        modifier = Modifier.testTag("global_sql_console_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "SQL Console",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_new_database")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Database"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Modern High-End Developer Studio Overview Deck
            item {
                StatsOverviewCard(
                    databaseCount = uiState.databases.size,
                    totalSize = uiState.totalSizeBytes,
                    totalTables = uiState.totalTables,
                    onOpenSaf = {
                        openDocumentLauncher.launch(
                            arrayOf(
                                "*/*",
                                "application/x-sqlite3",
                                "application/vnd.sqlite3",
                                "application/octet-stream"
                            )
                        )
                    },
                    onCreateSaf = {
                        createDocumentLauncher.launch("new_database.db")
                    },
                    onNewLocalDb = { viewModel.openCreateDialog() },
                    onLoadSamples = { viewModel.seedSampleDatabases() }
                )
            }

            // Quick Database Search / Filter Bar
            if (uiState.databases.isNotEmpty()) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("search_databases_input"),
                        placeholder = {
                            Text(
                                text = "Search databases by name or path...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MANAGED DATABASES",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${filteredDatabases.size} of ${uiState.databases.size} databases",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (uiState.databases.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyStateView(
                        title = "No SQLite Databases Found",
                        subtitle = "Open an existing .db file via Android file picker, create a new SQLite database, or load the starter test databases.",
                        actionButtonText = "Open SQLite File via SAF",
                        onActionClick = {
                            openDocumentLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            } else if (filteredDatabases.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No databases match \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { searchQuery = "" }) {
                            Text("Clear Filter")
                        }
                    }
                }
            } else {
                items(filteredDatabases, key = { it.id }) { db ->
                    DatabaseCard(
                        database = db,
                        onClick = { onDatabaseClick(db.id) },
                        onOpenConsole = { onOpenConsole(db.id) },
                        onDelete = { databaseToDelete = db }
                    )
                }
            }
        }
    }

    if (uiState.showCreateDialog) {
        CreateDatabaseDialog(
            onDismiss = { viewModel.closeCreateDialog() },
            onConfirm = { name, desc, sql ->
                viewModel.createDatabase(name, desc, sql)
            }
        )
    }

    // Confirmation dialog for deleting a database
    databaseToDelete?.let { db ->
        AlertDialog(
            onDismissRequest = { databaseToDelete = null },
            title = {
                Text(
                    text = "Delete Database?",
                    style = MaterialTheme.typography.titleMedium,
                    color = RoseAccent,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove '${db.name}'? If this is a local app database, its tables and data will be deleted permanently.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDatabase(db.id, db.name)
                        databaseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoseAccent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { databaseToDelete = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun StatsOverviewCard(
    databaseCount: Int,
    totalSize: Long,
    totalTables: Int,
    onOpenSaf: () -> Unit,
    onCreateSaf: () -> Unit,
    onNewLocalDb: () -> Unit,
    onLoadSamples: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Studio identity & engine status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "SQLite Developer Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Embedded SQLite 3 Engine • SAF 2.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Live status badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldAccent.copy(alpha = 0.12f))
                        .border(0.5.dp, EmeraldAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(EmeraldAccent)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "ONLINE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldAccent,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3 Segmented Metric Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricDeckTile(
                    title = "Databases",
                    value = "$databaseCount",
                    icon = Icons.Default.Storage,
                    accentColor = CyanAccent,
                    modifier = Modifier.weight(1f)
                )

                MetricDeckTile(
                    title = "Tables",
                    value = "$totalTables",
                    icon = Icons.Default.TableChart,
                    accentColor = EmeraldAccent,
                    modifier = Modifier.weight(1f)
                )

                MetricDeckTile(
                    title = "Storage",
                    value = SqlFormatUtils.formatBytes(totalSize),
                    icon = Icons.Default.DataUsage,
                    accentColor = AmberAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Actions Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenSaf,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("open_saf_file_btn")
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open File (SAF)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = onNewLocalDb,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("create_local_db_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Database", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Actions Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCreateSaf,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("create_saf_btn")
                ) {
                    Icon(Icons.Default.NoteAdd, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create via SAF", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onLoadSamples,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("load_samples_btn")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sample Datasets", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun MetricDeckTile(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 10.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
