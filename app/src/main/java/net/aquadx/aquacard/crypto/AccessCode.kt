package net.aquadx.aquacard.crypto

object AccessCode {
    /**
     * Converts a 16-character hexadecimal FeliCa IDm (SID) into 
     * a 20-digit AquaDX Access Code.
     */
    fun aquaAccessCode(idmHex: String): String {
        val idm = java.lang.Long.parseUnsignedLong(idmHex, 16)
        return idm.toString().replace("-", "").padStart(20, '0')
    }
}
