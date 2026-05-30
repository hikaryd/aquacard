package net.aquadx.aquacard.data

/**
 * Чистая (НЕ-Compose) логика решения о загрузке профиля — stale-while-revalidate.
 * Вынесена из ProfileScreen, чтобы развилка покрывалась юнит-тестом, а в Compose
 * оставался только плумбинг.
 */
sealed interface Decision {
    /** Нет username — нечего грузить. */
    object Idle : Decision

    /** Кэш свежий — показать кэш, сеть не дёргать. */
    object ServeCachedOnly : Decision

    /** Показать кэш сразу и обновить в фоне. */
    object ServeCachedThenRefresh : Decision

    /** Кэша нет — грузить из сети. */
    object RefreshOnly : Decision
}

object CachePolicy {
    const val DEFAULT_THRESHOLD_MS = 60_000L

    /**
     * @param query текущий ник (пусто/null → Idle)
     * @param cached последний кэш для этого ника (или null)
     * @param now текущее время (инъекция — для тестируемости)
     * @param manualRefresh пользователь нажал refresh (обходит порог свежести)
     */
    fun decide(
        query: String?,
        cached: CachedProfile?,
        now: Long,
        thresholdMs: Long = DEFAULT_THRESHOLD_MS,
        manualRefresh: Boolean
    ): Decision {
        if (query.isNullOrBlank()) return Decision.Idle
        if (manualRefresh) {
            return if (cached != null) Decision.ServeCachedThenRefresh else Decision.RefreshOnly
        }
        if (cached == null) return Decision.RefreshOnly
        val fresh = now - cached.savedAtMillis <= thresholdMs
        return if (fresh) Decision.ServeCachedOnly else Decision.ServeCachedThenRefresh
    }
}
