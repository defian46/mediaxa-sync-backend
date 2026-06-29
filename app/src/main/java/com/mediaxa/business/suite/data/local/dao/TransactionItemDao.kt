package com.mediaxa.business.suite.data.local.dao

import androidx.room.*
import com.mediaxa.business.suite.data.local.entity.TransactionItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionItemDao {
    @Query("SELECT * FROM transaction_items WHERE transactionUuid = :transactionUuid AND isDeleted = 0")
    fun getItemsForTransactionFlow(transactionUuid: String): Flow<List<TransactionItem>>

    @Query("SELECT * FROM transaction_items WHERE transactionUuid = :transactionUuid AND isDeleted = 0")
    suspend fun getItemsForTransaction(transactionUuid: String): List<TransactionItem>

    @Query("SELECT * FROM transaction_items WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedTransactionItems(): List<TransactionItem>

    @Query("SELECT * FROM transaction_items WHERE uuid = :uuid LIMIT 1")
    suspend fun getTransactionItemByUuid(uuid: String): TransactionItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<TransactionItem>)

    @Query("SELECT COUNT(*) FROM transaction_items WHERE isDeleted = 0")
    suspend fun getTransactionItemCount(): Int
}
