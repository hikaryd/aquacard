package net.aquadx.aquacard.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

/**
 * Оркестрация загрузки профиля (docs/profile-ui-plan.md §3.3).
 * Все секции грузятся параллельно; try/catch ВНУТРИ каждого async, поэтому отказ
 * одной секции не отменяет соседей (supervisorScope) и пишется в errors.
 * Толерантные wire-DTO мапятся в строгие per-game sealed-модели по игре.
 */
class AquaProfileRepository(
    private val service: AquaProfileService,
    private val metaCache: MutableMap<String, Map<Int, MusicMeta>> = sharedMetaCache
) {

    suspend fun load(baseUrl: String, game: String, username: String): ProfileBundle =
        withContext(Dispatchers.IO) {
            val errors = mutableListOf<String>()
            supervisorScope {
                val summaryD = async { section(errors, "summary") { service.summary(game, username) } }
                val detailD = async { section(errors, "detail") { service.detail(game, username) } }
                val ratingD = async { section(errors, "rating") { service.rating(game, username) } }
                val recentD = async { section(errors, "recent") { service.recent(game, username) } }
                val trendD = async { section(errors, "trend") { service.trend(game, username) } }
                val metaD = async { section(errors, "meta") { loadMeta(baseUrl, game) } }

                val rating = ratingD.await()
                ProfileBundle(
                    summary = summaryD.await(),
                    detail = detailD.await(),
                    best = mapBest(game, primaryBest(game, rating)),
                    bestSecondary = mapBest(game, secondaryBest(game, rating)),
                    scores = mapScores(game, rating?.musicList ?: emptyList()),
                    recent = mapRecent(game, recentD.await() ?: emptyList()),
                    trend = trendD.await() ?: emptyList(),
                    meta = metaD.await() ?: emptyMap(),
                    errors = errors
                )
            }
        }

    private suspend fun loadMeta(baseUrl: String, game: String): Map<Int, MusicMeta> {
        metaCache[game]?.let { return it }
        val raw = service.allMusic(allMusicUrl(baseUrl, game))
        val parsed = raw.mapNotNull { (key, value) -> key.toIntOrNull()?.let { it to value } }.toMap()
        metaCache[game] = parsed
        return parsed
    }

    /** Inline → suspend-вызовы внутри block разрешены; ошибка секции изолируется в null. */
    private inline fun <T> section(errors: MutableList<String>, name: String, block: () -> T): T? =
        try {
            block()
        } catch (e: Exception) {
            synchronized(errors) { errors.add("$name: ${e.message ?: e::class.simpleName ?: "error"}") }
            null
        }

    private fun primaryBest(game: String, rating: UserRatingDto?): List<List<String>> =
        when (game) {
            "mai2" -> rating?.best35
            "chu3" -> rating?.best30
            else -> null
        } ?: emptyList()

    private fun secondaryBest(game: String, rating: UserRatingDto?): List<List<String>> =
        when (game) {
            "mai2" -> rating?.best15
            "chu3" -> rating?.recent10
            else -> null
        } ?: emptyList()

    private fun mapBest(game: String, tuples: List<List<String>>): List<BestEntry> =
        tuples.mapNotNull { ScoreFormat.parseBestTuple(game, it) }

    private fun mapScores(game: String, list: List<MusicScoreDto>): List<ProfileScore> =
        list.mapNotNull { dto ->
            val level = dto.level ?: 0
            when (game) {
                "mai2" -> ProfileScore.Mai(
                    musicId = dto.musicId, level = level, achievement = dto.achievement,
                    deluxscore = dto.deluxscoreMax, comboStatus = dto.comboStatus,
                    syncStatus = dto.syncStatus, scoreRank = dto.scoreRank
                )
                "chu3" -> ProfileScore.Chu(
                    musicId = dto.musicId, level = level, score = dto.scoreMax,
                    scoreRank = dto.scoreRank, isFullCombo = dto.isFullCombo, isAllJustice = dto.isAllJustice
                )
                else -> null
            }
        }.sortedByDescending {
            when (it) {
                is ProfileScore.Mai -> it.achievement ?: 0
                is ProfileScore.Chu -> it.score ?: 0
            }
        }

    private fun mapRecent(game: String, list: List<RecentPlayDto>): List<RecentEntry> =
        list.mapNotNull { dto ->
            val level = dto.level ?: 0
            val date = dto.userPlayDate ?: dto.playDate
            when (game) {
                "mai2" -> RecentEntry.Mai(
                    musicId = dto.musicId, level = level, playDate = date,
                    achievement = dto.achievement, rank = dto.scoreRank ?: dto.rank,
                    comboStatus = dto.comboStatus, isClear = dto.isClear
                )
                "chu3" -> RecentEntry.Chu(
                    musicId = dto.musicId, level = level, playDate = date,
                    score = dto.score, rank = dto.rank ?: dto.scoreRank
                )
                else -> null
            }
        }

    companion object {
        val sharedMetaCache = mutableMapOf<String, Map<Int, MusicMeta>>()

        /** Статик-хост ассетов из API-базы: убираем хвостовой /aqua, чтобы получить корень. */
        fun staticBase(baseUrl: String): String {
            val trimmed = baseUrl.trim().removeSuffix("/")
            return if (trimmed.endsWith("/aqua")) trimmed.removeSuffix("/aqua") else trimmed
        }

        fun allMusicUrl(baseUrl: String, game: String): String =
            "${staticBase(baseUrl)}/d/$game/00/all-music.json"
    }
}
