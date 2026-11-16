package com.example

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SukoonApp(
    viewModel: SukoonViewModel,
    loadArtwork: suspend (Long, Long) -> Bitmap?
) {
    val tracks by viewModel.tracks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val librarySubTab by viewModel.librarySubTab.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val selectedPlaylistId by viewModel.selectedPlaylistId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val currentTrackId by viewModel.currentTrackId.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val positionMs by viewModel.positionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val shuffle by viewModel.shuffle.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val queueIds by viewModel.queueIds.collectAsState()
    val isNowPlayingOpen by viewModel.isNowPlayingOpen.collectAsState()
    val nowPlayingTab by viewModel.nowPlayingTab.collectAsState()
    val contextMenuTrack by viewModel.contextMenuTrack.collectAsState()
    val isCreatePlaylistOpen by viewModel.isCreatePlaylistOpen.collectAsState()
    val editingPlaylistId by viewModel.editingPlaylistId.collectAsState()
    val playlistSettingsId by viewModel.playlistSettingsId.collectAsState()
    val activePalette by viewModel.activePalette.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val lyricsResults by viewModel.lyricsResults.collectAsState()
    val isSearchingLyrics by viewModel.isSearchingLyrics.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val currentTrack = remember(tracks, currentTrackId) {
        tracks.firstOrNull { it.id == currentTrackId }
    }

    val animTop by animateColorAsState(
        targetValue = activePalette.glow.copy(alpha = 0.78f),
        animationSpec = tween(700),
        label = "bgTop"
    )
    val animMid by animateColorAsState(
        targetValue = activePalette.secondary.copy(alpha = 0.52f),
        animationSpec = tween(700),
        label = "bgMid"
    )
    val animBottom by animateColorAsState(
        targetValue = activePalette.glow.copy(alpha = 0.20f),
        animationSpec = tween(700),
        label = "bgBottom"
    )
    val animAccent by animateColorAsState(
        targetValue = activePalette.accent,
        animationSpec = tween(700),
        label = "accent"
    )

    val colorScheme = darkColorScheme(
        primary = animAccent,
        background = Color(0xFF0A0A0E),
        surface = Color(0xFF141418),
        onBackground = Color.White,
        onSurface = Color.White
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = colorScheme) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A0A0E))
                    .background(Brush.verticalGradient(colors = listOf(animTop, animMid, animBottom)))
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopHeader(
                        activeTab = activeTab,
                        isScanning = isScanning,
                        searchQuery = searchQuery,
                        sortBy = sortBy,
                        onSearchChange = viewModel::setSearchQuery,
                        onCycleSort = viewModel::cycleSortBy,
                        onRefresh = viewModel::scanDeviceAudio
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (activeTab) {
                            AppTab.LIBRARY -> LibraryScreen(
                                tracks = tracks,
                                playlists = playlists,
                                librarySubTab = librarySubTab,
                                selectedPlaylistId = selectedPlaylistId,
                                searchQuery = searchQuery,
                                sortBy = sortBy,
                                currentTrackId = currentTrackId,
                                isPlaying = isPlaying,
                                accentColor = animAccent,
                                loadArtwork = loadArtwork,
                                onSelectSubTab = viewModel::setLibrarySubTab,
                                onSelectPlaylist = viewModel::setSelectedPlaylistId,
                                onPlayTrack = viewModel::playTrack,
                                onMoreClick = viewModel::openContextMenu,
                                onCreatePlaylist = { viewModel.openCreatePlaylist(null) },
                                onPlaylistSettings = viewModel::openPlaylistSettings,
                                onRestoreTrack = viewModel::restoreTrack
                            )

                            AppTab.FOLDERS -> FoldersScreen(
                                tracks = tracks,
                                selectedFolder = selectedFolder,
                                searchQuery = searchQuery,
                                sortBy = sortBy,
                                currentTrackId = currentTrackId,
                                isPlaying = isPlaying,
                                accentColor = animAccent,
                                loadArtwork = loadArtwork,
                                onSelectFolder = viewModel::setSelectedFolder,
                                onPlayTrack = viewModel::playTrack,
                                onMoreClick = viewModel::openContextMenu
                            )

                            AppTab.SETTINGS -> SettingsScreen(
                                settings = settings,
                                excludedCount = tracks.count { it.exclusionReason != null },
                                totalCount = tracks.size,
                                accentColor = animAccent,
                                onUpdateSettings = viewModel::updateSettings,
                                onRescan = viewModel::scanDeviceAudio,
                                onClearData = viewModel::clearLibraryData
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (currentTrack != null) {
                            MiniPlayerBar(
                                track = currentTrack,
                                isPlaying = isPlaying,
                                positionMs = positionMs,
                                durationMs = durationMs,
                                accentColor = animAccent,
                                loadArtwork = loadArtwork,
                                onTogglePlay = viewModel::togglePlayPause,
                                onNext = viewModel::playNext,
                                onOpenNowPlaying = { viewModel.openNowPlaying(NowPlayingTab.ARTWORK) }
                            )
                        }
                        LiquidGlassNav(
                            activeTab = activeTab,
                            accentColor = animAccent,
                            onSelectTab = viewModel::setTab
                        )
                    }
                }

                AnimatedVisibility(
                    visible = toastMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                ) {
                    Surface(
                        color = Color(0xEE1E1E24),
                        shape = RoundedCornerShape(24.dp),
                        shadowElevation = 6.dp
                    ) {
                        Text(
                            text = toastMessage.orEmpty(),
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isNowPlayingOpen && currentTrack != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    if (currentTrack != null) {
                        NowPlayingSheet(
                            track = currentTrack,
                            allTracks = tracks,
                            queueIds = queueIds,
                            isPlaying = isPlaying,
                            positionMs = positionMs,
                            durationMs = durationMs,
                            shuffle = shuffle,
                            repeatMode = repeatMode,
                            activeTab = nowPlayingTab,
                            accentColor = animAccent,
                            bgTop = animTop,
                            bgMid = animMid,
                            bgBottom = animBottom,
                            lyrics = lyrics,
                            lyricsResults = lyricsResults,
                            isSearchingLyrics = isSearchingLyrics,
                            loadArtwork = loadArtwork,
                            onClose = viewModel::closeNowPlaying,
                            onSelectTab = viewModel::setNowPlayingTab,
                            onTogglePlay = viewModel::togglePlayPause,
                            onSeek = viewModel::seekTo,
                            onNext = viewModel::playNext,
                            onPrev = viewModel::playPrev,
                            onToggleShuffle = viewModel::toggleShuffle,
                            onCycleRepeat = viewModel::cycleRepeatMode,
                            onToggleFavorite = { viewModel.toggleFavorite(currentTrack.id) },
                            onSearchLyrics = { q -> viewModel.searchLyricsOnline(q) },
                            onApplyLyrics = { res -> viewModel.applyLyricsResult(currentTrack.id, res) },
                            onPlayTrack = viewModel::playTrack,
                            onClearQueue = viewModel::clearQueue
                        )
                    }
                }

                if (contextMenuTrack != null) {
                    TrackContextMenu(
                        track = contextMenuTrack!!,
                        playlists = playlists,
                        onDismiss = { viewModel.openContextMenu(null) },
                        onToggleFavorite = { viewModel.toggleFavorite(contextMenuTrack!!.id) },
                        onPlayNext = { viewModel.playNextInQueue(contextMenuTrack!!) },
                        onAddToQueue = { viewModel.addToQueue(contextMenuTrack!!) },
                        onTogglePlaylist = { pid -> viewModel.toggleTrackInPlaylist(pid, contextMenuTrack!!.id) },
                        onCreatePlaylist = {
                            val tid = contextMenuTrack!!.id
                            viewModel.openContextMenu(null)
                            viewModel.openCreatePlaylist(tid)
                        },
                        onExcludeOrRestore = {
                            if (contextMenuTrack!!.exclusionReason != null) viewModel.restoreTrack(contextMenuTrack!!)
                            else viewModel.excludeTrack(contextMenuTrack!!)
                        }
                    )
                }

                if (isCreatePlaylistOpen) {
                    CreatePlaylistDialog(
                        onDismiss = viewModel::closeCreatePlaylist,
                        onCreate = { name ->
                            val initial = if (editingPlaylistId != null) listOf(editingPlaylistId!!) else emptyList()
                            viewModel.createPlaylist(name, initial)
                        }
                    )
                }

                if (playlistSettingsId != null) {
                    val pl = playlists.firstOrNull { it.id == playlistSettingsId }
                    if (pl != null) {
                        PlaylistSettingsDialog(
                            playlist = pl,
                            onDismiss = { viewModel.openPlaylistSettings(null) },
                            onRename = { newName -> viewModel.renamePlaylist(pl.id, newName) },
                            onDelete = { viewModel.deletePlaylist(pl.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopHeader(
    activeTab: AppTab,
    isScanning: Boolean,
    searchQuery: String,
    sortBy: SortBy,
    onSearchChange: (String) -> Unit,
    onCycleSort: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = when (activeTab) {
                        AppTab.LIBRARY -> "سُكون"
                        AppTab.FOLDERS -> "المجلدات"
                        AppTab.SETTINGS -> "الإعدادات"
                    },
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (activeTab != AppTab.SETTINGS) {
                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable(onClick = onCycleSort)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.Sort, contentDescription = "ترتيب", tint = Color.White, modifier = Modifier.size(16.dp))
                            Text(sortBy.labelAr, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
                IconButton(onClick = onRefresh, enabled = !isScanning) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = "تحديث", tint = Color.White)
                    }
                }
            }
        }

        if (activeTab != AppTab.SETTINGS) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("ابحث عن أغنية أو فنان أو ألبوم...", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.6f)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "مسح", tint = Color.White.copy(alpha = 0.6f))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White.copy(alpha = 0.3f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedContainerColor = Color.White.copy(alpha = 0.08f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LibraryScreen(
    tracks: List<Track>,
    playlists: List<Playlist>,
    librarySubTab: LibrarySubTab,
    selectedPlaylistId: String?,
    searchQuery: String,
    sortBy: SortBy,
    currentTrackId: String?,
    isPlaying: Boolean,
    accentColor: Color,
    loadArtwork: suspend (Long, Long) -> Bitmap?,
    onSelectSubTab: (LibrarySubTab) -> Unit,
    onSelectPlaylist: (String?) -> Unit,
    onPlayTrack: (Track, List<Track>?) -> Unit,
    onMoreClick: (Track) -> Unit,
    onCreatePlaylist: () -> Unit,
    onPlaylistSettings: (String) -> Unit,
    onRestoreTrack: (Track) -> Unit
) {
    val activeTracks = remember(tracks) { tracks.filter { it.exclusionReason == null } }
    val excludedTracks = remember(tracks) { tracks.filter { it.exclusionReason != null } }

    val filteredTracks = remember(activeTracks, excludedTracks, playlists, librarySubTab, selectedPlaylistId, searchQuery, sortBy) {
        val base = when (librarySubTab) {
            LibrarySubTab.SONGS -> activeTracks
            LibrarySubTab.FAVORITES -> activeTracks.filter { it.isFavorite }
            LibrarySubTab.PLAYLISTS -> {
                val pl = playlists.firstOrNull { it.id == selectedPlaylistId }
                if (pl != null) activeTracks.filter { it.id in pl.trackIds } else emptyList()
            }
            LibrarySubTab.QUARANTINE -> excludedTracks
        }
        val q = searchQuery.trim().lowercase()
        val searched = if (q.isEmpty()) base else base.filter {
            it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q)
        }
        sortTracks(searched, sortBy)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(LibrarySubTab.entries) { tab ->
                val selected = tab == librarySubTab
                val label = when (tab) {
                    LibrarySubTab.SONGS -> "الأغاني (${activeTracks.size})"
                    LibrarySubTab.FAVORITES -> "المفضلة (${activeTracks.count { it.isFavorite }})"
                    LibrarySubTab.PLAYLISTS -> "القوائم (${playlists.size})"
                    LibrarySubTab.QUARANTINE -> "المستبعدة (${excludedTracks.size})"
                }
                FilterChip(
                    selected = selected,
                    onClick = { onSelectSubTab(tab) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor.copy(alpha = 0.35f),
                        selectedLabelColor = Color.White,
                        containerColor = Color.White.copy(alpha = 0.08f),
                        labelColor = Color.White.copy(alpha = 0.75f)
                    ),
                    border = null
                )
            }
        }

        if (librarySubTab == LibrarySubTab.PLAYLISTS && selectedPlaylistId == null) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Surface(
                        color = accentColor.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onCreatePlaylist)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White)
                            Text("إنشاء قائمة تشغيل جديدة", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                items(playlists, key = { it.id }) { pl ->
                    Surface(
                        color = Color.White.copy(alpha = 0.07f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPlaylist(pl.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(accentColor.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null, tint = Color.White)
                                }
                                Column {
                                    Text(pl.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    Text("${pl.trackIds.size} أغنية", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                }
                            }
                            IconButton(onClick = { onPlaylistSettings(pl.id) }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "إعدادات القائمة", tint = Color.White.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        } else {
            if (librarySubTab == LibrarySubTab.PLAYLISTS && selectedPlaylistId != null) {
                val pl = playlists.firstOrNull { it.id == selectedPlaylistId }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { onSelectPlaylist(null) }) {
                        Text("← كل القوائم", color = accentColor)
                    }
                    Text(pl?.name ?: "", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            if (filteredTracks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد أغاني مطابقة", color = Color.White.copy(alpha = 0.55f), fontSize = 15.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredTracks, key = { it.id }) { track ->
                        TrackRow(
                            track = track,
                            isCurrent = track.id == currentTrackId,
                            isPlaying = isPlaying,
                            accentColor = accentColor,
                            loadArtwork = loadArtwork,
                            onClick = { onPlayTrack(track, filteredTracks) },
                            onMoreClick = {
                                if (librarySubTab == LibrarySubTab.QUARANTINE) onRestoreTrack(track)
                                else onMoreClick(track)
                            },
                            isExcludedMode = librarySubTab == LibrarySubTab.QUARANTINE
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FoldersScreen(
    tracks: List<Track>,
    selectedFolder: String,
    searchQuery: String,
    sortBy: SortBy,
    currentTrackId: String?,
    isPlaying: Boolean,
    accentColor: Color,
    loadArtwork: suspend (Long, Long) -> Bitmap?,
    onSelectFolder: (String) -> Unit,
    onPlayTrack: (Track, List<Track>?) -> Unit,
    onMoreClick: (Track) -> Unit
) {
    val activeTracks = remember(tracks) { tracks.filter { it.exclusionReason == null } }
    val folders = remember(activeTracks) {
        listOf("الكل") + activeTracks.map { folderLabel(it.folderPath) }.distinct().sorted()
    }
    val filtered = remember(activeTracks, selectedFolder, searchQuery, sortBy) {
        val base = if (selectedFolder == "الكل") activeTracks else activeTracks.filter { folderLabel(it.folderPath) == selectedFolder }
        val q = searchQuery.trim().lowercase()
        val searched = if (q.isEmpty()) base else base.filter {
            it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q)
        }
        sortTracks(searched, sortBy)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(folders) { folder ->
                val selected = folder == selectedFolder
                FilterChip(
                    selected = selected,
                    onClick = { onSelectFolder(folder) },
                    leadingIcon = { Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(folder, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor.copy(alpha = 0.3f),
                        selectedLabelColor = Color.White,
                        containerColor = Color.White.copy(alpha = 0.06f),
                        labelColor = Color.White.copy(alpha = 0.7f)
                    ),
                    border = null
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered, key = { it.id }) { track ->
                TrackRow(
                    track = track,
                    isCurrent = track.id == currentTrackId,
                    isPlaying = isPlaying,
                    accentColor = accentColor,
                    loadArtwork = loadArtwork,
                    onClick = { onPlayTrack(track, filtered) },
                    onMoreClick = { onMoreClick(track) },
                    isExcludedMode = false
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    accentColor: Color,
    loadArtwork: suspend (Long, Long) -> Bitmap?,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    isExcludedMode: Boolean
) {
    val bg = if (isCurrent) accentColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
    Surface(
        color = bg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onMoreClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncArtworkTile(
                mediaStoreId = track.mediaStoreId,
                albumId = track.albumId,
                size = 50.dp,
                cornerRadius = 12.dp,
                loadArtwork = loadArtwork
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = if (isCurrent) accentColor else Color.White,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artist} • ${formatSeconds(track.durationSec)}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isCurrent && isPlaying) {
                Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = if (isExcludedMode) Icons.Rounded.Restore else Icons.Rounded.MoreVert,
                    contentDescription = "خيارات",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun AsyncArtworkTile(
    mediaStoreId: Long,
    albumId: Long,
    size: Dp,
    cornerRadius: Dp,
    loadArtwork: suspend (Long, Long) -> Bitmap?,
    modifier: Modifier = Modifier
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = mediaStoreId, key2 = albumId) {
        value = loadArtwork(mediaStoreId, albumId)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.size(size * 0.45f)
            )
        }
    }
}

@Composable
private fun MiniPlayerBar(
    track: Track,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    accentColor: Color,
    loadArtwork: suspend (Long, Long) -> Bitmap?,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onOpenNowPlaying: () -> Unit
) {
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    Surface(
        color = Color(0xCC18181E),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenNowPlaying)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AsyncArtworkTile(
                    mediaStoreId = track.mediaStoreId,
                    albumId = track.albumId,
                    size = 44.dp,
                    cornerRadius = 10.dp,
                    loadArtwork = loadArtwork
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "تشغيل/إيقاف",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "التالي", tint = Color.White)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(2.dp)
                        .background(accentColor)
                )
            }
        }
    }
}

@Composable
private fun LiquidGlassNav(
    activeTab: AppTab,
    accentColor: Color,
    onSelectTab: (AppTab) -> Unit
) {
    Surface(
        color = Color(0xCC141418),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                Triple(AppTab.LIBRARY, "المكتبة", Icons.Rounded.LibraryMusic),
                Triple(AppTab.FOLDERS, "المجلدات", Icons.Rounded.Folder),
                Triple(AppTab.SETTINGS, "الإعدادات", Icons.Rounded.Settings)
            )
            items.forEach { (tab, label, icon) ->
                val selected = tab == activeTab
                val tint = if (selected) accentColor else Color.White.copy(alpha = 0.6f)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectTab(tab) }
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
                    Text(label, color = tint, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
private fun NowPlayingSheet(
    track: Track,
    allTracks: List<Track>,
    queueIds: List<String>,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffle: Boolean,
    repeatMode: RepeatModeState,
    activeTab: NowPlayingTab,
    accentColor: Color,
    bgTop: Color,
    bgMid: Color,
    bgBottom: Color,
    lyrics: TrackLyrics?,
    lyricsResults: List<LrcSearchResult>,
    isSearchingLyrics: Boolean,
    loadArtwork: suspend (Long, Long) -> Bitmap?,
    onClose: () -> Unit,
    onSelectTab: (NowPlayingTab) -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSearchLyrics: (String) -> Unit,
    onApplyLyrics: (LrcSearchResult) -> Unit,
    onPlayTrack: (Track, List<Track>?) -> Unit,
    onClearQueue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgTop, bgMid, bgBottom)))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "إغلاق", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    NowPlayingTab.ARTWORK to "الغلاف",
                    NowPlayingTab.LYRICS to "الكلمات",
                    NowPlayingTab.QUEUE to "الطابور"
                ).forEach { (tab, label) ->
                    FilterChip(
                        selected = tab == activeTab,
                        onClick = { onSelectTab(tab) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor.copy(alpha = 0.35f),
                            selectedLabelColor = Color.White,
                            containerColor = Color.White.copy(alpha = 0.08f),
                            labelColor = Color.White.copy(alpha = 0.7f)
                        ),
                        border = null
                    )
                }
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "مفضلة",
                    tint = if (track.isFavorite) Color(0xFFFF4D6D) else Color.White
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when (activeTab) {
                NowPlayingTab.ARTWORK -> {
                    AsyncArtworkTile(
                        mediaStoreId = track.mediaStoreId,
                        albumId = track.albumId,
                        size = 300.dp,
                        cornerRadius = 28.dp,
                        loadArtwork = loadArtwork,
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .aspectRatio(1f)
                    )
                }

                NowPlayingTab.LYRICS -> {
                    LyricsPanel(
                        track = track,
                        positionMs = positionMs,
                        lyrics = lyrics,
                        results = lyricsResults,
                        isSearching = isSearchingLyrics,
                        accentColor = accentColor,
                        onSearch = onSearchLyrics,
                        onApplyResult = onApplyLyrics
                    )
                }

                NowPlayingTab.QUEUE -> {
                    val queueTracks = remember(queueIds, allTracks) {
                        val map = allTracks.associateBy { it.id }
                        queueIds.mapNotNull { map[it] }
                    }
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("قائمة الانتظار (${queueTracks.size})", color = Color.White, fontWeight = FontWeight.Bold)
                            TextButton(onClick = onClearQueue) { Text("مسح", color = accentColor) }
                        }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(queueTracks, key = { it.id }) { qt ->
                                TrackRow(
                                    track = qt,
                                    isCurrent = qt.id == track.id,
                                    isPlaying = isPlaying,
                                    accentColor = accentColor,
                                    loadArtwork = loadArtwork,
                                    onClick = { onPlayTrack(qt, queueTracks) },
                                    onMoreClick = {},
                                    isExcludedMode = false
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(track.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)

            val maxDur = durationMs.coerceAtLeast(1L).toFloat()
            Slider(
                value = positionMs.coerceIn(0L, durationMs.coerceAtLeast(1L)).toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..maxDur,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(positionMs), color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
                Text(formatTime(durationMs), color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = "عشوائي", tint = if (shuffle) accentColor else Color.White.copy(alpha = 0.6f))
                }
                IconButton(onClick = onPrev) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "السابق", tint = Color.White, modifier = Modifier.size(34.dp))
                }
                Surface(
                    color = accentColor,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp).clickable(onClick = onTogglePlay)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "تشغيل/إيقاف",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "التالي", tint = Color.White, modifier = Modifier.size(34.dp))
                }
                IconButton(onClick = onCycleRepeat) {
                    val icon = if (repeatMode == RepeatModeState.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
                    val tint = if (repeatMode != RepeatModeState.OFF) accentColor else Color.White.copy(alpha = 0.6f)
                    Icon(icon, contentDescription = "تكرار", tint = tint)
                }
            }
        }
    }
}

@Composable
private fun LyricsPanel(
    track: Track,
    positionMs: Long,
    lyrics: TrackLyrics?,
    results: List<LrcSearchResult>,
    isSearching: Boolean,
    accentColor: Color,
    onSearch: (String) -> Unit,
    onApplyResult: (LrcSearchResult) -> Unit
) {
    var query by remember(track.id) { mutableStateOf("${track.title} ${track.artist}") }
    val listState = rememberLazyListState()

    val activeIndex = remember(lyrics, positionMs) {
        if (lyrics == null || lyrics.syncedLines.isEmpty()) -1
        else lyrics.syncedLines.indexOfLast { it.timeMs <= positionMs }
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            listState.animateScrollToItem(activeIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("ابحث عن الكلمات...", fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                )
            )
            Button(
                onClick = { onSearch(query) },
                enabled = !isSearching,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text(if (isSearching) "..." else "بحث")
            }
        }

        if (lyrics != null && lyrics.syncedLines.isNotEmpty()) {
            LazyColumn(state = listState, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(lyrics.syncedLines) { idx, line ->
                    val isCurrent = idx == activeIndex
                    Text(
                        text = line.text,
                        color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.45f),
                        fontSize = if (isCurrent) 18.sp else 15.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        } else if (results.isNotEmpty()) {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results, key = { it.id }) { res ->
                    Surface(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onApplyResult(res) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(res.trackName, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(res.artistName, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("اضغط بحث لجلب كلمات الأغنية من LRCLIB", color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: AppSettings,
    excludedCount: Int,
    totalCount: Int,
    accentColor: Color,
    onUpdateSettings: (AppSettings) -> Unit,
    onRescan: () -> Unit,
    onClearData: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Surface(color = Color.White.copy(alpha = 0.06f), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("الفلترة الذكية", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("استبعاد المقاطع القصيرة والتسجيلات", color = Color.White, fontSize = 14.sp)
                            Text("إخفاء المقاطع أقل من ${settings.minDurationSeconds} ثانية ومجلدات المكالمات", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                        Switch(
                            checked = settings.autoExcludeRecordings,
                            onCheckedChange = { onUpdateSettings(settings.copy(autoExcludeRecordings = it)) }
                        )
                    }
                    Text("إجمالي الملفات: $totalCount • المستبعدة: $excludedCount", color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
                }
            }
        }

        item {
            Surface(color = Color.White.copy(alpha = 0.06f), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("المكتبة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Button(
                        onClick = onRescan,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إعادة فحص الجهاز الآن")
                    }
                    TextButton(onClick = onClearData, modifier = Modifier.fillMaxWidth()) {
                        Text("مسح القوائم والمفضلة المحفوظة", color = Color(0xFFFF6B6B))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackContextMenu(
    track: Track,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onTogglePlaylist: (String) -> Unit,
    onCreatePlaylist: () -> Unit,
    onExcludeOrRestore: () -> Unit
) {
    val isExcluded = track.exclusionReason != null
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A22),
        contentColor = Color.White
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(track.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))

            MenuActionRow(
                icon = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                label = if (track.isFavorite) "إزالة من المفضلة" else "إضافة إلى المفضلة",
                onClick = { onToggleFavorite(); onDismiss() }
            )
            MenuActionRow(icon = Icons.AutoMirrored.Rounded.PlaylistPlay, label = "تشغيل تالياً", onClick = { onPlayNext(); onDismiss() })
            MenuActionRow(icon = Icons.AutoMirrored.Rounded.QueueMusic, label = "إضافة إلى طابور الانتظار", onClick = { onAddToQueue(); onDismiss() })
            MenuActionRow(icon = Icons.AutoMirrored.Rounded.PlaylistAdd, label = "قائمة تشغيل جديدة بهذه الأغنية", onClick = onCreatePlaylist)
            MenuActionRow(
                icon = if (isExcluded) Icons.Rounded.Restore else Icons.Rounded.VisibilityOff,
                label = if (isExcluded) "استعادة الأغنية للمكتبة" else "إخفاء من المكتبة",
                onClick = { onExcludeOrRestore(); onDismiss() }
            )

            if (playlists.isNotEmpty()) {
                Text("إضافة إلى قائمة تشغيل:", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                playlists.forEach { pl ->
                    val inPl = track.id in pl.trackIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTogglePlaylist(pl.id) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(pl.name, color = Color.White)
                        Checkbox(checked = inPl, onCheckedChange = { onTogglePlaylist(pl.id) })
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MenuActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
        Text(label, color = Color.White, fontSize = 14.sp)
    }
}

@Composable
private fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A22),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = { Text("قائمة تشغيل جديدة") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("اسم القائمة") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name.trim()) }) {
                Text("إنشاء")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun PlaylistSettingsDialog(
    playlist: Playlist,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember(playlist.id) { mutableStateOf(playlist.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A22),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = { Text("إعدادات القائمة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("اسم القائمة") }
                )
                TextButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color(0xFFFF6B6B))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حذف القائمة", color = Color(0xFFFF6B6B))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onRename(name.trim()) }) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

private fun sortTracks(list: List<Track>, sortBy: SortBy): List<Track> = when (sortBy) {
    SortBy.RECENT -> list.sortedByDescending { it.addedAtMs }
    SortBy.TITLE -> list.sortedBy { it.title.lowercase() }
    SortBy.ARTIST -> list.sortedBy { it.artist.lowercase() }
    SortBy.DURATION -> list.sortedByDescending { it.durationSec }
}

private fun folderLabel(path: String): String {
    val clean = path.trimEnd('/', '\\')
    val idx = clean.lastIndexOfAny(charArrayOf('/', '\\'))
    return if (idx >= 0 && idx + 1 < clean.length) clean.substring(idx + 1) else clean.ifEmpty { "Music" }
}

private fun formatSeconds(sec: Double): String = formatTime((sec * 1000).toLong())

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return "%d:%02d".format(mins, secs)
}
