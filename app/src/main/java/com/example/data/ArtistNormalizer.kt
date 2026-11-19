package com.example.data

object ArtistNormalizer {

    private val aliasMap: Map<String, String> = mapOf(
        "abyusif" to "Abyusif",
        "ابيوسف" to "Abyusif",
        "أبيوسف" to "Abyusif",
        "ابو يوسف" to "Abyusif",
        "abyu" to "Abyusif",
        "abyusif i أبيوسف" to "Abyusif",
        "abyusif i ابيوسف" to "Abyusif",
        "abyusif | أبيوسف" to "Abyusif",
        "abyusif / أبيوسف" to "Abyusif",
        "أبيوسف | abyusif" to "Abyusif",
        "marwan moussa" to "Marwan Moussa",
        "marwan mousa" to "Marwan Moussa",
        "مروان موسى" to "Marwan Moussa",
        "مروان موسي" to "Marwan Moussa",
        "wegz" to "Wegz",
        "ويجز" to "Wegz",
        "wegz ويجز" to "Wegz",
        "ويجز wegz" to "Wegz",
        "wegz / ويجز" to "Wegz",
        "wegz - ويجز" to "Wegz",
        "wegz | ويجز" to "Wegz",
        "ويجز | wegz" to "Wegz",
        "marwan pablo" to "Marwan Pablo",
        "مروان بابلو" to "Marwan Pablo",
        "pablo" to "Marwan Pablo",
        "karim osama" to "Karim Osama",
        "كريم أسامة" to "Karim Osama",
        "كريم اسامه" to "Karim Osama",
        "كريم اسامة" to "Karim Osama",
        "abo el anwar" to "Abo El Anwar",
        "أبو الأنوار" to "Abo El Anwar",
        "ابو الانوار" to "Abo El Anwar",
        "abuanwar" to "Abo El Anwar",
        "el anwar" to "Abo El Anwar",
        "lege-cy" to "Lege-Cy",
        "legecy" to "Lege-Cy",
        "ليجي-سي" to "Lege-Cy",
        "ليجيسي" to "Lege-Cy",
        "afroto" to "Afroto",
        "عفروتو" to "Afroto",
        "molotof" to "Molotof",
        "مولوتوف" to "Molotof",
        "mousv" to "Mousv",
        "موسى" to "Mousv",
        "موسي" to "Mousv",
        "cairokee" to "Cairokee",
        "كايروكي" to "Cairokee",
        "amr diab" to "Amr Diab",
        "عمرو دياب" to "Amr Diab",
        "mohamed mounir" to "Mohamed Mounir",
        "محمد منير" to "Mohamed Mounir",
        "الكينج" to "Mohamed Mounir",
        "tamer hosny" to "Tamer Hosny",
        "تامر حسني" to "Tamer Hosny",
        "hamza namira" to "Hamza Namira",
        "حمزة نمرة" to "Hamza Namira",
        "حمزه نمره" to "Hamza Namira",
        "ahmed saad" to "Ahmed Saad",
        "أحمد سعد" to "Ahmed Saad",
        "احمد سعد" to "Ahmed Saad",
        "ziad zaza" to "Ziad Zaza",
        "زياد ظاظا" to "Ziad Zaza",
        "arsenik" to "Arsenik",
        "ارسينك" to "Arsenik",
        "أرسينك" to "Arsenik",
        "el joker" to "El Joker",
        "الجوكر" to "El Joker",
        "muslim" to "Muslim",
        "مسلم" to "Muslim",
        "essam sasa" to "Essam Sasa",
        "عصام صاصا" to "Essam Sasa",
        "sharmoofers" to "Sharmoofers",
        "شارموفرز" to "Sharmoofers",
        "massar egbari" to "Massar Egbari",
        "مسار إجباري" to "Massar Egbari",
        "مسار اجباري" to "Massar Egbari",
        "sherine" to "Sherine",
        "شيرين" to "Sherine",
        "شيرين عبد الوهاب" to "Sherine",
        "angham" to "Angham",
        "أنغام" to "Angham",
        "انغام" to "Angham",
        "hussein yasser" to "Hussein Yasser",
        "حسين ياسر" to "Hussein Yasser",
        "xxxtentacion" to "XXXTENTACION",
        "travis scott" to "Travis Scott",
        "metro boomin" to "Metro Boomin",
        "adele" to "Adele",
        "lil nas x" to "Lil Nas X",
        "james brown" to "James Brown",
        "jvke" to "JVKE",
        "egzod" to "Egzod",
        "chris grey" to "Chris Grey",
        "rustage" to "Rustage",
        "moscow" to "Moscow",
        "el waili" to "El Waili"
    )

    fun getArtistArabicName(artist: String?): String? {
        val norm = normalizeArtist(artist).lowercase()
        return when (norm) {
            "marwan moussa" -> "مروان موسى"
            "wegz" -> "ويجز"
            "abyusif" -> "أبيوسف"
            "marwan pablo" -> "مروان بابلو"
            "abo el anwar" -> "أبو الأنوار"
            "karim osama" -> "كريم أسامة"
            "afroto" -> "عفروتو"
            "lege-cy" -> "ليجي-سي"
            "molotof" -> "مولوتوف"
            "cairokee" -> "كايروكي"
            "amr diab" -> "عمرو دياب"
            "mohamed mounir" -> "محمد منير"
            "tamer hosny" -> "تامر حسني"
            "hamza namira" -> "حمزة نمرة"
            "ahmed saad" -> "أحمد سعد"
            "ziad zaza" -> "زياد ظاظا"
            "arsenik" -> "أرسينك"
            "el joker" -> "الجوكر"
            "muslim" -> "مسلم"
            "essam sasa" -> "عصام صاصا"
            "sharmoofers" -> "شارموفرز"
            "massar egbari" -> "مسار إجباري"
            "sherine" -> "شيرين"
            "angham" -> "أنغام"
            "hussein yasser" -> "حسين ياسر"
            else -> null
        }
    }

    private val titleJunkRegex = Regex(
        """(\(official\s*(music\s*)?video\)|\[official\s*(music\s*)?video\]|\(official\s*(music\s*)?audio\)|\(official\s*audio\)|\[official\s*audio\]|\(official\s*visualizer\)|\(audio\)|\(official\)|\(visualizer\)|\(clip\)|\(كليب\)|فيديو\s*كليب|\(mp3_\d+k\)|\(\d+k\)|\(2k_hd\)|\(hd\)|\.mp3|\.flac|\.m4a|\.wav|\[\s*instrumental\s*\]|prod\.?\s*by\s*[^_()]+)""",
        RegexOption.IGNORE_CASE
    )

    private val leadingTrackNumberRegex = Regex("""^(\d{1,3}[\s.\-_]+)""")

    private val junkAlbumNames = setOf(
        "unknown album", "unknown", "<unknown>", "ألبوم غير محدد", "single", "singles",
        "telegram", "telegram audio", "music", "tracks", "download", "movies", "camera",
        "sofascore", "call recordings", "meeting recordings", "standard recordings",
        "notifications", "whatsapp", "snaptube audio", "instagram", "screenshots",
        "instagold", "converted_audio", "xigncode", "g", "jly3", "audio", "sound", "sounds"
    )

    private val albumJunkPatterns = listOf(
        "t.me", ".com", "telegram", "http", "dndnha", "official video", "official audio",
        "slowed", "reverb", "كوكتيل", "أفضل أغانى", "للمونتاج", "prod.", "prod by",
        "music video", "mashup", "- topic", "topic"
    )

    fun isFakeOrJunkAlbum(
        album: String?,
        artist: String?,
        trackCount: Int,
        trackTitle: String? = null,
        fileName: String? = null
    ): Boolean {
        if (album.isNullOrBlank()) return true
        val alb = album.trim().lowercase()
        if (alb.length < 2 || alb in junkAlbumNames) return true

        for (pattern in albumJunkPatterns) {
            if (alb.contains(pattern)) return true
        }

        val normArtist = normalizeArtist(artist).lowercase()
        val normAlb = normalizeArtist(album).lowercase()

        if (alb == normArtist || normAlb == normArtist || lookupAlias(album) != null) {
            return true
        }

        val parts = alb.split(Regex("""\s+(?:[-|/iI•:]|ft\.|feat\.)\s+|[|/]"""))
        if (parts.any { p ->
            val pt = p.trim()
            pt == normArtist || lookupAlias(pt) != null || normalizeArtist(pt).lowercase() == normArtist
        }) {
            return true
        }

        if (trackCount == 1) {
            val titleClean = trackTitle?.trim()?.lowercase().orEmpty()
            val fileClean = fileName?.trim()?.lowercase().orEmpty()
            if (alb == titleClean || titleClean.startsWith(alb) || alb.startsWith(titleClean)) {
                return true
            }
            if (fileClean.contains(alb)) {
                return true
            }
            if (alb.length > 4 && (titleClean.contains(alb) || alb.contains(titleClean))) {
                return true
            }
            if (alb.contains("remix") || alb.contains(" x ") || alb.contains(" ft") || alb.contains(" feat")) {
                return true
            }
        }

        return false
    }

    fun normalizeArtist(rawArtist: String?): String {
        if (rawArtist.isNullOrBlank()) return "Unknown Artist"
        val trimmed = rawArtist.trim()

        if (trimmed == "فنان غير معروف" || trimmed.equals("unknown", true) || trimmed.equals("<unknown>", true)) {
            return "Unknown Artist"
        }

        val compoundDelimiters = listOf(" I ", " | ", " / ", " - ")
        for (delim in compoundDelimiters) {
            if (trimmed.contains(delim)) {
                val parts = trimmed.split(delim).map { it.trim() }
                for (part in parts) {
                    val resolved = lookupAlias(part)
                    if (resolved != null) return resolved
                }
            }
        }

        lookupAlias(trimmed)?.let { return it }

        val splitDelimiters = listOf(",", " & ", " x ", " X ", " feat.", " feat ", " ft.", " ft ")
        for (delim in splitDelimiters) {
            if (trimmed.contains(delim)) {
                val primary = trimmed.substringBefore(delim).trim()
                lookupAlias(primary)?.let { return it }
                val secondary = trimmed.substringAfter(delim).trim()
                lookupAlias(secondary)?.let { return it }
            }
        }

        return trimmed
    }

    private fun lookupAlias(name: String): String? {
        val lower = name.trim().lowercase()
        aliasMap[lower]?.let { return it }

        val normalized = lower
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")

        for ((alias, canonical) in aliasMap) {
            val aliasNorm = alias.lowercase()
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ة", "ه")
                .replace("ى", "ي")
            if (normalized == aliasNorm) return canonical
        }
        return null
    }

    fun resolveArtistAndTitle(
        rawTitle: String?,
        rawArtist: String?,
        fileName: String?
    ): Pair<String, String> {
        val baseFileName = fileName.orEmpty().substringBeforeLast('.').replace('_', ' ').trim()
        val baseTitle = rawTitle.orEmpty().ifBlank { baseFileName }.trim()

        var detectedArtist = normalizeArtist(rawArtist)
        var titleCandidate = baseTitle

        if (detectedArtist == "Unknown Artist") {
            val dashSplits = listOf(" - ", " – ", " — ", " _ ")
            for (dash in dashSplits) {
                if (titleCandidate.contains(dash)) {
                    val candidateArtist = titleCandidate.substringBefore(dash).trim()
                    val candidateTitle = titleCandidate.substringAfter(dash).trim()
                    val resolved = lookupAlias(candidateArtist)
                    if (resolved != null) {
                        detectedArtist = resolved
                        titleCandidate = candidateTitle
                        break
                    }
                }
            }

            if (detectedArtist == "Unknown Artist") {
                val combined = "$baseTitle $baseFileName".lowercase()
                for ((alias, canonical) in aliasMap) {
                    if (alias.length >= 4 && combined.contains(alias.lowercase())) {
                        detectedArtist = canonical
                        break
                    }
                }
            }
        }

        val cleanTitle = cleanTrackTitle(titleCandidate, detectedArtist)
        return Pair(detectedArtist, cleanTitle)
    }

    fun cleanTrackTitle(rawTitle: String?, artist: String? = null): String {
        if (rawTitle.isNullOrBlank()) return "Untitled Track"
        var title = rawTitle.trim()

        title = titleJunkRegex.replace(title, "").trim()
        title = leadingTrackNumberRegex.replace(title, "").trim()
        title = title.replace('_', ' ').trim()

        if (!artist.isNullOrBlank() && artist != "Unknown Artist") {
            val prefixDash = "$artist - "
            val prefixSpace = "$artist "
            if (title.startsWith(prefixDash, ignoreCase = true)) {
                title = title.substring(prefixDash.length).trim()
            } else if (title.startsWith(prefixSpace, ignoreCase = true)) {
                title = title.substring(prefixSpace.length).trim()
            }
        }

        if (title.contains(" - ")) {
            val beforeDash = title.substringBefore(" - ").trim()
            val afterDash = title.substringAfter(" - ").trim()
            if (afterDash.isNotBlank() && (beforeDash.equals(artist, ignoreCase = true) ||
                beforeDash.contains(" x ", ignoreCase = true) ||
                beforeDash.contains(" & ") ||
                beforeDash.contains(" ft", ignoreCase = true) ||
                lookupAlias(beforeDash) != null)) {
                title = afterDash
            }
        }

        val artistAliases = listOfNotNull(artist, lookupAlias(artist.orEmpty()))
        var working = title
        if (working.contains('|')) {
            val segments = working.split('|').map { it.trim() }
            val nonArtistSegments = segments.filter { seg ->
                artistAliases.none { a -> seg.equals(a, ignoreCase = true) }
            }
            if (nonArtistSegments.isNotEmpty()) {
                working = nonArtistSegments.joinToString(" - ")
            }
        }
        if (working.contains(" - ") || working.contains('-')) {
            val pieces = working.split(Regex("""\s*[-–—]\s*""")).map { it.trim() }.filter { it.isNotBlank() }
            val nonArtistPieces = pieces.filter { piece ->
                artistAliases.none { a -> piece.equals(a, ignoreCase = true) }
            }
            val cleanLatin = nonArtistPieces.filter { p ->
                p.any { it in 'a'..'z' || it in 'A'..'Z' } && !p.any { it in '\u0600'..'\u06FF' }
            }
            if (cleanLatin.isNotEmpty()) {
                val best = cleanLatin.maxByOrNull { it.length } ?: cleanLatin.first()
                if (best.length >= 2) {
                    working = best
                }
            } else if (nonArtistPieces.isNotEmpty()) {
                working = nonArtistPieces.first()
            }
        }
        title = working

        title = title.replace(Regex("""\s{2,}"""), " ").trim()
        title = title.trimEnd('-', '_', ' ')

        return title.ifBlank { rawTitle.trim() }
    }
}
