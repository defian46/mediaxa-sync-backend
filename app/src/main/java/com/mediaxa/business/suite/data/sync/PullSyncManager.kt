package com.mediaxa.business.suite.data.sync

import android.util.Log
import com.mediaxa.business.suite.data.local.entity.*
import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.remote.NetworkClient
import com.mediaxa.business.suite.data.remote.PullResponse

private const val TAG = "PullSyncManager"

/**
 * Applies data pulled from the cloud (GET /api/v1/sync/pull) into local Room tables.
 *
 * Design contract:
 * - All pulled records are upserted via Room's REPLACE strategy (idempotent).
 * - Pulled records are marked [SyncStatus.SYNCED] immediately since they are cloud-sourced.
 * - This class does NOT modify the SyncQueue — it only writes to entity tables.
 * - Immutable financial records (transactions, stock movements) are INSERT-ONLY: if they
 *   already exist locally, they are skipped to protect local data integrity.
 * - Called after [SyncEngine] completes a push cycle, or on app foreground resume.
 */
class PullSyncManager(
    private val localDataSource: LocalDataSource
) {

    /**
     * Pulls all changes since [lastSyncTime] for the given [storeUuid] and
     * persists them to Room.
     *
     * @return new high-watermark timestamp on success, null on network failure
     */
    suspend fun pull(storeUuid: String, lastSyncTime: Long, accessToken: String): Long? {
        Log.d(TAG, "Pulling changes since $lastSyncTime for store $storeUuid")

        val response: PullResponse = NetworkClient.pull(storeUuid, lastSyncTime, accessToken)
            ?: run {
                Log.e(TAG, "Pull failed — no response from server")
                return null
            }

        val now = System.currentTimeMillis()
        var persistedCount = 0

        // ── Categories ────────────────────────────────────────────────────────
        response.categories.forEach { dto ->
            val existing = localDataSource.categoryDao.getCategoryByUuid(dto.uuid)
            val entity = existing?.copy(
                name = dto.name,
                isDeleted = dto.isDeleted,
                updatedAt = NetworkClient.parseIsoDateTime(dto.updatedAt),
                syncStatus = SyncStatus.SYNCED.name,
                isSynced = true,
                lastSyncedAt = now
            ) ?: Category(
                uuid = dto.uuid,
                name = dto.name,
                isDeleted = dto.isDeleted,
                updatedAt = NetworkClient.parseIsoDateTime(dto.updatedAt),
                syncStatus = SyncStatus.SYNCED.name,
                isSynced = true,
                lastSyncedAt = now
            )
            localDataSource.categoryDao.insertCategory(entity)
            persistedCount++
        }

        // ── Menus ─────────────────────────────────────────────────────────────
        response.menus.forEach { dto ->
            val existing = localDataSource.menuDao.getMenuByUuid(dto.uuid)
            val entity = existing?.copy(
                name = dto.name,
                price = dto.price,
                categoryUuid = dto.categoryUuid,
                isActive = dto.isActive,
                isDeleted = dto.isDeleted,
                updatedAt = NetworkClient.parseIsoDateTime(dto.updatedAt),
                syncStatus = SyncStatus.SYNCED.name,
                isSynced = true,
                lastSyncedAt = now
            ) ?: Menu(
                uuid = dto.uuid,
                name = dto.name,
                price = dto.price,
                categoryUuid = dto.categoryUuid,
                isActive = dto.isActive,
                isDeleted = dto.isDeleted,
                updatedAt = NetworkClient.parseIsoDateTime(dto.updatedAt),
                syncStatus = SyncStatus.SYNCED.name,
                isSynced = true,
                lastSyncedAt = now
            )
            localDataSource.menuDao.insertMenu(entity)
            persistedCount++
        }

        // ── Ingredients ───────────────────────────────────────────────────────
        // Stock level is updated by StockMovement pull — we only sync metadata here
        response.ingredients.forEach { dto ->
            val existing = localDataSource.ingredientDao.getIngredientByUuid(dto.uuid)
            val entity = existing?.copy(
                name = dto.name,
                unit = dto.unit,
                availableStock = dto.availableStock,
                isDeleted = dto.isDeleted,
                updatedAt = NetworkClient.parseIsoDateTime(dto.updatedAt),
                syncStatus = SyncStatus.SYNCED.name,
                isSynced = true,
                lastSyncedAt = now
            ) ?: Ingredient(
                uuid = dto.uuid,
                name = dto.name,
                unit = dto.unit,
                availableStock = dto.availableStock,
                isDeleted = dto.isDeleted,
                updatedAt = NetworkClient.parseIsoDateTime(dto.updatedAt),
                syncStatus = SyncStatus.SYNCED.name,
                isSynced = true,
                lastSyncedAt = now,
                // Required fields with safe defaults for cloud-sourced records
                purchasePrice = 0.0,
                packageSize = 1.0,
                unitPrice = 0.0
            )
            localDataSource.ingredientDao.insertIngredient(entity)
            persistedCount++
        }

        // ── Customers ─────────────────────────────────────────────────────────
        response.customers.forEach { dto ->
            val existing = localDataSource.customerDao.getCustomerByUuid(dto.uuid)
            val entity = existing?.copy(
                customerName = dto.name,
                phone = dto.phone,
                email = dto.email,
                updatedAt = NetworkClient.parseIsoDateTime(dto.updatedAt),
                syncStatus = SyncStatus.SYNCED.name
            ) ?: Customer(
                uuid = dto.uuid,
                customerCode = "CLOUD-${dto.uuid.takeLast(6).uppercase()}",
                customerName = dto.name,
                phone = dto.phone,
                email = dto.email,
                birthday = null,
                gender = null,
                address = null,
                notes = null,
                updatedAt = NetworkClient.parseIsoDateTime(dto.updatedAt),
                syncStatus = SyncStatus.SYNCED.name
            )
            localDataSource.customerDao.insertCustomer(entity)
            persistedCount++
        }

        // ── Transactions (immutable — insert only if not yet present locally) ─
        response.transactions.forEach { dto ->
            val existing = localDataSource.transactionDao.getTransactionByUuid(dto.uuid)
            if (existing == null) {
                val entity = Transaction(
                    uuid = dto.uuid,
                    cashierUuid = dto.cashierUserUuid,
                    cashierName = dto.cashierUserUuid,
                    transactionNumber = "SYNC-${dto.uuid.takeLast(8).uppercase()}",
                    createdAt = NetworkClient.parseIsoDateTime(dto.transactionDate),
                    timestamp = NetworkClient.parseIsoDateTime(dto.transactionDate),
                    subtotal = dto.subtotal,
                    total = dto.total,
                    transactionHpp = dto.transactionHpp,
                    grossProfit = dto.grossProfit,
                    paymentMethod = dto.paymentMethod,
                    amountReceived = dto.total,
                    changeAmount = 0.0,
                    status = dto.status,
                    discount = 0.0,
                    updatedAt = NetworkClient.parseIsoDateTime(dto.updatedAt),
                    syncStatus = SyncStatus.SYNCED.name,
                    isSynced = true,
                    lastSyncedAt = now
                )
                localDataSource.transactionDao.insertTransaction(entity)
                persistedCount++
            }
        }

        // ── Stock Movements (immutable — insert only if not yet present) ──────
        response.stockMovements.forEach { dto ->
            val existing = localDataSource.stockMovementDao.getStockMovementByUuid(dto.uuid)
            if (existing == null) {
                val entity = StockMovement(
                    uuid = dto.uuid,
                    ingredientUuid = dto.ingredientUuid,
                    type = dto.type,
                    quantity = dto.qty,
                    note = dto.reason,
                    userUuid = "CLOUD",
                    createdAt = NetworkClient.parseIsoDateTime(dto.movementDate),
                    updatedAt = NetworkClient.parseIsoDateTime(dto.updatedAt),
                    syncStatus = SyncStatus.SYNCED.name,
                    isSynced = true,
                    lastSyncedAt = now
                )
                localDataSource.stockMovementDao.insertMovement(entity)
                persistedCount++
            }
        }

        Log.d(TAG, "Pull complete. Persisted $persistedCount records from cloud.")
        return now
    }
}
