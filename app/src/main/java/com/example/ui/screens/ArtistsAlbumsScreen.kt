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
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.example.model.Album
import com.example.model.AppStrings
import com.example.model.Artist
import com.example.model.Track
import com.example.ui.components.TrackRowItem
import com.example.ui.theme.AppFontFamily
import com.example.ui.theme.LocalWaterGlassState
import com.example.ui.theme.NocturneColors
import com.example.ui.theme.matteCard
import com.example.ui.theme.openTopGlassFade
import com.example.ui.theme.waterGlassContent

enum class ArtistsAlbumsTab {
    ARTISTS,
    ALBUMS
}

@Composable
fun ArtistsAlbumsScreen(
    strings: AppStrings,
    artists: List<Artist>,
    albums: List<Album>,
    tracks: List<Track>,
    favoriteIds: Set<String>,
    currentTrackId: String?,
    isPlaying: Boolean,
    accentColor: Color,
    artistPhotos: Map<String, String> = emptyMap(),
    albumArt: Map<String, String> = emptyMap(),
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onMoreClick: (Track) -> Unit,
    bottomPadding: PaddingValues,
    isPageActive: Boolean = true
) {
    var selectedTab by remember { mutableStateOf(ArtistsAlbumsTab.ARTISTS) }
    var selectedArtist by remember { mutableStateOf<Artist?>(null) }
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }

    val artistsGridState = rememberLazyGridState()
    val albumsGridState = rememberLazyGridState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchOpen) {
        if (isSearchOpen) {
            searchFocusRequester.requestFocus()
        }
    }

    BackHandler(enabled = selectedArtist != null || selectedAlbum != null || isSearchOpen) {
        if (isSearchOpen) {
            isSearchOpen = false
            searchQuery = ""
        } else if (selectedArtist != null) {
            selectedArtist = null
        } else if (selectedAlbum != null) {
            selectedAlbum = null
        }
    }

    val filteredArtists = remember(artists, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) artists
        else artists.filter { it.name.contains(q, ignoreCase = true) }
    }

    val filteredAlbums = remember(albums, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) albums
        else albums.filter {
            it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true)
        }
    }

    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var headerHeightDp by remember { mutableStateOf(statusTop + 120.dp) }
    val density = LocalDensity.current
    val headerContentTop by animateDpAsState(
        targetValue = headerHeightDp + 10.dp,
        animationSpec = tween(200),
        label = "artistsHeaderTop"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .waterGlassContent(if (isPageActive) LocalWaterGlassState.current else null)
        ) {
            AnimatedContent(
                targetState = Triple(selectedArtist, selectedAlbum, selectedTab),
                transitionSpec = {
                    val intoDetails = (targetState.first != null || targetState.second != null) &&
                        (initialState.first == null && initialState.second == null)
                    val outOfDetails = (initialState.first != null || initialState.second != null) &&
                        (targetState.first == null && targetState.second == null)

                    if (intoDetails) {
                        (fadeIn(tween(260)) + slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it / 3 })
                            .togetherWith(fadeOut(tween(180)) + slideOutHorizontally(tween(240)) { -it / 4 })
                    } else if (outOfDetails) {
                        (fadeIn(tween(240)) + slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { -it / 4 })
                            .togetherWith(fadeOut(tween(180)) + slideOutHorizontally(tween(240)) { it / 3 })
                    } else {
                        fadeIn(tween(200)).togetherWith(fadeOut(tween(180)))
                    }
                },
                label = "artistsAlbumsScreenTransition"
            ) { (currentArtist, currentAlbum, currentTab) ->
                when {
                    currentArtist != null -> {
                        val artistTracks = remember(currentArtist, tracks) {
                            tracks.filter { currentArtist.trackIds.contains(it.id) || it.artist.equals(currentArtist.name, ignoreCase = true) }
                        }
                        ArtistDetailView(
                            artist = currentArtist,
                            artistPhotoUrl = artistPhotos[currentArtist.name],
                            tracks = artistTracks,
                            strings = strings,
                            currentTrackId = currentTrackId,
                            isPlaying = isPlaying,
                            favoriteIds = favoriteIds,
                            accentColor = accentColor,
                            onBack = { selectedArtist = null },
                            onPlayTrack = onPlayTrack,
                            onToggleFavorite = onToggleFavorite,
                            onMoreClick = onMoreClick,
                            bottomPadding = bottomPadding
                        )
                    }

                    currentAlbum != null -> {
                        val albumTracks = remember(currentAlbum, tracks) {
                            tracks.filter { currentAlbum.trackIds.contains(it.id) || (it.album == currentAlbum.title && it.artist == currentAlbum.artist) }
                        }
                        val albumArtCandidate = albumArt[currentAlbum.id]
                            ?: currentAlbum.artUri
                            ?: albumTracks.firstNotNullOfOrNull { it.artUri?.takeIf { u -> !u.startsWith("track_art://") } }
                        AlbumDetailView(
                            album = currentAlbum,
                            albumArtUrl = albumArtCandidate,
                            tracks = albumTracks,
                            strings = strings,
                            currentTrackId = currentTrackId,
                            isPlaying = isPlaying,
                            favoriteIds = favoriteIds,
                            accentColor = accentColor,
                            onBack = { selectedAlbum = null },
                            onPlayTrack = onPlayTrack,
                            onToggleFavorite = onToggleFavorite,
                            onMoreClick = onMoreClick,
                            bottomPadding = bottomPadding
                        )
                    }

                    currentTab == ArtistsAlbumsTab.ARTISTS -> {
                        if (filteredArtists.isEmpty()) {
                            EmptyListCard(
                                title = if (strings.isRtl) "لا يوجد فنانون" else "No artists found",
                                subtitle = if (strings.isRtl) "لم يتم العثور على أي فنان في المكتبة" else "No artists currently in library",
                                topPadding = headerContentTop,
                                accentColor = accentColor
                            )
                        } else {
                            LazyVerticalGrid(
                                state = artistsGridState,
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    top = headerContentTop,
                                    bottom = bottomPadding.calculateBottomPadding() + 16.dp,
                                    start = 14.dp,
                                    end = 14.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredArtists, key = { it.name }, contentType = { "artist_grid_card" }) { artist ->
                                    ArtistGridItem(
                                        artist = artist,
                                        artistPhotoUrl = artistPhotos[artist.name],
                                        strings = strings,
                                        accentColor = accentColor,
                                        onClick = { selectedArtist = artist }
                                    )
                                }
                            }
                        }
                    }

                    currentTab == ArtistsAlbumsTab.ALBUMS -> {
                        if (filteredAlbums.isEmpty()) {
                            EmptyListCard(
                                title = if (strings.isRtl) "لا توجد ألبومات" else "No albums found",
                                subtitle = if (strings.isRtl) "لم يتم العثور على أي ألبوم في المكتبة" else "No albums currently in library",
                                topPadding = headerContentTop,
                                accentColor = accentColor
                            )
                        } else {
                            val albumArtLookup = remember(filteredAlbums, tracks, albumArt) {
                                val fallbackByTrackId = HashMap<String, String>()
                                for (t in tracks) {
                                    val u = t.artUri
                                    if (!u.isNullOrBlank() && !u.startsWith("track_art://")) {
                                        fallbackByTrackId[t.id] = u
                                    }
                                }
                                filteredAlbums.associate { alb ->
                                    val resolved = albumArt[alb.id]
                                        ?: alb.artUri
                                        ?: alb.trackIds.firstNotNullOfOrNull { fallbackByTrackId[it] }
                                    alb.id to resolved
                                }
                            }
                            LazyVerticalGrid(
                                state = albumsGridState,
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    top = headerContentTop,
                                    bottom = bottomPadding.calculateBottomPadding() + 16.dp,
                                    start = 14.dp,
                                    end = 14.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredAlbums, key = { it.id }, contentType = { "album_grid_card" }) { album ->
                                    AlbumGridItem(
                                        album = album,
                                        albumArtUrl = albumArtLookup[album.id],
                                        strings = strings,
                                        accentColor = accentColor,
                                        onClick = { selectedAlbum = album }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedArtist == null && selectedAlbum == null) {
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
                                text = strings.navArtists,
                                color = Color.White,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = AppFontFamily
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${artists.size} ${strings.artistsTab} · ${albums.size} ${strings.albumsTab}",
                                color = NocturneColors.TextMuted,
                                fontSize = 12.sp,
                                fontFamily = AppFontFamily
                            )
                        }

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
                                .testTag("artists_search_toggle")
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
                    }

                    AnimatedVisibility(
                        visible = isSearchOpen,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
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
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(color = Color.White, fontSize = 13.sp, fontFamily = AppFontFamily),
                                cursorBrush = SolidColor(accentColor),
                                decorationBox = { inner ->
                                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = strings.searchPlaceholder,
                                                color = NocturneColors.TextSecondary,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
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

                    val activeTextColor = if (accentColor.luminance() > 0.45f) Color(0xFF141110) else Color.White
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isArtists = selectedTab == ArtistsAlbumsTab.ARTISTS
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isArtists) accentColor else Color.Transparent)
                                .testTag("artists_subtab")
                                .clickable { selectedTab = ArtistsAlbumsTab.ARTISTS },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${strings.artistsTab} (${artists.size})",
                                color = if (isArtists) activeTextColor else NocturneColors.TextSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = if (isArtists) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = AppFontFamily
                            )
                        }

                        val isAlbums = selectedTab == ArtistsAlbumsTab.ALBUMS
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isAlbums) accentColor else Color.Transparent)
                                .testTag("albums_subtab")
                                .clickable { selectedTab = ArtistsAlbumsTab.ALBUMS },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${strings.albumsTab} (${albums.size})",
                                color = if (isAlbums) activeTextColor else NocturneColors.TextSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = if (isAlbums) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = AppFontFamily
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistGridItem(
    artist: Artist,
    artistPhotoUrl: String?,
    strings: AppStrings,
    accentColor: Color,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val photo = artistPhotoUrl?.takeIf { it.isNotBlank() } ?: artist.artUri
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .matteCard(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(accentColor.copy(alpha = 0.35f), Color.White.copy(alpha = 0.12f))
                    )
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!photo.isNullOrBlank()) {
                AsyncImage(
                    model = remember(photo) {
                        ImageRequest.Builder(context)
                            .data(photo)
                            .size(480, 480)
                            .precision(Precision.INEXACT)
                            .memoryCacheKey("${photo}_480")
                            .diskCacheKey(photo)
                            .crossfade(false)
                            .build()
                    },
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val initial = artist.name.trim().take(1)
                if (initial.isNotBlank()) {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFontFamily
                    )
                } else {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = artist.name,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.5.sp,
            fontFamily = AppFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(3.dp))

        Text(
            text = if (strings.isRtl) "${artist.trackCount} أغنية · ${artist.albumCount} ألبوم"
            else "${artist.trackCount} songs · ${artist.albumCount} albums",
            color = NocturneColors.TextSecondary,
            fontSize = 11.sp,
            fontFamily = AppFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AlbumGridItem(
    album: Album,
    albumArtUrl: String?,
    strings: AppStrings,
    accentColor: Color,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val displayArt = albumArtUrl ?: album.artUri
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .matteCard(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(accentColor.copy(alpha = 0.30f), Color.White.copy(alpha = 0.10f))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (!displayArt.isNullOrBlank()) {
                AsyncImage(
                    model = remember(displayArt) {
                        ImageRequest.Builder(context)
                            .data(displayArt)
                            .size(512, 512)
                            .precision(Precision.INEXACT)
                            .memoryCacheKey("${displayArt}_512")
                            .diskCacheKey(displayArt)
                            .crossfade(false)
                            .build()
                    },
                    contentDescription = album.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Rounded.Album,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = album.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
            fontFamily = AppFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = album.artist,
            color = accentColor.copy(alpha = 0.90f),
            fontSize = 12.sp,
            fontFamily = AppFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = if (strings.isRtl) "${album.trackCount} أغنية" else "${album.trackCount} songs",
            color = NocturneColors.TextMuted,
            fontSize = 11.sp,
            fontFamily = AppFontFamily,
            maxLines = 1
        )
    }
}

@Composable
private fun ArtistDetailView(
    artist: Artist,
    artistPhotoUrl: String?,
    tracks: List<Track>,
    strings: AppStrings,
    currentTrackId: String?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    accentColor: Color,
    onBack: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onMoreClick: (Track) -> Unit,
    bottomPadding: PaddingValues
) {
    val context = LocalContext.current
    val photo = artistPhotoUrl?.takeIf { it.isNotBlank() } ?: artist.artUri
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
            bottom = bottomPadding.calculateBottomPadding() + 24.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = strings.backToPlaylists,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .matteCard(RoundedCornerShape(26.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(accentColor.copy(alpha = 0.4f), Color.White.copy(alpha = 0.12f))
                            )
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.22f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!photo.isNullOrBlank()) {
                        AsyncImage(
                            model = remember(photo) {
                                ImageRequest.Builder(context)
                                    .data(photo)
                                    .size(640, 640)
                                    .precision(Precision.INEXACT)
                                    .memoryCacheKey("${photo}_640")
                                    .diskCacheKey(photo)
                                    .crossfade(true)
                                    .build()
                            },
                            contentDescription = artist.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val initial = artist.name.trim().take(1)
                        Text(
                            text = initial,
                            color = Color.White,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = AppFontFamily
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = artist.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    fontFamily = AppFontFamily,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = if (strings.isRtl) "${tracks.size} أغنية · ${artist.albumCount} ألبوم"
                    else "${tracks.size} songs · ${artist.albumCount} albums",
                    color = NocturneColors.TextSecondary,
                    fontSize = 12.5.sp,
                    fontFamily = AppFontFamily
                )

                Spacer(Modifier.height(16.dp))

                val activeTextColor = if (accentColor.luminance() > 0.45f) Color(0xFF141110) else Color.White
                Button(
                    onClick = {
                        if (tracks.isNotEmpty()) {
                            onPlayTrack(tracks.first(), tracks)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(42.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = activeTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (strings.isRtl) "تشغيل الكل" else "Play All",
                        color = activeTextColor,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFontFamily
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (strings.isRtl) "الأغاني (${tracks.size})" else "Songs (${tracks.size})",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = AppFontFamily,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        items(tracks, key = { it.id }, contentType = { "track_row" }) { track ->
            TrackRowItem(
                track = track,
                isCurrent = track.id == currentTrackId,
                isPlaying = isPlaying,
                isFavorite = favoriteIds.contains(track.id),
                accentColor = accentColor,
                onClick = { onPlayTrack(track, tracks) },
                onToggleFavorite = { onToggleFavorite(track.id) },
                onMoreClick = { onMoreClick(track) },
                fallbackArtUri = photo
            )
        }
    }
}

@Composable
private fun AlbumDetailView(
    album: Album,
    albumArtUrl: String?,
    tracks: List<Track>,
    strings: AppStrings,
    currentTrackId: String?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    accentColor: Color,
    onBack: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onMoreClick: (Track) -> Unit,
    bottomPadding: PaddingValues
) {
    val context = LocalContext.current
    val bannerArt = albumArtUrl ?: album.artUri
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
            bottom = bottomPadding.calculateBottomPadding() + 24.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = strings.backToPlaylists,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .matteCard(RoundedCornerShape(26.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(accentColor.copy(alpha = 0.35f), Color.White.copy(alpha = 0.12f))
                            )
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!bannerArt.isNullOrBlank()) {
                        AsyncImage(
                            model = remember(bannerArt) {
                                ImageRequest.Builder(context)
                                    .data(bannerArt)
                                    .size(720, 720)
                                    .precision(Precision.INEXACT)
                                    .memoryCacheKey("${bannerArt}_720")
                                    .diskCacheKey(bannerArt)
                                    .crossfade(true)
                                    .build()
                            },
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Album,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = album.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontFamily = AppFontFamily,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = album.artist,
                    color = accentColor.copy(alpha = 0.90f),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = AppFontFamily,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = if (strings.isRtl) "${tracks.size} أغنية" else "${tracks.size} songs",
                    color = NocturneColors.TextMuted,
                    fontSize = 12.sp,
                    fontFamily = AppFontFamily
                )

                Spacer(Modifier.height(16.dp))

                val activeTextColor = if (accentColor.luminance() > 0.45f) Color(0xFF141110) else Color.White
                Button(
                    onClick = {
                        if (tracks.isNotEmpty()) {
                            onPlayTrack(tracks.first(), tracks)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(42.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = activeTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (strings.isRtl) "تشغيل الكل" else "Play All",
                        color = activeTextColor,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFontFamily
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (strings.isRtl) "أغاني الألبوم (${tracks.size})" else "Album Songs (${tracks.size})",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = AppFontFamily,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        items(tracks, key = { it.id }, contentType = { "track_row" }) { track ->
            TrackRowItem(
                track = track,
                isCurrent = track.id == currentTrackId,
                isPlaying = isPlaying,
                isFavorite = favoriteIds.contains(track.id),
                accentColor = accentColor,
                onClick = { onPlayTrack(track, tracks) },
                onToggleFavorite = { onToggleFavorite(track.id) },
                onMoreClick = { onMoreClick(track) },
                fallbackArtUri = bannerArt
            )
        }
    }
}

@Composable
private fun EmptyListCard(
    title: String,
    subtitle: String,
    topPadding: Dp,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .padding(top = topPadding, start = 16.dp, end = 16.dp)
            .fillMaxWidth()
            .matteCard(RoundedCornerShape(20.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.Album,
            null,
            tint = accentColor.copy(alpha = 0.7f),
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            fontFamily = AppFontFamily
        )
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            color = NocturneColors.TextSecondary,
            fontSize = 12.5.sp,
            textAlign = TextAlign.Center,
            fontFamily = AppFontFamily
        )
    }
}
