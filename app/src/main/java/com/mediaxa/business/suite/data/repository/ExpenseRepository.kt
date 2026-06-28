package com.mediaxa.business.suite.data.repository

import com.mediaxa.business.suite.data.local.database.AppDatabase
import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.local.entity.AuditLog
import com.mediaxa.business.suite.data.local.entity.Expense
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val database: AppDatabase,
    private val localDataSource: LocalDataSource,
    private val transactionRunner: TransactionRunner = RoomTransactionRunner(database)
) {

    fun getAllExpensesFlow(storeId: Long): Flow<List<Expense>> {
        return localDataSource.inventoryLiteDao.getAllExpensesFlow(storeId)
    }

    suspend fun insertExpense(expense: Expense): Long {
        return transactionRunner.run {
            val localId = localDataSource.inventoryLiteDao.insertExpense(expense)
            val user = localDataSource.userDao.getUserByUuid(expense.userUuid)
            val username = user?.username ?: "System"
            val auditLog = AuditLog(
                storeId = expense.storeId,
                deviceId = expense.deviceId,
                userUuid = expense.userUuid,
                username = username,
                action = "GENERAL_EXPENSE",
                entity = "Expense",
                entityId = expense.uuid,
                oldValue = null,
                newValue = "Category: ${expense.category}, Amount: ${expense.amount}, PaymentMethod: ${expense.paymentMethod}, Notes: ${expense.notes}"
            )
            localDataSource.auditLogDao.insertLog(auditLog)
            localId
        }
    }
}

