package com.mediaxa.business.suite.data.repository

import com.mediaxa.business.suite.data.local.database.AppDatabase
import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class WasteRepository(
    private val database: AppDatabase,
    private val localDataSource: LocalDataSource,
    private val transactionRunner: TransactionRunner = RoomTransactionRunner(database)
) {

    fun getAllWasteLogsFlow(storeId: Long): Flow<List<WasteLog>> {
        return localDataSource.inventoryLiteDao.getAllWasteLogsFlow(storeId)
    }

    suspend fun recordWaste(
        wasteLog: WasteLog,
        userUuid: String
    ): Boolean {
        return try {
            transactionRunner.run {
                val ingredient = localDataSource.ingredientDao.getIngredientByUuid(wasteLog.ingredientUuid)
                if (ingredient == null || ingredient.availableStock < wasteLog.quantity) {
                    throw IllegalStateException("Bahan baku tidak ditemukan atau stok tidak cukup")
                }

                // Calculate cost dynamically
                val calculatedCost = wasteLog.quantity * ingredient.unitPrice
                val finalWasteLog = wasteLog.copy(calculatedCost = calculatedCost)

                // 1. Insert Waste Log
                localDataSource.inventoryLiteDao.insertWasteLog(finalWasteLog)

                // 2. Decrement available stock
                val updatedIngredient = ingredient.copy(
                    availableStock = ingredient.availableStock - wasteLog.quantity,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING_UPDATE.name
                )
                localDataSource.ingredientDao.updateIngredient(updatedIngredient)

                // 3. Insert StockMovement WASTE
                val movement = StockMovement(
                    uuid = UUID.randomUUID().toString(),
                    storeId = wasteLog.storeId,
                    deviceId = wasteLog.deviceId,
                    ingredientUuid = wasteLog.ingredientUuid,
                    quantity = -wasteLog.quantity,
                    type = "WASTE",
                    note = "Waste: ${wasteLog.reason}. ${wasteLog.notes ?: ""}",
                    userUuid = userUuid,
                    syncStatus = SyncStatus.PENDING_CREATE.name
                )
                localDataSource.stockMovementDao.insertMovement(movement)

                // 4. Create AuditLog for Waste
                val user = localDataSource.userDao.getUserByUuid(userUuid)
                val username = user?.username ?: "System"
                val auditLog = AuditLog(
                    storeId = wasteLog.storeId,
                    deviceId = wasteLog.deviceId,
                    userUuid = userUuid,
                    username = username,
                    action = "WASTE",
                    entity = "WasteLog",
                    entityId = wasteLog.uuid,
                    oldValue = null,
                    newValue = "IngredientUuid: ${wasteLog.ingredientUuid}, Quantity: ${wasteLog.quantity}, Reason: ${wasteLog.reason}, Cost: $calculatedCost"
                )
                localDataSource.auditLogDao.insertLog(auditLog)

                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
