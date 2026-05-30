package net.aquadx.aquacard.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

/**
 * Оркестрация загрузки профиля maimai (docs/profile-ui-plan.md §3.3).
 * Все секции грузятся параллельно; try/catch ВНУТРИ каждого async, поэтому отказ
 * одной секции не отменяет соседей (supervisorScope) и пишется в errors.
 */
class AquaProfileRepository(
    private val service: AquaProfileService,
    private val metaCache: MutableMap<String, Map<Int, MusicMeta>> = sharedMetaCache
) {

    suspend fun load(baseUrl: String, username: String): ProfileBundle =
        withContext(Dispatchers.IO) {
            val errors = mutableListOf<String>()
            supervisorScope {
                val summaryD = async { section(errors, "summary") { service.summary(username) } }
                val detailD = async { section(errors, "detail") { service.detail(username) } }
                val ratingD = async { section(errors, "rating") { service.rating(username) } }
                val recentD = async { section(errors, "recent") { service.recent(username) } }
                val trendD = async { section(errors, "trend") { service.trend(username) } }
                val metaD = async { section(errors, "meta") { loadMeta(baseUrl) } }

                val rating = ratingD.await()
                ProfileBundle(
                    summary = summaryD.await(),
                    detail = detailD.await(),
                    best = mapBest(rating?.best35 ?: emptyList()),
                    bestSecondary = mapBest(rating?.best15 ?: emptyList()),
                    scores = mapScores(rating?.musicList ?: emptyList()),
                    recent = mapRecent(recentD.await() ?: emptyList()),
                    trend = trendD.await() ?: emptyList(),
                    meta = metaD.await() ?: emptyMap(),
                    errors = errors
                )
            }
        }

    private suspend fun loadMeta(baseUrl: String): Map<Int, MusicMeta> {
        metaCache[GAME]?.let { return it }
        val raw = service.allMusic(allMusicUrl(baseUrl))
        val parsed = raw.mapNotNull { (key, value) -> key.toIntOrNull()?.let { it to value } }.toMap()
        metaCache[GAME] = parsed
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

    private fun mapBest(tuples: List<List<String>>): List<BestEntry> =
        tuples.mapNotNull { ScoreFormat.parseBestTuple(it) }

    private fun mapScores(list: List<MusicScoreDto>): List<ProfileScore> =
        list.map { dto ->
            ProfileScore(
                musicId = dto.musicId,
                level = dto.level ?: 0,
                achievement = dto.achievement,
                deluxscore = dto.deluxscoreMax,
                comboStatus = dto.comboStatus,
                syncStatus = dto.syncStatus,
                scoreRank = dto.scoreRank
            )
        }.sortedByDescending { it.achievement ?: 0 }

    private fun mapRecent(list: List<RecentPlayDto>): List<RecentEntry> =
        list.map { dto ->
            RecentEntry(
                musicId = dto.musicId,
                level = dto.level ?: 0,
                playDate = dto.userPlayDate ?: dto.playDate,
                achievement = dto.achievement,
                rank = dto.scoreRank ?: dto.rank,
                comboStatus = dto.comboStatus,
                isClear = dto.isClear,
                syncStatus = dto.syncStatus,
                deluxscore = dto.deluxscore,
                beforeRating = dto.beforeRating,
                afterRating = dto.afterRating,
                placeName = dto.placeName
            )
        }

    companion object {
        private const val GAME = "mai2"
        val sharedMetaCache = mutableMapOf<String, Map<Int, MusicMeta>>()

        /** Статик-хост ассетов из API-базы: убираем хвостовой /aqua, чтобы получить корень. */
        fun staticBase(baseUrl: String): String {
            val trimmed = baseUrl.trim().removeSuffix("/")
            return if (trimmed.endsWith("/aqua")) trimmed.removeSuffix("/aqua") else trimmed
        }

        fun allMusicUrl(baseUrl: String): String =
            "${staticBase(baseUrl)}/d/$GAME/00/all-music.json"
    }
}
