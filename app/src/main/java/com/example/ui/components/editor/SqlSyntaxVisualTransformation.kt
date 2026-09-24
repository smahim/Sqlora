package com.example.ui.components.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.ui.theme.EditorColorTheme
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonokaiEditorTheme

class SqlSyntaxVisualTransformation(
    private val fontFamily: FontFamily = JetBrainsMonoFontFamily,
    private val theme: EditorColorTheme = MonokaiEditorTheme
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = SqlSyntaxHighlighter.highlight(text.text, fontFamily, theme)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}
