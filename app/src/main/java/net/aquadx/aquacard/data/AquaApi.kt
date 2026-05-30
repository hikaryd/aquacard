package net.aquadx.aquacard.data

import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json

@Serializable
data class AquaUser(
    val username: String,
    val displayName: String? = null,
    val country: String? = null,
    val regTime: Long? = null,
    val profileBio: String? = null,
    val profilePicture: String? = null
)

object AquaApi {
    internal val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    /**
     * Профильный сервис (maimai). Логирование BASIC — musicList может содержать
     * тысячи строк, BODY забивал бы лог.
     */
    fun createProfileService(baseUrl: String): AquaProfileService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(cleanUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AquaProfileService::class.java)
    }
}
