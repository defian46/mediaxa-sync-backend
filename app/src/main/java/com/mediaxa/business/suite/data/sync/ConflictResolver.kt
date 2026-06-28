package com.mediaxa.business.suite.data.sync

import com.mediaxa.business.suite.data.local.entity.SyncQueueItem

/**
 * Last-Write-Wins conflict resolver for Offline-First synchronization.
 *
 * When the same entity is mutated on multiple devices simultaneously,
 * the version with the higher [SyncQueueItem]-associated `updatedAt`
 * timestamp is considered the authoritative version.
 *
 * Resolution rules:
 * - Higher updatedAt wins (Last-Write-Wins)
 * - If timestamps are equal, server version wins (conservative approach)
 * - DELETE operations always win over UPDATE (tombstone semantics)
 */
object ConflictResolver {

    /**
     * Determines which version should be kept given two conflicting records.
     *
     * @param localUpdatedAt  Local record's updatedAt epoch millis
     * @param remoteUpdatedAt Remote record's updatedAt epoch millis
     * @return [ConflictWinner] indicating which side wins
     */
    fun resolve(localUpdatedAt: Long, remoteUpdatedAt: Long): ConflictWinner {
        return when {
            localUpdatedAt > remoteUpdatedAt -> ConflictWinner.LOCAL
            remoteUpdatedAt > localUpdatedAt -> ConflictWinner.REMOTE
            else -> ConflictWinner.REMOTE // Tie → server wins (conservative)
        }
    }

    /**
     * Special case: a DELETE operation always wins over an UPDATE.
     * This implements tombstone semantics — once deleted, always deleted.
     */
    fun resolveDeleteVsUpdate(
        localOperation: String,
        remoteOperation: String
    ): ConflictWinner {
        return when {
            localOperation == "DELETE" -> ConflictWinner.LOCAL
            remoteOperation == "DELETE" -> ConflictWinner.REMOTE
            else -> ConflictWinner.NONE
        }
    }

    /**
     * Calculate the exponential backoff delay for a retry attempt.
     *
     * Schedule:
     *   - Attempt 1: 30 seconds
     *   - Attempt 2: 60 seconds
     *   - Attempt 3: 120 seconds
     *   - Attempt 4: 240 seconds
     *   - Attempt 5+: 60 minutes (capped)
     *
     * @param retryCount Number of retries already attempted (0-indexed)
     * @return Delay in milliseconds before the next retry
     */
    fun calculateBackoffDelayMs(retryCount: Int): Long {
        val baseDelayMs = 30_000L           // 30 seconds
        val maxDelayMs = 3_600_000L         // 60 minutes cap
        val multiplier = Math.pow(2.0, retryCount.coerceAtLeast(0).toDouble()).toLong()
        return (baseDelayMs * multiplier).coerceAtMost(maxDelayMs)
    }
}

enum class ConflictWinner {
    LOCAL,   // Use local data, push again
    REMOTE,  // Discard local change, accept server state
    NONE     // No conflict, both sides agree
}
