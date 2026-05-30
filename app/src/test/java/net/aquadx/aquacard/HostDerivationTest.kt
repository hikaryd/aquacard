package net.aquadx.aquacard

import net.aquadx.aquacard.data.AquaProfileRepository
import org.junit.Assert.assertEquals
import org.junit.Test

/** Деривация абсолютного URL all-music.json из API-базы для разных форм baseUrl. */
class HostDerivationTest {

    @Test
    fun staticBase_for_3_shapes() {
        assertEquals(
            "https://aquadx.net/d/mai2/00/all-music.json",
            AquaProfileRepository.allMusicUrl("https://aquadx.net/aqua", "mai2")
        )
        assertEquals(
            "https://aquadx.net/d/mai2/00/all-music.json",
            AquaProfileRepository.allMusicUrl("https://aquadx.net/aqua/", "mai2")
        )
        assertEquals(
            "https://host:8080/d/chu3/00/all-music.json",
            AquaProfileRepository.allMusicUrl("https://host:8080/", "chu3")
        )
    }

    @Test
    fun staticBase_strips_trailing_aqua() {
        assertEquals("https://aquadx.net", AquaProfileRepository.staticBase("https://aquadx.net/aqua"))
        assertEquals("https://host:8080", AquaProfileRepository.staticBase("https://host:8080/"))
    }
}
