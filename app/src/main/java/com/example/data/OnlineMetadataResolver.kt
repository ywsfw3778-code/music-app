package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object OnlineMetadataResolver {

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private val artistPhotoCache = ConcurrentHashMap<String, String>()
    private val albumArtCache = ConcurrentHashMap<String, String>()

    suspend fun resolveAlbumArtOnline(
        context: Context,
        albumTitle: String,
        artistName: String
    ): String? = withContext(Dispatchers.IO) {
        val cleanAlbum = albumTitle.trim()
        val cleanArtist = ArtistNormalizer.normalizeArtist(artistName)
        val cacheKey = "$cleanAlbum:::$cleanArtist".lowercase()
        albumArtCache[cacheKey]?.let { return@withContext it }

        val artCacheDir = File(context.filesDir, "track_art_v2").apply { mkdirs() }
        val safeKey = "${cleanArtist}_${cleanAlbum}".replace(Regex("[^a-zA-Z0-9_\\-\\u0600-\\u06FF]"), "_")
        val targetFile = File(artCacheDir, "album_${safeKey}.jpg")
        val noArtMarker = File(artCacheDir, "album_${safeKey}.none")

        if (targetFile.exists() && targetFile.length() > 0L) {
            val uriStr = android.net.Uri.fromFile(targetFile).toString()
            albumArtCache[cacheKey] = uriStr
            return@withContext uriStr
        }
        if (noArtMarker.exists()) {
            val ageMs = System.currentTimeMillis() - noArtMarker.lastModified()
            if (ageMs < 30 * 60 * 1000L) return@withContext null
            noArtMarker.delete()
        }

        val arabicArtist = ArtistNormalizer.getArtistArabicName(cleanArtist)

        val queries = listOfNotNull(
            "$cleanArtist $cleanAlbum",
            cleanAlbum,
            arabicArtist?.let { "$it $cleanAlbum" }
        )

        var coverUrl: String? = null
        for (q in queries) {
            val encoded = runCatching { URLEncoder.encode(q, "UTF-8") }.getOrNull() ?: continue
            val url = "https://api.deezer.com/search/album?q=$encoded&limit=1"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Sukoon-Nocturne/2.0 (Android)")
                .build()

            coverUrl = runCatching {
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val body = resp.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val data = json.optJSONArray("data") ?: return@use null
                    if (data.length() == 0) return@use null
                    val first = data.getJSONObject(0)
                    first.optString("cover_xl").takeIf { it.isNotBlank() }
                        ?: first.optString("cover_big").takeIf { it.isNotBlank() }
                        ?: first.optString("cover_medium").takeIf { it.isNotBlank() }
                }
            }.getOrNull()

            if (!coverUrl.isNullOrBlank()) break

            val trackSearchUrl = "https://api.deezer.com/search?q=$encoded&limit=1"
            val trackReq = Request.Builder()
                .url(trackSearchUrl)
                .header("User-Agent", "Sukoon-Nocturne/2.0 (Android)")
                .build()

            coverUrl = runCatching {
                client.newCall(trackReq).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val body = resp.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val data = json.optJSONArray("data") ?: return@use null
                    if (data.length() == 0) return@use null
                    val first = data.getJSONObject(0)
                    val albObj = first.optJSONObject("album")
                    albObj?.optString("cover_xl")?.takeIf { it.isNotBlank() }
                        ?: albObj?.optString("cover_big")?.takeIf { it.isNotBlank() }
                        ?: albObj?.optString("cover_medium")?.takeIf { it.isNotBlank() }
                }
            }.getOrNull()

            if (!coverUrl.isNullOrBlank()) break
        }

        if (coverUrl.isNullOrBlank()) {
            val iTunesQuery = runCatching { URLEncoder.encode("$cleanArtist $cleanAlbum", "UTF-8") }.getOrNull()
            if (!iTunesQuery.isNullOrBlank()) {
                val itunesUrl = "https://itunes.apple.com/search?term=$iTunesQuery&entity=album&limit=1"
                val itunesReq = Request.Builder()
                    .url(itunesUrl)
                    .header("User-Agent", "Sukoon-Nocturne/2.0 (Android)")
                    .build()

                coverUrl = runCatching {
                    client.newCall(itunesReq).execute().use { resp ->
                        if (!resp.isSuccessful) return@use null
                        val body = resp.body?.string().orEmpty()
                        val json = JSONObject(body)
                        val results = json.optJSONArray("results") ?: return@use null
                        if (results.length() == 0) return@use null
                        val first = results.getJSONObject(0)
                        val rawArt = first.optString("artworkUrl100")
                        if (rawArt.isNotBlank()) {
                            rawArt.replace("100x100bb", "1000x1000bb")
                        } else null
                    }
                }.getOrNull()
            }
        }

        if (!coverUrl.isNullOrBlank()) {
            val success = downloadToFile(coverUrl, targetFile)
            if (success) {
                val uriStr = android.net.Uri.fromFile(targetFile).toString()
                albumArtCache[cacheKey] = uriStr
                return@withContext uriStr
            }
        }

        runCatching { noArtMarker.createNewFile() }
        null
    }

    suspend fun resolveTrackArtOnline(
        context: Context,
        trackId: String,
        title: String,
        artist: String,
        targetFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        if (targetFile.exists() && targetFile.length() > 0L) return@withContext true
        val onlineMissMarker = File(targetFile.parentFile, "track_${trackId}.online_miss")
        if (onlineMissMarker.exists()) {
            val ageMs = System.currentTimeMillis() - onlineMissMarker.lastModified()
            if (ageMs < 30 * 60 * 1000L) return@withContext false
            onlineMissMarker.delete()
        }
        val cleanTitle = ArtistNormalizer.cleanTrackTitle(title, artist)
        val cleanArtist = ArtistNormalizer.normalizeArtist(artist)

        val queries = mutableListOf<String>()
        queries.add("$cleanArtist $cleanTitle".trim())
        if (cleanTitle.contains("|")) {
            val p1 = cleanTitle.substringBefore("|").trim()
            val p2 = cleanTitle.substringAfter("|").trim()
            if (p1.isNotBlank()) queries.add("$cleanArtist $p1")
            if (p2.isNotBlank()) queries.add("$cleanArtist $p2")
        }
        if (cleanTitle.contains("-")) {
            val p2 = cleanTitle.substringAfter("-").trim()
            if (p2.isNotBlank()) queries.add("$cleanArtist $p2")
        }
        queries.add(cleanTitle)

        for (q in queries) {
            val encoded = runCatching { URLEncoder.encode(q.trim(), "UTF-8") }.getOrNull() ?: continue
            val url = "https://api.deezer.com/search?q=$encoded&limit=1"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Sukoon-Nocturne/2.0 (Android)")
                .build()

            var coverUrl = runCatching {
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val body = resp.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val data = json.optJSONArray("data") ?: return@use null
                    if (data.length() == 0) return@use null
                    val first = data.getJSONObject(0)
                    val album = first.optJSONObject("album")
                    album?.optString("cover_xl")?.takeIf { it.isNotBlank() }
                        ?: album?.optString("cover_big")?.takeIf { it.isNotBlank() }
                        ?: album?.optString("cover_medium")?.takeIf { it.isNotBlank() }
                }
            }.getOrNull()

            if (coverUrl.isNullOrBlank()) {
                val itunesUrl = "https://itunes.apple.com/search?term=$encoded&entity=song&limit=1"
                val itunesReq = Request.Builder()
                    .url(itunesUrl)
                    .header("User-Agent", "Sukoon-Nocturne/2.0 (Android)")
                    .build()

                coverUrl = runCatching {
                    client.newCall(itunesReq).execute().use { resp ->
                        if (!resp.isSuccessful) return@use null
                        val body = resp.body?.string().orEmpty()
                        val json = JSONObject(body)
                        val results = json.optJSONArray("results") ?: return@use null
                        if (results.length() == 0) return@use null
                        val first = results.getJSONObject(0)
                        val rawArt = first.optString("artworkUrl100")
                        if (rawArt.isNotBlank()) {
                            rawArt.replace("100x100bb", "1000x1000bb")
                        } else null
                    }
                }.getOrNull()
            }

            if (!coverUrl.isNullOrBlank()) {
                val downloaded = downloadToFile(coverUrl, targetFile)
                if (downloaded) return@withContext true
            }
        }
        runCatching { onlineMissMarker.createNewFile() }
        false
    }

    suspend fun resolveArtistPhoto(
        context: Context,
        artistName: String
    ): String? = withContext(Dispatchers.IO) {
        val canonical = ArtistNormalizer.normalizeArtist(artistName)
        artistPhotoCache[canonical]?.let { return@withContext it }

        val artistDir = File(context.filesDir, "artist_art").apply { mkdirs() }
        val safeName = canonical.replace(Regex("[^a-zA-Z0-9_\\-\\u0600-\\u06FF]"), "_")
        val cachedFile = File(artistDir, "artist_${safeName}.jpg")
        val noArtMarker = File(artistDir, "artist_${safeName}.none")

        if (cachedFile.exists() && cachedFile.length() > 3000L) {
            val uriStr = android.net.Uri.fromFile(cachedFile).toString()
            artistPhotoCache[canonical] = uriStr
            return@withContext uriStr
        }
        if (cachedFile.exists() && cachedFile.length() <= 3000L) {
            cachedFile.delete()
        }
        if (noArtMarker.exists()) {
            val ageMs = System.currentTimeMillis() - noArtMarker.lastModified()
            if (ageMs < 30 * 60 * 1000L && !canonical.equals("Wegz", ignoreCase = true)) {
                return@withContext null
            } else {
                noArtMarker.delete()
            }
        }

        val arabicName = ArtistNormalizer.getArtistArabicName(canonical)
        val queries = listOfNotNull(canonical, arabicName)

        var photoUrl: String? = null
        for (q in queries) {
            val encoded = runCatching { URLEncoder.encode(q, "UTF-8") }.getOrNull() ?: continue
            val url = "https://api.deezer.com/search/artist?q=$encoded&limit=1"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Sukoon-Nocturne/2.0 (Android)")
                .build()

            photoUrl = runCatching {
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val body = resp.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val data = json.optJSONArray("data") ?: return@use null
                    if (data.length() == 0) return@use null
                    val first = data.getJSONObject(0)
                    first.optString("picture_xl").takeIf { it.isNotBlank() }
                        ?: first.optString("picture_big").takeIf { it.isNotBlank() }
                        ?: first.optString("picture_medium").takeIf { it.isNotBlank() }
                }
            }.getOrNull()

            if (!photoUrl.isNullOrBlank()) break
        }

        if (!photoUrl.isNullOrBlank()) {
            val success = downloadToFile(photoUrl, cachedFile)
            if (success) {
                val uriStr = android.net.Uri.fromFile(cachedFile).toString()
                artistPhotoCache[canonical] = uriStr
                return@withContext uriStr
            }
        }

        runCatching { noArtMarker.createNewFile() }
        null
    }

    private fun downloadToFile(url: String, target: File): Boolean {
        val req = Request.Builder().url(url).build()
        return runCatching {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use false
                val bytes = resp.body?.bytes() ?: return@use false
                if (bytes.isEmpty()) return@use false
                FileOutputStream(target).use { out -> out.write(bytes) }
                target.exists() && target.length() > 0L
            }
        }.getOrDefault(false)
    }
}
