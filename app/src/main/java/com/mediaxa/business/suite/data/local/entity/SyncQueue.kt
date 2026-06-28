package com.mediaxa.business.suite.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Defines the type of entity being synced to the cloud.
 * Used as discriminator in [SyncQueueItem.entityType].
 */
enum class SyncEntityType {
    TRANSACTION,
    TRANSACTION_ITEM,
    PAYMENT,
    CUSTOMER,
    MENU,
    CATEGORY,
    INGREDIENT,
    MENU_RECIPE,
    STOCK_MOVEMENT,
    PURCHASE_EXPENSE,
    PURCHASE_EXPENSE_ITEM,
    STOCK_OPNAME,
    STOCK_OPNAME_ITEM,
    WASTE_LOG,
    EXPENSE,
    LOYALTY_POINT_HISTORY,
    PROMOTION_RULE,
    CASH_SHIFT,
    DAILY_CLOSING,
    VOID_LOG,
    AUDIT_LOG,
    STORE_SETTING,
    USER
}

/** The type of mutation that produced this queue entry. */
enum class SyncOperation { CREATE, UPDATE, DELETE }

/** Lifecycle state of a single [SyncQueueItem]. */
enum class SyncQueueStatus {
    PENDING,      // Waiting to be processed
    IN_PROGRESS,  // Currently being sent to remote
    SYNCED,       // Successfully acknowledged by remote
    FAILED,       // Exhausted all retries
    CANCELLED     // Superseded by a later mutation on the same entity
}

/**
 * Persistent outbox queue for Offline-First cloud synchronization.
 *
 * Every data mutation (insert/update/delete) on a sync-enabled entity must
 * produce a corresponding [SyncQueueItem]. The [SyncEngine] drains this table
 * in batches, forwarding payloads to [RemoteDataSource].
 *
 * Design decisions:
 * - [payload] stores a JSON-serialized DTO so the engine can replay mutations
 *   independently of the live entity state.
 * - [uuid] + [entityType] + [operation] together are used for deduplication.
 * - [nextRetryAt] drives exponential backoff scheduling.
 */
@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["status", "nextRetryAt"]),
        Index(value = ["entityType", "uuid", "operation"]),
        Index(value = ["storeId", "status"])
    ]
)
data class SyncQueueItem(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,

    /** UUID of the entity being synced — used for idempotency on the server. */
    val uuid: String,
    val storeId: Long,
    val deviceId: String,

    val entityType: String,   // SyncEntityType.name
    val operation: String,    // SyncOperation.name

    /** JSON-serialized DTO payload (kotlinx.serialization). */
    val payload: String,

    val status: String = SyncQueueStatus.PENDING.name,
    val retryCount: Int = 0,
    val maxRetries: Int = 5,

    /** Epoch millis — item is eligible for processing when System.currentTimeMillis() >= nextRetryAt. */
    val nextRetryAt: Long = 0L,

    val createdAt: Long = System.currentTimeMillis(),
    val lastAttemptAt: Long? = null,
    val errorMessage: String? = null
)
