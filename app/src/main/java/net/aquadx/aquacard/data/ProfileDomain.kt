package net.aquadx.aquacard.data

/**
 * Доменное ядро профиля: per-game sealed-модели (см. docs/profile-ui-plan.md §1, §3).
 * Репозиторий мапит толерантные wire-DTO в эти строгие типы по игре, и UI ветвится
 * исчерпывающим `when` — компилятор запрещает прочитать mai-поле у chu-строки,
 * исключая «тихий неверный рендер».
 */

sealed interface ProfileScore {
    val musicId: Int
    val level: Int

    data class Mai(
        override val musicId: Int,
        override val level: Int,
        val achievement: Int?,
        val deluxscore: Int?,
        val comboStatus: Int?,
        val syncStatus: Int?,
        val scoreRank: Int?
    ) : ProfileScore

    data class Chu(
        override val musicId: Int,
        override val level: Int,
        val score: Int?,
        val scoreRank: Int?,
        val isFullCombo: Boolean?,
        val isAllJustice: Boolean?
    ) : ProfileScore
}

sealed interface RecentEntry {
    val musicId: Int
    val level: Int
    val playDate: String?

    data class Mai(
        override val musicId: Int,
        override val level: Int,
        override val playDate: String?,
        val achievement: Int?,
        val rank: Int?,
        val comboStatus: Int?,
        val isClear: Boolean?
    ) : RecentEntry

    data class Chu(
        override val musicId: Int,
        override val level: Int,
        override val playDate: String?,
        val score: Int?,
        val rank: Int?
    ) : RecentEntry
}

/** Запись из секции Best: значение = achievement (mai) или score (chu). */
data class BestEntry(
    val musicId: Int,
    val level: Int,
    val value: Int
)

/** Полный результат загрузки профиля. Любая секция может быть пустой при отказе. */
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
