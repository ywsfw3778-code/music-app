package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AppStrings
import com.example.model.LrcSearchResult
import com.example.model.LyricsData
import com.example.model.RepeatMode
import com.example.model.Track
import com.example.ui.components.TrackArtwork
import com.example.ui.components.formatMillis
import com.example.ui.theme.NocturneColors
import com.example.ui.theme.matteCard

@Composable
fun FullPlayerSheet(
    strings: AppStrings,
    track: Track,
    isPlaying: Boolean,
    isFavorite: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    crossfadeEnabled: Boolean,
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
    var showSleepDialog by remember { mutableStateOf(false) }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Blurred Cover + Dynamic Radial Backdrop
        if (!track.artUri.isNullOrBlank()) {
            AsyncImage(
                model = track.artUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.28f,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(65.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.18f),
                            Color.Black.copy(alpha = 0.78f),
                            Color.Black.copy(alpha = 0.96f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(strings.nowPlaying, color = NocturneColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    if (crossfadeEnabled) {
                        Text(strings.crossfadeActive, color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Row {
                    IconButton(onClick = { showSleepDialog = true }) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Bedtime,
                                null,
                                tint = if (sleepTimerSeconds != null) accentColor else NocturneColors.TextSecondary,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                    IconButton(onClick = onMoreClick) {
                        Icon(Icons.Rounded.MoreVert, null, tint = NocturneColors.TextSecondary, modifier = Modifier.size(22.dp))
                    }
                }
            }

            if (sleepTimerSeconds != null) {
                Text(
                    text = "${strings.sleepTimerTitle}: ${sleepTimerSeconds / 60}:${"%02d".format(sleepTimerSeconds % 60)}",
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(Modifier.height(8.dp))

            // 3 Top Tabs: Artwork | Lyrics | Queue
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(strings.artworkTab, strings.lyricsTab, strings.queueTab).forEachIndexed { idx, label ->
                    val selected = activePage == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) accentColor else Color.Transparent)
                            .clickable { activePage = idx }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.Black else NocturneColors.TextSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Main Content Page
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = activePage,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                    label = "playerPage"
                ) { page ->
                    when (page) {
                        0 -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                TrackArtwork(
                                    artUri = track.artUri,
                                    size = 270.dp,
                                    cornerRadius = 26.dp,
                                    accentColor = accentColor
                                )
                                Spacer(Modifier.height(22.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "${track.artist} · ${track.album}",
                                            color = NocturneColors.TextSecondary,
                                            fontSize = 13.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(onClick = onToggleFavorite) {
                                        Icon(
                                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                            contentDescription = null,
                                            tint = if (isFavorite) accentColor else NocturneColors.TextSecondary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            }
                        }

                        1 -> {
                            LyricsPage(
                                strings = strings,
                                lyrics = lyrics,
                                isFetching = isFetchingLyrics,
                                positionMs = positionMs,
                                lyricOffsetMs = lyricOffsetMs,
                                accentColor = accentColor,
                                onSeek = onSeek,
                                onOpenManualSearch = { showManualSearch = true },
                                onAttachLrc = { lrcPicker.launch(arrayOf("*/*")) },
                                onAdjustOffset = onAdjustLyricOffset
                            )
                        }

                        else -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${strings.upNext} (${queue.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (queue.size > 1) {
                                        TextButton(onClick = onClearQueue) {
                                            Text(strings.clearQueue, color = NocturneColors.Danger, fontSize = 12.sp)
                                        }
                                    }
                                }
                                if (queue.isEmpty()) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(strings.queueEmpty, color = NocturneColors.TextSecondary)
                                    }
                                } else {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(queue, key = { it.id }) { qTrack ->
                                            val current = qTrack.id == track.id
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .matteCard(RoundedCornerShape(14.dp))
                                                    .clickable { onPlayFromQueue(qTrack) }
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TrackArtwork(qTrack.artUri, size = 40.dp, cornerRadius = 10.dp, accentColor = accentColor)
                                                Spacer(Modifier.width(10.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text(
                                                        qTrack.title,
                                                        color = if (current) accentColor else Color.White,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 13.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(qTrack.artist, color = NocturneColors.TextSecondary, fontSize = 11.sp, maxLines = 1)
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

            // SeekBar
            val safeDur = durationMs.coerceAtLeast(1L)
            var isDragging by remember { mutableStateOf(false) }
            var dragPos by remember { mutableFloatStateOf(0f) }
            val currentVal = if (isDragging) dragPos else positionMs.toFloat().coerceIn(0f, safeDur.toFloat())

            Slider(
                value = currentVal,
                onValueChange = {
                    isDragging = true
                    dragPos = it
                },
                onValueChangeFinished = {
                    isDragging = false
                    onSeek(dragPos.toLong())
                },
                valueRange = 0f..safeDur.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = Color.White.copy(alpha = 0.14f)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatMillis(currentVal.toLong()), color = NocturneColors.TextSecondary, fontSize = 11.5.sp)
                Text(formatMillis(durationMs), color = NocturneColors.TextSecondary, fontSize = 11.5.sp)
            }

            Spacer(Modifier.height(8.dp))

            // Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        null,
                        tint = if (shuffleEnabled) accentColor else NocturneColors.TextMuted,
                        modifier = Modifier.size(23.dp)
                    )
                }
                IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(34.dp))
                }
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.size(34.dp))
                }
                IconButton(onClick = onCycleRepeat) {
                    Icon(
                        imageVector = if (repeatMode == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = null,
                        tint = if (repeatMode != RepeatMode.OFF) accentColor else NocturneColors.TextMuted,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }

    if (showManualSearch) {
        var query by remember(track.id) { mutableStateOf("${track.title} ${track.artist}") }
        AlertDialog(
            onDismissRequest = { showManualSearch = false },
            containerColor = NocturneColors.MatteElevatedBg,
            title = { Text(strings.manualSearchTitle, color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                        Button(
                            onClick = { onSearchManualLyrics(query) },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black)
                        ) {
                            Text(strings.manualSearchAction, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    if (isSearchingManual) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accentColor, modifier = Modifier.size(26.dp))
                        }
                    } else if (manualResults.isEmpty()) {
                        Text(strings.searchResultsEmpty, color = NocturneColors.TextMuted, fontSize = 12.sp)
                    } else {
                        LazyColumn(modifier = Modifier.height(220.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(manualResults, key = { it.id }) { res ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .matteCard(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onApplyManualLyrics(res)
                                            showManualSearch = false
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(res.trackName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                        Text(res.artistName, color = NocturneColors.TextSecondary, fontSize = 11.5.sp, maxLines = 1)
                                    }
                                    if (res.syncedLyrics != null) {
                                        Text(strings.syncedBadge, color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showManualSearch = false }) {
                    Text(strings.close, color = NocturneColors.TextSecondary)
                }
            }
        )
    }

    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            containerColor = NocturneColors.MatteElevatedBg,
            title = { Text(strings.sleepTimerTitle, color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 20, 30, 45, 60).forEach { mins ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .matteCard(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSetSleepTimer(mins)
                                    showSleepDialog = false
                                }
                                .padding(12.dp)
                        ) {
                            Text("$mins ${strings.minutesUnit}", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (sleepTimerSeconds != null) {
                        TextButton(
                            onClick = {
                                onSetSleepTimer(0)
                                showSleepDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strings.sleepTimerOff, color = NocturneColors.Danger, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepDialog = false }) {
                    Text(strings.close, color = NocturneColors.TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun LyricsPage(
    strings: AppStrings,
    lyrics: LyricsData?,
    isFetching: Boolean,
    positionMs: Long,
    lyricOffsetMs: Long,
    accentColor: Color,
    onSeek: (Long) -> Unit,
    onOpenManualSearch: () -> Unit,
    onAttachLrc: () -> Unit,
    onAdjustOffset: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable(onClick = onOpenManualSearch)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Search, null, tint = accentColor, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.searchLyricsOnline, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable(onClick = onAttachLrc)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.UploadFile, null, tint = accentColor, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.uploadLrcFile, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "-0.5s",
                    color = NocturneColors.TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable { onAdjustOffset(-500L) }.padding(4.dp)
                )
                Text(
                    "${lyricOffsetMs / 1000.0}s",
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text(
                    "+0.5s",
                    color = NocturneColors.TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable { onAdjustOffset(500L) }.padding(4.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        when {
            isFetching -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = accentColor)
                        Spacer(Modifier.height(10.dp))
                        Text(strings.fetchingLyrics, color = NocturneColors.TextSecondary, fontSize = 13.sp)
                    }
                }
            }

            lyrics != null && lyrics.lines.isNotEmpty() -> {
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(lyrics.lines.size) { idx ->
                        val line = lyrics.lines[idx]
                        val isActive = idx == activeIdx
                        Text(
                            text = line.text,
                            color = if (isActive) accentColor else Color.White.copy(alpha = 0.45f),
                            fontSize = if (isActive) 19.sp else 15.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSeek((line.timeMs - lyricOffsetMs).coerceAtLeast(0L)) }
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        )
                    }
                }
            }

            lyrics != null && !lyrics.plainLyrics.isNullOrBlank() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(strings.plainLyricsNote, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = lyrics.plainLyrics,
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 15.sp,
                        lineHeight = 25.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(strings.noLyricsFound, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(strings.noLyricsHint, color = NocturneColors.TextSecondary, fontSize = 12.5.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
