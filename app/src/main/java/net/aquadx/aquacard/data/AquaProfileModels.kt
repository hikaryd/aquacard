package net.aquadx.aquacard.data

import kotlinx.serialization.Serializable

/**
 * Wire-DTO для публичного веб-API AquaDX. Толерантны к дрейфу схемы:
 * все поля, кроме идентификатора песни, имеют дефолт, так что отсутствующее
 * или null-поле не роняет парсинг (см. docs/profile-ui-plan.md §3, §1).
 */

@Serializable
data class GameSummary(
    val name: String? = null,
    val aquaUser: AquaUser? = null,
    val serverRank: Int? = null,
    val rating: Int? = null,
    val ratingHighest: Int? = null,
    val accuracy: Double? = null,
    val plays: Int? = null,
    val maxCombo: Int? = null,
    val fullCombo: Int? = null,
    val allPerfect: Int? = null,
    val totalScore: Long? = null,
    val lastVersion: String? = null,
    val joined: String? = null,
    val lastSeen: String? = null,
    val ranks: List<RankCount> = emptyList()
)

@Serializable
data class RankCount(
    val name: String? = null,
    val count: Int? = null
)

/** Точный рейтинг/косметика. Поля различаются между играми — берём только нужные. */
@Serializable
data class UserDetailDto(
    val playerRating: Int? = null,
    val classRank: Int? = null,
    val courseRank: Int? = null,
    val level: Int? = null
)

/** Рейтинг-композиция + все скоры. best* — позиционные кортежи (mai=4, chu=3 элемента). */
@Serializable
data class UserRatingDto(
    val best35: List<List<String>> = emptyList(),
    val best15: List<List<String>> = emptyList(),
    val best30: List<List<String>> = emptyList(),
    val recent10: List<List<String>> = emptyList(),
    val musicList: List<MusicScoreDto> = emptyList()
)

/** Один скор из musicList. Union обеих игр — все поля nullable. */
@Serializable
data class MusicScoreDto(
    val musicId: Int = 0,
    val level: Int? = null,
    val playCount: Int? = null,
    val achievement: Int? = null,    // mai2: ачивка ×10000
    val deluxscoreMax: Int? = null,  // mai2: DX-счёт
    val scoreMax: Int? = null,       // chu3: лучший скор 0..1010000
    val scoreRank: Int? = null,
    val comboStatus: Int? = null,    // mai2: 0..4
    val syncStatus: Int? = null,     // mai2: 0..4
    val isFullCombo: Boolean? = null, // chu3
    val isAllJustice: Boolean? = null // chu3
)

/**
 * Запись недавней партии maimai (playlog). Имена полей сверены с живым ответом
 * GET /api/v2/game/mai2/recent (см. docs). Толерантна: все поля nullable/default.
 * Агрегатных джаджментов в ответе НЕТ — суммируются из 25 per-note полей (см. AquaProfileRepository).
 */
@Serializable
data class RecentPlayDto(
    val musicId: Int = 0,
    val level: Int? = null,
    val trackNo: Int? = null,
    val userPlayDate: String? = null, // "yyyy-MM-dd HH:mm:ss" (+".0" у новейших) — ключ сортировки
    val playDate: String? = null,
    val achievement: Int? = null,     // ×10000 (919811 = 91.9811%)
    val deluxscore: Int? = null,
    val scoreRank: Int? = null,       // числовой код
    val comboStatus: Int? = null,
    val syncStatus: Int? = null,
    val maxCombo: Int? = null,
    val totalCombo: Int? = null,      // знаменатель FC; deluxscore max = totalCombo*3
    val fastCount: Int? = null,
    val lateCount: Int? = null,
    val isClear: Boolean? = null,
    val isFullCombo: Boolean? = null,
    val isAllPerfect: Boolean? = null,
    val beforeRating: Int? = null,
    val afterRating: Int? = null,
    val placeName: String? = null,
    // джаджменты по типам нот
    val tapCriticalPerfect: Int? = null,
    val tapPerfect: Int? = null,
    val tapGreat: Int? = null,
    val tapGood: Int? = null,
    val tapMiss: Int? = null,
    val holdCriticalPerfect: Int? = null,
    val holdPerfect: Int? = null,
    val holdGreat: Int? = null,
    val holdGood: Int? = null,
    val holdMiss: Int? = null,
    val slideCriticalPerfect: Int? = null,
    val slidePerfect: Int? = null,
    val slideGreat: Int? = null,
    val slideGood: Int? = null,
    val slideMiss: Int? = null,
    val touchCriticalPerfect: Int? = null,
    val touchPerfect: Int? = null,
    val touchGreat: Int? = null,
    val touchGood: Int? = null,
    val touchMiss: Int? = null,
    val breakCriticalPerfect: Int? = null,
    val breakPerfect: Int? = null,
    val breakGreat: Int? = null,
    val breakGood: Int? = null,
    val breakMiss: Int? = null
)

@Serializable
data class TrendPoint(
    val date: String = "",
    val rating: Int? = null,
    val plays: Int? = null
)

@Serializable
data class GameBrief(
    val name: String? = null,
    val rating: Int? = null,
    val lastLogin: String? = null
)

/** Метаданные песни из all-music.json. */
@Serializable
data class MusicMeta(
    val name: String? = null,
    val genre: String? = null,
    val notes: List<NoteLv> = emptyList()
)

@Serializable
data class NoteLv(
    val lv: Double? = null
)
