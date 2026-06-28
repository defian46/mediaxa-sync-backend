package com.mediaxa.business.suite.data.repository

import com.mediaxa.business.suite.data.local.database.AppDatabase
import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class PurchaseExpenseRepository(
    private val database: AppDatabase,
    private val localDataSource: LocalDataSource,
    private val transactionRunner: TransactionRunner = RoomTransactionRunner(database)
) {

    fun getAllPurchaseExpensesFlow(storeId: Long): Flow<List<PurchaseExpense>> {
        return localDataSource.inventoryLiteDao.getAllPurchaseExpensesFlow(storeId)
    }

    suspend fun recordPurchaseExpense(
        purchaseExpense: PurchaseExpense,
        items: List<PurchaseExpenseItem>,
        userUuid: String,
        paymentMethod: String
    ): Boolean {
        if (items.isEmpty()) return false

        return try {
            transactionRunner.run {
                // 1. Insert Purchase Expense Header
                localDataSource.inventoryLiteDao.insertPurchaseExpense(purchaseExpense)

                // 2. Insert Items and Update Ingredients Stocks/Prices
                localDataSource.inventoryLiteDao.insertPurchaseExpenseItems(items)

                for (item in items) {
                    val ingredient = localDataSource.ingredientDao.getIngredientByUuid(item.ingredientUuid)
                    if (ingredient != null) {
                        // Calculate new prices
                        val newPurchasePrice = item.unitPrice // Cost per package bought
                        val newUnitPrice = if (ingredient.packageSize > 0) newPurchasePrice / ingredient.packageSize else newPurchasePrice

                        val updatedIngredient = ingredient.copy(
                            availableStock = ingredient.availableStock + item.quantity,
                            purchasePrice = newPurchasePrice,
                            unitPrice = newUnitPrice,
                            updatedAt = System.currentTimeMillis(),
                            syncStatus = SyncStatus.PENDING_UPDATE.name
                        )
                        localDataSource.ingredientDao.updateIngredient(updatedIngredient)

                        // 3. Write StockMovement STOCK_IN_PURCHASE
                        val movement = StockMovement(
                            uuid = UUID.randomUUID().toString(),
                            storeId = purchaseExpense.storeId,
                            deviceId = purchaseExpense.deviceId,
                            ingredientUuid = item.ingredientUuid,
                            quantity = item.quantity,
                            type = "STOCK_IN_PURCHASE",
                            note = "Belanja: ${purchaseExpense.notes ?: ""}",
                            userUuid = userUuid,
                            syncStatus = SyncStatus.PENDING_CREATE.name
                        )
                        localDataSource.stockMovementDao.insertMovement(movement)
                    }
                }

                // 4. Create General Expense (Belanja Bahan Baku via ExpenseCategory enum name)
                val generalExpense = Expense(
                    uuid = UUID.randomUUID().toString(),
                    storeId = purchaseExpense.storeId,
                    deviceId = purchaseExpense.deviceId,
                    expenseDate = purchaseExpense.purchaseDate,
                    category = ExpenseCategory.INVENTORY_PURCHASE.name,
                    amount = purchaseExpense.totalAmount,
                    notes = "Belanja Bahan Baku: ${purchaseExpense.notes ?: ""}",
                    userUuid = userUuid,
                    paymentMethod = paymentMethod
                )
                localDataSource.inventoryLiteDao.insertExpense(generalExpense)

                // 5. Create AuditLog for Purchase Expense
                val user = localDataSource.userDao.getUserByUuid(userUuid)
                val username = user?.username ?: "System"
                val auditLog = AuditLog(
                    storeId = purchaseExpense.storeId,
                    deviceId = purchaseExpense.deviceId,
                    userUuid = userUuid,
                    username = username,
                    action = "PURCHASE_EXPENSE",
                    entity = "PurchaseExpense",
                    entityId = purchaseExpense.uuid,
                    oldValue = null,
                    newValue = "Place: ${purchaseExpense.purchasePlaceName}, Amount: ${purchaseExpense.totalAmount}, PaymentMethod: ${purchaseExpense.paymentMethod}, Notes: ${purchaseExpense.notes}"
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
