package com.mediaxa.business.suite.data.repository

import com.mediaxa.business.suite.data.local.database.AppDatabase
import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class StockOpnameRepository(
    private val database: AppDatabase,
    private val localDataSource: LocalDataSource,
    private val transactionRunner: TransactionRunner = RoomTransactionRunner(database)
) {

    fun getAllStockOpnamesFlow(storeId: Long): Flow<List<StockOpname>> {
        return localDataSource.inventoryLiteDao.getAllStockOpnamesFlow(storeId)
    }

    suspend fun recordStockOpname(
        opname: StockOpname,
        items: List<StockOpnameItem>,
        userUuid: String
    ): Boolean {
        if (items.isEmpty()) return false

        return try {
            transactionRunner.run {
                // 1. Insert Stock Opname Header
                localDataSource.inventoryLiteDao.insertStockOpname(opname)

                // 2. Insert Items and Update Ingredient Stock
                localDataSource.inventoryLiteDao.insertStockOpnameItems(items)

                for (item in items) {
                    val ingredient = localDataSource.ingredientDao.getIngredientByUuid(item.ingredientUuid)
                    if (ingredient != null) {
                        val updatedIngredient = ingredient.copy(
                            availableStock = item.physicalStock,
                            updatedAt = System.currentTimeMillis(),
                            syncStatus = SyncStatus.PENDING_UPDATE.name
                        )
                        localDataSource.ingredientDao.updateIngredient(updatedIngredient)

                        // 3. Write StockMovement ADJUSTMENT
                        val movement = StockMovement(
                            uuid = UUID.randomUUID().toString(),
                            storeId = opname.storeId,
                            deviceId = opname.deviceId,
                            ingredientUuid = item.ingredientUuid,
                            quantity = item.diffStock,
                            type = "ADJUSTMENT",
                            note = "Opname: ${item.notes}",
                            userUuid = userUuid,
                            syncStatus = SyncStatus.PENDING_CREATE.name
                        )
                        localDataSource.stockMovementDao.insertMovement(movement)
                    }
                }

                // 4. Create AuditLog for Stock Opname
                val user = localDataSource.userDao.getUserByUuid(userUuid)
                val username = user?.username ?: "System"
                val auditLog = AuditLog(
                    storeId = opname.storeId,
                    deviceId = opname.deviceId,
                    userUuid = userUuid,
                    username = username,
                    action = "STOCK_OPNAME",
                    entity = "StockOpname",
                    entityId = opname.uuid,
                    oldValue = null,
                    newValue = "Date: ${opname.opnameDate}, ItemsCount: ${items.size}, Notes: ${opname.notes}"
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
