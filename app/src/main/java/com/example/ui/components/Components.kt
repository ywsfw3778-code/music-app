package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil.Coil
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.data.AudioRepository
import com.example.model.AppStrings
import com.example.model.Playlist
import com.example.model.Track
import com.example.ui.theme.NocturneColors
import com.example.ui.theme.frostedFloatingBar
import com.example.ui.theme.glassSheetSurface
import com.example.ui.theme.matteCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

private val TrackRowShape = RoundedCornerShape(16.dp)
private val WallpaperBlurCache = LruCache<String, ImageBitmap>(8)

private suspend fun loadPreBlurredWallpaper(context: Context, uri: String): ImageBitmap? {
    WallpaperBlurCache.get(uri)?.let { return it }
    return withContext(Dispatchers.IO) {
        val rawBmp = decodeSmallBitmap(context, uri, 96) ?: return@withContext null
        val blurred = try {
            fastBoxBlur(rawBmp, radius = 6, passes = 2)
        } finally {
            rawBmp.recycle()
        }
        val imageBmp = blurred.asImageBitmap()
        WallpaperBlurCache.put(uri, imageBmp)
        imageBmp
    }
}

private suspend fun decodeSmallBitmap(context: Context, uri: String, targetSize: Int): Bitmap? {
    return runCatching {
        when {
            uri.startsWith("file://") -> {
                val path = Uri.parse(uri).path ?: return@runCatching null
                val file = File(path)
                if (!file.exists()) return@runCatching null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
                if (maxDim <= 0) return@runCatching null
                var sample = 1
                while (maxDim / (sample * 2) >= targetSize) sample *= 2
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = BitmapFactory.decodeFile(path, opts) ?: return@runCatching null
                scaleToSquare(decoded, targetSize)
            }
            uri.startsWith("content://") -> {
                val parsed = Uri.parse(uri)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(parsed)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
                val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
                if (maxDim <= 0) return@runCatching null
                var sample = 1
                while (maxDim / (sample * 2) >= targetSize) sample *= 2
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = context.contentResolver.openInputStream(parsed)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                } ?: return@runCatching null
                scaleToSquare(decoded, targetSize)
            }
            else -> {
                val req = ImageRequest.Builder(context)
                    .data(uri)
                    .size(targetSize, targetSize)
                    .allowHardware(false)
                    .build()
                val res = Coil.imageLoader(context).execute(req) as? SuccessResult ?: return@runCatching null
                val dr = res.drawable
                val bmp = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                dr.setBounds(0, 0, targetSize, targetSize)
                dr.draw(canvas)
                bmp
            }
        }
    }.getOrNull()
}

private fun scaleToSquare(src: Bitmap, target: Int): Bitmap {
    if (src.width == target && src.height == target && src.config == Bitmap.Config.ARGB_8888) {
        return src
    }
    val scaled = Bitmap.createScaledBitmap(src, target, target, true)
    if (scaled !== src) src.recycle()
    return if (scaled.config == Bitmap.Config.ARGB_8888) scaled else {
        val copy = scaled.copy(Bitmap.Config.ARGB_8888, false)
        scaled.recycle()
        copy
    }
}

private fun fastBoxBlur(src: Bitmap, radius: Int, passes: Int): Bitmap {
    val w = src.width
    val h = src.height
    val pixels = IntArray(w * h)
    val temp = IntArray(w * h)
    src.getPixels(pixels, 0, w, 0, 0, w, h)
    val div = radius * 2 + 1

    repeat(passes) {
        // Horizontal pass
        for (y in 0 until h) {
            val row = y * w
            var rSum = 0
            var gSum = 0
            var bSum = 0
            for (i in -radius..radius) {
                val p = pixels[row + i.coerceIn(0, w - 1)]
                rSum += (p ushr 16) and 0xFF
                gSum += (p ushr 8) and 0xFF
                bSum += p and 0xFF
            }
            for (x in 0 until w) {
                temp[row + x] = (0xFF shl 24) or ((rSum / div) shl 16) or ((gSum / div) shl 8) or (bSum / div)
                val outP = pixels[row + (x - radius).coerceIn(0, w - 1)]
                val inP = pixels[row + (x + radius + 1).coerceIn(0, w - 1)]
                rSum += ((inP ushr 16) and 0xFF) - ((outP ushr 16) and 0xFF)
                gSum += ((inP ushr 8) and 0xFF) - ((outP ushr 8) and 0xFF)
                bSum += (inP and 0xFF) - (outP and 0xFF)
            }
        }
        // Vertical pass
        for (x in 0 until w) {
            var rSum = 0
            var gSum = 0
            var bSum = 0
            for (i in -radius..radius) {
                val p = temp[i.coerceIn(0, h - 1) * w + x]
                rSum += (p ushr 16) and 0xFF
                gSum += (p ushr 8) and 0xFF
                bSum += p and 0xFF
            }
            for (y in 0 until h) {
                pixels[y * w + x] = (0xFF shl 24) or ((rSum / div) shl 16) or ((gSum / div) shl 8) or (bSum / div)
                val outP = temp[(y - radius).coerceIn(0, h - 1) * w + x]
                val inP = temp[(y + radius + 1).coerceIn(0, h - 1) * w + x]
                rSum += ((inP ushr 16) and 0xFF) - ((outP ushr 16) and 0xFF)
                gSum += ((inP ushr 8) and 0xFF) - ((outP ushr 8) and 0xFF)
                bSum += (inP and 0xFF) - (outP and 0xFF)
            }
        }
    }
    return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
}

@Composable
fun BlurredWallpaperImage(
    artUri: String?,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    if (artUri.isNullOrBlank()) return
    val context = LocalContext.current
    val cached = remember(artUri) { WallpaperBlurCache.get(artUri) }
    val bitmap by produceState<ImageBitmap?>(initialValue = cached, key1 = artUri) {
        if (value == null) {
            value = loadPreBlurredWallpaper(context, artUri)
        }
    }
    bitmap?.let { bmp ->
        Image(
            bitmap = bmp,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Medium,
            alpha = alpha,
            modifier = modifier
        )
    }
}

@Composable
fun TrackArtwork(
    artUri: String?,
    size: Dp,
    cornerRadius: Dp = 12.dp,
    accentColor: Color = Color.White,
    modifier: Modifier = Modifier,
    fallbackArtUri: String? = null
) {
    val context = LocalContext.current
    val targetPx = remember(size) {
        when {
            size >= 200.dp -> 720
            size >= 100.dp -> 384
            else -> 192
        }
    }
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val needsResolve = !artUri.isNullOrBlank() && artUri.startsWith("track_art://")
    val displayUri = if (!needsResolve) {
        artUri?.takeIf { it.isNotBlank() } ?: fallbackArtUri
    } else {
        val resolved by produceState<Pair<String?, Boolean>>(
            initialValue = null to false,
            key1 = artUri
        ) {
            value = AudioRepository.resolveArtUri(context, artUri) to true
        }
        resolved.first ?: if (resolved.second) fallbackArtUri else null
    }

    if (!displayUri.isNullOrBlank()) {
        AsyncImage(
            model = remember(displayUri, targetPx) {
                ImageRequest.Builder(context)
                    .data(displayUri)
                    .size(targetPx, targetPx)
                    .memoryCacheKey("${displayUri}_$targetPx")
                    .diskCacheKey(displayUri)
                    .crossfade(false)
                    .build()
            },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(Color(0xFF14141C))
        )
    } else {
        val bgBrush = remember(accentColor) {
            Brush.linearGradient(
                listOf(
                    accentColor.copy(alpha = 0.22f),
                    Color(0xFF14141C)
                )
            )
        }
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(bgBrush),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.55f),
                modifier = Modifier.size(size * 0.45f)
            )
        }
    }
}

@Composable
fun TrackRowItem(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoreClick: () -> Unit,
    fallbackArtUri: String? = null
) {
    val rowBg = if (isCurrent) Color.White.copy(alpha = 0.085f) else Color.Transparent
    val ext = remember(track.fileName) { track.fileName.substringAfterLast('.', "MP3").uppercase() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TrackRowShape)
            .background(rowBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackArtwork(
            artUri = track.artUri,
            size = 48.dp,
            cornerRadius = 12.dp,
            accentColor = accentColor,
            fallbackArtUri = fallbackArtUri
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isCurrent) accentColor else NocturneColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(textDirection = TextDirection.Content),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = track.artist.ifBlank { "Unknown Artist" },
                    color = NocturneColors.TextSecondary,
                    fontSize = 11.5.sp,
                    style = TextStyle(textDirection = TextDirection.Content),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = " · ",
                    color = NocturneColors.TextMuted.copy(alpha = 0.5f),
                    fontSize = 11.5.sp
                )
                Text(
                    text = formatDuration(track.durationSec),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = " · $ext",
                    color = NocturneColors.TextMuted.copy(alpha = 0.6f),
                    fontSize = 10.5.sp
                )
            }
        }
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(34.dp)) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = null,
                tint = if (isFavorite) accentColor else NocturneColors.TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onMoreClick, modifier = Modifier.size(34.dp)) {
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = null,
                tint = NocturneColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun MiniPlayerBar(
    track: Track,
    isPlaying: Boolean,
    positionMsFlow: StateFlow<Long>,
    durationMsFlow: StateFlow<Long>,
    isCrossfading: Boolean = false,
    crossfadeProgressFlow: StateFlow<Float>? = null,
    accentColor: Color,
    onOpenFull: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .frostedFloatingBar(RoundedCornerShape(26.dp))
            .testTag("mini_player_bar")
            .clickable(onClick = onOpenFull)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackArtwork(
                artUri = track.artUri,
                size = 44.dp,
                cornerRadius = 12.dp,
                accentColor = accentColor
            )

            Spacer(Modifier.width(10.dp))

            AnimatedContent(
                targetState = track,
                transitionSpec = {
                    (fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                        slideInVertically(tween(280, easing = FastOutSlowInEasing)) { it / 2 })
                        .togetherWith(
                            fadeOut(tween(180)) +
                                slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { -it / 2 }
                        )
                },
                label = "miniTrackTransition",
                modifier = Modifier.weight(1f)
            ) { current ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = current.title,
                        color = NocturneColors.TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(textDirection = TextDirection.Content),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = current.artist,
                            color = NocturneColors.TextSecondary,
                            fontSize = 11.5.sp,
                            style = TextStyle(textDirection = TextDirection.Content),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        AnimatedVisibility(
                            visible = isCrossfading,
                            enter = fadeIn(tween(200)),
                            exit = fadeOut(tween(200))
                        ) {
                            val pulse = rememberInfiniteTransition(label = "cfPulse")
                            val scale by pulse.animateFloat(
                                initialValue = 0.94f,
                                targetValue = 1.06f,
                                animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
                                label = "cfScale"
                            )
                            Row(
                                modifier = Modifier
                                    .scale(scale)
                                    .clip(RoundedCornerShape(50))
                                    .background(accentColor.copy(alpha = 0.22f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SwapHoriz,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                val cfProgress = if (crossfadeProgressFlow != null) {
                                    crossfadeProgressFlow.collectAsState().value
                                } else 0f
                                Text(
                                    text = "${(cfProgress * 100).toInt()}%",
                                    color = accentColor,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                        .testTag("mini_player_play_pause")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("mini_player_next")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = null,
                        tint = NocturneColors.TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("mini_player_close")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = NocturneColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        MiniPlayerProgressBar(
            positionMsFlow = positionMsFlow,
            durationMsFlow = durationMsFlow,
            accentColor = accentColor
        )
        Spacer(Modifier.height(3.dp))
    }
}

@Composable
private fun MiniPlayerProgressBar(
    positionMsFlow: StateFlow<Long>,
    durationMsFlow: StateFlow<Long>,
    accentColor: Color
) {
    val positionMs by positionMsFlow.collectAsState()
    val durationMs by durationMsFlow.collectAsState()
    val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .height(2.5.dp)
            .padding(horizontal = 12.dp),
        color = accentColor,
        trackColor = Color.White.copy(alpha = 0.08f)
    )
}

@Composable
fun TrackOptionsBottomSheet(
    track: Track,
    strings: AppStrings,
    isFavorite: Boolean,
    accentColor: Color,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRename: () -> Unit,
    onViewLyrics: () -> Unit,
    onShowDetails: () -> Unit,
    onExclude: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    var dragOffsetY by remember(track.id) { mutableFloatStateOf(0f) }
    val animatedOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "sheetDragY"
    )
    val dismissThresholdPx = with(LocalDensity.current) { 110.dp.toPx() }
    val sheetShape = remember { RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.48f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, animatedOffsetY.roundToInt().coerceAtLeast(0)) }
                .glassSheetSurface(sheetShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .pointerInput(track.id) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                        },
                        onDragEnd = {
                            if (dragOffsetY > dismissThresholdPx) {
                                onDismiss()
                            } else {
                                dragOffsetY = 0f
                            }
                        },
                        onDragCancel = { dragOffsetY = 0f }
                    )
                }
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 2.dp, bottom = 12.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.34f))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackArtwork(
                    artUri = track.artUri,
                    size = 46.dp,
                    cornerRadius = 11.dp,
                    accentColor = accentColor
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp,
                        style = TextStyle(textDirection = TextDirection.Content),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = track.artist.ifBlank { strings.unknownArtist },
                        color = NocturneColors.TextSecondary,
                        fontSize = 12.sp,
                        style = TextStyle(textDirection = TextDirection.Content),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 1.dp,
                color = Color.White.copy(alpha = 0.10f)
            )
            SheetActionRow(Icons.AutoMirrored.Rounded.PlaylistPlay, strings.playNext, accentColor, onPlayNext)
            SheetActionRow(Icons.AutoMirrored.Rounded.QueueMusic, strings.addToQueue, accentColor, onAddToQueue)
            SheetActionRow(
                Icons.Rounded.Favorite,
                if (isFavorite) strings.removeFromFavorites else strings.addToFavorites,
                accentColor,
                onToggleFavorite
            )
            SheetActionRow(Icons.AutoMirrored.Rounded.PlaylistAdd, strings.addToPlaylist, accentColor, onAddToPlaylist)
            SheetActionRow(Icons.Rounded.Edit, strings.renameTrackAction, accentColor, onRename)
            SheetActionRow(Icons.Rounded.Lyrics, strings.viewLyrics, accentColor, onViewLyrics)
            SheetActionRow(Icons.Rounded.Info, strings.trackDetails, accentColor, onShowDetails)
            SheetActionRow(Icons.Rounded.Block, strings.deleteFromLibrary, NocturneColors.Danger, onExclude)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun RenameTrackDialog(
    track: Track,
    strings: AppStrings,
    accentColor: Color,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var titleText by androidx.compose.runtime.remember(track.id) {
        androidx.compose.runtime.mutableStateOf(track.title)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NocturneColors.MatteElevatedBg,
        title = {
            Text(strings.renameTrackTitle, color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = titleText,
                onValueChange = { titleText = it },
                singleLine = true,
                placeholder = {
                    Text(strings.renameTrackPlaceholder, color = NocturneColors.TextMuted)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                    cursorColor = accentColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rename_track_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(titleText) },
                enabled = titleText.trim().isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.Black
                ),
                modifier = Modifier.testTag("rename_track_confirm")
            ) {
                Text(strings.saveAction, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = NocturneColors.TextSecondary)
            }
        }
    )
}

@Composable
fun TrackDetailsDialog(
    track: Track,
    strings: AppStrings,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NocturneColors.MatteElevatedBg,
        title = { Text(strings.trackDetails, color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailRow(strings.fileNameLabel, track.fileName)
                DetailRow(strings.fileDuration, formatDuration(track.durationSec))
                DetailRow(strings.fileSize, "${"%.2f".format(track.sizeBytes / (1024.0 * 1024.0))} MB")
                DetailRow(strings.fileFormat, track.mimeType.ifBlank { "audio" })
                DetailRow(strings.folderLocation, track.folderPath)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.close, color = accentColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun PlaylistPickerDialog(
    track: Track,
    playlists: List<Playlist>,
    strings: AppStrings,
    onSelectPlaylist: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NocturneColors.MatteElevatedBg,
        title = { Text(strings.addToPlaylist, color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            if (playlists.isEmpty()) {
                Text(strings.noPlaylistsDesc, color = NocturneColors.TextSecondary, fontSize = 13.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    playlists.forEach { pl ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .matteCard(RoundedCornerShape(12.dp))
                                .clickable { onSelectPlaylist(pl.id, track.id) }
                                .padding(12.dp)
                        ) {
                            Text("${pl.name} (${pl.trackIds.size})", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.close, color = NocturneColors.TextSecondary)
            }
        }
    )
}

@Composable
private fun SheetActionRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, color = NocturneColors.TextMuted, fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

fun formatDuration(seconds: Double): String {
    val total = seconds.toLong().coerceAtLeast(0L)
    val mins = total / 60
    val secs = total % 60
    return mins.toString().padStart(2, '0') + ":" + secs.toString().padStart(2, '0')
}

fun formatMillis(ms: Long): String {
    val total = (ms / 1000L).coerceAtLeast(0L)
    val mins = total / 60
    val secs = total % 60
    return mins.toString() + ":" + secs.toString().padStart(2, '0')
}
