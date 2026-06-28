package com.mediaxa.business.suite.data.sync

import android.util.Log
import com.mediaxa.business.suite.data.local.dao.SyncQueueDao
import com.mediaxa.business.suite.data.local.entity.SyncEntityType
import com.mediaxa.business.suite.data.local.entity.SyncQueueItem
import com.mediaxa.business.suite.data.local.entity.SyncQueueStatus
import com.mediaxa.business.suite.data.remote.datasource.RemoteDataSource
import com.mediaxa.business.suite.data.remote.datasource.SyncResult
import com.mediaxa.business.suite.data.remote.dto.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

private const val TAG = "SyncEngine"
private const val BATCH_SIZE = 50

/**
 * Core sync engine that drains the [SyncQueueDao] and forwards payloads
 * to [RemoteDataSource].
 *
 * Responsibilities:
 * - Polls the sync_queue table for PENDING items
 * - Groups items by entity type for batched network calls
 * - Applies exponential backoff on failure via [ConflictResolver.calculateBackoffDelayMs]
 * - Marks items SYNCED or increments retry count
 * - Recovers items stuck in IN_PROGRESS state on startup (crash recovery)
 *
 * This class is intentionally stateless — all state lives in the Room database.
 */
class SyncEngine(
    private val syncQueueDao: SyncQueueDao,
    private val remoteDataSource: RemoteDataSource
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Entry point for a single sync cycle.
     * Called by [SyncWorker] or manually via "Force Sync" in [SyncMonitorScreen].
     *
     * @return [SyncEngineResult] with statistics for monitoring UI
     */
    suspend fun processQueue(): SyncEngineResult {
        // Crash recovery: reset any IN_PROGRESS items from a previous crashed session
        syncQueueDao.recoverStuckItems()

        val now = System.currentTimeMillis()
        val pendingItems = syncQueueDao.getPendingItems(now, BATCH_SIZE)

        if (pendingItems.isEmpty()) {
            Log.d(TAG, "Queue is empty, nothing to sync.")
            return SyncEngineResult(processedCount = 0, successCount = 0, failureCount = 0)
        }

        Log.d(TAG, "Processing ${pendingItems.size} pending items...")

        // Mark all as IN_PROGRESS atomically before processing
        syncQueueDao.markInProgress(pendingItems.map { it.localId })

        val grouped = pendingItems.groupBy { it.entityType }
        var successCount = 0
        var failureCount = 0

        for ((entityType, items) in grouped) {
            val result = dispatchBatch(entityType, items)
            when (result) {
                is SyncResult.Success -> {
                    items.forEach { syncQueueDao.markSynced(it.localId) }
                    successCount += items.size
                    Log.d(TAG, "✓ $entityType: ${items.size} items synced.")
                }
                is SyncResult.NetworkUnavailable -> {
                    // Reset to PENDING with no penalty — just retry later
                    items.forEach {
                        syncQueueDao.incrementRetry(
                            localId = it.localId,
                            nextRetryAt = System.currentTimeMillis() + 15_000L, // retry in 15s
                            errorMsg = "Network unavailable"
                        )
                    }
                    failureCount += items.size
                    Log.w(TAG, "✗ $entityType: Network unavailable. Will retry.")
                }
                is SyncResult.Unauthorized -> {
                    // Non-retryable until user re-authenticates
                    items.forEach {
                        syncQueueDao.markFailed(it.localId, "Unauthorized — re-login required")
                    }
                    failureCount += items.size
                    Log.e(TAG, "✗ $entityType: Unauthorized. Items marked FAILED.")
                }
                is SyncResult.Failure -> {
                    items.forEach { item ->
                        val nextRetry = item.retryCount + 1
                        if (nextRetry > item.maxRetries) {
                            syncQueueDao.markFailed(item.localId, result.errorMsg)
                            Log.e(TAG, "✗ $entityType uuid=${item.uuid}: Max retries exhausted. Marked FAILED.")
                        } else {
                            val backoffMs = ConflictResolver.calculateBackoffDelayMs(nextRetry)
                            syncQueueDao.incrementRetry(
                                localId = item.localId,
                                nextRetryAt = System.currentTimeMillis() + backoffMs,
                                errorMsg = result.errorMsg
                            )
                            Log.w(TAG, "✗ $entityType uuid=${item.uuid}: Retry $nextRetry scheduled in ${backoffMs}ms.")
                        }
                    }
                    failureCount += items.size
                }
            }
        }

        // Purge SYNCED items older than 7 days to prevent table bloat
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        syncQueueDao.clearSyncedItems(sevenDaysAgo)

        Log.d(TAG, "Sync cycle complete. Success=$successCount, Failure=$failureCount")
        return SyncEngineResult(
            processedCount = pendingItems.size,
            successCount = successCount,
            failureCount = failureCount
        )
    }

    /** Enqueue all FAILED items for retry (triggered by user "Retry Failed" action). */
    suspend fun retryFailedItems() {
        syncQueueDao.resetFailedItems()
        Log.d(TAG, "All FAILED items reset to PENDING for retry.")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dispatch — routes each entity type batch to the correct remote method
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun dispatchBatch(
        entityType: String,
        items: List<SyncQueueItem>
    ): SyncResult {
        return try {
            when (SyncEntityType.valueOf(entityType)) {
                SyncEntityType.TRANSACTION ->
                    remoteDataSource.syncTransactions(items.decodePayloads())
                SyncEntityType.TRANSACTION_ITEM ->
                    remoteDataSource.syncTransactionItems(items.decodePayloads())
                SyncEntityType.PAYMENT ->
                    remoteDataSource.syncPayments(items.decodePayloads())
                SyncEntityType.CUSTOMER ->
                    remoteDataSource.syncCustomers(items.decodePayloads())
                SyncEntityType.LOYALTY_POINT_HISTORY ->
                    remoteDataSource.syncLoyaltyPointHistory(items.decodePayloads())
                SyncEntityType.MENU ->
                    remoteDataSource.syncMenus(items.decodePayloads())
                SyncEntityType.CATEGORY ->
                    remoteDataSource.syncCategories(items.decodePayloads())
                SyncEntityType.INGREDIENT ->
                    remoteDataSource.syncIngredients(items.decodePayloads())
                SyncEntityType.MENU_RECIPE ->
                    remoteDataSource.syncMenuRecipes(items.decodePayloads())
                SyncEntityType.STOCK_MOVEMENT ->
                    remoteDataSource.syncStockMovements(items.decodePayloads())
                SyncEntityType.PURCHASE_EXPENSE ->
                    remoteDataSource.syncPurchaseExpenses(items.decodePayloads())
                SyncEntityType.PURCHASE_EXPENSE_ITEM ->
                    remoteDataSource.syncPurchaseExpenseItems(items.decodePayloads())
                SyncEntityType.STOCK_OPNAME ->
                    remoteDataSource.syncStockOpnames(items.decodePayloads())
                SyncEntityType.STOCK_OPNAME_ITEM ->
                    remoteDataSource.syncStockOpnameItems(items.decodePayloads())
                SyncEntityType.EXPENSE ->
                    remoteDataSource.syncExpenses(items.decodePayloads())
                SyncEntityType.WASTE_LOG ->
                    remoteDataSource.syncWasteLogs(items.decodePayloads())
                SyncEntityType.CASH_SHIFT ->
                    remoteDataSource.syncCashShifts(items.decodePayloads())
                SyncEntityType.DAILY_CLOSING ->
                    remoteDataSource.syncDailyClosings(items.decodePayloads())
                SyncEntityType.PROMOTION_RULE ->
                    remoteDataSource.syncPromotionRules(items.decodePayloads())
                // Metadata entities — no cloud sync needed in Phase 9
                SyncEntityType.VOID_LOG,
                SyncEntityType.AUDIT_LOG,
                SyncEntityType.STORE_SETTING,
                SyncEntityType.USER -> SyncResult.Success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception dispatching $entityType: ${e.message}")
            SyncResult.Failure(errorMsg = e.message ?: "Unknown error", isRetryable = true)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inline helper: decode JSON payload list for a given DTO type
    // ─────────────────────────────────────────────────────────────────────────

    private inline fun <reified T> List<SyncQueueItem>.decodePayloads(): List<T> =
        this.mapNotNull { item ->
            try {
                json.decodeFromString<T>(item.payload)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode payload for uuid=${item.uuid}: ${e.message}")
                null
            }
        }
}

/** Summary of a single sync engine processing cycle. */
data class SyncEngineResult(
    val processedCount: Int,
    val successCount: Int,
    val failureCount: Int
) {
    val hasFailures: Boolean get() = failureCount > 0
    val isFullSuccess: Boolean get() = processedCount > 0 && failureCount == 0
}
