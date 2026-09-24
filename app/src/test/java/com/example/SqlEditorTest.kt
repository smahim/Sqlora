package com.example

import android.content.Context
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.EditorPreferences
import com.example.ui.components.editor.SqlSyntaxHighlighter
import com.example.ui.components.editor.SqlSyntaxVisualTransformation
import com.example.ui.theme.MonokaiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SqlEditorTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testEditorPreferences_defaultFontSize() {
        val prefs = EditorPreferences(context)
        assertEquals(15, prefs.fontSizeSp.value)
    }

    @Test
    fun testEditorPreferences_setFontSize_updatesAndClamps() {
        val prefs = EditorPreferences(context)

        // Set within valid range
        prefs.setFontSize(18)
        assertEquals(18, prefs.fontSizeSp.value)

        // Test clamping to minimum 12
        prefs.setFontSize(5)
        assertEquals(12, prefs.fontSizeSp.value)

        // Test clamping to maximum 24
        prefs.setFontSize(50)
        assertEquals(24, prefs.fontSizeSp.value)

        // Set back to 16
        prefs.setFontSize(16)
        assertEquals(16, prefs.fontSizeSp.value)

        // Re-read from a new instance to verify persistence in SharedPreferences
        val prefsNewInstance = EditorPreferences(context)
        assertEquals(16, prefsNewInstance.fontSizeSp.value)
    }

    @Test
    fun testSqlSyntaxHighlighter_highlightsKeywords() {
        val sql = "SELECT id, name FROM users WHERE age > 21 ORDER BY id DESC LIMIT 10;"
        val highlighted = SqlSyntaxHighlighter.highlight(sql, FontFamily.Monospace)

        // Confirm text content is unchanged
        assertEquals(sql, highlighted.text)

        // Verify span styles exist
        assertTrue("Expected syntax styles in highlighted text", highlighted.spanStyles.isNotEmpty())

        // Find keyword spans
        val keywordSpans = highlighted.spanStyles.filter { it.item.color == MonokaiTheme.Keyword }
        assertTrue("Should have multiple keyword spans for SELECT, FROM, WHERE, ORDER BY, DESC, LIMIT", keywordSpans.size >= 5)
    }

    @Test
    fun testSqlSyntaxHighlighter_highlightsStringsAndNumbers() {
        val sql = "INSERT INTO products (name, price) VALUES ('Widget Pro', 99.95);"
        val highlighted = SqlSyntaxHighlighter.highlight(sql, FontFamily.Monospace)

        assertEquals(sql, highlighted.text)

        // Find string literal span ('Widget Pro')
        val stringSpans = highlighted.spanStyles.filter { it.item.color == MonokaiTheme.StringLiteral }
        assertTrue("Should highlight 'Widget Pro' with Monokai StringLiteral color", stringSpans.isNotEmpty())

        // Find number span (99.95)
        val numberSpans = highlighted.spanStyles.filter { it.item.color == MonokaiTheme.Number }
        assertTrue("Should highlight 99.95 with Monokai Number color", numberSpans.isNotEmpty())
    }

    @Test
    fun testSqlSyntaxHighlighter_highlightsComments() {
        val sql = "-- This is a line comment\nSELECT 1;\n/* block comment */"
        val highlighted = SqlSyntaxHighlighter.highlight(sql, FontFamily.Monospace)

        assertEquals(sql, highlighted.text)

        val commentSpans = highlighted.spanStyles.filter { it.item.color == MonokaiTheme.Comment }
        assertTrue("Should highlight both line comment and block comment", commentSpans.size >= 2)
    }

    @Test
    fun testSqlSyntaxHighlighter_highlightsFunctions() {
        val sql = "SELECT COUNT(id), AVG(price), ROUND(total, 2) FROM orders;"
        val highlighted = SqlSyntaxHighlighter.highlight(sql, FontFamily.Monospace)

        assertEquals(sql, highlighted.text)

        val functionSpans = highlighted.spanStyles.filter { it.item.color == MonokaiTheme.Function }
        assertTrue("Should highlight COUNT, AVG, ROUND with Monokai Function color", functionSpans.isNotEmpty())
    }

    @Test
    fun testSqlSyntaxVisualTransformation_identityOffsetMapping() {
        val transformation = SqlSyntaxVisualTransformation(FontFamily.Monospace)
        val input = AnnotatedString("SELECT * FROM table_name;")
        val transformed = transformation.filter(input)

        // Text preserved
        assertEquals("SELECT * FROM table_name;", transformed.text.text)

        // 1:1 mapping required for cursor navigation and selection
        assertEquals(0, transformed.offsetMapping.originalToTransformed(0))
        assertEquals(6, transformed.offsetMapping.originalToTransformed(6))
        assertEquals(input.length, transformed.offsetMapping.originalToTransformed(input.length))
        assertEquals(0, transformed.offsetMapping.transformedToOriginal(0))
        assertEquals(6, transformed.offsetMapping.transformedToOriginal(6))
    }
}
