package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.model.AppLanguage
import com.example.model.ArtworkBgMode
import com.example.model.MainTab
import com.example.model.StringsProvider
import com.example.model.Track
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.formatDuration
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.FullPlayerSheet
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.NocturneColors
import com.example.ui.theme.SukoonNocturneTheme
import com.example.ui.theme.matteCard
import com.example.ui.theme.waterDropBar

class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SukoonAppRoot(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SukoonAppRoot(viewModel: PlayerViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val strings = StringsProvider.forLang(settings.language)
    val activeTracks by viewModel.activeTracks.collectAsState()
    val excludedTracks by viewModel.excludedTracks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val positionMs by viewModel.positionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val accentInt by viewModel.accentColor.collectAsState()
    val secondaryInt by viewModel.secondaryColor.collectAsState()
    val lyrics by viewModel.currentLyrics.collectAsState()
    val isFetchingLyrics by viewModel.isFetchingLyrics.collectAsState()
    val manualResults by viewModel.manualSearchResults.collectAsState()
    val isSearchingManual by viewModel.isSearchingManualLyrics.collectAsState()
    val sleepTimerSec by viewModel.sleepTimerSeconds.collectAsState()
    val toastMsg by viewModel.toastMessage.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    val accentColor = Color(accentInt)
    val secondaryColor = Color(secondaryInt)

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasAudioPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotifPerm by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasAudioPerm = result[audioPermission] ?: hasAudioPerm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotifPerm = result[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotifPerm
        }
        if (hasAudioPerm) {
            viewModel.scanLibrary()
        }
    }

    LaunchedEffect(Unit) {
        val toRequest = mutableListOf<String>()
        if (!hasAudioPerm) toRequest.add(audioPermission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPerm) {
            toRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (toRequest.isNotEmpty()) {
            permLauncher.launch(toRequest.toTypedArray())
        } else {
            viewModel.scanLibrary()
        }
    }

    var currentTab by remember { mutableStateOf(MainTab.LIBRARY) }
    var showFullPlayer by remember { mutableStateOf(false) }
    var menuTrack by remember { mutableStateOf<Track?>(null) }
    var detailsTrack by remember { mutableStateOf<Track?>(null) }
    var playlistPickerTrack by remember { mutableStateOf<Track?>(null) }

    BackHandler(enabled = showFullPlayer) {
        showFullPlayer = false
    }

    val layoutDir = if (settings.language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr

    SukoonNocturneTheme(accentColorInt = accentInt) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NocturneColors.Black)
            ) {
                // Dynamic Blurred Cover Background
                val artUri = currentTrack?.artUri
                if (!artUri.isNullOrBlank() && settings.artworkBgMode != ArtworkBgMode.OFF) {
                    AsyncImage(
                        model = artUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alpha = if (settings.artworkBgMode == ArtworkBgMode.FULL) 0.22f else 0.16f,
                        modifier = if (settings.artworkBgMode == ArtworkBgMode.FULL) {
                            Modifier.fillMaxSize().blur(55.dp)
                        } else {
                            Modifier.fillMaxWidth().height(340.dp).blur(55.dp)
                        }
                    )
                }

                // Dynamic Radial Glow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.14f),
                                    secondaryColor.copy(alpha = 0.06f),
                                    Color.Black.copy(alpha = 0.92f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    // Smooth Native Compose Tab Transition
                    val listBottomPad = PaddingValues(
                        bottom = if (currentTrack != null) 168.dp else 96.dp
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                val forward = targetState.ordinal >= initialState.ordinal
                                val dir = if (forward) 1 else -1
                                (fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                                    slideInHorizontally(tween(180, easing = FastOutSlowInEasing)) { dir * it / 8 })
                                    .togetherWith(
                                        fadeOut(tween(120)) +
                                            slideOutHorizontally(tween(140)) { -dir * it / 10 }
                                    )
                            },
                            label = "mainTabTransition"
                        ) { tab ->
                            when (tab) {
                                MainTab.LIBRARY -> LibraryScreen(
                                    strings = strings,
                                    tracks = activeTracks,
                                    excludedTracks = excludedTracks,
                                    playlists = playlists,
                                    favoriteIds = favoriteIds,
                                    currentTrackId = currentTrack?.id,
                                    isPlaying = isPlaying,
                                    accentColor = accentColor,
                                    hasAudioPermission = hasAudioPerm,
                                    onRequestPermission = { permLauncher.launch(arrayOf(audioPermission)) },
                                    onScan = { viewModel.scanLibrary() },
                                    onPlayTrack = { t, list -> viewModel.playTrack(t, list) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onMoreClick = { menuTrack = it },
                                    onRestoreTrack = { viewModel.restoreTrack(it) },
                                    onCreatePlaylist = { name, ids -> viewModel.createPlaylist(name, ids) },
                                    onUpdatePlaylist = { viewModel.updatePlaylist(it) },
                                    onDeletePlaylist = { viewModel.deletePlaylist(it) },
                                    onRemoveFromPlaylist = { plId, tId -> viewModel.removeTrackFromPlaylist(plId, tId) },
                                    bottomPadding = listBottomPad
                                )

                                MainTab.FOLDERS -> FoldersScreen(
                                    strings = strings,
                                    tracks = activeTracks,
                                    favoriteIds = favoriteIds,
                                    currentTrackId = currentTrack?.id,
                                    isPlaying = isPlaying,
                                    accentColor = accentColor,
                                    onPlayTrack = { t, list -> viewModel.playTrack(t, list) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onMoreClick = { menuTrack = it },
                                    bottomPadding = listBottomPad
                                )

                                MainTab.SETTINGS -> SettingsScreen(
                                    strings = strings,
                                    settings = settings,
                                    activeCount = activeTracks.size,
                                    excludedCount = excludedTracks.size,
                                    hasNotificationPermission = hasNotifPerm,
                                    accentColor = accentColor,
                                    onLanguageChange = { viewModel.updateLanguage(it) },
                                    onCrossfadeChange = { en, sec -> viewModel.updateCrossfade(en, sec) },
                                    onAutoExcludeRecordingsChange = { viewModel.updateAutoExcludeRecordings(it) },
                                    onMinDurationChange = { viewModel.updateMinDuration(it) },
                                    onAddKeyword = { viewModel.addCustomKeyword(it) },
                                    onRemoveKeyword = { viewModel.removeCustomKeyword(it) },
                                    onDynamicAccentChange = { viewModel.updateDynamicAccent(it) },
                                    onArtworkBgModeChange = { viewModel.updateArtworkBgMode(it) },
                                    onRescan = { viewModel.scanLibrary() },
                                    onClearData = { viewModel.clearSavedData() },
                                    onOpenNotificationSettings = {
                                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                        context.startActivity(intent)
                                    },
                                    bottomPadding = listBottomPad
                                )
                            }
                        }
                    }
                }

                // Bottom Floating MiniPlayer + Pill Navigation
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    currentTrack?.let { track ->
                        val prog = if (durationMs > 0L) positionMs.toFloat() / durationMs.toFloat() else 0f
                        MiniPlayerBar(
                            track = track,
                            isPlaying = isPlaying,
                            progress = prog,
                            accentColor = accentColor,
                            onOpenFull = { showFullPlayer = true },
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onNext = { viewModel.seekToNext() },
                            onClose = { viewModel.stopAndClearPlayer() }
                        )
                    }

                    // Floating Water-Drop Pill Bottom Navigation (LiquidGlassNav)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .waterDropBar(RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            Triple(MainTab.LIBRARY, strings.navLibrary, Icons.Rounded.LibraryMusic),
                            Triple(MainTab.FOLDERS, strings.navFolders, Icons.Rounded.Folder),
                            Triple(MainTab.SETTINGS, strings.navSettings, Icons.Rounded.Settings)
                        ).forEach { (tab, label, icon) ->
                            val selected = currentTab == tab
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (selected) NocturneColors.WaterDropPillBg else Color.Transparent)
                                    .clickable { currentTab = tab },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (selected) accentColor else Color(0xFFCBD5E1),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = label,
                                    color = if (selected) Color.White else Color(0xFFCBD5E1),
                                    fontSize = 10.5.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }
                }

                // Full Player Modal Sheet
                AnimatedVisibility(
                    visible = showFullPlayer && currentTrack != null,
                    enter = slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(180)),
                    exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(150))
                ) {
                    currentTrack?.let { track ->
                        FullPlayerSheet(
                            strings = strings,
                            track = track,
                            isPlaying = isPlaying,
                            isFavorite = favoriteIds.contains(track.id),
                            positionMs = positionMs,
                            durationMs = durationMs,
                            shuffleEnabled = shuffleEnabled,
                            repeatMode = repeatMode,
                            crossfadeEnabled = settings.crossfadeEnabled,
                            queue = queue,
                            lyrics = lyrics,
                            isFetchingLyrics = isFetchingLyrics,
                            lyricOffsetMs = settings.lyricOffsetMs,
                            manualResults = manualResults,
                            isSearchingManual = isSearchingManual,
                            sleepTimerSeconds = sleepTimerSec,
                            accentColor = accentColor,
                            secondaryColor = secondaryColor,
                            onClose = { showFullPlayer = false },
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onNext = { viewModel.seekToNext() },
                            onPrevious = { viewModel.seekToPrevious() },
                            onSeek = { viewModel.seekTo(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(track.id) },
                            onToggleShuffle = { viewModel.toggleShuffle() },
                            onCycleRepeat = { viewModel.cycleRepeatMode() },
                            onPlayFromQueue = { viewModel.playTrack(it, queue) },
                            onClearQueue = { viewModel.clearQueue() },
                            onSearchManualLyrics = { viewModel.searchLyricsManual(it) },
                            onApplyManualLyrics = { viewModel.applyManualLyrics(it) },
                            onApplyLrcFile = { viewModel.applyLrcContent(it) },
                            onAdjustLyricOffset = { viewModel.adjustLyricOffset(it) },
                            onSetSleepTimer = { viewModel.startSleepTimer(it) },
                            onMoreClick = { menuTrack = track }
                        )
                    }
                }

                // Toast Banner
                AnimatedVisibility(
                    visible = toastMsg != null,
                    enter = fadeIn() + slideInVertically { -it },
                    exit = fadeOut() + slideOutVertically { -it },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 12.dp)
                ) {
                    toastMsg?.let { msg ->
                        Box(
                            modifier = Modifier
                                .waterDropBar(RoundedCornerShape(20.dp))
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(msg, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Track Options Bottom Sheet
            menuTrack?.let { t ->
                val isFav = favoriteIds.contains(t.id)
                ModalBottomSheet(
                    onDismissRequest = { menuTrack = null },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = NocturneColors.MatteElevatedBg
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(t.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                        SheetActionRow(Icons.Rounded.PlaylistPlay, strings.playNext, accentColor) {
                            viewModel.playNext(t); menuTrack = null
                        }
                        SheetActionRow(Icons.Rounded.QueueMusic, strings.addToQueue, accentColor) {
                            viewModel.addToQueue(t); menuTrack = null
                        }
                        SheetActionRow(
                            Icons.Rounded.Favorite,
                            if (isFav) strings.removeFromFavorites else strings.addToFavorites,
                            accentColor
                        ) {
                            viewModel.toggleFavorite(t.id); menuTrack = null
                        }
                        SheetActionRow(Icons.Rounded.PlaylistAdd, strings.addToPlaylist, accentColor) {
                            playlistPickerTrack = t; menuTrack = null
                        }
                        SheetActionRow(Icons.Rounded.Lyrics, strings.viewLyrics, accentColor) {
                            if (currentTrack?.id != t.id) viewModel.playTrack(t)
                            showFullPlayer = true
                            menuTrack = null
                        }
                        SheetActionRow(Icons.Rounded.Info, strings.trackDetails, accentColor) {
                            detailsTrack = t; menuTrack = null
                        }
                        SheetActionRow(Icons.Rounded.Block, strings.deleteFromLibrary, NocturneColors.Danger) {
                            viewModel.excludeTrack(t); menuTrack = null
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            // Track Details Dialog
            detailsTrack?.let { t ->
                AlertDialog(
                    onDismissRequest = { detailsTrack = null },
                    containerColor = NocturneColors.MatteElevatedBg,
                    title = { Text(strings.trackDetails, color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            DetailRow(strings.fileNameLabel, t.fileName)
                            DetailRow(strings.fileDuration, formatDuration(t.durationSec))
                            DetailRow(strings.fileSize, "${"%.2f".format(t.sizeBytes / (1024.0 * 1024.0))} MB")
                            DetailRow(strings.fileFormat, t.mimeType.ifBlank { "audio" })
                            DetailRow(strings.folderLocation, t.folderPath)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { detailsTrack = null }) {
                            Text(strings.close, color = accentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // Add to Playlist Picker
            playlistPickerTrack?.let { t ->
                AlertDialog(
                    onDismissRequest = { playlistPickerTrack = null },
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
                                            .clickable {
                                                viewModel.addTrackToPlaylist(pl.id, t.id)
                                                playlistPickerTrack = null
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Text("${pl.name} (${pl.trackIds.size})", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { playlistPickerTrack = null }) {
                            Text(strings.close, color = NocturneColors.TextSecondary)
                        }
                    }
                )
            }
        }
    }
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
        Text(value, color = Color.White, fontSize = 13.sp)
    }
}
