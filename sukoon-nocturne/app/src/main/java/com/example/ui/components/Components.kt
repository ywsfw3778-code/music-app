package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.AudioRepository
import com.example.model.AppStrings
import com.example.model.Playlist
import com.example.model.Track
import com.example.ui.theme.NocturneColors
import com.example.ui.theme.frostedFloatingBar
import com.example.ui.theme.matteCard

@Composable
fun TrackArtwork(
    artUri: String?,
    size: Dp,
    cornerRadius: Dp = 12.dp,
    accentColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val bgBrush = remember(accentColor) {
        Brush.linearGradient(
            listOf(
                accentColor.copy(alpha = 0.22f),
                Color(0xFF14141C)
            )
        )
    }
    val resolvedUri by produceState<String?>(
        initialValue = if (!artUri.isNullOrBlank() && !artUri.startsWith("track_art://")) artUri else null,
        key1 = artUri
    ) {
        if (artUri.isNullOrBlank()) {
            value = null
        } else if (!artUri.startsWith("track_art://")) {
            value = artUri
        } else {
            value = AudioRepository.resolveArtUri(context, artUri)
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(bgBrush),
        contentAlignment = Alignment.Center
    ) {
        if (!resolvedUri.isNullOrBlank()) {
            AsyncImage(
                model = remember(resolvedUri) {
                    ImageRequest.Builder(context)
                        .data(resolvedUri)
                        .size(128, 128)
                        .memoryCacheKey(resolvedUri)
                        .diskCacheKey(resolvedUri)
                        .crossfade(false)
                        .build()
                },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
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
    onMoreClick: () -> Unit
) {
    val rowBg = if (isCurrent) Color.White.copy(alpha = 0.085f) else Color.Transparent
    val ext = track.fileName.substringAfterLast('.', "MP3").uppercase()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(rowBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackArtwork(
            artUri = track.artUri,
            size = 48.dp,
            cornerRadius = 12.dp,
            accentColor = accentColor
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
    progress: Float,
    isCrossfading: Boolean = false,
    crossfadeProgress: Float = 0f,
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
            .clickable(onClick = onOpenFull)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stationary Album Artwork with smooth internal Crossfade
            TrackArtwork(
                artUri = track.artUri,
                size = 44.dp,
                cornerRadius = 12.dp,
                accentColor = accentColor
            )

            Spacer(Modifier.width(10.dp))

            // Vertical Slide Transition for Track Title and Artist
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
                                Text(
                                    text = "${(crossfadeProgress * 100).toInt()}%",
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
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = null,
                        tint = NocturneColors.TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = NocturneColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .padding(horizontal = 12.dp),
            color = accentColor,
            trackColor = Color.White.copy(alpha = 0.08f)
        )
        Spacer(Modifier.height(3.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onViewLyrics: () -> Unit,
    onShowDetails: () -> Unit,
    onExclude: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = NocturneColors.MatteElevatedBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = track.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                style = TextStyle(textDirection = TextDirection.Content),
                modifier = Modifier.padding(bottom = 8.dp)
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
            SheetActionRow(Icons.Rounded.Lyrics, strings.viewLyrics, accentColor, onViewLyrics)
            SheetActionRow(Icons.Rounded.Info, strings.trackDetails, accentColor, onShowDetails)
            SheetActionRow(Icons.Rounded.Block, strings.deleteFromLibrary, NocturneColors.Danger, onExclude)
            Spacer(Modifier.height(16.dp))
        }
    }
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
    return "%02d:%02d".format(mins, secs)
}

fun formatMillis(ms: Long): String {
    val total = (ms / 1000L).coerceAtLeast(0L)
    val mins = total / 60
    val secs = total % 60
    return "%d:%02d".format(mins, secs)
}
