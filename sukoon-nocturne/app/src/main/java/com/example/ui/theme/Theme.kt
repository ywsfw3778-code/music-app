package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Stable
class WaterGlassState(
    val bgLayer: GraphicsLayer,
    val contentLayer: GraphicsLayer,
    val blurredBgLayer: GraphicsLayer,
    val blurredAllLayer: GraphicsLayer
) {
    var rootOffset by mutableStateOf(Offset.Zero)
    var hasContent by mutableStateOf(false)
    var isRecordingContent: Boolean = false
    var appliedBlurPx: Float = -1f
    var blurEnabled by mutableStateOf(true)
}

@Composable
fun rememberWaterGlassState(blurEnabled: Boolean = true): WaterGlassState {
    val bgLayer = rememberGraphicsLayer()
    val contentLayer = rememberGraphicsLayer()
    val blurredBgLayer = rememberGraphicsLayer()
    val blurredAllLayer = rememberGraphicsLayer()
    return remember(bgLayer, contentLayer, blurredBgLayer, blurredAllLayer) {
        WaterGlassState(bgLayer, contentLayer, blurredBgLayer, blurredAllLayer)
    }.also { it.blurEnabled = blurEnabled }
}

val LocalWaterGlassState = compositionLocalOf<WaterGlassState?> { null }

@Composable
fun SukoonNocturneTheme(
    accentColorInt: Int,
    content: @Composable () -> Unit
) {
    val accent = Color(accentColorInt)
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
        typography = AppTypography
    ) {
        ProvideTextStyle(value = TextStyle(fontFamily = AppFontFamily)) {
            content()
        }
    }
}

fun Modifier.waterGlassBackground(state: WaterGlassState): Modifier = this
    .onGloballyPositioned { coords ->
        state.rootOffset = coords.positionInWindow()
    }
    .drawWithContent {
        state.bgLayer.record {
            drawRect(Color.Black)
            this@drawWithContent.drawContent()
        }
        drawLayer(state.bgLayer)

        val targetBlurPx = 15.dp.toPx()
        if (state.appliedBlurPx != targetBlurPx) {
            val effect = BlurEffect(targetBlurPx, targetBlurPx, TileMode.Clamp)
            state.blurredBgLayer.renderEffect = effect
            state.blurredAllLayer.renderEffect = effect
            state.appliedBlurPx = targetBlurPx
        }
        state.blurredBgLayer.record {
            drawLayer(state.bgLayer)
        }
    }

fun Modifier.waterGlassContent(state: WaterGlassState?): Modifier = if (state == null) {
    this
} else {
    this.composed {
        DisposableEffect(state) {
            onDispose {
                state.hasContent = false
            }
        }
        Modifier.drawWithContent {
            if (!state.blurEnabled) {
                drawContent()
                if (state.hasContent) state.hasContent = false
                return@drawWithContent
            }
            state.isRecordingContent = true
            try {
                state.contentLayer.record {
                    this@drawWithContent.drawContent()
                }
            } finally {
                state.isRecordingContent = false
            }
            drawLayer(state.contentLayer)

            state.blurredAllLayer.record {
                drawLayer(state.bgLayer)
                drawLayer(state.contentLayer)
            }
            if (!state.hasContent) {
                state.hasContent = true
            }
        }
    }
}

private class OffsetHolder {
    var offset by mutableStateOf(Offset.Zero)
}

private fun DrawScope.drawPositionedGlassLayer(
    glass: WaterGlassState,
    itemOffset: Offset,
    bgOnly: Boolean
) {
    val useStaticBgOnly = !glass.blurEnabled || bgOnly || glass.isRecordingContent || !glass.hasContent
    val targetLayer = if (useStaticBgOnly) glass.blurredBgLayer else glass.blurredAllLayer
    val dx = -(itemOffset.x - glass.rootOffset.x)
    val dy = -(itemOffset.y - glass.rootOffset.y)
    clipRect {
        translate(left = dx, top = dy) {
            drawLayer(targetLayer)
        }
    }
}

private fun Modifier.pureWaterGlass(
    shape: Shape,
    waterSheen: Color = Color(0x04FFFFFF),
    bgOnly: Boolean = false
): Modifier = composed {
    val glass = LocalWaterGlassState.current
    val itemOffset = remember { OffsetHolder() }
    val base = this
        .onGloballyPositioned { coords ->
            val pos = coords.positionInWindow()
            if (itemOffset.offset != pos) {
                itemOffset.offset = pos
            }
        }
        .clip(shape)

    if (glass != null) {
        base.drawBehind {
            drawPositionedGlassLayer(glass, itemOffset.offset, bgOnly)
            if (!bgOnly) {
                drawRect(if (glass.blurEnabled) Color(0x47080A10) else Color(0x66080A10))
            }
            if (waterSheen != Color.Transparent) {
                drawRect(waterSheen)
            }
        }
    } else {
        base.background(waterSheen)
    }
}

fun Modifier.openTopGlassFade(): Modifier = composed {
    val glass = LocalWaterGlassState.current
    val itemOffset = remember { OffsetHolder() }
    this
        .onGloballyPositioned { coords ->
            val pos = coords.positionInWindow()
            if (itemOffset.offset != pos) {
                itemOffset.offset = pos
            }
        }
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawBehind {
            if (glass != null) {
                drawPositionedGlassLayer(glass, itemOffset.offset, bgOnly = false)
            }
            // Gentle translucent tint matching the rest of the app so album ambient colors show through vibrantly
            val topAlpha = if (glass?.blurEnabled == false) 0.45f else 0.28f
            val midAlpha = if (glass?.blurEnabled == false) 0.35f else 0.18f
            drawRect(
                Brush.verticalGradient(
                    0.0f to Color.Black.copy(alpha = topAlpha),
                    0.72f to Color.Black.copy(alpha = midAlpha),
                    1.0f to Color.Transparent
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to Color.Black,
                    0.72f to Color.Black,
                    0.92f to Color.Black.copy(alpha = 0.40f),
                    1.0f to Color.Transparent
                ),
                blendMode = BlendMode.DstIn
            )
        }
}

fun Modifier.matteCard(shape: Shape = RoundedCornerShape(18.dp)): Modifier =
    pureWaterGlass(shape = shape, waterSheen = Color(0x03FFFFFF), bgOnly = true)

fun Modifier.waterDropBar(shape: Shape = RoundedCornerShape(26.dp), bgOnly: Boolean = false): Modifier =
    pureWaterGlass(shape = shape, waterSheen = Color(0x04FFFFFF), bgOnly = bgOnly)

fun Modifier.frostedFloatingBar(shape: Shape = RoundedCornerShape(26.dp)): Modifier =
    pureWaterGlass(shape = shape, waterSheen = Color(0x04FFFFFF), bgOnly = false)

fun Modifier.matteElevated(shape: Shape = RoundedCornerShape(24.dp)): Modifier =
    pureWaterGlass(shape = shape, waterSheen = Color(0x08FFFFFF), bgOnly = true)
