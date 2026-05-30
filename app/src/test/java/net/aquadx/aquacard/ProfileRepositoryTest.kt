package net.aquadx.aquacard

import kotlinx.coroutines.runBlocking
import net.aquadx.aquacard.data.AquaProfileRepository
import net.aquadx.aquacard.data.AquaProfileService
import net.aquadx.aquacard.data.GameBrief
import net.aquadx.aquacard.data.GameSummary
import net.aquadx.aquacard.data.MusicMeta
import net.aquadx.aquacard.data.MusicScoreDto
import net.aquadx.aquacard.data.RecentPlayDto
import net.aquadx.aquacard.data.ScoreFormat
import net.aquadx.aquacard.data.TrendPoint
import net.aquadx.aquacard.data.UserDetailDto
import net.aquadx.aquacard.data.UserRatingDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Фейковый сервис: каждая секция — лямбда, которая может бросить. */
private class FakeProfileService(
    val summaryFn: () -> GameSummary = { GameSummary(name = "Ivy", rating = 16666) },
    val detailFn: () -> UserDetailDto = { UserDetailDto(playerRating = 16666) },
    val ratingFn: () -> UserRatingDto = { UserRatingDto() },
    val recentFn: () -> List<RecentPlayDto> = { emptyList() },
    val trendFn: () -> List<TrendPoint> = { emptyList() },
    val userGamesFn: () -> Map<String, GameBrief?> = { emptyMap() },
    val allMusicFn: () -> Map<String, MusicMeta> = { emptyMap() }
) : AquaProfileService {
    override suspend fun summary(game: String, username: String) = summaryFn()
    override suspend fun detail(game: String, username: String) = detailFn()
    override suspend fun rating(game: String, username: String) = ratingFn()
    override suspend fun recent(game: String, username: String) = recentFn()
    override suspend fun trend(game: String, username: String) = trendFn()
    override suspend fun userGames(username: String) = userGamesFn()
    override suspend fun allMusic(url: String) = allMusicFn()
}

class ProfileRepositoryTest {

    // Свежий metaCache на каждый тест — без утечки между тестами и без сети.
    private fun repo(svc: AquaProfileService) =
        AquaProfileRepository(svc, metaCache = mutableMapOf())

    @Test
    fun oneSectionThrows_othersSurvive() = runBlocking {
        val svc = FakeProfileService(
            ratingFn = {
                UserRatingDto(
                    best35 = listOf(listOf("834", "4", "19998", "1008790")),
                    musicList = listOf(MusicScoreDto(musicId = 22, level = 3, achievement = 1005000))
                )
            },
            trendFn = { throw RuntimeException("trend down") }
        )
        val b = repo(svc).load("https://aquadx.net/aqua", "mai2", "Sigma")
        assertNotNull(b.summary)
        assertTrue("scores must survive", b.scores.isNotEmpty())
        assertTrue("best must survive", b.best.isNotEmpty())
        assertTrue("trend isolated to empty", b.trend.isEmpty())
        assertTrue("error recorded for trend", b.errors.any { it.contains("trend") })
    }

    @Test
    fun unsupportedGame_degradesToHeaderOnly() = runBlocking {
        val svc = FakeProfileService(
            ratingFn = {
                UserRatingDto(
                    best35 = listOf(listOf("1", "2", "3", "4")),
                    musicList = listOf(MusicScoreDto(musicId = 1, level = 1, achievement = 1000000))
                )
            }
        )
        val b = repo(svc).load("https://aquadx.net/aqua", "ongeki", "Sigma")
        assertNotNull(b.summary)
        assertTrue("no scores for unsupported game", b.scores.isEmpty())
        assertTrue("no recent for unsupported game", b.recent.isEmpty())
        assertTrue("no best for unsupported game", b.best.isEmpty())
    }

    @Test
    fun emptyMeta_fallsBackToMusicId() = runBlocking {
        val svc = FakeProfileService(allMusicFn = { emptyMap() })
        val b = repo(svc).load("https://aquadx.net/aqua", "mai2", "Sigma")
        assertTrue(b.meta.isEmpty())
        assertEquals("22", ScoreFormat.songName(b.meta, 22))
    }

    @Test
    fun supportedGame_mapsScoresToSealed() = runBlocking {
        val svc = FakeProfileService(
            ratingFn = {
                UserRatingDto(
                    musicList = listOf(
                        MusicScoreDto(musicId = 10, level = 3, achievement = 990000),
                        MusicScoreDto(musicId = 11, level = 2, achievement = 1005000)
                    )
                )
            },
            allMusicFn = { mapOf("10" to MusicMeta(name = "Song A")) }
        )
        val b = repo(svc).load("https://aquadx.net/aqua", "mai2", "Sigma")
        assertEquals(2, b.scores.size)
        // Отсортировано по achievement убыванием -> сначала 1005000 (musicId 11)
        assertEquals(11, b.scores.first().musicId)
        assertEquals("Song A", ScoreFormat.songName(b.meta, 10))
    }
}
