package com.example.offlinegemma

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF060A1C)
val Accent = Color(0xFF4F7CFF)
val Accent2 = Color(0xFF8B5CF6)
val TextHi = Color(0xFFF1F4FF)
val TextLo = Color(0xFFA3ABCB)
val Good = Color(0xFF34D399)
val Bad = Color(0xFFFF7B8A)
val AccentBrush: Brush = Brush.horizontalGradient(listOf(Accent, Accent2))

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            secondary = Accent2,
            background = Ink,
            surface = Ink,
            onBackground = TextHi,
            onSurface = TextHi
        )
    ) {
        CompositionLocalProvider(LocalContentColor provides TextHi, content = content)
    }
}

/** Dark gradient with soft glowing colour blobs - this is what the glass cards sit on. */
@Composable
fun GlassBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF070B1F), Color(0xFF0B1030), Color(0xFF140B33))))
            .drawBehind {
                fun orb(c: Color, cx: Float, cy: Float, r: Float) {
                    drawCircle(
                        brush = Brush.radialGradient(listOf(c, Color.Transparent), center = Offset(cx, cy), radius = r),
                        radius = r,
                        center = Offset(cx, cy)
                    )
                }
                orb(Accent.copy(alpha = 0.30f), size.width * 0.10f, size.height * 0.10f, size.width * 0.9f)
                orb(Accent2.copy(alpha = 0.28f), size.width * 0.95f, size.height * 0.55f, size.width * 0.8f)
                orb(Color(0xFF22D3EE).copy(alpha = 0.12f), size.width * 0.20f, size.height * 0.95f, size.width * 0.7f)
            }
    ) { content() }
}

/** The glass look: see-through white tint + a thin light border that fades out. */
fun Modifier.glass(shape: Shape = RoundedCornerShape(20.dp), tint: Float = 0.08f): Modifier =
    this.clip(shape)
        .background(Brush.linearGradient(listOf(Color.White.copy(alpha = tint + 0.04f), Color.White.copy(alpha = tint - 0.02f))))
        .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.04f))), shape)

@Composable
fun GradientButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) AccentBrush else Brush.horizontalGradient(listOf(Color(0x33FFFFFF), Color(0x33FFFFFF))))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun GlassButton(text: String, modifier: Modifier = Modifier, color: Color = TextHi, onClick: () -> Unit) {
    Box(
        modifier
            .glass(RoundedCornerShape(16.dp), 0.09f)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = color, fontWeight = FontWeight.Medium) }
}

@Composable
fun GlassIconButton(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).glass(CircleShape, 0.10f).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(glyph, fontSize = 18.sp) }
}

/** The blue/purple diamond logo, drawn with code (no image file needed). */
@Composable
fun GemLogo(logoSize: Dp) {
    Canvas(Modifier.size(logoSize)) {
        val w = this.size.width
        val h = this.size.height
        val top = Path().apply {
            moveTo(w * 0.5f, 0f); lineTo(w, h * 0.38f); lineTo(w * 0.5f, h * 0.50f); lineTo(0f, h * 0.38f); close()
        }
        val left = Path().apply {
            moveTo(0f, h * 0.38f); lineTo(w * 0.5f, h * 0.50f); lineTo(w * 0.5f, h); close()
        }
        val right = Path().apply {
            moveTo(w, h * 0.38f); lineTo(w * 0.5f, h * 0.50f); lineTo(w * 0.5f, h); close()
        }
        drawPath(top, Brush.linearGradient(listOf(Color(0xFFA9C0FF), Color(0xFF6A8DFF))))
        drawPath(left, Brush.linearGradient(listOf(Color(0xFF4F7CFF), Color(0xFF3355D6))))
        drawPath(right, Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF5B34C9))))
    }
}
