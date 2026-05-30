package net.aquadx.aquacard.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File

/** Снимок профиля для кэша (без transient `errors`) + время записи. */
@Serializable
data class CachedProfile(
    val summary: GameSummary? = null,
    val detail: UserDetailDto? = null,
    val best: List<BestEntry> = emptyList(),
    val bestSecondary: List<BestEntry> = emptyList(),
    val scores: List<ProfileScore> = emptyList(),
    val recent: List<RecentEntry> = emptyList(),
    val trend: List<TrendPoint> = emptyList(),
    val meta: Map<Int, MusicMeta> = emptyMap(),
    val savedAtMillis: Long = 0L
) {
    fun toBundle(): ProfileBundle = ProfileBundle(
        summary = summary,
        detail = detail,
        best = best,
        bestSecondary = bestSecondary,
        scores = scores,
        recent = recent,
        trend = trend,
        meta = meta,
        errors = emptyList()
    )

    companion object {
        fun fromBundle(b: ProfileBundle, now: Long) = CachedProfile(
            summary = b.summary,
            detail = b.detail,
            best = b.best,
            bestSecondary = b.bestSecondary,
            scores = b.scores,
            recent = b.recent,
            trend = b.trend,
            meta = b.meta,
            savedAtMillis = now
        )
    }
}

/**
 * Дисковый кэш профиля (filesDir/profile_v2_<user>.json) + процесс-синглтон зеркало [mem],
 * чтобы кэш переживал пересоздание ProfileScreen при переключении вкладок (`when(currentTab)`).
 * Имя версионировано (v2): несовместимые старые файлы не читаются по имени.
 * Чтение толерантно: битый/несовместимый JSON → null (без краша).
 * `nowMillis` инъектируется снаружи — ProfileCache не дёргает время сам.
 */
class ProfileCache(private val dir: File) {

    fun read(username: String): CachedProfile? {
        val key = sanitize(username)
        mem[key]?.let { return it }
        val f = fileFor(key)
        if (!f.exists()) return null
        return try {
            val parsed = AquaApi.json.decodeFromString<CachedProfile>(f.readText())
            mem[key] = parsed
            parsed
        } catch (e: Exception) {
            null
        }
    }

    fun write(username: String, bundle: ProfileBundle, nowMillis: Long) {
        val key = sanitize(username)
        val cached = CachedProfile.fromBundle(bundle, nowMillis)
        mem[key] = cached
        try {
            fileFor(key).writeText(AquaApi.json.encodeToString(cached))
        } catch (e: Exception) {
            // запись кэша — best-effort; провал не критичен для UX
        }
    }

    private fun fileFor(key: String) = File(dir, "profile_v2_$key.json")

    private fun sanitize(username: String): String =
        username.trim().lowercase().replace(Regex("[^a-z0-9_-]"), "_").ifBlank { "_" }

    companion object {
        /** Процесс-синглтон зеркало (живёт дольше composition). */
        val mem = mutableMapOf<String, CachedProfile>()

        fun of(context: Context) = ProfileCache(context.filesDir)
    }
}
