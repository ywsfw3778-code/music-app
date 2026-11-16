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

        val coverUrl = runCatching {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string().orEmpty()
                val json = JSONObject(body)
                val data = json.optJSONArray("data") ?: return@use null
                if (data.length() == 0) return@use null
                val first = data.getJSONObject(0)
                val album = first.optJSONObject("album")
                album?.optString("cover_medium")?.takeIf { it.isNotBlank() }
                    ?: album?.optString("cover_big")?.takeIf { it.isNotBlank() }
            }
        }.getOrNull()

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
