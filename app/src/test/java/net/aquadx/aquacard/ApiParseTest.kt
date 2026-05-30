package net.aquadx.aquacard

import kotlinx.serialization.decodeFromString
import net.aquadx.aquacard.data.AquaApi
import net.aquadx.aquacard.data.GameSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiParseTest {

    /** Реальный ответ AquaDX user-summary: regTime — число, есть неизвестные поля. */
    @Test
    fun parsesRealUserSummaryWithNumericRegTime() {
        val fixture = """
            {"name":"Ivy","aquaUser":{"username":"Sigma","displayName":"DEMONDX","country":"US",
            "regTime":1748046905871,"profileLocation":"","profileBio":"x","profilePicture":"8781.png"},
            "serverRank":1,"accuracy":94.79,"rating":16666,"ratingHighest":16666,"plays":2795,
            "totalScore":1027767,"unknownFutureField":true}
        """.trimIndent()

        val r = AquaApi.json.decodeFromString<GameSummary>(fixture)

        assertEquals("Sigma", r.aquaUser?.username)
        assertEquals(1748046905871L, r.aquaUser?.regTime)
        assertEquals(16666, r.rating)
        assertEquals(2795, r.plays)
        assertEquals(1, r.serverRank)
    }
}
