package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppLanguage
import com.example.model.AppSettings
import com.example.model.ArtworkBgMode
import com.example.model.LibrarySubTab
import com.example.model.LyricsData
import com.example.model.MainTab
import com.example.model.Playlist
import com.example.model.RepeatMode
import com.example.model.SavedSession
import com.example.model.SortOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "sukoon_nocturne_prefs")

class SettingsStore(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val CROSSFADE_ENABLED = booleanPreferencesKey("crossfade_enabled")
        val CROSSFADE_SEC = intPreferencesKey("crossfade_sec")
        val AUTO_EXCLUDE_REC = booleanPreferencesKey("auto_exclude_rec")
        val MIN_DURATION_SEC = intPreferencesKey("min_duration_sec_v2")
        val CUSTOM_KEYWORDS = stringPreferencesKey("custom_keywords")
        val DYNAMIC_ACCENT = booleanPreferencesKey("dynamic_accent")
        val ARTWORK_BG_MODE = stringPreferencesKey("artwork_bg_mode")
        val GLASS_BLUR_ENABLED = booleanPreferencesKey("glass_blur_enabled")
        val LYRIC_OFFSET_MS = longPreferencesKey("lyric_offset_ms")
        val FAVORITE_IDS = stringSetPreferencesKey("favorite_ids")
        val MANUAL_EXCLUDED_IDS = stringSetPreferencesKey("manual_excluded_ids")
        val MANUAL_RESTORED_IDS = stringSetPreferencesKey("manual_restored_ids")
        val EXCLUDED_FOLDERS = stringSetPreferencesKey("excluded_folders")
        val PLAYLISTS_JSON = stringPreferencesKey("playlists_json")
        val SAVED_LYRICS_JSON = stringPreferencesKey("saved_lyrics_json")
        val CUSTOM_TRACK_TITLES_JSON = stringPreferencesKey("custom_track_titles_json")
        val LAST_TRACK_ID = stringPreferencesKey("last_track_id")
        val LAST_POSITION_MS = longPreferencesKey("last_position_ms")
        val LAST_QUEUE_IDS_JSON = stringPreferencesKey("last_queue_ids_json")
        val LAST_SHUFFLE = booleanPreferencesKey("last_shuffle")
        val LAST_REPEAT_MODE = stringPreferencesKey("last_repeat_mode")
        val LAST_MAIN_TAB = stringPreferencesKey("last_main_tab")
        val LAST_LIBRARY_SUBTAB = stringPreferencesKey("last_library_subtab")
        val LAST_SORT_OPTION = stringPreferencesKey("last_sort_option")
        val LAST_SHOW_FULL_PLAYER = booleanPreferencesKey("last_show_full_player")
        val LAST_FULL_PLAYER_PAGE = intPreferencesKey("last_full_player_page")
        val LAST_SCROLL_INDEX = intPreferencesKey("last_scroll_index")
        val LAST_SCROLL_OFFSET = intPreferencesKey("last_scroll_offset")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val lang = if (prefs[Keys.LANGUAGE] == "EN") AppLanguage.EN else AppLanguage.AR
        val bgMode = when (prefs[Keys.ARTWORK_BG_MODE]) {
            "PARTIAL" -> ArtworkBgMode.PARTIAL
            "OFF" -> ArtworkBgMode.OFF
            else -> ArtworkBgMode.FULL
        }
        val keywordsRaw = prefs[Keys.CUSTOM_KEYWORDS].orEmpty()
        val keywords = if (keywordsRaw.isBlank()) emptyList() else keywordsRaw.split("\n").filter { it.isNotBlank() }

        AppSettings(
            language = lang,
            crossfadeEnabled = prefs[Keys.CROSSFADE_ENABLED] ?: true,
            crossfadeSec = (prefs[Keys.CROSSFADE_SEC] ?: 4).coerceIn(1, 12),
            autoExcludeRecordings = prefs[Keys.AUTO_EXCLUDE_REC] ?: true,
            minDurationSec = (prefs[Keys.MIN_DURATION_SEC] ?: 60).coerceIn(0, 180),
            customKeywords = keywords,
            excludedFolderPaths = prefs[Keys.EXCLUDED_FOLDERS] ?: emptySet(),
            dynamicAccent = prefs[Keys.DYNAMIC_ACCENT] ?: true,
            artworkBgMode = bgMode,
            glassBlurEnabled = prefs[Keys.GLASS_BLUR_ENABLED] ?: true,
            lyricOffsetMs = prefs[Keys.LYRIC_OFFSET_MS] ?: 0L
        )
    }

    val favoriteIdsFlow: Flow<Set<String>> = context.dataStore.data.map { it[Keys.FAVORITE_IDS] ?: emptySet() }
    val manualExcludedIdsFlow: Flow<Set<String>> = context.dataStore.data.map { it[Keys.MANUAL_EXCLUDED_IDS] ?: emptySet() }
    val manualRestoredIdsFlow: Flow<Set<String>> = context.dataStore.data.map { it[Keys.MANUAL_RESTORED_IDS] ?: emptySet() }

    val playlistsFlow: Flow<List<Playlist>> = context.dataStore.data.map { prefs ->
        parsePlaylists(prefs[Keys.PLAYLISTS_JSON].orEmpty())
    }

    val customTrackTitlesFlow: Flow<Map<String, String>> = context.dataStore.data.map { prefs ->
        parseStringMap(prefs[Keys.CUSTOM_TRACK_TITLES_JSON].orEmpty())
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = AppSettings(
                language = if (prefs[Keys.LANGUAGE] == "EN") AppLanguage.EN else AppLanguage.AR,
                crossfadeEnabled = prefs[Keys.CROSSFADE_ENABLED] ?: true,
                crossfadeSec = (prefs[Keys.CROSSFADE_SEC] ?: 4).coerceIn(1, 12),
                autoExcludeRecordings = prefs[Keys.AUTO_EXCLUDE_REC] ?: true,
                minDurationSec = (prefs[Keys.MIN_DURATION_SEC] ?: 60).coerceIn(0, 180),
                customKeywords = prefs[Keys.CUSTOM_KEYWORDS].orEmpty().split("\n").filter { it.isNotBlank() },
                excludedFolderPaths = prefs[Keys.EXCLUDED_FOLDERS] ?: emptySet(),
                dynamicAccent = prefs[Keys.DYNAMIC_ACCENT] ?: true,
                artworkBgMode = when (prefs[Keys.ARTWORK_BG_MODE]) {
                    "PARTIAL" -> ArtworkBgMode.PARTIAL
                    "OFF" -> ArtworkBgMode.OFF
                    else -> ArtworkBgMode.FULL
                },
                glassBlurEnabled = prefs[Keys.GLASS_BLUR_ENABLED] ?: true,
                lyricOffsetMs = prefs[Keys.LYRIC_OFFSET_MS] ?: 0L
            )
            val next = transform(current)
            prefs[Keys.LANGUAGE] = if (next.language == AppLanguage.EN) "EN" else "AR"
            prefs[Keys.CROSSFADE_ENABLED] = next.crossfadeEnabled
            prefs[Keys.CROSSFADE_SEC] = next.crossfadeSec
            prefs[Keys.AUTO_EXCLUDE_REC] = next.autoExcludeRecordings
            prefs[Keys.MIN_DURATION_SEC] = next.minDurationSec
            prefs[Keys.CUSTOM_KEYWORDS] = next.customKeywords.joinToString("\n")
            prefs[Keys.DYNAMIC_ACCENT] = next.dynamicAccent
            prefs[Keys.ARTWORK_BG_MODE] = next.artworkBgMode.name
            prefs[Keys.GLASS_BLUR_ENABLED] = next.glassBlurEnabled
            prefs[Keys.LYRIC_OFFSET_MS] = next.lyricOffsetMs
            prefs[Keys.EXCLUDED_FOLDERS] = next.excludedFolderPaths
        }
    }

    suspend fun toggleExcludedFolder(folderPath: String) {
        context.dataStore.edit { prefs ->
            val set = (prefs[Keys.EXCLUDED_FOLDERS] ?: emptySet()).toMutableSet()
            if (set.contains(folderPath)) {
                set.remove(folderPath)
            } else {
                set.add(folderPath)
            }
            prefs[Keys.EXCLUDED_FOLDERS] = set
        }
    }

    suspend fun setAllFoldersExcluded(allPaths: Set<String>, excludeAll: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EXCLUDED_FOLDERS] = if (excludeAll) allPaths else emptySet()
        }
    }

    suspend fun toggleFavorite(trackId: String): Boolean {
        var added = false
        context.dataStore.edit { prefs ->
            val set = (prefs[Keys.FAVORITE_IDS] ?: emptySet()).toMutableSet()
            added = if (set.contains(trackId)) {
                set.remove(trackId)
                false
            } else {
                set.add(trackId)
                true
            }
            prefs[Keys.FAVORITE_IDS] = set
        }
        return added
    }

    suspend fun setManualExclusion(trackId: String, excluded: Boolean) {
        context.dataStore.edit { prefs ->
            val ex = (prefs[Keys.MANUAL_EXCLUDED_IDS] ?: emptySet()).toMutableSet()
            val res = (prefs[Keys.MANUAL_RESTORED_IDS] ?: emptySet()).toMutableSet()
            if (excluded) {
                ex.add(trackId)
                res.remove(trackId)
            } else {
                ex.remove(trackId)
                res.add(trackId)
            }
            prefs[Keys.MANUAL_EXCLUDED_IDS] = ex
            prefs[Keys.MANUAL_RESTORED_IDS] = res
        }
    }

    suspend fun savePlaylists(playlists: List<Playlist>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PLAYLISTS_JSON] = serializePlaylists(playlists)
        }
    }

    suspend fun getSavedLyrics(trackId: String): LyricsData? {
        val prefs = context.dataStore.data.first()
        val raw = prefs[Keys.SAVED_LYRICS_JSON].orEmpty()
        if (raw.isBlank()) return null
        return runCatching {
            val root = JSONObject(raw)
            val obj = root.optJSONObject(trackId) ?: return null
            val synced = obj.optString("synced", "").takeIf { it.isNotBlank() }
            val plain = obj.optString("plain", "").takeIf { it.isNotBlank() }
            val lines = if (synced != null) LyricsRepository.parseLrc(synced) else emptyList()
            LyricsData(syncedLyrics = synced, plainLyrics = plain, lines = lines, sourceName = "saved")
        }.getOrNull()
    }

    suspend fun saveLyrics(trackId: String, synced: String?, plain: String?) {
        context.dataStore.edit { prefs ->
            val raw = prefs[Keys.SAVED_LYRICS_JSON].orEmpty()
            val root = runCatching { if (raw.isBlank()) JSONObject() else JSONObject(raw) }.getOrDefault(JSONObject())
            val obj = JSONObject().apply {
                put("synced", synced ?: "")
                put("plain", plain ?: "")
            }
            root.put(trackId, obj)
            prefs[Keys.SAVED_LYRICS_JSON] = root.toString()
        }
    }

    suspend fun saveCustomTrackTitle(trackId: String, title: String) {
        val clean = title.trim()
        if (clean.isBlank()) return
        context.dataStore.edit { prefs ->
            val raw = prefs[Keys.CUSTOM_TRACK_TITLES_JSON].orEmpty()
            val root = runCatching { if (raw.isBlank()) JSONObject() else JSONObject(raw) }.getOrDefault(JSONObject())
            root.put(trackId, clean)
            prefs[Keys.CUSTOM_TRACK_TITLES_JSON] = root.toString()
        }
    }

    suspend fun getSavedSession(): SavedSession {
        val prefs = context.dataStore.data.first()
        val queueIds = runCatching {
            val raw = prefs[Keys.LAST_QUEUE_IDS_JSON].orEmpty()
            if (raw.isBlank()) emptyList() else {
                val arr = JSONArray(raw)
                List(arr.length()) { i -> arr.getString(i) }
            }
        }.getOrDefault(emptyList())

        val repeat = runCatching {
            RepeatMode.valueOf(prefs[Keys.LAST_REPEAT_MODE] ?: RepeatMode.ALL.name)
        }.getOrDefault(RepeatMode.ALL)

        val mainTab = runCatching {
            MainTab.valueOf(prefs[Keys.LAST_MAIN_TAB] ?: MainTab.LIBRARY.name)
        }.getOrDefault(MainTab.LIBRARY)

        val subTab = runCatching {
            LibrarySubTab.valueOf(prefs[Keys.LAST_LIBRARY_SUBTAB] ?: LibrarySubTab.ALL_SONGS.name)
        }.getOrDefault(LibrarySubTab.ALL_SONGS)

        val sortOpt = runCatching {
            SortOption.valueOf(prefs[Keys.LAST_SORT_OPTION] ?: SortOption.RECENT.name)
        }.getOrDefault(SortOption.RECENT)

        return SavedSession(
            lastTrackId = prefs[Keys.LAST_TRACK_ID]?.takeIf { it.isNotBlank() },
            lastPositionMs = (prefs[Keys.LAST_POSITION_MS] ?: 0L).coerceAtLeast(0L),
            lastQueueIds = queueIds,
            shuffleEnabled = prefs[Keys.LAST_SHUFFLE] ?: false,
            repeatMode = repeat,
            lastMainTab = mainTab,
            lastLibrarySubTab = subTab,
            lastSortOption = sortOpt,
            lastShowFullPlayer = prefs[Keys.LAST_SHOW_FULL_PLAYER] ?: false,
            lastFullPlayerPage = (prefs[Keys.LAST_FULL_PLAYER_PAGE] ?: 0).coerceIn(0, 2),
            lastScrollIndex = (prefs[Keys.LAST_SCROLL_INDEX] ?: 0).coerceAtLeast(0),
            lastScrollOffset = (prefs[Keys.LAST_SCROLL_OFFSET] ?: 0).coerceAtLeast(0)
        )
    }

    suspend fun savePlaybackSession(
        trackId: String?,
        positionMs: Long,
        queueIds: List<String>,
        shuffleEnabled: Boolean,
        repeatMode: RepeatMode
    ) {
        context.dataStore.edit { prefs ->
            if (trackId.isNullOrBlank()) {
                prefs.remove(Keys.LAST_TRACK_ID)
                prefs[Keys.LAST_POSITION_MS] = 0L
                prefs.remove(Keys.LAST_QUEUE_IDS_JSON)
            } else {
                prefs[Keys.LAST_TRACK_ID] = trackId
                prefs[Keys.LAST_POSITION_MS] = positionMs.coerceAtLeast(0L)
                // Cap serialized queue IDs to avoid bloated DataStore entry on massive libraries
                val limited = if (queueIds.size > 1000) queueIds.take(1000) else queueIds
                prefs[Keys.LAST_QUEUE_IDS_JSON] = JSONArray(limited).toString()
            }
            prefs[Keys.LAST_SHUFFLE] = shuffleEnabled
            prefs[Keys.LAST_REPEAT_MODE] = repeatMode.name
        }
    }

    suspend fun savePositionOnly(positionMs: Long) {
        context.dataStore.edit { prefs ->
            if (!prefs[Keys.LAST_TRACK_ID].isNullOrBlank()) {
                prefs[Keys.LAST_POSITION_MS] = positionMs.coerceAtLeast(0L)
            }
        }
    }

    suspend fun clearPlaybackSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.LAST_TRACK_ID)
            prefs[Keys.LAST_POSITION_MS] = 0L
            prefs.remove(Keys.LAST_QUEUE_IDS_JSON)
            prefs[Keys.LAST_SHOW_FULL_PLAYER] = false
        }
    }

    suspend fun saveUiSession(
        mainTab: MainTab? = null,
        librarySubTab: LibrarySubTab? = null,
        sortOption: SortOption? = null,
        showFullPlayer: Boolean? = null,
        fullPlayerPage: Int? = null,
        scrollIndex: Int? = null,
        scrollOffset: Int? = null
    ) {
        context.dataStore.edit { prefs ->
            mainTab?.let { prefs[Keys.LAST_MAIN_TAB] = it.name }
            librarySubTab?.let { prefs[Keys.LAST_LIBRARY_SUBTAB] = it.name }
            sortOption?.let { prefs[Keys.LAST_SORT_OPTION] = it.name }
            showFullPlayer?.let { prefs[Keys.LAST_SHOW_FULL_PLAYER] = it }
            fullPlayerPage?.let { prefs[Keys.LAST_FULL_PLAYER_PAGE] = it.coerceIn(0, 2) }
            scrollIndex?.let { prefs[Keys.LAST_SCROLL_INDEX] = it.coerceAtLeast(0) }
            scrollOffset?.let { prefs[Keys.LAST_SCROLL_OFFSET] = it.coerceAtLeast(0) }
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    private fun serializePlaylists(list: List<Playlist>): String {
        val arr = JSONArray()
        list.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("createdAt", p.createdAt)
                put("trackIds", JSONArray(p.trackIds))
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parsePlaylists(raw: String): List<Playlist> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                val idsArr = obj.optJSONArray("trackIds") ?: JSONArray()
                val ids = List(idsArr.length()) { j -> idsArr.getString(j) }
                Playlist(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    trackIds = ids,
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun parseStringMap(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            val map = LinkedHashMap<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = obj.optString(k, "").trim()
                if (v.isNotBlank()) map[k] = v
            }
            map
        }.getOrDefault(emptyMap())
    }
}
