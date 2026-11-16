package com.example.data

object ArtistNormalizer {

    private val aliasMap: Map<String, String> = mapOf(
        // Abyusif
        "abyusif" to "أبيوسف",
        "ابيوسف" to "أبيوسف",
        "أبيوسف" to "أبيوسف",
        "ابو يوسف" to "أبيوسف",
        "abyu" to "أبيوسف",

        // Marwan Moussa
        "marwan moussa" to "مروان موسى",
        "مروان موسى" to "مروان موسى",
        "مروان موسي" to "مروان موسى",
        "marwan mousa" to "مروان موسى",

        // Wegz
        "wegz" to "ويجز",
        "ويجز" to "ويجز",

        // Amr Diab
        "amr diab" to "عمرو دياب",
        "عمرو دياب" to "عمرو دياب",

        // Mohamed Mounir
        "mohamed mounir" to "محمد منير",
        "محمد منير" to "محمد منير",
        "الكينج" to "محمد منير",

        // Tamer Hosny
        "tamer hosny" to "تامر حسني",
        "تامر حسني" to "تامر حسني",

        // Ahmed Saad
        "ahmed saad" to "أحمد سعد",
        "أحمد سعد" to "أحمد سعد",
        "احمد سعد" to "أحمد سعد",

        // Hamza Namira
        "hamza namira" to "حمزة نمرة",
        "حمزة نمرة" to "حمزة نمرة",
        "حمزه نمره" to "حمزة نمرة",

        // Cairokee
        "cairokee" to "كايروكي",
        "كايروكي" to "كايروكي",

        // Marwan Pablo
        "marwan pablo" to "مروان بابلو",
        "مروان بابلو" to "مروان بابلو",

        // Afroto
        "afroto" to "عفروتو",
        "عفروتو" to "عفروتو",

        // El Joker
        "el joker" to "الجوكر",
        "الجوكر" to "الجوكر",

        // Lege-Cy
        "lege-cy" to "ليجي-سي",
        "legecy" to "ليجي-سي",
        "ليجي-سي" to "ليجي-سي",
        "ليجيسي" to "ليجي-سي",

        // Sherine
        "sherine" to "شيرين",
        "شيرين" to "شيرين",
        "شيرين عبد الوهاب" to "شيرين",

        // Angham
        "angham" to "أنغام",
        "أنغام" to "أنغام",
        "انغام" to "أنغام",

        // Massar Egbari
        "massar egbari" to "مسار إجباري",
        "مسار إجباري" to "مسار إجباري",
        "مسار اجباري" to "مسار إجباري",

        // Sharmoofers
        "sharmoofers" to "شارموفرز",
        "شارموفرز" to "شارموفرز",

        // Essam Sasa
        "essam sasa" to "عصام صاصا",
        "عصام صاصا" to "عصام صاصا",

        // Muslim
        "muslim" to "مسلم",
        "مسلم" to "مسلم"
    )

    private val titleJunkRegex = Regex(
        """(\(official\s*(music\s*)?video\)|\[official\s*(music\s*)?audio\]|\(official\s*audio\)|\[audio\]|\(clip\)|\(كليب\)|فيديو\s*كليب|\(mp3_\d+k\)|\(\d+k\)|\.mp3|\.flac|\.m4a)""",
        RegexOption.IGNORE_CASE
    )

    private val leadingTrackNumberRegex = Regex("""^(\d{1,3}[\s.\-_]+)""")

    /**
     * Normalizes artist name, resolving Arabic/English aliases (e.g. "Abyusif" -> "أبيوسف").
     */
    fun normalizeArtist(rawArtist: String?): String {
        if (rawArtist.isNullOrBlank()) return "فنان غير معروف"
        val trimmed = rawArtist.trim()
        val key = trimmed.lowercase()
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")

        // First check exact lower case in alias map
        aliasMap[trimmed.lowercase()]?.let { return it }

        // Check normalized key in alias map
        for ((alias, canonical) in aliasMap) {
            val normalizedAlias = alias.lowercase()
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ة", "ه")
                .replace("ى", "ي")
            if (key == normalizedAlias) return canonical
        }

        // Handle featuring (e.g. "Abyusif Ft. Wegz" -> extract primary "أبيوسف")
        val featDelimiters = listOf(" feat.", " feat ", " ft.", " ft ", " with ", " مع ")
        for (delim in featDelimiters) {
            if (trimmed.contains(delim, ignoreCase = true)) {
                val primary = trimmed.substringBefore(delim, "").trim()
                if (primary.isNotBlank()) {
                    return normalizeArtist(primary)
                }
            }
        }

        return trimmed
    }

    /**
     * Cleans titles from YouTube/Telegram download junk (e.g. "01 FAHMAN DONYA (Official Video)" -> "FAHMAN DONYA").
     */
    fun cleanTrackTitle(rawTitle: String?, artist: String? = null): String {
        if (rawTitle.isNullOrBlank()) return "أغنية بدون عنوان"
        var title = rawTitle.trim()

        // Remove (Official Music Video), (MP3_160K), etc.
        title = titleJunkRegex.replace(title, "").trim()

        // Remove leading numbers like "01 ", "01 - "
        title = leadingTrackNumberRegex.replace(title, "").trim()

        // If title begins with "Artist - Title", strip the duplicate artist
        if (!artist.isNullOrBlank()) {
            val prefix = "$artist - "
            if (title.startsWith(prefix, ignoreCase = true)) {
                title = title.substring(prefix.length).trim()
            }
        }

        return title.ifBlank { rawTitle.trim() }
    }
}
