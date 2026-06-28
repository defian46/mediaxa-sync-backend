package com.mediaxa.business.suite.data.sync

import android.util.Log

/**
 * Backward-compatible facade over [SyncEngine].
 *
 * [SyncManager] existed before Phase 9 as a prototype sync coordinator.
 * It is preserved to avoid breaking any existing call sites, but all logic
 * has been delegated to [SyncEngine] which implements the full queue-based
 * Offline-First sync architecture.
 *
 * Usage: Continue using [SyncManager.performSync] as the entry point from
 * application code. The [SyncEngine] can also be used directly where more
 * granular control (e.g., retry only failed) is needed.
 */
class SyncManager(private val syncEngine: SyncEngine) {

    /**
     * Performs a full sync cycle by draining the [SyncEngine] queue.
     *
     * @return true if all items synced successfully, false if any failures occurred
     */
    suspend fun performSync(): Boolean {
        Log.d("SyncManager", "Starting sync via SyncEngine...")
        val result = syncEngine.processQueue()
        Log.d("SyncManager", "Sync complete: $result")
        return !result.hasFailures
    }

    /** Retry all previously failed sync items. */
    suspend fun retryFailed() {
        syncEngine.retryFailedItems()
    }
}
