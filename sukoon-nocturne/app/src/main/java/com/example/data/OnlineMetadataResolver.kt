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

        val artCacheDir = File(context.cacheDir, "track_art_v2").apply { mkdirs() }
        val safeKey = "${cleanArtist}_${cleanAlbum}".replace(Regex("[^a-zA-Z0-9_\\-\\u0600-\\u06FF]"), "_")
        val targetFile = File(artCacheDir, "album_${safeKey}.jpg")
        val noArtMarker = File(artCacheDir, "album_${safeKey}.none")

        if (targetFile.exists() && targetFile.length() > 0L) {
            val uriStr = android.net.Uri.fromFile(targetFile).toString()
            albumArtCache[cacheKey] = uriStr
            return@withContext uriStr
        }
        if (noArtMarker.exists()) return@withContext null

        // 1. Try Deezer Search
        val queries = listOf(
            "$cleanArtist $cleanAlbum",
            cleanAlbum
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
                    first.optString("cover_big").takeIf { it.isNotBlank() }
                        ?: first.optString("cover_medium").takeIf { it.isNotBlank() }
                }
            }.getOrNull()

            if (!coverUrl.isNullOrBlank()) break

            // Fallback track search that returns album object
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
                    albObj?.optString("cover_big")?.takeIf { it.isNotBlank() }
                        ?: albObj?.optString("cover_medium")?.takeIf { it.isNotBlank() }
                }
            }.getOrNull()

            if (!coverUrl.isNullOrBlank()) break
        }

        // 2. Try iTunes Search API if Deezer did not return artwork
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
                            rawArt.replace("100x100bb", "600x600bb")
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
        val cleanTitle = ArtistNormalizer.cleanTrackTitle(title, artist)
        val cleanArtist = ArtistNormalizer.normalizeArtist(artist)
        val query = "$cleanArtist $cleanTitle".trim()
        if (query.length < 3) return@withContext false

        val encoded = runCatching { URLEncoder.encode(query, "UTF-8") }.getOrNull() ?: return@withContext false
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
                album?.optString("cover_big")?.takeIf { it.isNotBlank() }
                    ?: album?.optString("cover_medium")?.takeIf { it.isNotBlank() }
            }
        }.getOrNull()

        // Fallback to iTunes song search
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
                        rawArt.replace("100x100bb", "600x600bb")
                    } else null
                }
            }.getOrNull()
        }

        if (!coverUrl.isNullOrBlank()) {
            return@withContext downloadToFile(coverUrl, targetFile)
        }
        false
    }

    suspend fun resolveArtistPhoto(
        context: Context,
        artistName: String
    ): String? = withContext(Dispatchers.IO) {
        val canonical = ArtistNormalizer.normalizeArtist(artistName)
        artistPhotoCache[canonical]?.let { return@withContext it }

        val artistDir = File(context.cacheDir, "artist_art").apply { mkdirs() }
        val safeName = canonical.replace(Regex("[^a-zA-Z0-9_\\-\\u0600-\\u06FF]"), "_")
        val cachedFile = File(artistDir, "artist_${safeName}.jpg")
        val noArtMarker = File(artistDir, "artist_${safeName}.none")

        if (cachedFile.exists() && cachedFile.length() > 0L) {
            val uriStr = android.net.Uri.fromFile(cachedFile).toString()
            artistPhotoCache[canonical] = uriStr
            return@withContext uriStr
        }
        if (noArtMarker.exists()) return@withContext null

        val encoded = runCatching { URLEncoder.encode(canonical, "UTF-8") }.getOrNull() ?: return@withContext null
        val url = "https://api.deezer.com/search/artist?q=$encoded&limit=1"
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Sukoon-Nocturne/2.0 (Android)")
            .build()

        val photoUrl = runCatching {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string().orEmpty()
                val json = JSONObject(body)
                val data = json.optJSONArray("data") ?: return@use null
                if (data.length() == 0) return@use null
                val first = data.getJSONObject(0)
                first.optString("picture_medium").takeIf { it.isNotBlank() }
                    ?: first.optString("picture_big").takeIf { it.isNotBlank() }
            }
        }.getOrNull()

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
