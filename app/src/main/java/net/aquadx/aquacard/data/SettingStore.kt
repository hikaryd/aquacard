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

    /** Последний успешно загруженный ник — для авто-загрузки профиля при открытии. */
    fun getLastUsername(): String? =
        prefs.getString(KEY_LAST_USERNAME, null)?.takeIf { it.isNotBlank() }

    fun setLastUsername(username: String) {
        prefs.edit().putString(KEY_LAST_USERNAME, username).apply()
    }

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_LAST_USERNAME = "last_username"
        const val DEFAULT_BASE_URL = "https://aquadx.net/aqua"
    }
}
