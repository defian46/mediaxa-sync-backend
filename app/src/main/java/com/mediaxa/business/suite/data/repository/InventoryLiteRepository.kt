package com.mediaxa.business.suite.data.repository

import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.local.dao.IngredientUsageResult
import com.mediaxa.business.suite.data.local.entity.Expense
import com.mediaxa.business.suite.data.local.entity.WasteLog

class InventoryLiteRepository(private val localDataSource: LocalDataSource) {

    suspend fun getTotalStockAssetValue(storeId: Long): Double {
        return localDataSource.inventoryLiteDao.getTotalStockAssetValue(storeId) ?: 0.0
    }

    suspend fun getLowStockCount(storeId: Long): Int {
        return localDataSource.inventoryLiteDao.getLowStockCount(storeId)
    }

    suspend fun getOutOfStockCount(storeId: Long): Int {
        return localDataSource.inventoryLiteDao.getOutOfStockCount(storeId)
    }

    suspend fun getMonthlyPurchasesTotal(storeId: Long, startDate: Long, endDate: Long): Double {
        val expenses = localDataSource.inventoryLiteDao.getExpensesInPeriod(storeId, startDate, endDate)
        return expenses.filter { 
            it.category == com.mediaxa.business.suite.data.local.entity.ExpenseCategory.INVENTORY_PURCHASE.name || 
            it.category == "Belanja Bahan Baku" 
        }.sumOf { it.amount }
    }

    suspend fun getMonthlyWasteTotal(storeId: Long, startDate: Long, endDate: Long): Double {
        val logs = localDataSource.inventoryLiteDao.getWasteLogsInPeriod(storeId, startDate, endDate)
        return logs.sumOf { it.calculatedCost }
    }

    suspend fun getTopConsumedIngredients(storeId: Long, startDate: Long, endDate: Long, limit: Int = 5): List<IngredientUsageResult> {
        return localDataSource.inventoryLiteDao.getTopConsumedIngredients(storeId, startDate, endDate, limit)
    }

    suspend fun getTopPurchasedIngredients(storeId: Long, startDate: Long, endDate: Long, limit: Int = 5): List<IngredientUsageResult> {
        return localDataSource.inventoryLiteDao.getTopPurchasedIngredients(storeId, startDate, endDate, limit)
    }
}
