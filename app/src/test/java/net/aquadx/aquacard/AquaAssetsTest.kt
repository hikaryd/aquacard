package net.aquadx.aquacard

import net.aquadx.aquacard.data.AquaAssets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AquaAssetsTest {

    private val base = "https://aquadx.net/aqua"

    @Test
    fun jacket_pads_to_6_digits() {
        assertEquals("https://aquadx.net/d/mai2/music/000011.png", AquaAssets.jacketUrl(base, 11))
        assertEquals("https://aquadx.net/d/mai2/music/001571.png", AquaAssets.jacketUrl(base, 1571))
        assertEquals("https://aquadx.net/d/mai2/music/000100.png", AquaAssets.jacketUrl(base, 100))
    }

    @Test
    fun jacket_highId_isLossyButNoThrow() {
        // musicId >= 10000 теряет старшие цифры (документированный lossy-кейс), но не падает
        assertEquals("https://aquadx.net/d/mai2/music/001451.png", AquaAssets.jacketUrl(base, 11451))
    }

    @Test
    fun avatar_blank_returnsNull() {
        assertNull(AquaAssets.avatarUrlOrNull(base, null))
        assertNull(AquaAssets.avatarUrlOrNull(base, ""))
        assertNull(AquaAssets.avatarUrlOrNull(base, "   "))
    }

    @Test
    fun avatar_builds_portrait_url() {
        assertEquals(
            "https://aquadx.net/uploads/net/portrait/8781.png",
            AquaAssets.avatarUrlOrNull(base, "8781.png")
        )
    }

    @Test
    fun selfHosted_shape() {
        assertEquals(
            "https://host:8080/d/mai2/music/000011.png",
            AquaAssets.jacketUrl("https://host:8080/", 11)
        )
        assertEquals(
            "https://host:8080/uploads/net/portrait/8781.png",
            AquaAssets.avatarUrlOrNull("https://host:8080/", "8781.png")
        )
    }
}
