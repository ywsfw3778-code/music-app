package com.example

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MusicRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("sukoon_native_prefs", Context.MODE_PRIVATE)

    private val bitmapCache = object : LruCache<Long, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: Long, value: Bitmap): Int = value.byteCount.coerceAtLeast(1)
    }
    private val paletteCache = LruCache<String, TrackPalette>(256)
    private val emptyArtMarkers = mutableSetOf<Long>()

    fun getSortBy(): SortBy = SortBy.fromKey(prefs.getString("sort_by", SortBy.RECENT.key))

    fun saveSortBy(sortBy: SortBy) {
        prefs.edit().putString("sort_by", sortBy.key).apply()
    }

    fun getSettings(): AppSettings {
        val raw = prefs.getString("settings_json", null) ?: return AppSettings()
        return try {
            val obj = JSONObject(raw)
            AppSettings(
                minDurationSeconds = obj.optInt("minDurationSeconds", 30),
                autoExcludeRecordings = obj.optBoolean("autoExcludeRecordings", true),
                dynamicArtworkAccent = obj.optBoolean("dynamicArtworkAccent", true),
                artworkBackgroundEnabled = obj.optBoolean("artworkBackgroundEnabled", true)
            )
        } catch (_: Exception) {
            AppSettings()
        }
    }

    fun saveSettings(settings: AppSettings) {
        val obj = JSONObject().apply {
            put("minDurationSeconds", settings.minDurationSeconds)
            put("autoExcludeRecordings", settings.autoExcludeRecordings)
            put("dynamicArtworkAccent", settings.dynamicArtworkAccent)
            put("artworkBackgroundEnabled", settings.artworkBackgroundEnabled)
        }
        prefs.edit().putString("settings_json", obj.toString()).apply()
    }

    fun getFavoriteIds(): Set<String> = prefs.getStringSet("favorite_ids", emptySet())?.toSet() ?: emptySet()
    fun saveFavoriteIds(ids: Set<String>) {
        prefs.edit().putStringSet("favorite_ids", ids).apply()
    }

    fun getRestoredIds(): Set<String> = prefs.getStringSet("restored_ids", emptySet())?.toSet() ?: emptySet()
    fun saveRestoredIds(ids: Set<String>) {
        prefs.edit().putStringSet("restored_ids", ids).apply()
    }

    fun getManualExcludedIds(): Set<String> = prefs.getStringSet("manual_excluded_ids", emptySet())?.toSet() ?: emptySet()
    fun saveManualExcludedIds(ids: Set<String>) {
        prefs.edit().putStringSet("manual_excluded_ids", ids).apply()
    }

    fun getPlaylists(): List<Playlist> {
        val raw = prefs.getString("playlists_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val trackArr = obj.optJSONArray("trackIds") ?: JSONArray()
                    val trackIds = buildList {
                        for (j in 0 until trackArr.length()) add(trackArr.getString(j))
                    }
                    add(
                        Playlist(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            trackIds = trackIds,
                            createdAtMs = obj.optLong("createdAtMs", System.currentTimeMillis())
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun savePlaylists(playlists: List<Playlist>) {
        val arr = JSONArray()
        for (p in playlists) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("createdAtMs", p.createdAtMs)
                put("trackIds", JSONArray(p.trackIds))
            }
            arr.put(obj)
        }
        prefs.edit().putString("playlists_json", arr.toString()).apply()
    }

    fun clearAllUserData() {
        prefs.edit()
            .remove("playlists_json")
            .remove("favorite_ids")
            .remove("restored_ids")
            .remove("manual_excluded_ids")
            .apply()
    }

    @Suppress("DEPRECATION")
    suspend fun scanDeviceAudio(settings: AppSettings): List<Track> = withContext(Dispatchers.IO) {
        val favIds = getFavoriteIds()
        val restoredIds = getRestoredIds()
        val manualExcludedIds = getManualExcludedIds()

        val result = mutableListOf<Track>()
        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection.add(MediaStore.Audio.Media.RELATIVE_PATH)
        }

        val selection = "${MediaStore.Audio.Media.DURATION} > 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection.toTypedArray(),
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            } else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val durationMs = cursor.getLong(durationCol)
                if (durationMs <= 0L) continue

                val displayName = cursor.getString(nameCol) ?: "track_$id.mp3"
                val rawTitle = cursor.getString(titleCol)?.trim().orEmpty()
                val cleanTitle = if (rawTitle.isNotEmpty() && rawTitle != "<unknown>") {
                    rawTitle
                } else {
                    displayName.substringBeforeLast('.')
                }

                val rawArtist = cursor.getString(artistCol)?.trim().orEmpty()
                val cleanArtist = if (rawArtist.isNotEmpty() && rawArtist != "<unknown>") {
                    rawArtist
                } else {
                    "فنان غير معروف"
                }

                val rawAlbum = cursor.getString(albumCol)?.trim().orEmpty()
                val cleanAlbum = if (rawAlbum.isNotEmpty() && rawAlbum != "<unknown>") {
                    rawAlbum
                } else {
                    "سُكون"
                }

                val albumId = cursor.getLong(albumIdCol)
                val mimeType = cursor.getString(mimeCol) ?: "audio/mpeg"
                val fileSize = cursor.getLong(sizeCol)
                val dateAddedSec = cursor.getLong(dateCol)

                val relPath = if (relPathCol >= 0) cursor.getString(relPathCol) else null
                val fullDataPath = if (dataCol >= 0) cursor.getString(dataCol) else null
                val folderPath = when {
                    !relPath.isNullOrBlank() -> relPath.trimEnd('/', '\\')
                    !fullDataPath.isNullOrBlank() -> File(fullDataPath).parentFile?.name ?: "Music"
                    else -> "Music"
                }

                val ext = displayName.substringAfterLast('.', "mp3").uppercase()
                val trackId = "ms_$id"
                val durationSec = durationMs / 1000.0
                val isRestored = restoredIds.contains(trackId)
                val isManualExcluded = manualExcludedIds.contains(trackId)
                val (reason, rule) = SmartFilter.evaluate(
                    fileName = displayName,
                    folderPath = folderPath,
                    durationSec = durationSec,
                    manuallyRestored = isRestored,
                    manuallyExcluded = isManualExcluded,
                    settings = settings
                )

                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                result.add(
                    Track(
                        id = trackId,
                        mediaStoreId = id,
                        albumId = albumId,
                        title = cleanTitle,
                        artist = cleanArtist,
                        album = cleanAlbum,
                        durationSec = durationSec,
                        folderPath = folderPath,
                        fileName = displayName,
                        mimeType = mimeType,
                        fileSize = fileSize,
                        formatLabel = ext,
                        addedAtMs = if (dateAddedSec > 0) dateAddedSec * 1000L else System.currentTimeMillis(),
                        contentUriString = contentUri.toString(),
                        isFavorite = favIds.contains(trackId),
                        exclusionReason = reason,
                        matchedRule = rule,
                        manuallyRestored = isRestored
                    )
                )
            }
        }
        result
    }

    suspend fun loadArtworkBitmap(mediaId: Long, albumId: Long): Bitmap? = withContext(Dispatchers.IO) {
        bitmapCache.get(mediaId)?.let { return@withContext it }
        if (emptyArtMarkers.contains(mediaId)) return@withContext null

        val cacheFolder = File(context.cacheDir, "art_cache").apply { mkdirs() }
        val artFile = File(cacheFolder, "art_$mediaId.jpg")
        val noneFile = File(cacheFolder, "art_$mediaId.none")

        if (artFile.exists() && artFile.length() > 0) {
            val bmp = BitmapFactory.decodeFile(artFile.absolutePath)
            if (bmp != null) {
                bitmapCache.put(mediaId, bmp)
                return@withContext bmp
            }
        }
        if (noneFile.exists()) {
            emptyArtMarkers.add(mediaId)
            return@withContext null
        }

        val audioUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId)
        var rawBytes: ByteArray? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val bmp = context.contentResolver.loadThumbnail(audioUri, Size(320, 320), null)
                bitmapCache.put(mediaId, bmp)
                val bos = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 82, bos)
                try {
                    artFile.writeBytes(bos.toByteArray())
                } catch (_: Exception) {
                }
                return@withContext bmp
            } catch (_: Exception) {
            }
        }

        if (albumId > 0) {
            try {
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )
                context.contentResolver.openInputStream(albumArtUri)?.use { rawBytes = it.readBytes() }
            } catch (_: Exception) {
            }
        }

        if (rawBytes == null || rawBytes!!.isEmpty()) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, audioUri)
                rawBytes = retriever.embeddedPicture
            } catch (_: Exception) {
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }
        }

        val bytes = rawBytes
        if (bytes != null && bytes.isNotEmpty()) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val maxDim = max(bounds.outWidth, bounds.outHeight)
            var sample = 1
            while (maxDim / (sample * 2) >= 240) sample *= 2
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            if (bmp != null) {
                bitmapCache.put(mediaId, bmp)
                try {
                    val bos = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, 82, bos)
                    artFile.writeBytes(bos.toByteArray())
                } catch (_: Exception) {
                }
                return@withContext bmp
            }
        }

        emptyArtMarkers.add(mediaId)
        try {
            noneFile.createNewFile()
        } catch (_: Exception) {
        }
        null
    }

    fun getArtworkBytesForMediaSession(mediaId: Long): ByteArray? {
        val artFile = File(File(context.cacheDir, "art_cache"), "art_$mediaId.jpg")
        if (artFile.exists() && artFile.length() > 0) {
            return try {
                artFile.readBytes()
            } catch (_: Exception) {
                null
            }
        }
        val bmp = bitmapCache.get(mediaId) ?: return null
        val bos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 85, bos)
        return bos.toByteArray()
    }

    fun extractPalette(trackId: String, bitmap: Bitmap?, fallbackTitle: String): TrackPalette {
        paletteCache.get(trackId)?.let { return it }
        if (bitmap == null) {
            val fb = fallbackPalette(fallbackTitle)
            paletteCache.put(trackId, fb)
            return fb
        }

        val scaled = Bitmap.createScaledBitmap(bitmap, 24, 24, true)
        val bucketWeight = FloatArray(12)
        val bucketR = FloatArray(12)
        val bucketG = FloatArray(12)
        val bucketB = FloatArray(12)

        val hsv = FloatArray(3)
        for (y in 0 until scaled.height) {
            for (x in 0 until scaled.width) {
                val px = scaled.getPixel(x, y)
                val r = android.graphics.Color.red(px)
                val g = android.graphics.Color.green(px)
                val b = android.graphics.Color.blue(px)
                android.graphics.Color.RGBToHSV(r, g, b, hsv)
                val sat = hsv[1]
                val v = hsv[2]
                if (v < 0.08f || v > 0.96f || sat < 0.12f) continue
                val bucket = ((hsv[0] / 30f).toInt()) % 12
                val w = (sat + 0.15f) * (sat + 0.15f) * (1f - abs(v - 0.55f) * 0.7f)
                bucketWeight[bucket] += w
                bucketR[bucket] += r * w
                bucketG[bucket] += g * w
                bucketB[bucket] += b * w
            }
        }
        if (scaled !== bitmap) scaled.recycle()

        var bestIdx = -1
        var secondIdx = -1
        for (i in 0 until 12) {
            if (bestIdx == -1 || bucketWeight[i] > bucketWeight[bestIdx]) {
                secondIdx = bestIdx
                bestIdx = i
            } else if (secondIdx == -1 || bucketWeight[i] > bucketWeight[secondIdx]) {
                secondIdx = i
            }
        }

        if (bestIdx == -1 || bucketWeight[bestIdx] <= 0.01f) {
            val fb = fallbackPalette(fallbackTitle)
            paletteCache.put(trackId, fb)
            return fb
        }

        val w1 = bucketWeight[bestIdx]
        android.graphics.Color.RGBToHSV(
            (bucketR[bestIdx] / w1).toInt().coerceIn(0, 255),
            (bucketG[bestIdx] / w1).toInt().coerceIn(0, 255),
            (bucketB[bestIdx] / w1).toInt().coerceIn(0, 255),
            hsv
        )
        val hue1 = hsv[0]
        val sat1 = min(0.92f, max(0.55f, hsv[1] * 1.2f))

        val accent = Color.hsl(hue1, sat1, 0.68f)
        val glow = Color.hsl(hue1, min(0.95f, sat1 * 1.05f), 0.45f)

        val hue2 = if (secondIdx != -1 && bucketWeight[secondIdx] > w1 * 0.2f) {
            val w2 = bucketWeight[secondIdx]
            android.graphics.Color.RGBToHSV(
                (bucketR[secondIdx] / w2).toInt().coerceIn(0, 255),
                (bucketG[secondIdx] / w2).toInt().coerceIn(0, 255),
                (bucketB[secondIdx] / w2).toInt().coerceIn(0, 255),
                hsv
            )
            hsv[0]
        } else {
            (hue1 + 36f) % 360f
        }
        val secondary = Color.hsl(hue2, max(0.50f, sat1 * 0.9f), 0.52f)

        val palette = TrackPalette(accent = accent, glow = glow, secondary = secondary)
        paletteCache.put(trackId, palette)
        return palette
    }

    fun fallbackPalette(key: String): TrackPalette {
        val hash = abs(key.hashCode())
        val hue = (hash % 360).toFloat()
        return TrackPalette(
            accent = Color.hsl(hue, 0.72f, 0.68f),
            glow = Color.hsl(hue, 0.78f, 0.44f),
            secondary = Color.hsl((hue + 38f) % 360f, 0.65f, 0.50f)
        )
    }

    suspend fun searchLyrics(query: String): List<LrcSearchResult> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext emptyList()
        val encoded = URLEncoder.encode(q, "UTF-8")
        val url = URL("https://lrclib.net/api/search?q=$encoded")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("User-Agent", "SukoonNocturne/1.0")
        }
        try {
            if (conn.responseCode !in 200..299) return@withContext emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(body)
            buildList {
                for (i in 0 until min(15, arr.length())) {
                    val obj = arr.getJSONObject(i)
                    add(
                        LrcSearchResult(
                            id = obj.optLong("id", i.toLong()),
                            trackName = obj.optString("trackName", ""),
                            artistName = obj.optString("artistName", ""),
                            syncedLyrics = obj.optString("syncedLyrics", "").takeIf { it.isNotBlank() && it != "null" },
                            plainLyrics = obj.optString("plainLyrics", "").takeIf { it.isNotBlank() && it != "null" }
                        )
                    )
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    fun parseLrc(lrcText: String): List<LyricLine> {
        val regex = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
        val lines = mutableListOf<LyricLine>()
        for (rawLine in lrcText.lines()) {
            val matches = regex.findAll(rawLine).toList()
            if (matches.isEmpty()) continue
            val text = rawLine.replace(regex, "").trim()
            if (text.isEmpty()) continue
            for (m in matches) {
                val min = m.groupValues[1].toLongOrNull() ?: 0L
                val sec = m.groupValues[2].toLongOrNull() ?: 0L
                val fracStr = m.groupValues[3]
                val ms = when (fracStr.length) {
                    1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                    2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                    3 -> fracStr.toLongOrNull() ?: 0L
                    else -> 0L
                }
                lines.add(LyricLine(timeMs = min * 60_000L + sec * 1_000L + ms, text = text))
            }
        }
        return lines.sortedBy { it.timeMs }
    }
}
