package net.aquadx.aquacard.data

import android.content.Context
import android.content.SharedPreferences

class SettingStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("aquacard_settings", Context.MODE_PRIVATE)

    fun getBaseUrl(): String {
        return prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun setBaseUrl(url: String) {
        prefs.edit().putString(KEY_BASE_URL, url).apply()
    }

    fun resetToDefault() {
        prefs.edit().putString(KEY_BASE_URL, DEFAULT_BASE_URL).apply()
    }

    companion object {
        private const val KEY_BASE_URL = "base_url"
        const val DEFAULT_BASE_URL = "https://aquadx.net/aqua"
    }
}
