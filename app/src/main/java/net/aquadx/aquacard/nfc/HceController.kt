package net.aquadx.aquacard.nfc

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.cardemulation.NfcFCardEmulation
import android.util.Log

class HceController(private val context: Context) {
    
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)
    private var nfcFCardEmulation: NfcFCardEmulation? = null
    private val serviceComponent = ComponentName(context, CardEmulationService::class.java)

    init {
        nfcAdapter?.let {
            nfcFCardEmulation = NfcFCardEmulation.getInstance(it)
            // Register System Code = 4000
            nfcFCardEmulation?.registerSystemCodeForService(serviceComponent, "4000")
        }
    }

    /**
     * Check if the device hardware + OS supports NFC Host Card Emulation for FeliCa (HCE-F).
     */
    fun isHceFSupported(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION_NFCF)
    }

    /**
     * Check if the device's NFC hardware is enabled.
     */
    fun isNfcEnabled(): Boolean {
        return nfcAdapter?.isEnabled == true
    }

    /**
     * Set the dynamic FeliCa NFCID2 (IDm) for our service.
     * Must be exactly 16 hex characters, starting with "02FE" (Android constraint).
     */
    fun selectCard(idm: String): Boolean {
        val uppercaseIdm = idm.uppercase()
        require(uppercaseIdm.length == 16 && uppercaseIdm.matches(Regex("[0-9A-Fa-f]+"))) {
            "IDm must be exactly 16 hexadecimal characters"
        }
        require(uppercaseIdm.startsWith("02FE")) {
            "Android HCE-F requires NFCID2 to start with '02FE'"
        }
        
        Log.d(TAG, "selectCard: assigning dynamic NFCID2/IDm: $uppercaseIdm")
        return nfcFCardEmulation?.setNfcid2ForService(serviceComponent, uppercaseIdm) ?: false
    }

    /**
     * Enables HCE-F routing and foreground dispatch for the specified activity.
     */
    fun enableEmulation(activity: Activity): Boolean {
        return try {
            nfcFCardEmulation?.enableService(activity, serviceComponent)
            Log.d(TAG, "enableEmulation: service enabled in foreground")
            true
        } catch (e: Exception) {
            Log.e(TAG, "enableEmulation failed", e)
            false
        }
    }

    /**
     * Disables dynamic HCE-F routing to return NFC stack to its default state.
     */
    fun disableEmulation(activity: Activity): Boolean {
        return try {
            nfcFCardEmulation?.disableService(activity)
            Log.d(TAG, "disableEmulation: service disabled")
            true
        } catch (e: Exception) {
            Log.e(TAG, "disableEmulation failed", e)
            false
        }
    }

    companion object {
        private const val TAG = "HceController"
    }
}
