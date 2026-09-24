package com.example.ui.components.chart

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ScatterPlot
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QueryResult
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.DecimalFormat
import kotlin.math.cos
import kotlin.math.sin

private val ChartPalette = listOf(
    CyanAccent,
    EmeraldAccent,
    AmberAccent,
    PurpleAccent,
    RoseAccent,
    Color(0xFF38BDF8), // Sky Blue
    Color(0xFFF472B6), // Pink
    Color(0xFF34D399)  // Mint
)

private val NumberFormat = DecimalFormat("#,##0.##")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SqlChartView(
    queryResult: QueryResult,
    isFullScreen: Boolean = false,
    onToggleFullScreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val columnAnalysis = remember(queryResult) {
        ChartDataExtractor.analyzeColumns(queryResult)
    }

    val suitableTypes = remember(columnAnalysis, queryResult.rows.size) {
        ChartDataExtractor.getSuitableChartTypes(columnAnalysis, queryResult.rows.size)
    }

    var config by remember(queryResult) {
        mutableStateOf(ChartDataExtractor.createDefaultConfig(queryResult, columnAnalysis))
    }

    var selectedDataPointTooltip by remember { mutableStateOf<String?>(null) }

    if (config == null || suitableTypes.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Chartable Data Found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Charts require at least one numeric column (e.g. price, count, quantity) and categorical labels.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val currentConfig = config!!

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Top Toolbar: Chart Type Selector & Fullscreen Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val typeScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(typeScrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartType.values().forEach { type ->
                    val isSuitable = suitableTypes.contains(type)
                    val isSelected = currentConfig.selectedType == type

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSuitable) {
                                config = currentConfig.copy(selectedType = type)
                                selectedDataPointTooltip = null
                            }
                        },
                        enabled = isSuitable,
                        label = {
                            Text(
                                text = type.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = when (type) {
                                    ChartType.BAR -> Icons.Default.BarChart
                                    ChartType.LINE -> Icons.AutoMirrored.Filled.ShowChart
                                    ChartType.PIE -> Icons.Default.PieChart
                                    ChartType.SCATTER -> Icons.Default.ScatterPlot
                                },
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = if (isSuitable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("chart_type_${type.name}")
                    )
                }
            }

            if (onToggleFullScreen != null) {
                IconButton(
                    onClick = onToggleFullScreen,
                    modifier = Modifier.testTag("chart_fullscreen_toggle")
                ) {
                    Icon(
                        imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullScreen) "Exit Full Screen" else "Full Screen",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Axis / Column Controls
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // X-Axis or Category Column Dropdown
            ColumnSelectorDropdown(
                label = if (currentConfig.selectedType == ChartType.PIE) "Category" else "X-Axis",
                selectedColumn = currentConfig.xColumn,
                columns = queryResult.columns,
                onSelectColumn = {
                    config = currentConfig.copy(xColumn = it, categoryColumn = it)
                    selectedDataPointTooltip = null
                }
            )

            // Y-Axis or Value Column Dropdown
            ColumnSelectorDropdown(
                label = if (currentConfig.selectedType == ChartType.PIE) "Values" else "Y-Axis",
                selectedColumn = currentConfig.yColumn,
                columns = queryResult.columns,
                onSelectColumn = {
                    config = currentConfig.copy(yColumn = it)
                    selectedDataPointTooltip = null
                }
            )

            // Active Tooltip Badge
            if (selectedDataPointTooltip != null) {
                StatusBadge(
                    text = selectedDataPointTooltip!!,
                    color = AmberAccent,
                    showDot = true
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chart Canvas Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            AnimatedContent(
                targetState = currentConfig.selectedType,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "ChartCanvasTransition"
            ) { targetType ->
                when (targetType) {
                    ChartType.BAR -> {
                        val points = remember(queryResult, currentConfig) {
                            ChartDataExtractor.extractPoints(queryResult, currentConfig.xColumn, currentConfig.yColumn)
                        }
                        BarChartCanvas(
                            points = points,
                            yColName = currentConfig.yColumn,
                            onSelectPoint = { selectedDataPointTooltip = it }
                        )
                    }
                    ChartType.LINE -> {
                        val points = remember(queryResult, currentConfig) {
                            ChartDataExtractor.extractPoints(queryResult, currentConfig.xColumn, currentConfig.yColumn)
                        }
                        LineChartCanvas(
                            points = points,
                            yColName = currentConfig.yColumn,
                            onSelectPoint = { selectedDataPointTooltip = it }
                        )
                    }
                    ChartType.PIE -> {
                        val slices = remember(queryResult, currentConfig) {
                            ChartDataExtractor.extractPieSlices(queryResult, currentConfig.xColumn, currentConfig.yColumn)
                        }
                        PieChartCanvas(
                            slices = slices,
                            onSelectSlice = { selectedDataPointTooltip = it }
                        )
                    }
                    ChartType.SCATTER -> {
                        val points = remember(queryResult, currentConfig) {
                            ChartDataExtractor.extractPoints(queryResult, currentConfig.xColumn, currentConfig.yColumn)
                        }
                        ScatterChartCanvas(
                            points = points,
                            xColName = currentConfig.xColumn,
                            yColName = currentConfig.yColumn,
                            onSelectPoint = { selectedDataPointTooltip = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnSelectorDropdown(
    label: String,
    selectedColumn: String,
    columns: List<String>,
    onSelectColumn: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text(
                text = "$label: $selectedColumn",
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            columns.forEach { col ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = col,
                            color = if (col == selectedColumn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (col == selectedColumn) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    onClick = {
                        onSelectColumn(col)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun BarChartCanvas(
    points: List<ChartPoint>,
    yColName: String,
    onSelectPoint: (String) -> Unit
) {
    if (points.isEmpty()) {
        EmptyChartFallback()
        return
    }

    val maxY = (points.maxOfOrNull { it.yValue } ?: 1.0).coerceAtLeast(0.001)
    val minY = (points.minOfOrNull { it.yValue } ?: 0.0).coerceAtMost(0.0)
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(points) {
                detectTapGestures { offset ->
                    val totalBars = points.size
                    val barWidth = size.width / totalBars
                    val clickedIndex = (offset.x / barWidth).toInt().coerceIn(0, totalBars - 1)
                    val pt = points[clickedIndex]
                    onSelectPoint("${pt.label}: ${NumberFormat.format(pt.yValue)} ($yColName)")
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val paddingBottom = 24.dp.toPx()
        val usableHeight = h - paddingBottom
        val totalBars = points.size
        val barSlotWidth = w / totalBars
        val barWidth = (barSlotWidth * 0.65f).coerceAtLeast(4f)

        // Draw 3 horizontal subtle grid lines
        for (i in 0..3) {
            val y = usableHeight * (i / 3f)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
        }

        // Draw Bars
        points.forEachIndexed { index, pt ->
            val fraction = ((pt.yValue - minY) / (maxY - minY)).coerceIn(0.0, 1.0).toFloat()
            val barHeight = usableHeight * fraction
            val left = index * barSlotWidth + (barSlotWidth - barWidth) / 2
            val top = usableHeight - barHeight

            val color = ChartPalette[index % ChartPalette.size]

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(color, color.copy(alpha = 0.6f)),
                    startY = top,
                    endY = usableHeight
                ),
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}

@Composable
private fun LineChartCanvas(
    points: List<ChartPoint>,
    yColName: String,
    onSelectPoint: (String) -> Unit
) {
    if (points.size < 2) {
        EmptyChartFallback("Line chart requires at least 2 data points")
        return
    }

    val maxY = (points.maxOfOrNull { it.yValue } ?: 1.0).coerceAtLeast(0.001)
    val minY = (points.minOfOrNull { it.yValue } ?: 0.0)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val pointBgColor = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(points) {
                detectTapGestures { offset ->
                    val stepX = size.width / (points.size - 1)
                    val clickedIndex = ((offset.x + stepX / 2) / stepX).toInt().coerceIn(0, points.size - 1)
                    val pt = points[clickedIndex]
                    onSelectPoint("${pt.label}: ${NumberFormat.format(pt.yValue)} ($yColName)")
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val padding = 20.dp.toPx()
        val usableHeight = h - padding * 2
        val stepX = w / (points.size - 1)

        // Draw grid
        for (i in 0..3) {
            val y = padding + usableHeight * (i / 3f)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
        }

        val path = Path()
        val fillPath = Path()

        val coordinates = points.mapIndexed { idx, pt ->
            val fraction = ((pt.yValue - minY) / (maxY - minY)).coerceIn(0.0, 1.0).toFloat()
            val x = idx * stepX
            val y = padding + usableHeight * (1f - fraction)
            Offset(x, y)
        }

        coordinates.forEachIndexed { i, coord ->
            if (i == 0) {
                path.moveTo(coord.x, coord.y)
                fillPath.moveTo(coord.x, coord.y)
            } else {
                path.lineTo(coord.x, coord.y)
                fillPath.lineTo(coord.x, coord.y)
            }
        }

        // Fill area under line
        fillPath.lineTo(w, h)
        fillPath.lineTo(0f, h)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(CyanAccent.copy(alpha = 0.25f), Color.Transparent),
                startY = padding,
                endY = h
            )
        )

        // Draw stroke line
        drawPath(
            path = path,
            color = CyanAccent,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw data points
        coordinates.forEach { coord ->
            drawCircle(
                color = pointBgColor,
                radius = 5.dp.toPx(),
                center = coord
            )
            drawCircle(
                color = CyanAccent,
                radius = 3.5.dp.toPx(),
                center = coord
            )
        }
    }
}

@Composable
private fun PieChartCanvas(
    slices: List<PieSlice>,
    onSelectSlice: (String) -> Unit
) {
    if (slices.isEmpty()) {
        EmptyChartFallback()
        return
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pie Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(170.dp)
                    .pointerInput(slices) {
                        detectTapGestures { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            var angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) angle += 360f

                            var cumulativeAngle = 0f
                            for (slice in slices) {
                                val sweep = (slice.percentage / 100f) * 360f
                                if (angle >= cumulativeAngle && angle < cumulativeAngle + sweep) {
                                    onSelectSlice("${slice.label}: ${NumberFormat.format(slice.value)} (${String.format("%.1f", slice.percentage)}%)")
                                    break
                                }
                                cumulativeAngle += sweep
                            }
                        }
                    }
            ) {
                var currentAngle = -90f
                val strokeWidth = 32.dp.toPx()

                slices.forEachIndexed { index, slice ->
                    val sweep = (slice.percentage / 100f) * 360f
                    val color = ChartPalette[index % ChartPalette.size]

                    // Doughnut Slice
                    drawArc(
                        color = color,
                        startAngle = currentAngle,
                        sweepAngle = sweep - 1.5f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    )
                    currentAngle += sweep
                }
            }
        }

        // Legend Column
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            slices.forEachIndexed { index, slice ->
                val color = ChartPalette[index % ChartPalette.size]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = slice.label,
                        fontSize = 11.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${String.format("%.1f", slice.percentage)}%",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun ScatterChartCanvas(
    points: List<ChartPoint>,
    xColName: String,
    yColName: String,
    onSelectPoint: (String) -> Unit
) {
    if (points.size < 2) {
        EmptyChartFallback("Scatter chart requires at least 2 points")
        return
    }

    val minX = points.minOfOrNull { it.xValue } ?: 0.0
    val maxX = (points.maxOfOrNull { it.xValue } ?: 1.0).coerceAtLeast(minX + 0.001)
    val minY = points.minOfOrNull { it.yValue } ?: 0.0
    val maxY = (points.maxOfOrNull { it.yValue } ?: 1.0).coerceAtLeast(minY + 0.001)
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(points) {
                detectTapGestures { offset ->
                    val w = size.width
                    val h = size.height
                    val padding = 20.dp.toPx()
                    val usableW = w - padding * 2
                    val usableH = h - padding * 2

                    val nearest = points.minByOrNull { pt ->
                        val px = padding + usableW * ((pt.xValue - minX) / (maxX - minX)).toFloat()
                        val py = padding + usableH * (1f - ((pt.yValue - minY) / (maxY - minY)).toFloat())
                        val distSq = (offset.x - px) * (offset.x - px) + (offset.y - py) * (offset.y - py)
                        distSq
                    }
                    if (nearest != null) {
                        onSelectPoint("${nearest.label}: $xColName=${NumberFormat.format(nearest.xValue)}, $yColName=${NumberFormat.format(nearest.yValue)}")
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val padding = 20.dp.toPx()
        val usableW = w - padding * 2
        val usableH = h - padding * 2

        // Grid lines
        for (i in 0..3) {
            val y = padding + usableH * (i / 3f)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
        }

        points.forEachIndexed { idx, pt ->
            val px = padding + usableW * ((pt.xValue - minX) / (maxX - minX)).toFloat()
            val py = padding + usableH * (1f - ((pt.yValue - minY) / (maxY - minY)).toFloat())
            val color = ChartPalette[idx % ChartPalette.size]

            drawCircle(
                color = color.copy(alpha = 0.8f),
                radius = 5.dp.toPx(),
                center = Offset(px, py)
            )
        }
    }
}

@Composable
private fun EmptyChartFallback(message: String = "Not enough data points for this chart type") {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = TextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}
