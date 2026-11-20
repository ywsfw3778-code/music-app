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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.AppStrings
import com.example.model.ArtworkBgMode
import com.example.model.LibrarySubTab
import com.example.model.MainTab
import com.example.model.SortOption
import com.example.model.StringsProvider
import com.example.model.Track
import com.example.ui.components.BlurredWallpaperImage
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.PlaylistPickerDialog
import com.example.ui.components.RenameTrackDialog
import com.example.ui.components.TrackDetailsDialog
import com.example.ui.components.TrackOptionsBottomSheet
import com.example.ui.screens.ArtistsAlbumsScreen
import com.example.ui.screens.FullPlayerSheet
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LocalWaterGlassState
import com.example.ui.theme.NocturneColors
import com.example.ui.theme.SukoonNocturneTheme
import com.example.ui.theme.frostedFloatingBar
import com.example.ui.theme.rememberWaterGlassState
import com.example.ui.theme.waterGlassBackground

class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SukoonAppRoot(viewModel = viewModel)
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.persistSessionNow()
    }
}

@Composable
fun SukoonAppRoot(viewModel: PlayerViewModel) {
    val isSessionReady by viewModel.isSessionReady.collectAsState()
    if (!isSessionReady) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NocturneColors.Black)
        )
        return
    }

    val context = LocalContext.current
    val initialSession by viewModel.initialSession.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val strings = StringsProvider.forLang(settings.language)
    val currentTrack by viewModel.currentTrack.collectAsState()
    val bgArtUriState by viewModel.bgArtUri.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isCrossfading by viewModel.isCrossfading.collectAsState()
    val accentInt by viewModel.accentColor.collectAsState()
    val secondaryInt by viewModel.secondaryColor.collectAsState()

    val accentColor by animateColorAsState(
        targetValue = Color(accentInt),
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "accentColor"
    )
    val secondaryColor by animateColorAsState(
        targetValue = Color(secondaryInt),
        animationSpec = tween(220, easing = FastOutSlowInEasing),
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
            viewModel.syncLibraryOnStartup()
        }
    }

    var currentTab by remember { mutableStateOf(initialSession.lastMainTab) }
    var showFullPlayer by remember { mutableStateOf(false) }
    var menuTrack by remember { mutableStateOf<Track?>(null) }
    var detailsTrack by remember { mutableStateOf<Track?>(null) }
    var playlistPickerTrack by remember { mutableStateOf<Track?>(null) }
    var renameTrack by remember { mutableStateOf<Track?>(null) }

    val miniArtCenterRef = remember { object { var center: Offset? = null } }
    val launchOriginRef = remember { object { var center: Offset? = null } }
    val expandProgress = remember { Animatable(0f) }
    var isFullPlayerMounted by remember { mutableStateOf(false) }

    LaunchedEffect(currentTrack?.id) {
        if (currentTrack != null && viewModel.consumedStartupFullPlayerSnap()) {
            isFullPlayerMounted = true
            expandProgress.snapTo(1f)
            showFullPlayer = true
        }
    }

    LaunchedEffect(showFullPlayer, currentTrack != null) {
        val shouldShow = showFullPlayer && currentTrack != null
        if (shouldShow) {
            if (!isFullPlayerMounted) {
                launchOriginRef.center = miniArtCenterRef.center
                isFullPlayerMounted = true
            }
            if (expandProgress.value < 0.999f) {
                expandProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.86f,
                        stiffness = 380f
                    )
                )
            }
        } else if (isFullPlayerMounted) {
            miniArtCenterRef.center?.let { launchOriginRef.center = it }
            expandProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
            )
            isFullPlayerMounted = false
        }
    }

    BackHandler(enabled = showFullPlayer && menuTrack == null) {
        showFullPlayer = false
        viewModel.updateFullPlayerOpen(false)
    }
    BackHandler(enabled = !showFullPlayer && menuTrack == null && currentTab != MainTab.LIBRARY) {
        currentTab = MainTab.LIBRARY
        viewModel.updateMainTab(MainTab.LIBRARY)
    }

    val layoutDir = LayoutDirection.Rtl
    val waterGlassState = rememberWaterGlassState(blurEnabled = settings.glassBlurEnabled)

    LaunchedEffect(currentTab, showFullPlayer) {
        viewModel.setFullPlayerVisible(showFullPlayer)
        if (currentTab == MainTab.SETTINGS || showFullPlayer) {
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
                // Pure Background Box recorded at 1:1 full screen into waterGlassState (paused when FullPlayer covers screen)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .waterGlassBackground(waterGlassState, active = !showFullPlayer || menuTrack != null)
                ) {
                    val effectiveBgUri = if (settings.artworkBgMode != ArtworkBgMode.OFF) bgArtUriState else null
                    Crossfade(
                        targetState = effectiveBgUri,
                        animationSpec = tween(380, easing = FastOutSlowInEasing),
                        label = "mainBgFade"
                    ) { artUri ->
                        if (!artUri.isNullOrBlank()) {
                            BlurredWallpaperImage(
                                artUri = artUri,
                                alpha = if (settings.artworkBgMode == ArtworkBgMode.FULL) 0.55f else 0.45f,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    val colorsOnlyMode = settings.artworkBgMode == ArtworkBgMode.OFF || bgArtUriState.isNullOrBlank()
                    val hasActiveAccent = currentTrack != null && settings.dynamicAccent
                    val ambientBrush = remember(colorsOnlyMode, accentColor, secondaryColor, hasActiveAccent) {
                        Brush.verticalGradient(
                            if (colorsOnlyMode) {
                                listOf(
                                    accentColor.copy(alpha = if (hasActiveAccent) 0.72f else 0.36f),
                                    secondaryColor.copy(alpha = if (hasActiveAccent) 0.56f else 0.28f),
                                    accentColor.copy(alpha = if (hasActiveAccent) 0.38f else 0.20f)
                                )
                            } else {
                                listOf(
                                    accentColor.copy(alpha = if (hasActiveAccent) 0.28f else 0.14f),
                                    secondaryColor.copy(alpha = if (hasActiveAccent) 0.22f else 0.10f),
                                    accentColor.copy(alpha = if (hasActiveAccent) 0.18f else 0.08f)
                                )
                            }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                drawRect(ambientBrush)
                            }
                    )
                }

                val listBottomPad = remember(currentTrack != null) {
                    PaddingValues(bottom = if (currentTrack != null) 168.dp else 96.dp)
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            val forward = targetState.ordinal >= initialState.ordinal
                            val dir = if (forward) 1 else -1
                            (slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { dir * it / 8 } + fadeIn(tween(180)))
                                .togetherWith(
                                    slideOutHorizontally(tween(180, easing = FastOutSlowInEasing)) { -dir * it / 8 } + fadeOut(tween(140))
                                )
                        },
                        label = "mainTabTransition",
                        modifier = Modifier.fillMaxSize()
                    ) { activeTab ->
                        val pageVisible = !showFullPlayer && currentTab == activeTab
                        when (activeTab) {
                            MainTab.LIBRARY -> LibraryTabContainer(
                                viewModel = viewModel,
                                strings = strings,
                                currentTrackId = currentTrack?.id,
                                isPlaying = isPlaying,
                                accentColor = accentColor,
                                hasAudioPermission = hasAudioPerm,
                                onRequestPermission = { permLauncher.launch(arrayOf(audioPermission)) },
                                onMoreClick = { menuTrack = it },
                                bottomPadding = listBottomPad,
                                isPageActive = pageVisible,
                                initialSubTab = initialSession.lastLibrarySubTab,
                                initialSortOption = initialSession.lastSortOption,
                                initialScrollIndex = initialSession.lastScrollIndex,
                                initialScrollOffset = initialSession.lastScrollOffset
                            )

                            MainTab.ARTISTS -> ArtistsTabContainer(
                                viewModel = viewModel,
                                strings = strings,
                                currentTrackId = currentTrack?.id,
                                isPlaying = isPlaying,
                                accentColor = accentColor,
                                onMoreClick = { menuTrack = it },
                                bottomPadding = listBottomPad,
                                isPageActive = pageVisible
                            )

                            MainTab.SETTINGS -> SettingsTabContainer(
                                viewModel = viewModel,
                                strings = strings,
                                settings = settings,
                                hasNotificationPermission = hasNotifPerm,
                                accentColor = accentColor,
                                onOpenNotificationSettings = {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                },
                                bottomPadding = listBottomPad,
                                isPageActive = pageVisible
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
                            accentColor = accentColor,
                            onOpenFull = {
                                showFullPlayer = true
                                viewModel.updateFullPlayerOpen(true)
                            },
                            onArtworkPositioned = { center -> miniArtCenterRef.center = center }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .frostedFloatingBar(RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val navItems = remember(strings) {
                            listOf(
                                Triple(MainTab.LIBRARY, strings.navLibrary, Icons.Rounded.LibraryMusic),
                                Triple(MainTab.ARTISTS, strings.navArtists, Icons.Rounded.Person),
                                Triple(MainTab.SETTINGS, strings.navSettings, Icons.Rounded.Settings)
                            )
                        }
                        navItems.forEach { (tab, label, icon) ->
                            val selected = currentTab == tab
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (selected) NocturneColors.WaterDropPillBg else Color.Transparent)
                                    .testTag("nav_tab_${tab.name.lowercase()}")
                                    .clickable {
                                        currentTab = tab
                                        viewModel.updateMainTab(tab)
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

                // Full Player Modal Sheet — morphs smoothly out of the MiniPlayer artwork to fill the screen
                if (isFullPlayerMounted && currentTrack != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = expandProgress.value.coerceIn(0f, 1f) * 0.65f }
                            .background(Color.Black)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val p = expandProgress.value.coerceIn(0f, 1f)
                                val w = size.width
                                val h = size.height
                                val targetCenterX = w * 0.5f
                                val targetCenterY = h * 0.5f
                                val fallbackOrigin = Offset(
                                    x = if (layoutDir == LayoutDirection.Rtl) w * 0.84f else w * 0.16f,
                                    y = h * 0.84f
                                )
                                val origin = launchOriginRef.center ?: fallbackOrigin
                                val startScale = 0.14f
                                val scale = startScale + (1f - startScale) * p
                                scaleX = scale
                                scaleY = scale
                                translationX = (origin.x - targetCenterX) * (1f - p)
                                translationY = (origin.y - targetCenterY) * (1f - p)
                                alpha = (p / 0.16f).coerceIn(0f, 1f)
                                shape = RoundedCornerShape(((1f - p) * 86f).dp)
                                clip = p < 0.998f
                            }
                    ) {
                        currentTrack?.let { track ->
                            FullPlayerContainer(
                                viewModel = viewModel,
                                strings = strings,
                                track = track,
                                isPlaying = isPlaying,
                                crossfadeEnabled = settings.crossfadeEnabled,
                                crossfadeSec = settings.crossfadeSec,
                                isCrossfading = isCrossfading,
                                showArtworkBg = settings.artworkBgMode != ArtworkBgMode.OFF,
                                glassBlurEnabled = settings.glassBlurEnabled,
                                lyricOffsetMs = settings.lyricOffsetMs,
                                accentColor = accentColor,
                                secondaryColor = secondaryColor,
                                initialPage = initialSession.lastFullPlayerPage,
                                onPageChange = { viewModel.updateFullPlayerPage(it) },
                                onClose = {
                                    showFullPlayer = false
                                    viewModel.updateFullPlayerOpen(false)
                                },
                                onMoreClick = { menuTrack = track }
                            )
                        }
                    }
                }

                var lastMenuTrack by remember { mutableStateOf<Track?>(null) }
                if (menuTrack != null) {
                    lastMenuTrack = menuTrack
                }
                AnimatedVisibility(
                    visible = menuTrack != null,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(150))
                ) {
                    lastMenuTrack?.let { t ->
                        val favoriteIds by viewModel.favoriteIds.collectAsState()
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
                            onRename = { renameTrack = t; menuTrack = null },
                            onViewLyrics = {
                                if (currentTrack?.id != t.id) viewModel.playTrack(t)
                                showFullPlayer = true
                                viewModel.updateFullPlayerOpen(true)
                                menuTrack = null
                            },
                            onShowDetails = { detailsTrack = t; menuTrack = null },
                            onExclude = { viewModel.excludeTrack(t); menuTrack = null }
                        )
                    }
                }

                ToastBannerOverlay(
                    viewModel = viewModel,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 12.dp)
                )
            }

            detailsTrack?.let { t ->
                TrackDetailsDialog(
                    track = t,
                    strings = strings,
                    accentColor = accentColor,
                    onDismiss = { detailsTrack = null }
                )
            }

            renameTrack?.let { t ->
                RenameTrackDialog(
                    track = t,
                    strings = strings,
                    accentColor = accentColor,
                    onConfirm = { newTitle ->
                        viewModel.renameTrack(t, newTitle)
                        renameTrack = null
                    },
                    onDismiss = { renameTrack = null }
                )
            }

            playlistPickerTrack?.let { t ->
                val playlists by viewModel.playlists.collectAsState()
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
private fun LibraryTabContainer(
    viewModel: PlayerViewModel,
    strings: AppStrings,
    currentTrackId: String?,
    isPlaying: Boolean,
    accentColor: Color,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onMoreClick: (Track) -> Unit,
    bottomPadding: PaddingValues,
    isPageActive: Boolean,
    initialSubTab: LibrarySubTab,
    initialSortOption: SortOption,
    initialScrollIndex: Int,
    initialScrollOffset: Int
) {
    val activeTracks by viewModel.activeTracks.collectAsState()
    val excludedTracks by viewModel.excludedTracks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    LibraryScreen(
        strings = strings,
        tracks = activeTracks,
        excludedTracks = excludedTracks,
        playlists = playlists,
        favoriteIds = favoriteIds,
        currentTrackId = currentTrackId,
        isPlaying = isPlaying,
        accentColor = accentColor,
        hasAudioPermission = hasAudioPermission,
        onRequestPermission = onRequestPermission,
        onScan = { viewModel.scanLibrary() },
        onPlayTrack = { t, list -> viewModel.playTrack(t, list) },
        onToggleFavorite = { viewModel.toggleFavorite(it) },
        onMoreClick = onMoreClick,
        onRestoreTrack = { viewModel.restoreTrack(it) },
        onCreatePlaylist = { name, ids -> viewModel.createPlaylist(name, ids) },
        onUpdatePlaylist = { viewModel.updatePlaylist(it) },
        onDeletePlaylist = { viewModel.deletePlaylist(it) },
        onRemoveFromPlaylist = { plId, tId -> viewModel.removeTrackFromPlaylist(plId, tId) },
        bottomPadding = bottomPadding,
        isPageActive = isPageActive,
        initialSubTab = initialSubTab,
        initialSortOption = initialSortOption,
        initialScrollIndex = initialScrollIndex,
        initialScrollOffset = initialScrollOffset,
        isLibraryReady = !isScanning,
        onSubTabChange = { viewModel.updateLibrarySubTab(it) },
        onSortOptionChange = { viewModel.updateSortOption(it) },
        onScrollChanged = { idx, offset -> viewModel.updateLibraryScroll(idx, offset) }
    )
}

@Composable
private fun ArtistsTabContainer(
    viewModel: PlayerViewModel,
    strings: AppStrings,
    currentTrackId: String?,
    isPlaying: Boolean,
    accentColor: Color,
    onMoreClick: (Track) -> Unit,
    bottomPadding: PaddingValues,
    isPageActive: Boolean
) {
    val artists by viewModel.artists.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val activeTracks by viewModel.activeTracks.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val artistPhotos by viewModel.artistPhotos.collectAsState()
    val albumArt by viewModel.albumArt.collectAsState()

    ArtistsAlbumsScreen(
        strings = strings,
        artists = artists,
        albums = albums,
        tracks = activeTracks,
        favoriteIds = favoriteIds,
        currentTrackId = currentTrackId,
        isPlaying = isPlaying,
        accentColor = accentColor,
        artistPhotos = artistPhotos,
        albumArt = albumArt,
        onPlayTrack = { t, list -> viewModel.playTrack(t, list) },
        onToggleFavorite = { viewModel.toggleFavorite(it) },
        onMoreClick = onMoreClick,
        bottomPadding = bottomPadding,
        isPageActive = isPageActive
    )
}

@Composable
private fun SettingsTabContainer(
    viewModel: PlayerViewModel,
    strings: AppStrings,
    settings: com.example.model.AppSettings,
    hasNotificationPermission: Boolean,
    accentColor: Color,
    onOpenNotificationSettings: () -> Unit,
    bottomPadding: PaddingValues,
    isPageActive: Boolean
) {
    val activeTracks by viewModel.activeTracks.collectAsState()
    val excludedTracks by viewModel.excludedTracks.collectAsState()
    val folderItems by viewModel.folderItems.collectAsState()
    val artworkFetchProgress by viewModel.artworkFetchProgress.collectAsState()

    SettingsScreen(
        strings = strings,
        settings = settings,
        activeCount = activeTracks.size,
        excludedCount = excludedTracks.size,
        activeTracks = activeTracks,
        excludedTracks = excludedTracks,
        folderItems = folderItems,
        artworkFetchProgress = artworkFetchProgress,
        hasNotificationPermission = hasNotificationPermission,
        accentColor = accentColor,
        onLanguageChange = { viewModel.updateLanguage(it) },
        onCrossfadeChange = { en, sec -> viewModel.updateCrossfade(en, sec) },
        onAutoExcludeRecordingsChange = { viewModel.updateAutoExcludeRecordings(it) },
        onMinDurationChange = { viewModel.updateMinDuration(it) },
        onAddKeyword = { viewModel.addCustomKeyword(it) },
        onRemoveKeyword = { viewModel.removeCustomKeyword(it) },
        onToggleExcludedFolder = { viewModel.toggleExcludedFolder(it) },
        onSetAllFoldersExcluded = { paths, ex -> viewModel.setAllFoldersExcluded(paths, ex) },
        onFetchTracksArtwork = { ids -> viewModel.fetchArtworkForTracks(ids) },
        onDynamicAccentChange = { viewModel.updateDynamicAccent(it) },
        onArtworkBgModeChange = { viewModel.updateArtworkBgMode(it) },
        onGlassBlurChange = { viewModel.updateGlassBlur(it) },
        onRestoreTrack = { viewModel.restoreTrack(it) },
        onRescan = { viewModel.scanLibrary() },
        onClearData = { viewModel.clearSavedData() },
        onOpenNotificationSettings = onOpenNotificationSettings,
        bottomPadding = bottomPadding,
        isPageActive = isPageActive
    )
}

@Composable
private fun ToastBannerOverlay(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val toastMsg by viewModel.toastMessage.collectAsState()
    AnimatedVisibility(
        visible = toastMsg != null,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier
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

@Composable
private fun MiniPlayerContainer(
    viewModel: PlayerViewModel,
    track: Track,
    isPlaying: Boolean,
    isCrossfading: Boolean,
    accentColor: Color,
    onOpenFull: () -> Unit,
    onArtworkPositioned: (Offset) -> Unit
) {
    MiniPlayerBar(
        track = track,
        isPlaying = isPlaying,
        positionMsFlow = viewModel.positionMs,
        durationMsFlow = viewModel.durationMs,
        isCrossfading = isCrossfading,
        crossfadeProgressFlow = viewModel.crossfadeProgress,
        accentColor = accentColor,
        onOpenFull = onOpenFull,
        onTogglePlayPause = { viewModel.togglePlayPause() },
        onNext = { viewModel.seekToNext() },
        onClose = { viewModel.stopAndClearPlayer() },
        onArtworkPositioned = onArtworkPositioned
    )
}

@Composable
private fun FullPlayerContainer(
    viewModel: PlayerViewModel,
    strings: AppStrings,
    track: Track,
    isPlaying: Boolean,
    crossfadeEnabled: Boolean,
    crossfadeSec: Int,
    isCrossfading: Boolean,
    showArtworkBg: Boolean,
    glassBlurEnabled: Boolean,
    lyricOffsetMs: Long,
    accentColor: Color,
    secondaryColor: Color,
    initialPage: Int,
    onPageChange: (Int) -> Unit,
    onClose: () -> Unit,
    onMoreClick: () -> Unit
) {
    val favoriteIds by viewModel.favoriteIds.collectAsState()
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
        isFavorite = favoriteIds.contains(track.id),
        positionMsFlow = viewModel.positionMs,
        durationMsFlow = viewModel.durationMs,
        shuffleEnabled = shuffleEnabled,
        repeatMode = repeatMode,
        crossfadeEnabled = crossfadeEnabled,
        crossfadeSec = crossfadeSec,
        isCrossfading = isCrossfading,
        crossfadeProgressFlow = viewModel.crossfadeProgress,
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
        onMoreClick = onMoreClick,
        initialPage = initialPage,
        onPageChange = onPageChange
    )
}
