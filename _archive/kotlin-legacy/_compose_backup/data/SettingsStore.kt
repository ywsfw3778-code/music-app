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
import com.example.model.LyricsData
import com.example.model.Playlist
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
        val MIN_DURATION_SEC = intPreferencesKey("min_duration_sec")
        val CUSTOM_KEYWORDS = stringPreferencesKey("custom_keywords")
        val DYNAMIC_ACCENT = booleanPreferencesKey("dynamic_accent")
        val ARTWORK_BG_MODE = stringPreferencesKey("artwork_bg_mode")
        val LYRIC_OFFSET_MS = longPreferencesKey("lyric_offset_ms")
        val FAVORITE_IDS = stringSetPreferencesKey("favorite_ids")
        val MANUAL_EXCLUDED_IDS = stringSetPreferencesKey("manual_excluded_ids")
        val MANUAL_RESTORED_IDS = stringSetPreferencesKey("manual_restored_ids")
        val PLAYLISTS_JSON = stringPreferencesKey("playlists_json")
        val SAVED_LYRICS_JSON = stringPreferencesKey("saved_lyrics_json")
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
            crossfadeEnabled = prefs[Keys.CROSSFADE_ENABLED] ?: false,
            crossfadeSec = (prefs[Keys.CROSSFADE_SEC] ?: 4).coerceIn(1, 12),
            autoExcludeRecordings = prefs[Keys.AUTO_EXCLUDE_REC] ?: true,
            minDurationSec = (prefs[Keys.MIN_DURATION_SEC] ?: 30).coerceIn(0, 120),
            customKeywords = keywords,
            dynamicAccent = prefs[Keys.DYNAMIC_ACCENT] ?: true,
            artworkBgMode = bgMode,
            lyricOffsetMs = prefs[Keys.LYRIC_OFFSET_MS] ?: 0L
        )
    }

    val favoriteIdsFlow: Flow<Set<String>> = context.dataStore.data.map { it[Keys.FAVORITE_IDS] ?: emptySet() }
    val manualExcludedIdsFlow: Flow<Set<String>> = context.dataStore.data.map { it[Keys.MANUAL_EXCLUDED_IDS] ?: emptySet() }
    val manualRestoredIdsFlow: Flow<Set<String>> = context.dataStore.data.map { it[Keys.MANUAL_RESTORED_IDS] ?: emptySet() }

    val playlistsFlow: Flow<List<Playlist>> = context.dataStore.data.map { prefs ->
        parsePlaylists(prefs[Keys.PLAYLISTS_JSON].orEmpty())
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = AppSettings(
                language = if (prefs[Keys.LANGUAGE] == "EN") AppLanguage.EN else AppLanguage.AR,
                crossfadeEnabled = prefs[Keys.CROSSFADE_ENABLED] ?: false,
                crossfadeSec = (prefs[Keys.CROSSFADE_SEC] ?: 4).coerceIn(1, 12),
                autoExcludeRecordings = prefs[Keys.AUTO_EXCLUDE_REC] ?: true,
                minDurationSec = (prefs[Keys.MIN_DURATION_SEC] ?: 30).coerceIn(0, 120),
                customKeywords = prefs[Keys.CUSTOM_KEYWORDS].orEmpty().split("\n").filter { it.isNotBlank() },
                dynamicAccent = prefs[Keys.DYNAMIC_ACCENT] ?: true,
                artworkBgMode = when (prefs[Keys.ARTWORK_BG_MODE]) {
                    "PARTIAL" -> ArtworkBgMode.PARTIAL
                    "OFF" -> ArtworkBgMode.OFF
                    else -> ArtworkBgMode.FULL
                },
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
            prefs[Keys.LYRIC_OFFSET_MS] = next.lyricOffsetMs
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
}
