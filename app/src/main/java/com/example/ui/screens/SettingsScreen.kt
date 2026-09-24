package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.EditorPreferences
import com.example.ui.components.StatusBadge
import com.example.ui.components.StudioTopBar
import com.example.ui.components.editor.JetBrainsSqlEditor
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XcodeBlue
import com.example.ui.theme.XcodeCobalt
import com.example.ui.theme.XcodeCrimson
import com.example.ui.theme.XcodeIndigo
import com.example.ui.theme.XcodeMagenta

@Composable
fun SettingsScreen(
    editorPreferences: EditorPreferences,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeMode by editorPreferences.themeMode.collectAsStateWithLifecycle()
    val fontSizeSp by editorPreferences.fontSizeSp.collectAsStateWithLifecycle()
    val showLineNumbers by editorPreferences.showLineNumbers.collectAsStateWithLifecycle()

    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        EditorPreferences.THEME_LIGHT -> false
        EditorPreferences.THEME_SYSTEM -> isSystemDark
        else -> true
    }

    val previewSql = if (isDark) {
        """-- JetBrains Mono & Monokai Theme (Dark)
SELECT p.id, p.name, p.price,
       COUNT(o.id) AS total_orders
FROM products p
LEFT JOIN orders o ON p.id = o.product_id
WHERE p.stock > 0
GROUP BY p.id
ORDER BY p.price DESC
LIMIT 10;"""
    } else {
        """-- Apple Xcode Code Theme (Light)
SELECT p.id, p.name, p.price,
       COUNT(o.id) AS total_orders
FROM products p
LEFT JOIN orders o ON p.id = o.product_id
WHERE p.stock > 0
GROUP BY p.id
ORDER BY p.price DESC
LIMIT 10;"""
    }

    var editorText by remember(isDark) { mutableStateOf(previewSql) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            StudioTopBar(
                title = "Settings",
                subtitle = "Appearance, typography & SQL editor",
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION: APP APPEARANCE & THEME
            item {
                Text(
                    text = "APPEARANCE & THEME",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            }

            // Theme Mode Selector Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .testTag("settings_theme_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Theme & Code Highlighting",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isDark) "Monokai (Dark IDE)" else "Apple Xcode (Light IDE)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(
                                text = if (isDark) "Monokai" else "Xcode Light",
                                color = if (isDark) AmberAccent else XcodeBlue,
                                showDot = true
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Theme Options Grid / List
                        val themeOptions = listOf(
                            Triple(
                                EditorPreferences.THEME_LIGHT,
                                "Light • Apple Xcode",
                                "Clean white background with Xcode signature magenta keywords, indigo functions, crimson strings, and cobalt numbers"
                            ),
                            Triple(
                                EditorPreferences.THEME_DARK,
                                "Dark • JetBrains Monokai",
                                "Dark Monokai IDE palette with neon pink keywords, cyan functions, and olive comments"
                            ),
                            Triple(
                                EditorPreferences.THEME_SYSTEM,
                                "System Default",
                                "Follow device system dark/light mode automatically"
                            )
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            themeOptions.forEach { (mode, title, desc) ->
                                val isSelected = themeMode == mode
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { editorPreferences.setThemeMode(mode) }
                                        .testTag("theme_option_$mode"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (mode) {
                                                    EditorPreferences.THEME_LIGHT -> Icons.Default.LightMode
                                                    EditorPreferences.THEME_DARK -> Icons.Default.DarkMode
                                                    else -> Icons.Default.BrightnessAuto
                                                },
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                            Text(
                                                text = desc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        }

                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION: SQL CODE EDITOR SETTINGS
            item {
                Text(
                    text = "SQL CODE EDITOR",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            }

            // Editor Font Size Setting Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .testTag("settings_font_size_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatSize,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Editor Font Size",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Only affects the SQL editor; app UI remains unchanged",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(
                                text = "${fontSizeSp}sp",
                                color = MaterialTheme.colorScheme.primary,
                                showDot = false
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Font size stepper and slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val newSize = (fontSizeSp - 1).coerceAtLeast(EditorPreferences.MIN_FONT_SIZE)
                                    editorPreferences.setFontSize(newSize)
                                },
                                enabled = fontSizeSp > EditorPreferences.MIN_FONT_SIZE,
                                modifier = Modifier.testTag("decrease_font_size_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TextDecrease,
                                    contentDescription = "Decrease",
                                    tint = if (fontSizeSp > EditorPreferences.MIN_FONT_SIZE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Slider(
                                value = fontSizeSp.toFloat(),
                                onValueChange = { editorPreferences.setFontSize(it.toInt()) },
                                valueRange = EditorPreferences.MIN_FONT_SIZE.toFloat()..EditorPreferences.MAX_FONT_SIZE.toFloat(),
                                steps = (EditorPreferences.MAX_FONT_SIZE - EditorPreferences.MIN_FONT_SIZE) - 1,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("font_size_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )

                            IconButton(
                                onClick = {
                                    val newSize = (fontSizeSp + 1).coerceAtMost(EditorPreferences.MAX_FONT_SIZE)
                                    editorPreferences.setFontSize(newSize)
                                },
                                enabled = fontSizeSp < EditorPreferences.MAX_FONT_SIZE,
                                modifier = Modifier.testTag("increase_font_size_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TextIncrease,
                                    contentDescription = "Increase",
                                    tint = if (fontSizeSp < EditorPreferences.MAX_FONT_SIZE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Preset chips row
                        val chipsScrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(chipsScrollState),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            EditorPreferences.AVAILABLE_FONT_SIZES.forEach { size ->
                                val selected = size == fontSizeSp
                                FilterChip(
                                    selected = selected,
                                    onClick = { editorPreferences.setFontSize(size) },
                                    label = {
                                        Text(
                                            text = "${size}sp",
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.testTag("chip_font_size_$size")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Reset to default button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { editorPreferences.setFontSize(EditorPreferences.DEFAULT_FONT_SIZE) },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                modifier = Modifier.testTag("reset_font_size_btn")
                            ) {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Default (15sp)", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Live Preview Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isDark) "LIVE MONOKAI PREVIEW" else "LIVE APPLE XCODE PREVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (isDark) "JetBrains Mono • Monokai Dark" else "JetBrains Mono • Apple Xcode Light",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            // Live Code Editor Preview
            item {
                JetBrainsSqlEditor(
                    sqlText = editorText,
                    onSqlChange = { editorText = it },
                    onExecute = {},
                    onClear = { editorText = "" },
                    onInsertKeyword = { snippet -> editorText += " $snippet" },
                    isExecuting = false,
                    fontSizeSp = fontSizeSp,
                    onFontSizeChange = { editorPreferences.setFontSize(it) },
                    overrideDarkTheme = isDark
                )
            }

            // SECTION: SYNTAX PALETTE INFO
            item {
                Text(
                    text = if (isDark) "MONOKAI SYNTAX TOKENS" else "APPLE XCODE SYNTAX TOKENS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!isDark) {
                            Text(
                                text = "Apple Xcode Syntax Highlighting",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Engineered according to macOS Xcode typography and code token hierarchy:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val tokens = listOf(
                                Triple("Keywords (SELECT, FROM, WHERE)", XcodeMagenta, "Bold #9B2393"),
                                Triple("Built-in Functions (COUNT, AVG)", XcodeIndigo, "SemiBold #3900A0"),
                                Triple("String Literals ('Active')", XcodeCrimson, "#C41A16"),
                                Triple("Numeric Constants (10, 899.99)", XcodeCobalt, "#1C00CF"),
                                Triple("Comments (-- explanation)", Color(0xFF5D6C79), "Italic #5D6C79"),
                                Triple("Data Types (INTEGER, TEXT)", Color(0xFF4B2185), "#4B2185")
                            )

                            tokens.forEach { (tokenName, color, hex) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(color, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = tokenName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = hex,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "JetBrains Monokai Syntax Highlighting",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Standard IntelliJ IDEA / DataGrip Monokai syntax highlighting:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val tokens = listOf(
                                Triple("Keywords (SELECT, FROM, WHERE)", Color(0xFFF92672), "#F92672"),
                                Triple("Built-in Functions (COUNT, AVG)", Color(0xFF66D9EF), "#66D9EF"),
                                Triple("String Literals ('Active')", Color(0xFFE6DB74), "#E6DB74"),
                                Triple("Numeric Constants (10, 899.99)", Color(0xFFAE81FF), "#AE81FF"),
                                Triple("Comments (-- explanation)", Color(0xFF75715E), "Italic #75715E"),
                                Triple("Entity Names (tables, columns)", Color(0xFFA6E22E), "#A6E22E")
                            )

                            tokens.forEach { (tokenName, color, hex) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(color, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = tokenName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = hex,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION: ARCHITECTURE INFO
            item {
                Text(
                    text = "ENGINE ARCHITECTURE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Editor Font Family", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                Text("JetBrains Mono (Bundled .ttf)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusBadge("JetBrains Mono", color = EmeraldAccent, showDot = false)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Active Theme Engine", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                Text(if (isDark) "Monokai Dark" else "Apple Xcode Light", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusBadge(if (isDark) "Monokai" else "Apple Xcode", color = if (isDark) AmberAccent else XcodeBlue, showDot = false)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("SQLite Engine", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                Text("Native Android SQLite + SAF Storage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusBadge("Native", color = PurpleAccent, showDot = false)
                        }
                    }
                }
            }
        }
    }
}
