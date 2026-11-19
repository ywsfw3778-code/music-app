package com.example.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.model.AppSettings
import com.example.model.ExclusionReason
import com.example.model.FolderItem
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

class AudioRepository(private val context: Context) {

    companion object {
        private val artSemaphore = Semaphore(4)
        private val artMemoryCache = ConcurrentHashMap<String, Triple<String?, Int, Int>>()
        private val resolvedArtCache = ConcurrentHashMap<String, String>()

        suspend fun resolveArtUri(context: Context, rawUri: String?): String? {
            if (rawUri.isNullOrBlank()) return null
            if (!rawUri.startsWith("track_art://")) return rawUri
            resolvedArtCache[rawUri]?.let { cached -> return cached.ifEmpty { null } }
            return withContext(Dispatchers.IO) {
                artSemaphore.withPermit {
                    val parsed = Uri.parse(rawUri)
                    val trackId = parsed.host ?: return@withPermit null
                    val contentUri = parsed.getQueryParameter("uri").orEmpty()
                    val filePath = parsed.getQueryParameter("path").orEmpty()
                    val resolved = extractOrGetTrackArtFile(context, trackId, contentUri, filePath)?.let {
                        Uri.fromFile(it).toString()
                    }
                    resolvedArtCache[rawUri] = resolved ?: ""
                    resolved
                }
            }
        }

        fun extractOrGetTrackArtFile(
            context: Context,
            trackId: String,
            contentUri: String,
            filePath: String
        ): File? {
            val artCacheDir = File(context.filesDir, "track_art_v2").apply { mkdirs() }
            val cachedArt = File(artCacheDir, "track_${trackId}.jpg")
            val noArtMarker = File(artCacheDir, "track_${trackId}.none")

            if (cachedArt.exists() && cachedArt.length() > 0L) return cachedArt
            if (noArtMarker.exists()) return null

            val embeddedBytes = runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    if (filePath.isNotBlank() && File(filePath).exists()) {
                        retriever.setDataSource(filePath)
                    } else if (contentUri.isNotBlank()) {
                        retriever.setDataSource(context, Uri.parse(contentUri))
                    } else {
                        return@runCatching null
                    }
                    retriever.embeddedPicture
                } finally {
                    runCatching { retriever.release() }
                }
            }.getOrNull()

            if (embeddedBytes != null && embeddedBytes.isNotEmpty()) {
                runCatching {
                    FileOutputStream(cachedArt).use { out ->
                        out.write(embeddedBytes)
                    }
                }
                if (cachedArt.exists() && cachedArt.length() > 0L) {
                    saveCoverToTrackFolder(filePath, cachedArt)
                    return cachedArt
                }
            }

            runCatching { noArtMarker.createNewFile() }
            return null
        }

        private fun saveCoverToTrackFolder(filePath: String, artFile: File) {
            if (filePath.isBlank()) return
            runCatching {
                val parent = File(filePath).parentFile ?: return@runCatching
                if (parent.exists() && parent.canWrite()) {
                    val destCover = File(parent, "cover.jpg")
                    if (!destCover.exists() || destCover.length() == 0L) {
                        artFile.copyTo(destCover, overwrite = false)
                    }
                }
            }
        }
    }

    private val recordingRegex = Regex(
        """(call_?rec|call\s*record|record(ing)?|voice_?note|audio_?note|whatsapp\s*(audio|voice)|telegram\s*voice|ptt|تسجيل|مكالمة|ملاحظة\s*صوتية|فويس|aud-\d+|wa0\d+)""",
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

        val artCacheDir = File(context.filesDir, "track_art_v2").apply { mkdirs() }

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

                val (artist, title) = ArtistNormalizer.resolveArtistAndTitle(rawTitle, rawArtist, fileName)
                val album = if (rawAlbum.isBlank() || rawAlbum.equals("<unknown>", true)) "Unknown Album" else rawAlbum

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

                val cachedArt = File(artCacheDir, "track_${id}.jpg")
                val noArtMarker = File(artCacheDir, "track_${id}.none")
                val safeKey = "${artist}_${album.trim()}".replace(Regex("[^a-zA-Z0-9_\\-\\u0600-\\u06FF]"), "_")
                val cachedAlbumArt = File(artCacheDir, "album_${safeKey}.jpg")
                val isValidAlbum = !ArtistNormalizer.isFakeOrJunkAlbum(album, artist, 2, title, fileName)
                val artUri = when {
                    cachedArt.exists() && cachedArt.length() > 0L -> Uri.fromFile(cachedArt).toString()
                    noArtMarker.exists() && isValidAlbum && cachedAlbumArt.exists() && cachedAlbumArt.length() > 0L ->
                        Uri.fromFile(cachedAlbumArt).toString()
                    else -> "track_art://$id?uri=${Uri.encode(contentUri)}&path=${Uri.encode(filePath)}"
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
        saveTracksToCache(tracks)
        tracks
    }

    fun saveTracksToCache(tracks: List<Track>) {
        runCatching {
            val file = File(context.filesDir, "tracks_cache_v2.json")
            val array = JSONArray()
            for (t in tracks) {
                val obj = JSONObject()
                obj.put("id", t.id)
                obj.put("uri", t.contentUri)
                obj.put("title", t.title)
                obj.put("artist", t.artist)
                obj.put("album", t.album)
                obj.put("dur", t.durationSec)
                obj.put("name", t.fileName)
                obj.put("path", t.folderPath)
                obj.put("fname", t.folderName)
                obj.put("size", t.sizeBytes)
                obj.put("mime", t.mimeType)
                obj.put("date", t.dateAdded)
                obj.put("albId", t.albumId)
                if (t.artUri != null) obj.put("art", t.artUri)
                array.put(obj)
            }
            file.writeText(array.toString())
        }
    }

    fun loadCachedTracks(): List<Track> {
        return runCatching {
            val file = File(context.filesDir, "tracks_cache_v2.json")
            if (!file.exists() || file.length() == 0L) return emptyList()
            val content = file.readText()
            val array = JSONArray(content)
            val list = ArrayList<Track>(array.length())
            val artCacheDir = File(context.filesDir, "track_art_v2")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val rawTitle = obj.getString("title")
                val rawArtist = obj.getString("artist")
                val fileName = obj.getString("name")
                val (artist, title) = ArtistNormalizer.resolveArtistAndTitle(rawTitle, rawArtist, fileName)
                val rawAlbum = obj.getString("album")
                val trackId = obj.getString("id")
                val cachedFile = File(artCacheDir, "track_${trackId}.jpg")
                val noArtMarker = File(artCacheDir, "track_${trackId}.none")
                val safeKey = "${artist}_${rawAlbum.trim()}".replace(Regex("[^a-zA-Z0-9_\\-\\u0600-\\u06FF]"), "_")
                val cachedAlbumArt = File(artCacheDir, "album_${safeKey}.jpg")
                val isValidAlbum = !ArtistNormalizer.isFakeOrJunkAlbum(rawAlbum, artist, 2, title, fileName)
                var art = obj.optString("art").takeIf { !it.isNullOrBlank() }
                if (cachedFile.exists() && cachedFile.length() > 0L) {
                    art = Uri.fromFile(cachedFile).toString()
                } else if (art == null || art.startsWith("track_art://")) {
                    if (noArtMarker.exists() && isValidAlbum && cachedAlbumArt.exists() && cachedAlbumArt.length() > 0L) {
                        art = Uri.fromFile(cachedAlbumArt).toString()
                    }
                } else if (!File(Uri.parse(art).path ?: "").exists()) {
                    art = null
                }

                list.add(
                    Track(
                        id = trackId,
                        contentUri = obj.getString("uri"),
                        title = title,
                        artist = artist,
                        album = rawAlbum,
                        durationSec = obj.getDouble("dur"),
                        fileName = fileName,
                        folderPath = obj.getString("path"),
                        folderName = obj.getString("fname"),
                        sizeBytes = obj.getLong("size"),
                        mimeType = obj.getString("mime"),
                        dateAdded = obj.getLong("date"),
                        albumId = obj.getLong("albId"),
                        artUri = art
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    fun getFolderItems(tracks: List<Track>, excludedPaths: Set<String>): List<FolderItem> {
        return tracks.groupBy { it.folderPath }
            .map { (path, list) ->
                val name = list.firstOrNull()?.folderName?.ifBlank { "Music" } ?: "Music"
                FolderItem(
                    path = path,
                    name = name,
                    trackCount = list.size,
                    isExcluded = excludedPaths.contains(path)
                )
            }
            .sortedWith(compareByDescending<FolderItem> { it.trackCount }.thenBy { it.name })
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
        if (settings.excludedFolderPaths.contains(folderPath)) return ExclusionReason.FOLDER

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
        artMemoryCache[track.id]?.let { return@withContext it }

        val fullPath = File(track.folderPath, track.fileName).absolutePath
        var artFile = extractOrGetTrackArtFile(
            context = context,
            trackId = track.id,
            contentUri = track.contentUri,
            filePath = fullPath
        )

        if ((artFile == null || !artFile.exists()) && track.title.isNotBlank()) {
            val artCacheDir = File(context.filesDir, "track_art_v2").apply { mkdirs() }
            val cachedArt = File(artCacheDir, "track_${track.id}.jpg")
            val fetched = OnlineMetadataResolver.resolveTrackArtOnline(
                context = context,
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                targetFile = cachedArt
            )
            if (fetched && cachedArt.exists() && cachedArt.length() > 0L) {
                artFile = cachedArt
                File(artCacheDir, "track_${track.id}.none").delete()
            } else {
                val normArtist = ArtistNormalizer.normalizeArtist(track.artist)
                val safeKey = "${normArtist}_${track.album.trim()}".replace(Regex("[^a-zA-Z0-9_\\-\\u0600-\\u06FF]"), "_")
                val albumFallback = File(artCacheDir, "album_${safeKey}.jpg")
                if (!ArtistNormalizer.isFakeOrJunkAlbum(track.album, normArtist, 2, track.title, track.fileName) &&
                    albumFallback.exists() && albumFallback.length() > 0L
                ) {
                    artFile = albumFallback
                }
            }
        }

        val bitmap: Bitmap? = if (artFile != null && artFile.exists()) {
            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
            BitmapFactory.decodeFile(artFile.absolutePath, opts)
        } else {
            null
        }

        if (bitmap == null || artFile == null) {
            val fallback = Triple(null, 0xFFF5F5F5.toInt(), 0xFF1C1C22.toInt())
            artMemoryCache[track.id] = fallback
            return@withContext fallback
        }

        val (accent, _, secondary) = extractCoverColors(bitmap)
        bitmap.recycle()
        val result = Triple(Uri.fromFile(artFile).toString(), accent, secondary)
        artMemoryCache[track.id] = result
        result
    }

    private fun extractCoverColors(bitmap: Bitmap): Triple<Int, Int, Int> {
        val size = 36
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val pixels = IntArray(size * size)
        scaled.getPixels(pixels, 0, size, 0, 0, size, size)
        if (scaled !== bitmap) scaled.recycle()

        class Bucket(var weight: Float = 0f, var r: Float = 0f, var g: Float = 0f, var b: Float = 0f)
        val buckets = Array(24) { Bucket() }
        var neutralWeight = 0f
        var whiteWeight = 0f
        var totalColorWeight = 0f

        val half = size / 2f
        for (y in 0 until size) {
            val dy = (y - half) / half
            for (x in 0 until size) {
                val dx = (x - half) / half
                val distSq = (dx * dx + dy * dy).coerceIn(0f, 2f)
                val centerWeight = 1.25f - 0.25f * distSq

                val px = pixels[y * size + x]
                val r = (px shr 16) and 0xFF
                val g = (px shr 8) and 0xFF
                val b = px and 0xFF
                val maxC = maxOf(r, g, b)
                val minC = minOf(r, g, b)
                val chroma = maxC - minC
                val (h, s, l) = rgbToHsl(r, g, b)

                if (l >= 0.70f && s <= 0.25f) {
                    whiteWeight += centerWeight
                    neutralWeight += centerWeight
                    continue
                }
                if (l < 0.02f || l > 0.98f) {
                    neutralWeight += centerWeight
                    continue
                }
                val isDarkVibrant = l in 0.025f..0.30f && chroma >= 10
                if ((chroma < 14 || s < 0.10f) && !isDarkVibrant) {
                    neutralWeight += centerWeight
                    continue
                }

                val idx = ((h / 15f).toInt()).coerceIn(0, 23)
                val lWeight = (1f - kotlin.math.abs(l - 0.50f) * 0.65f).coerceIn(0.35f, 1f)
                val weight = centerWeight * (0.45f + 0.55f * s) * lWeight
                buckets[idx].weight += weight
                buckets[idx].r += r * weight
                buckets[idx].g += g * weight
                buckets[idx].b += b * weight
                totalColorWeight += weight
            }
        }

        val totalPixelArea = size * size.toFloat()
        if (whiteWeight > totalPixelArea * 0.30f && whiteWeight > totalColorWeight * 1.1f) {
            return Triple(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFCBD5E1.toInt())
        }

        if (totalColorWeight <= 0.01f || totalColorWeight < neutralWeight * 0.05f) {
            return Triple(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFCBD5E1.toInt())
        }

        var bestIdx = 0
        var bestClusterWeight = -1f
        for (i in 0 until 24) {
            val prev = buckets[(i + 23) % 24].weight * 0.5f
            val curr = buckets[i].weight
            val next = buckets[(i + 1) % 24].weight * 0.5f
            val sum = prev + curr + next
            if (sum > bestClusterWeight) {
                bestClusterWeight = sum
                bestIdx = i
            }
        }

        if (bestClusterWeight < 2.0f) {
            return Triple(0xFFF1F5F9.toInt(), 0xFFFFFFFF.toInt(), 0xFF1C1C22.toInt())
        }

        fun clusterRgb(centerIdx: Int): Triple<Int, Int, Int> {
            var w = 0f
            var rSum = 0f
            var gSum = 0f
            var bSum = 0f
            for (offset in -1..1) {
                val factor = if (offset == 0) 1f else 0.5f
                val b = buckets[(centerIdx + offset + 24) % 24]
                w += b.weight * factor
                rSum += b.r * factor
                gSum += b.g * factor
                bSum += b.b * factor
            }
            if (w <= 0.001f) return Triple(240, 240, 240)
            return Triple(
                (rSum / w).toInt().coerceIn(0, 255),
                (gSum / w).toInt().coerceIn(0, 255),
                (bSum / w).toInt().coerceIn(0, 255)
            )
        }

        val (domR, domG, domB) = clusterRgb(bestIdx)
        val (topH, topS, topL) = rgbToHsl(domR, domG, domB)
        val accentSat = if (topL >= 0.72f && topS <= 0.25f) 0.05f else (topS * 1.08f).coerceIn(0.20f, 0.86f)
        val accentLight = if (topL >= 0.72f && topS <= 0.25f) 0.95f else topL.coerceIn(0.52f, 0.72f)
        val accent = hslToColor(topH, accentSat, accentLight)
        val glow = hslToColor(topH, accentSat, topL.coerceIn(0.36f, 0.50f))

        var secIdx = -1
        var secWeight = 0f
        for (i in 0 until 24) {
            val dist = minOf(kotlin.math.abs(i - bestIdx), 24 - kotlin.math.abs(i - bestIdx))
            if (dist >= 3 && buckets[i].weight > secWeight) {
                secWeight = buckets[i].weight
                secIdx = i
            }
        }

        val secondary = if (secIdx != -1 && secWeight > buckets[bestIdx].weight * 0.28f) {
            val (sR, sG, sB) = clusterRgb(secIdx)
            val (secH, secS, secL) = rgbToHsl(sR, sG, sB)
            hslToColor(secH, (secS * 1.05f).coerceIn(0.28f, 0.80f), secL.coerceIn(0.24f, 0.45f))
        } else {
            hslToColor(topH, (accentSat * 0.85f).coerceAtLeast(0.22f), topL.coerceIn(0.20f, 0.38f))
        }

        return Triple(accent, glow, secondary)
    }

    private fun rgbToHsl(rInt: Int, gInt: Int, bInt: Int): Triple<Float, Float, Float> {
        val r = rInt / 255f
        val g = gInt / 255f
        val b = bInt / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val l = (max + min) / 2f
        if (max == min) return Triple(0f, 0f, l)
        val d = max - min
        val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
        val h = when (max) {
            r -> (g - b) / d + (if (g < b) 6f else 0f)
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        } / 6f
        return Triple(h * 360f, s, l)
    }

    private fun hslToColor(hDeg: Float, s: Float, l: Float): Int {
        val h = ((hDeg % 360f) + 360f) % 360f
        val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f
        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val r = ((r1 + m) * 255f).toInt().coerceIn(0, 255)
        val g = ((g1 + m) * 255f).toInt().coerceIn(0, 255)
        val b = ((b1 + m) * 255f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
