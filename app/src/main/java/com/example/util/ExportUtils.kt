package com.example.util

import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject

object ExportUtils {

    /**
     * Converts a table or query result set (columns + rows) into RFC 4180 CSV string.
     */
    fun exportToCsv(columns: List<String>, rows: List<List<String?>>): String {
        val sb = StringBuilder()

        // Header
        sb.append(columns.joinToString(",") { escapeCsv(it) }).append("\r\n")

        // Rows
        for (row in rows) {
            val line = columns.indices.joinToString(",") { colIndex ->
                escapeCsv(row.getOrNull(colIndex) ?: "")
            }
            sb.append(line).append("\r\n")
        }

        return sb.toString()
    }

    /**
     * Converts a table or query result set into pretty-formatted JSON string.
     */
    fun exportToJson(columns: List<String>, rows: List<List<String?>>): String {
        val jsonArray = JSONArray()

        for (row in rows) {
            val obj = JSONObject()
            columns.forEachIndexed { index, colName ->
                val cell = row.getOrNull(index)
                if (cell == null) {
                    obj.put(colName, JSONObject.NULL)
                } else {
                    // Try parsing as number if applicable
                    val asLong = cell.toLongOrNull()
                    val asDouble = cell.toDoubleOrNull()
                    when {
                        asLong != null && cell == asLong.toString() -> obj.put(colName, asLong)
                        asDouble != null && cell == asDouble.toString() -> obj.put(colName, asDouble)
                        cell.equals("true", ignoreCase = true) -> obj.put(colName, true)
                        cell.equals("false", ignoreCase = true) -> obj.put(colName, false)
                        else -> obj.put(colName, cell)
                    }
                }
            }
            jsonArray.put(obj)
        }

        return jsonArray.toString(2)
    }

    private fun escapeCsv(value: String): String {
        var result = value
        val mustQuote = result.contains(",") || result.contains("\"") || result.contains("\n") || result.contains("\r")
        if (result.contains("\"")) {
            result = result.replace("\"", "\"\"")
        }
        return if (mustQuote) "\"$result\"" else result
    }

    /**
     * Shares plain text content using Android native share sheet.
     */
    fun shareContent(context: Context, title: String, content: String, mimeType: String = "text/plain") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        val chooser = Intent.createChooser(intent, title)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
