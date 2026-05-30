package net.aquadx.aquacard

import net.aquadx.aquacard.crypto.CardFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardFormatTest {

    @Test
    fun serialNumberFormat() {
        assertEquals("02:FE:00:00:00:00:00:01", CardFormat.serialNumber("02FE000000000001"))
        assertEquals("02:FE:DE:AD:BE:EF:12:34", CardFormat.serialNumber("02FEDEADBEEF1234"))
    }

    @Test
    fun groupedAccessCodeFormat() {
        assertEquals("0021 5609 8321 6036 2497", CardFormat.groupedAccessCode("02FE000000000001"))
        assertEquals("0021 5854 6699 7440 9780", CardFormat.groupedAccessCode("02FEDEADBEEF1234"))
    }

    @Test
    fun importFromSerialAndAccessConverge() {
        assertEquals("02FE000000000001", CardFormat.idmFromSerial("02:FE:00:00:00:00:00:01"))
        assertEquals("02FE000000000001", CardFormat.idmFromSerial("02fe000000000001"))
        assertEquals("02FE000000000001", CardFormat.idmFromAccessCode("00215609832160362497"))
        assertEquals("02FE000000000001", CardFormat.idmFromAccessCode("0021 5609 8321 6036 2497"))
    }

    @Test
    fun rejectsInvalidInput() {
        assertNull(CardFormat.idmFromSerial("01:FE:00"))          // too short
        assertNull(CardFormat.idmFromSerial("ZZ:FE:00:00:00:00:00:01"))
    }

    @Test
    fun emulatableOnlyFor02FE() {
        assertTrue(CardFormat.isEmulatable("02FE000000000001"))
        assertFalse(CardFormat.isEmulatable("012E1A2B3C4D5E6F"))
    }
}
