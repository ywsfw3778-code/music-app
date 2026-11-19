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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private const val GLASS_BLUR_SCALE = 0.25f

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
    var bgBlurScaleX: Float = GLASS_BLUR_SCALE
    var bgBlurScaleY: Float = GLASS_BLUR_SCALE
    var allBlurScaleX: Float = GLASS_BLUR_SCALE
    var allBlurScaleY: Float = GLASS_BLUR_SCALE
}

@Composable
fun rememberWaterGlassState(blurEnabled: Boolean = true): WaterGlassState {
    val bgLayer = rememberGraphicsLayer()
    val contentLayer = rememberGraphicsLayer()
    val blurredBgLayer = rememberGraphicsLayer()
    val blurredAllLayer = rememberGraphicsLayer()
    return remember(bgLayer, contentLayer, blurredBgLayer, blurredAllLayer) {
        WaterGlassState(bgLayer, contentLayer, blurredBgLayer, blurredAllLayer)
    }.also {
        it.blurEnabled = blurEnabled
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

        if (state.blurEnabled && size.width > 0f && size.height > 0f) {
            val targetBlurPx = (12.dp.toPx() * GLASS_BLUR_SCALE).coerceAtLeast(1f)
            if (state.appliedBlurPx != targetBlurPx) {
                val effect = BlurEffect(targetBlurPx, targetBlurPx, TileMode.Clamp)
                state.blurredBgLayer.renderEffect = effect
                state.blurredAllLayer.renderEffect = effect
                state.appliedBlurPx = targetBlurPx
            }
            val scaledW = (size.width * GLASS_BLUR_SCALE).roundToInt().coerceAtLeast(1)
            val scaledH = (size.height * GLASS_BLUR_SCALE).roundToInt().coerceAtLeast(1)
            val sx = scaledW.toFloat() / size.width
            val sy = scaledH.toFloat() / size.height
            state.bgBlurScaleX = sx
            state.bgBlurScaleY = sy
            state.blurredBgLayer.record(size = IntSize(scaledW, scaledH)) {
                scale(scaleX = sx, scaleY = sy, pivot = Offset.Zero) {
                    drawLayer(state.bgLayer)
                }
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
            if (!state.blurEnabled || size.width <= 0f || size.height <= 0f) {
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

            val scaledW = (size.width * GLASS_BLUR_SCALE).roundToInt().coerceAtLeast(1)
            val scaledH = (size.height * GLASS_BLUR_SCALE).roundToInt().coerceAtLeast(1)
            val sx = scaledW.toFloat() / size.width
            val sy = scaledH.toFloat() / size.height
            state.allBlurScaleX = sx
            state.allBlurScaleY = sy
            state.blurredAllLayer.record(size = IntSize(scaledW, scaledH)) {
                scale(scaleX = sx, scaleY = sy, pivot = Offset.Zero) {
                    drawLayer(state.bgLayer)
                    drawLayer(state.contentLayer)
                }
            }
            if (!state.hasContent) {
                state.hasContent = true
            }
        }
    }
}

fun DrawScope.drawFullScreenGlassBackground(glass: WaterGlassState) {
    if (!glass.blurEnabled) {
        drawLayer(glass.bgLayer)
    } else {
        val invX = if (glass.bgBlurScaleX > 0f) 1f / glass.bgBlurScaleX else 1f
        val invY = if (glass.bgBlurScaleY > 0f) 1f / glass.bgBlurScaleY else 1f
        scale(scaleX = invX, scaleY = invY, pivot = Offset.Zero) {
            drawLayer(glass.blurredBgLayer)
        }
    }
}

private fun DrawScope.drawPositionedGlassLayer(
    glass: WaterGlassState,
    itemOffset: Offset,
    bgOnly: Boolean
) {
    val dx = -(itemOffset.x - glass.rootOffset.x)
    val dy = -(itemOffset.y - glass.rootOffset.y)
    if (!glass.blurEnabled) {
        clipRect {
            translate(left = dx, top = dy) {
                drawLayer(glass.bgLayer)
            }
        }
        return
    }
    val useBgOnly = bgOnly || glass.isRecordingContent || !glass.hasContent
    val targetLayer = if (useBgOnly) glass.blurredBgLayer else glass.blurredAllLayer
    val sx = if (useBgOnly) glass.bgBlurScaleX else glass.allBlurScaleX
    val sy = if (useBgOnly) glass.bgBlurScaleY else glass.allBlurScaleY
    val invX = if (sx > 0f) 1f / sx else 1f
    val invY = if (sy > 0f) 1f / sy else 1f
    clipRect {
        translate(left = dx, top = dy) {
            scale(scaleX = invX, scaleY = invY, pivot = Offset.Zero) {
                drawLayer(targetLayer)
            }
        }
    }
}

private fun Modifier.pureWaterGlass(
    shape: Shape,
    waterSheen: Color = Color(0x04FFFFFF),
    bgOnly: Boolean = false
): Modifier = composed {
    val glass = LocalWaterGlassState.current
    val itemOffset = remember { mutableStateOf(Offset.Zero) }
    val base = this
        .onGloballyPositioned { coords ->
            val pos = coords.positionInWindow()
            if (itemOffset.value != pos) {
                itemOffset.value = pos
            }
        }
        .clip(shape)

    if (glass != null) {
        base.drawBehind {
            drawPositionedGlassLayer(glass, itemOffset.value, bgOnly)
            if (!glass.blurEnabled) {
                drawRect(Color(0x2E080A10))
                drawRect(Color(0x0AFFFFFF))
            } else if (waterSheen != Color.Transparent) {
                drawRect(waterSheen)
            }
        }
    } else {
        base.background(waterSheen)
    }
}

fun Modifier.glassSheetSurface(shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)): Modifier = composed {
    val glass = LocalWaterGlassState.current
    val itemOffset = remember { mutableStateOf(Offset.Zero) }
    val base = this
        .onGloballyPositioned { coords ->
            val pos = coords.positionInWindow()
            if (itemOffset.value != pos) {
                itemOffset.value = pos
            }
        }
        .clip(shape)

    if (glass != null) {
        base.drawBehind {
            drawPositionedGlassLayer(glass, itemOffset.value, bgOnly = false)
            drawRect(if (glass.blurEnabled) Color(0x36080A10) else Color(0x48080A10))
            drawRect(Color(0x08FFFFFF))
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
    val itemOffset = remember { mutableStateOf(Offset.Zero) }
    this
        .onGloballyPositioned { coords ->
            val pos = coords.positionInWindow()
            if (itemOffset.value != pos) {
                itemOffset.value = pos
            }
        }
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawBehind {
            if (glass != null) {
                drawPositionedGlassLayer(glass, itemOffset.value, bgOnly = false)
            }
            drawRect(HeaderTintBlurred)
            drawRect(
                brush = HeaderFadeMask,
                blendMode = BlendMode.DstIn
            )
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

fun Modifier.matteCard(shape: Shape = RoundedCornerShape(18.dp)): Modifier = this
    .clip(shape)
    .background(MatteCardBrush)

fun Modifier.waterDropBar(shape: Shape = RoundedCornerShape(26.dp), bgOnly: Boolean = false): Modifier =
    pureWaterGlass(shape = shape, waterSheen = Color(0x04FFFFFF), bgOnly = bgOnly)

fun Modifier.frostedFloatingBar(shape: Shape = RoundedCornerShape(26.dp)): Modifier =
    pureWaterGlass(shape = shape, waterSheen = Color(0x04FFFFFF), bgOnly = false)

fun Modifier.matteElevated(shape: Shape = RoundedCornerShape(24.dp)): Modifier = this
    .clip(shape)
    .background(MatteElevatedBrush)
