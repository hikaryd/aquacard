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

    // /recent отдаёт партии по возрастанию (старые первыми) — сортируем по дате УБЫВАНИЕМ,
    // чтобы «последние» были действительно последними. Дата нормализуется (срез хвостового ".0").
    private fun mapRecent(list: List<RecentPlayDto>): List<RecentEntry> =
        list.sortedByDescending { (it.userPlayDate ?: it.playDate ?: "").substringBefore('.') }
            .map { dto ->
                RecentEntry(
                    musicId = dto.musicId,
                    level = dto.level ?: 0,
                    playDate = dto.userPlayDate ?: dto.playDate,
                    achievement = dto.achievement,
                    rank = dto.scoreRank,
                    comboStatus = dto.comboStatus,
                    isClear = dto.isClear,
                    syncStatus = dto.syncStatus,
                    deluxscore = dto.deluxscore,
                    beforeRating = dto.beforeRating,
                    afterRating = dto.afterRating,
                    placeName = dto.placeName,
                    maxCombo = dto.maxCombo,
                    totalCombo = dto.totalCombo,
                    fastCount = dto.fastCount,
                    lateCount = dto.lateCount,
                    isFullCombo = dto.isFullCombo,
                    isAllPerfect = dto.isAllPerfect,
                    trackNo = dto.trackNo,
                    judges = judgeBreakdown(dto),
                    notes = noteBreakdown(dto)
                )
            }

    private fun judgeBreakdown(d: RecentPlayDto): JudgeBreakdown? {
        val crit = sum(d.tapCriticalPerfect, d.holdCriticalPerfect, d.slideCriticalPerfect, d.touchCriticalPerfect, d.breakCriticalPerfect)
        val perfect = sum(d.tapPerfect, d.holdPerfect, d.slidePerfect, d.touchPerfect, d.breakPerfect)
        val great = sum(d.tapGreat, d.holdGreat, d.slideGreat, d.touchGreat, d.breakGreat)
        val good = sum(d.tapGood, d.holdGood, d.slideGood, d.touchGood, d.breakGood)
        val miss = sum(d.tapMiss, d.holdMiss, d.slideMiss, d.touchMiss, d.breakMiss)
        return if (crit + perfect + great + good + miss == 0) null
        else JudgeBreakdown(crit, perfect, great, good, miss)
    }

    private fun noteBreakdown(d: RecentPlayDto): NoteBreakdown? {
        val tap = sum(d.tapCriticalPerfect, d.tapPerfect, d.tapGreat, d.tapGood, d.tapMiss)
        val hold = sum(d.holdCriticalPerfect, d.holdPerfect, d.holdGreat, d.holdGood, d.holdMiss)
        val slide = sum(d.slideCriticalPerfect, d.slidePerfect, d.slideGreat, d.slideGood, d.slideMiss)
        val touch = sum(d.touchCriticalPerfect, d.touchPerfect, d.touchGreat, d.touchGood, d.touchMiss)
        val brk = sum(d.breakCriticalPerfect, d.breakPerfect, d.breakGreat, d.breakGood, d.breakMiss)
        return if (tap + hold + slide + touch + brk == 0) null
        else NoteBreakdown(tap, hold, slide, touch, brk)
    }

    private fun sum(vararg v: Int?): Int = v.sumOf { it ?: 0 }

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
