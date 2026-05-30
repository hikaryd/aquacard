package net.aquadx.aquacard

import net.aquadx.aquacard.data.AquaUser
import net.aquadx.aquacard.data.BestEntry
import net.aquadx.aquacard.data.GameSummary
import net.aquadx.aquacard.data.MusicMeta
import net.aquadx.aquacard.data.ProfileBundle
import net.aquadx.aquacard.data.ProfileCache
import net.aquadx.aquacard.data.ProfileScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ProfileCacheTest {

    private lateinit var dir: File
    private lateinit var cache: ProfileCache

    @Before
    fun setup() {
        dir = Files.createTempDirectory("pc").toFile()
        ProfileCache.mem.clear()
        cache = ProfileCache(dir)
    }

    @Test
    fun roundTrip_preservesScoresAndSummary() {
        val bundle = ProfileBundle(
            summary = GameSummary(rating = 16666, aquaUser = AquaUser(username = "Sigma")),
            scores = listOf(
                ProfileScore(22, 3, achievement = 1005000, deluxscore = 2625, comboStatus = 3, syncStatus = 2, scoreRank = 13)
            ),
            best = listOf(BestEntry(834, 4, 1008790)),
            meta = mapOf(22 to MusicMeta(name = "PANDORA"))
        )
        cache.write("Sigma", bundle, 123L)
        ProfileCache.mem.clear() // форсируем чтение из файла, не из зеркала
        val back = cache.read("Sigma")
        assertNotNull(back)
        assertEquals(123L, back!!.savedAtMillis)
        assertEquals(16666, back.summary?.rating)
        assertEquals(1, back.scores.size)
        assertEquals(22, back.scores[0].musicId)
        assertEquals("PANDORA", back.meta[22]?.name) // Map<Int,..> round-trip
    }

    @Test
    fun corruptFile_returnsNull() {
        File(dir, "profile_v2_sigma.json").writeText("{ this is not json")
        ProfileCache.mem.clear()
        assertNull(cache.read("Sigma"))
    }

    @Test
    fun incompatibleFormat_readsAsNull() {
        // валидный JSON, но не та структура (массив вместо объекта) → null, без краша
        File(dir, "profile_v2_sigma.json").writeText("[1,2,3]")
        ProfileCache.mem.clear()
        assertNull(cache.read("Sigma"))
    }
}
