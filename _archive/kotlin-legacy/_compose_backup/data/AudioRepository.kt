package com.example.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.palette.graphics.Palette
import com.example.model.AppSettings
import com.example.model.ExclusionReason
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AudioRepository(private val context: Context) {

    private val recordingRegex = Regex(
        """(call|rec|record|recording|voice|voicenote|audio_note|whatsapp|telegram|ptt|تسجيل|مكالمة|ملاحظة|صوتية|رسالة|فويس|aud-|wa0)""",
        RegexOption.IGNORE_CASE
    )

    suspend fun scanDeviceTracks(
        settings: AppSettings,
        manualExcludedIds: Set<String>,
        manualRestoredIds: Set<String>
    ): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%'"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val artCacheDir = File(context.cacheDir, "art_cache_hd").apply { mkdirs() }

        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val mediaId = cursor.getLong(idCol)
                val id = mediaId.toString()
                val rawTitle = cursor.getString(titleCol).orEmpty()
                val rawArtist = cursor.getString(artistCol).orEmpty()
                val rawAlbum = cursor.getString(albumCol).orEmpty()
                val durationMs = cursor.getLong(durationCol)
                val durationSec = durationMs / 1000.0
                val fileName = cursor.getString(nameCol).orEmpty().ifBlank { "$rawTitle.mp3" }
                val filePath = cursor.getString(dataCol).orEmpty()
                val sizeBytes = cursor.getLong(sizeCol)
                val mimeType = cursor.getString(mimeCol).orEmpty()
                val dateAdded = cursor.getLong(dateCol)
                val albumId = cursor.getLong(albumIdCol)

                if (sizeBytes <= 0L) continue

                val parentFile = if (filePath.isNotBlank()) File(filePath).parentFile else null
                val folderPath = parentFile?.absolutePath ?: "/"
                val folderName = parentFile?.name ?: "Music"

                val title = rawTitle.ifBlank { fileName.substringBeforeLast('.') }
                val artist = if (rawArtist.isBlank() || rawArtist.equals("<unknown>", true)) "فنان غير معروف" else rawArtist
                val album = if (rawAlbum.isBlank() || rawAlbum.equals("<unknown>", true)) "ألبوم غير محدد" else rawAlbum

                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId).toString()

                val reason = computeExclusionReason(
                    id = id,
                    title = title,
                    fileName = fileName,
                    folderPath = folderPath,
                    durationSec = durationSec,
                    settings = settings,
                    manualExcludedIds = manualExcludedIds,
                    manualRestoredIds = manualRestoredIds
                )

                val cachedArt = File(artCacheDir, "album_${albumId}.jpg")
                val artUri = if (cachedArt.exists() && cachedArt.length() > 0L) {
                    Uri.fromFile(cachedArt).toString()
                } else {
                    ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString()
                }

                tracks.add(
                    Track(
                        id = id,
                        contentUri = contentUri,
                        title = title,
                        artist = artist,
                        album = album,
                        durationSec = durationSec,
                        fileName = fileName,
                        folderPath = folderPath,
                        folderName = folderName,
                        sizeBytes = sizeBytes,
                        mimeType = mimeType,
                        dateAdded = dateAdded,
                        albumId = albumId,
                        artUri = artUri,
                        isExcluded = reason != ExclusionReason.NONE,
                        exclusionReason = reason
                    )
                )
            }
        }
        tracks
    }

    fun computeExclusionReason(
        id: String,
        title: String,
        fileName: String,
        folderPath: String,
        durationSec: Double,
        settings: AppSettings,
        manualExcludedIds: Set<String>,
        manualRestoredIds: Set<String>
    ): ExclusionReason {
        if (manualExcludedIds.contains(id)) return ExclusionReason.MANUAL
        if (manualRestoredIds.contains(id)) return ExclusionReason.NONE

        val combined = "$title $fileName $folderPath"
        if (settings.autoExcludeRecordings && recordingRegex.containsMatchIn(combined)) {
            return ExclusionReason.RECORDING
        }
        if (settings.minDurationSec > 0 && durationSec < settings.minDurationSec) {
            return ExclusionReason.SHORT_CLIP
        }
        if (settings.customKeywords.any { kw -> kw.isNotBlank() && combined.contains(kw.trim(), ignoreCase = true) }) {
            return ExclusionReason.KEYWORD
        }
        return ExclusionReason.NONE
    }

    suspend fun loadHdArtworkAndColors(track: Track): Triple<String?, Int, Int> = withContext(Dispatchers.IO) {
        val artCacheDir = File(context.cacheDir, "art_cache_hd").apply { mkdirs() }
        val cachedFile = File(artCacheDir, "album_${track.albumId}.jpg")
        var bitmap: Bitmap? = null

        if (cachedFile.exists() && cachedFile.length() > 0L) {
            bitmap = BitmapFactory.decodeFile(cachedFile.absolutePath)
        } else {
            bitmap = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(
                        Uri.parse(track.contentUri),
                        Size(720, 720),
                        null
                    )
                } else {
                    val artUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        track.albumId
                    )
                    context.contentResolver.openInputStream(artUri)?.use { BitmapFactory.decodeStream(it) }
                }
            }.getOrNull()

            if (bitmap != null) {
                runCatching {
                    FileOutputStream(cachedFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }
                }
            }
        }

        if (bitmap == null) {
            return@withContext Triple(null, 0xFFF5F5F5.toInt(), 0xFF64748B.toInt())
        }

        val palette = Palette.from(bitmap).generate()
        val accent = palette.getLightVibrantColor(
            palette.getVibrantColor(
                palette.getDominantColor(0xFFF5F5F5.toInt())
            )
        )
        val secondary = palette.getMutedColor(
            palette.getDarkVibrantColor(accent)
        )
        Triple(Uri.fromFile(cachedFile).toString(), accent, secondary)
    }
}
