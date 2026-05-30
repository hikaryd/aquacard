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

    private const val HEX = "0123456789ABCDEF"

    /** 16-hex -> как есть; 12-hex -> 02FE+суффикс; иначе null. */
    private fun hexToIdm(s: String): String? {
        val up = s.uppercase(Locale.ROOT)
        return when {
            up.length == 16 && up.all { it in HEX } -> up
            up.length == 12 && up.all { it in HEX } -> "02FE$up"
            else -> null
        }
    }

    /**
     * Умный разбор «номера карты»: принимает то, что удобно скопировать из eamemu или AquaDX:
     *  - IDm: 16 hex, либо 12-hex суффикс (02FE добавится сам); двоеточия/пробелы/дефисы игнорируются;
     *  - 20-значный Access Code (с пробелами или без);
     *  - eamemu-JSON: {"sid":"02FE…"} (в т.ч. внутри массива).
     * Возвращает 16-hex IDm или null.
     */
    fun idmFromAny(raw: String): String? {
        Regex("\"sid\"\\s*:\\s*\"([0-9A-Fa-f]{12,16})\"").find(raw)?.let { m ->
            hexToIdm(m.groupValues[1])?.let { return it }
        }
        val cleaned = raw.filterNot { it.isWhitespace() || it == ':' || it == '-' }
        hexToIdm(cleaned)?.let { return it }
        val digits = raw.filter { it.isDigit() }
        if (digits.length == 20) return idmFromAccessCode(digits)
        return null
    }

    /** Имя карты из eamemu-JSON {"name":"…"} — для авто-подстановки при импорте. */
    fun nameFromImport(raw: String): String? =
        Regex("\"name\"\\s*:\\s*\"([^\"]*)\"").find(raw)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
}
