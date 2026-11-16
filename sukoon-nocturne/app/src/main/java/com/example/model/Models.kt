package com.example.model

enum class AppLanguage { AR, EN }

enum class ArtworkBgMode { FULL, PARTIAL, OFF }

enum class MainTab { LIBRARY, ARTISTS, SETTINGS }

enum class LibrarySubTab { ALL_SONGS, PLAYLISTS, FAVORITES, QUARANTINE }

enum class SortOption { TITLE, ARTIST, DURATION, RECENT }

enum class RepeatMode { OFF, ALL, ONE }

enum class ExclusionReason { NONE, RECORDING, SHORT_CLIP, KEYWORD, MANUAL, FOLDER }

data class Track(
    val id: String,
    val contentUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSec: Double,
    val fileName: String,
    val folderPath: String,
    val folderName: String,
    val sizeBytes: Long,
    val mimeType: String,
    val dateAdded: Long,
    val albumId: Long,
    val artUri: String? = null,
    val accentColor: Int = 0xFFF5F5F5.toInt(),
    val secondaryColor: Int = 0xFF94A3B8.toInt(),
    val isExcluded: Boolean = false,
    val exclusionReason: ExclusionReason = ExclusionReason.NONE
)

data class Playlist(
    val id: String,
    val name: String,
    val trackIds: List<String>,
    val createdAt: Long = System.currentTimeMillis()
)

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class LyricsData(
    val syncedLyrics: String? = null,
    val plainLyrics: String? = null,
    val lines: List<LyricLine> = emptyList(),
    val sourceName: String = ""
)

data class LrcSearchResult(
    val id: Long,
    val trackName: String,
    val artistName: String,
    val albumName: String,
    val duration: Double,
    val syncedLyrics: String?,
    val plainLyrics: String?
)

data class Artist(
    val name: String,
    val trackCount: Int,
    val albumCount: Int,
    val artUri: String? = null,
    val trackIds: List<String> = emptyList()
)

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val trackCount: Int,
    val artUri: String? = null,
    val trackIds: List<String> = emptyList()
)

data class FolderItem(
    val path: String,
    val name: String,
    val trackCount: Int,
    val isExcluded: Boolean = false
)

data class AppSettings(
    val language: AppLanguage = AppLanguage.AR,
    val crossfadeEnabled: Boolean = true,
    val crossfadeSec: Int = 4,
    val autoExcludeRecordings: Boolean = true,
    val minDurationSec: Int = 60,
    val customKeywords: List<String> = emptyList(),
    val excludedFolderPaths: Set<String> = emptySet(),
    val dynamicAccent: Boolean = true,
    val artworkBgMode: ArtworkBgMode = ArtworkBgMode.FULL,
    val glassBlurEnabled: Boolean = true,
    val lyricOffsetMs: Long = 0L
)
