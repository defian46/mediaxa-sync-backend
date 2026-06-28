package com.mediaxa.business.suite

import com.mediaxa.business.suite.data.sync.ConflictResolver
import com.mediaxa.business.suite.data.sync.ConflictWinner
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [ConflictResolver] — covers all LWW rules and backoff calculation.
 */
class ConflictResolverTest {

    // ─── Last-Write-Wins Resolution ──────────────────────────────────────────

    @Test
    fun `local wins when local updatedAt is newer`() {
        val localTime = 1000L
        val remoteTime = 500L
        val result = ConflictResolver.resolve(localTime, remoteTime)
        assertEquals(ConflictWinner.LOCAL, result)
    }

    @Test
    fun `remote wins when remote updatedAt is newer`() {
        val localTime = 500L
        val remoteTime = 1000L
        val result = ConflictResolver.resolve(localTime, remoteTime)
        assertEquals(ConflictWinner.REMOTE, result)
    }

    @Test
    fun `remote wins on tie (conservative server preference)`() {
        val sameTime = 1000L
        val result = ConflictResolver.resolve(sameTime, sameTime)
        assertEquals(ConflictWinner.REMOTE, result)
    }

    // ─── Delete vs Update (Tombstone Semantics) ──────────────────────────────

    @Test
    fun `local DELETE wins over remote UPDATE`() {
        val result = ConflictResolver.resolveDeleteVsUpdate("DELETE", "UPDATE")
        assertEquals(ConflictWinner.LOCAL, result)
    }

    @Test
    fun `remote DELETE wins over local UPDATE`() {
        val result = ConflictResolver.resolveDeleteVsUpdate("UPDATE", "DELETE")
        assertEquals(ConflictWinner.REMOTE, result)
    }

    @Test
    fun `no conflict when both are UPDATE`() {
        val result = ConflictResolver.resolveDeleteVsUpdate("UPDATE", "UPDATE")
        assertEquals(ConflictWinner.NONE, result)
    }

    @Test
    fun `no conflict when both are CREATE`() {
        val result = ConflictResolver.resolveDeleteVsUpdate("CREATE", "CREATE")
        assertEquals(ConflictWinner.NONE, result)
    }

    // ─── Exponential Backoff Calculation ─────────────────────────────────────

    @Test
    fun `first retry has 30 second backoff`() {
        val delayMs = ConflictResolver.calculateBackoffDelayMs(0)
        assertEquals(30_000L, delayMs)
    }

    @Test
    fun `second retry doubles to 60 seconds`() {
        val delayMs = ConflictResolver.calculateBackoffDelayMs(1)
        assertEquals(60_000L, delayMs)
    }

    @Test
    fun `third retry doubles to 120 seconds`() {
        val delayMs = ConflictResolver.calculateBackoffDelayMs(2)
        assertEquals(120_000L, delayMs)
    }

    @Test
    fun `fourth retry is 240 seconds`() {
        val delayMs = ConflictResolver.calculateBackoffDelayMs(3)
        assertEquals(240_000L, delayMs)
    }

    @Test
    fun `backoff is capped at 60 minutes`() {
        val maxDelay = 3_600_000L // 60 minutes
        // After many retries, should not exceed cap
        val delayAt10 = ConflictResolver.calculateBackoffDelayMs(10)
        val delayAt20 = ConflictResolver.calculateBackoffDelayMs(20)
        assertEquals(maxDelay, delayAt10)
        assertEquals(maxDelay, delayAt20)
    }

    @Test
    fun `backoff with negative retry count treated as zero`() {
        val delayMs = ConflictResolver.calculateBackoffDelayMs(-1)
        assertEquals(30_000L, delayMs)
    }
}
