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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppStrings
import com.example.model.Track
import com.example.ui.components.TrackRowItem
import com.example.ui.theme.NocturneColors
import com.example.ui.theme.matteCard

@Composable
fun FoldersScreen(
    strings: AppStrings,
    tracks: List<Track>,
    favoriteIds: Set<String>,
    currentTrackId: String?,
    isPlaying: Boolean,
    accentColor: Color,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onMoreClick: (Track) -> Unit,
    bottomPadding: PaddingValues
) {
    val folders = remember(tracks) {
        tracks.groupBy { it.folderPath }
            .map { (path, list) ->
                Triple(path, list.firstOrNull()?.folderName ?: "Music", list)
            }
            .sortedByDescending { it.third.size }
    }

    var selectedFolderPath by remember { mutableStateOf<String?>(null) }

    val displayedTracks = remember(tracks, selectedFolderPath) {
        if (selectedFolderPath == null) tracks else tracks.filter { it.folderPath == selectedFolderPath }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Text(
            text = strings.navFolders,
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "${tracks.size} ${strings.songsCount}",
            color = NocturneColors.TextSecondary,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(14.dp))

        if (tracks.isEmpty()) {
            Column(
                modifier = Modifier
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val allSelected = selectedFolderPath == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (allSelected) NocturneColors.WaterDropPillBg else NocturneColors.WaterDropBarBg)
                        .clickable { selectedFolderPath = null }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = "${strings.allFolders} · ${tracks.size}",
                        color = if (allSelected) Color.White else Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                folders.forEach { (path, name, list) ->
                    val selected = selectedFolderPath == path
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) NocturneColors.WaterDropPillBg else NocturneColors.WaterDropBarBg)
                            .clickable { selectedFolderPath = path }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Folder,
                            null,
                            tint = if (selected) accentColor else Color(0xFFCBD5E1),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "$name · ${list.size}",
                            color = if (selected) Color.White else Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = bottomPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
}
