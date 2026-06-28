package com.mediaxa.business.suite.data.local.dao

import androidx.room.*
import com.mediaxa.business.suite.data.local.entity.SyncEntityType
import com.mediaxa.business.suite.data.local.entity.SyncQueueItem
import com.mediaxa.business.suite.data.local.entity.SyncQueueStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {

    // ─── Write Operations ───────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(item: SyncQueueItem): Long

    @Update
    suspend fun update(item: SyncQueueItem)

    // ─── Queue Polling ───────────────────────────────────────────────────────

    /**
     * Returns up to [limit] items that are eligible for processing.
     * An item is eligible when its [SyncQueueItem.nextRetryAt] <= [now]
     * and its status is PENDING.
     */
    @Query("""
        SELECT * FROM sync_queue
        WHERE status = 'PENDING' AND nextRetryAt <= :now
        ORDER BY createdAt ASC
        LIMIT :limit
    """)
    suspend fun getPendingItems(now: Long, limit: Int = 50): List<SyncQueueItem>

    /** Mark a batch of items as IN_PROGRESS atomically. */
    @Query("""
        UPDATE sync_queue
        SET status = 'IN_PROGRESS', lastAttemptAt = :now
        WHERE localId IN (:ids)
    """)
    suspend fun markInProgress(ids: List<Long>, now: Long = System.currentTimeMillis())

    /** Mark an item as successfully synced. */
    @Query("""
        UPDATE sync_queue
        SET status = 'SYNCED'
        WHERE localId = :localId
    """)
    suspend fun markSynced(localId: Long)

    /** Increment retry count, set next eligible time, and record error. */
    @Query("""
        UPDATE sync_queue
        SET status = 'PENDING',
            retryCount = retryCount + 1,
            nextRetryAt = :nextRetryAt,
            errorMessage = :errorMsg,
            lastAttemptAt = :now
        WHERE localId = :localId
    """)
    suspend fun incrementRetry(
        localId: Long,
        nextRetryAt: Long,
        errorMsg: String,
        now: Long = System.currentTimeMillis()
    )

    /** Mark item as permanently FAILED after exhausting maxRetries. */
    @Query("""
        UPDATE sync_queue
        SET status = 'FAILED', errorMessage = :errorMsg
        WHERE localId = :localId
    """)
    suspend fun markFailed(localId: Long, errorMsg: String)

    /**
     * Cancel all PENDING queue entries for a specific entity.
     * Used when a DELETE operation supersedes earlier CREATE/UPDATE entries.
     */
    @Query("""
        UPDATE sync_queue
        SET status = 'CANCELLED'
        WHERE uuid = :entityUuid AND entityType = :entityType AND status = 'PENDING'
    """)
    suspend fun cancelByEntityUuid(entityUuid: String, entityType: String)

    // ─── Observability ───────────────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'FAILED'")
    fun observeFailedCount(): Flow<Int>

    @Query("SELECT * FROM sync_queue WHERE status = 'FAILED' ORDER BY lastAttemptAt DESC LIMIT 50")
    fun observeFailedItems(): Flow<List<SyncQueueItem>>

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC LIMIT 20")
    fun observePendingItems(): Flow<List<SyncQueueItem>>

    @Query("SELECT MAX(lastAttemptAt) FROM sync_queue WHERE status = 'SYNCED'")
    fun observeLastSyncedAt(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status IN ('PENDING', 'IN_PROGRESS', 'FAILED')")
    fun observeTotalQueueSize(): Flow<Int>

    // ─── Maintenance ─────────────────────────────────────────────────────────

    /**
     * Purge successfully synced items older than [olderThanMs] to prevent table bloat.
     * Recommended to run during off-peak hours or on app start.
     */
    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED' AND lastAttemptAt < :olderThanMs")
    suspend fun clearSyncedItems(olderThanMs: Long)

    /** Reset IN_PROGRESS items back to PENDING (recovery after app crash). */
    @Query("UPDATE sync_queue SET status = 'PENDING' WHERE status = 'IN_PROGRESS'")
    suspend fun recoverStuckItems()

    /** Reset all FAILED items to PENDING for a forced retry. */
    @Query("UPDATE sync_queue SET status = 'PENDING', retryCount = 0, nextRetryAt = 0 WHERE status = 'FAILED'")
    suspend fun resetFailedItems()

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun getSyncedCount(): Int
}
