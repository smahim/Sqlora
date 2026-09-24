package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SqlFormatUtils {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[index])
    }

    fun formatDateTime(timestamp: Long): String {
        if (timestamp <= 0) return "-"
        val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        if (timestamp <= 0) return "-"
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    val COMMON_SQL_KEYWORDS = listOf(
        "SELECT * FROM",
        "WHERE",
        "ORDER BY",
        "LIMIT 25",
        "COUNT(*)",
        "INSERT INTO",
        "UPDATE",
        "DELETE FROM",
        "JOIN",
        "GROUP BY"
    )

    /**
     * Formats a SQL statement with clean line breaks, consistent keyword casing,
     * and readable indentation without corrupting string literals or comments.
     */
    fun formatSql(rawSql: String): String {
        if (rawSql.isBlank()) return rawSql

        try {
            // 1. Extract and preserve string literals and comments
            val placeholders = mutableListOf<String>()
            val placeholderPrefix = "___SQL_TOKEN_PLACEHOLDER_"
            val literalPattern = java.util.regex.Pattern.compile(
                "('--[^\\r\\n]*|/\\*[\\s\\S]*?\\*/|'([^']|'')*'|\"([^\"]|\"\")*\"|`([^`]|``)*`)"
            )
            val matcher = literalPattern.matcher(rawSql)
            val sb = StringBuffer()
            var placeholderIndex = 0
            while (matcher.find()) {
                val token = matcher.group()
                placeholders.add(token)
                matcher.appendReplacement(sb, "$placeholderPrefix$placeholderIndex")
                placeholderIndex++
            }
            matcher.appendTail(sb)

            var sql = sb.toString().trim()
            // Collapse excessive whitespace while preserving single spaces
            sql = sql.replace(Regex("[ \\t]+"), " ")

            // Normalize semicolon
            sql = sql.replace(Regex("\\s*;\\s*$"), ";")

            // Top-level clauses that start on a new line (0-indent)
            val majorClauses = listOf(
                "SELECT", "FROM", "WHERE", "GROUP BY", "HAVING", "ORDER BY", "LIMIT", "OFFSET",
                "LEFT OUTER JOIN", "RIGHT OUTER JOIN", "FULL OUTER JOIN", "LEFT JOIN", "RIGHT JOIN",
                "INNER JOIN", "CROSS JOIN", "JOIN",
                "INSERT INTO", "VALUES", "UPDATE", "SET", "DELETE FROM",
                "CREATE TABLE", "CREATE INDEX", "CREATE VIEW", "CREATE TRIGGER",
                "UNION ALL", "UNION"
            )

            for (clause in majorClauses) {
                // Match case-insensitively, ensure whole word boundary
                val pattern = Regex("(?i)(?<!^)\\b(${Regex.escape(clause)})\\b")
                sql = sql.replace(pattern) { matchResult ->
                    "\n${clause.uppercase()}"
                }
                // Also uppercase leading clause if present at start
                val startPattern = Regex("(?i)^\\b(${Regex.escape(clause)})\\b")
                sql = sql.replace(startPattern, clause.uppercase())
            }

            // Indent AND / OR under WHERE / HAVING
            val subClauses = listOf("AND", "OR")
            for (sub in subClauses) {
                val pattern = Regex("(?i)(?<!^)\\b(${Regex.escape(sub)})\\b")
                sql = sql.replace(pattern) { "\n  ${sub.uppercase()}" }
            }

            // Put comma-separated items on new indented lines for SELECT and SET if statement is multi-line
            val lines = sql.lines()
            val formattedLines = mutableListOf<String>()

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("SELECT ", ignoreCase = true) && trimmed.contains(",")) {
                    val rest = trimmed.substring(7)
                    val parts = splitTopLevelCommas(rest)
                    if (parts.size > 1) {
                        formattedLines.add("SELECT")
                        parts.forEachIndexed { i, p ->
                            val comma = if (i < parts.size - 1) "," else ""
                            formattedLines.add("  ${p.trim()}$comma")
                        }
                        continue
                    }
                }
                if (trimmed.startsWith("SET ", ignoreCase = true) && trimmed.contains(",")) {
                    val rest = trimmed.substring(4)
                    val parts = splitTopLevelCommas(rest)
                    if (parts.size > 1) {
                        formattedLines.add("SET")
                        parts.forEachIndexed { i, p ->
                            val comma = if (i < parts.size - 1) "," else ""
                            formattedLines.add("  ${p.trim()}$comma")
                        }
                        continue
                    }
                }
                formattedLines.add(line)
            }

            var result = formattedLines.joinToString("\n")

            // Restore preserved literals and comments
            for (i in placeholders.indices) {
                result = result.replace("$placeholderPrefix$i", placeholders[i])
            }

            return result
        } catch (e: Throwable) {
            // Safety fallback: Never break user code
            return rawSql
        }
    }

    private fun splitTopLevelCommas(text: String): List<String> {
        val parts = mutableListOf<String>()
        var depth = 0
        var current = StringBuilder()

        for (ch in text) {
            when (ch) {
                '(' -> depth++
                ')' -> depth = (depth - 1).coerceAtLeast(0)
                ',' -> {
                    if (depth == 0) {
                        parts.add(current.toString().trim())
                        current = StringBuilder()
                        continue
                    }
                }
            }
            current.append(ch)
        }
        if (current.isNotBlank()) {
            parts.add(current.toString().trim())
        }
        return parts
    }
}
