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

fun Modifier.waterGlassContent(state: WaterGlassState?): Modifier = this

private class OffsetHolder {
    var offset by mutableStateOf(Offset.Zero)
}

private fun DrawScope.drawPositionedGlassLayer(
    glass: WaterGlassState,
    itemOffset: Offset,
    bgOnly: Boolean
) {
    val targetLayer = glass.blurredBgLayer
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
    bgOnly: Boolean = true
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
            if (!glass.blurEnabled) {
                drawRect(Color(0x55080A10))
            }
            if (waterSheen != Color.Transparent) {
                drawRect(waterSheen)
            }
        }
    } else {
        base.background(waterSheen)
    }
}

fun Modifier.openTopGlassFade(): Modifier = this.drawBehind {
    drawRect(
        Brush.verticalGradient(
            0.0f to Color(0xF207080C),
            0.55f to Color(0xD007080C),
            0.80f to Color(0x7007080C),
            1.0f to Color.Transparent
        )
    )
}

fun Modifier.matteCard(
    shape: Shape = RoundedCornerShape(18.dp),
    backgroundColor: Color = Color(0x0CFFFFFF),
    borderColor: Color = Color(0x14FFFFFF)
): Modifier = this
    .clip(shape)
    .background(backgroundColor)
    .border(1.dp, borderColor, shape)

fun Modifier.waterDropBar(shape: Shape = RoundedCornerShape(26.dp), bgOnly: Boolean = true): Modifier =
    pureWaterGlass(shape = shape, waterSheen = Color(0x04FFFFFF), bgOnly = bgOnly)

fun Modifier.frostedFloatingBar(shape: Shape = RoundedCornerShape(26.dp)): Modifier =
    pureWaterGlass(shape = shape, waterSheen = Color(0x04FFFFFF), bgOnly = true)

fun Modifier.matteElevated(shape: Shape = RoundedCornerShape(24.dp)): Modifier =
    this
        .clip(shape)
        .background(Color(0x12FFFFFF))
        .border(1.dp, Color(0x1AFFFFFF), shape)
