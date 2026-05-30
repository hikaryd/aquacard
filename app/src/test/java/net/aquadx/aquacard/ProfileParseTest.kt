package net.aquadx.aquacard

import kotlinx.serialization.decodeFromString
import net.aquadx.aquacard.data.AquaApi
import net.aquadx.aquacard.data.GameBrief
import net.aquadx.aquacard.data.GameSummary
import net.aquadx.aquacard.data.MusicMeta
import net.aquadx.aquacard.data.MusicScoreDto
import net.aquadx.aquacard.data.TrendPoint
import net.aquadx.aquacard.data.UserRatingDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Образцы JSON сняты с docs/aquadx-api-spec.md. Проверяем парс обеих игр без исключений. */
class ProfileParseTest {

    private val json = AquaApi.json

    @Test
    fun summaryMai_parses() {
        val fx = """
            {"name":"Ivy","aquaUser":{"username":"Sigma","displayName":"DEMONDX","country":"US",
            "regTime":1748046905871,"profileBio":"x","profilePicture":"8781.png"},
            "serverRank":1,"accuracy":94.7967,"rating":16666,"ratingHighest":16666,
            "maxCombo":1282,"fullCombo":140,"allPerfect":111,"totalScore":1027767,"plays":2795,
            "lastVersion":"1.60.00","ranks":[{"name":"SSS+","count":987},{"name":"SSS","count":570}],
            "detailedRanks":{"13":{"SSS+":206}},"extraFuture":42}
        """.trimIndent()
        val s = json.decodeFromString<GameSummary>(fx)
        assertEquals(16666, s.rating)
        assertEquals(2795, s.plays)
        assertEquals("Sigma", s.aquaUser?.username)
        assertEquals("8781.png", s.aquaUser?.profilePicture)
        assertEquals(2, s.ranks.size)
        assertEquals("SSS+", s.ranks[0].name)
        assertEquals(987, s.ranks[0].count)
        assertEquals(94.7967, s.accuracy!!, 1e-6)
    }

    @Test
    fun ratingMai_parsesBestAndMusicList() {
        val fx = """
            {"best35":[["834","4","19998","1008790"]],"best15":[["11820","4","25510","1005156"]],
            "musicList":[{"musicId":22,"level":4,"playCount":4,"achievement":1005590,"comboStatus":0,
            "syncStatus":0,"deluxscoreMax":2625,"scoreRank":13,"extNum1":0}]}
        """.trimIndent()
        val r = json.decodeFromString<UserRatingDto>(fx)
        assertEquals(1, r.best35.size)
        assertEquals("1008790", r.best35[0][3])
        assertEquals(1, r.musicList.size)
        assertEquals(1005590, r.musicList[0].achievement)
        assertEquals(2625, r.musicList[0].deluxscoreMax)
    }

    @Test
    fun trend_parses() {
        val fx = """[{"date":"2025-05-13","rating":691,"plays":3},{"date":"2025-05-14","rating":1368,"plays":3}]"""
        val list = json.decodeFromString<List<TrendPoint>>(fx)
        assertEquals(2, list.size)
        assertEquals(1368, list[1].rating)
    }

    @Test
    fun userGames_parses() {
        val fx = """
            {"mai2":{"name":"Ivy","rating":16666,"lastLogin":"2026-05-29 23:53:32.0"},
            "chu3":{"name":"x","rating":52,"lastLogin":"2025-11-25T02:51:19"},"ongeki":null,"wacca":null}
        """.trimIndent()
        val map = json.decodeFromString<Map<String, GameBrief?>>(fx)
        assertEquals(16666, map["mai2"]?.rating)
        assertNull(map["ongeki"])
    }

    @Test
    fun allMusic_parses() {
        val fx = """
            {"8":{"name":"True Love Song","ver":"Ver1.00.00","composer":"Kai","genre":"maimai",
            "notes":[{"lv":5},{"lv":7.2},{"lv":10.2},{"lv":12.4}]}}
        """.trimIndent()
        val map = json.decodeFromString<Map<String, MusicMeta>>(fx)
        assertEquals("True Love Song", map["8"]?.name)
        assertEquals(4, map["8"]?.notes?.size)
        assertEquals(12.4, map["8"]?.notes?.get(3)?.lv!!, 1e-6)
    }

    @Test
    fun unknownFields_doNotThrow() {
        val fx = """{"musicId":22,"level":4,"achievement":1005590,"brandNewObj":{"nested":true},"arr":[1,2,3]}"""
        val dto = json.decodeFromString<MusicScoreDto>(fx)
        assertEquals(22, dto.musicId)
        assertEquals(1005590, dto.achievement)
    }
}
