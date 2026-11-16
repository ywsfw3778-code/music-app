package com.example.data

object ArtistNormalizer {

    private val aliasMap: Map<String, String> = mapOf(
        // Abyusif
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

        // Marwan Moussa
        "marwan moussa" to "Marwan Moussa",
        "marwan mousa" to "Marwan Moussa",
        "مروان موسى" to "Marwan Moussa",
        "مروان موسي" to "Marwan Moussa",

        // Wegz
        "wegz" to "Wegz",
        "ويجز" to "Wegz",

        // Marwan Pablo
        "marwan pablo" to "Marwan Pablo",
        "مروان بابلو" to "Marwan Pablo",
        "pablo" to "Marwan Pablo",

        // Karim Osama
        "karim osama" to "Karim Osama",
        "كريم أسامة" to "Karim Osama",
        "كريم اسامه" to "Karim Osama",
        "كريم اسامة" to "Karim Osama",

        // Abo El Anwar
        "abo el anwar" to "Abo El Anwar",
        "أبو الأنوار" to "Abo El Anwar",
        "ابو الانوار" to "Abo El Anwar",
        "abuanwar" to "Abo El Anwar",
        "el anwar" to "Abo El Anwar",

        // Lege-Cy
        "lege-cy" to "Lege-Cy",
        "legecy" to "Lege-Cy",
        "ليجي-سي" to "Lege-Cy",
        "ليجيسي" to "Lege-Cy",

        // Afroto
        "afroto" to "Afroto",
        "عفروتو" to "Afroto",

        // Molotof
        "molotof" to "Molotof",
        "مولوتوف" to "Molotof",

        // Mousv
        "mousv" to "Mousv",
        "موسى" to "Mousv",
        "موسي" to "Mousv",

        // Cairokee
        "cairokee" to "Cairokee",
        "كايروكي" to "Cairokee",

        // Amr Diab
        "amr diab" to "Amr Diab",
        "عمرو دياب" to "Amr Diab",

        // Mohamed Mounir
        "mohamed mounir" to "Mohamed Mounir",
        "محمد منير" to "Mohamed Mounir",
        "الكينج" to "Mohamed Mounir",

        // Tamer Hosny
        "tamer hosny" to "Tamer Hosny",
        "تامر حسني" to "Tamer Hosny",

        // Hamza Namira
        "hamza namira" to "Hamza Namira",
        "حمزة نمرة" to "Hamza Namira",
        "حمزه نمره" to "Hamza Namira",

        // Ahmed Saad
        "ahmed saad" to "Ahmed Saad",
        "أحمد سعد" to "Ahmed Saad",
        "احمد سعد" to "Ahmed Saad",

        // Ziad Zaza
        "ziad zaza" to "Ziad Zaza",
        "زياد ظاظا" to "Ziad Zaza",

        // Arsenik
        "arsenik" to "Arsenik",
        "ارسينك" to "Arsenik",
        "أرسينك" to "Arsenik",

        // El Joker
        "el joker" to "El Joker",
        "الجوكر" to "El Joker",

        // Muslim
        "muslim" to "Muslim",
        "مسلم" to "Muslim",

        // Essam Sasa
        "essam sasa" to "Essam Sasa",
        "عصام صاصا" to "Essam Sasa",

        // Sharmoofers
        "sharmoofers" to "Sharmoofers",
        "شارموفرز" to "Sharmoofers",

        // Massar Egbari
        "massar egbari" to "Massar Egbari",
        "مسار إجباري" to "Massar Egbari",
        "مسار اجباري" to "Massar Egbari",

        // Sherine
        "sherine" to "Sherine",
        "شيرين" to "Sherine",
        "شيرين عبد الوهاب" to "Sherine",

        // Angham
        "angham" to "Angham",
        "أنغام" to "Angham",
        "انغام" to "Angham",

        // Hussein Yasser
        "hussein yasser" to "Hussein Yasser",
        "حسين ياسر" to "Hussein Yasser",

        // International Artists
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

    private val titleJunkRegex = Regex(
        """(\(official\s*(music\s*)?video\)|\[official\s*(music\s*)?video\]|\(official\s*(music\s*)?audio\)|\(official\s*audio\)|\[official\s*audio\]|\(official\s*visualizer\)|\(audio\)|\(official\)|\(visualizer\)|\(clip\)|\(كليب\)|فيديو\s*كليب|\(mp3_\d+k\)|\(\d+k\)|\(2k_hd\)|\(hd\)|\.mp3|\.flac|\.m4a|\.wav|\[\s*instrumental\s*\]|prod\.?\s*by\s*[^_()]+)""",
        RegexOption.IGNORE_CASE
    )

    private val leadingTrackNumberRegex = Regex("""^(\d{1,3}[\s.\-_]+)""")

    /**
     * Normalizes artist name, ensuring clean English canonical names (e.g. "أبيوسف" -> "Abyusif").
     */
    fun normalizeArtist(rawArtist: String?): String {
        if (rawArtist.isNullOrBlank()) return "Unknown Artist"
        val trimmed = rawArtist.trim()

        if (trimmed == "فنان غير معروف" || trimmed.equals("unknown", true) || trimmed.equals("<unknown>", true)) {
            return "Unknown Artist"
        }

        // Check if bilingual compound with pipe, slash, or dash (e.g. "Abyusif I أبيوسف")
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

        // Direct lookup
        lookupAlias(trimmed)?.let { return it }

        // Multiple artists with comma or & or x (e.g. "Marwan Pablo, Hesham Nazih" or "Hussein Yasser X Abyusif")
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

    /**
     * Resolves the true artist and clean title by inspecting the raw tags, title, and filename.
     */
    fun resolveArtistAndTitle(
        rawTitle: String?,
        rawArtist: String?,
        fileName: String?
    ): Pair<String, String> {
        val baseFileName = fileName.orEmpty().substringBeforeLast('.').replace('_', ' ').trim()
        val baseTitle = rawTitle.orEmpty().ifBlank { baseFileName }.trim()

        var detectedArtist = normalizeArtist(rawArtist)
        var titleCandidate = baseTitle

        // If artist is unknown, try to extract artist from title or filename
        if (detectedArtist == "Unknown Artist") {
            // Check for "Artist - Title" or "Artist _ Title"
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

            // Check if any known artist name appears in title or filename
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

    /**
     * Cleans titles from YouTube/Telegram download junk to make it look clean like Spotify.
     */
    fun cleanTrackTitle(rawTitle: String?, artist: String? = null): String {
        if (rawTitle.isNullOrBlank()) return "Untitled Track"
        var title = rawTitle.trim()

        // Remove (Official Music Video), (MP3_160K), (Audio), etc.
        title = titleJunkRegex.replace(title, "").trim()

        // Remove leading numbers like "01 ", "01 - ", "02. "
        title = leadingTrackNumberRegex.replace(title, "").trim()

        // Replace leftover underscores with spaces
        title = title.replace('_', ' ').trim()

        // If title begins with "Artist - " or "Artist ", strip the duplicate artist prefix
        if (!artist.isNullOrBlank() && artist != "Unknown Artist") {
            val prefixDash = "$artist - "
            val prefixSpace = "$artist "
            if (title.startsWith(prefixDash, ignoreCase = true)) {
                title = title.substring(prefixDash.length).trim()
            } else if (title.startsWith(prefixSpace, ignoreCase = true)) {
                title = title.substring(prefixSpace.length).trim()
            }
        }

        // Clean up redundant double spaces or trailing dashes
        title = title.replace(Regex("""\s{2,}"""), " ").trim()
        title = title.trimEnd('-', '_', ' ')

        return title.ifBlank { rawTitle.trim() }
    }
}
