package com.example

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class SukoonViewModel(application: Application) : AndroidViewModel(application) {

    val repo = MusicRepository(application)

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _playlists = MutableStateFlow(repo.getPlaylists())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _settings = MutableStateFlow(repo.getSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _sortBy = MutableStateFlow(repo.getSortBy())
    val sortBy: StateFlow<SortBy> = _sortBy.asStateFlow()

    private val _activeTab = MutableStateFlow(AppTab.LIBRARY)
    val activeTab: StateFlow<AppTab> = _activeTab.asStateFlow()

    private val _librarySubTab = MutableStateFlow(LibrarySubTab.SONGS)
    val librarySubTab: StateFlow<LibrarySubTab> = _librarySubTab.asStateFlow()

    private val _selectedFolder = MutableStateFlow("ALL")
    val selectedFolder: StateFlow<String> = _selectedFolder.asStateFlow()

    private val _selectedPlaylistId = MutableStateFlow<String?>(null)
    val selectedPlaylistId: StateFlow<String?> = _selectedPlaylistId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _currentTrackId = MutableStateFlow<String?>(null)
    val currentTrackId: StateFlow<String?> = _currentTrackId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatModeState.OFF)
    val repeatMode: StateFlow<RepeatModeState> = _repeatMode.asStateFlow()

    private val _queueIds = MutableStateFlow<List<String>>(emptyList())
    val queueIds: StateFlow<List<String>> = _queueIds.asStateFlow()

    private val _playbackContextIds = MutableStateFlow<List<String>>(emptyList())

    private val _isNowPlayingOpen = MutableStateFlow(false)
    val isNowPlayingOpen: StateFlow<Boolean> = _isNowPlayingOpen.asStateFlow()

    private val _nowPlayingTab = MutableStateFlow(NowPlayingTab.ARTWORK)
    val nowPlayingTab: StateFlow<NowPlayingTab> = _nowPlayingTab.asStateFlow()

    private val _contextMenuTrack = MutableStateFlow<Track?>(null)
    val contextMenuTrack: StateFlow<Track?> = _contextMenuTrack.asStateFlow()

    private val _isCreatePlaylistOpen = MutableStateFlow(false)
    val isCreatePlaylistOpen: StateFlow<Boolean> = _isCreatePlaylistOpen.asStateFlow()

    private val _editingPlaylistId = MutableStateFlow<String?>(null)
    val editingPlaylistId: StateFlow<String?> = _editingPlaylistId.asStateFlow()

    private val _playlistSettingsId = MutableStateFlow<String?>(null)
    val playlistSettingsId: StateFlow<String?> = _playlistSettingsId.asStateFlow()

    private val _activePalette = MutableStateFlow(
        TrackPalette(
            accent = Color(0xFFE2E8F0),
            glow = Color(0xFF475569),
            secondary = Color(0xFF334155)
        )
    )
    val activePalette: StateFlow<TrackPalette> = _activePalette.asStateFlow()

    private val _lyrics = MutableStateFlow<TrackLyrics?>(null)
    val lyrics: StateFlow<TrackLyrics?> = _lyrics.asStateFlow()

    private val _lyricsResults = MutableStateFlow<List<LrcSearchResult>>(emptyList())
    val lyricsResults: StateFlow<List<LrcSearchResult>> = _lyricsResults.asStateFlow()

    private val _isSearchingLyrics = MutableStateFlow(false)
    val isSearchingLyrics: StateFlow<Boolean> = _isSearchingLyrics.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var tickerJob: Job? = null
    private var toastJob: Job? = null

    init {
        connectController()
    }

    private fun connectController() {
        val ctx = getApplication<Application>()
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(ctx, token).buildAsync().apply {
            addListener({
                try {
                    val controller = get()
                    mediaController = controller
                    _isPlaying.value = controller.isPlaying
                    if (controller.duration > 0) _durationMs.value = controller.duration
                    _positionMs.value = controller.currentPosition.coerceAtLeast(0L)
                    controller.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _isPlaying.value = isPlaying
                            if (isPlaying) startTicker() else stopTicker()
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_READY) {
                                if (controller.duration > 0) _durationMs.value = controller.duration
                            } else if (playbackState == Player.STATE_ENDED) {
                                handleTrackEnded()
                            }
                        }
                    })
                    if (controller.isPlaying) startTicker()
                } catch (_: Exception) {
                }
            }, MoreExecutors.directExecutor())
        }
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = viewModelScope.launch {
            while (isActive) {
                mediaController?.let { c ->
                    _positionMs.value = c.currentPosition.coerceAtLeast(0L)
                    if (c.duration > 0) _durationMs.value = c.duration
                }
                delay(250)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    fun scanDeviceAudio() {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            try {
                _tracks.value = repo.scanDeviceAudio(_settings.value)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            delay(2500)
            _toastMessage.value = null
        }
    }

    fun setTab(tab: AppTab) {
        _activeTab.value = tab
    }

    fun setLibrarySubTab(sub: LibrarySubTab) {
        _librarySubTab.value = sub
        if (sub != LibrarySubTab.PLAYLISTS) _selectedPlaylistId.value = null
    }

    fun setSelectedFolder(folder: String) {
        _selectedFolder.value = folder
    }

    fun setSelectedPlaylistId(id: String?) {
        _selectedPlaylistId.value = id
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun cycleSortBy() {
        val next = _sortBy.value.next()
        _sortBy.value = next
        repo.saveSortBy(next)
    }

    fun openNowPlaying(tab: NowPlayingTab = NowPlayingTab.ARTWORK) {
        _nowPlayingTab.value = tab
        _isNowPlayingOpen.value = true
    }

    fun closeNowPlaying() {
        _isNowPlayingOpen.value = false
    }

    fun setNowPlayingTab(tab: NowPlayingTab) {
        _nowPlayingTab.value = tab
    }

    fun openContextMenu(track: Track?) {
        _contextMenuTrack.value = track
    }

    fun openCreatePlaylist(editingId: String? = null) {
        _editingPlaylistId.value = editingId
        _isCreatePlaylistOpen.value = true
    }

    fun closeCreatePlaylist() {
        _isCreatePlaylistOpen.value = false
        _editingPlaylistId.value = null
    }

    fun openPlaylistSettings(playlistId: String?) {
        _playlistSettingsId.value = playlistId
    }

    fun playTrack(track: Track, contextList: List<Track>? = null) {
        if (contextList != null && contextList.isNotEmpty()) {
            _playbackContextIds.value = contextList.map { it.id }
        }
        _currentTrackId.value = track.id
        _positionMs.value = 0L
        _durationMs.value = (track.durationSec * 1000.0).toLong()
        _lyrics.value = null
        _lyricsResults.value = emptyList()

        viewModelScope.launch {
            val bmp = repo.loadArtworkBitmap(track.mediaStoreId, track.albumId)
            if (_settings.value.dynamicArtworkAccent) {
                _activePalette.value = repo.extractPalette(track.id, bmp, track.title)
            }
            val artBytes = repo.getArtworkBytesForMediaSession(track.mediaStoreId)
            val item = PlaybackService.buildRealmeCompatibleMediaItem(
                mediaId = track.id,
                audioUri = Uri.parse(track.contentUriString),
                title = track.title,
                artist = track.artist,
                album = track.album,
                artworkBytes = artBytes
            )
            mediaController?.let { c ->
                c.setMediaItem(item)
                c.prepare()
                c.play()
            }
        }
    }

    fun togglePlayPause() {
        val c = mediaController ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun seekTo(ms: Long) {
        mediaController?.seekTo(ms.coerceAtLeast(0L))
        _positionMs.value = ms.coerceAtLeast(0L)
    }

    fun stopAndClosePlayer() {
        mediaController?.stop()
        _isPlaying.value = false
        _currentTrackId.value = null
        _isNowPlayingOpen.value = false
    }

    private fun resolveNextTrack(): Track? {
        val active = _tracks.value.filter { it.exclusionReason == null }
        if (active.isEmpty()) return null
        val curId = _currentTrackId.value
        if (_repeatMode.value == RepeatModeState.ONE && curId != null) {
            return active.firstOrNull { it.id == curId }
        }
        val q = _queueIds.value
        if (q.isNotEmpty()) {
            val qTrack = active.firstOrNull { it.id == q.first() }
            if (qTrack != null) return qTrack
        }
        val ctxIds = _playbackContextIds.value
        val pool = if (ctxIds.isNotEmpty()) {
            ctxIds.mapNotNull { id -> active.firstOrNull { it.id == id } }.ifEmpty { active }
        } else active

        if (_shuffle.value && pool.size > 1) {
            return pool.filter { it.id != curId }.randomOrNull() ?: pool.first()
        }
        val idx = pool.indexOfFirst { it.id == curId }
        return if (idx == -1) pool.firstOrNull()
        else if (idx + 1 < pool.size) pool[idx + 1]
        else if (_repeatMode.value != RepeatModeState.OFF || ctxIds.isNotEmpty()) pool.firstOrNull()
        else null
    }

    private fun handleTrackEnded() {
        val next = resolveNextTrack()
        if (next != null) {
            if (_queueIds.value.firstOrNull() == next.id) {
                _queueIds.value = _queueIds.value.drop(1)
            }
            playTrack(next)
        } else {
            _isPlaying.value = false
        }
    }

    fun playNext() {
        val next = resolveNextTrack() ?: return
        if (_queueIds.value.firstOrNull() == next.id) {
            _queueIds.value = _queueIds.value.drop(1)
        }
        playTrack(next)
    }

    fun playPrev() {
        if (_positionMs.value > 3000L) {
            seekTo(0L)
            return
        }
        val active = _tracks.value.filter { it.exclusionReason == null }
        val ctxIds = _playbackContextIds.value
        val pool = if (ctxIds.isNotEmpty()) {
            ctxIds.mapNotNull { id -> active.firstOrNull { it.id == id } }.ifEmpty { active }
        } else active
        if (pool.isEmpty()) return
        val idx = pool.indexOfFirst { it.id == _currentTrackId.value }
        val prev = if (idx <= 0) pool.last() else pool[idx - 1]
        playTrack(prev)
    }

    fun toggleShuffle() {
        _shuffle.value = !_shuffle.value
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatModeState.OFF -> RepeatModeState.ALL
            RepeatModeState.ALL -> RepeatModeState.ONE
            RepeatModeState.ONE -> RepeatModeState.OFF
        }
    }

    fun playNextInQueue(track: Track) {
        _queueIds.value = listOf(track.id) + _queueIds.value.filter { it != track.id }
        showToast("سيتم التشغيل تالياً")
    }

    fun addToQueue(track: Track) {
        if (!_queueIds.value.contains(track.id)) {
            _queueIds.value = _queueIds.value + track.id
        }
        showToast("تمت الإضافة لقائمة الانتظار")
    }

    fun clearQueue() {
        _queueIds.value = emptyList()
    }

    fun toggleFavorite(trackId: String) {
        val favs = repo.getFavoriteIds().toMutableSet()
        if (favs.contains(trackId)) favs.remove(trackId) else favs.add(trackId)
        repo.saveFavoriteIds(favs)
        _tracks.value = _tracks.value.map {
            if (it.id == trackId) it.copy(isFavorite = favs.contains(trackId)) else it
        }
    }

    fun restoreTrack(track: Track) {
        val restored = repo.getRestoredIds().toMutableSet().apply { add(track.id) }
        val manualEx = repo.getManualExcludedIds().toMutableSet().apply { remove(track.id) }
        repo.saveRestoredIds(restored)
        repo.saveManualExcludedIds(manualEx)
        _tracks.value = _tracks.value.map {
            if (it.id == track.id) it.copy(exclusionReason = null, matchedRule = null, manuallyRestored = true) else it
        }
        showToast("تمت استعادة الأغنية")
    }

    fun excludeTrack(track: Track) {
        val restored = repo.getRestoredIds().toMutableSet().apply { remove(track.id) }
        val manualEx = repo.getManualExcludedIds().toMutableSet().apply { add(track.id) }
        repo.saveRestoredIds(restored)
        repo.saveManualExcludedIds(manualEx)
        _tracks.value = _tracks.value.map {
            if (it.id == track.id) it.copy(exclusionReason = "recording", matchedRule = "Manual", manuallyRestored = false) else it
        }
        if (_currentTrackId.value == track.id) stopAndClosePlayer()
        showToast("تم استبعاد المقطع")
    }

    fun createPlaylist(name: String, trackIds: List<String>) {
        val pl = Playlist(
            id = "pl_${UUID.randomUUID().toString().take(8)}",
            name = name.ifBlank { "قائمة تشغيل جديدة" },
            trackIds = trackIds,
            createdAtMs = System.currentTimeMillis()
        )
        val updated = listOf(pl) + _playlists.value
        _playlists.value = updated
        repo.savePlaylists(updated)
        _selectedPlaylistId.value = pl.id
    }

    fun toggleTrackInPlaylist(playlistId: String, trackId: String) {
        val updated = _playlists.value.map { pl ->
            if (pl.id != playlistId) pl
            else {
                val ids = if (pl.trackIds.contains(trackId)) pl.trackIds - trackId else pl.trackIds + trackId
                pl.copy(trackIds = ids)
            }
        }
        _playlists.value = updated
        repo.savePlaylists(updated)
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val updated = _playlists.value.map {
            if (it.id == playlistId) it.copy(name = trimmed) else it
        }
        _playlists.value = updated
        repo.savePlaylists(updated)
    }

    fun deletePlaylist(playlistId: String) {
        val updated = _playlists.value.filter { it.id != playlistId }
        _playlists.value = updated
        repo.savePlaylists(updated)
        if (_selectedPlaylistId.value == playlistId) _selectedPlaylistId.value = null
        if (_playlistSettingsId.value == playlistId) _playlistSettingsId.value = null
    }

    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
        repo.saveSettings(newSettings)
        scanDeviceAudio()
    }

    fun clearLibraryData() {
        stopAndClosePlayer()
        repo.clearAllUserData()
        _playlists.value = emptyList()
        _queueIds.value = emptyList()
        scanDeviceAudio()
        showToast("تم مسح بيانات القوائم والمفضلة")
    }

    fun searchLyricsOnline(query: String) {
        viewModelScope.launch {
            _isSearchingLyrics.value = true
            try {
                _lyricsResults.value = repo.searchLyrics(query)
            } catch (_: Exception) {
                _lyricsResults.value = emptyList()
            } finally {
                _isSearchingLyrics.value = false
            }
        }
    }

    fun applyLyricsResult(trackId: String, res: LrcSearchResult) {
        val synced = res.syncedLyrics?.let { repo.parseLrc(it) } ?: emptyList()
        _lyrics.value = TrackLyrics(
            trackId = trackId,
            syncedLines = synced,
            plainLyrics = res.plainLyrics.orEmpty(),
            source = "lrclib"
        )
        showToast("تم تطبيق الكلمات")
    }

    fun handleBackPress(): Boolean {
        if (_contextMenuTrack.value != null) {
            _contextMenuTrack.value = null
            return true
        }
        if (_isCreatePlaylistOpen.value) {
            closeCreatePlaylist()
            return true
        }
        if (_playlistSettingsId.value != null) {
            _playlistSettingsId.value = null
            return true
        }
        if (_isNowPlayingOpen.value) {
            _isNowPlayingOpen.value = false
            return true
        }
        if (_activeTab.value == AppTab.LIBRARY && _librarySubTab.value == LibrarySubTab.PLAYLISTS && _selectedPlaylistId.value != null) {
            _selectedPlaylistId.value = null
            return true
        }
        if (_activeTab.value == AppTab.FOLDERS && _selectedFolder.value != "ALL") {
            _selectedFolder.value = "ALL"
            return true
        }
        if (_activeTab.value == AppTab.LIBRARY && _librarySubTab.value != LibrarySubTab.SONGS) {
            _librarySubTab.value = LibrarySubTab.SONGS
            return true
        }
        if (_activeTab.value != AppTab.LIBRARY) {
            _activeTab.value = AppTab.LIBRARY
            return true
        }
        return false
    }

    suspend fun loadArtwork(mediaStoreId: Long, albumId: Long): android.graphics.Bitmap? =
        repo.loadArtworkBitmap(mediaStoreId, albumId)

    override fun onCleared() {
        stopTicker()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
