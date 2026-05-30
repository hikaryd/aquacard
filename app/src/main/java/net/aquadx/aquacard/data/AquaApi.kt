package net.aquadx.aquacard.data

import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json

@Serializable
data class AquaUser(
    val username: String,
    val displayName: String? = null,
    val country: String? = null,
    val regTime: Long? = null,
    val profileBio: String? = null
)

@Serializable
data class GameSummaryResponse(
    val name: String? = null,
    val serverRank: Int? = null,
    val rating: Int? = null,
    val plays: Int? = null,
    val aquaUser: AquaUser? = null
)

@Serializable
data class LinkCardResponse(
    val status: String? = null,
    val message: String? = null,
    val extId: Int? = null
)

interface AquaService {
    @GET("api/v2/game/{game}/user-summary")
    suspend fun getGameSummary(
        @Path("game") game: String,
        @Query("username") username: String
    ): GameSummaryResponse

    @POST("api/v2/card/link")
    suspend fun linkCard(
        @Query("token") token: String,
        @Query("cardId") cardId: String, // 20-digit access code
        @Query("migrate") migrate: String = "mai2,chu3,ongeki,wacca"
    ): LinkCardResponse
}

object AquaApi {
    internal val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    fun createService(baseUrl: String): AquaService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
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
            .create(AquaService::class.java)
        }

    /**
     * Профильный сервис (полный профиль игрока). Переиспользует тот же [json],
     * чтобы не разъезжались настройки парсинга. Логирование BASIC — musicList
     * может содержать тысячи строк, BODY забивал бы лог.
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
