package com.example.ui.components.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.EditorColorTheme
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonokaiEditorTheme
import java.util.regex.Pattern

object SqlSyntaxHighlighter {

    private val KEYWORDS = listOf(
        "PRIMARY KEY", "FOREIGN KEY", "LEFT JOIN", "RIGHT JOIN", "INNER JOIN",
        "CROSS JOIN", "FULL JOIN", "GROUP BY", "ORDER BY",
        "SELECT", "FROM", "WHERE", "JOIN", "ON", "HAVING", "LIMIT", "OFFSET",
        "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE",
        "CREATE", "TABLE", "ALTER", "DROP", "INDEX", "VIEW", "TRIGGER",
        "DISTINCT", "AS", "AND", "OR", "NOT", "NULL", "LIKE", "BETWEEN", "IN",
        "CASE", "WHEN", "THEN", "ELSE", "END", "ASC", "DESC",
        "DEFAULT", "CHECK", "UNIQUE", "AUTOINCREMENT", "REFERENCES", "EXISTS",
        "IS", "UNION", "ALL", "PRAGMA", "TRANSACTION", "BEGIN", "COMMIT",
        "ROLLBACK", "ADD", "COLUMN", "CASCADE", "REPLACE", "TEMPORARY", "TEMP",
        "EXPLAIN", "QUERY", "PLAN", "VACUUM", "ATTACH", "DETACH"
    )

    private val FUNCTIONS = listOf(
        "AVG", "COUNT", "SUM", "MIN", "MAX", "ROUND", "COALESCE", "IFNULL",
        "LENGTH", "UPPER", "LOWER", "SUBSTR", "TOTAL", "ABS", "RANDOM",
        "DATETIME", "DATE", "TIME", "STRFTIME", "GROUP_CONCAT", "TRIM",
        "LTRIM", "RTRIM", "TYPEOF", "HEX", "PRINTF", "INSTR", "GLOB"
    )

    private val TYPES = listOf(
        "INTEGER", "INT", "TEXT", "REAL", "BLOB", "NUMERIC", "BOOLEAN",
        "VARCHAR", "CHAR", "FLOAT", "DOUBLE", "BIGINT", "TIMESTAMP"
    )

    // Build the master regex pattern with named groups
    private val masterPattern: Pattern by lazy {
        val keywordsPattern = KEYWORDS.joinToString("|") { Pattern.quote(it) }
        val functionsPattern = FUNCTIONS.joinToString("|") { Pattern.quote(it) }
        val typesPattern = TYPES.joinToString("|") { Pattern.quote(it) }

        val regex = listOf(
            // Comments: line comment or block comment
            "(?<COMMENT>--[^\r\n]*|/\\*[\\s\\S]*?(\\*/|$))",
            // Strings: single-quote with escaped quotes or double quotes
            "(?<STRING>'([^']|'')*(')?|\"([^\"]|\"\")*(\")?|`([^`]|``)*(`)?)",
            // Numbers: hex, float, integer
            "(?<NUMBER>\\b0x[0-9a-fA-F]+\\b|\\b\\d+(\\.\\d+)?\\b)",
            // Multi-word / single-word keywords (word-boundary)
            "(?<KEYWORD>\\b(?i:$keywordsPattern)\\b)",
            // Built-in SQL functions
            "(?<FUNCTION>\\b(?i:$functionsPattern)(?=\\s*\\())",
            // Data types
            "(?<TYPE>\\b(?i:$typesPattern)\\b)",
            // Operators
            "(?<OPERATOR>=|!=|<>|<=|>=|<|>|\\+|-|\\*|/|%|\\|\\|)",
            // Punctuation
            "(?<PUNCTUATION>\\(|\\)|,|;)"
        ).joinToString("|")

        Pattern.compile(regex)
    }

    fun highlight(
        text: String,
        fontFamily: FontFamily = JetBrainsMonoFontFamily,
        theme: EditorColorTheme = MonokaiEditorTheme
    ): AnnotatedString {
        if (text.isEmpty()) {
            return AnnotatedString("")
        }

        return buildAnnotatedString {
            append(text)
            // Apply base style
            addStyle(
                SpanStyle(
                    color = theme.identifier,
                    fontFamily = fontFamily
                ),
                0,
                text.length
            )

            val matcher = masterPattern.matcher(text)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()

                when {
                    matcher.group("COMMENT") != null -> {
                        addStyle(
                            SpanStyle(
                                color = theme.comment,
                                fontStyle = FontStyle.Italic,
                                fontFamily = fontFamily
                            ),
                            start,
                            end
                        )
                    }
                    matcher.group("STRING") != null -> {
                        addStyle(
                            SpanStyle(
                                color = theme.stringLiteral,
                                fontFamily = fontFamily
                            ),
                            start,
                            end
                        )
                    }
                    matcher.group("NUMBER") != null -> {
                        addStyle(
                            SpanStyle(
                                color = theme.number,
                                fontFamily = fontFamily
                            ),
                            start,
                            end
                        )
                    }
                    matcher.group("KEYWORD") != null -> {
                        addStyle(
                            SpanStyle(
                                color = theme.keyword,
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontFamily
                            ),
                            start,
                            end
                        )
                    }
                    matcher.group("FUNCTION") != null -> {
                        addStyle(
                            SpanStyle(
                                color = theme.function,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = fontFamily
                            ),
                            start,
                            end
                        )
                    }
                    matcher.group("TYPE") != null -> {
                        addStyle(
                            SpanStyle(
                                color = theme.type,
                                fontWeight = FontWeight.Medium,
                                fontFamily = fontFamily
                            ),
                            start,
                            end
                        )
                    }
                    matcher.group("OPERATOR") != null -> {
                        addStyle(
                            SpanStyle(
                                color = theme.operator,
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontFamily
                            ),
                            start,
                            end
                        )
                    }
                    matcher.group("PUNCTUATION") != null -> {
                        addStyle(
                            SpanStyle(
                                color = theme.punctuation,
                                fontFamily = fontFamily
                            ),
                            start,
                            end
                        )
                    }
                }
            }
        }
    }
}
