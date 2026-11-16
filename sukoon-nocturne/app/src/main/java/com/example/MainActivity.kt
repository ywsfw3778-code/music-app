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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.AppLanguage
import com.example.model.ArtworkBgMode
import com.example.model.MainTab
import com.example.model.StringsProvider
import com.example.model.Track
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.PlaylistPickerDialog
import com.example.ui.components.TrackDetailsDialog
import com.example.ui.components.TrackOptionsBottomSheet
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.FullPlayerSheet
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LocalWaterGlassState
import com.example.ui.theme.NocturneColors
import com.example.ui.theme.SukoonNocturneTheme
import com.example.ui.theme.frostedFloatingBar
import com.example.ui.theme.rememberWaterGlassState
import com.example.ui.theme.waterGlassBackground
import kotlinx.coroutines.launch

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
    val bgArtUriState by viewModel.bgArtUri.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isCrossfading by viewModel.isCrossfading.collectAsState()
    val crossfadeProgress by viewModel.crossfadeProgress.collectAsState()
    val accentInt by viewModel.accentColor.collectAsState()
    val secondaryInt by viewModel.secondaryColor.collectAsState()
    val toastMsg by viewModel.toastMessage.collectAsState()

    val accentColor by animateColorAsState(
        targetValue = Color(accentInt),
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "accentColor"
    )
    val secondaryColor by animateColorAsState(
        targetValue = Color(secondaryInt),
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "secondaryColor"
    )

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

    val pagerState = rememberPagerState(initialPage = MainTab.LIBRARY.ordinal, pageCount = { MainTab.entries.size })
    val coroutineScope = rememberCoroutineScope()
    val currentTab = MainTab.entries[pagerState.currentPage]
    var showFullPlayer by remember { mutableStateOf(false) }
    var menuTrack by remember { mutableStateOf<Track?>(null) }
    var detailsTrack by remember { mutableStateOf<Track?>(null) }
    var playlistPickerTrack by remember { mutableStateOf<Track?>(null) }

    BackHandler(enabled = showFullPlayer) {
        showFullPlayer = false
    }
    BackHandler(enabled = !showFullPlayer && pagerState.currentPage != MainTab.LIBRARY.ordinal) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(
                page = MainTab.LIBRARY.ordinal,
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            )
        }
    }

    // Keep RTL direction consistent across both Arabic and English UI languages
    val layoutDir = LayoutDirection.Rtl
    val waterGlassState = rememberWaterGlassState(blurEnabled = settings.glassBlurEnabled)

    LaunchedEffect(currentTab) {
        if (currentTab == MainTab.SETTINGS) {
            waterGlassState.hasContent = false
        }
    }

    SukoonNocturneTheme(accentColorInt = accentInt) {
        CompositionLocalProvider(
            LocalLayoutDirection provides layoutDir,
            LocalWaterGlassState provides waterGlassState
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NocturneColors.Black)
            ) {
                // Pure Background Box recorded at 1:1 full screen into waterGlassState
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .waterGlassBackground(waterGlassState)
                ) {
                    val effectiveBgUri = if (settings.artworkBgMode != ArtworkBgMode.OFF) bgArtUriState else null
                    Crossfade(
                        targetState = effectiveBgUri,
                        animationSpec = tween(550, easing = FastOutSlowInEasing),
                        label = "mainBgFade"
                    ) { artUri ->
                        if (!artUri.isNullOrBlank()) {
                            AsyncImage(
                                model = remember(artUri) {
                                    ImageRequest.Builder(context)
                                        .data(artUri)
                                        .size(128)
                                        .crossfade(false)
                                        .build()
                                },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                alpha = if (settings.artworkBgMode == ArtworkBgMode.FULL) 0.55f else 0.45f,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(26.dp)
                            )
                        }
                    }

                    // Dynamic Ambient Glow (vibrant gradient when Colors Only, soft tint over artwork)
                    val colorsOnlyMode = settings.artworkBgMode == ArtworkBgMode.OFF || bgArtUriState.isNullOrBlank()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    if (colorsOnlyMode) {
                                        listOf(
                                            accentColor.copy(alpha = if (currentTrack != null && settings.dynamicAccent) 0.72f else 0.36f),
                                            secondaryColor.copy(alpha = if (currentTrack != null && settings.dynamicAccent) 0.56f else 0.28f),
                                            accentColor.copy(alpha = if (currentTrack != null && settings.dynamicAccent) 0.38f else 0.20f)
                                        )
                                    } else {
                                        listOf(
                                            accentColor.copy(alpha = if (currentTrack != null && settings.dynamicAccent) 0.28f else 0.14f),
                                            secondaryColor.copy(alpha = if (currentTrack != null && settings.dynamicAccent) 0.22f else 0.10f),
                                            accentColor.copy(alpha = if (currentTrack != null && settings.dynamicAccent) 0.18f else 0.08f)
                                        )
                                    }
                                )
                            )
                    )
                }

                // Directional Slide + Fade Tab Transition (left-to-right and right-to-left)
                val listBottomPad = PaddingValues(
                    bottom = if (currentTrack != null) 168.dp else 96.dp
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        beyondViewportPageCount = 2,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (MainTab.entries[page]) {
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
                                bottomPadding = listBottomPad,
                                isPageActive = pagerState.currentPage == MainTab.LIBRARY.ordinal
                            )

                            MainTab.FOLDERS -> FoldersScreen(
                                strings = strings,
                                tracks = activeTracks,
                                excludedCount = excludedTracks.size,
                                favoriteIds = favoriteIds,
                                currentTrackId = currentTrack?.id,
                                isPlaying = isPlaying,
                                accentColor = accentColor,
                                onPlayTrack = { t, list -> viewModel.playTrack(t, list) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onMoreClick = { menuTrack = it },
                                bottomPadding = listBottomPad,
                                isPageActive = pagerState.currentPage == MainTab.FOLDERS.ordinal
                            )

                            MainTab.SETTINGS -> SettingsScreen(
                                strings = strings,
                                settings = settings,
                                activeCount = activeTracks.size,
                                excludedCount = excludedTracks.size,
                                excludedTracks = excludedTracks,
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
                                onGlassBlurChange = { viewModel.updateGlassBlur(it) },
                                onRestoreTrack = { viewModel.restoreTrack(it) },
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
                        MiniPlayerContainer(
                            viewModel = viewModel,
                            track = track,
                            isPlaying = isPlaying,
                            isCrossfading = isCrossfading,
                            crossfadeProgress = crossfadeProgress,
                            accentColor = accentColor,
                            onOpenFull = { showFullPlayer = true }
                        )
                    }

                    // Floating Water-Drop Pill Bottom Navigation (LiquidGlassNav)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .frostedFloatingBar(RoundedCornerShape(50))
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
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(
                                                page = tab.ordinal,
                                                animationSpec = tween(220, easing = FastOutSlowInEasing)
                                            )
                                        }
                                    },
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

                // Full Player Modal Sheet (isolates 150ms positionMs updates from main screen)
                AnimatedVisibility(
                    visible = showFullPlayer && currentTrack != null,
                    enter = slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(180)),
                    exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(150))
                ) {
                    currentTrack?.let { track ->
                        FullPlayerContainer(
                            viewModel = viewModel,
                            strings = strings,
                            track = track,
                            isPlaying = isPlaying,
                            isFavorite = favoriteIds.contains(track.id),
                            crossfadeEnabled = settings.crossfadeEnabled,
                            crossfadeSec = settings.crossfadeSec,
                            isCrossfading = isCrossfading,
                            crossfadeProgress = crossfadeProgress,
                            showArtworkBg = settings.artworkBgMode != ArtworkBgMode.OFF,
                            glassBlurEnabled = settings.glassBlurEnabled,
                            lyricOffsetMs = settings.lyricOffsetMs,
                            accentColor = accentColor,
                            secondaryColor = secondaryColor,
                            onClose = { showFullPlayer = false },
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
                                .frostedFloatingBar(RoundedCornerShape(20.dp))
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(msg, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Track Options Bottom Sheet
            menuTrack?.let { t ->
                TrackOptionsBottomSheet(
                    track = t,
                    strings = strings,
                    isFavorite = favoriteIds.contains(t.id),
                    accentColor = accentColor,
                    onDismiss = { menuTrack = null },
                    onPlayNext = { viewModel.playNext(t); menuTrack = null },
                    onAddToQueue = { viewModel.addToQueue(t); menuTrack = null },
                    onToggleFavorite = { viewModel.toggleFavorite(t.id); menuTrack = null },
                    onAddToPlaylist = { playlistPickerTrack = t; menuTrack = null },
                    onViewLyrics = {
                        if (currentTrack?.id != t.id) viewModel.playTrack(t)
                        showFullPlayer = true
                        menuTrack = null
                    },
                    onShowDetails = { detailsTrack = t; menuTrack = null },
                    onExclude = { viewModel.excludeTrack(t); menuTrack = null }
                )
            }

            // Track Details Dialog
            detailsTrack?.let { t ->
                TrackDetailsDialog(
                    track = t,
                    strings = strings,
                    accentColor = accentColor,
                    onDismiss = { detailsTrack = null }
                )
            }

            // Add to Playlist Picker
            playlistPickerTrack?.let { t ->
                PlaylistPickerDialog(
                    track = t,
                    playlists = playlists,
                    strings = strings,
                    onDismiss = { playlistPickerTrack = null },
                    onSelectPlaylist = { plId, trackId ->
                        viewModel.addTrackToPlaylist(plId, trackId)
                        playlistPickerTrack = null
                    }
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerContainer(
    viewModel: PlayerViewModel,
    track: Track,
    isPlaying: Boolean,
    isCrossfading: Boolean,
    crossfadeProgress: Float,
    accentColor: Color,
    onOpenFull: () -> Unit
) {
    val positionMs by viewModel.positionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val prog = if (durationMs > 0L) positionMs.toFloat() / durationMs.toFloat() else 0f

    MiniPlayerBar(
        track = track,
        isPlaying = isPlaying,
        progress = prog,
        isCrossfading = isCrossfading,
        crossfadeProgress = crossfadeProgress,
        accentColor = accentColor,
        onOpenFull = onOpenFull,
        onTogglePlayPause = { viewModel.togglePlayPause() },
        onNext = { viewModel.seekToNext() },
        onClose = { viewModel.stopAndClearPlayer() }
    )
}

@Composable
private fun FullPlayerContainer(
    viewModel: PlayerViewModel,
    strings: com.example.model.AppStrings,
    track: Track,
    isPlaying: Boolean,
    isFavorite: Boolean,
    crossfadeEnabled: Boolean,
    crossfadeSec: Int,
    isCrossfading: Boolean,
    crossfadeProgress: Float,
    showArtworkBg: Boolean,
    glassBlurEnabled: Boolean,
    lyricOffsetMs: Long,
    accentColor: Color,
    secondaryColor: Color,
    onClose: () -> Unit,
    onMoreClick: () -> Unit
) {
    val positionMs by viewModel.positionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val lyrics by viewModel.currentLyrics.collectAsState()
    val isFetchingLyrics by viewModel.isFetchingLyrics.collectAsState()
    val manualResults by viewModel.manualSearchResults.collectAsState()
    val isSearchingManual by viewModel.isSearchingManualLyrics.collectAsState()
    val sleepTimerSec by viewModel.sleepTimerSeconds.collectAsState()

    FullPlayerSheet(
        strings = strings,
        track = track,
        isPlaying = isPlaying,
        isFavorite = isFavorite,
        positionMs = positionMs,
        durationMs = durationMs,
        shuffleEnabled = shuffleEnabled,
        repeatMode = repeatMode,
        crossfadeEnabled = crossfadeEnabled,
        crossfadeSec = crossfadeSec,
        isCrossfading = isCrossfading,
        crossfadeProgress = crossfadeProgress,
        showArtworkBg = showArtworkBg,
        glassBlurEnabled = glassBlurEnabled,
        queue = queue,
        lyrics = lyrics,
        isFetchingLyrics = isFetchingLyrics,
        lyricOffsetMs = lyricOffsetMs,
        manualResults = manualResults,
        isSearchingManual = isSearchingManual,
        sleepTimerSeconds = sleepTimerSec,
        accentColor = accentColor,
        secondaryColor = secondaryColor,
        onClose = onClose,
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
        onMoreClick = onMoreClick
    )
}
