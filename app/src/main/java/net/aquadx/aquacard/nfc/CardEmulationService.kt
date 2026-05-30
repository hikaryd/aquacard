package net.aquadx.aquacard.nfc

import android.nfc.cardemulation.HostNfcFService
import android.os.Bundle
import android.util.Log

class CardEmulationService : HostNfcFService() {

    override fun processNfcFPacket(command: ByteArray, extras: Bundle?): ByteArray? {
        if (command.size < 10) {
            return null
        }
        
        Log.d(TAG, "processNfcFPacket: received ${command.joinToString { String.format("%02X", it) }}")
        
        // Command byte is at index 1 in FeliCa packet structure
        val commandCode = command[1].toInt() and 0xFF
        
        // FeliCa Request Response code is 0x04
        if (commandCode == 0x04) {
            // IDm is contained at command[2..9] (8 bytes)
            val idm = command.copyOfRange(2, 10)
            
            // Return Response Packet:
            // byte 0: Length = 11 (0x0B)
            // byte 1: Response Command Code = 0x05 (Request Response Code)
            // byte 2..9: IDm
            // byte 10: Mode/Status = 0x00
            val response = ByteArray(11)
            response[0] = 0x0B.toByte()
            response[1] = 0x05.toByte()
            System.arraycopy(idm, 0, response, 2, 8)
            response[10] = 0x00.toByte()
            
            Log.d(TAG, "processNfcFPacket: sending FeliCa response ${response.joinToString { String.format("%02X", it) }}")
            return response
        }
        
        // Polling (0x00) is handled by the OS framework automatically, mapping the SC to SENSF_RES
        return null
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "onDeactivated: reason = $reason")
    }

    companion object {
        private const val TAG = "CardEmulationService"
    }
}
