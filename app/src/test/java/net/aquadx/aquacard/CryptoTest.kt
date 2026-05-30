package net.aquadx.aquacard

import net.aquadx.aquacard.crypto.AccessCode
import org.junit.Assert.assertEquals
import org.junit.Test

class CryptoTest {

    @Test
    fun testAccessCodeVectors() {
        val testCases = listOf(
            "02FE000000000001" to "00215609832160362497",
            "02FEDEADBEEF1234" to "00215854669974409780"
        )

        for ((idm, expected) in testCases) {
            val actual = AccessCode.aquaAccessCode(idm)
            assertEquals("IDm $idm did not convert correctly to Access Code", expected, actual)
        }
    }

    @Test
    fun testIdmValidator() {
        val validRegex = Regex("^02FE[0-9A-Fa-f]{12}$")
        
        val validIdms = listOf(
            "02FE000000000001",
            "02FEDEADBEEF1234",
            "02fe0000000000a1"
        )
        for (idm in validIdms) {
            assert(idm.uppercase().matches(validRegex)) { "Validator rejected a valid IDm: $idm" }
        }

        val invalidIdms = listOf(
            "01FE000000000001", // Wrong prefix
            "02FE00000000000G", // Non-hex
            "02FE000000000",    // Too short
            "02FE0000000000001" // Too long
        )
        for (idm in invalidIdms) {
            assert(!idm.uppercase().matches(validRegex)) { "Validator accepted an invalid IDm: $idm" }
        }
    }
}
