package com.example.offlinegemma

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Tiny markdown renderer: **bold**, `code`, ``` code blocks ```, # headings, - bullets. */
@Composable
fun MarkdownText(text: String) {
    val parts = remember(text) { splitCode(text) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for ((isCode, body) in parts) {
            if (isCode) CodeBlock(body) else body.lines().forEach { MdLine(it) }
        }
    }
}

private fun splitCode(text: String): List<Pair<Boolean, String>> =
    text.split("```").mapIndexed { i, s ->
        if (i % 2 == 1) {
            val nl = s.indexOf('\n')
            val firstLine = if (nl >= 0) s.substring(0, nl).trim() else ""
            val isLang = nl >= 0 && firstLine.all { it.isLetterOrDigit() || it == '+' || it == '#' }
            true to (if (isLang) s.substring(nl + 1) else s)
        } else {
            false to s
        }
    }.filter { it.second.isNotBlank() }

@Composable
private fun CodeBlock(code: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .horizontalScroll(rememberScrollState())
            .padding(10.dp)
    ) {
        Text(code.trimEnd(), fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Color(0xFFBFD2FF))
    }
}

@Composable
private fun MdLine(line: String) {
    val t = line.trimEnd()
    val s = t.trimStart()
    when {
        t.isBlank() -> Spacer(Modifier.height(4.dp))
        s.startsWith("#") -> Text(
            mdInline(s.trimStart('#').trim()),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        s.startsWith("- ") || s.startsWith("* ") -> Text(
            mdInline("•  " + s.drop(2)),
            modifier = Modifier.padding(start = 6.dp)
        )
        else -> Text(mdInline(t))
    }
}

private fun mdInline(line: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < line.length) {
        if (line.startsWith("**", i)) {
            val end = line.indexOf("**", i + 2)
            if (end > 0) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(line.substring(i + 2, end)) }
                i = end + 2
                continue
            }
        }
        if (line[i] == '`') {
            val end = line.indexOf('`', i + 1)
            if (end > 0) {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color.White.copy(alpha = 0.12f))) {
                    append(line.substring(i + 1, end))
                }
                i = end + 1
                continue
            }
        }
        append(line[i])
        i++
    }
}
