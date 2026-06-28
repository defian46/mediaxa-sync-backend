package com.mediaxa.business.suite.data.repository

import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.local.entity.Transaction
import com.mediaxa.business.suite.data.local.entity.TransactionItem
import com.mediaxa.business.suite.data.local.entity.Payment
import com.mediaxa.business.suite.data.local.entity.VoidLog
import com.mediaxa.business.suite.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val localDataSource: LocalDataSource) {
    fun getAllTransactionsFlow(): Flow<List<Transaction>> = localDataSource.transactionDao.getAllTransactionsFlow()
    fun getTransactionsByCashierFlow(cashierUuid: String): Flow<List<Transaction>> = localDataSource.transactionDao.getTransactionsByCashierFlow(cashierUuid)
    fun getTransactionsInRangeFlow(startTime: Long, endTime: Long): Flow<List<Transaction>> = localDataSource.transactionDao.getTransactionsInRangeFlow(startTime, endTime)
    suspend fun getTransactionsInRange(startTime: Long, endTime: Long): List<Transaction> = localDataSource.transactionDao.getTransactionsInRange(startTime, endTime)
    suspend fun getTransactionByUuid(uuid: String): Transaction? = localDataSource.transactionDao.getTransactionByUuid(uuid)
    suspend fun getTransactionCount(): Int = localDataSource.transactionDao.getTransactionCount()

    fun getItemsForTransactionFlow(transactionUuid: String): Flow<List<TransactionItem>> = localDataSource.transactionItemDao.getItemsForTransactionFlow(transactionUuid)
    suspend fun getItemsForTransaction(transactionUuid: String): List<TransactionItem> = localDataSource.transactionItemDao.getItemsForTransaction(transactionUuid)

    fun getPaymentsForTransactionFlow(transactionUuid: String): Flow<List<Payment>> = localDataSource.paymentDao.getPaymentsForTransactionFlow(transactionUuid)

    suspend fun insertTransaction(transaction: Transaction, items: List<TransactionItem>, payments: List<Payment>): Long {
        val txId = localDataSource.transactionDao.insertTransaction(transaction)
        localDataSource.transactionItemDao.insertTransactionItems(items)
        localDataSource.paymentDao.insertPayments(payments)
        return txId
    }

    fun getAllVoidLogsFlow(): Flow<List<VoidLog>> = localDataSource.voidLogDao.getAllVoidLogsFlow()

    suspend fun voidTransaction(transactionUuid: String, voidedByUserUuid: String, reason: String) {
        val tx = localDataSource.transactionDao.getTransactionByUuid(transactionUuid)
        if (tx != null) {
            val updatedTx = tx.copy(
                status = "VOID",
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_UPDATE.name
            )
            localDataSource.transactionDao.updateTransaction(updatedTx)
            val voidLog = VoidLog(
                transactionUuid = transactionUuid,
                voidedByUserUuid = voidedByUserUuid,
                reason = reason
            )
            localDataSource.voidLogDao.insertVoidLog(voidLog)
        }
    }
}
