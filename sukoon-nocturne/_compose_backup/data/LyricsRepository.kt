package com.example.data

import com.example.model.LrcSearchResult
import com.example.model.LyricLine
import com.example.model.LyricsData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object LyricsRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val lrcRegex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?](.*)""")

    fun parseLrc(lrcText: String): List<LyricLine> {
        val result = mutableListOf<LyricLine>()
        lrcText.lines().forEach { line ->
            val match = lrcRegex.find(line.trim()) ?: return@forEach
            val min = match.groupValues[1].toLongOrNull() ?: 0L
            val sec = match.groupValues[2].toLongOrNull() ?: 0L
            val fracRaw = match.groupValues[3]
            val ms = when (fracRaw.length) {
                1 -> (fracRaw.toLongOrNull() ?: 0L) * 100L
                2 -> (fracRaw.toLongOrNull() ?: 0L) * 10L
                3 -> fracRaw.toLongOrNull() ?: 0L
                else -> 0L
            }
            val text = match.groupValues[4].trim()
            if (text.isNotEmpty()) {
                result.add(LyricLine(timeMs = (min * 60 + sec) * 1000L + ms, text = text))
            }
        }
        return result.sortedBy { it.timeMs }
    }

    fun cleanLyricsQuery(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace(Regex("""\[[^\]]*]"""), " ")
            .replace(
                Regex(
                    """\([^)]*(official|audio|video|lyric|lyrics|clip|hq|hd|4k|mp3|kbps|remix|live|cover|version|prod|feat|ft\.|اوديو|أوديو|فيديو|كليب|حصري|كلمات|نسخه|نسخة|موسيقى)[^)]*\)""",
                    RegexOption.IGNORE_CASE
                ),
                " "
            )
            .replace(
                Regex(
                    """\b(official|audio|music\s*video|lyric\s*video|lyrics|visualizer|hq|hd|4k|mp3|m4a|flac|wav|feat\.?|ft\.?|prod\.?)\b""",
                    RegexOption.IGNORE_CASE
                ),
                " "
            )
            .replace(Regex("""(اوديو|أوديو|فيديو\s*كليب|كليب|حصريا|حصري|كلمات|النسخة\s*الأصلية|نسخة\s*أصلية)"""), " ")
            .replace(Regex("""[|•_—–/\\]+"""), " ")
            .replace(Regex("""\s+-\s+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    fun francoToArabic(input: String): String {
        val q = input.lowercase().trim()
        val dict = mapOf(
            "enta" to "انت", "enti" to "انتي", "ana" to "انا", "ehna" to "احنا",
            "howa" to "هو", "heya" to "هي", "el" to "ال", "ya" to "يا",
            "fe" to "في", "fi" to "في", "men" to "من", "3ala" to "على", "ala" to "على",
            "ma3" to "مع", "ma3ak" to "معاك", "ma3aya" to "معايا", "habibi" to "حبيبي",
            "7abibi" to "حبيبي", "omri" to "عمري", "3omri" to "عمري", "albi" to "قلبي",
            "2albi" to "قلبي", "ro7i" to "روحي", "rohi" to "روحي", "law" to "لو",
            "kol" to "كل", "yom" to "يوم", "leil" to "ليل", "nour" to "نور",
            "3ein" to "عين", "ein" to "عين", "eih" to "ايه", "leh" to "ليه",
            "mesh" to "مش", "mish" to "مش", "aw" to "او", "bas" to "بس",
            "keda" to "كده", "bahebak" to "بحبك", "ba7ebak" to "بحبك",
            "ahebak" to "احبك", "wa7ashtini" to "وحشتيني", "ghali" to "غالي",
            "tamally" to "تملي"
        )
        val tokens = q.split(Regex("""\s+""")).map { tok ->
            dict[tok] ?: tok
                .replace("kh", "خ").replace("sh", "ش").replace("ch", "ش")
                .replace("th", "ث").replace("gh", "غ").replace("3", "ع")
                .replace("7", "ح").replace("2", "ا").replace("5", "خ")
                .replace("6", "ط").replace("9", "ص").replace("8", "ق")
                .replace("a", "ا").replace("b", "ب").replace("t", "ت")
                .replace("g", "ج").replace("d", "د").replace("r", "ر")
                .replace("z", "ز").replace("s", "س").replace("f", "ف")
                .replace("k", "ك").replace("l", "ل").replace("m", "م")
                .replace("n", "ن").replace("h", "ه").replace("w", "و")
                .replace("o", "و").replace("u", "و").replace("y", "ي")
                .replace("i", "ي").replace("e", "ي")
        }
        return tokens.joinToString(" ").trim()
    }

    suspend fun searchManual(query: String): List<LrcSearchResult> = withContext(Dispatchers.IO) {
        val cleaned = cleanLyricsQuery(query).ifBlank { query.trim() }
        if (cleaned.isBlank()) return@withContext emptyList()

        val candidates = linkedSetOf(cleaned)
        val arMatch = Regex("""[\u0600-\u06FF][\u0600-\u06FF\s]{2,}""").find(cleaned)?.value?.trim()
        val enMatch = Regex("""[A-Za-z][A-Za-z0-9\s']{2,}""").find(cleaned)?.value?.trim()
        if (!arMatch.isNullOrBlank()) candidates.add(arMatch)
        if (!enMatch.isNullOrBlank()) {
            candidates.add(enMatch)
            if (arMatch.isNullOrBlank()) {
                val franco = francoToArabic(enMatch)
                if (franco.isNotBlank()) candidates.add(franco)
            }
        }

        val allResults = linkedMapOf<Long, LrcSearchResult>()
        for (q in candidates.take(4)) {
            val encoded = URLEncoder.encode(q, "UTF-8")
            val url = "https://lrclib.net/api/search?q=$encoded"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Sukoon-Nocturne/1.0 (Android)")
                .build()
            runCatching {
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val body = resp.body?.string().orEmpty()
                    val arr = JSONArray(body)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optLong("id", i.toLong())
                        val synced = obj.optString("syncedLyrics", "").takeIf { it.isNotBlank() && it != "null" }
                        val plain = obj.optString("plainLyrics", "").takeIf { it.isNotBlank() && it != "null" }
                        if (synced != null || plain != null) {
                            allResults.putIfAbsent(
                                id,
                                LrcSearchResult(
                                    id = id,
                                    trackName = obj.optString("trackName", ""),
                                    artistName = obj.optString("artistName", ""),
                                    albumName = obj.optString("albumName", ""),
                                    duration = obj.optDouble("duration", 0.0),
                                    syncedLyrics = synced,
                                    plainLyrics = plain
                                )
                            )
                        }
                    }
                }
            }
            if (allResults.isNotEmpty()) break
        }
        allResults.values.toList()
    }

    suspend fun fetchBestLyrics(title: String, artist: String, durationSec: Double): LyricsData? =
        withContext(Dispatchers.IO) {
            val cleanTitle = cleanLyricsQuery(title)
            val cleanArtist = cleanLyricsQuery(artist)
            val query = listOf(cleanTitle, cleanArtist)
                .filter { it.isNotBlank() && !it.contains("غير معروف") && !it.contains("Unknown", true) }
                .joinToString(" ")
                .ifBlank { cleanTitle }

            val results = searchManual(query)
            if (results.isEmpty()) return@withContext null

            val best = results
                .sortedBy { item ->
                    val syncedPenalty = if (item.syncedLyrics != null) 0.0 else 500.0
                    val diff = if (durationSec > 0 && item.duration > 0) kotlin.math.abs(item.duration - durationSec) else 10.0
                    syncedPenalty + diff
                }
                .firstOrNull() ?: return@withContext null

            val lines = best.syncedLyrics?.let { parseLrc(it) } ?: emptyList()
            LyricsData(
                syncedLyrics = best.syncedLyrics,
                plainLyrics = best.plainLyrics,
                lines = lines,
                sourceName = "LRCLIB"
            )
        }
}
