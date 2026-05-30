package net.aquadx.aquacard

import net.aquadx.aquacard.data.CachePolicy
import net.aquadx.aquacard.data.CachedProfile
import net.aquadx.aquacard.data.Decision
import org.junit.Assert.assertEquals
import org.junit.Test

class CachePolicyTest {

    private val base = 1_000_000L
    private val cached = CachedProfile(savedAtMillis = base)

    @Test
    fun noUsername_idle() {
        assertEquals(Decision.Idle, CachePolicy.decide(null, cached, base, manualRefresh = false))
        assertEquals(Decision.Idle, CachePolicy.decide("   ", cached, base, manualRefresh = false))
    }

    @Test
    fun noCache_refreshOnly() {
        assertEquals(Decision.RefreshOnly, CachePolicy.decide("Sigma", null, base, manualRefresh = false))
    }

    @Test
    fun freshCache_serveCachedOnly() {
        assertEquals(
            Decision.ServeCachedOnly,
            CachePolicy.decide("Sigma", cached, base + 30_000, manualRefresh = false)
        )
    }

    @Test
    fun staleCache_serveCachedThenRefresh() {
        assertEquals(
            Decision.ServeCachedThenRefresh,
            CachePolicy.decide("Sigma", cached, base + 120_000, manualRefresh = false)
        )
    }

    @Test
    fun manualRefresh_bypassesThreshold() {
        // свежий кэш, но ручной refresh → всё равно обновляем
        assertEquals(
            Decision.ServeCachedThenRefresh,
            CachePolicy.decide("Sigma", cached, base + 1_000, manualRefresh = true)
        )
        // ручной refresh без кэша → просто грузим
        assertEquals(
            Decision.RefreshOnly,
            CachePolicy.decide("Sigma", null, base, manualRefresh = true)
        )
    }
}
