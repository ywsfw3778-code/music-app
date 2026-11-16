package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import com.example.model.AppStrings
import com.example.model.ExclusionReason
import com.example.model.LibrarySubTab
import com.example.model.Playlist
import com.example.model.SortOption
import com.example.model.Track
import com.example.ui.components.TrackArtwork
import com.example.ui.components.TrackRowItem
import com.example.ui.components.formatDuration
import com.example.ui.theme.AppFontFamily
import com.example.ui.theme.LocalWaterGlassState
import com.example.ui.theme.NocturneColors
import com.example.ui.theme.matteCard
import com.example.ui.theme.openTopGlassFade
import com.example.ui.theme.waterDropBar
import com.example.ui.theme.waterGlassContent

@Composable
fun LibraryScreen(
    strings: AppStrings,
    tracks: List<Track>,
    excludedTracks: List<Track>,
    playlists: List<Playlist>,
    favoriteIds: Set<String>,
    currentTrackId: String?,
    isPlaying: Boolean,
    accentColor: Color,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onScan: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onMoreClick: (Track) -> Unit,
    onRestoreTrack: (Track) -> Unit,
    onCreatePlaylist: (String, List<String>) -> Unit,
    onUpdatePlaylist: (Playlist) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onRemoveFromPlaylist: (String, String) -> Unit,
    bottomPadding: PaddingValues,
    isPageActive: Boolean = true
) {
    var subTab by remember { mutableStateOf(LibrarySubTab.ALL_SONGS) }
    val allSongsListState = rememberLazyListState()
    val favListState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(SortOption.RECENT) }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var editingPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var addingToPlaylist by remember { mutableStateOf<Playlist?>(null) }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchOpen) {
        if (isSearchOpen) {
            searchFocusRequester.requestFocus()
        }
    }

    BackHandler(enabled = isPageActive && selectedPlaylistId != null) {
        selectedPlaylistId = null
    }
    BackHandler(enabled = isPageActive && selectedPlaylistId == null && isSearchOpen) {
        searchQuery = ""
        isSearchOpen = false
    }
    BackHandler(enabled = isPageActive && selectedPlaylistId == null && !isSearchOpen && subTab != LibrarySubTab.ALL_SONGS) {
        subTab = LibrarySubTab.ALL_SONGS
    }

    val filteredTracks = remember(tracks, searchQuery, sortOption) {
        val q = searchQuery.trim()
        val base = if (q.isEmpty()) tracks else tracks.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.artist.contains(q, ignoreCase = true) ||
                it.album.contains(q, ignoreCase = true)
        }
        when (sortOption) {
            SortOption.TITLE -> base.sortedBy { it.title.lowercase() }
            SortOption.ARTIST -> base.sortedBy { it.artist.lowercase() }
            SortOption.DURATION -> base.sortedByDescending { it.durationSec }
            SortOption.RECENT -> base.sortedByDescending { it.dateAdded }
        }
    }

    val favTracks = remember(filteredTracks, favoriteIds) {
        filteredTracks.filter { favoriteIds.contains(it.id) }
    }

    val waterGlass = LocalWaterGlassState.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var headerHeightDp by remember { mutableStateOf(statusTop + 116.dp) }
    val density = LocalDensity.current
    val headerContentTop by animateDpAsState(
        targetValue = headerHeightDp + 10.dp,
        animationSpec = tween(200),
        label = "libHeaderTop"
    )
    val activeTabTextColor = if (accentColor.luminance() > 0.45f) Color(0xFF141110) else Color.White
    val listPadding = PaddingValues(
        top = headerContentTop,
        bottom = bottomPadding.calculateBottomPadding(),
        start = 16.dp,
        end = 16.dp
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: True full-screen (0,0) waterGlassContent container so blurred top bar has zero eaten/clipped edges
        Box(
            modifier = Modifier
                .fillMaxSize()
                .waterGlassContent(if (isPageActive) waterGlass else null)
        ) {
            if (!hasAudioPermission) {
                Box(Modifier.padding(top = headerContentTop, start = 16.dp, end = 16.dp)) {
                    EmptyStateCard(
                        title = strings.emptyTitle,
                        description = strings.emptyDescription,
                        note = strings.emptyFilterNote,
                        buttonText = strings.importFiles,
                        accentColor = accentColor,
                        onAction = onRequestPermission
                    )
                }
            } else {
                AnimatedContent(
                    targetState = subTab,
                    transitionSpec = {
                        val forward = targetState.ordinal >= initialState.ordinal
                        val dir = if (forward) 1 else -1
                        (slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { dir * it / 4 } + fadeIn(tween(220)))
                            .togetherWith(slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) { -dir * it / 4 } + fadeOut(tween(160)))
                    },
                    label = "librarySubTabTransition"
                ) { currentTab ->
                    when (currentTab) {
                    LibrarySubTab.ALL_SONGS -> {
                        if (tracks.isEmpty()) {
                            Box(Modifier.padding(top = headerContentTop, start = 16.dp, end = 16.dp)) {
                                EmptyStateCard(
                                    title = strings.emptyTitle,
                                    description = strings.emptyDescription,
                                    note = strings.emptyFilterNote,
                                    buttonText = strings.importFiles,
                                    accentColor = accentColor,
                                    onAction = onScan
                                )
                            }
                        } else if (filteredTracks.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(strings.noSearchResults, color = NocturneColors.TextSecondary, fontSize = 14.sp)
                            }
                        } else {
                            LazyColumn(
                                state = allSongsListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = listPadding,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(filteredTracks, key = { it.id }, contentType = { "track_row" }) { track ->
                                    TrackRowItem(
                                        track = track,
                                        isCurrent = track.id == currentTrackId,
                                        isPlaying = isPlaying,
                                        isFavorite = favoriteIds.contains(track.id),
                                        accentColor = accentColor,
                                        onClick = { onPlayTrack(track, filteredTracks) },
                                        onToggleFavorite = { onToggleFavorite(track.id) },
                                        onMoreClick = { onMoreClick(track) }
                                    )
                                }
                            }
                        }
                    }

                    LibrarySubTab.FAVORITES -> {
                        if (favTracks.isEmpty()) {
                            Box(Modifier.padding(top = headerContentTop, start = 16.dp, end = 16.dp)) {
                                SimpleInfoEmpty(
                                    icon = Icons.Rounded.FavoriteBorder,
                                    title = strings.noFavoritesTitle,
                                    desc = strings.noFavoritesDesc,
                                    accentColor = accentColor
                                )
                            }
                        } else {
                            LazyColumn(
                                state = favListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = listPadding,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(favTracks, key = { it.id }, contentType = { "track_row" }) { track ->
                                    TrackRowItem(
                                        track = track,
                                        isCurrent = track.id == currentTrackId,
                                        isPlaying = isPlaying,
                                        isFavorite = true,
                                        accentColor = accentColor,
                                        onClick = { onPlayTrack(track, favTracks) },
                                        onToggleFavorite = { onToggleFavorite(track.id) },
                                        onMoreClick = { onMoreClick(track) }
                                    )
                                }
                            }
                        }
                    }

                    LibrarySubTab.PLAYLISTS -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = headerContentTop, start = 16.dp, end = 16.dp)
                        ) {
                            val activePlaylist = playlists.find { it.id == selectedPlaylistId }
                            if (activePlaylist != null) {
                                val plTracks = activePlaylist.trackIds.mapNotNull { id -> tracks.find { it.id == id } }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.clickable { selectedPlaylistId = null },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = accentColor, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(strings.backToPlaylists, color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (plTracks.isNotEmpty()) {
                                            TextButton(onClick = { onPlayTrack(plTracks.first(), plTracks) }) {
                                                Text(strings.playAll, color = accentColor, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        IconButton(onClick = { addingToPlaylist = activePlaylist }, modifier = Modifier.size(34.dp)) {
                                            Icon(Icons.Rounded.Add, null, tint = accentColor, modifier = Modifier.size(21.dp))
                                        }
                                        IconButton(onClick = { editingPlaylist = activePlaylist }, modifier = Modifier.size(34.dp)) {
                                            Icon(Icons.Rounded.Settings, null, tint = NocturneColors.TextSecondary, modifier = Modifier.size(19.dp))
                                        }
                                    }
                                }
                                Text(
                                    text = "${activePlaylist.name} · ${plTracks.size} ${strings.songsCount}",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                                if (plTracks.isEmpty()) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(strings.emptyPlaylistDesc, color = NocturneColors.TextSecondary, textAlign = TextAlign.Center)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = bottomPadding,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(plTracks, key = { it.id }, contentType = { "track_row" }) { track ->
                                            TrackRowItem(
                                                track = track,
                                                isCurrent = track.id == currentTrackId,
                                                isPlaying = isPlaying,
                                                isFavorite = favoriteIds.contains(track.id),
                                                accentColor = accentColor,
                                                onClick = { onPlayTrack(track, plTracks) },
                                                onToggleFavorite = { onToggleFavorite(track.id) },
                                                onMoreClick = { onMoreClick(track) }
                                            )
                                        }
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { showCreatePlaylistDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.createPlaylist, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(10.dp))
                                if (playlists.isEmpty()) {
                                    SimpleInfoEmpty(
                                        icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                        title = strings.noPlaylistsTitle,
                                        desc = strings.noPlaylistsDesc,
                                        accentColor = accentColor
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = bottomPadding,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(playlists, key = { it.id }, contentType = { "playlist_card" }) { pl ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .matteCard(RoundedCornerShape(16.dp))
                                                    .clickable { selectedPlaylistId = pl.id }
                                                    .padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(accentColor.copy(alpha = 0.16f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = accentColor)
                                                }
                                                Spacer(Modifier.width(12.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text(pl.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                    Text("${pl.trackIds.size} ${strings.songsCount}", color = NocturneColors.TextSecondary, fontSize = 12.sp)
                                                }
                                                IconButton(onClick = { editingPlaylist = pl }) {
                                                    Icon(Icons.Rounded.Settings, null, tint = NocturneColors.TextSecondary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    LibrarySubTab.QUARANTINE -> {
                        // Quarantine moved to SettingsScreen
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(strings.noQuarantineDesc, color = NocturneColors.TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

        // Layer 2: Open Glass Fade Header (Option 1 — seamless fade + expandable search button + 3 symmetric tabs)
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeightDp + 40.dp)
                    .openTopGlassFade()
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        val hDp = with(density) { coords.size.height.toDp() }
                        if (headerHeightDp != hDp) {
                            headerHeightDp = hDp
                        }
                    }
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.appName,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (excludedTracks.isNotEmpty()) {
                                "${tracks.size} ${strings.songsCount} · ${excludedTracks.size} ${strings.filteredCount}"
                            } else {
                                "${tracks.size} ${strings.songsCount}"
                            },
                            color = NocturneColors.TextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    val sortLabel = when (sortOption) {
                        SortOption.TITLE -> strings.sortTitle
                        SortOption.ARTIST -> strings.sortArtist
                        SortOption.DURATION -> strings.sortDuration
                        SortOption.RECENT -> strings.sortRecent
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSearchOpen || searchQuery.isNotEmpty()) accentColor.copy(alpha = 0.22f)
                                    else Color.White.copy(alpha = 0.09f)
                                )
                                .border(
                                    1.dp,
                                    if (isSearchOpen || searchQuery.isNotEmpty()) accentColor.copy(alpha = 0.45f)
                                    else Color.White.copy(alpha = 0.14f),
                                    CircleShape
                                )
                                .clickable {
                                    if (isSearchOpen) {
                                        searchQuery = ""
                                        isSearchOpen = false
                                    } else {
                                        isSearchOpen = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSearchOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                                contentDescription = null,
                                tint = if (isSearchOpen || searchQuery.isNotEmpty()) accentColor else Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.09f))
                                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
                                .clickable {
                                    val order = listOf(SortOption.RECENT, SortOption.TITLE, SortOption.ARTIST, SortOption.DURATION)
                                    sortOption = order[(order.indexOf(sortOption) + 1) % order.size]
                                }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SwapVert,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = sortLabel,
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isSearchOpen,
                    enter = expandVertically(tween(200)) + fadeIn(tween(180)),
                    exit = shrinkVertically(tween(180)) + fadeOut(tween(140))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .height(40.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.10f))
                            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                            .padding(horizontal = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Search, null, tint = accentColor, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp, fontFamily = AppFontFamily),
                            cursorBrush = SolidColor(accentColor),
                            decorationBox = { inner ->
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                    if (searchQuery.isEmpty()) {
                                        Text(strings.searchPlaceholder, color = NocturneColors.TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    inner()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(searchFocusRequester)
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Rounded.Close, null, tint = NocturneColors.TextMuted, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf(
                        LibrarySubTab.ALL_SONGS to "${strings.tabAllSongs} · ${tracks.size}",
                        LibrarySubTab.PLAYLISTS to "${strings.tabPlaylists} · ${playlists.size}",
                        LibrarySubTab.FAVORITES to "${strings.tabFavorites} · ${favTracks.size}"
                    )
                    tabs.forEach { (tab, label) ->
                        val selected = subTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) accentColor else Color.White.copy(alpha = 0.08f))
                                .border(
                                    width = 1.dp,
                                    color = if (selected) Color.Transparent else Color.White.copy(alpha = 0.10f),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable {
                                    subTab = tab
                                    if (tab != LibrarySubTab.PLAYLISTS) selectedPlaylistId = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selected) activeTabTextColor else Color(0xFFE2E8F0).copy(alpha = 0.82f),
                                fontSize = 11.5.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showCreatePlaylistDialog,
            enter = slideInVertically { it / 4 } + fadeIn(),
            exit = slideOutVertically { it / 4 } + fadeOut()
        ) {
            PlaylistSongPickerOverlay(
                title = strings.createPlaylist,
                actionLabel = strings.createAction,
                showNameField = true,
                initialName = "",
                namePlaceholder = strings.playlistNamePlaceholder,
                searchPlaceholder = strings.searchPlaceholder,
                tracks = tracks,
                initialSelectedIds = emptySet(),
                accentColor = accentColor,
                bottomPadding = bottomPadding,
                onDismiss = { showCreatePlaylistDialog = false },
                onConfirm = { name, ids ->
                    if (name.isNotBlank()) {
                        onCreatePlaylist(name, ids)
                        showCreatePlaylistDialog = false
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = addingToPlaylist != null,
            enter = slideInVertically { it / 4 } + fadeIn(),
            exit = slideOutVertically { it / 4 } + fadeOut()
        ) {
            addingToPlaylist?.let { pl ->
                PlaylistSongPickerOverlay(
                    title = pl.name,
                    actionLabel = strings.saveChanges,
                    showNameField = false,
                    initialName = pl.name,
                    namePlaceholder = strings.playlistNamePlaceholder,
                    searchPlaceholder = strings.searchPlaceholder,
                    tracks = tracks,
                    initialSelectedIds = pl.trackIds.toSet(),
                    accentColor = accentColor,
                    bottomPadding = bottomPadding,
                    onDismiss = { addingToPlaylist = null },
                    onConfirm = { _, ids ->
                        val idSet = ids.toSet()
                        val merged = pl.trackIds.filter { idSet.contains(it) } +
                            ids.filterNot { pl.trackIds.contains(it) }
                        onUpdatePlaylist(pl.copy(trackIds = merged))
                        addingToPlaylist = null
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = editingPlaylist != null,
            enter = slideInVertically { it / 4 } + fadeIn(),
            exit = slideOutVertically { it / 4 } + fadeOut()
        ) {
            editingPlaylist?.let { pl ->
                PlaylistEditorOverlay(
                    playlist = pl,
                    tracks = tracks,
                    strings = strings,
                    accentColor = accentColor,
                    bottomPadding = bottomPadding,
                    onDismiss = { editingPlaylist = null },
                    onSave = { updated ->
                        onUpdatePlaylist(updated)
                        editingPlaylist = null
                    },
                    onDelete = {
                        onDeletePlaylist(pl.id)
                        if (selectedPlaylistId == pl.id) selectedPlaylistId = null
                        editingPlaylist = null
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    description: String,
    note: String,
    buttonText: String,
    accentColor: Color,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .matteCard(RoundedCornerShape(22.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.LibraryMusic, null, tint = accentColor, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(description, color = NocturneColors.TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(note, color = NocturneColors.TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(buttonText, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SimpleInfoEmpty(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .matteCard(RoundedCornerShape(20.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = accentColor.copy(alpha = 0.7f), modifier = Modifier.size(38.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(desc, color = NocturneColors.TextSecondary, fontSize = 12.5.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PlaylistSongPickerOverlay(
    title: String,
    actionLabel: String,
    showNameField: Boolean,
    initialName: String,
    namePlaceholder: String,
    searchPlaceholder: String,
    tracks: List<Track>,
    initialSelectedIds: Set<String>,
    accentColor: Color,
    bottomPadding: PaddingValues,
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>) -> Unit
) {
    BackHandler(onBack = onDismiss)
    var name by remember(initialName) { mutableStateOf(initialName) }
    var filter by remember { mutableStateOf("") }
    var selectedIds by remember(initialSelectedIds) { mutableStateOf(initialSelectedIds) }

    val filtered = remember(tracks, filter) {
        val q = filter.trim()
        if (q.isEmpty()) tracks else tracks.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.artist.contains(q, ignoreCase = true) ||
                it.album.contains(q, ignoreCase = true)
        }
    }
    val glass = LocalWaterGlassState.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                if (glass != null) {
                    drawLayer(glass.blurredBgLayer)
                }
                drawRect(Color.Black.copy(alpha = 0.48f))
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(12.dp))
            // Header bar with Close, Title, and Confirm Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .waterDropBar(CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = if (selectedIds.isEmpty()) title else "$title (${selectedIds.size})",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp).weight(1f),
                    textAlign = TextAlign.Center
                )
                val canSave = !showNameField || name.isNotBlank()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (canSave) accentColor else Color.White.copy(alpha = 0.12f))
                        .clickable(enabled = canSave) { onConfirm(name, selectedIds.toList()) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = actionLabel,
                        color = if (canSave) Color.Black else NocturneColors.TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showNameField) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .waterDropBar(RoundedCornerShape(50))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppFontFamily),
                        cursorBrush = SolidColor(accentColor),
                        decorationBox = { inner ->
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                if (name.isEmpty()) {
                                    Text(namePlaceholder, color = NocturneColors.TextSecondary, fontSize = 13.sp)
                                }
                                inner()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Search bar matching LibraryScreen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .waterDropBar(RoundedCornerShape(50))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Search, null, tint = NocturneColors.TextMuted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 13.5.sp, fontFamily = AppFontFamily),
                    cursorBrush = SolidColor(accentColor),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            if (filter.isEmpty()) {
                                Text(searchPlaceholder, color = NocturneColors.TextSecondary, fontSize = 12.5.sp)
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                if (filter.isNotEmpty()) {
                    IconButton(onClick = { filter = "" }, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Rounded.Close, null, tint = NocturneColors.TextMuted, modifier = Modifier.size(15.dp))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Songs list in exact Library style with Album Art + Title + Artist + (+) button
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = bottomPadding.calculateBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filtered, key = { it.id }) { t ->
                    val picked = selectedIds.contains(t.id)
                    val toggle = {
                        selectedIds = if (picked) selectedIds - t.id else selectedIds + t.id
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (picked) accentColor.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable(onClick = toggle)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TrackArtwork(
                            artUri = t.artUri,
                            size = 48.dp,
                            cornerRadius = 11.dp,
                            accentColor = accentColor
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = t.title,
                                color = if (picked) accentColor else NocturneColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                style = TextStyle(textDirection = TextDirection.Content),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${t.artist} · ${formatDuration(t.durationSec)}",
                                color = NocturneColors.TextSecondary,
                                fontSize = 12.sp,
                                style = TextStyle(textDirection = TextDirection.Content),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (picked) accentColor else Color.White.copy(alpha = 0.10f))
                                .clickable(onClick = toggle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (picked) Icons.Rounded.Check else Icons.Rounded.Add,
                                contentDescription = null,
                                tint = if (picked) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistEditorOverlay(
    playlist: Playlist,
    tracks: List<Track>,
    strings: AppStrings,
    accentColor: Color,
    bottomPadding: PaddingValues,
    onDismiss: () -> Unit,
    onSave: (Playlist) -> Unit,
    onDelete: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    var editName by remember(playlist.id) { mutableStateOf(playlist.name) }
    var editIds by remember(playlist.id) { mutableStateOf(playlist.trackIds) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    val glass = LocalWaterGlassState.current
    val itemStepPx = with(LocalDensity.current) { 68.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                if (glass != null) {
                    drawLayer(glass.blurredBgLayer)
                }
                drawRect(Color.Black.copy(alpha = 0.50f))
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(12.dp))

            // Top Header: Close + Title + Save Changes Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .waterDropBar(CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = strings.playlistSettingsTitle,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp).weight(1f),
                    textAlign = TextAlign.Center
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accentColor)
                        .clickable {
                            onSave(playlist.copy(name = editName.ifBlank { playlist.name }, trackIds = editIds))
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.saveChanges,
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Playlist Name Field in Water-Drop Pill Style
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .waterDropBar(RoundedCornerShape(50))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFontFamily
                    ),
                    cursorBrush = SolidColor(accentColor),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            if (editName.isEmpty()) {
                                Text(strings.playlistNamePlaceholder, color = NocturneColors.TextSecondary, fontSize = 13.sp)
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(10.dp))

            // Quick Actions Row: Reverse Order | Sort Alphabetically | Delete Playlist
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .waterDropBar(RoundedCornerShape(50))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
                        .clickable { editIds = editIds.reversed() },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.SwapVert, null, tint = accentColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(strings.reversePlaylistOrder, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .waterDropBar(RoundedCornerShape(50))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
                        .clickable {
                            editIds = editIds.sortedBy { id -> tracks.find { it.id == id }?.title?.lowercase() ?: "" }
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.SortByAlpha, null, tint = accentColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(strings.sortPlaylistAlpha, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(NocturneColors.Danger.copy(alpha = 0.16f))
                        .border(1.dp, NocturneColors.Danger.copy(alpha = 0.35f), RoundedCornerShape(50))
                        .clickable(onClick = onDelete)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.DeleteOutline, null, tint = NocturneColors.Danger, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.deletePlaylist, color = NocturneColors.Danger, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Drag & Drop Reorderable Songs List with Album Artwork
            if (editIds.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.emptyPlaylistDesc,
                        color = NocturneColors.TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = bottomPadding.calculateBottomPadding()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(editIds, key = { _, id -> id }) { _, id ->
                        val t = tracks.find { it.id == id }
                        val isDragging = draggingId == id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isDragging) 10f else 0f)
                                .offset { IntOffset(0, if (isDragging) dragOffsetY.roundToInt() else 0) }
                                .graphicsLayer {
                                    scaleX = if (isDragging) 1.03f else 1f
                                    scaleY = if (isDragging) 1.03f else 1f
                                }
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isDragging) accentColor.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isDragging) accentColor.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.07f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Drag Handle (touch and drag up/down to reorder)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isDragging) accentColor.copy(alpha = 0.25f) else Color.Transparent)
                                    .pointerInput(id) {
                                        detectVerticalDragGestures(
                                            onDragStart = {
                                                draggingId = id
                                                dragOffsetY = 0f
                                            },
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetY += dragAmount
                                                val curIdx = editIds.indexOf(id)
                                                if (curIdx != -1) {
                                                    val steps = (dragOffsetY / itemStepPx).toInt()
                                                    if (steps != 0) {
                                                        val targetIdx = (curIdx + steps).coerceIn(0, editIds.lastIndex)
                                                        if (targetIdx != curIdx) {
                                                            val mutable = editIds.toMutableList()
                                                            val item = mutable.removeAt(curIdx)
                                                            mutable.add(targetIdx, item)
                                                            editIds = mutable
                                                            dragOffsetY -= (targetIdx - curIdx) * itemStepPx
                                                        }
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                draggingId = null
                                                dragOffsetY = 0f
                                            },
                                            onDragCancel = {
                                                draggingId = null
                                                dragOffsetY = 0f
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DragHandle,
                                    contentDescription = null,
                                    tint = if (isDragging) accentColor else NocturneColors.TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            TrackArtwork(
                                artUri = t?.artUri,
                                size = 46.dp,
                                cornerRadius = 11.dp,
                                accentColor = accentColor
                            )

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = t?.title ?: id,
                                    color = if (isDragging) accentColor else NocturneColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    style = TextStyle(textDirection = TextDirection.Content),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (t != null) "${t.artist} · ${formatDuration(t.durationSec)}" else "",
                                    color = NocturneColors.TextSecondary,
                                    fontSize = 12.sp,
                                    style = TextStyle(textDirection = TextDirection.Content),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(NocturneColors.Danger.copy(alpha = 0.14f))
                                    .clickable { editIds = editIds.filterNot { it == id } },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    tint = NocturneColors.Danger,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
