package com.example

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.ArtistNormalizer
import com.example.data.AudioRepository
import com.example.data.LyricsRepository
import com.example.data.OnlineMetadataResolver
import com.example.data.SettingsStore
import com.example.model.Album
import com.example.model.AppLanguage
import com.example.model.AppSettings
import com.example.model.Artist
import com.example.model.ArtworkBgMode
import com.example.model.ExclusionReason
import com.example.model.FolderItem
import com.example.model.LibrarySubTab
import com.example.model.LrcSearchResult
import com.example.model.LyricsData
import com.example.model.MainTab
import com.example.model.Playlist
import com.example.model.RepeatMode
import com.example.model.SavedSession
import com.example.model.SortOption
import com.example.model.StringsProvider
import com.example.model.Track
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val audioRepo = AudioRepository(application)
    val settingsStore = SettingsStore(application)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    val settings: StateFlow<AppSettings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val favoriteIds: StateFlow<Set<String>> = settingsStore.favoriteIdsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val playlists: StateFlow<List<Playlist>> = settingsStore.playlistsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val manualExcludedIds = settingsStore.manualExcludedIdsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val manualRestoredIds = settingsStore.manualRestoredIdsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val customTrackTitles: StateFlow<Map<String, String>> = settingsStore.customTrackTitlesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _allScannedTracks = MutableStateFlow<List<Track>>(emptyList())
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _artworkFetchProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val artworkFetchProgress: StateFlow<Pair<Int, Int>?> = _artworkFetchProgress.asStateFlow()

    private val classifiedTracks: StateFlow<List<Track>> = combine(
        _allScannedTracks,
        settings,
        manualExcludedIds,
        manualRestoredIds,
        customTrackTitles
    ) { all, cfg, exIds, resIds, customTitles ->
        all.map { track ->
            val effectiveTitle = customTitles[track.id] ?: track.title
            val reason = audioRepo.computeExclusionReason(
                id = track.id,
                title = effectiveTitle,
                fileName = track.fileName,
                folderPath = track.folderPath,
                durationSec = track.durationSec,
                settings = cfg,
                manualExcludedIds = exIds,
                manualRestoredIds = resIds
            )
            track.copy(
                title = effectiveTitle,
                isExcluded = reason != ExclusionReason.NONE,
                exclusionReason = reason
            )
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeTracks: StateFlow<List<Track>> = classifiedTracks
        .map { list -> list.filter { !it.isExcluded } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val excludedTracks: StateFlow<List<Track>> = classifiedTracks
        .map { list -> list.filter { it.isExcluded } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val artists: StateFlow<List<Artist>> = activeTracks.map { tracks ->
        val map = LinkedHashMap<String, MutableList<Track>>()
        for (t in tracks) {
            val name = ArtistNormalizer.normalizeArtist(t.artist)
            map.getOrPut(name) { mutableListOf() }.add(t)
        }
        map.map { (name, list) ->
            val albumCount = list.map { it.album }.distinct().size
            val artUri = list.firstNotNullOfOrNull { it.artUri?.takeIf { u -> !u.startsWith("track_art://") } }
                ?: list.firstOrNull()?.artUri
            Artist(
                name = name,
                trackCount = list.size,
                albumCount = albumCount,
                artUri = artUri,
                trackIds = list.map { it.id }
            )
        }.sortedWith(compareByDescending<Artist> { it.trackCount }.thenBy { it.name })
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _albumArt = MutableStateFlow<Map<String, String>>(emptyMap())
    val albumArt: StateFlow<Map<String, String>> = _albumArt.asStateFlow()

    val albums: StateFlow<List<Album>> = combine(activeTracks, _albumArt) { tracks, artMap ->
        val map = LinkedHashMap<String, MutableList<Track>>()
        for (t in tracks) {
            val normArtist = ArtistNormalizer.normalizeArtist(t.artist)
            val albTrimmed = t.album.trim()
            val groupKey = "${albTrimmed.lowercase()}:::${normArtist.lowercase()}"
            map.getOrPut(groupKey) { mutableListOf() }.add(t)
        }
        map.mapNotNull { (key, list) ->
            val first = list.first()
            val normArtist = ArtistNormalizer.normalizeArtist(first.artist)
            if (ArtistNormalizer.isFakeOrJunkAlbum(
                    album = first.album,
                    artist = normArtist,
                    trackCount = list.size,
                    trackTitle = first.title,
                    fileName = first.fileName
                )) {
                return@mapNotNull null
            }
            val safeKey = "${normArtist}_${first.album.trim()}".replace(Regex("[^a-zA-Z0-9_\\-\\u0600-\\u06FF]"), "_")
            val diskAlbumArt = File(getApplication<Application>().filesDir, "track_art_v2/album_${safeKey}.jpg")
            val diskArtUri = if (diskAlbumArt.exists() && diskAlbumArt.length() > 0L) Uri.fromFile(diskAlbumArt).toString() else null

            val artUri = diskArtUri
                ?: list.firstNotNullOfOrNull { it.artUri?.takeIf { u -> !u.startsWith("track_art://") } }
                ?: artMap[key]
                ?: first.artUri
            Album(
                id = key,
                title = first.album.trim(),
                artist = normArtist,
                trackCount = list.size,
                artUri = artUri,
                trackIds = list.map { it.id }
            )
        }.sortedWith(compareByDescending<Album> { it.trackCount }.thenBy { it.title })
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val folderItems: StateFlow<List<FolderItem>> = combine(
        _allScannedTracks,
        settings
    ) { all, cfg ->
        audioRepo.getFolderItems(all, cfg.excludedFolderPaths)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _artistPhotos = MutableStateFlow<Map<String, String>>(emptyMap())
    val artistPhotos: StateFlow<Map<String, String>> = _artistPhotos.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _accentColor = MutableStateFlow(0xFFF5F5F5.toInt())
    val accentColor: StateFlow<Int> = _accentColor.asStateFlow()

    private val _secondaryColor = MutableStateFlow(0xFF64748B.toInt())
    val secondaryColor: StateFlow<Int> = _secondaryColor.asStateFlow()

    private val _currentLyrics = MutableStateFlow<LyricsData?>(null)
    val currentLyrics: StateFlow<LyricsData?> = _currentLyrics.asStateFlow()

    private val _isFetchingLyrics = MutableStateFlow(false)
    val isFetchingLyrics: StateFlow<Boolean> = _isFetchingLyrics.asStateFlow()

    private val _manualSearchResults = MutableStateFlow<List<LrcSearchResult>>(emptyList())
    val manualSearchResults: StateFlow<List<LrcSearchResult>> = _manualSearchResults.asStateFlow()

    private val _isSearchingManualLyrics = MutableStateFlow(false)
    val isSearchingManualLyrics: StateFlow<Boolean> = _isSearchingManualLyrics.asStateFlow()

    private val _sleepTimerSeconds = MutableStateFlow<Int?>(null)
    val sleepTimerSeconds: StateFlow<Int?> = _sleepTimerSeconds.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _bgArtUri = MutableStateFlow<String?>(null)
    val bgArtUri: StateFlow<String?> = _bgArtUri.asStateFlow()

    private val _isCrossfading = MutableStateFlow(false)
    val isCrossfading: StateFlow<Boolean> = _isCrossfading.asStateFlow()

    private val _crossfadeProgress = MutableStateFlow(0f)
    val crossfadeProgress: StateFlow<Float> = _crossfadeProgress.asStateFlow()

    private val _isSessionReady = MutableStateFlow(false)
    val isSessionReady: StateFlow<Boolean> = _isSessionReady.asStateFlow()

    private val _initialSession = MutableStateFlow(SavedSession())
    val initialSession: StateFlow<SavedSession> = _initialSession.asStateFlow()

    private var savedSessionSnapshot: SavedSession = SavedSession()
    @Volatile
    private var hasLoadedInitialCache = false
    @Volatile
    private var hasAttemptedColdRestore = false
    @Volatile
    private var pendingStartupFullPlayerSnap = false
    private var lastSavedPosMs = -1L

    private var sleepJob: Job? = null
    private var fadeJob: Job? = null
    private var fadeOutJob: Job? = null
    private var visualsJob: Job? = null
    private var fadeOutPlayer: ExoPlayer? = null
    private var crossfadeTriggeredForTrackId: String? = null
    private var lastFetchedLyricsTrackId: String? = null
    @Volatile
    private var isFullPlayerVisible: Boolean = false

    fun setFullPlayerVisible(visible: Boolean) {
        isFullPlayerVisible = visible
    }

    fun consumedStartupFullPlayerSnap(): Boolean {
        val snap = pendingStartupFullPlayerSnap
        pendingStartupFullPlayerSnap = false
        return snap
    }

    fun updateMainTab(tab: MainTab) {
        _initialSession.value = _initialSession.value.copy(lastMainTab = tab)
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.saveUiSession(mainTab = tab)
        }
    }

    fun updateLibrarySubTab(subTab: LibrarySubTab) {
        _initialSession.value = _initialSession.value.copy(lastLibrarySubTab = subTab)
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.saveUiSession(librarySubTab = subTab)
        }
    }

    fun updateSortOption(sortOption: SortOption) {
        _initialSession.value = _initialSession.value.copy(lastSortOption = sortOption)
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.saveUiSession(sortOption = sortOption)
        }
    }

    fun updateFullPlayerOpen(open: Boolean) {
        isFullPlayerVisible = open
        _initialSession.value = _initialSession.value.copy(lastShowFullPlayer = open)
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.saveUiSession(showFullPlayer = open)
        }
    }

    fun updateFullPlayerPage(page: Int) {
        val clamped = page.coerceIn(0, 2)
        _initialSession.value = _initialSession.value.copy(lastFullPlayerPage = clamped)
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.saveUiSession(fullPlayerPage = clamped)
        }
    }

    fun updateLibraryScroll(index: Int, offset: Int) {
        _initialSession.value = _initialSession.value.copy(
            lastScrollIndex = index.coerceAtLeast(0),
            lastScrollOffset = offset.coerceAtLeast(0)
        )
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.saveUiSession(scrollIndex = index, scrollOffset = offset)
        }
    }

    init {
        viewModelScope.launch {
            try {
                val session = withContext(Dispatchers.IO) { settingsStore.getSavedSession() }
                savedSessionSnapshot = session
                _initialSession.value = session
                pendingStartupFullPlayerSnap = session.lastShowFullPlayer && !session.lastTrackId.isNullOrBlank()
                _shuffleEnabled.value = session.shuffleEnabled
                _repeatMode.value = session.repeatMode

                val cached = withContext(Dispatchers.IO) { audioRepo.loadCachedTracks() }
                if (cached.isNotEmpty() && _allScannedTracks.value.isEmpty()) {
                    _allScannedTracks.value = cached
                }
                // Warm custom track titles before restoring playback
                withContext(Dispatchers.IO) { settingsStore.customTrackTitlesFlow.first() }
                hasLoadedInitialCache = true
                maybeRestoreLastPlayback()
            } finally {
                _isSessionReady.value = true
            }
        }
        connectController()
        startProgressLoop()
        startArtistPhotoResolver()
        startAlbumArtResolver()
        startBackgroundTrackArtResolver()
    }

    private fun startArtistPhotoResolver() {
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>()
            val artCacheDir = File(ctx.filesDir, "track_art_v2")
            val attempted = HashSet<String>()
            var networkDelayDone = false
            artists.collect { list ->
                if (list.isEmpty()) return@collect
                val map = _artistPhotos.value.toMutableMap()
                // Pass 1: Instantly attach any already-cached disk photos in a single emit
                var diskHits = 0
                val toFetchOnline = mutableListOf<Artist>()
                for (a in list) {
                    if (a.name.isBlank() || a.name == "Unknown Artist") continue
                    if (map.containsKey(a.name)) continue
                    val safeName = a.name.replace(Regex("[^a-zA-Z0-9_\\-\\u0600-\\u06FF]"), "_")
                    val cachedPhoto = File(artCacheDir, "artist_${safeName}.jpg")
                    if (cachedPhoto.exists() && cachedPhoto.length() > 0L) {
                        map[a.name] = Uri.fromFile(cachedPhoto).toString()
                        attempted.add(a.name)
                        diskHits++
                    } else if (!attempted.contains(a.name)) {
                        toFetchOnline.add(a)
                    }
                }
                if (diskHits > 0) {
                    _artistPhotos.value = LinkedHashMap(map)
                }
                if (toFetchOnline.isEmpty()) return@collect
                if (!networkDelayDone) {
                    delay(4500L)
                    networkDelayDone = true
                }
                var pendingCount = 0
                for (a in toFetchOnline) {
                    if (!attempted.add(a.name)) continue
                    val photoUri = OnlineMetadataResolver.resolveArtistPhoto(ctx, a.name)
                    if (!photoUri.isNullOrBlank()) {
                        map[a.name] = photoUri
                        pendingCount++
                        if (pendingCount >= 10) {
                            _artistPhotos.value = LinkedHashMap(map)
                            pendingCount = 0
                        }
                    }
                    delay(80L)
                }
                if (pendingCount > 0) {
                    _artistPhotos.value = LinkedHashMap(map)
                }
            }
        }
    }

    private fun startAlbumArtResolver() {
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>()
            val artCacheDir = File(ctx.filesDir, "track_art_v2")
            val attempted = HashSet<String>()
            var networkDelayDone = false
            activeTracks.collect { currentTracks ->
                if (currentTracks.isEmpty()) return@collect
                val grouped = LinkedHashMap<String, MutableList<Track>>()
                for (t in currentTracks) {
                    if (t.isExcluded) continue
                    val normArtist = ArtistNormalizer.normalizeArtist(t.artist)
                    val key = "${t.album.trim().lowercase()}:::${normArtist.lowercase()}"
                    grouped.getOrPut(key) { mutableListOf() }.add(t)
                }

                val map = _albumArt.value.toMutableMap()
                var diskHits = 0
                val toFetchOnline = mutableListOf<Triple<String, String, String>>()
                for ((groupKey, list) in grouped) {
                    val first = list.first()
                    val normArtist = ArtistNormalizer.normalizeArtist(first.artist)
                    if (ArtistNormalizer.isFakeOrJunkAlbum(first.album, normArtist, list.size, first.title, first.fileName)) {
                        continue
                    }
                    if (map.containsKey(groupKey) && !map[groupKey].isNullOrBlank()) continue
                    val safeKey = "${normArtist}_${first.album.trim()}".replace(Regex("[^a-zA-Z0-9_\\-\\u0600-\\u06FF]"), "_")
                    val diskFile = File(artCacheDir, "album_${safeKey}.jpg")
                    if (diskFile.exists() && diskFile.length() > 0L) {
                        map[groupKey] = Uri.fromFile(diskFile).toString()
                        attempted.add(groupKey)
                        diskHits++
                    } else if (!attempted.contains(groupKey)) {
                        toFetchOnline.add(Triple(groupKey, first.album.trim(), normArtist))
                    }
                }
                if (diskHits > 0) {
                    _albumArt.value = LinkedHashMap(map)
                }
                if (toFetchOnline.isEmpty()) return@collect
                if (!networkDelayDone) {
                    delay(5000L)
                    networkDelayDone = true
                }
                var pendingCount = 0
                for ((groupKey, albumTitle, normArtist) in toFetchOnline) {
                    if (!attempted.add(groupKey)) continue
                    val coverUri = OnlineMetadataResolver.resolveAlbumArtOnline(ctx, albumTitle, normArtist)
                    if (!coverUri.isNullOrBlank()) {
                        map[groupKey] = coverUri
                        pendingCount++
                        if (pendingCount >= 8) {
                            _albumArt.value = LinkedHashMap(map)
                            pendingCount = 0
                        }
                    }
                    delay(90L)
                }
                if (pendingCount > 0) {
                    _albumArt.value = LinkedHashMap(map)
                }
            }
        }
    }

    private fun startBackgroundTrackArtResolver() {
        viewModelScope.launch(Dispatchers.IO) {
            val checkedIds = HashSet<String>()
            val ctx = getApplication<Application>()
            val artCacheDir = File(ctx.filesDir, "track_art_v2").apply { mkdirs() }

            _allScannedTracks.collect { currentTracks ->
                if (currentTracks.isEmpty()) return@collect
                val hasUnresolved = currentTracks.any { t ->
                    !t.isExcluded &&
                        (t.artUri.isNullOrBlank() || t.artUri.startsWith("track_art://")) &&
                        !checkedIds.contains(t.id)
                }
                if (!hasUnresolved) return@collect
                delay(3200L)

                val latestTracks = _allScannedTracks.value
                val updated = latestTracks.toMutableList()
                var hasChanges = false

                for (i in updated.indices) {
                    val t = updated[i]
                    if (t.isExcluded) continue
                    // Never overwrite tracks that already have their own artwork URI
                    if (!t.artUri.isNullOrBlank() && !t.artUri.startsWith("track_art://")) continue
                    if (!checkedIds.add(t.id)) continue

                    val cachedFile = File(artCacheDir, "track_${t.id}.jpg")
                    if (cachedFile.exists() && cachedFile.length() > 0L) {
                        updated[i] = t.copy(artUri = Uri.fromFile(cachedFile).toString())
                        hasChanges = true
                        continue
                    }

                    // 1. Check the audio file's embedded art FIRST before any fallback
                    val fullPath = File(t.folderPath, t.fileName).absolutePath
                    val embeddedFile = AudioRepository.extractOrGetTrackArtFile(
                        context = ctx,
                        trackId = t.id,
                        contentUri = t.contentUri,
                        filePath = fullPath
                    )
                    if (embeddedFile != null && embeddedFile.exists() && embeddedFile.length() > 0L) {
                        updated[i] = t.copy(artUri = Uri.fromFile(embeddedFile).toString())
                        hasChanges = true
                        delay(20L)
                        continue
                    }

                    // 2. Only if the track truly has NO embedded art (.none confirmed), check album or online art
                    val normArtist = ArtistNormalizer.normalizeArtist(t.artist)
                    val groupKey = "${t.album.trim().lowercase()}:::${normArtist.lowercase()}"
                    val isValidAlbum = !ArtistNormalizer.isFakeOrJunkAlbum(t.album, normArtist, 2, t.title, t.fileName)
                    val safeKey = "${normArtist}_${t.album.trim()}".replace(Regex("[^a-zA-Z0-9_\\-\\u0600-\\u06FF]"), "_")
                    val diskAlbumCover = File(artCacheDir, "album_${safeKey}.jpg")
                    val albumCover = if (isValidAlbum) {
                        _albumArt.value[groupKey]
                            ?: (if (diskAlbumCover.exists() && diskAlbumCover.length() > 0L) Uri.fromFile(diskAlbumCover).toString() else null)
                    } else null

                    if (!albumCover.isNullOrBlank()) {
                        updated[i] = t.copy(artUri = albumCover)
                        hasChanges = true
                    } else if (t.title.isNotBlank() && !File(artCacheDir, "track_${t.id}.online_miss").exists()) {
                        val fetched = OnlineMetadataResolver.resolveTrackArtOnline(
                            context = ctx,
                            trackId = t.id,
                            title = t.title,
                            artist = t.artist,
                            targetFile = cachedFile
                        )
                        if (fetched && cachedFile.exists() && cachedFile.length() > 0L) {
                            File(artCacheDir, "track_${t.id}.none").delete()
                            updated[i] = t.copy(artUri = Uri.fromFile(cachedFile).toString())
                            hasChanges = true
                        }
                        delay(35L)
                    }
                }

                if (hasChanges) {
                    _allScannedTracks.value = ArrayList(updated)
                    audioRepo.saveTracksToCache(updated)
                }
            }
        }
    }

    private fun connectController() {
        val ctx = getApplication<Application>()
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        controllerFuture = future
        future.addListener({
            val ctrl = runCatching { future.get() }.getOrNull() ?: return@addListener
            mediaController = ctrl
            ctrl.repeatMode = when (_repeatMode.value) {
                RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            }
            ctrl.shuffleModeEnabled = _shuffleEnabled.value
            if (ctrl.currentMediaItem != null) {
                hasAttemptedColdRestore = true
                pendingStartupFullPlayerSnap = false
                syncFromPlayer(ctrl)
            } else {
                maybeRestoreLastPlayback()
            }
            ctrl.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (!isPlaying && _currentTrack.value != null) {
                        persistSessionNow()
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    crossfadeTriggeredForTrackId = null
                    syncFromPlayer(ctrl)
                    if (_currentTrack.value != null) {
                        persistSessionNow()
                    }
                    if (settings.value.crossfadeEnabled) {
                        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && fadeJob?.isActive != true) {
                            performFadeIn(ctrl, settings.value.crossfadeSec)
                        }
                    } else if (fadeJob?.isActive != true) {
                        ctrl.volume = 1f
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _durationMs.value = ctrl.duration.coerceAtLeast(0L)
                    }
                }
            })
        }, ContextCompat.getMainExecutor(ctx))
    }

    private fun startProgressLoop() {
        viewModelScope.launch {
            while (true) {
                val ctrl = mediaController
                var playing = false
                if (ctrl != null) {
                    val pos = ctrl.currentPosition.coerceAtLeast(0L)
                    val dur = ctrl.duration.coerceAtLeast(0L)
                    playing = ctrl.isPlaying
                    if (_positionMs.value != pos && (playing || pos > 0L || ctrl.currentMediaItem == null)) {
                        _positionMs.value = pos
                    }
                    if (dur > 0L && _durationMs.value != dur) _durationMs.value = dur
                    if (_isPlaying.value != playing) _isPlaying.value = playing

                    val activeTrack = _currentTrack.value
                    if (playing && activeTrack != null && abs(pos - lastSavedPosMs) >= 4000L) {
                        lastSavedPosMs = pos
                        viewModelScope.launch(Dispatchers.IO) {
                            settingsStore.savePositionOnly(pos)
                        }
                    }
                    val fadeSec = settings.value.crossfadeSec.coerceIn(1, 12)
                    val fadeMs = fadeSec * 1000L
                    if (
                        settings.value.crossfadeEnabled &&
                        playing &&
                        activeTrack != null &&
                        _repeatMode.value != RepeatMode.ONE &&
                        dur > fadeMs + 1500L &&
                        ctrl.hasNextMediaItem()
                    ) {
                        val remaining = dur - pos
                        if (remaining in 250L..fadeMs && crossfadeTriggeredForTrackId != activeTrack.id) {
                            crossfadeTriggeredForTrackId = activeTrack.id
                            triggerCrossfade(ctrl, activeTrack, pos, fadeSec) {
                                ctrl.seekToNextMediaItem()
                            }
                        }
                    } else if (fadeJob?.isActive != true) {
                        if (ctrl.volume < 1f) ctrl.volume = 1f
                    }
                }
                delay(
                    when {
                        !playing -> 600L
                        isFullPlayerVisible || _isCrossfading.value -> 200L
                        else -> 500L
                    }
                )
            }
        }
    }

    private fun getOrCreateAuxPlayer(): ExoPlayer {
        fadeOutPlayer?.let { return it }
        val ctx = getApplication<Application>()
        val attrs = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
        return ExoPlayer.Builder(ctx)
            .setAudioAttributes(attrs, false)
            .build()
            .also { fadeOutPlayer = it }
    }

    private fun triggerCrossfade(
        ctrl: MediaController,
        oldTrack: Track,
        oldPosMs: Long,
        sec: Int,
        switchAction: () -> Unit
    ) {
        val currentVol = ctrl.volume.coerceIn(0.05f, 1f)
        startFadeOutOverlay(oldTrack, oldPosMs, sec, currentVol)
        performFadeIn(ctrl, sec)
        switchAction()
    }

    private fun startFadeOutOverlay(oldTrack: Track, oldPosMs: Long, sec: Int, startVol: Float) {
        fadeOutJob?.cancel()
        if (oldTrack.contentUri.isBlank()) return

        val aux = getOrCreateAuxPlayer()
        aux.stop()
        aux.setMediaItem(MediaItem.fromUri(oldTrack.contentUri), oldPosMs.coerceAtLeast(0L))
        val initVol = startVol.coerceIn(0.05f, 1f)
        aux.volume = initVol
        aux.prepare()
        aux.play()

        val durationMs = sec.coerceIn(1, 12) * 1000L
        fadeOutJob = viewModelScope.launch {
            var waitMs = 0L
            while (aux.playbackState == Player.STATE_BUFFERING && waitMs < 400L) {
                delay(25L)
                waitMs += 25L
            }
            val startTime = SystemClock.elapsedRealtime()
            while (true) {
                val elapsed = SystemClock.elapsedRealtime() - startTime
                val t = (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                val curve = cos(t * (PI / 2.0)).toFloat().coerceIn(0f, 1f)
                aux.volume = (initVol * curve).coerceIn(0f, 1f)
                if (t >= 1f) break
                delay(40L)
            }
            aux.pause()
            aux.clearMediaItems()
        }
    }

    private fun performFadeIn(ctrl: MediaController, sec: Int) {
        fadeJob?.cancel()
        ctrl.volume = 0.02f
        val durationMs = sec.coerceIn(1, 12) * 1000L
        fadeJob = viewModelScope.launch {
            _isCrossfading.value = true
            _crossfadeProgress.value = 0f
            val startTime = SystemClock.elapsedRealtime()
            try {
                while (true) {
                    val elapsed = SystemClock.elapsedRealtime() - startTime
                    val t = (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    _crossfadeProgress.value = t
                    val curve = sin(t * (PI / 2.0)).toFloat().coerceIn(0.02f, 1f)
                    ctrl.volume = curve
                    if (t >= 1f) break
                    delay(40L)
                }
                ctrl.volume = 1f
            } finally {
                _isCrossfading.value = false
                _crossfadeProgress.value = 0f
            }
        }
    }

    private fun syncFromPlayer(ctrl: MediaController) {
        val currentId = ctrl.currentMediaItem?.mediaId ?: return
        val match = classifiedTracks.value.find { it.id == currentId }
            ?: _allScannedTracks.value.find { it.id == currentId }
            ?: _queue.value.find { it.id == currentId }
        if (match != null && _currentTrack.value?.id != match.id) {
            setActiveTrackVisuals(match)
        }
        val dur = ctrl.duration.coerceAtLeast(0L)
        if (dur > 0L) _durationMs.value = dur
        val pos = ctrl.currentPosition.coerceAtLeast(0L)
        if (pos > 0L || ctrl.isPlaying) _positionMs.value = pos
        _isPlaying.value = ctrl.isPlaying
    }

    private fun maybeRestoreLastPlayback() {
        if (!hasLoadedInitialCache || hasAttemptedColdRestore) return
        val ctrl = mediaController ?: return
        if (ctrl.currentMediaItem != null) {
            hasAttemptedColdRestore = true
            pendingStartupFullPlayerSnap = false
            syncFromPlayer(ctrl)
            return
        }
        val session = savedSessionSnapshot
        val targetId = session.lastTrackId
        if (targetId.isNullOrBlank()) {
            hasAttemptedColdRestore = true
            pendingStartupFullPlayerSnap = false
            return
        }
        val rawTracks = _allScannedTracks.value
        if (rawTracks.isEmpty()) return
        hasAttemptedColdRestore = true

        val customTitles = customTrackTitles.value
        val titlesApplied = rawTracks.map { t ->
            val ct = customTitles[t.id]
            if (ct != null) t.copy(title = ct) else t
        }
        val target = titlesApplied.find { it.id == targetId }
        if (target == null) {
            pendingStartupFullPlayerSnap = false
            return
        }

        val byId = titlesApplied.associateBy { it.id }
        val fromSavedQueue = session.lastQueueIds.mapNotNull { byId[it] }
        val restoredQueue = when {
            fromSavedQueue.any { it.id == target.id } -> fromSavedQueue
            fromSavedQueue.isNotEmpty() -> listOf(target) + fromSavedQueue
            else -> activeTracks.value.takeIf { q -> q.any { it.id == target.id } } ?: listOf(target)
        }
        val startIndex = restoredQueue.indexOfFirst { it.id == target.id }.coerceAtLeast(0)
        val savedPos = session.lastPositionMs.coerceAtLeast(0L)

        _queue.value = restoredQueue
        _positionMs.value = savedPos
        _durationMs.value = (target.durationSec * 1000.0).toLong().coerceAtLeast(savedPos)
        lastSavedPosMs = savedPos
        setActiveTrackVisuals(target)

        ctrl.repeatMode = when (_repeatMode.value) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        ctrl.shuffleModeEnabled = _shuffleEnabled.value
        val items = restoredQueue.map { toMediaItem(it) }
        ctrl.setMediaItems(items, startIndex, savedPos)
        ctrl.prepare()
    }

    fun persistSessionNow() {
        val track = _currentTrack.value
        val pos = mediaController?.currentPosition?.coerceAtLeast(0L) ?: _positionMs.value
        val qIds = _queue.value.map { it.id }
        val shuf = _shuffleEnabled.value
        val rep = _repeatMode.value
        if (track != null) lastSavedPosMs = pos
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.savePlaybackSession(
                trackId = track?.id,
                positionMs = pos,
                queueIds = qIds,
                shuffleEnabled = shuf,
                repeatMode = rep
            )
        }
    }

    private fun setActiveTrackVisuals(track: Track) {
        val immediateArt = track.artUri?.takeIf { !it.startsWith("track_art://") }
        // Never inject the previous track's artwork onto the newly active track
        _currentTrack.value = track
        if (immediateArt != null) {
            _bgArtUri.value = immediateArt
        }
        visualsJob?.cancel()
        visualsJob = viewModelScope.launch {
            val (hdUri, accent, secondary) = audioRepo.loadHdArtworkAndColors(track)
            if (_currentTrack.value?.id != track.id) return@launch
            val updated = track.copy(
                artUri = hdUri ?: track.artUri,
                accentColor = accent,
                secondaryColor = secondary
            )
            _bgArtUri.value = hdUri
            _currentTrack.value = updated
            if (settings.value.dynamicAccent) {
                _accentColor.value = accent
                _secondaryColor.value = secondary
            } else {
                _accentColor.value = 0xFFF5F5F5.toInt()
                _secondaryColor.value = 0xFF1C1C22.toInt()
            }
            if (lastFetchedLyricsTrackId != track.id) {
                fetchLyricsForTrack(updated)
            }
            val artBytes = audioRepo.loadNotificationArtworkBytes(updated.artUri)
            if (artBytes != null && _currentTrack.value?.id == track.id) {
                val ctrl = mediaController
                if (ctrl != null) {
                    val idx = ctrl.currentMediaItemIndex
                    val curItem = ctrl.currentMediaItem
                    if (idx >= 0 && curItem != null && curItem.mediaId == updated.id && curItem.mediaMetadata.artworkData == null) {
                        val updatedMetadata = curItem.mediaMetadata.buildUpon()
                            .setTitle(updated.title)
                            .setDisplayTitle(updated.title)
                            .setArtist(updated.artist)
                            .setAlbumTitle(updated.album)
                            .setArtworkData(artBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                            .build()
                        val updatedItem = curItem.buildUpon()
                            .setMediaMetadata(updatedMetadata)
                            .build()
                        ctrl.replaceMediaItem(idx, updatedItem)
                    }
                }
            }
        }
    }

    fun syncLibraryOnStartup() {
        if (_isScanning.value) return
        viewModelScope.launch {
            while (!hasLoadedInitialCache) delay(15L)
            if (_allScannedTracks.value.isEmpty()) {
                scanLibrary()
                return@launch
            }
            // Cached tracks already displayed cleanly; run quiet diff check in background without forcing UI rebuild
            delay(1800L)
            scanLibrary(silentIfUnchanged = true)
        }
    }

    fun scanLibrary(silentIfUnchanged: Boolean = false) {
        if (_isScanning.value) return
        viewModelScope.launch {
            if (!silentIfUnchanged) _isScanning.value = true
            try {
                val prev = _allScannedTracks.value
                val existingById = prev.associateBy { it.id }
                val scanned = audioRepo.scanDeviceTracks(
                    settings = settings.value,
                    manualExcludedIds = manualExcludedIds.value,
                    manualRestoredIds = manualRestoredIds.value,
                    existingTracksById = existingById
                )
                val unchanged = prev.size == scanned.size && prev.indices.all { i ->
                    prev[i].id == scanned[i].id &&
                        prev[i].artUri == scanned[i].artUri &&
                        prev[i].title == scanned[i].title &&
                        prev[i].artist == scanned[i].artist
                }
                if (!unchanged) {
                    _allScannedTracks.value = scanned
                }
                maybeRestoreLastPlayback()
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun playTrack(track: Track, contextQueue: List<Track> = activeTracks.value) {
        val ctrl = mediaController
        val prevTrack = _currentTrack.value
        val prevPos = ctrl?.currentPosition ?: 0L
        val shouldCrossfade = settings.value.crossfadeEnabled &&
            ctrl != null &&
            ctrl.isPlaying &&
            prevTrack != null &&
            prevTrack.id != track.id

        val list = if (contextQueue.any { it.id == track.id }) contextQueue else listOf(track) + contextQueue
        _queue.value = list
        val startIndex = list.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        setActiveTrackVisuals(track)

        if (ctrl == null) {
            persistSessionNow()
            return
        }
        if (shouldCrossfade) {
            val fadeSec = settings.value.crossfadeSec.coerceIn(1, 12)
            startFadeOutOverlay(prevTrack, prevPos, fadeSec, ctrl.volume)
            ctrl.volume = 0.05f
        } else {
            fadeJob?.cancel()
            _isCrossfading.value = false
            _crossfadeProgress.value = 0f
            ctrl.volume = 1f
        }
        val currentQ = _queue.value
        val isSameQueue = currentQ.size == list.size &&
            currentQ.firstOrNull()?.id == list.firstOrNull()?.id &&
            currentQ.lastOrNull()?.id == list.lastOrNull()?.id

        if (isSameQueue && ctrl.mediaItemCount == list.size) {
            ctrl.seekTo(startIndex, 0L)
            ctrl.prepare()
            ctrl.play()
        } else {
            val items = list.map { toMediaItem(it) }
            ctrl.setMediaItems(items, startIndex, 0L)
            ctrl.prepare()
            ctrl.play()
        }
        if (shouldCrossfade) {
            performFadeIn(ctrl, settings.value.crossfadeSec)
        }
        _positionMs.value = 0L
        lastSavedPosMs = 0L
        persistSessionNow()
    }

    fun playAll(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        playTrack(tracks.first(), tracks)
    }

    fun togglePlayPause() {
        val ctrl = mediaController ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun seekToNext() {
        val ctrl = mediaController ?: return
        val prevTrack = _currentTrack.value
        if (settings.value.crossfadeEnabled && ctrl.isPlaying && prevTrack != null && ctrl.hasNextMediaItem()) {
            val fadeSec = settings.value.crossfadeSec.coerceIn(1, 12)
            triggerCrossfade(ctrl, prevTrack, ctrl.currentPosition, fadeSec) {
                ctrl.seekToNextMediaItem()
            }
        } else {
            ctrl.seekToNextMediaItem()
        }
    }

    fun seekToPrevious() {
        val ctrl = mediaController ?: return
        if (ctrl.currentPosition > 3000L) {
            ctrl.seekTo(0L)
        } else {
            val prevTrack = _currentTrack.value
            if (settings.value.crossfadeEnabled && ctrl.isPlaying && prevTrack != null && ctrl.hasPreviousMediaItem()) {
                val fadeSec = settings.value.crossfadeSec.coerceIn(1, 12)
                triggerCrossfade(ctrl, prevTrack, ctrl.currentPosition, fadeSec) {
                    ctrl.seekToPreviousMediaItem()
                }
            } else {
                ctrl.seekToPreviousMediaItem()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceAtLeast(0L)
        mediaController?.seekTo(clamped)
        _positionMs.value = clamped
        lastSavedPosMs = clamped
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.savePositionOnly(clamped)
        }
    }

    fun stopAndClearPlayer() {
        fadeJob?.cancel()
        fadeOutJob?.cancel()
        runCatching { fadeOutPlayer?.pause() }
        mediaController?.stop()
        mediaController?.clearMediaItems()
        _currentTrack.value = null
        _bgArtUri.value = null
        _isCrossfading.value = false
        _crossfadeProgress.value = 0f
        _isPlaying.value = false
        _positionMs.value = 0L
        _durationMs.value = 0L
        _queue.value = emptyList()
        lastSavedPosMs = 0L
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.clearPlaybackSession()
        }
    }

    fun toggleShuffle() {
        val next = !_shuffleEnabled.value
        _shuffleEnabled.value = next
        mediaController?.shuffleModeEnabled = next
        persistSessionNow()
    }

    fun cycleRepeatMode() {
        val next = when (_repeatMode.value) {
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
            RepeatMode.OFF -> RepeatMode.ALL
        }
        _repeatMode.value = next
        mediaController?.repeatMode = when (next) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        persistSessionNow()
    }

    fun playNext(track: Track) {
        val s = StringsProvider.forLang(settings.value.language)
        val ctrl = mediaController
        if (ctrl == null || _currentTrack.value == null) {
            playTrack(track)
            return
        }
        val currentIdx = ctrl.currentMediaItemIndex
        val insertIdx = (currentIdx + 1).coerceAtMost(ctrl.mediaItemCount)
        ctrl.addMediaItem(insertIdx, toMediaItem(track))
        val mutable = _queue.value.toMutableList()
        val qIdx = (currentIdx + 1).coerceAtMost(mutable.size)
        mutable.add(qIdx, track)
        _queue.value = mutable
        persistSessionNow()
        showToast(s.toastPlayNext)
    }

    fun addToQueue(track: Track) {
        val s = StringsProvider.forLang(settings.value.language)
        val ctrl = mediaController
        if (ctrl == null || _currentTrack.value == null) {
            playTrack(track)
            return
        }
        ctrl.addMediaItem(toMediaItem(track))
        _queue.value = _queue.value + track
        persistSessionNow()
        showToast(s.toastAddedQueue)
    }

    fun clearQueue() {
        val ctrl = mediaController ?: return
        val current = _currentTrack.value ?: return
        val pos = ctrl.currentPosition
        val wasPlaying = ctrl.isPlaying
        ctrl.setMediaItem(toMediaItem(current), pos)
        ctrl.prepare()
        if (wasPlaying) ctrl.play()
        _queue.value = listOf(current)
        persistSessionNow()
    }

    fun toggleFavorite(trackId: String) {
        viewModelScope.launch {
            settingsStore.toggleFavorite(trackId)
        }
    }

    fun excludeTrack(track: Track) {
        val s = StringsProvider.forLang(settings.value.language)
        viewModelScope.launch {
            settingsStore.setManualExclusion(track.id, excluded = true)
            showToast(s.toastExcluded)
        }
    }

    fun restoreTrack(track: Track) {
        val s = StringsProvider.forLang(settings.value.language)
        viewModelScope.launch {
            settingsStore.setManualExclusion(track.id, excluded = false)
            showToast(s.toastRestored)
        }
    }

    fun renameTrack(track: Track, newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isBlank() || clean == track.title) return
        val s = StringsProvider.forLang(settings.value.language)
        viewModelScope.launch {
            settingsStore.saveCustomTrackTitle(track.id, clean)
            if (_currentTrack.value?.id == track.id) {
                _currentTrack.value = _currentTrack.value?.copy(title = clean)
            }
            if (_queue.value.any { it.id == track.id }) {
                _queue.value = _queue.value.map { if (it.id == track.id) it.copy(title = clean) else it }
            }
            val ctrl = mediaController
            if (ctrl != null) {
                for (idx in 0 until ctrl.mediaItemCount) {
                    val item = ctrl.getMediaItemAt(idx)
                    if (item.mediaId == track.id) {
                        val updatedMeta = item.mediaMetadata.buildUpon()
                            .setTitle(clean)
                            .setDisplayTitle(clean)
                            .build()
                        ctrl.replaceMediaItem(idx, item.buildUpon().setMediaMetadata(updatedMeta).build())
                    }
                }
            }
            showToast(s.toastTrackRenamed)
        }
    }

    fun fetchArtworkForTracks(trackIds: Set<String>) {
        if (trackIds.isEmpty() || _artworkFetchProgress.value != null) return
        val targets = classifiedTracks.value.filter { trackIds.contains(it.id) }
        if (targets.isEmpty()) return

        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val lang = settings.value.language
            val artCacheDir = File(ctx.filesDir, "track_art_v2").apply { mkdirs() }
            val updatedUris = LinkedHashMap<String, String>()
            var successCount = 0
            val total = targets.size
            _artworkFetchProgress.value = 0 to total

            try {
                withContext(Dispatchers.IO) {
                    targets.forEachIndexed { index, track ->
                        val cachedFile = File(artCacheDir, "track_${track.id}.jpg")
                        File(artCacheDir, "track_${track.id}.none").delete()
                        File(artCacheDir, "track_${track.id}.online_miss").delete()

                        val ok = OnlineMetadataResolver.resolveTrackArtOnline(
                            context = ctx,
                            trackId = track.id,
                            title = track.title,
                            artist = track.artist,
                            targetFile = cachedFile,
                            forceRefresh = true
                        )
                        if (ok && cachedFile.exists() && cachedFile.length() > 0L) {
                            AudioRepository.invalidateTrackArtCache(track.id)
                            val uriStr = Uri.fromFile(cachedFile)
                                .buildUpon()
                                .appendQueryParameter("t", System.currentTimeMillis().toString())
                                .build()
                                .toString()
                            updatedUris[track.id] = uriStr
                            successCount++
                        }
                        _artworkFetchProgress.value = (index + 1) to total
                    }
                }

                if (updatedUris.isNotEmpty()) {
                    val refreshed = _allScannedTracks.value.map { t ->
                        val newUri = updatedUris[t.id]
                        if (newUri != null) t.copy(artUri = newUri) else t
                    }
                    _allScannedTracks.value = refreshed
                    withContext(Dispatchers.IO) {
                        audioRepo.saveTracksToCache(refreshed)
                    }

                    val active = _currentTrack.value
                    if (active != null && updatedUris.containsKey(active.id)) {
                        val updatedActive = active.copy(artUri = updatedUris[active.id])
                        setActiveTrackVisuals(updatedActive)
                    }
                    if (_queue.value.any { updatedUris.containsKey(it.id) }) {
                        _queue.value = _queue.value.map { t ->
                            val newUri = updatedUris[t.id]
                            if (newUri != null) t.copy(artUri = newUri) else t
                        }
                    }
                }

                val msg = if (lang == AppLanguage.AR) {
                    if (successCount > 0) "تم جلب $successCount من أصل $total صورة بنجاح"
                    else "لم يتم العثور على صور مطابقة بالأسماء الحالية"
                } else {
                    if (successCount > 0) "Fetched $successCount of $total covers"
                    else "No matching covers found for selected songs"
                }
                showToast(msg)
            } finally {
                _artworkFetchProgress.value = null
            }
        }
    }

    fun createPlaylist(name: String, initialTrackIds: List<String> = emptyList()) {
        val clean = name.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            val current = playlists.value.toMutableList()
            current.add(0, Playlist(id = UUID.randomUUID().toString(), name = clean, trackIds = initialTrackIds))
            settingsStore.savePlaylists(current)
        }
    }

    fun addTrackToPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            val updated = playlists.value.map { p ->
                if (p.id == playlistId && !p.trackIds.contains(trackId)) {
                    p.copy(trackIds = p.trackIds + trackId)
                } else p
            }
            settingsStore.savePlaylists(updated)
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            val updated = playlists.value.map { p ->
                if (p.id == playlistId) p.copy(trackIds = p.trackIds.filterNot { it == trackId }) else p
            }
            settingsStore.savePlaylists(updated)
        }
    }

    fun updatePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            val updated = playlists.value.map { if (it.id == playlist.id) playlist else it }
            settingsStore.savePlaylists(updated)
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            val updated = playlists.value.filterNot { it.id == playlistId }
            settingsStore.savePlaylists(updated)
        }
    }

    fun fetchLyricsForTrack(track: Track) {
        lastFetchedLyricsTrackId = track.id
        viewModelScope.launch {
            _isFetchingLyrics.value = true
            _currentLyrics.value = null
            val saved = settingsStore.getSavedLyrics(track.id)
            if (saved != null && (!saved.syncedLyrics.isNullOrBlank() || !saved.plainLyrics.isNullOrBlank())) {
                _currentLyrics.value = saved
                _isFetchingLyrics.value = false
                return@launch
            }
            val fetched = LyricsRepository.fetchBestLyrics(track.title, track.artist, track.durationSec)
            if (_currentTrack.value?.id == track.id) {
                _currentLyrics.value = fetched
            }
            if (fetched != null) {
                settingsStore.saveLyrics(track.id, fetched.syncedLyrics, fetched.plainLyrics)
            }
            _isFetchingLyrics.value = false
        }
    }

    fun searchLyricsManual(query: String) {
        viewModelScope.launch {
            _isSearchingManualLyrics.value = true
            _manualSearchResults.value = LyricsRepository.searchManual(query)
            _isSearchingManualLyrics.value = false
        }
    }

    fun applyManualLyrics(result: LrcSearchResult) {
        val track = _currentTrack.value ?: return
        val s = StringsProvider.forLang(settings.value.language)
        val lines = result.syncedLyrics?.let { LyricsRepository.parseLrc(it) } ?: emptyList()
        val data = LyricsData(
            syncedLyrics = result.syncedLyrics,
            plainLyrics = result.plainLyrics,
            lines = lines,
            sourceName = "LRCLIB"
        )
        _currentLyrics.value = data
        viewModelScope.launch {
            settingsStore.saveLyrics(track.id, result.syncedLyrics, result.plainLyrics)
            showToast(s.toastLyricsApplied)
        }
    }

    fun applyLrcContent(rawLrc: String) {
        val track = _currentTrack.value ?: return
        val s = StringsProvider.forLang(settings.value.language)
        val lines = LyricsRepository.parseLrc(rawLrc)
        val data = if (lines.isNotEmpty()) {
            LyricsData(syncedLyrics = rawLrc, plainLyrics = null, lines = lines, sourceName = "LRC")
        } else {
            LyricsData(syncedLyrics = null, plainLyrics = rawLrc, lines = emptyList(), sourceName = "LRC")
        }
        _currentLyrics.value = data
        viewModelScope.launch {
            settingsStore.saveLyrics(track.id, data.syncedLyrics, data.plainLyrics)
            showToast(s.toastLyricsApplied)
        }
    }

    fun startSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerSeconds.value = null
            return
        }
        sleepJob = viewModelScope.launch {
            var remaining = minutes * 60
            while (remaining > 0) {
                _sleepTimerSeconds.value = remaining
                delay(1000L)
                remaining--
            }
            _sleepTimerSeconds.value = null
            mediaController?.pause()
        }
    }

    fun updateLanguage(lang: AppLanguage) {
        viewModelScope.launch { settingsStore.updateSettings { it.copy(language = lang) } }
    }

    fun updateCrossfade(enabled: Boolean, sec: Int = settings.value.crossfadeSec) {
        viewModelScope.launch { settingsStore.updateSettings { it.copy(crossfadeEnabled = enabled, crossfadeSec = sec) } }
    }

    fun updateAutoExcludeRecordings(enabled: Boolean) {
        viewModelScope.launch { settingsStore.updateSettings { it.copy(autoExcludeRecordings = enabled) } }
    }

    fun updateMinDuration(sec: Int) {
        viewModelScope.launch { settingsStore.updateSettings { it.copy(minDurationSec = sec) } }
    }

    fun addCustomKeyword(keyword: String) {
        val clean = keyword.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            settingsStore.updateSettings {
                if (it.customKeywords.contains(clean)) it else it.copy(customKeywords = it.customKeywords + clean)
            }
        }
    }

    fun removeCustomKeyword(keyword: String) {
        viewModelScope.launch {
            settingsStore.updateSettings { it.copy(customKeywords = it.customKeywords.filterNot { k -> k == keyword }) }
        }
    }

    fun updateDynamicAccent(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.updateSettings { it.copy(dynamicAccent = enabled) }
            val track = _currentTrack.value
            if (enabled && track != null) {
                _accentColor.value = track.accentColor
                _secondaryColor.value = track.secondaryColor
            } else {
                _accentColor.value = 0xFFF5F5F5.toInt()
                _secondaryColor.value = 0xFF1C1C22.toInt()
            }
        }
    }

    fun toggleExcludedFolder(folderPath: String) {
        viewModelScope.launch {
            settingsStore.toggleExcludedFolder(folderPath)
        }
    }

    fun setAllFoldersExcluded(allPaths: Set<String>, excludeAll: Boolean) {
        viewModelScope.launch {
            settingsStore.setAllFoldersExcluded(allPaths, excludeAll)
        }
    }

    fun updateArtworkBgMode(mode: ArtworkBgMode) {
        viewModelScope.launch { settingsStore.updateSettings { it.copy(artworkBgMode = mode) } }
    }

    fun updateGlassBlur(enabled: Boolean) {
        viewModelScope.launch { settingsStore.updateSettings { it.copy(glassBlurEnabled = enabled) } }
    }

    fun adjustLyricOffset(deltaMs: Long) {
        viewModelScope.launch {
            settingsStore.updateSettings { it.copy(lyricOffsetMs = it.lyricOffsetMs + deltaMs) }
        }
    }

    fun clearSavedData() {
        val s = StringsProvider.forLang(settings.value.language)
        viewModelScope.launch {
            settingsStore.clearAll()
            showToast(s.toastLibraryCleared)
        }
    }

    fun showToast(message: String) {
        viewModelScope.launch {
            _toastMessage.value = message
            delay(2600L)
            if (_toastMessage.value == message) _toastMessage.value = null
        }
    }

    private fun toMediaItem(track: Track, artworkBytes: ByteArray? = null): MediaItem {
        val extras = Bundle().apply { putString("trackId", track.id) }
        val metaBuilder = MediaMetadata.Builder()
            .setTitle(track.title)
            .setDisplayTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setExtras(extras)
        if (artworkBytes != null && artworkBytes.isNotEmpty()) {
            metaBuilder.setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        } else {
            val publicUri = track.artUri?.takeIf {
                it.startsWith("http://") || it.startsWith("https://") || it.startsWith("content://")
            }
            if (publicUri != null) {
                metaBuilder.setArtworkUri(Uri.parse(publicUri))
            }
        }
        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.contentUri)
            .setMediaMetadata(metaBuilder.build())
            .build()
    }

    override fun onCleared() {
        fadeJob?.cancel()
        fadeOutJob?.cancel()
        runCatching {
            fadeOutPlayer?.stop()
            fadeOutPlayer?.release()
        }
        fadeOutPlayer = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
