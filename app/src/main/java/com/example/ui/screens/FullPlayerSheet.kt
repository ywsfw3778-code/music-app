package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.AppStrings
import com.example.model.LrcSearchResult
import com.example.model.LyricsData
import com.example.model.RepeatMode
import com.example.model.Track
import com.example.ui.components.TrackArtwork
import com.example.ui.components.formatDuration
import com.example.ui.components.formatMillis
import com.example.ui.theme.LocalWaterGlassState
import com.example.ui.theme.NocturneColors
import com.example.ui.theme.matteCard
import com.example.ui.theme.rememberWaterGlassState
import com.example.ui.theme.waterDropBar
import com.example.ui.theme.waterGlassBackground
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun FullPlayerSheet(
    strings: AppStrings,
    track: Track,
    isPlaying: Boolean,
    isFavorite: Boolean,
    positionMsFlow: StateFlow<Long>,
    durationMsFlow: StateFlow<Long>,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    crossfadeEnabled: Boolean,
    crossfadeSec: Int,
    isCrossfading: Boolean = false,
    crossfadeProgressFlow: StateFlow<Float>,
    showArtworkBg: Boolean = true,
    glassBlurEnabled: Boolean = true,
    queue: List<Track>,
    lyrics: LyricsData?,
    isFetchingLyrics: Boolean,
    lyricOffsetMs: Long,
    manualResults: List<LrcSearchResult>,
    isSearchingManual: Boolean,
    sleepTimerSeconds: Int?,
    accentColor: Color,
    secondaryColor: Color,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onPlayFromQueue: (Track) -> Unit,
    onClearQueue: () -> Unit,
    onSearchManualLyrics: (String) -> Unit,
    onApplyManualLyrics: (LrcSearchResult) -> Unit,
    onApplyLrcFile: (String) -> Unit,
    onAdjustLyricOffset: (Long) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onMoreClick: () -> Unit
) {
    var activePage by remember { mutableIntStateOf(0) } // 0=Artwork, 1=Lyrics, 2=Queue
    var showManualSearch by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val lrcPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()?.let { content ->
                onApplyLrcFile(content)
            }
        }
    }

    val playerGlassState = rememberWaterGlassState(
        blurEnabled = glassBlurEnabled,
        accentColor = accentColor,
        secondaryColor = secondaryColor
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .waterGlassBackground(playerGlassState)
        ) {
            val bgUri = if (showArtworkBg) {
                track.artUri?.takeIf { it.isNotBlank() && !it.startsWith("track_art://") }
            } else null
            Crossfade(
                targetState = bgUri,
                animationSpec = tween(550, easing = FastOutSlowInEasing),
                label = "playerBgFade"
            ) { artUri ->
                if (!artUri.isNullOrBlank()) {
                    AsyncImage(
                        model = remember(artUri) {
                            ImageRequest.Builder(context)
                                .data(artUri)
                                .size(256)
                                .memoryCacheKey("${artUri}_bg256")
                                .diskCacheKey(artUri)
                                .crossfade(false)
                                .build()
                        },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alpha = 0.55f,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(26.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            if (showArtworkBg) {
                                listOf(
                                    accentColor.copy(alpha = 0.28f),
                                    secondaryColor.copy(alpha = 0.20f),
                                    Color.Black.copy(alpha = 0.65f)
                                )
                            } else {
                                listOf(
                                    accentColor.copy(alpha = 0.78f),
                                    secondaryColor.copy(alpha = 0.58f),
                                    accentColor.copy(alpha = 0.36f)
                                )
                            }
                        )
                    )
            )
        }

        CompositionLocalProvider(LocalWaterGlassState provides playerGlassState) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .waterDropBar(CircleShape)
                            .testTag("full_player_close")
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.KeyboardArrowDown, strings.close, tint = Color.White, modifier = Modifier.size(22.dp))
                    }

                    Row(
                        modifier = Modifier
                            .waterDropBar(RoundedCornerShape(50))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(strings.artworkTab, strings.lyricsTab, strings.queueTab).forEachIndexed { idx, label ->
                            val selected = activePage == idx
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (selected) NocturneColors.WaterDropPillBg else Color.Transparent)
                                    .testTag("full_player_tab_$idx")
                                    .clickable { activePage = idx }
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) Color.White else Color(0xFFCBD5E1),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .waterDropBar(CircleShape)
                            .testTag("full_player_more")
                            .clickable(onClick = onMoreClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.MoreHoriz, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(10.dp))

                val isRtlLayout = LocalLayoutDirection.current == LayoutDirection.Rtl
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = activePage,
                        transitionSpec = {
                            val forward = targetState >= initialState
                            val dir = if (isRtlLayout) (if (forward) -1 else 1) else (if (forward) 1 else -1)
                            (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { dir * it } +
                                fadeIn(tween(240)))
                                .togetherWith(
                                    slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { -dir * it } +
                                        fadeOut(tween(180))
                                )
                        },
                        label = "playerPage"
                    ) { page ->
                        when (page) {
                            0 -> {
                                val artScale by animateFloatAsState(
                                    targetValue = if (isPlaying) 1f else 0.88f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.8f),
                                    label = "artScale"
                                )
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Crossfade(
                                        targetState = track.artUri,
                                        animationSpec = tween(420, easing = FastOutSlowInEasing),
                                        label = "playerArtFade"
                                    ) { art ->
                                        TrackArtwork(
                                            artUri = art,
                                            size = 285.dp,
                                            cornerRadius = 28.dp,
                                            accentColor = accentColor,
                                            modifier = Modifier.scale(artScale)
                                        )
                                    }
                                    if (crossfadeEnabled) {
                                        Spacer(Modifier.height(14.dp))
                                        val cfProgress = if (isCrossfading) crossfadeProgressFlow.collectAsState().value else 0f
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = if (isCrossfading) "Crossfade" else "${strings.crossfadeDuration}:",
                                                    color = if (isCrossfading) accentColor else Color(0xFFE2E8F0).copy(alpha = 0.85f),
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isCrossfading) FontWeight.Bold else FontWeight.Normal
                                                )
                                                Text(
                                                    text = if (isCrossfading) "${(cfProgress * 100).toInt()}%" else "$crossfadeSec",
                                                    color = accentColor,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (!isCrossfading) {
                                                    Text(
                                                        text = strings.secondsUnit,
                                                        color = Color(0xFFE2E8F0).copy(alpha = 0.85f),
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                            AnimatedVisibility(
                                                visible = isCrossfading,
                                                enter = fadeIn(tween(200)) + expandVertically(),
                                                exit = fadeOut(tween(200)) + shrinkVertically()
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(top = 6.dp)
                                                        .width(140.dp)
                                                        .height(4.dp)
                                                        .clip(RoundedCornerShape(50))
                                                        .background(Color.White.copy(alpha = 0.16f))
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(cfProgress.coerceIn(0f, 1f))
                                                            .height(4.dp)
                                                            .clip(RoundedCornerShape(50))
                                                            .background(
                                                                Brush.horizontalGradient(
                                                                    listOf(accentColor.copy(alpha = 0.65f), accentColor)
                                                                )
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            1 -> {
                                LyricsPage(
                                    strings = strings,
                                    track = track,
                                    lyrics = lyrics,
                                    isFetching = isFetchingLyrics,
                                    positionMsFlow = positionMsFlow,
                                    lyricOffsetMs = lyricOffsetMs,
                                    showManualSearch = showManualSearch,
                                    manualResults = manualResults,
                                    isSearchingManual = isSearchingManual,
                                    accentColor = accentColor,
                                    onSeek = onSeek,
                                    onToggleManualSearch = { showManualSearch = !showManualSearch },
                                    onSearchManualLyrics = onSearchManualLyrics,
                                    onApplyManualLyrics = {
                                        onApplyManualLyrics(it)
                                        showManualSearch = false
                                    },
                                    onAttachLrc = { lrcPicker.launch(arrayOf("*/*")) },
                                    onAdjustOffset = onAdjustLyricOffset
                                )
                            }

                            else -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(strings.upNext, color = Color(0xFFCBD5E1), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        if (queue.size > 1) {
                                            Text(
                                                text = strings.clearQueue,
                                                color = NocturneColors.TextSecondary,
                                                fontSize = 12.sp,
                                                modifier = Modifier.clickable(onClick = onClearQueue)
                                            )
                                        }
                                    }
                                    if (queue.isEmpty()) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(strings.queueEmpty, color = NocturneColors.TextMuted, fontSize = 12.5.sp)
                                        }
                                    } else {
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(queue, key = { it.id }, contentType = { "queue_track" }) { qTrack ->
                                                val current = qTrack.id == track.id
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(if (current) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                                                        .clickable { onPlayFromQueue(qTrack) }
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    TrackArtwork(qTrack.artUri, size = 40.dp, cornerRadius = 8.dp, accentColor = accentColor)
                                                    Spacer(Modifier.width(12.dp))
                                                    Column(Modifier.weight(1f)) {
                                                        Text(
                                                            qTrack.title,
                                                            color = if (current) accentColor else Color(0xFFF1F5F9),
                                                            fontWeight = FontWeight.Medium,
                                                            fontSize = 14.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            "${qTrack.artist} · ${formatDuration(qTrack.durationSec)}",
                                                            color = NocturneColors.TextSecondary,
                                                            fontSize = 12.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedContent(
                            targetState = track,
                            transitionSpec = {
                                (fadeIn(tween(260, easing = FastOutSlowInEasing)) + slideInVertically(tween(280, easing = FastOutSlowInEasing)) { it / 2 })
                                    .togetherWith(fadeOut(tween(180)) + slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { -it / 2 })
                            },
                            modifier = Modifier.weight(1f),
                            label = "playerTrackMeta"
                        ) { curTrack ->
                            Column {
                                Text(
                                    text = curTrack.title,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    style = TextStyle(textDirection = TextDirection.Content),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "${curTrack.artist} · ${curTrack.album}",
                                    color = NocturneColors.TextSecondary,
                                    fontSize = 13.sp,
                                    style = TextStyle(textDirection = TextDirection.Content),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.testTag("full_player_favorite")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) accentColor else Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    PlayerSeekBar(
                        positionMsFlow = positionMsFlow,
                        durationMsFlow = durationMsFlow,
                        accentColor = accentColor,
                        onSeek = onSeek
                    )

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onCycleRepeat,
                                modifier = Modifier.testTag("full_player_repeat")
                            ) {
                                Icon(
                                    imageVector = if (repeatMode == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                    contentDescription = null,
                                    tint = if (repeatMode != RepeatMode.OFF) accentColor else NocturneColors.TextMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(
                                onClick = onPrevious,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("full_player_previous")
                            ) {
                                Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            IconButton(
                                onClick = onTogglePlayPause,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                                    .testTag("full_player_play_pause")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            IconButton(
                                onClick = onNext,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("full_player_next")
                            ) {
                                Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            IconButton(
                                onClick = onToggleShuffle,
                                modifier = Modifier.testTag("full_player_shuffle")
                            ) {
                                Icon(
                                    Icons.Rounded.Shuffle,
                                    null,
                                    tint = if (shuffleEnabled) accentColor else NocturneColors.TextMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerSeekBar(
    positionMsFlow: StateFlow<Long>,
    durationMsFlow: StateFlow<Long>,
    accentColor: Color,
    onSeek: (Long) -> Unit
) {
    val positionMs by positionMsFlow.collectAsState()
    val durationMs by durationMsFlow.collectAsState()
    val safeDur = durationMs.coerceAtLeast(1L)
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val currentFraction = if (isDragging) dragFraction else (positionMs.toFloat() / safeDur.toFloat()).coerceIn(0f, 1f)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val fillBrush = remember(accentColor) {
        Brush.horizontalGradient(listOf(accentColor.copy(alpha = 0.75f), accentColor))
    }

    Column {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .pointerInput(safeDur, isRtl) {
                    detectTapGestures { offset ->
                        val raw = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val frac = if (isRtl) 1f - raw else raw
                        onSeek((frac * safeDur).toLong())
                    }
                }
                .pointerInput(safeDur, isRtl) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val raw = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            dragFraction = if (isRtl) 1f - raw else raw
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val raw = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            dragFraction = if (isRtl) 1f - raw else raw
                        },
                        onDragEnd = {
                            onSeek((dragFraction * safeDur).toLong())
                            isDragging = false
                        },
                        onDragCancel = { isDragging = false }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val density = LocalDensity.current
            val trackWidthPx = with(density) { maxWidth.toPx() }
            val thumbSizePx = with(density) { 18.dp.toPx() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.16f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(currentFraction)
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(fillBrush)
                )
            }
            Box(
                modifier = Modifier
                    .offset {
                        val x = (currentFraction * (trackWidthPx - thumbSizePx)).roundToInt()
                        IntOffset(x, 0)
                    }
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .border(2.5.dp, Color.White, CircleShape)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatMillis((currentFraction * safeDur).toLong()),
                color = accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                formatMillis(durationMs),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LyricsPage(
    strings: AppStrings,
    track: Track,
    lyrics: LyricsData?,
    isFetching: Boolean,
    positionMsFlow: StateFlow<Long>,
    lyricOffsetMs: Long,
    showManualSearch: Boolean,
    manualResults: List<LrcSearchResult>,
    isSearchingManual: Boolean,
    accentColor: Color,
    onSeek: (Long) -> Unit,
    onToggleManualSearch: () -> Unit,
    onSearchManualLyrics: (String) -> Unit,
    onApplyManualLyrics: (LrcSearchResult) -> Unit,
    onAttachLrc: () -> Unit,
    onAdjustOffset: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    lyrics != null && lyrics.lines.isNotEmpty() -> strings.syncedBadge
                    lyrics != null && !lyrics.plainLyrics.isNullOrBlank() -> strings.plainLyricsNote
                    else -> ""
                },
                color = NocturneColors.TextSecondary,
                fontSize = 11.5.sp
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (lyrics != null && lyrics.lines.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .waterDropBar(RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("-0.5s", color = Color(0xFFCBD5E1), fontSize = 11.sp, modifier = Modifier.clickable { onAdjustOffset(-500L) })
                        Text("${lyricOffsetMs / 1000.0}s", color = NocturneColors.TextSecondary, fontSize = 11.sp)
                        Text("+0.5s", color = Color(0xFFCBD5E1), fontSize = 11.sp, modifier = Modifier.clickable { onAdjustOffset(500L) })
                    }
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .waterDropBar(CircleShape)
                        .clickable(onClick = onToggleManualSearch),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Search, null, tint = Color(0xFFE2E8F0), modifier = Modifier.size(15.dp))
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .waterDropBar(CircleShape)
                        .clickable(onClick = onAttachLrc),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.UploadFile, null, tint = Color(0xFFE2E8F0), modifier = Modifier.size(15.dp))
                }
            }
        }

        if (showManualSearch) {
            var query by remember(track.id) { mutableStateOf("${track.title} ${track.artist}") }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .waterDropBar(RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.manualSearchTitle, color = Color(0xFFE2E8F0), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Rounded.Close, null, tint = NocturneColors.TextSecondary, modifier = Modifier.size(18.dp).clickable(onClick = onToggleManualSearch))
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(strings.manualSearchPlaceholder, color = NocturneColors.TextMuted, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.35f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                        )
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable { onSearchManualLyrics(query) }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(strings.manualSearchAction, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(10.dp))
                when {
                    isSearchingManual -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    }
                    manualResults.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(strings.searchResultsEmpty, color = NocturneColors.TextMuted, fontSize = 12.sp)
                    }
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(manualResults, key = { it.id }) { res ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .matteCard(RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(res.trackName, color = Color(0xFFF1F5F9), fontSize = 13.5.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                    Text(
                                        "${res.artistName}${if (res.syncedLyrics != null) " · ${strings.syncedBadge}" else ""}",
                                        color = NocturneColors.TextSecondary,
                                        fontSize = 11.5.sp,
                                        maxLines = 1
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .clickable { onApplyManualLyrics(res) }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(strings.applyLyrics, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            when {
                isFetching -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = accentColor, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.height(10.dp))
                            Text(strings.fetchingLyrics, color = NocturneColors.TextSecondary, fontSize = 13.sp)
                        }
                    }
                }

                lyrics != null && lyrics.lines.isNotEmpty() -> {
                    val positionMs by positionMsFlow.collectAsState()
                    val effectivePos = (positionMs + lyricOffsetMs).coerceAtLeast(0L)
                    val activeIdx = remember(effectivePos, lyrics.lines) {
                        lyrics.lines.indexOfLast { it.timeMs <= effectivePos }.coerceAtLeast(0)
                    }
                    val listState = rememberLazyListState()
                    LaunchedEffect(activeIdx) {
                        listState.animateScrollToItem((activeIdx - 2).coerceAtLeast(0))
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Text(
                                text = strings.tapLineToSeek,
                                color = NocturneColors.TextMuted,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        items(lyrics.lines.size, key = { idx -> "${lyrics.lines[idx].timeMs}_$idx" }) { idx ->
                            val line = lyrics.lines[idx]
                            val isActive = idx == activeIdx
                            val distance = abs(idx - activeIdx)

                            val lineScale by animateFloatAsState(
                                targetValue = if (isActive) 1.04f else 0.96f,
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessLow,
                                    dampingRatio = Spring.DampingRatioLowBouncy
                                ),
                                label = "lyricScale"
                            )
                            val lineAlpha by animateFloatAsState(
                                targetValue = when {
                                    isActive -> 1f
                                    distance == 1 -> 0.58f
                                    distance == 2 -> 0.36f
                                    else -> 0.20f
                                },
                                animationSpec = tween(320, easing = FastOutSlowInEasing),
                                label = "lyricAlpha"
                            )
                            val lineOffsetY by animateDpAsState(
                                targetValue = when {
                                    isActive -> 0.dp
                                    idx < activeIdx -> (-2).dp
                                    else -> 4.dp
                                },
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "lyricOffset"
                            )
                            val lineColor by animateColorAsState(
                                targetValue = if (isActive) Color.White else Color(0xFFE2E8F0),
                                animationSpec = tween(280, easing = FastOutSlowInEasing),
                                label = "lyricColor"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = lineOffsetY)
                                    .graphicsLayer {
                                        scaleX = lineScale
                                        scaleY = lineScale
                                        alpha = lineAlpha
                                        transformOrigin = TransformOrigin.Center
                                    }
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isActive) Color.White.copy(alpha = 0.07f) else Color.Transparent
                                    )
                                    .clickable { onSeek((line.timeMs - lyricOffsetMs).coerceAtLeast(0L)) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = line.text,
                                    color = lineColor,
                                    fontSize = if (isActive) 20.5.sp else 16.5.sp,
                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    lineHeight = if (isActive) 31.sp else 25.sp,
                                    style = TextStyle(
                                        textDirection = TextDirection.Content,
                                        shadow = if (isActive) {
                                            Shadow(
                                                color = accentColor.copy(alpha = 0.45f),
                                                offset = Offset.Zero,
                                                blurRadius = 14f
                                            )
                                        } else null
                                    )
                                )
                            }
                        }
                    }
                }

                lyrics != null && !lyrics.plainLyrics.isNullOrBlank() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = lyrics.plainLyrics,
                            color = Color(0xFFE2E8F0),
                            fontSize = 16.sp,
                            lineHeight = 28.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                else -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.Lyrics, null, tint = Color(0xFF475569), modifier = Modifier.size(34.dp))
                            Spacer(Modifier.height(10.dp))
                            Text(strings.noLyricsFound, color = Color(0xFFE2E8F0), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(strings.noLyricsHint, color = NocturneColors.TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier
                                        .waterDropBar(RoundedCornerShape(14.dp))
                                        .clickable(onClick = onToggleManualSearch)
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Rounded.Search, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Text(strings.searchLyricsOnline, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Row(
                                    modifier = Modifier
                                        .waterDropBar(RoundedCornerShape(14.dp))
                                        .clickable(onClick = onAttachLrc)
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Rounded.UploadFile, null, tint = Color(0xFFE2E8F0), modifier = Modifier.size(14.dp))
                                    Text(strings.uploadLrcFile, color = Color(0xFFE2E8F0), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
