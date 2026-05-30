package net.aquadx.aquacard.data

import kotlinx.serialization.Serializable

/**
 * Доменное ядро профиля (maimai-only). Репозиторий мапит толерантные wire-DTO в эти
 * плоские типы. `@Serializable` нужен для дискового кэша (ProfileCache/CachedProfile).
 */

/** Один скор песни maimai. */
@Serializable
data class ProfileScore(
    val musicId: Int,
    val level: Int,
    val achievement: Int?,
    val deluxscore: Int?,
    val comboStatus: Int?,
    val syncStatus: Int?,
    val scoreRank: Int?
)

/** Разбивка джаджментов (агрегат по типам нот). */
@Serializable
data class JudgeBreakdown(
    val crit: Int = 0,
    val perfect: Int = 0,
    val great: Int = 0,
    val good: Int = 0,
    val miss: Int = 0
)

/** Кол-во нот по типам. */
@Serializable
data class NoteBreakdown(
    val tap: Int = 0,
    val hold: Int = 0,
    val slide: Int = 0,
    val touch: Int = 0,
    val brk: Int = 0
)

/** Недавняя партия maimai (полная запись для детального экрана). */
@Serializable
data class RecentEntry(
    val musicId: Int,
    val level: Int,
    val playDate: String?,
    val achievement: Int?,
    val rank: Int?,
    val comboStatus: Int?,
    val isClear: Boolean?,
    val syncStatus: Int? = null,
    val deluxscore: Int? = null,
    val beforeRating: Int? = null,
    val afterRating: Int? = null,
    val placeName: String? = null,
    val maxCombo: Int? = null,
    val totalCombo: Int? = null,
    val fastCount: Int? = null,
    val lateCount: Int? = null,
    val isFullCombo: Boolean? = null,
    val isAllPerfect: Boolean? = null,
    val trackNo: Int? = null,
    val judges: JudgeBreakdown? = null,
    val notes: NoteBreakdown? = null
)

/** Запись из секции Best: значение = achievement. */
@Serializable
data class BestEntry(
    val musicId: Int,
    val level: Int,
    val value: Int
)

/** Полный результат загрузки профиля. Любая секция может быть пустой при отказе. */
@Serializable
data class ProfileBundle(
    val summary: GameSummary? = null,
    val detail: UserDetailDto? = null,
    val best: List<BestEntry> = emptyList(),
    val bestSecondary: List<BestEntry> = emptyList(),
    val scores: List<ProfileScore> = emptyList(),
    val recent: List<RecentEntry> = emptyList(),
    val trend: List<TrendPoint> = emptyList(),
    val meta: Map<Int, MusicMeta> = emptyMap(),
    val errors: List<String> = emptyList()
)
