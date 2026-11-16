package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppStrings
import com.example.model.SortOption
import com.example.model.Track
import com.example.ui.components.TrackRowItem
import com.example.ui.theme.AppFontFamily
import com.example.ui.theme.LocalWaterGlassState
import com.example.ui.theme.NocturneColors
import com.example.ui.theme.matteCard
import com.example.ui.theme.openTopGlassFade
import com.example.ui.theme.waterGlassContent

@Composable
fun FoldersScreen(
    strings: AppStrings,
    tracks: List<Track>,
    excludedCount: Int,
    favoriteIds: Set<String>,
    currentTrackId: String?,
    isPlaying: Boolean,
    accentColor: Color,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onMoreClick: (Track) -> Unit,
    bottomPadding: PaddingValues,
    isPageActive: Boolean = true
) {
    val folders = remember(tracks) {
        tracks.groupBy { it.folderPath }
            .map { (path, list) ->
                val rel = path.removePrefix("/storage/emulated/0/").ifBlank { list.firstOrNull()?.folderName ?: "Music" }
                Triple(path, rel, list)
            }
            .sortedByDescending { it.third.size }
    }

    var selectedFolderPath by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(SortOption.RECENT) }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchOpen) {
        if (isSearchOpen) {
            searchFocusRequester.requestFocus()
        }
    }

    BackHandler(enabled = isPageActive && isSearchOpen) {
        searchQuery = ""
        isSearchOpen = false
    }
    BackHandler(enabled = isPageActive && !isSearchOpen && selectedFolderPath != null) {
        selectedFolderPath = null
    }

    val displayedTracks = remember(tracks, selectedFolderPath, searchQuery, sortOption) {
        val byFolder = if (selectedFolderPath == null) tracks else tracks.filter { it.folderPath == selectedFolderPath }
        val q = searchQuery.trim()
        val filtered = if (q.isEmpty()) byFolder else byFolder.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.artist.contains(q, ignoreCase = true) ||
                it.album.contains(q, ignoreCase = true)
        }
        when (sortOption) {
            SortOption.TITLE -> filtered.sortedBy { it.title.lowercase() }
            SortOption.ARTIST -> filtered.sortedBy { it.artist.lowercase() }
            SortOption.DURATION -> filtered.sortedByDescending { it.durationSec }
            SortOption.RECENT -> filtered.sortedByDescending { it.dateAdded }
        }
    }

    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var headerHeightDp by remember { mutableStateOf(statusTop + (if (tracks.isNotEmpty()) 116.dp else 78.dp)) }
    val density = LocalDensity.current
    val headerContentTop by animateDpAsState(
        targetValue = headerHeightDp + 10.dp,
        animationSpec = tween(200),
        label = "foldersHeaderTop"
    )
    val activeTabTextColor = if (accentColor.luminance() > 0.45f) Color(0xFF141110) else Color.White

    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: True full-screen (0,0) waterGlassContent container so blurred top bar has zero eaten/clipped edges
        Box(
            modifier = Modifier
                .fillMaxSize()
                .waterGlassContent(if (isPageActive) LocalWaterGlassState.current else null)
        ) {
            if (tracks.isEmpty()) {
                Column(
                    modifier = Modifier
                        .padding(top = headerContentTop, start = 16.dp, end = 16.dp)
                        .fillMaxWidth()
                        .matteCard(RoundedCornerShape(20.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Rounded.Folder, null, tint = accentColor.copy(alpha = 0.7f), modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(strings.foldersEmptyTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(strings.foldersEmptyDesc, color = NocturneColors.TextSecondary, fontSize = 12.5.sp, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = headerContentTop,
                        bottom = bottomPadding.calculateBottomPadding(),
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(displayedTracks, key = { it.id }) { track ->
                        TrackRowItem(
                            track = track,
                            isCurrent = track.id == currentTrackId,
                            isPlaying = isPlaying,
                            isFavorite = favoriteIds.contains(track.id),
                            accentColor = accentColor,
                            onClick = { onPlayTrack(track, displayedTracks) },
                            onToggleFavorite = { onToggleFavorite(track.id) },
                            onMoreClick = { onMoreClick(track) }
                        )
                    }
                }
            }
        }

        // Layer 2: Open Glass Fade Header (Option 1 — seamless fade + expandable search button + floating pills)
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
                            text = strings.navFolders,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (excludedCount > 0) "${tracks.size} ${strings.songsCount} · $excludedCount ${strings.filteredCount}" else "${tracks.size} ${strings.songsCount}",
                            color = NocturneColors.TextSecondary,
                            fontSize = 11.5.sp,
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
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(
                                    if (isSearchOpen || searchQuery.isNotEmpty()) accentColor.copy(alpha = 0.22f)
                                    else Color.White.copy(alpha = 0.09f)
                                )
                                .border(
                                    1.dp,
                                    if (isSearchOpen || searchQuery.isNotEmpty()) accentColor.copy(alpha = 0.45f)
                                    else Color.White.copy(alpha = 0.14f),
                                    androidx.compose.foundation.shape.CircleShape
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
                            Icon(Icons.Rounded.SwapVert, null, tint = accentColor, modifier = Modifier.size(15.dp))
                            Text(sortLabel, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
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
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp, fontFamily = AppFontFamily),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(accentColor),
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

                if (tracks.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val allSelected = selectedFolderPath == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (allSelected) accentColor else Color.White.copy(alpha = 0.08f))
                                .border(
                                    width = 1.dp,
                                    color = if (allSelected) Color.Transparent else Color.White.copy(alpha = 0.10f),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable { selectedFolderPath = null }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "${strings.allFolders} · ${tracks.size}",
                                color = if (allSelected) activeTabTextColor else Color(0xFFE2E8F0).copy(alpha = 0.80f),
                                fontSize = 11.sp,
                                fontWeight = if (allSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }

                        folders.forEach { (path, rel, list) ->
                            val selected = selectedFolderPath == path
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (selected) accentColor else Color.White.copy(alpha = 0.08f))
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) Color.Transparent else Color.White.copy(alpha = 0.10f),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .clickable { selectedFolderPath = path }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "$rel · ${list.size}",
                                    color = if (selected) activeTabTextColor else Color(0xFFE2E8F0).copy(alpha = 0.80f),
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
