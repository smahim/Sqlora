package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.util.ExportUtils
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TableCellTextStyle
import com.example.ui.theme.TableHeaderTextStyle
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class SortDirection { ASC, DESC, NONE }

private fun compareValuesWithNumeric(a: String?, b: String?): Int {
    if (a == null && b == null) return 0
    if (a == null) return -1
    if (b == null) return 1
    val aLong = a.toLongOrNull()
    val bLong = b.toLongOrNull()
    if (aLong != null && bLong != null) return aLong.compareTo(bLong)
    val aDouble = a.toDoubleOrNull()
    val bDouble = b.toDoubleOrNull()
    if (aDouble != null && bDouble != null) return aDouble.compareTo(bDouble)
    return a.compareTo(b, ignoreCase = true)
}

data class CellDetailInfo(
    val columnName: String,
    val rowIndex: Int,
    val value: String?,
    val inferredType: String
)

@Composable
fun DataTableGrid(
    columns: List<String>,
    rows: List<List<String?>>,
    primaryKeyColumns: Set<String> = emptySet(),
    isFullScreen: Boolean = false,
    onToggleFullScreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    emptyMessage: String = "No rows returned by this query"
) {
    val context = LocalContext.current
    var selectedCell by remember { mutableStateOf<CellDetailInfo?>(null) }
    var filterQuery by remember { mutableStateOf("") }
    var isFilterVisible by remember { mutableStateOf(false) }
    var sortColumnIndex by remember { mutableStateOf<Int?>(null) }
    var sortDirection by remember { mutableStateOf(SortDirection.NONE) }
    var showExportMenu by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val horizontalScrollState = rememberScrollState()

    // Filtered and sorted rows for fast in-memory search and header sorting
    val displayedRows = remember(rows, filterQuery, sortColumnIndex, sortDirection) {
        val filtered = if (filterQuery.isBlank()) {
            rows
        } else {
            val q = filterQuery.trim().lowercase()
            rows.filter { row ->
                row.any { it?.lowercase()?.contains(q) == true }
            }
        }

        val colIdx = sortColumnIndex
        if (colIdx != null && sortDirection != SortDirection.NONE) {
            filtered.sortedWith { r1, r2 ->
                val v1 = r1.getOrNull(colIdx)
                val v2 = r2.getOrNull(colIdx)
                val cmp = compareValuesWithNumeric(v1, v2)
                if (sortDirection == SortDirection.DESC) -cmp else cmp
            }
        } else {
            filtered
        }
    }

    // Calculate dynamic column widths based on content length
    val columnWidths = remember(columns, rows) {
        columns.mapIndexed { colIndex, colName ->
            var maxLen = colName.length
            val sampleRows = rows.take(40)
            for (row in sampleRows) {
                val len = row.getOrNull(colIndex)?.length ?: 4
                if (len > maxLen) maxLen = len
            }
            // Map character count to dp width (clamp between 100.dp and 260.dp)
            val calculatedDp = (maxLen * 9 + 28).coerceIn(100, 260)
            calculatedDp.dp
        }
    }

    val indexWidth = 48.dp

    if (columns.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyMessage,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
    ) {
        // Table Sub-Toolbar (Filter & Full-Screen Actions)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${displayedRows.size} of ${rows.size} rows",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "•",
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${columns.size} columns",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.weight(1f))

            // Export / Share Actions
            Box {
                IconButton(
                    onClick = { showExportMenu = true },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("table_export_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export Results",
                        tint = CyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showExportMenu,
                    onDismissRequest = { showExportMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Share as CSV", fontSize = 13.sp) },
                        onClick = {
                            showExportMenu = false
                            val csv = ExportUtils.exportToCsv(columns, displayedRows)
                            ExportUtils.shareContent(context, "Query Result CSV", csv, "text/csv")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share as JSON", fontSize = 13.sp) },
                        onClick = {
                            showExportMenu = false
                            val json = ExportUtils.exportToJson(columns, displayedRows)
                            ExportUtils.shareContent(context, "Query Result JSON", json, "application/json")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Copy CSV to Clipboard", fontSize = 13.sp) },
                        onClick = {
                            showExportMenu = false
                            val csv = ExportUtils.exportToCsv(columns, displayedRows)
                            clipboardManager.setText(AnnotatedString(csv))
                        }
                    )
                }
            }

            // Toggle Search Filter
            IconButton(
                onClick = {
                    isFilterVisible = !isFilterVisible
                    if (!isFilterVisible) filterQuery = ""
                },
                modifier = Modifier
                    .size(28.dp)
                    .testTag("table_filter_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter rows",
                    tint = if (isFilterVisible || filterQuery.isNotBlank()) CyanAccent else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Fullscreen Toggle Action
            if (onToggleFullScreen != null) {
                IconButton(
                    onClick = onToggleFullScreen,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag(if (isFullScreen) "exit_fullscreen_table_btn" else "fullscreen_table_btn")
                ) {
                    Icon(
                        imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullScreen) "Exit Full Screen" else "Full Screen Table",
                        tint = CyanAccent,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        // Inline Filter Bar (if activated)
        if (isFilterVisible) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    placeholder = {
                        Text(
                            "Filter rows in this result set...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
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
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("table_filter_input")
                )
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        // Desktop-Grade Horizontal & Vertical Scrolling Grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(horizontalScrollState)
        ) {
            Column {
                // Sticky Header Row
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    // Index Header
                    Box(
                        modifier = Modifier
                            .width(indexWidth)
                            .height(40.dp)
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Column Headers (Tap to Sort: ASC -> DESC -> NONE)
                    columns.forEachIndexed { colIndex, colName ->
                        val colWidth = columnWidths[colIndex]
                        val isSorted = sortColumnIndex == colIndex && sortDirection != SortDirection.NONE
                        val isPrimaryKey = colName in primaryKeyColumns ||
                            colName.equals("id", ignoreCase = true) ||
                            colName.endsWith("_id", ignoreCase = true)

                        Box(
                            modifier = Modifier
                                .width(colWidth)
                                .height(40.dp)
                                .background(if (isSorted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                .clickable {
                                    if (sortColumnIndex == colIndex) {
                                        sortDirection = when (sortDirection) {
                                            SortDirection.NONE -> SortDirection.ASC
                                            SortDirection.ASC -> SortDirection.DESC
                                            SortDirection.DESC -> SortDirection.NONE
                                        }
                                    } else {
                                        sortColumnIndex = colIndex
                                        sortDirection = SortDirection.ASC
                                    }
                                }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isPrimaryKey) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = "Primary Key",
                                        tint = AmberAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = colName,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSorted) AmberAccent else if (isPrimaryKey) AmberAccent else CyanAccent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (isSorted) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (sortDirection == SortDirection.ASC) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = if (sortDirection == SortDirection.ASC) "Ascending" else "Descending",
                                        tint = AmberAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Table Rows
                if (displayedRows.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .width(columnWidths.sumOf { it.value.toDouble() }.dp + indexWidth)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (filterQuery.isNotBlank()) "No rows match '$filterQuery'" else emptyMessage,
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        itemsIndexed(displayedRows) { rowIndex, row ->
                            val rowBg = if (rowIndex % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            Row(
                                modifier = Modifier
                                    .background(rowBg)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                // Row Index Cell
                                Box(
                                    modifier = Modifier
                                        .width(indexWidth)
                                        .height(38.dp)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${rowIndex + 1}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Data Cells
                                columns.forEachIndexed { colIndex, colName ->
                                    val cellValue = row.getOrNull(colIndex)
                                    val colWidth = columnWidths[colIndex]
                                    val isNull = cellValue == null

                                    Box(
                                        modifier = Modifier
                                            .width(colWidth)
                                            .height(38.dp)
                                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                            .clickable {
                                                val inferredType = inferType(cellValue)
                                                selectedCell = CellDetailInfo(
                                                    columnName = colName,
                                                    rowIndex = rowIndex + 1,
                                                    value = cellValue,
                                                    inferredType = inferredType
                                                )
                                            }
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (isNull) {
                                            // Desktop-like NULL badge
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "NULL",
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = AmberAccent
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = cellValue ?: "",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Desktop-Grade Cell Inspection Dialog on Tap
    selectedCell?.let { cellInfo ->
        AlertDialog(
            onDismissRequest = { selectedCell = null },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                val isCellPk = cellInfo.columnName in primaryKeyColumns ||
                    cellInfo.columnName.equals("id", ignoreCase = true) ||
                    cellInfo.columnName.endsWith("_id", ignoreCase = true)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isCellPk) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = "Primary Key",
                                    tint = AmberAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = cellInfo.columnName,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (isCellPk) AmberAccent else MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Row #${cellInfo.rowIndex} • ${cellInfo.inferredType}" + if (isCellPk) " • [PRIMARY KEY]" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = cellInfo.value ?: "NULL (Null value)",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 13.sp,
                            color = if (cellInfo.value == null) AmberAccent else MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (cellInfo.value != null) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(cellInfo.value))
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Value", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = { selectedCell = null },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Close", fontSize = 12.sp)
                    }
                }
            }
        )
    }
}

private fun inferType(value: String?): String {
    if (value == null) return "NULL"
    if (value.toIntOrNull() != null) return "INTEGER"
    if (value.toDoubleOrNull() != null) return "REAL"
    if (value.equals("true", ignoreCase = true) || value.equals("false", ignoreCase = true)) return "BOOLEAN"
    return "TEXT (${value.length} chars)"
}
