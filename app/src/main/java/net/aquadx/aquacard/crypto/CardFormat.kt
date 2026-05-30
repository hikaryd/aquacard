package net.aquadx.aquacard.crypto

import java.math.BigInteger
import java.util.Locale

/** Форматирование/разбор идентификаторов карты под форму AquaDX «Link Card». */
object CardFormat {

    /** IDm (16 hex) -> Serial Number вида "02:FE:00:00:00:00:00:01". */
    fun serialNumber(idmHex: String): String =
        idmHex.uppercase(Locale.ROOT).chunked(2).joinToString(":")

    /** IDm -> 20-значный Access Code группами по 4: "0021 5609 8321 6036 2497". */
    fun groupedAccessCode(idmHex: String): String =
        AccessCode.aquaAccessCode(idmHex).chunked(4).joinToString(" ")

    /** Serial Number (с двоеточиями/пробелами или без) -> 16-hex IDm, либо null. */
    fun idmFromSerial(serial: String): String? {
        val hex = serial.filterNot { it.isWhitespace() || it == ':' || it == '-' }.uppercase(Locale.ROOT)
        return if (hex.length == 16 && hex.all { it in "0123456789ABCDEF" }) hex else null
    }

    /** Access Code (с пробелами или без) -> 16-hex IDm, либо null. */
    fun idmFromAccessCode(code: String): String? {
        val digits = code.filter { it.isDigit() }
        if (digits.isEmpty() || digits.length > 20) return null
        val value = BigInteger(digits)
        if (value.bitLength() > 64) return null // не помещается в 64-битный IDm
        return String.format("%016X", value.toLong())
    }

    /** Эмулировать можно только карты, чей IDm начинается с 02FE (ограничение Android HCE-F). */
    fun isEmulatable(idmHex: String): Boolean =
        idmHex.uppercase(Locale.ROOT).startsWith("02FE")
}
