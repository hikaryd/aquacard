package net.aquadx.aquacard

import net.aquadx.aquacard.data.ScoreFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScoreFormatTest {

    @Test
    fun achievement_1008790_to_percent() {
        assertEquals("100.8790%", ScoreFormat.achievementPercent(1008790))
        assertEquals("99.0050%", ScoreFormat.achievementPercent(990050))
        assertEquals("—", ScoreFormat.achievementPercent(null))
    }

    @Test
    fun levelIndex_3_is_MASTER() {
        assertEquals("MASTER", ScoreFormat.levelName("mai2", 3))
        assertEquals("Re:MASTER", ScoreFormat.levelName("mai2", 4))
        assertEquals("ULTIMA", ScoreFormat.levelName("chu3", 4))
        assertEquals("?", ScoreFormat.levelName("mai2", null))
    }

    @Test
    fun bestTuple_mai_parses() {
        val e = ScoreFormat.parseBestTuple("mai2", listOf("834", "4", "19998", "1008790"))!!
        assertEquals(834, e.musicId)
        assertEquals(4, e.level)
        assertEquals(1008790, e.value)
    }

    @Test
    fun bestTuple_chu_parses() {
        val e = ScoreFormat.parseBestTuple("chu3", listOf("2422", "2", "993344"))!!
        assertEquals(2422, e.musicId)
        assertEquals(2, e.level)
        assertEquals(993344, e.value)
    }

    @Test
    fun bestTuple_shortAndLong_doNotThrow() {
        // Слишком короткий для mai (нужен индекс 3) -> null, без исключения
        assertNull(ScoreFormat.parseBestTuple("mai2", listOf("834")))
        // Пустой -> null
        assertNull(ScoreFormat.parseBestTuple("chu3", emptyList()))
        // Длиннее ожидаемого -> читаем нужный индекс, не падаем
        val long = ScoreFormat.parseBestTuple("mai2", listOf("1", "2", "3", "4", "5", "6"))
        assertEquals(1, long?.musicId)
        assertEquals(4, long?.value)
    }

    @Test
    fun formatRating_perGame() {
        assertEquals("16666", ScoreFormat.formatRating("mai2", 16666))
        assertEquals("17.00", ScoreFormat.formatRating("chu3", 1700))
        assertEquals("—", ScoreFormat.formatRating("mai2", null))
    }

    @Test
    fun songName_fallsBackToId() {
        assertEquals("22", ScoreFormat.songName(emptyMap(), 22))
    }

    @Test
    fun maiRank_thresholds() {
        assertEquals("SSS+", ScoreFormat.maiRank(1008790))
        assertEquals("SSS", ScoreFormat.maiRank(1000000))
        assertEquals("S", ScoreFormat.maiRank(970000))
        assertEquals("—", ScoreFormat.maiRank(null))
    }

    @Test
    fun comboAndSyncLabels() {
        assertEquals("AP+", ScoreFormat.comboLabel(4))
        assertNull(ScoreFormat.comboLabel(0))
        assertEquals("FDX", ScoreFormat.syncLabel(3))
        assertNull(ScoreFormat.syncLabel(null))
    }
}
