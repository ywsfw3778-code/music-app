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
import com.example.model.LrcSearchResult
import com.example.model.LyricsData
import com.example.model.Playlist
import com.example.model.RepeatMode
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.PI
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

    private val _allScannedTracks = MutableStateFlow<List<Track>>(emptyList())
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val classifiedTracks: StateFlow<List<Track>> = combine(
        _allScannedTracks,
        settings,
        manualExcludedIds,
        manualRestoredIds
    ) { all, cfg, exIds, resIds ->
        all.map { track ->
            val reason = audioRepo.computeExclusionReason(
                id = track.id,
                title = track.title,
                fileName = track.fileName,
                folderPath = track.folderPath,
                durationSec = track.durationSec,
                settings = cfg,
                manualExcludedIds = exIds,
                manualRestoredIds = resIds
            )
            track.copy(
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

    private var sleepJob: Job? = null
    private var fadeJob: Job? = null
    private var fadeOutJob: Job? = null
    private var visualsJob: Job? = null
    private var fadeOutPlayer: ExoPlayer? = null
    private var crossfadeTriggeredForTrackId: String? = null
    private var lastFetchedLyricsTrackId: String? = null

    init {
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) { audioRepo.loadCachedTracks() }
            if (cached.isNotEmpty() && _allScannedTracks.value.isEmpty()) {
                _allScannedTracks.value = cached
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
            val attempted = HashSet<String>()
            artists.collect { list ->
                val ctx = getApplication<Application>()
                val map = _artistPhotos.value.toMutableMap()
                var pendingCount = 0
                for (a in list) {
                    if (a.name.isBlank() || a.name == "Unknown Artist") continue
                    if (map.containsKey(a.name) || !attempted.add(a.name)) continue
                    val photoUri = OnlineMetadataResolver.resolveArtistPhoto(ctx, a.name)
                    if (!photoUri.isNullOrBlank()) {
                        map[a.name] = photoUri
                        pendingCount++
                        if (pendingCount >= 5) {
                            _artistPhotos.value = LinkedHashMap(map)
                            pendingCount = 0
                        }
                    }
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
            val attempted = HashSet<String>()
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
                var pendingCount = 0
                for ((groupKey, list) in grouped) {
                    val first = list.first()
                    val normArtist = ArtistNormalizer.normalizeArtist(first.artist)
                    if (ArtistNormalizer.isFakeOrJunkAlbum(first.album, normArtist, list.size, first.title, first.fileName)) {
                        continue
                    }
                    if ((map.containsKey(groupKey) && !map[groupKey].isNullOrBlank()) || !attempted.add(groupKey)) continue
                    val coverUri = OnlineMetadataResolver.resolveAlbumArtOnline(ctx, first.album.trim(), normArtist)
                    if (!coverUri.isNullOrBlank()) {
                        map[groupKey] = coverUri
                        pendingCount++
                        if (pendingCount >= 4) {
                            _albumArt.value = LinkedHashMap(map)
                            pendingCount = 0
                        }
                    }
                    delay(40)
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
                delay(900)

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
                    } else if (t.title.isNotBlank()) {
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
            syncFromPlayer(ctrl)
            ctrl.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    crossfadeTriggeredForTrackId = null
                    syncFromPlayer(ctrl)
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
                    if (_positionMs.value != pos) _positionMs.value = pos
                    if (dur > 0L && _durationMs.value != dur) _durationMs.value = dur
                    if (_isPlaying.value != playing) _isPlaying.value = playing

                    val activeTrack = _currentTrack.value
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
                delay(if (playing) 200L else 450L)
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
        val match = _allScannedTracks.value.find { it.id == currentId }
            ?: _queue.value.find { it.id == currentId }
        if (match != null && _currentTrack.value?.id != match.id) {
            setActiveTrackVisuals(match)
        }
        _durationMs.value = ctrl.duration.coerceAtLeast(0L)
        _positionMs.value = ctrl.currentPosition.coerceAtLeast(0L)
        _isPlaying.value = ctrl.isPlaying
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

    fun scanLibrary() {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            val scanned = audioRepo.scanDeviceTracks(
                settings = settings.value,
                manualExcludedIds = manualExcludedIds.value,
                manualRestoredIds = manualRestoredIds.value
            )
            _allScannedTracks.value = scanned
            _isScanning.value = false
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

        if (ctrl == null) return
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
        mediaController?.seekTo(positionMs.coerceAtLeast(0L))
        _positionMs.value = positionMs.coerceAtLeast(0L)
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
    }

    fun toggleShuffle() {
        val next = !_shuffleEnabled.value
        _shuffleEnabled.value = next
        mediaController?.shuffleModeEnabled = next
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
