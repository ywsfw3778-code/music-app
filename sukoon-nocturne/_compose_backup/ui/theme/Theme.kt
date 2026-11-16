package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

object NocturneColors {
    val Black = Color(0xFF000000)
    val MatteCardBg = Color(0x0EFFFFFF) // ~rgba(255,255,255,0.055)
    val MatteCardBorder = Color(0x10FFFFFF) // ~rgba(255,255,255,0.06)
    val WaterDropBarBg = Color(0xE014141A) // ~rgba(20,20,26,0.88)
    val WaterDropBarBorder = Color(0x14FFFFFF) // ~rgba(255,255,255,0.08)
    val WaterDropPillBg = Color(0x17FFFFFF) // #ffffff17
    val MatteElevatedBg = Color(0xF5101016) // ~rgba(16,16,22,0.96)
    val MatteElevatedBorder = Color(0x17FFFFFF) // ~rgba(255,255,255,0.09)
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)
    val Danger = Color(0xFFEF4444)
    val Emerald = Color(0xFF10B981)
}

@Composable
fun SukoonNocturneTheme(
    accentColorInt: Int,
    content: @Composable () -> Unit
) {
    val accent by animateColorAsState(
        targetValue = Color(accentColorInt),
        animationSpec = tween(600),
        label = "accent"
    )
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = Color.Black,
        background = NocturneColors.Black,
        onBackground = NocturneColors.TextPrimary,
        surface = Color(0xFF0A0A0E),
        onSurface = NocturneColors.TextPrimary,
        surfaceVariant = Color(0xFF14141A),
        onSurfaceVariant = NocturneColors.TextSecondary
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.SansSerif)
        ),
        content = content
    )
}

fun Modifier.matteCard(shape: Shape = RoundedCornerShape(18.dp)): Modifier =
    this
        .clip(shape)
        .background(NocturneColors.MatteCardBg)
        .border(BorderStroke(1.dp, NocturneColors.MatteCardBorder), shape)

fun Modifier.waterDropBar(shape: Shape = RoundedCornerShape(26.dp)): Modifier =
    this
        .clip(shape)
        .background(NocturneColors.WaterDropBarBg)
        .border(BorderStroke(1.dp, NocturneColors.WaterDropBarBorder), shape)

fun Modifier.matteElevated(shape: Shape = RoundedCornerShape(24.dp)): Modifier =
    this
        .clip(shape)
        .background(NocturneColors.MatteElevatedBg)
        .border(BorderStroke(1.dp, NocturneColors.MatteElevatedBorder), shape)
