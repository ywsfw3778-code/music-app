package com.example

import androidx.compose.ui.graphics.Color

enum class SortBy(val key: String, val labelAr: String) {
    RECENT("addedAt", "الأحدث"),
    TITLE("title", "الاسم"),
    ARTIST("artist", "الفنان"),
    DURATION("duration", "المدة");

    fun next(): SortBy = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromKey(key: String?): SortBy =
            entries.firstOrNull { it.key == key } ?: RECENT
    }
}

enum class AppTab { LIBRARY, FOLDERS, SETTINGS }
enum class LibrarySubTab { SONGS, PLAYLISTS, FAVORITES, QUARANTINE }
enum class RepeatModeState { OFF, ALL, ONE }
enum class NowPlayingTab { ARTWORK, LYRICS, QUEUE }

data class Track(
    val id: String,
    val mediaStoreId: Long,
    val albumId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationSec: Double,
    val folderPath: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val formatLabel: String,
    val addedAtMs: Long,
    val contentUriString: String,
    val isFavorite: Boolean = false,
    val exclusionReason: String? = null,
    val matchedRule: String? = null,
    val manuallyRestored: Boolean = false
)

data class Playlist(
    val id: String,
    val name: String,
    val trackIds: List<String>,
    val createdAtMs: Long
)

data class AppSettings(
    val minDurationSeconds: Int = 30,
    val autoExcludeRecordings: Boolean = true,
    val dynamicArtworkAccent: Boolean = true,
    val artworkBackgroundEnabled: Boolean = true
)

data class TrackPalette(
    val accent: Color,
    val glow: Color,
    val secondary: Color
)

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class TrackLyrics(
    val trackId: String,
    val syncedLines: List<LyricLine>,
    val plainLyrics: String,
    val offsetMs: Long = 0L,
    val source: String = "lrclib"
)

data class LrcSearchResult(
    val id: Long,
    val trackName: String,
    val artistName: String,
    val syncedLyrics: String?,
    val plainLyrics: String?
)

object SmartFilter {
    private val recordingKeywords = listOf(
        "recording", "record", "voice", "call", "rec_", "rec-", "ptt",
        "whatsapp audio", "telegram audio", "sound_recorder", "callrec",
        "تسجيل", "مكالمة", "صوت", "واتساب"
    )

    fun evaluate(
        fileName: String,
        folderPath: String,
        durationSec: Double,
        manuallyRestored: Boolean,
        manuallyExcluded: Boolean,
        settings: AppSettings
    ): Pair<String?, String?> {
        if (manuallyExcluded) return "recording" to "Manual"
        if (manuallyRestored) return null to null

        if (settings.minDurationSeconds > 0 && durationSec > 0 && durationSec < settings.minDurationSeconds) {
            return "short" to "أقل من ${settings.minDurationSeconds} ثانية"
        }

        if (settings.autoExcludeRecordings) {
            val haystack = "$fileName $folderPath".lowercase()
            val matched = recordingKeywords.firstOrNull { haystack.contains(it) }
            if (matched != null) {
                return "recording" to matched
            }
        }
        return null to null
    }
}
