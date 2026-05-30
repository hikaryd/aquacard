package net.aquadx.aquacard.data

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Публичные читающие эндпоинты AquaDX (maimai/mai2) для отрисовки профиля.
 * Идентификатор игрока — username. allMusic берётся по АБСОЛЮТНОМУ @Url
 * (статик-хост, не /aqua API-база), иначе Retrofit резолвит относительный путь к /aqua → 404.
 */
interface AquaProfileService {

    @GET("api/v2/game/mai2/user-summary")
    suspend fun summary(@Query("username") username: String): GameSummary

    @GET("api/v2/game/mai2/user-detail")
    suspend fun detail(@Query("username") username: String): UserDetailDto

    @GET("api/v2/game/mai2/user-rating")
    suspend fun rating(@Query("username") username: String): UserRatingDto

    @GET("api/v2/game/mai2/recent")
    suspend fun recent(@Query("username") username: String): List<RecentPlayDto>

    @GET("api/v2/game/mai2/trend")
    suspend fun trend(@Query("username") username: String): List<TrendPoint>

    @GET("api/v2/card/user-games")
    suspend fun userGames(@Query("username") username: String): Map<String, GameBrief?>

    @GET
    suspend fun allMusic(@Url url: String): Map<String, MusicMeta>
}
