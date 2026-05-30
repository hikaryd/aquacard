package net.aquadx.aquacard.data

import java.util.Locale

/**
 * Чистые null-safe форматтеры кодировок AquaDX (docs/aquadx-api-spec.md §5).
 * Доступ к позиционным кортежам best* через getOrNull — устойчивость к дрейфу длины.
 * Все числовые форматы — Locale.US, чтобы разделитель был точкой независимо от локали устройства.
 */
object ScoreFormat {

    private val MAI_LEVELS = listOf("BASIC", "ADVANCED", "EXPERT", "MASTER", "Re:MASTER")
    private val CHU_LEVELS = listOf("BASIC", "ADVANCED", "EXPERT", "MASTER", "ULTIMA", "WORLD'S END")

    /** mai2 achievement = % × 10000. 1008790 -> "100.8790%". */
    fun achievementPercent(achievement: Int?): String {
        if (achievement == null) return "—"
        val whole = achievement / 10000
        val frac = achievement % 10000
        return String.format(Locale.US, "%d.%04d%%", whole, frac)
    }

    fun chuScore(score: Int?): String = score?.toString() ?: "—"

    fun levelName(game: String, level: Int?): String {
        if (level == null) return "?"
        val list = if (game == "chu3") CHU_LEVELS else MAI_LEVELS
        return list.getOrNull(level) ?: "LV$level"
    }

    /** Буква-ранг maimai по achievement (порог 100.5% = 1005000 и ниже). */
    fun maiRank(achievement: Int?): String = when {
        achievement == null -> "—"
        achievement >= 1005000 -> "SSS+"
        achievement >= 1000000 -> "SSS"
        achievement >= 995000 -> "SS+"
        achievement >= 990000 -> "SS"
        achievement >= 980000 -> "S+"
        achievement >= 970000 -> "S"
        achievement >= 940000 -> "AAA"
        achievement >= 900000 -> "AA"
        achievement >= 800000 -> "A"
        else -> "—"
    }

    fun comboLabel(comboStatus: Int?): String? = when (comboStatus) {
        1 -> "FC"; 2 -> "FC+"; 3 -> "AP"; 4 -> "AP+"; else -> null
    }

    fun syncLabel(syncStatus: Int?): String? = when (syncStatus) {
        1 -> "FS"; 2 -> "FS+"; 3 -> "FDX"; 4 -> "FDX+"; else -> null
    }

    /** maimai rating — целое как есть (16666); chunithm playerRating ×100 (52 -> 0.52). */
    fun formatRating(game: String, rating: Int?): String {
        if (rating == null) return "—"
        return if (game == "chu3") String.format(Locale.US, "%.2f", rating / 100.0) else rating.toString()
    }

    /** Название песни по musicId с фолбэком на сам id, если метаданных нет. */
    fun songName(meta: Map<Int, MusicMeta>, musicId: Int): String =
        meta[musicId]?.name?.takeIf { it.isNotBlank() } ?: musicId.toString()

    /**
     * Парс позиционного кортежа Best. mai2: [musicId, levelIndex, v3, achievement] (4);
     * chu3: [musicId, levelIndex, score] (3). Любая нехватка элементов -> null, без исключений.
     */
    fun parseBestTuple(game: String, tuple: List<String>): BestEntry? {
        val musicId = tuple.getOrNull(0)?.toIntOrNull() ?: return null
        val level = tuple.getOrNull(1)?.toIntOrNull() ?: 0
        val valueIndex = if (game == "chu3") 2 else 3
        val value = tuple.getOrNull(valueIndex)?.toIntOrNull() ?: return null
        return BestEntry(musicId, level, value)
    }

    /** Текст значения Best: chu3 — сырой скор, иначе achievement как процент. */
    fun bestValueLabel(game: String, value: Int): String =
        if (game == "chu3") value.toString() else achievementPercent(value)
}
