package com.example.ui.components.chart

import com.example.data.model.QueryResult

enum class ChartType(val title: String, val iconName: String) {
    BAR("Bar Chart", "BarChart"),
    LINE("Line Chart", "ShowChart"),
    PIE("Pie Chart", "PieChart"),
    SCATTER("Scatter Chart", "ScatterPlot")
}

data class ChartColumnSuitability(
    val column: String,
    val isNumeric: Boolean,
    val distinctValuesCount: Int,
    val sampleValues: List<String>
)

data class ChartConfiguration(
    val selectedType: ChartType,
    val xColumn: String,
    val yColumn: String,
    val categoryColumn: String? = null
)

data class ChartPoint(
    val label: String,
    val xValue: Double,
    val yValue: Double,
    val displayLabel: String = label
)

data class PieSlice(
    val label: String,
    val value: Double,
    val percentage: Float
)

object ChartDataExtractor {

    /**
     * Inspects QueryResult columns and detects whether they contain numeric data.
     */
    fun analyzeColumns(queryResult: QueryResult): List<ChartColumnSuitability> {
        if (!queryResult.isSuccess || !queryResult.isSelect || queryResult.columns.isEmpty()) {
            return emptyList()
        }

        val rowLimit = minOf(queryResult.rows.size, 100)
        return queryResult.columns.mapIndexed { colIndex, colName ->
            var numericCount = 0
            var nonNullCount = 0
            val distinctSet = mutableSetOf<String>()
            val samples = mutableListOf<String>()

            for (r in 0 until rowLimit) {
                val row = queryResult.rows[r]
                val value = row.getOrNull(colIndex)
                if (value != null && value.isNotBlank() && value != "NULL") {
                    nonNullCount++
                    distinctSet.add(value)
                    if (samples.size < 3) samples.add(value)
                    val parsed = value.toDoubleOrNull()
                    if (parsed != null) {
                        numericCount++
                    }
                }
            }

            val isNumeric = nonNullCount > 0 && (numericCount.toDouble() / nonNullCount) >= 0.8
            ChartColumnSuitability(
                column = colName,
                isNumeric = isNumeric,
                distinctValuesCount = distinctSet.size,
                sampleValues = samples
            )
        }
    }

    /**
     * Evaluates which chart types are suitable for the given query result.
     */
    fun getSuitableChartTypes(columns: List<ChartColumnSuitability>, rowCount: Int): Set<ChartType> {
        if (rowCount == 0) return emptySet()
        val numericCols = columns.filter { it.isNumeric }
        val categoricalCols = columns.filter { !it.isNumeric }

        val suitable = mutableSetOf<ChartType>()

        // Bar Chart: at least 1 numeric column and either a categorical or another column
        if (numericCols.isNotEmpty() && columns.size >= 2) {
            suitable.add(ChartType.BAR)
        }

        // Line Chart: at least 1 numeric column, works well with sequential/date/numeric X
        if (numericCols.isNotEmpty() && columns.size >= 2) {
            suitable.add(ChartType.LINE)
        }

        // Pie Chart: exactly 1 numeric column and 1 categorical column with 2..15 distinct categories
        if (numericCols.isNotEmpty() && (categoricalCols.isNotEmpty() || columns.size >= 2) && rowCount in 2..20) {
            suitable.add(ChartType.PIE)
        }

        // Scatter Chart: at least 2 distinct numeric columns
        if (numericCols.size >= 2) {
            suitable.add(ChartType.SCATTER)
        }

        return suitable
    }

    /**
     * Creates a default sensible chart configuration from available columns.
     */
    fun createDefaultConfig(
        queryResult: QueryResult,
        analysis: List<ChartColumnSuitability>
    ): ChartConfiguration? {
        if (analysis.size < 2 || queryResult.rows.isEmpty()) return null

        val numericCols = analysis.filter { it.isNumeric }
        val categoricalCols = analysis.filter { !it.isNumeric }

        val suitable = getSuitableChartTypes(analysis, queryResult.rows.size)
        if (suitable.isEmpty()) return null

        val defaultType = when {
            suitable.contains(ChartType.BAR) -> ChartType.BAR
            suitable.contains(ChartType.LINE) -> ChartType.LINE
            suitable.contains(ChartType.SCATTER) -> ChartType.SCATTER
            else -> suitable.first()
        }

        val yCol = numericCols.firstOrNull()?.column ?: analysis[1].column
        val xCol = if (categoricalCols.isNotEmpty()) {
            categoricalCols.first().column
        } else {
            analysis.first { it.column != yCol }.column
        }

        return ChartConfiguration(
            selectedType = defaultType,
            xColumn = xCol,
            yColumn = yCol,
            categoryColumn = xCol
        )
    }

    /**
     * Extracts ChartPoints for Bar, Line, or Scatter charts.
     */
    fun extractPoints(
        queryResult: QueryResult,
        xCol: String,
        yCol: String
    ): List<ChartPoint> {
        val xIndex = queryResult.columns.indexOf(xCol)
        val yIndex = queryResult.columns.indexOf(yCol)
        if (xIndex == -1 || yIndex == -1) return emptyList()

        val points = mutableListOf<ChartPoint>()
        queryResult.rows.forEachIndexed { index, row ->
            val rawX = row.getOrNull(xIndex)
            val rawY = row.getOrNull(yIndex)

            val yVal = rawY?.toDoubleOrNull()
            if (yVal != null && !yVal.isNaN()) {
                val xVal = rawX?.toDoubleOrNull() ?: index.toDouble()
                val label = rawX ?: "Row $index"
                points.add(
                    ChartPoint(
                        label = label,
                        xValue = xVal,
                        yValue = yVal,
                        displayLabel = label
                    )
                )
            }
        }
        return points
    }

    /**
     * Extracts PieSlices for Pie Chart.
     */
    fun extractPieSlices(
        queryResult: QueryResult,
        labelCol: String,
        valueCol: String
    ): List<PieSlice> {
        val labelIndex = queryResult.columns.indexOf(labelCol)
        val valueIndex = queryResult.columns.indexOf(valueCol)
        if (labelIndex == -1 || valueIndex == -1) return emptyList()

        val rawSlices = mutableListOf<Pair<String, Double>>()
        var total = 0.0

        queryResult.rows.take(15).forEachIndexed { idx, row ->
            val label = row.getOrNull(labelIndex)?.takeIf { it.isNotBlank() } ?: "Item ${idx + 1}"
            val value = row.getOrNull(valueIndex)?.toDoubleOrNull()
            if (value != null && value > 0) {
                rawSlices.add(label to value)
                total += value
            }
        }

        if (total <= 0.0) return emptyList()

        return rawSlices.map { (label, value) ->
            PieSlice(
                label = label,
                value = value,
                percentage = ((value / total) * 100).toFloat()
            )
        }
    }
}
