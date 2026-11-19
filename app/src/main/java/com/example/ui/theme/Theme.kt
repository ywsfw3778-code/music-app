package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.lerp
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
    var rootOffset: Offset = Offset.Zero
    var hasContent by mutableStateOf(false)
    var isRecordingContent: Boolean = false
    var appliedBlurPx: Float = -1f
    var blurEnabled by mutableStateOf(true)
    var accentColor by mutableStateOf(Color(0xFFF5F5F5))
    var secondaryColor by mutableStateOf(Color(0xFF64748B))

    fun solidSurfaceColor(): Color {
        val tinted = lerp(Color(0xFF0B0E16), secondaryColor.copy(alpha = 1f), 0.34f)
        return lerp(tinted, accentColor.copy(alpha = 1f), 0.08f).copy(alpha = 1f)
    }

    fun solidSurfaceTop(): Color {
        val tinted = lerp(Color(0xFF131824), secondaryColor.copy(alpha = 1f), 0.42f)
        return lerp(tinted, accentColor.copy(alpha = 1f), 0.13f).copy(alpha = 1f)
    }

    fun solidSurfaceDeep(): Color {
        val tinted = lerp(Color(0xFF080A10), secondaryColor.copy(alpha = 1f), 0.24f)
        return lerp(tinted, accentColor.copy(alpha = 1f), 0.04f).copy(alpha = 1f)
    }

    fun satinSurfaceBrush(): Brush = Brush.verticalGradient(
        0.0f to solidSurfaceTop(),
        0.52f to solidSurfaceColor(),
        1.0f to solidSurfaceDeep()
    )

    fun satinTopBloomBrush(): Brush = Brush.verticalGradient(
        0.0f to lerp(Color.White, accentColor.copy(alpha = 1f), 0.65f).copy(alpha = 0.11f),
        0.38f to Color.White.copy(alpha = 0.025f),
        1.0f to Color.Transparent
    )

    fun satinRimBrush(): Brush = Brush.verticalGradient(
        0.0f to lerp(Color.White, accentColor.copy(alpha = 1f), 0.38f).copy(alpha = 0.30f),
        0.45f to Color.White.copy(alpha = 0.10f),
        1.0f to Color.White.copy(alpha = 0.04f)
    )

    fun satinCardBrush(): Brush {
        val top = lerp(Color(0xFF131824), secondaryColor.copy(alpha = 1f), 0.32f)
            .let { lerp(it, accentColor.copy(alpha = 1f), 0.09f) }
            .copy(alpha = 1f)
        val bottom = lerp(Color(0xFF0C0F17), secondaryColor.copy(alpha = 1f), 0.24f)
            .let { lerp(it, accentColor.copy(alpha = 1f), 0.04f) }
            .copy(alpha = 1f)
        return Brush.verticalGradient(listOf(top, bottom))
    }

    fun satinCardRimBrush(): Brush = Brush.verticalGradient(
        0.0f to lerp(Color.White, accentColor.copy(alpha = 1f), 0.30f).copy(alpha = 0.18f),
        1.0f to Color.White.copy(alpha = 0.04f)
    )
}

@Composable
fun rememberWaterGlassState(
    blurEnabled: Boolean = true,
    accentColor: Color = Color(0xFFF5F5F5),
    secondaryColor: Color = Color(0xFF64748B)
): WaterGlassState {
    val bgLayer = rememberGraphicsLayer()
    val contentLayer = rememberGraphicsLayer()
    val blurredBgLayer = rememberGraphicsLayer()
    val blurredAllLayer = rememberGraphicsLayer()
    return remember(bgLayer, contentLayer, blurredBgLayer, blurredAllLayer) {
        WaterGlassState(bgLayer, contentLayer, blurredBgLayer, blurredAllLayer)
    }.also {
        it.blurEnabled = blurEnabled
        it.accentColor = accentColor
        it.secondaryColor = secondaryColor
    }
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

fun Modifier.waterGlassBackground(state: WaterGlassState, active: Boolean = true): Modifier = this
    .onGloballyPositioned { coords ->
        state.rootOffset = coords.positionInWindow()
    }
    .drawWithContent {
        if (!active) {
            drawContent()
            return@drawWithContent
        }
        state.bgLayer.record {
            drawRect(Color.Black)
            this@drawWithContent.drawContent()
        }
        drawLayer(state.bgLayer)

        if (state.blurEnabled) {
            val targetBlurPx = 12.dp.toPx()
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

private class OffsetRef(var offset: Offset = Offset.Zero)

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
    val itemOffset = remember { OffsetRef() }
    val base = this
        .onGloballyPositioned { coords ->
            itemOffset.offset = coords.positionInWindow()
        }
        .clip(shape)

    if (glass != null) {
        if (glass.blurEnabled) {
            base.drawBehind {
                drawPositionedGlassLayer(glass, itemOffset.offset, bgOnly)
                if (waterSheen != Color.Transparent) {
                    drawRect(waterSheen)
                }
            }
        } else {
            base
                .drawBehind {
                    drawRect(glass.satinSurfaceBrush())
                    drawRect(glass.satinTopBloomBrush())
                }
                .border(0.85.dp, glass.satinRimBrush(), shape)
        }
    } else {
        base.background(waterSheen)
    }
}

fun Modifier.glassSheetSurface(shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)): Modifier = composed {
    val glass = LocalWaterGlassState.current
    val itemOffset = remember { OffsetRef() }
    val base = this
        .onGloballyPositioned { coords ->
            itemOffset.offset = coords.positionInWindow()
        }
        .clip(shape)

    if (glass != null) {
        if (glass.blurEnabled) {
            base
                .drawBehind {
                    drawPositionedGlassLayer(glass, itemOffset.offset, bgOnly = false)
                    drawRect(Color(0x2E080A10))
                    drawRect(Color(0x06FFFFFF))
                }
                .border(
                    width = 0.85.dp,
                    brush = Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = 0.20f),
                        0.5f to Color.White.copy(alpha = 0.05f),
                        1.0f to Color.Transparent
                    ),
                    shape = shape
                )
        } else {
            base
                .drawBehind {
                    drawRect(glass.satinSurfaceBrush())
                    drawRect(glass.satinTopBloomBrush())
                }
                .border(0.85.dp, glass.satinRimBrush(), shape)
        }
    } else {
        base.background(NocturneColors.MatteElevatedBg)
    }
}

private val HeaderTintBlurred = Brush.verticalGradient(
    0.0f to Color.Black.copy(alpha = 0.28f),
    0.72f to Color.Black.copy(alpha = 0.18f),
    1.0f to Color.Transparent
)

private val HeaderFadeMask = Brush.verticalGradient(
    0.0f to Color.Black,
    0.72f to Color.Black,
    0.92f to Color.Black.copy(alpha = 0.40f),
    1.0f to Color.Transparent
)

fun Modifier.openTopGlassFade(): Modifier = composed {
    val glass = LocalWaterGlassState.current
    val itemOffset = remember { OffsetRef() }
    val blurOn = glass?.blurEnabled == true
    if (blurOn && glass != null) {
        this
            .onGloballyPositioned { coords ->
                itemOffset.offset = coords.positionInWindow()
            }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawBehind {
                drawPositionedGlassLayer(glass, itemOffset.offset, bgOnly = false)
                drawRect(HeaderTintBlurred)
                drawRect(
                    brush = HeaderFadeMask,
                    blendMode = BlendMode.DstIn
                )
            }
    } else {
        this.drawBehind {
            val topCol = glass?.solidSurfaceTop() ?: Color(0xFF131824)
            val midCol = glass?.solidSurfaceColor() ?: Color(0xFF0B0E16)
            val deepCol = glass?.solidSurfaceDeep() ?: Color(0xFF080A10)
            drawRect(
                Brush.verticalGradient(
                    0.00f to topCol,
                    0.66f to midCol,
                    0.82f to midCol.copy(alpha = 0.90f),
                    0.94f to deepCol.copy(alpha = 0.42f),
                    1.00f to Color.Transparent
                )
            )
            if (glass != null) {
                drawRect(
                    Brush.verticalGradient(
                        0.0f to glass.accentColor.copy(alpha = 0.10f),
                        0.55f to Color.Transparent
                    )
                )
            }
        }
    }
}

private val MatteCardBrush = Brush.verticalGradient(
    listOf(
        Color(0x6E161822),
        Color(0x5210121A)
    )
)

private val MatteElevatedBrush = Brush.verticalGradient(
    listOf(
        Color(0x8C1B1E2A),
        Color(0x7012141D)
    )
)

fun Modifier.matteCard(shape: Shape = RoundedCornerShape(18.dp)): Modifier = composed {
    val glass = LocalWaterGlassState.current
    if (glass != null && !glass.blurEnabled) {
        this
            .clip(shape)
            .background(glass.satinCardBrush())
            .border(0.75.dp, glass.satinCardRimBrush(), shape)
    } else {
        this
            .clip(shape)
            .background(MatteCardBrush)
    }
}

fun Modifier.waterDropBar(shape: Shape = RoundedCornerShape(26.dp), bgOnly: Boolean = false): Modifier =
    pureWaterGlass(shape = shape, waterSheen = Color(0x04FFFFFF), bgOnly = bgOnly)

fun Modifier.frostedFloatingBar(shape: Shape = RoundedCornerShape(26.dp)): Modifier =
    pureWaterGlass(shape = shape, waterSheen = Color(0x04FFFFFF), bgOnly = false)

fun Modifier.matteElevated(shape: Shape = RoundedCornerShape(24.dp)): Modifier = composed {
    val glass = LocalWaterGlassState.current
    if (glass != null && !glass.blurEnabled) {
        this
            .clip(shape)
            .background(glass.satinSurfaceBrush())
            .border(0.85.dp, glass.satinRimBrush(), shape)
    } else {
        this
            .clip(shape)
            .background(MatteElevatedBrush)
    }
}
