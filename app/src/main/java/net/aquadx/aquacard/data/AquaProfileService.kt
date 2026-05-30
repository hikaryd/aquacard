package net.aquadx.aquacard.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Публичные читающие эндпоинты AquaDX для отрисовки профиля (docs/aquadx-api-spec.md).
 * Идентификатор игрока — username. allMusic берётся по АБСОЛЮТНОМУ @Url
 * (статик-хост, не /aqua API-база), иначе Retrofit резолвит относительный путь к /aqua → 404.
 */
interface AquaProfileService {

    @GET("api/v2/game/{game}/user-summary")
    suspend fun summary(
        @Path("game") game: String,
        @Query("username") username: String
    ): GameSummary

    @GET("api/v2/game/{game}/user-detail")
    suspend fun detail(
        @Path("game") game: String,
        @Query("username") username: String
    ): UserDetailDto

    @GET("api/v2/game/{game}/user-rating")
    suspend fun rating(
        @Path("game") game: String,
        @Query("username") username: String
    ): UserRatingDto

    @GET("api/v2/game/{game}/recent")
    suspend fun recent(
        @Path("game") game: String,
        @Query("username") username: String
    ): List<RecentPlayDto>

    @GET("api/v2/game/{game}/trend")
    suspend fun trend(
        @Path("game") game: String,
        @Query("username") username: String
    ): List<TrendPoint>

    @GET("api/v2/card/user-games")
    suspend fun userGames(
        @Query("username") username: String
    ): Map<String, GameBrief?>

    @GET
    suspend fun allMusic(@Url url: String): Map<String, MusicMeta>
}
