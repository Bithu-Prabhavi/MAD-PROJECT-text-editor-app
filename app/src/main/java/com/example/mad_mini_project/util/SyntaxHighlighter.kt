package com.example.mad_mini_project.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class SyntaxVisualTransformation(
    private val searchQuery: String = "",
    private val currentMatchIndex: Int = 0
) : VisualTransformation {

    private val kotlinKeywords = setOf(
        "fun", "val", "var", "class", "object", "interface", "if", "else", "when",
        "for", "while", "return", "import", "package", "true", "false", "null",
        "try", "catch", "finally", "private", "public", "protected", "override",
        "in", "is", "as", "this", "super", "break", "continue"
    )

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val annotated = buildAnnotatedString {
            append(raw)
            addStyle(SpanStyle(fontFamily = FontFamily.Monospace), 0, raw.length)

            // 1. Kotlin Keywords
            val wordRegex = Regex("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b")
            for (match in wordRegex.findAll(raw)) {
                if (match.value in kotlinKeywords) {
                    addStyle(
                        SpanStyle(color = Color(0xFF6200EE), fontWeight = FontWeight.Bold),
                        match.range.first,
                        match.range.last + 1
                    )
                }
            }

            // 2. Kotlin Annotations (@Annotation)
            val annotationRegex = Regex("@[A-Za-z0-9_]+")
            for (match in annotationRegex.findAll(raw)) {
                addStyle(
                    SpanStyle(color = Color(0xFF9C27B0), fontWeight = FontWeight.Bold),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // 3. Kotlin Strings ("string")
            val stringRegex = Regex("\".*?\"")
            for (match in stringRegex.findAll(raw)) {
                addStyle(
                    SpanStyle(color = Color(0xFF2E7D32)),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // 4. Single-line Comments (// comment)
            val singleLineCommentRegex = Regex("//.*")
            for (match in singleLineCommentRegex.findAll(raw)) {
                addStyle(
                    SpanStyle(color = Color(0xFF757575), fontStyle = FontStyle.Italic),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // 5. Multi-line Comments (/* comment */)
            val multiLineCommentRegex = Regex("/\\*[\\s\\S]*?\\*/")
            for (match in multiLineCommentRegex.findAll(raw)) {
                addStyle(
                    SpanStyle(color = Color(0xFF757575), fontStyle = FontStyle.Italic),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // 6. Markdown Headings (# Heading)
            val headingRegex = Regex("(?m)^(#{1,6})\\s+.*$")
            for (match in headingRegex.findAll(raw)) {
                addStyle(
                    SpanStyle(color = Color(0xFF00897B), fontWeight = FontWeight.Bold),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // 7. Markdown Bold (**text**)
            val boldRegex = Regex("\\*\\*([^*]+)\\*\\*")
            for (match in boldRegex.findAll(raw)) {
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // 8. Markdown Italics (*text*)
            val italicRegex = Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)")
            for (match in italicRegex.findAll(raw)) {
                addStyle(
                    SpanStyle(fontStyle = FontStyle.Italic),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // 9. Markdown Inline Code (`code`)
            val inlineCodeRegex = Regex("`[^`\\n]+`")
            for (match in inlineCodeRegex.findAll(raw)) {
                addStyle(
                    SpanStyle(color = Color(0xFFD84315), fontFamily = FontFamily.Monospace),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // 10. Search Query Highlighting
            if (searchQuery.isNotEmpty()) {
                var index = raw.indexOf(searchQuery, ignoreCase = true)
                var matchCount = 0
                val activeIndex = currentMatchIndex
                while (index != -1) {
                    val isCurrentMatch = (matchCount == activeIndex)
                    val bgColor = if (isCurrentMatch) Color(0xFFFF9800) else Color(0xFFFFF176)
                    val textColor = Color.Black
                    
                    addStyle(
                        SpanStyle(
                            background = bgColor,
                            color = textColor,
                            fontWeight = if (isCurrentMatch) FontWeight.Bold else FontWeight.Normal
                        ),
                        index,
                        index + searchQuery.length
                    )
                    matchCount++
                    index = raw.indexOf(searchQuery, index + searchQuery.length, ignoreCase = true)
                }
            }
        }
        return TransformedText(annotated, OffsetMapping.Identity)
    }
}
