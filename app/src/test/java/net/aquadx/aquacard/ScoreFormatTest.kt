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
        assertEquals("MASTER", ScoreFormat.levelName(3))
        assertEquals("Re:MASTER", ScoreFormat.levelName(4))
        assertEquals("?", ScoreFormat.levelName(null))
    }

    @Test
    fun bestTuple_mai_parses() {
        val e = ScoreFormat.parseBestTuple(listOf("834", "4", "19998", "1008790"))!!
        assertEquals(834, e.musicId)
        assertEquals(4, e.level)
        assertEquals(1008790, e.value)
    }

    @Test
    fun bestTuple_shortAndLong_doNotThrow() {
        // Слишком короткий (нужен индекс 3) -> null, без исключения
        assertNull(ScoreFormat.parseBestTuple(listOf("834")))
        // Пустой -> null
        assertNull(ScoreFormat.parseBestTuple(emptyList()))
        // Длиннее ожидаемого -> читаем индекс 3, не падаем
        val long = ScoreFormat.parseBestTuple(listOf("1", "2", "3", "4", "5", "6"))
        assertEquals(1, long?.musicId)
        assertEquals(4, long?.value)
    }

    @Test
    fun formatRating_isRawInt() {
        assertEquals("16666", ScoreFormat.formatRating(16666))
        assertEquals("—", ScoreFormat.formatRating(null))
    }

    @Test
    fun bestValueLabel_isPercent() {
        assertEquals("100.8790%", ScoreFormat.bestValueLabel(1008790))
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
