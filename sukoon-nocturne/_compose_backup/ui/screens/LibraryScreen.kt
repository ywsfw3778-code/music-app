package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppStrings
import com.example.model.ExclusionReason
import com.example.model.LibrarySubTab
import com.example.model.Playlist
import com.example.model.SortOption
import com.example.model.Track
import com.example.ui.components.TrackRowItem
import com.example.ui.components.formatDuration
import com.example.ui.theme.NocturneColors
import com.example.ui.theme.matteCard

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
    bottomPadding: PaddingValues
) {
    var subTab by remember { mutableStateOf(LibrarySubTab.ALL_SONGS) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(SortOption.RECENT) }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var editingPlaylist by remember { mutableStateOf<Playlist?>(null) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        // Exact LibraryView Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = strings.appName,
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${tracks.size} ${strings.songsCount}",
                    color = NocturneColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                if (excludedTracks.isNotEmpty()) {
                    Text(
                        text = " · ${excludedTracks.size} ${strings.filteredCount}",
                        color = NocturneColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Search Bar + Single Cycle Sort Pill (side-by-side)
        val sortLabel = when (sortOption) {
            SortOption.TITLE -> strings.sortTitle
            SortOption.ARTIST -> strings.sortArtist
            SortOption.DURATION -> strings.sortDuration
            SortOption.RECENT -> strings.sortRecent
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(NocturneColors.WaterDropBarBg)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Search, null, tint = NocturneColors.TextMuted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.5.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(accentColor),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(strings.searchPlaceholder, color = NocturneColors.TextSecondary, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Rounded.Close, null, tint = NocturneColors.TextMuted, modifier = Modifier.size(15.dp))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(NocturneColors.WaterDropBarBg)
                    .clickable {
                        val order = listOf(SortOption.RECENT, SortOption.TITLE, SortOption.ARTIST, SortOption.DURATION)
                        sortOption = order[(order.indexOf(sortOption) + 1) % order.size]
                    }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = sortLabel,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Sub-tabs inside ONE water-drop-bar pill container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(NocturneColors.WaterDropBarBg)
                .horizontalScroll(rememberScrollState())
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                LibrarySubTab.ALL_SONGS to "${strings.tabAllSongs} · ${tracks.size}",
                LibrarySubTab.PLAYLISTS to "${strings.tabPlaylists} · ${playlists.size}",
                LibrarySubTab.FAVORITES to "${strings.tabFavorites} · ${favTracks.size}",
                LibrarySubTab.QUARANTINE to "${strings.tabQuarantine} · ${excludedTracks.size}"
            )
            tabs.forEach { (tab, label) ->
                val selected = subTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) NocturneColors.WaterDropPillBg else Color.Transparent)
                        .clickable {
                            subTab = tab
                            if (tab != LibrarySubTab.PLAYLISTS) selectedPlaylistId = null
                        }
                        .padding(horizontal = 13.dp, vertical = 8.dp)
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

        Spacer(Modifier.height(12.dp))

        if (!hasAudioPermission) {
            EmptyStateCard(
                title = strings.emptyTitle,
                description = strings.emptyDescription,
                note = strings.emptyFilterNote,
                buttonText = strings.importFiles,
                accentColor = accentColor,
                onAction = onRequestPermission
            )
        } else {
            when (subTab) {
                LibrarySubTab.ALL_SONGS -> {
                    if (tracks.isEmpty()) {
                        EmptyStateCard(
                            title = strings.emptyTitle,
                            description = strings.emptyDescription,
                            note = strings.emptyFilterNote,
                            buttonText = strings.importFiles,
                            accentColor = accentColor,
                            onAction = onScan
                        )
                    } else if (filteredTracks.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(strings.noSearchResults, color = NocturneColors.TextSecondary, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = bottomPadding,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredTracks, key = { it.id }) { track ->
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
                        SimpleInfoEmpty(
                            icon = Icons.Rounded.FavoriteBorder,
                            title = strings.noFavoritesTitle,
                            desc = strings.noFavoritesDesc,
                            accentColor = accentColor
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = bottomPadding,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(favTracks, key = { it.id }) { track ->
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (plTracks.isNotEmpty()) {
                                    TextButton(onClick = { onPlayTrack(plTracks.first(), plTracks) }) {
                                        Text(strings.playAll, color = accentColor, fontWeight = FontWeight.Bold)
                                    }
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
                                items(plTracks, key = { it.id }) { track ->
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
                                icon = Icons.Rounded.QueueMusic,
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
                                items(playlists, key = { it.id }) { pl ->
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
                                            Icon(Icons.Rounded.QueueMusic, null, tint = accentColor)
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

                LibrarySubTab.QUARANTINE -> {
                    if (excludedTracks.isEmpty()) {
                        SimpleInfoEmpty(
                            icon = Icons.Rounded.LibraryMusic,
                            title = strings.noQuarantineTitle,
                            desc = strings.noQuarantineDesc,
                            accentColor = accentColor
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = bottomPadding,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(excludedTracks, key = { it.id }) { track ->
                                val reasonLabel = when (track.exclusionReason) {
                                    ExclusionReason.RECORDING -> strings.reasonRecording
                                    ExclusionReason.SHORT_CLIP -> strings.reasonShortClip
                                    ExclusionReason.KEYWORD -> strings.reasonKeyword
                                    else -> strings.tabQuarantine
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .matteCard(RoundedCornerShape(16.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(track.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("$reasonLabel · ${formatDuration(track.durationSec)}", color = NocturneColors.TextMuted, fontSize = 12.sp)
                                    }
                                    TextButton(onClick = { onRestoreTrack(track) }) {
                                        Icon(Icons.Rounded.Restore, null, tint = accentColor, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(strings.restoreTrack, color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            containerColor = NocturneColors.MatteElevatedBg,
            title = { Text(strings.createPlaylist, color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(strings.playlistNamePlaceholder, color = NocturneColors.TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreatePlaylist(name, emptyList())
                            showCreatePlaylistDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black)
                ) {
                    Text(strings.createAction, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text(strings.cancel, color = NocturneColors.TextSecondary)
                }
            }
        )
    }

    editingPlaylist?.let { pl ->
        var editName by remember(pl.id) { mutableStateOf(pl.name) }
        var editIds by remember(pl.id) { mutableStateOf(pl.trackIds) }
        AlertDialog(
            onDismissRequest = { editingPlaylist = null },
            containerColor = NocturneColors.MatteElevatedBg,
            title = { Text(strings.playlistSettingsTitle, color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { editIds = editIds.reversed() }) {
                            Text(strings.reversePlaylistOrder, color = accentColor, fontSize = 12.sp)
                        }
                        TextButton(onClick = {
                            editIds = editIds.sortedBy { id -> tracks.find { it.id == id }?.title?.lowercase() ?: "" }
                        }) {
                            Text(strings.sortPlaylistAlpha, color = accentColor, fontSize = 12.sp)
                        }
                    }
                    if (editIds.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.height(160.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(editIds.size) { idx ->
                                val id = editIds[idx]
                                val t = tracks.find { it.id == id }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text(t?.title ?: id, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = {
                                            if (idx > 0) {
                                                val m = editIds.toMutableList()
                                                val tmp = m[idx - 1]; m[idx - 1] = m[idx]; m[idx] = tmp
                                                editIds = m
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) { Icon(Icons.Rounded.ArrowUpward, null, tint = NocturneColors.TextSecondary, modifier = Modifier.size(16.dp)) }
                                    IconButton(
                                        onClick = {
                                            if (idx < editIds.lastIndex) {
                                                val m = editIds.toMutableList()
                                                val tmp = m[idx + 1]; m[idx + 1] = m[idx]; m[idx] = tmp
                                                editIds = m
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) { Icon(Icons.Rounded.ArrowDownward, null, tint = NocturneColors.TextSecondary, modifier = Modifier.size(16.dp)) }
                                    IconButton(
                                        onClick = { editIds = editIds.filterNot { it == id } },
                                        modifier = Modifier.size(28.dp)
                                    ) { Icon(Icons.Rounded.Delete, null, tint = NocturneColors.Danger, modifier = Modifier.size(16.dp)) }
                                }
                            }
                        }
                    }
                    TextButton(
                        onClick = {
                            onDeletePlaylist(pl.id)
                            if (selectedPlaylistId == pl.id) selectedPlaylistId = null
                            editingPlaylist = null
                        }
                    ) {
                        Text(strings.deletePlaylist, color = NocturneColors.Danger, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdatePlaylist(pl.copy(name = editName.ifBlank { pl.name }, trackIds = editIds))
                        editingPlaylist = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black)
                ) {
                    Text(strings.saveChanges, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPlaylist = null }) {
                    Text(strings.cancel, color = NocturneColors.TextSecondary)
                }
            }
        )
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
