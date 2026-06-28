package com.mediaxa.business.suite.data.local.dao

import androidx.room.*
import com.mediaxa.business.suite.data.local.entity.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE cashierUuid = :cashierUuid AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getTransactionsByCashierFlow(cashierUuid: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getTransactionsInRangeFlow(startTime: Long, endTime: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime AND isDeleted = 0")
    suspend fun getTransactionsInRange(startTime: Long, endTime: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE uuid = :uuid AND isDeleted = 0 LIMIT 1")
    suspend fun getTransactionByUuid(uuid: String): Transaction?

    @Query("SELECT COUNT(*) FROM transactions WHERE isDeleted = 0")
    suspend fun getTransactionCount(): Int

    @Query("SELECT * FROM transactions WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedTransactions(): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)
}
